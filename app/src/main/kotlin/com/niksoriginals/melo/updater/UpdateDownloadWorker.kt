package com.niksoriginals.melo.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.niksoriginals.melo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_URL = "download_url"
        const val KEY_VERSION = "version"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_PROGRESS = "progress"

        const val CHANNEL_ID = "update_download_channel"
        const val NOTIFICATION_ID = 99281
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadUrlStr = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: "Update"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "melo-update.apk"

        createNotificationChannel()

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetApk = File(updatesDir, fileName)

        try {
            showProgressNotification(0, version)

            var currentUrl = URL(downloadUrlStr)
            var connection: HttpURLConnection? = null
            var redirects = 0

            // Follow redirects manually (e.g., GitHub release assets redirect to AWS S3)
            while (redirects < 6) {
                val conn = (currentUrl.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Melo-Updater")
                    connect()
                }

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrEmpty()) {
                        showFailedNotification(version, "Redirect failed")
                        return@withContext Result.failure()
                    }
                    currentUrl = URL(currentUrl, location)
                    redirects++
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect()
                    showFailedNotification(version, "Server returned $code")
                    return@withContext Result.failure()
                }
                connection = conn
                break
            }

            if (connection == null) {
                showFailedNotification(version, "Connection failed")
                return@withContext Result.failure()
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(targetApk)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgress = -1
            var lastUpdateTime = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()
                    if (targetApk.exists()) targetApk.delete()
                    notificationManager.cancel(NOTIFICATION_ID)
                    return@withContext Result.retry()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (fileLength > 0) {
                    val progress = (totalBytesRead.toFloat() / fileLength.toFloat() * 100).toInt()
                    val now = System.currentTimeMillis()
                    if (progress != lastProgress && (now - lastUpdateTime >= 800 || progress == 100)) {
                        lastProgress = progress
                        lastUpdateTime = now
                        showProgressNotification(progress, version)
                        setProgress(workDataOf(KEY_PROGRESS to (progress / 100f)))
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Integrity check 1: byte count match
            if (fileLength > 0 && totalBytesRead != fileLength.toLong()) {
                if (targetApk.exists()) targetApk.delete()
                showFailedNotification(version, "Incomplete download")
                return@withContext Result.retry()
            }

            // Integrity check 2: APK zip magic "PK\x03\x04"
            if (!isValidApk(targetApk)) {
                if (targetApk.exists()) targetApk.delete()
                showFailedNotification(version, "Invalid APK downloaded")
                return@withContext Result.failure()
            }

            showCompleteNotification(version, targetApk)

            Result.success(workDataOf(KEY_FILE_PATH to targetApk.absolutePath))
        } catch (e: Exception) {
            if (targetApk.exists()) targetApk.delete()
            showFailedNotification(version, e.message ?: "Download failed")
            Result.failure()
        }
    }

    private fun isValidApk(file: File): Boolean = try {
        FileInputStream(file).use { fis ->
            val magic = ByteArray(4)
            fis.read(magic) == 4 &&
                magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
        }
    } catch (_: Exception) {
        false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_progress_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.download_progress_description)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(progress: Int, version: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.update)
            .setContentTitle(context.getString(R.string.downloading_update))
            .setContentText(context.getString(R.string.version_progress, version, progress))
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompleteNotification(version: String, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.updated)
            .setContentTitle(context.getString(R.string.update_ready))
            .setContentText(context.getString(R.string.tap_to_install_version, version))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailedNotification(version: String, reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.update)
            .setContentTitle(context.getString(R.string.update_failed))
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

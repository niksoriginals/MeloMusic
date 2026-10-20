package com.niksoriginals.melo.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object InstallHelper {
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: SecurityException) {
                false
            }
            if (!canInstall) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return false
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }
}

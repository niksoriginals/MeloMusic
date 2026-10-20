package com.niksoriginals.melo.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import java.io.IOException

class ChunkingDataSource(
    private val upstream: DataSource,
    private val chunkSize: Long
) : DataSource {

    private var dataSpec: DataSpec? = null
    private var bytesToRead: Long = C.LENGTH_UNSET.toLong()
    private var bytesReadTotal: Long = 0
    private var isOpened = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.bytesReadTotal = 0
        this.bytesToRead = dataSpec.length
        this.isOpened = true

        openNextChunk()

        return bytesToRead
    }

    private fun openNextChunk() {
        val currentDataSpec = this.dataSpec ?: throw IOException("DataSpec is null")
        val position = currentDataSpec.position + bytesReadTotal

        val length = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
            chunkSize
        } else {
            val remaining = bytesToRead - bytesReadTotal
            if (remaining == 0L) return
            minOf(chunkSize, remaining)
        }

        val chunkDataSpec = currentDataSpec.buildUpon()
            .setPosition(position)
            .setLength(length)
            .build()

        upstream.open(chunkDataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!isOpened) return C.RESULT_END_OF_INPUT
        if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesReadTotal >= bytesToRead) {
            return C.RESULT_END_OF_INPUT
        }

        val bytes = try {
            upstream.read(buffer, offset, readLength)
        } catch (e: Exception) {
            -1
        }
        
        if (bytes == C.RESULT_END_OF_INPUT || bytes == -1) {
            upstream.close()
            try {
                openNextChunk()
            } catch (e: InvalidResponseCodeException) {
                if (e.responseCode == 416) {
                    return C.RESULT_END_OF_INPUT
                }
                throw e
            } catch (e: androidx.media3.datasource.DataSourceException) {
                if (e.reason == androidx.media3.datasource.DataSourceException.POSITION_OUT_OF_RANGE) {
                    return C.RESULT_END_OF_INPUT
                }
                throw e
            }
            return try {
                val newBytes = upstream.read(buffer, offset, readLength)
                if (newBytes == C.RESULT_END_OF_INPUT || newBytes == -1) {
                    C.RESULT_END_OF_INPUT
                } else {
                    bytesReadTotal += newBytes
                    newBytes
                }
            } catch (e: Exception) {
                C.RESULT_END_OF_INPUT
            }
        }
        bytesReadTotal += bytes
        return bytes
    }

    override fun getUri(): Uri? = upstream.uri

    override fun close() {
        isOpened = false
        upstream.close()
    }
}

class ChunkingDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val chunkSize: Long = 5L * 1024 * 1024 // 5MB chunks
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return ChunkingDataSource(upstreamFactory.createDataSource(), chunkSize)
    }
}

/**
 * googlevideo rejects requests without a bounded Range (open-ended `bytes=0-` and no-Range
 * requests both 403 even for perfectly valid URLs). ExoPlayer's first open() carries
 * length=UNSET, which OkHttpDataSource sends without any Range header, so every first request
 * fails. This wrapper binds the unbounded spec to the `clen` parameter already present in the
 * URL so the upstream sends a closed `bytes=0-<clen-1>` range, which the CDN accepts.
 */
class BoundedRangeDataSource(
    private val upstream: DataSource,
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        var spec = dataSpec
        if (spec.length == C.LENGTH_UNSET.toLong()) {
            val clen = parseCleanLength(spec.uri) ?: return upstream.open(spec)
            spec = spec.buildUpon().setLength(clen).build()
        }
        return upstream.open(spec)
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return upstream.read(buffer, offset, readLength)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun close() {
        upstream.close()
    }

    private fun parseCleanLength(uri: Uri): Long? {
        val query = uri.query ?: return null
        val params = query.split('&')
        for (param in params) {
            if (param.startsWith("clen=")) {
                return param.removePrefix("clen=").toLongOrNull()
            }
        }
        return null
    }
}

class BoundedRangeDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return BoundedRangeDataSource(upstreamFactory.createDataSource())
    }
}

package com.expstudio.localai.download

import android.content.Context
import android.util.Log
import com.expstudio.localai.data.model.CatalogModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Resumable model downloader.
 *
 * - Streams to a `.part` file and uses HTTP Range requests to resume after
 *   interruptions.
 * - Emits [DownloadState] so the UI can show progress / speed.
 * - Optionally verifies SHA-256 when the catalog provides a checksum.
 *
 * Files land in `getExternalFilesDir("models")` (app-scoped, no runtime
 * permission needed, cleared on uninstall).
 */
class ModelDownloadManager(context: Context) {

    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun modelsDir(): File =
        File(appContext.getExternalFilesDir(null), "models").apply { mkdirs() }

    fun targetFile(model: CatalogModel): File = File(modelsDir(), model.fileName)

    fun isDownloaded(model: CatalogModel): Boolean = targetFile(model).exists()

    /**
     * Downloads [model], resuming if a partial file exists. Cancellable via the
     * collecting coroutine's scope.
     */
    fun download(model: CatalogModel): Flow<DownloadState> = flow {
        val finalFile = targetFile(model)
        if (finalFile.exists()) {
            emit(DownloadState.Completed(finalFile.absolutePath))
            return@flow
        }
        val partFile = File(finalFile.absolutePath + ".part")
        var existing = if (partFile.exists()) partFile.length() else 0L

        try {
            val request = Request.Builder()
                .url(model.downloadUrl)
                .apply { if (existing > 0) header("Range", "bytes=$existing-") }
                .build()

            client.newCall(request).execute().use { resp ->
                // If the server ignored our Range (200 instead of 206), restart.
                if (existing > 0 && resp.code == 200) {
                    partFile.delete(); existing = 0
                }
                if (!resp.isSuccessful) {
                    emit(DownloadState.Failed("HTTP ${resp.code}"))
                    return@flow
                }
                val body = resp.body ?: run {
                    emit(DownloadState.Failed("Empty response body")); return@flow
                }
                val total = existing + (body.contentLength().takeIf { it > 0 } ?: model.fileSizeBytes)

                RandomAccessFile(partFile, "rw").use { raf ->
                    raf.seek(existing)
                    body.byteStream().use { input ->
                        val buf = ByteArray(1 shl 16) // 64 KB
                        var downloaded = existing
                        var lastTick = System.currentTimeMillis()
                        var lastBytes = downloaded
                        while (true) {
                            coroutineContext.ensureActive() // cooperative cancellation
                            val n = input.read(buf)
                            if (n < 0) break
                            raf.write(buf, 0, n)
                            downloaded += n

                            val now = System.currentTimeMillis()
                            if (now - lastTick >= 250) {
                                val bps = (downloaded - lastBytes) * 1000 / (now - lastTick)
                                emit(DownloadState.Downloading(downloaded, total, bps))
                                lastTick = now; lastBytes = downloaded
                            }
                        }
                    }
                }
            }

            // Optional integrity check.
            if (model.sha256 != null) {
                emit(DownloadState.Verifying)
                val actual = sha256(partFile)
                if (!actual.equals(model.sha256, ignoreCase = true)) {
                    partFile.delete()
                    emit(DownloadState.Failed("Checksum mismatch")); return@flow
                }
            }

            if (!partFile.renameTo(finalFile)) {
                emit(DownloadState.Failed("Could not finalize file")); return@flow
            }
            emit(DownloadState.Completed(finalFile.absolutePath))
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Leave the .part file in place so the next attempt resumes.
            emit(DownloadState.Cancelled)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            emit(DownloadState.Failed(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    fun delete(model: CatalogModel): Boolean {
        File(targetFile(model).absolutePath + ".part").delete()
        return targetFile(model).delete()
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ins ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object { private const val TAG = "ModelDownload" }
}

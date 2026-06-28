package com.expstudio.localai.download

/** Progress snapshot for a single model download, surfaced to the UI. */
sealed interface DownloadState {
    data object Idle : DownloadState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val bytesPerSecond: Long,
    ) : DownloadState {
        val fraction: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
        val percent: Int get() = (fraction * 100).toInt()
    }

    data object Verifying : DownloadState
    data class Completed(val filePath: String) : DownloadState
    data class Failed(val message: String) : DownloadState
    data object Cancelled : DownloadState
}

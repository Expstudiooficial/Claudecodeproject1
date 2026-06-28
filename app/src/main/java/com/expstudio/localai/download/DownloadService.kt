package com.expstudio.localai.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.expstudio.localai.R
import com.expstudio.localai.data.model.ModelCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps a model download alive while the app is backgrounded by running it as a
 * foreground service with an ongoing progress notification.
 *
 * Start with [start]; the heavy lifting is delegated to [ModelDownloadManager].
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        val model = modelId?.let { ModelCatalog.byId(it) }
        if (model == null) { stopSelf(); return START_NOT_STICKY }

        ensureChannel()
        startForeground(NOTIF_ID, buildNotification(model.displayName, 0))

        val manager = ModelDownloadManager(this)
        job = scope.launch {
            manager.download(model).collect { state ->
                when (state) {
                    is DownloadState.Downloading ->
                        notify(buildNotification(model.displayName, state.percent))
                    is DownloadState.Completed, is DownloadState.Failed,
                    is DownloadState.Cancelled -> stopSelf()
                    else -> Unit
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun notify(n: Notification) =
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, n)

    private fun buildNotification(name: String, percent: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.downloading_model))
            .setContentText(name)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Model downloads",
                        NotificationManager.IMPORTANCE_LOW,
                    )
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "model_downloads"
        private const val NOTIF_ID = 42
        const val EXTRA_MODEL_ID = "model_id"

        fun start(context: Context, modelId: String) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra(EXTRA_MODEL_ID, modelId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

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
import com.expstudio.localai.LocalAiApp
import com.expstudio.localai.R
import com.expstudio.localai.data.model.ModelCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process alive (and shows an ongoing progress
 * notification) while the app is backgrounded during downloads.
 *
 * It does **not** download anything itself — [DownloadCoordinator] owns the
 * actual transfer. The service just mirrors the coordinator's [DownloadState]s
 * into a notification and stops once nothing is active.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification("Preparing…", 0))

        val coordinator = (application as LocalAiApp).container.downloadCoordinator
        job?.cancel()
        job = scope.launch {
            var sawActive = false
            coordinator.states.collect { states ->
                val active = states.entries.firstOrNull { it.value is DownloadState.Downloading }
                val verifying = states.values.any { it is DownloadState.Verifying }
                when {
                    active != null -> {
                        sawActive = true
                        val d = active.value as DownloadState.Downloading
                        val name = ModelCatalog.byId(active.key)?.displayName ?: "Model"
                        notify(buildNotification(name, d.percent))
                    }
                    verifying -> { sawActive = true; notify(buildNotification("Verifying…", 100)) }
                    // Only stop once we've actually run a download and none remain —
                    // never stop on the initial empty tick (that killed downloads
                    // as soon as the app was backgrounded).
                    sawActive -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> Unit
                }
            }
        }
        return START_STICKY
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

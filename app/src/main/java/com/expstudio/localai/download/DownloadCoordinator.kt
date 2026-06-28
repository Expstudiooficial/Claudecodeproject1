package com.expstudio.localai.download

import android.content.Context
import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.data.repo.ModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Application-scoped download manager.
 *
 * Downloads run on a long-lived [scope] tied to the process (not to any screen
 * or ViewModel), so they keep going while the user navigates around — and a
 * foreground [DownloadService] keeps the process alive while backgrounded.
 *
 * The single source of truth for progress is [states]; both the UI and the
 * service observe it.
 */
class DownloadCoordinator(
    context: Context,
    private val scope: CoroutineScope,
    private val repo: ModelRepository,
) {
    private val appContext = context.applicationContext
    private val downloader = ModelDownloadManager(appContext)

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states: StateFlow<Map<String, DownloadState>> = _states.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    /** True while at least one model is actively downloading/verifying. */
    val hasActiveDownloads: Boolean
        get() = _states.value.values.any {
            it is DownloadState.Downloading || it is DownloadState.Verifying
        }

    fun start(model: CatalogModel) {
        if (jobs[model.id]?.isActive == true) return
        // Seed an active state *before* starting the service so the foreground
        // service never sees an empty state and stop itself in a race.
        _states.update { it + (model.id to DownloadState.Downloading(0, model.fileSizeBytes, 0)) }
        // Bring up the foreground notification so the OS keeps the process (and
        // this download) alive while the app is backgrounded.
        runCatching { DownloadService.start(appContext, model.id) }
        jobs[model.id] = scope.launch {
            downloader.download(model).collect { state ->
                _states.update { it + (model.id to state) }
                if (state is DownloadState.Completed) {
                    repo.registerDownloaded(model)
                }
            }
        }
    }

    fun cancel(model: CatalogModel) {
        jobs[model.id]?.cancel()
        jobs.remove(model.id)
        _states.update { it - model.id }
    }

    fun stateFor(modelId: String): DownloadState? = _states.value[modelId]
}

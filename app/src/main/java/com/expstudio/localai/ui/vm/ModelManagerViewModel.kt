package com.expstudio.localai.ui.vm

import androidx.lifecycle.viewModelScope
import com.expstudio.localai.AppContainer
import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.download.DownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModelManagerViewModel(private val container: AppContainer) : BaseViewModel() {

    val catalog: List<CatalogModel> = container.modelRepository.catalog

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _installedIds = MutableStateFlow<Set<String>>(emptySet())
    val installedIds: StateFlow<Set<String>> = _installedIds.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    init { refreshInstalled() }

    fun refreshInstalled() {
        viewModelScope.launch { _installedIds.value = container.modelRepository.installedIds() }
    }

    fun download(model: CatalogModel) {
        if (jobs[model.id]?.isActive == true) return
        jobs[model.id] = viewModelScope.launch {
            container.modelRepository.download(model).collect { state ->
                _downloadStates.update { it + (model.id to state) }
                if (state is DownloadState.Completed) {
                    container.modelRepository.registerDownloaded(model)
                    refreshInstalled()
                }
            }
        }
    }

    fun cancelDownload(model: CatalogModel) {
        jobs[model.id]?.cancel()
        jobs.remove(model.id)
        _downloadStates.update { it - model.id }
    }

    fun delete(model: CatalogModel) {
        viewModelScope.launch {
            container.modelRepository.delete(model)
            _downloadStates.update { it - model.id }
            refreshInstalled()
        }
    }
}

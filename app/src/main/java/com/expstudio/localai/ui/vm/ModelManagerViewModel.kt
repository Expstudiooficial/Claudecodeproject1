package com.expstudio.localai.ui.vm

import androidx.lifecycle.viewModelScope
import com.expstudio.localai.AppContainer
import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.download.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelManagerViewModel(private val container: AppContainer) : BaseViewModel() {

    val catalog: List<CatalogModel> = container.modelRepository.catalog

    /** Progress is owned by the app-scoped coordinator so it survives navigation. */
    val downloadStates: StateFlow<Map<String, DownloadState>> = container.downloadCoordinator.states

    private val _installedIds = MutableStateFlow<Set<String>>(emptySet())
    val installedIds: StateFlow<Set<String>> = _installedIds.asStateFlow()

    init {
        refreshInstalled()
        // Refresh the installed set whenever a download finishes.
        viewModelScope.launch {
            container.downloadCoordinator.states.collect { states ->
                if (states.values.any { it is DownloadState.Completed }) refreshInstalled()
            }
        }
    }

    fun refreshInstalled() {
        viewModelScope.launch { _installedIds.value = container.modelRepository.installedIds() }
    }

    fun download(model: CatalogModel) = container.downloadCoordinator.start(model)

    fun cancelDownload(model: CatalogModel) = container.downloadCoordinator.cancel(model)

    fun delete(model: CatalogModel) {
        viewModelScope.launch {
            container.modelRepository.delete(model)
            refreshInstalled()
        }
    }
}

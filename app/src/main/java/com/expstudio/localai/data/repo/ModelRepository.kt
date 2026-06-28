package com.expstudio.localai.data.repo

import android.content.Context
import com.expstudio.localai.data.db.AppDatabase
import com.expstudio.localai.data.db.entities.InstalledModel
import com.expstudio.localai.data.model.CatalogModel
import com.expstudio.localai.data.model.ModelCatalog
import com.expstudio.localai.download.DownloadState
import com.expstudio.localai.download.ModelDownloadManager
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for models: bridges the static [ModelCatalog], the
 * on-disk files managed by [ModelDownloadManager], and the [InstalledModel]
 * records in Room.
 */
class ModelRepository(context: Context) {

    private val dao = AppDatabase.get(context).modelDao()
    private val downloader = ModelDownloadManager(context)

    val installedModels: Flow<List<InstalledModel>> = dao.observeAll()

    val catalog: List<CatalogModel> = ModelCatalog.models

    suspend fun installedIds(): Set<String> =
        ModelCatalog.models.filter { downloader.isDownloaded(it) }.map { it.id }.toSet()

    fun isDownloaded(model: CatalogModel) = downloader.isDownloaded(model)

    /** Streams download progress and records the model on completion. */
    fun download(model: CatalogModel): Flow<DownloadState> = downloader.download(model)

    suspend fun registerDownloaded(model: CatalogModel) {
        val file = downloader.targetFile(model)
        dao.upsert(
            InstalledModel(
                id = model.id,
                displayName = model.displayName,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                quantization = model.quantization,
                contextWindow = model.maxContextWindow,
                paramsBillions = model.paramsBillions,
            )
        )
    }

    suspend fun delete(model: CatalogModel) {
        downloader.delete(model)
        dao.deleteById(model.id)
    }

    suspend fun getInstalled(id: String): InstalledModel? = dao.getById(id)

    fun filePathFor(model: CatalogModel): String = downloader.targetFile(model).absolutePath

    suspend fun totalBytesOnDisk(): Long = dao.totalBytesOnDisk()
}

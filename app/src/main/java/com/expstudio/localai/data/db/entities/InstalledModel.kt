package com.expstudio.localai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A model the user has downloaded to local storage and can run.
 *
 * @param id          stable catalog id (matches CatalogModel.id) or a custom id for sideloaded files
 * @param displayName human-friendly name shown in the UI
 * @param filePath    absolute path to the .gguf file in app storage
 * @param sizeBytes   on-disk size, used by the cache/storage screens
 * @param quantization e.g. "Q4_K_M", "Q3_K_S" — surfaced in the settings UI
 * @param contextWindow maximum context the model supports (tokens)
 * @param paramsBillions parameter count in billions, used for RAM estimation
 * @param downloadedAt epoch millis the download completed
 */
@Entity(tableName = "installed_models")
data class InstalledModel(
    @PrimaryKey val id: String,
    val displayName: String,
    val filePath: String,
    val sizeBytes: Long,
    val quantization: String,
    val contextWindow: Int,
    val paramsBillions: Float,
    val downloadedAt: Long = System.currentTimeMillis(),
)

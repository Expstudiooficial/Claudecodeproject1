package com.expstudio.localai.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expstudio.localai.data.db.entities.InstalledModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM installed_models ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<InstalledModel>>

    @Query("SELECT * FROM installed_models WHERE id = :id")
    suspend fun getById(id: String): InstalledModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: InstalledModel)

    @Query("DELETE FROM installed_models WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM installed_models")
    suspend fun totalBytesOnDisk(): Long
}

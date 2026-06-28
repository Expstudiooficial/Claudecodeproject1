package com.expstudio.localai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A project groups related chats together (like folders for conversations).
 * Each chat optionally belongs to one project via [ChatSession.projectId].
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** index into a small palette so each project gets a distinct accent. */
    val colorIndex: Int = 0,
)

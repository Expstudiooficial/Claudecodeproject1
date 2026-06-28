package com.expstudio.localai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single conversation. Each session pins the model + sampling parameters it
 * was started with so reopening it restores the exact setup.
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val modelId: String?,
    /** optional owning project; null = loose chat shown under "Chats". */
    val projectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Sampling params snapshot (see InferenceParams)
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val repeatPenalty: Float = 1.1f,
    val contextWindow: Int = 4096,
    val maxTokens: Int = 512,
)

package com.expstudio.localai.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Role of a chat turn. Stored as a string for forward-compat. */
enum class Role { USER, ASSISTANT, SYSTEM }

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** tokens/second measured for this turn (assistant turns only), for the stats UI. */
    val tokensPerSecond: Float? = null,
)

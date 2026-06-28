package com.expstudio.localai.data.repo

import android.content.Context
import com.expstudio.localai.data.db.AppDatabase
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.Role
import com.expstudio.localai.data.model.InferenceParams
import kotlinx.coroutines.flow.Flow

/** CRUD + helpers for conversations and their messages. */
class ChatRepository(context: Context) {

    private val dao = AppDatabase.get(context).chatDao()

    val sessions: Flow<List<ChatSession>> = dao.observeSessions()

    fun messages(sessionId: Long): Flow<List<ChatMessage>> = dao.observeMessages(sessionId)

    suspend fun createSession(
        title: String,
        modelId: String?,
        params: InferenceParams? = null,
    ): Long {
        val base = ChatSession(title = title, modelId = modelId)
        val session = params?.let {
            base.copy(
                temperature = it.temperature,
                topP = it.topP,
                repeatPenalty = it.repeatPenalty,
                contextWindow = it.contextWindow,
                maxTokens = it.maxTokens,
            )
        } ?: base
        return dao.insertSession(session)
    }

    suspend fun getSession(id: Long): ChatSession? = dao.getSession(id)

    suspend fun updateSession(session: ChatSession) = dao.updateSession(session)

    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    suspend fun addMessage(sessionId: Long, role: Role, content: String): Long {
        val id = dao.insertMessage(ChatMessage(sessionId = sessionId, role = role, content = content))
        dao.touchSession(sessionId)
        return id
    }

    suspend fun updateMessage(message: ChatMessage) = dao.updateMessage(message)

    suspend fun historyFor(sessionId: Long): List<ChatMessage> = dao.getMessages(sessionId)
}

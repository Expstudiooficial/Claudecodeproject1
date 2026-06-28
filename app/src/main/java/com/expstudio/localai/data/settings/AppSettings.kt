package com.expstudio.localai.data.settings

import com.expstudio.localai.agent.AgentMode
import com.expstudio.localai.data.model.InferenceParams

/**
 * User-controlled, app-wide preferences.
 *
 * [defaults] are the generation parameters applied to *new* chat sessions (the
 * manual counterpart to the one-tap Smart Setup). [agentEnabled] / [agentMode]
 * drive the in-chat agent that can act on the device.
 */
data class AppSettings(
    val defaults: InferenceParams = InferenceParams.DEFAULT,
    val agentEnabled: Boolean = false,
    val agentMode: AgentMode = AgentMode.ASK,
)

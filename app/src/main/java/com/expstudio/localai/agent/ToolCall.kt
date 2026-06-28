package com.expstudio.localai.agent

import kotlinx.coroutines.CompletableDeferred

/** A single tool invocation parsed out of the model's reply. */
data class ToolCall(
    val tool: AgentTool,
    val args: Map<String, String>,
    /** the original ```tool block, kept for display/debugging. */
    val raw: String,
) {
    fun arg(name: String): String? = args[name]

    /** One-line human summary, e.g. `delete_file(path=/sdcard/x.txt)`. */
    val summary: String
        get() = buildString {
            append(tool.id)
            append('(')
            append(args.entries.joinToString(", ") { (k, v) ->
                "$k=${v.take(60)}"
            })
            append(')')
        }
}

/** Outcome of running a [ToolCall]. */
data class ToolResult(
    val tool: AgentTool,
    val success: Boolean,
    val output: String,
) {
    /** Rendered into a chat (SYSTEM) message. */
    fun render(): String {
        val icon = if (success) "✅" else "⚠️"
        return "$icon ${tool.label}\n$output"
    }
}

/** A tool call awaiting the user's approve/deny decision. */
class PendingApproval(
    val call: ToolCall,
    val deferred: CompletableDeferred<Boolean>,
)

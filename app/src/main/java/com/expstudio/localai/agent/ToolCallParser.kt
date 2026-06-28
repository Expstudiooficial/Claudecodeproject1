package com.expstudio.localai.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Extracts tool calls from an assistant reply.
 *
 * The agreed format is a fenced block:
 * ```tool
 * {"tool":"list_files","args":{"path":"."}}
 * ```
 * Multiple blocks may appear; each becomes one [ToolCall]. Anything malformed is
 * skipped silently so a chatty model never crashes the agent loop.
 */
object ToolCallParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val blockRegex = Regex(
        "```tool\\s*(\\{.*?})\\s*```",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )

    fun parse(text: String): List<ToolCall> =
        blockRegex.findAll(text).mapNotNull { match ->
            runCatching {
                val obj = json.parseToJsonElement(match.groupValues[1]).jsonObject
                val toolId = obj["tool"]?.jsonPrimitive?.content ?: return@runCatching null
                val tool = AgentTool.fromId(toolId) ?: return@runCatching null
                val args = (obj["args"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }
                    ?: emptyMap()
                ToolCall(tool = tool, args = args, raw = match.value)
            }.getOrNull()
        }.toList()

    /** True if the text contains at least one well-formed tool call. */
    fun hasToolCalls(text: String): Boolean = parse(text).isNotEmpty()
}

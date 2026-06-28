package com.expstudio.localai.agent

/**
 * How much autonomy the in-chat agent has when it wants to act on the device.
 *
 * The agent can read/write/list/delete files, inspect the device, list and open
 * apps and run shell commands in the app sandbox. Each [AgentMode] decides when
 * the user is asked to approve those actions.
 */
enum class AgentMode(
    val label: String,
    val description: String,
) {
    /** Every action is confirmed by the user before it runs. Safest. */
    ASK(
        label = "Ask",
        description = "Confirm every action before it runs. Safest — nothing happens without your tap.",
    ),

    /** Most actions run automatically; only destructive ones (delete, run command) ask. */
    ACCEPT(
        label = "Accept edits",
        description = "Read, write and open run automatically. Deleting files and running terminal " +
            "commands still ask for permission.",
    ),

    /** The agent acts fully autonomously. Most powerful, least safe. */
    AUTO(
        label = "Auto",
        description = "Full autonomy — the agent does everything it decides to, no prompts. " +
            "Only enable if you trust the model and the task.",
    );

    /** Whether [tool] must be confirmed by the user under this mode. */
    fun requiresApproval(tool: AgentTool): Boolean = when (this) {
        ASK -> true
        ACCEPT -> tool.destructive
        AUTO -> false
    }
}

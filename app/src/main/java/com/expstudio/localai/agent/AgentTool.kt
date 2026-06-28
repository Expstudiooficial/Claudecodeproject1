package com.expstudio.localai.agent

/**
 * The catalogue of actions the agent can take on the device.
 *
 * [destructive] tools require explicit user approval even in [AgentMode.ACCEPT]
 * (only [AgentMode.AUTO] runs them without asking). [needsFileAccess] tools may
 * touch files outside the app sandbox and so depend on the all-files-access
 * permission for absolute paths.
 */
enum class AgentTool(
    val id: String,
    val label: String,
    val description: String,
    val destructive: Boolean = false,
    val needsFileAccess: Boolean = false,
) {
    DEVICE_INFO("device_info", "Read device info", "Report RAM, CPU, model and storage."),
    LIST_FILES("list_files", "List files", "List a folder's contents.", needsFileAccess = true),
    READ_FILE("read_file", "Read file", "Read a text file's contents.", needsFileAccess = true),
    WRITE_FILE("write_file", "Write file", "Create or overwrite a text file.", needsFileAccess = true),
    MAKE_DIR("make_dir", "Make folder", "Create a new folder.", needsFileAccess = true),
    DELETE_FILE(
        "delete_file", "Delete file", "Delete a file or folder.",
        destructive = true, needsFileAccess = true,
    ),
    LIST_APPS("list_apps", "List apps", "List installed applications."),
    OPEN_APP("open_app", "Open app", "Launch an app by package name."),
    RUN_COMMAND(
        "run_command", "Run command", "Run a shell command in the app sandbox.",
        destructive = true,
    );

    companion object {
        fun fromId(id: String): AgentTool? = entries.firstOrNull { it.id == id }
    }
}

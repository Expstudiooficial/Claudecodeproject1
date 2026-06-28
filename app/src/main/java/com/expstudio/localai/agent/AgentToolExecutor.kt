package com.expstudio.localai.agent

import android.content.Context
import android.content.Intent
import com.expstudio.localai.smart.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Executes [ToolCall]s against the real device.
 *
 * File operations resolve relative paths against the app's private files dir
 * (always allowed). Absolute paths outside the sandbox require all-files access
 * and fail with a clear message otherwise — honest about Android's limits rather
 * than pretending. Some asks (force-closing other apps) genuinely need
 * accessibility/root and report that plainly.
 */
class AgentToolExecutor(context: Context) {

    private val appContext = context.applicationContext

    suspend fun execute(call: ToolCall): ToolResult = withContext(Dispatchers.IO) {
        runCatching {
            when (call.tool) {
                AgentTool.DEVICE_INFO -> deviceInfo()
                AgentTool.LIST_FILES -> listFiles(call.arg("path") ?: ".")
                AgentTool.READ_FILE -> readFile(call.arg("path"))
                AgentTool.WRITE_FILE -> writeFile(call.arg("path"), call.arg("content").orEmpty())
                AgentTool.MAKE_DIR -> makeDir(call.arg("path"))
                AgentTool.DELETE_FILE -> deleteFile(call.arg("path"))
                AgentTool.LIST_APPS -> listApps()
                AgentTool.OPEN_APP -> openApp(call.arg("package"))
                AgentTool.RUN_COMMAND -> runCommand(call.arg("command"))
            }
        }.getOrElse { t ->
            ToolResult(call.tool, success = false, output = "Error: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    // ---------------------------------------------------------------- files ---

    /** Resolves a path: relative → app sandbox; absolute → as-is (perm-gated). */
    private fun resolve(path: String): File =
        if (path.startsWith("/")) File(path) else File(appContext.filesDir, path)

    private fun isInsideSandbox(file: File): Boolean {
        val roots = listOfNotNull(
            appContext.filesDir,
            appContext.cacheDir,
            appContext.getExternalFilesDir(null),
        ).map { it.canonicalPath }
        val target = file.canonicalPath
        return roots.any { target == it || target.startsWith("$it/") }
    }

    private fun guardAccess(file: File): String? {
        if (isInsideSandbox(file)) return null
        if (!AgentPermissions.hasAllFilesAccess()) {
            return "No access to ${file.path}. Grant “All files access” in Settings to let the " +
                "agent work outside the app sandbox."
        }
        return null
    }

    private fun deviceInfo(): ToolResult {
        val d = DeviceProfile.snapshot(appContext)
        val files = appContext.filesDir
        val freeMb = files.freeSpace / (1024 * 1024)
        val text = buildString {
            appendLine("Device: ${d.deviceModel}")
            appendLine("CPU cores: ${d.cpuCores}  ABI: ${d.primaryAbi}")
            appendLine("RAM: ${d.availableRamMb} MB free / ${d.totalRamMb} MB total")
            appendLine("Low-RAM device: ${if (d.isLowRamDevice) "yes" else "no"}")
            append("App storage free: $freeMb MB")
        }
        return ToolResult(AgentTool.DEVICE_INFO, true, text)
    }

    private fun listFiles(path: String): ToolResult {
        val dir = resolve(path)
        guardAccess(dir)?.let { return ToolResult(AgentTool.LIST_FILES, false, it) }
        if (!dir.exists()) return ToolResult(AgentTool.LIST_FILES, false, "Not found: ${dir.path}")
        if (!dir.isDirectory) return ToolResult(AgentTool.LIST_FILES, false, "Not a folder: ${dir.path}")
        val entries = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
        val text = if (entries.isEmpty()) {
            "(empty) ${dir.path}"
        } else {
            "${dir.path}\n" + entries.joinToString("\n") { f ->
                val tag = if (f.isDirectory) "DIR " else "FILE"
                "  $tag  ${f.name}" + if (f.isFile) " (${f.length()} B)" else ""
            }
        }
        return ToolResult(AgentTool.LIST_FILES, true, text)
    }

    private fun readFile(path: String?): ToolResult {
        path ?: return ToolResult(AgentTool.READ_FILE, false, "Missing 'path'")
        val file = resolve(path)
        guardAccess(file)?.let { return ToolResult(AgentTool.READ_FILE, false, it) }
        if (!file.exists() || !file.isFile) {
            return ToolResult(AgentTool.READ_FILE, false, "Not found: ${file.path}")
        }
        if (file.length() > MAX_READ_BYTES) {
            return ToolResult(AgentTool.READ_FILE, false, "Too large (${file.length()} B); limit ${MAX_READ_BYTES} B")
        }
        val content = file.readText()
        return ToolResult(AgentTool.READ_FILE, true, "${file.path}:\n$content")
    }

    private fun writeFile(path: String?, content: String): ToolResult {
        path ?: return ToolResult(AgentTool.WRITE_FILE, false, "Missing 'path'")
        val file = resolve(path)
        guardAccess(file)?.let { return ToolResult(AgentTool.WRITE_FILE, false, it) }
        file.parentFile?.mkdirs()
        file.writeText(content)
        return ToolResult(AgentTool.WRITE_FILE, true, "Wrote ${content.length} chars to ${file.path}")
    }

    private fun makeDir(path: String?): ToolResult {
        path ?: return ToolResult(AgentTool.MAKE_DIR, false, "Missing 'path'")
        val dir = resolve(path)
        guardAccess(dir)?.let { return ToolResult(AgentTool.MAKE_DIR, false, it) }
        val ok = dir.mkdirs() || dir.isDirectory
        return ToolResult(AgentTool.MAKE_DIR, ok, if (ok) "Created ${dir.path}" else "Could not create ${dir.path}")
    }

    private fun deleteFile(path: String?): ToolResult {
        path ?: return ToolResult(AgentTool.DELETE_FILE, false, "Missing 'path'")
        val file = resolve(path)
        guardAccess(file)?.let { return ToolResult(AgentTool.DELETE_FILE, false, it) }
        if (!file.exists()) return ToolResult(AgentTool.DELETE_FILE, false, "Not found: ${file.path}")
        val ok = file.deleteRecursively()
        return ToolResult(AgentTool.DELETE_FILE, ok, if (ok) "Deleted ${file.path}" else "Could not delete ${file.path}")
    }

    // ----------------------------------------------------------------- apps ---

    private fun listApps(): ToolResult {
        val pm = appContext.packageManager
        val apps = pm.getInstalledApplications(0)
            .mapNotNull { info ->
                val label = pm.getApplicationLabel(info).toString()
                if (label.isBlank()) null else "$label  —  ${info.packageName}"
            }
            .sorted()
            .take(MAX_APPS)
        val text = "Installed apps (showing ${apps.size}):\n" + apps.joinToString("\n") { "  $it" }
        return ToolResult(AgentTool.LIST_APPS, true, text)
    }

    private fun openApp(pkg: String?): ToolResult {
        pkg ?: return ToolResult(AgentTool.OPEN_APP, false, "Missing 'package'")
        val intent = appContext.packageManager.getLaunchIntentForPackage(pkg)
            ?: return ToolResult(AgentTool.OPEN_APP, false, "No launchable app for '$pkg'")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        return ToolResult(AgentTool.OPEN_APP, true, "Launched $pkg")
    }

    // -------------------------------------------------------------- command ---

    private fun runCommand(command: String?): ToolResult {
        command ?: return ToolResult(AgentTool.RUN_COMMAND, false, "Missing 'command'")
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        // Process.waitFor(timeout, unit) is available from API 26 (our minSdk).
        val completed = process.waitFor(COMMAND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!completed) {
            process.destroy()
            return ToolResult(AgentTool.RUN_COMMAND, false, "Timed out after $COMMAND_TIMEOUT_MS ms\n$output")
        }
        val exit = process.exitValue()
        val trimmed = output.take(MAX_OUTPUT_CHARS).ifBlank { "(no output)" }
        return ToolResult(AgentTool.RUN_COMMAND, exit == 0, "exit=$exit\n$trimmed")
    }

    private companion object {
        const val MAX_READ_BYTES = 256 * 1024
        const val MAX_OUTPUT_CHARS = 4000
        const val MAX_APPS = 60
        const val COMMAND_TIMEOUT_MS = 8000
    }
}

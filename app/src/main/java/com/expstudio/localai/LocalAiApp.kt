package com.expstudio.localai

import android.app.Application
import com.expstudio.localai.agent.AgentToolExecutor
import com.expstudio.localai.data.repo.ChatRepository
import com.expstudio.localai.data.repo.ModelRepository
import com.expstudio.localai.data.settings.SettingsStore
import com.expstudio.localai.download.DownloadCoordinator
import com.expstudio.localai.inference.LlamaInferenceEngine
import com.expstudio.localai.smart.SmartConfiguratorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight manual DI container. Avoids pulling in Hilt for a small app while
 * still giving ViewModels a single place to obtain shared singletons.
 */
class LocalAiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: LocalAiApp) {
    val appContext = app.applicationContext
    /** Long-lived scope for work that should outlive any single screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val modelRepository = ModelRepository(app)
    val chatRepository = ChatRepository(app)
    val inferenceEngine = LlamaInferenceEngine()
    val smartConfigurator = SmartConfiguratorEngine()
    val settingsStore = SettingsStore(app)
    val agentToolExecutor = AgentToolExecutor(app)
    val downloadCoordinator = DownloadCoordinator(app, appScope, modelRepository)
}

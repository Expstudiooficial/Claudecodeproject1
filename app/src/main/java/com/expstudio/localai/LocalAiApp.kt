package com.expstudio.localai

import android.app.Application
import com.expstudio.localai.data.repo.ChatRepository
import com.expstudio.localai.data.repo.ModelRepository
import com.expstudio.localai.inference.LlamaInferenceEngine
import com.expstudio.localai.smart.SmartConfiguratorEngine

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
    val modelRepository = ModelRepository(app)
    val chatRepository = ChatRepository(app)
    val inferenceEngine = LlamaInferenceEngine()
    val smartConfigurator = SmartConfiguratorEngine()
    val appContext = app.applicationContext
}

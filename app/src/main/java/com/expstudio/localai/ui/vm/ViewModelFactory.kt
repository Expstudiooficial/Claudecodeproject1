package com.expstudio.localai.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.expstudio.localai.LocalAiApp

/** Shorthand to read the [LocalAiApp] container inside a ViewModel initializer. */
private fun CreationExtras.app(): LocalAiApp =
    (this[APPLICATION_KEY] as LocalAiApp)

/** Central factory wiring our manual-DI container into ViewModels. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { ChatViewModel(app().container) }
    initializer { ModelManagerViewModel(app().container) }
    initializer { SettingsViewModel(app().container) }
}

/** Marker base to keep ViewModels uniform. */
abstract class BaseViewModel : ViewModel()

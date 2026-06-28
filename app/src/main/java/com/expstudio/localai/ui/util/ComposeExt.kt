package com.expstudio.localai.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin alias over [collectAsState] so screens have one consistent call site.
 * (Kept simple; swap for collectAsStateWithLifecycle if stricter lifecycle
 * awareness is needed.)
 */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState()

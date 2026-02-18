package com.docvaultbasic.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.docvaultbasic.security.PinManager
import com.docvaultbasic.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _startDestination = mutableStateOf(getInitialRoute())
    val startDestination: State<String> = _startDestination

    private fun getInitialRoute(): String {
        return if (!pinManager.isOnboardingComplete()) {
            Screen.Onboarding.route
        } else if (!pinManager.isPinSet()) {
            Screen.PinSetup.route
        } else {
            Screen.Lock.route
        }
    }
}

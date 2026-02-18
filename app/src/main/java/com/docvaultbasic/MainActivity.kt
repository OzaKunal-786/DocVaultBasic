package com.docvaultbasic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import com.docvaultbasic.ui.screens.MainScreen
import com.docvaultbasic.ui.theme.DocVaultBasicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides this) {
                DocVaultBasicTheme {
                    MainScreen()
                }
            }
        }
    }
}

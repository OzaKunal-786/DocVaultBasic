package com.docvaultbasic.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.docvaultbasic.security.BiometricHelper
import com.docvaultbasic.security.PinManager
import com.docvaultbasic.ui.navigation.Screen

@Composable
fun LockScreen(navController: NavController) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    val biometricHelper = remember { BiometricHelper(context) }

    val onSuccessfulUnlock = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Lock.route) { inclusive = true }
        }
    }

    val triggerBiometrics = {
        if (context is FragmentActivity && pinManager.isBiometricEnabled()) {
            biometricHelper.showBiometricPrompt(
                activity = context,
                onSuccess = { onSuccessfulUnlock() },
                onError = { errorMessage = it }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (pinManager.isBiometricEnabled()) {
            val canAuth = biometricHelper.canAuthenticate()
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                triggerBiometrics()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DocVault is Locked",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { 
                if (it.length <= 6) {
                    pin = it
                    errorMessage = null
                }
            },
            label = { Text("Enter PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pinManager.verifyPin(pin)) {
                    onSuccessfulUnlock()
                } else {
                    errorMessage = "Incorrect PIN"
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Unlock")
        }

        if (pinManager.isBiometricEnabled() && biometricHelper.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { triggerBiometrics() }) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}

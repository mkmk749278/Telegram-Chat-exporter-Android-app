package com.example.tgexporter.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tgexporter.telegram.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel, onReady: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val err by viewModel.errorMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is AuthState.Ready) onReady()
    }
    LaunchedEffect(err) {
        err?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sign in") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is AuthState.Initializing -> {
                    CircularProgressIndicator()
                    Text("Connecting to Telegram…")
                }
                is AuthState.WaitPhone -> PhoneInput(busy) { viewModel.submitPhone(it) }
                is AuthState.WaitCode -> CodeInput(busy) { viewModel.submitCode(it) }
                is AuthState.WaitPassword -> PasswordInput(busy) { viewModel.submitPassword(it) }
                is AuthState.Ready -> Text("Signed in.")
                is AuthState.LoggingOut -> Text("Logging out…")
                is AuthState.Closed -> Text("Session closed. Restart the app.")
                is AuthState.Error -> Text((state as AuthState.Error).message)
            }
        }
    }
}

@Composable
private fun PhoneInput(busy: Boolean, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Text("Enter your phone number with country code (e.g. +14155551234).")
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text("Phone number") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.9f),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        enabled = !busy && value.isNotBlank(),
        onClick = { onSubmit(value) },
    ) { Text("Send code") }
}

@Composable
private fun CodeInput(busy: Boolean, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Text("Enter the code Telegram just sent you.")
    OutlinedTextField(
        value = value,
        onValueChange = { value = it.filter { ch -> ch.isDigit() } },
        label = { Text("Login code") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.6f),
    )
    Button(
        enabled = !busy && value.length >= 4,
        onClick = { onSubmit(value) },
    ) { Text("Verify") }
}

@Composable
private fun PasswordInput(busy: Boolean, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Text("Enter your two-step verification password.")
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.9f),
    )
    Button(
        enabled = !busy && value.isNotBlank(),
        onClick = { onSubmit(value) },
    ) { Text("Continue") }
}


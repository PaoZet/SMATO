package com.example.smato.ui.screens

import android.accounts.AccountManager
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.smato.viewmodel.MedicionViewModel

// Pantalla de autenticación y registro de usuarios que integra selección de cuenta de Google,
// validación de credenciales y un segundo factor de autenticación (2FA) mediante biometría (huella digital).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MedicionViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    // Estados locales para el formulario de acceso y registro.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isRegistering by remember { mutableStateOf(false) } // false = Inicio de sesión, true = Registro de cuenta
    var pasoBiometria by remember { mutableStateOf(false) }   // false = Credenciales, true = Escaneo biométrico
    var errorMessage by remember { mutableStateOf("") }

    val TurquesaCustom = Color(0xFF00BCD4)
    val VerdeExito = Color(0xFF2E7D32)
    val RojoError = Color(0xFFE53935)

    // Lanzador de actividad para interactuar con el selector nativo de cuentas de Google del dispositivo.
    val googleAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                email = accountName
                errorMessage = ""
            }
        }
    }

    // Función encargada de desplegar el prompt del sistema para la autenticación biométrica (Segundo Factor).
    fun lanzarBiometria() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            errorMessage = "Error de contexto para biometría"
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Autenticación biométrica exitosa -> Concede acceso total a la aplicación.
                    onLoginSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    errorMessage = "Error de autenticación: $errString"
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    errorMessage = "Huella no reconocida. Inténtalo de nuevo."
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Segundo Factor - SMATO")
            .setSubtitle("Usa tu huella digital para confirmar tu acceso")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Función de validación de formato de correo electrónico mediante patrones estándar.
    fun esCorreoValido(target: CharSequence): Boolean {
        return target.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMATO - Acceso Seguro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título dinámico según el estado actual de la vista (Login, Registro o Biometría).
                    Text(
                        text = when {
                            pasoBiometria -> "Verificación por Huella"
                            isRegistering -> "Registro de Nueva Cuenta"
                            else -> "Iniciar Sesión"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = TurquesaCustom
                    )

                    if (!pasoBiometria) {
                        // Botón para seleccionar de forma rápida una cuenta de Google vinculada al teléfono.
                        Button(
                            onClick = {
                                try {
                                    val intent = AccountManager.newChooseAccountIntent(
                                        null, null, arrayOf("com.google"),
                                        false, null, null, null, null
                                    )
                                    googleAccountLauncher.launch(intent)
                                } catch (e: Exception) {
                                    errorMessage = "No se pudieron obtener las cuentas de Google"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Text("Seleccionar cuenta de Google", color = Color.DarkGray)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Campo de texto para el correo electrónico del usuario.
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo Electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = email.isNotBlank() && !esCorreoValido(email)
                        )

                        // Campo de texto para la contraseña.
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Campo adicional visible únicamente durante el flujo de registro de cuenta.
                        if (isRegistering) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirmar Contraseña") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Botón principal que procesa el inicio de sesión o el registro según corresponda.
                        Button(
                            onClick = {
                                if (!esCorreoValido(email)) {
                                    errorMessage = "Por favor selecciona o ingresa un correo válido"
                                    return@Button
                                }
                                if (password.isBlank()) {
                                    errorMessage = "La contraseña no puede estar vacía"
                                    return@Button
                                }

                                if (isRegistering) {
                                    // --- FLUJO DE REGISTRO ---
                                    if (password != confirmPassword) {
                                        errorMessage = "Las contraseñas no coinciden"
                                        return@Button
                                    }
                                    viewModel.registrarNuevoUsuario(email, password) { exito, errorMsg ->
                                        if (exito) {
                                            isRegistering = false
                                            password = ""
                                            confirmPassword = ""
                                            errorMessage = "¡Registro exitoso! Inicia sesión con tus datos."
                                        } else {
                                            errorMessage = errorMsg
                                        }
                                    }
                                } else {
                                    // --- FLUJO DE INICIO DE SESIÓN ---
                                    viewModel.verificarCredenciales(email, password) { exito, errorMsg ->
                                        if (exito) {
                                            // Credenciales correctas -> Avanza al segundo factor de autenticación (Huella).
                                            errorMessage = ""
                                            pasoBiometria = true
                                        } else {
                                            errorMessage = errorMsg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                        ) {
                            Text(if (isRegistering) "Registrarse" else "Iniciar Sesión", color = Color.White)
                        }

                        // Botón alternador para cambiar entre la vista de acceso y la de registro.
                        TextButton(
                            onClick = {
                                isRegistering = !isRegistering
                                errorMessage = ""
                                password = ""
                                confirmPassword = ""
                            }
                        ) {
                            Text(
                                text = if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                    } else {
                        // --- PASO 2: VERIFICACIÓN POR HUELLA DIGITAL (2FA) ---
                        Text(
                            text = "Credenciales verificadas. Usa tu huella digital para completar el acceso.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { lanzarBiometria() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdeExito)
                        ) {
                            Text("Escanear Huella Digital", color = Color.White)
                        }

                        TextButton(onClick = {
                            pasoBiometria = false
                            password = ""
                        }) {
                            Text("← Regresar", color = Color.Gray)
                        }
                    }

                    // Mensaje de retroalimentación en caso de errores o avisos de éxito.
                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = if (errorMessage.contains("exitoso")) VerdeExito else RojoError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
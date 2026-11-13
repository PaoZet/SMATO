package com.example.smato

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.example.smato.ui.screens.CatalogosScreen
import com.example.smato.ui.screens.DashboardScreen
import com.example.smato.ui.screens.HistorialScreen
import com.example.smato.ui.screens.HomeScreen
import com.example.smato.ui.screens.LoginScreen
import com.example.smato.ui.theme.SMATOTheme
import com.example.smato.viewmodel.MedicionViewModel

// Actividad principal que hospeda la interfaz de usuario basada en Jetpack Compose.
// Hereda de FragmentActivity para garantizar compatibilidad con componentes modernos y de autenticación.
class MainActivity : FragmentActivity() {

    // Instancia del ViewModel compartido para la gestión de estados y operaciones de datos.
    private val viewModel: MedicionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificamos la versión del sistema para solicitar permisos de notificaciones
        // en dispositivos con Android 13 (Tiramisu) o superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        // Establecemos el contenido visual utilizando Jetpack Compose bajo el tema personalizado de la app.
        setContent {
            SMATOTheme {
                // Estado local para controlar la navegación entre las diferentes pantallas de la aplicación.
                var pantallaActual by remember { mutableStateOf("login") }

                // Enrutador de pantallas basado en el estado actual.
                when (pantallaActual) {
                    "login" -> LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { pantallaActual = "home" }
                    )
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToCatalogos = { pantallaActual = "catalogos" },
                        onNavigateToHistorial = { pantallaActual = "historial" },
                        onNavigateToDashboard = { pantallaActual = "dashboard" },
                        onNavigateToLogin = { pantallaActual = "login" } // Permite cerrar sesión y volver al login
                    )
                    "catalogos" -> CatalogosScreen(
                        viewModel = viewModel,
                        onBack = { pantallaActual = "home" }
                    )
                    "historial" -> HistorialScreen(
                        viewModel = viewModel,
                        onNavigateBack = { pantallaActual = "home" }
                    )
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { pantallaActual = "home" }
                    )
                }
            }
        }
    }
}
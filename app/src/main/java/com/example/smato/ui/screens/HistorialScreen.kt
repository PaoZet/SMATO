package com.example.smato.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.smato.viewmodel.MedicionViewModel
import java.io.File
import com.example.smato.data.local.entities.SesionMedicion

// Pantalla del historial de mediciones que permite visualizar las sesiones guardadas,
// gestionar su eliminación individual o global, y exportarlas en paquetes ZIP cifrados con AES-256.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: MedicionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    // Recolectamos el historial completo de sesiones almacenadas de forma reactiva.
    val sesiones by viewModel.historialSesiones.collectAsState(initial = emptyList())

    // Estados de control para los diálogos de confirmación y eliminación.
    var sesionParaBorrarTodo by remember { mutableStateOf(false) }
    var sesionAEliminar by remember { mutableStateOf<SesionMedicion?>(null) }

    // Estados para la gestión del diálogo de contraseña y el tipo de exportación ("EXPORTAR_DATOS" o "EXPORTAR_EVIDENCIAS").
    var mostrarDialogoPassword by remember { mutableStateOf(false) }
    var passwordIngresado by remember { mutableStateOf("") }
    var mensajeErrorPassword by remember { mutableStateOf("") }
    var tipoExportacionActual by remember { mutableStateOf("") }

    val TurquesaExport = Color(0xFF00BCD4)
    val VioletaEvidencias = Color(0xFF673AB7)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Mediciones") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Volver", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                actions = {
                    // Botón en la barra superior para eliminar todo el historial de la aplicación.
                    IconButton(onClick = { sesionParaBorrarTodo = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar todo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
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
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Apartado superior con botones independientes para exportar datos y evidencias en formato cifrado.
                if (sesiones.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                tipoExportacionActual = "EXPORTAR_DATOS"
                                passwordIngresado = ""
                                mensajeErrorPassword = ""
                                mostrarDialogoPassword = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquesaExport)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Datos", color = Color.White)
                        }

                        Button(
                            onClick = {
                                tipoExportacionActual = "EXPORTAR_EVIDENCIAS"
                                passwordIngresado = ""
                                mensajeErrorPassword = ""
                                mostrarDialogoPassword = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VioletaEvidencias)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Evidencias", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Vista condicional en caso de que el historial se encuentre vacío.
                if (sesiones.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay sesiones guardadas aún.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    // Lista desplazable con el detalle de las sesiones de medición registradas.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sesiones) { sesion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Proceso: ${sesion.proceso} > ${sesion.subproceso}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Actividad: ${sesion.actividad}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Persona: ${sesion.personaMedida} | Área: ${sesion.areaMedida}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { sesionAEliminar = sesion }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar Sesión",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Diálogo unificado para solicitar la contraseña y aplicar cifrado AES-256 al archivo ZIP resultante.
            if (mostrarDialogoPassword) {
                AlertDialog(
                    onDismissRequest = {
                        mostrarDialogoPassword = false
                    },
                    title = { Text("Proteger Exportación con AES-256") },
                    text = {
                        Column {
                            Text("Ingresa una contraseña para cifrar el archivo ZIP resultante. La necesitarás para abrirlo en la computadora.")
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = passwordIngresado,
                                onValueChange = {
                                    passwordIngresado = it
                                    mensajeErrorPassword = ""
                                },
                                label = { Text("Contraseña de seguridad") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            if (mensajeErrorPassword.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mensajeErrorPassword,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (passwordIngresado.isBlank()) {
                                    mensajeErrorPassword = "La contraseña no puede estar vacía"
                                } else {
                                    mostrarDialogoPassword = false

                                    // Callback que maneja el resultado de la exportación y genera el Intent para compartir el archivo.
                                    val callbackResultado: (Boolean, String) -> Unit = { exito, rutaArchivo ->
                                        if (exito) {
                                            try {
                                                val file = File(rutaArchivo)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/zip"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Compartir ZIP Cifrado"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Guardado en: $rutaArchivo", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, rutaArchivo, Toast.LENGTH_LONG).show()
                                        }
                                    }

                                    // Ejecuta la función de exportación correspondiente según la opción seleccionada.
                                    when (tipoExportacionActual) {
                                        "EXPORTAR_DATOS" -> {
                                            viewModel.exportarTodoHistorialExcelCifrado(context, passwordIngresado, callbackResultado)
                                        }
                                        "EXPORTAR_EVIDENCIAS" -> {
                                            viewModel.exportarSoloEvidenciasCifrado(context, passwordIngresado, callbackResultado)
                                        }
                                    }

                                    passwordIngresado = ""
                                }
                            }
                        ) {
                            Text("Cifrar y Compartir")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogoPassword = false
                                passwordIngresado = ""
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo de confirmación para eliminar una sesión de medición específica.
            if (sesionAEliminar != null) {
                AlertDialog(
                    onDismissRequest = { sesionAEliminar = null },
                    title = { Text("Confirmar eliminación") },
                    text = { Text("¿Estás segura de querer borrar esta medición del historial? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        TextButton(onClick = {
                            sesionAEliminar?.let { viewModel.eliminarSesion(it.id) }
                            sesionAEliminar = null
                        }) {
                            Text("Sí, borrar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { sesionAEliminar = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo de advertencia para borrar absolutamente todo el historial de la aplicación.
            if (sesionParaBorrarTodo) {
                AlertDialog(
                    onDismissRequest = { sesionParaBorrarTodo = false },
                    title = { Text("⚠️ Borrar todo el historial?") },
                    text = { Text("Estás a punto de eliminar todas las sesiones y tareas registradas. ¿Estás totalmente segura?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.eliminarTodoHistorial()
                            sesionParaBorrarTodo = false
                        }) {
                            Text("Sí, borrar todo", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { sesionParaBorrarTodo = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
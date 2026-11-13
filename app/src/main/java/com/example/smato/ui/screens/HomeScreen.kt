package com.example.smato.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smato.viewmodel.MedicionViewModel
import kotlinx.coroutines.delay
import java.io.File

// Pantalla principal (Home) de la aplicación SMATO que permite configurar los metadatos de la sesión,
// controlar el cronómetro general, registrar tareas detalladas, adjuntar evidencias multimedia y acceder al menú de navegación.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MedicionViewModel,
    onNavigateToHistorial: () -> Unit,
    onNavigateToCatalogos: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current

    // Estados locales para los campos de estructura de la sesión de medición.
    var proceso by remember { mutableStateOf("") }
    var subproceso by remember { mutableStateOf("") }
    var actividad by remember { mutableStateOf("") }
    var personaMedida by remember { mutableStateOf("") }
    var areaMedida by remember { mutableStateOf("") }
    var agencia by remember { mutableStateOf("") }

    // Estados de control para el cronómetro general de la sesión.
    var tiempoMilisegundos by remember { mutableStateOf(0L) }
    var cronometroCorriendo by remember { mutableStateOf(false) }

    // Efecto lanzado que incrementa el tiempo del cronómetro cada 100 milisegundos cuando se encuentra activo.
    LaunchedEffect(cronometroCorriendo) {
        while (cronometroCorriendo) {
            delay(100L)
            tiempoMilisegundos += 100L
        }
    }

    // Estados locales para el registro de la tarea actual.
    var nombreTarea by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var tipoTarea by remember { mutableStateOf("Manual") }
    var campos by remember { mutableStateOf(0) }
    var hojas by remember { mutableStateOf(0) }
    var clicks by remember { mutableStateOf(0) }
    var pantallas by remember { mutableStateOf(0) }

    // Estado para controlar la visibilidad del menú desplegable superior.
    var menuExpandido by remember { mutableStateOf(false) }

    // Recolección de flujos reactivos desde el ViewModel (catálogos y elementos temporales).
    val areas by viewModel.areasCompletas.collectAsState(initial = emptyList())
    val personas by viewModel.personasCompletas.collectAsState(initial = emptyList())
    val tareasTemporales by viewModel.tareasTemporales.collectAsState(initial = emptyList())
    val evidenciasTemporales by viewModel.evidenciasTemporales.collectAsState(initial = emptyList())

    // Estados de expansión para los menús desplegables de catálogos y diálogos.
    var personaExpanded by remember { mutableStateOf(false) }
    var areaExpanded by remember { mutableStateOf(false) }
    var mostrarSnackbar by remember { mutableStateOf(false) }
    var mensajeSnackbar by remember { mutableStateOf("") }
    var mostrarDialogoEvidencia by remember { mutableStateOf(false) }

    // Archivos temporales para el almacenamiento de fotos y videos capturados.
    var tempFotoFile by remember { mutableStateOf<File?>(null) }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }

    // Variable auxiliar para diferir la acción de la cámara tras la concesión de permisos.
    var pendienteAccionCamara by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Lanzador para solicitar permisos de cámara en tiempo de ejecución.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendienteAccionCamara?.invoke()
        } else {
            mensajeSnackbar = "Permiso de cámara denegado"
            mostrarSnackbar = true
        }
        pendienteAccionCamara = null
    }

    // Lanzador de actividad para tomar fotografías mediante la cámara del dispositivo.
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFotoFile != null) {
            viewModel.agregarEvidenciaTemporal("FOTO", tempFotoFile!!.absolutePath)
            mensajeSnackbar = "Foto capturada con éxito"
            mostrarSnackbar = true
        }
    }

    // Lanzador de actividad para grabar videos mediante la cámara del dispositivo.
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoFile != null) {
            viewModel.agregarEvidenciaTemporal("VIDEO", tempVideoFile!!.absolutePath)
            mensajeSnackbar = "Video grabado con éxito"
            mostrarSnackbar = true
        }
    }

    // Lanzador de actividad para la grabación directa de audio.
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val sourceUri = result.data?.data
            if (sourceUri != null) {
                try {
                    val audioFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.mp3")
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        audioFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.agregarEvidenciaTemporal("AUDIO", audioFile.absolutePath)
                    mensajeSnackbar = "Audio grabado con éxito"
                    mostrarSnackbar = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val TurquesaCustom = Color(0xFF00BCD4)
    val RojoFinalizar = Color(0xFFE53935)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMATO - Control de Procesos") },
                actions = {
                    IconButton(onClick = { menuExpandido = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dashboard") },
                            onClick = {
                                menuExpandido = false
                                onNavigateToDashboard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Catálogos") },
                            onClick = {
                                menuExpandido = false
                                onNavigateToCatalogos()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Historial de Mediciones") },
                            onClick = {
                                menuExpandido = false
                                onNavigateToHistorial()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión", color = RojoFinalizar) },
                            onClick = {
                                menuExpandido = false
                                onNavigateToLogin()
                            }
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- ESTRUCTURA DEL PROCESO ---
                item {
                    Text("Estructura del Proceso", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = proceso,
                        onValueChange = { proceso = it },
                        label = { Text("Proceso") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = subproceso,
                        onValueChange = { subproceso = it },
                        label = { Text("Subproceso") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = actividad,
                        onValueChange = { actividad = it },
                        label = { Text("Actividad") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector desplegable para la persona medida basado en el catálogo.
                    ExposedDropdownMenuBox(
                        expanded = personaExpanded,
                        onExpandedChange = { personaExpanded = !personaExpanded }
                    ) {
                        OutlinedTextField(
                            value = personaMedida,
                            onValueChange = { personaMedida = it },
                            label = { Text("Persona medida") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personaExpanded) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = personaExpanded,
                            onDismissRequest = { personaExpanded = false }
                        ) {
                            personas.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.nombre) },
                                    onClick = {
                                        personaMedida = p.nombre
                                        personaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector desplegable para el área medida basado en el catálogo.
                    ExposedDropdownMenuBox(
                        expanded = areaExpanded,
                        onExpandedChange = { areaExpanded = !areaExpanded }
                    ) {
                        OutlinedTextField(
                            value = areaMedida,
                            onValueChange = { areaMedida = it },
                            label = { Text("Área medida") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = areaExpanded,
                            onDismissRequest = { areaExpanded = false }
                        ) {
                            areas.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.nombre) },
                                    onClick = {
                                        areaMedida = a.nombre
                                        areaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = agencia,
                        onValueChange = { agencia = it },
                        label = { Text("Agencia (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // --- CRONÓMETRO GENERAL ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Cronómetro General", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val segundosTotales = tiempoMilisegundos / 1000
                    val minutos = segundosTotales / 60
                    val segundos = segundosTotales % 60
                    val decimas = (tiempoMilisegundos % 1000) / 100
                    val tiempoFormateado = String.format("%02d:%02d.%d", minutos, segundos, decimas)

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tiempoFormateado,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { cronometroCorriendo = !cronometroCorriendo },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cronometroCorriendo) Color.DarkGray else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (cronometroCorriendo) "Pausar" else "Iniciar")
                        }

                        Button(
                            onClick = {
                                if (proceso.isNotBlank() && actividad.isNotBlank()) {
                                    viewModel.guardarSesionCompleta(
                                        proceso = proceso, subproceso = subproceso,
                                        actividad = actividad, persona = personaMedida,
                                        area = areaMedida, agencia = agencia
                                    ) { _ ->
                                        cronometroCorriendo = false
                                        tiempoMilisegundos = 0L
                                        proceso = ""
                                        subproceso = ""
                                        actividad = ""
                                        personaMedida = ""
                                        areaMedida = ""
                                        agencia = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RojoFinalizar),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finalizar y guardar")
                        }
                    }
                }

                // --- REGISTRO DE TAREAS ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Registro de Tareas (Acumuladas: ${tareasTemporales.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombreTarea,
                        onValueChange = { nombreTarea = it },
                        label = { Text("Nombre de la Tarea") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = { Text("Comentario / Observación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Tipo de Tarea", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tipoTarea == "Manual",
                                onClick = { tipoTarea = "Manual" }
                            )
                            Text("Manual")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tipoTarea == "Digital",
                                onClick = { tipoTarea = "Digital" }
                            )
                            Text("Digital")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tipoTarea == "Manual") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Campos: $campos")
                                Button(
                                    onClick = { campos++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                                ) { Text("+1") }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hojas: $hojas")
                                Button(
                                    onClick = { hojas++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                                ) { Text("+1") }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Clicks: $clicks")
                                Button(
                                    onClick = { clicks++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                                ) { Text("+1") }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pantallas: $pantallas")
                                Button(
                                    onClick = { pantallas++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                                ) { Text("+1") }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nombreTarea.isNotBlank()) {
                                viewModel.agregarTareaConTiempo(
                                    tarea = nombreTarea, comentario = comentario,
                                    clicks = clicks, pantallas = pantallas,
                                    camposLlenados = campos, hojasLlenadas = hojas,
                                    tiempoMilisegundos = tiempoMilisegundos
                                )
                                nombreTarea = ""
                                comentario = ""
                                campos = 0
                                hojas = 0
                                clicks = 0
                                pantallas = 0
                                mensajeSnackbar = "Tarea registrada con éxito"
                                mostrarSnackbar = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Tarea Actual", color = Color.White)
                    }
                }

                // --- EVIDENCIAS MULTIMEDIA ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Evidencias Multimedia (Acumuladas: ${evidenciasTemporales.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { mostrarDialogoEvidencia = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Añadir Evidencia (Foto / Video / Audio)", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (evidenciasTemporales.isNotEmpty()) {
                        evidenciasTemporales.forEachIndexed { index, evidencia ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• [${evidencia.first}] ${File(evidencia.second).name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.eliminarEvidenciaTemporal(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Diálogo de selección para el tipo de evidencia multimedia a adjuntar.
            if (mostrarDialogoEvidencia) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoEvidencia = false },
                    title = { Text("Seleccionar tipo de evidencia") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    mostrarDialogoEvidencia = false
                                    val permissionCheck = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    )

                                    val accionTomarFoto = {
                                        try {
                                            tempFotoFile = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFotoFile!!)
                                            takePictureLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            mensajeSnackbar = "Error al preparar archivo de foto"
                                            mostrarSnackbar = true
                                        }
                                    }

                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        accionTomarFoto()
                                    } else {
                                        pendienteAccionCamara = accionTomarFoto
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                            ) { Text("Tomar Foto con Cámara", color = Color.White) }

                            Button(
                                onClick = {
                                    mostrarDialogoEvidencia = false
                                    val permissionCheck = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    )

                                    val accionGrabarVideo = {
                                        try {
                                            tempVideoFile = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempVideoFile!!)
                                            captureVideoLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            mensajeSnackbar = "Error al preparar archivo de video"
                                            mostrarSnackbar = true
                                        }
                                    }

                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        accionGrabarVideo()
                                    } else {
                                        pendienteAccionCamara = accionGrabarVideo
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                            ) { Text("Grabar Video con Cámara", color = Color.White) }

                            Button(
                                onClick = {
                                    mostrarDialogoEvidencia = false
                                    val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                                    recordAudioLauncher.launch(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquesaCustom)
                            ) { Text("Grabar Audio Directo", color = Color.White) }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoEvidencia = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Snackbar flotante para notificaciones temporales al usuario.
            if (mostrarSnackbar) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { mostrarSnackbar = false }) {
                            Text("OK", color = TurquesaCustom)
                        }
                    }
                ) {
                    Text(mensajeSnackbar)
                }
                LaunchedEffect(Unit) {
                    delay(2000L)
                    mostrarSnackbar = false
                }
            }
        }
    }
}
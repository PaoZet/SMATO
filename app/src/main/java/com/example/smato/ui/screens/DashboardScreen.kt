package com.example.smato.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.smato.viewmodel.MedicionViewModel

// Pantalla del panel analítico (Dashboard) que muestra métricas, filtros avanzados
// y gráficas interactivas para evaluar el rendimiento y los tiempos de los procesos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MedicionViewModel,
    onNavigateBack: () -> Unit
) {
    // Recolectamos el historial de sesiones y tareas completas desde los flujos del ViewModel.
    val sesiones by viewModel.historialSesiones.collectAsState(initial = emptyList())
    val todasLasTareas by viewModel.todasLasTareas.collectAsState(initial = emptyList())

    // --- ESTADOS DE FILTROS ---
    var filtroProceso by remember { mutableStateOf("Todos") }
    var filtroSubproceso by remember { mutableStateOf("Todos") }
    var filtroActividad by remember { mutableStateOf("Todas") }
    var filtroArea by remember { mutableStateOf("Todas") }

    // Listas dinámicas para poblar los menús desplegables de filtrado basados en los datos existentes.
    val procesosDisponibles = remember(sesiones) {
        listOf("Todos") + sesiones.map { it.proceso }.distinct()
    }
    val subprocesosDisponibles = remember(sesiones, filtroProceso) {
        val lista = if (filtroProceso == "Todos") sesiones else sesiones.filter { it.proceso == filtroProceso }
        listOf("Todos") + lista.map { it.subproceso }.filter { it.isNotBlank() }.distinct()
    }
    val actividadesDisponibles = remember(sesiones, filtroProceso, filtroSubproceso) {
        val lista = sesiones.filter {
            (filtroProceso == "Todos" || it.proceso == filtroProceso) &&
                    (filtroSubproceso == "Todos" || it.subproceso == filtroSubproceso)
        }
        listOf("Todas") + lista.map { it.actividad }.filter { it.isNotBlank() }.distinct()
    }
    val areasDisponibles = remember(sesiones) {
        listOf("Todas") + sesiones.map { it.areaMedida }.filter { it.isNotBlank() }.distinct()
    }

    // Filtrado de sesiones aplicando los criterios seleccionados por el usuario.
    val sesionesFiltradas = remember(sesiones, filtroProceso, filtroSubproceso, filtroActividad, filtroArea) {
        sesiones.filter { sesion ->
            val matchProceso = filtroProceso == "Todos" || sesion.proceso == filtroProceso
            val matchSubproceso = filtroSubproceso == "Todos" || sesion.subproceso == filtroSubproceso
            val matchActividad = filtroActividad == "Todas" || sesion.actividad == filtroActividad
            val matchArea = filtroArea == "Todas" || sesion.areaMedida == filtroArea
            matchProceso && matchSubproceso && matchActividad && matchArea
        }
    }

    // Filtramos las tareas asociadas exclusivamente a las sesiones resultantes del filtro anterior.
    val idsSesionesFiltradas = sesionesFiltradas.map { it.id }.toSet()
    val tareasFiltradas = remember(todasLasTareas, idsSesionesFiltradas) {
        todasLasTareas.filter { it.sesionId in idsSesionesFiltradas }
    }

    // --- MÉTRICAS ---
    val totalSesiones = sesionesFiltradas.size
    val totalTareas = tareasFiltradas.size
    val tiempoTotalMs = tareasFiltradas.sumOf { it.tiempoMilisegundos }
    val tiempoTotalSegundos = tiempoTotalMs / 1000.0

    // Cálculo de datos para la Gráfica de Dona (Agrupación de tiempo por proceso).
    val tiempoPorProceso = remember(sesionesFiltradas, tareasFiltradas) {
        val mapa = mutableMapOf<String, Float>()
        for (sesion in sesionesFiltradas) {
            val ms = tareasFiltradas.filter { it.sesionId == sesion.id }.sumOf { it.tiempoMilisegundos }.toFloat()
            val actual = mapa[sesion.proceso] ?: 0f
            mapa[sesion.proceso] = actual + ms
        }
        mapa.toList().sortedByDescending { it.second }
    }

    // Cálculo de la comparativa de tiempo por tarea individual (porcentaje en relación al total).
    val tareasConPorcentaje = remember(tareasFiltradas) {
        val sumaTotal = tareasFiltradas.sumOf { it.tiempoMilisegundos }.toFloat().coerceAtLeast(1f)
        tareasFiltradas.groupBy { it.tarea }.map { (nombreTarea, listaTareas) ->
            val tiempoAcumulado = listaTareas.sumOf { it.tiempoMilisegundos }.toFloat()
            val porcentaje = (tiempoAcumulado / sumaTotal) * 100f
            Triple(nombreTarea, tiempoAcumulado / 1000.0, porcentaje)
        }.sortedByDescending { it.second }
    }

    // Definición de paleta de colores personalizada para las representaciones gráficas.
    val TurquesaCustom = Color(0xFF00BCD4)
    val MoradoGrafica = Color(0xFFAB47BC)
    val NaranjaGrafica = Color(0xFFFF9800)
    val VerdeGrafica = Color(0xFF4CAF50)
    val RosaGrafica = Color(0xFFE91E63)
    val AzulGrafica = Color(0xFF3F51B5)
    val FondoGrafica = Color(0xFFE0E0E0)

    val paletaColores = listOf(TurquesaCustom, MoradoGrafica, NaranjaGrafica, VerdeGrafica, RosaGrafica, AzulGrafica)

    // Estados de expansión para los menús desplegables (ExposedDropdownMenu).
    var expandedProceso by remember { mutableStateOf(false) }
    var expandedSubproceso by remember { mutableStateOf(false) }
    var expandedActividad by remember { mutableStateOf(false) }
    var expandedArea by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Analítico de Tiempos") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Volver", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- FILTROS AVANZADOS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Filtros de Análisis", style = MaterialTheme.typography.titleSmall)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Selector de Proceso
                            ExposedDropdownMenuBox(
                                expanded = expandedProceso,
                                onExpandedChange = { expandedProceso = !expandedProceso },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(value = filtroProceso, onValueChange = {}, readOnly = true, label = { Text("Proceso") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProceso) }, modifier = Modifier.menuAnchor())
                                ExposedDropdownMenu(expanded = expandedProceso, onDismissRequest = { expandedProceso = false }) {
                                    procesosDisponibles.forEach { proc ->
                                        DropdownMenuItem(text = { Text(proc) }, onClick = { filtroProceso = proc; filtroSubproceso = "Todos"; filtroActividad = "Todas"; expandedProceso = false })
                                    }
                                }
                            }
                            // Selector de Subproceso
                            ExposedDropdownMenuBox(
                                expanded = expandedSubproceso,
                                onExpandedChange = { expandedSubproceso = !expandedSubproceso },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(value = filtroSubproceso, onValueChange = {}, readOnly = true, label = { Text("Subproceso") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubproceso) }, modifier = Modifier.menuAnchor())
                                ExposedDropdownMenu(expanded = expandedSubproceso, onDismissRequest = { expandedSubproceso = false }) {
                                    subprocesosDisponibles.forEach { sub ->
                                        DropdownMenuItem(text = { Text(sub) }, onClick = { filtroSubproceso = sub; filtroActividad = "Todas"; expandedSubproceso = false })
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Selector de Actividad
                            ExposedDropdownMenuBox(
                                expanded = expandedActividad,
                                onExpandedChange = { expandedActividad = !expandedActividad },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(value = filtroActividad, onValueChange = {}, readOnly = true, label = { Text("Actividad") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActividad) }, modifier = Modifier.menuAnchor())
                                ExposedDropdownMenu(expanded = expandedActividad, onDismissRequest = { expandedActividad = false }) {
                                    actividadesDisponibles.forEach { act ->
                                        DropdownMenuItem(text = { Text(act) }, onClick = { filtroActividad = act; expandedActividad = false })
                                    }
                                }
                            }
                            // Selector de Área
                            ExposedDropdownMenuBox(
                                expanded = expandedArea,
                                onExpandedChange = { expandedArea = !expandedArea },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(value = filtroArea, onValueChange = {}, readOnly = true, label = { Text("Área") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedArea) }, modifier = Modifier.menuAnchor())
                                ExposedDropdownMenu(expanded = expandedArea, onDismissRequest = { expandedArea = false }) {
                                    areasDisponibles.forEach { area ->
                                        DropdownMenuItem(text = { Text(area) }, onClick = { filtroArea = area; expandedArea = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- TARJETAS DE KPIS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard("Sesiones", "$totalSesiones", TurquesaCustom, modifier = Modifier.weight(1f))
                    MetricCard("Tareas", "$totalTareas", MoradoGrafica, modifier = Modifier.weight(1f))
                    MetricCard("Tiempo", "%.1fs".format(tiempoTotalSegundos), NaranjaGrafica, modifier = Modifier.weight(1f))
                }
            }

            // --- COMPARATIVA DE TAREAS (IMPACTO DE TIEMPO POR TAREA) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⏱️ Impacto de Tiempo por Tarea (% del Proceso)", style = MaterialTheme.typography.titleSmall)

                        if (tareasConPorcentaje.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No hay tareas registradas con los filtros actuales", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                tareasConPorcentaje.forEach { (nombreTarea, segundos, porcentaje) ->
                                    // Animación fluida para el llenado de las barras de progreso
                                    val animProgreso by animateFloatAsState(
                                        targetValue = (porcentaje / 100f).coerceIn(0f, 1f),
                                        animationSpec = tween(1000),
                                        label = "tarea"
                                    )

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = nombreTarea, style = MaterialTheme.typography.bodySmall)
                                            Text(text = "%.1fs (%.1f%%)".format(segundos, porcentaje), style = MaterialTheme.typography.bodySmall, color = TurquesaCustom)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(12.dp)
                                        ) {
                                            // Renderizado personalizado de la barra con Canvas
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawRoundRect(color = FondoGrafica, size = size, cornerRadius = CornerRadius(6f, 6f))
                                                drawRoundRect(color = TurquesaCustom, size = Size(width = size.width * animProgreso, height = size.height), cornerRadius = CornerRadius(6f, 6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- GRÁFICA DE DONA (DISTRIBUCIÓN PERCENTUAL POR PROCESO) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🍩 Distribución Porcentual por Proceso", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

                        if (tiempoPorProceso.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                Text("Sin datos para graficar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        } else {
                            val sumaTotalMs = tiempoPorProceso.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

                            Box(
                                modifier = Modifier.size(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Dibujo vectorial de la gráfica circular tipo dona
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = -90f
                                    val strokeWidth = 36f

                                    tiempoPorProceso.forEachIndexed { index, pair ->
                                        val sweepAngle = (pair.second / sumaTotalMs) * 360f
                                        drawArc(
                                            color = paletaColores[index % paletaColores.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("%.1fs".format(tiempoTotalSegundos), style = MaterialTheme.typography.titleSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Leyenda detallada para la gráfica de dona
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tiempoPorProceso.forEachIndexed { index, (proc, ms) ->
                                    val porcentaje = (ms / sumaTotalMs) * 100f
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(10.dp)) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawCircle(color = paletaColores[index % paletaColores.size])
                                                }
                                            }
                                            Text(text = proc, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(text = "%.1f%%".format(porcentaje), style = MaterialTheme.typography.bodySmall, color = TurquesaCustom)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- HISTÓRICO FILTRADO ---
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Sesiones Filtradas (${sesionesFiltradas.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (sesionesFiltradas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros que coincidan con los filtros seleccionados.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(sesionesFiltradas) { sesion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Proceso: ${sesion.proceso}", style = MaterialTheme.typography.titleSmall, color = TurquesaCustom)
                                Text(text = "ID: #${sesion.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("Subproceso: ${sesion.subproceso}", style = MaterialTheme.typography.bodyMedium)
                            Text("Actividad: ${sesion.actividad}", style = MaterialTheme.typography.bodyMedium)
                            Text("Área: ${sesion.areaMedida} | Persona: ${sesion.personaMedida}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// Componente auxiliar reutilizable para mostrar tarjetas de indicadores clave de rendimiento (KPIs).
@Composable
fun MetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accentColor)
        }
    }
}
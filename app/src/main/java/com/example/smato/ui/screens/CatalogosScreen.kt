package com.example.smato.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.example.smato.viewmodel.MedicionViewModel

// Pantalla para la gestión de catálogos del sistema (Áreas y Personas),
// permitiendo registrar nuevos elementos, visualizarlos y eliminarlos de forma dinámica.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogosScreen(
    viewModel: MedicionViewModel,
    onBack: () -> Unit
) {
    // Estado local para controlar la pestaña seleccionada (0 para Áreas, 1 para Personas).
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Áreas", "Personas")

    // Recolectamos las listas completas desde los flujos (Flow) del ViewModel.
    val areas by viewModel.areasCompletas.collectAsState(initial = emptyList())
    val personas by viewModel.personasCompletas.collectAsState(initial = emptyList())

    // Estado local para capturar el texto ingresado en el campo de texto.
    var nuevoTextoInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Catálogos") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de pestañas para alternar entre la administración de Áreas y Personas.
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            nuevoTextoInput = "" // Limpia el campo de texto al cambiar de pestaña
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Campo de texto dinámico que cambia su etiqueta según la pestaña activa.
                OutlinedTextField(
                    value = nuevoTextoInput,
                    onValueChange = { nuevoTextoInput = it },
                    label = { Text(if (tabIndex == 0) "Nueva Área" else "Nueva Persona") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para guardar el nuevo elemento en el catálogo correspondiente.
                Button(
                    onClick = {
                        if (tabIndex == 0) {
                            viewModel.agregarArea(nuevoTextoInput)
                        } else {
                            viewModel.agregarPersona(nuevoTextoInput)
                        }
                        nuevoTextoInput = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (tabIndex == 0) "Guardar Área" else "Guardar Persona")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Renderizado condicional de la lista correspondiente según la pestaña seleccionada.
                if (tabIndex == 0) {
                    Text("Áreas Existentes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(areas) { area ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(area.nombre, style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = { viewModel.eliminarArea(area) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Personas Existentes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(personas) { persona ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(persona.nombre, style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = { viewModel.eliminarPersona(persona) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
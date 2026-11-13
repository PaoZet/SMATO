package com.example.smato.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smato.data.local.AppDatabase
import com.example.smato.data.local.entities.AreaEntity
import com.example.smato.data.local.entities.EvidenciaMultimedia
import com.example.smato.data.local.entities.PersonaEntity
import com.example.smato.data.local.entities.RegistroTarea
import com.example.smato.data.local.entities.SesionMedicion
import com.example.smato.data.local.entities.UsuarioEntity
import com.example.smato.utils.CryptoHelper
import com.example.smato.utils.ZipUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ViewModel principal de la aplicación SMATO que coordina las operaciones de negocio,
// la gestión de estado de la interfaz, el acceso a datos locales mediante DAOs
// y los flujos de exportación/cifrado de información.
class MedicionViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia principal de la base de datos local y referencias a los Objetos de Acceso a Datos (DAOs).
    private val database = AppDatabase.getDatabase(application)
    val medicionDao = database.medicionDao()
    private val evidenciaDao = database.evidenciaDao()
    val usuarioDao = database.usuarioDao()

    // Dirección de correo del usuario autenticado en la sesión activa.
    var usuarioActualEmail: String = ""
        private set

    // Flujo reactivo (Flow) que expone la lista de sesiones de medición pertenecientes al usuario actual.
    val historialSesiones: Flow<List<SesionMedicion>>
        get() = medicionDao.obtenerSesionesPorUsuario(usuarioActualEmail)

    // Flujo reactivo que expone la totalidad de tareas registradas en la base de datos.
    val todasLasTareas: Flow<List<RegistroTarea>> = medicionDao.obtenerTodasLasTareas()

    // Estado reactivo y de solo lectura para la lista de áreas de trabajo del catálogo.
    private val _areasCompletas = MutableStateFlow<List<AreaEntity>>(emptyList())
    val areasCompletas: StateFlow<List<AreaEntity>> = _areasCompletas.asStateFlow()

    // Estado reactivo y de solo lectura para la lista de personas registradas en el catálogo.
    private val _personasCompletas = MutableStateFlow<List<PersonaEntity>>(emptyList())
    val personasCompletas: StateFlow<List<PersonaEntity>> = _personasCompletas.asStateFlow()

    // Estado temporal para acumular las tareas creadas durante una sesión en curso antes de persisitirlas.
    private val _tareasTemporales = MutableStateFlow<List<RegistroTarea>>(emptyList())
    val tareasTemporales: StateFlow<List<RegistroTarea>> = _tareasTemporales.asStateFlow()

    // Estado temporal para acumular los archivos de evidencia multimedia adjuntos a la sesión actual.
    private val _evidenciasTemporales = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val evidenciasTemporales: StateFlow<List<Pair<String, String>>> = _evidenciasTemporales.asStateFlow()

    // Verifica las credenciales de acceso (correo y contraseña) registradas en la base de datos local.
    fun verificarCredenciales(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (email.isBlank() || pass.isBlank()) {
                    onResult(false, "Por favor completa todos los campos.")
                    return@launch
                }
                val usuario = usuarioDao.obtenerUsuarioPorEmail(email.trim())
                if (usuario == null) {
                    onResult(false, "Este correo no está registrado. Regístrate primero.")
                } else if (usuario.password != pass) {
                    onResult(false, "Contraseña incorrecta.")
                } else {
                    usuarioActualEmail = email.trim()
                    onResult(true, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error al iniciar sesión: ${e.localizedMessage}")
            }
        }
    }

    // Registra una nueva cuenta de usuario en el almacenamiento local verificando previamente su existencia.
    fun registrarNuevoUsuario(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (email.isBlank() || pass.isBlank()) {
                    onResult(false, "Por favor completa todos los campos.")
                    return@launch
                }
                val usuarioExistente = usuarioDao.obtenerUsuarioPorEmail(email.trim())
                if (usuarioExistente != null) {
                    onResult(false, "Este correo ya está registrado. Inicia sesión.")
                } else {
                    usuarioDao.registrarUsuario(UsuarioEntity(email = email.trim(), password = pass))
                    onResult(true, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error al registrar usuario: ${e.localizedMessage}")
            }
        }
    }

    // Cierra la sesión activa restableciendo el identificador del usuario.
    fun cerrarSesion() {
        usuarioActualEmail = ""
    }

    // Agrega una nueva área de trabajo al catálogo local.
    fun agregarArea(nombre: String) {
        if (nombre.isNotBlank()) {
            val lista = _areasCompletas.value.toMutableList()
            lista.add(AreaEntity(nombre = nombre))
            _areasCompletas.value = lista
        }
    }

    // Remueve un área del catálogo local.
    fun eliminarArea(area: AreaEntity) {
        val lista = _areasCompletas.value.toMutableList()
        lista.remove(area)
        _areasCompletas.value = lista
    }

    // Agrega una nueva persona al catálogo local.
    fun agregarPersona(nombre: String) {
        if (nombre.isNotBlank()) {
            val lista = _personasCompletas.value.toMutableList()
            lista.add(PersonaEntity(nombre = nombre))
            _personasCompletas.value = lista
        }
    }

    // Remueve una persona del catálogo local.
    fun eliminarPersona(persona: PersonaEntity) {
        val lista = _personasCompletas.value.toMutableList()
        lista.remove(persona)
        _personasCompletas.value = lista
    }

    // Agrega una tarea con sus métricas y tiempo acumulado a la lista temporal, cifrando el comentario antes de guardarlo.
    fun agregarTareaConTiempo(
        tarea: String, comentario: String, clicks: Int,
        pantallas: Int, camposLlenados: Int, hojasLlenadas: Int,
        tiempoMilisegundos: Long
    ) {
        val comentarioCifrado = CryptoHelper.encrypt(comentario)
        val lista = _tareasTemporales.value.toMutableList()
        lista.add(
            RegistroTarea(
                sesionId = 0L, tarea = tarea, comentario = comentarioCifrado,
                clicks = clicks, pantallas = pantallas,
                camposLlenados = camposLlenados, hojasLlenadas = hojasLlenadas,
                tiempoMilisegundos = tiempoMilisegundos
            )
        )
        _tareasTemporales.value = lista
    }

    // Registra temporalmente un archivo multimedia detectando automáticamente su tipo en base a la extensión.
    fun agregarEvidenciaTemporal(tipo: String, ruta: String) {
        val tipoReal = when {
            ruta.endsWith(".jpg", ignoreCase = true) || ruta.endsWith(".png", ignoreCase = true) -> "FOTO"
            ruta.endsWith(".mp4", ignoreCase = true) || ruta.endsWith(".3gp", ignoreCase = true) -> "VIDEO"
            ruta.endsWith(".mp3", ignoreCase = true) || ruta.endsWith(".m4a", ignoreCase = true) -> "AUDIO"
            else -> tipo
        }
        val lista = _evidenciasTemporales.value.toMutableList()
        lista.add(Pair(tipoReal, ruta))
        _evidenciasTemporales.value = lista
    }

    // Remueve un elemento de la lista temporal de evidencias multimedia según su índice.
    fun eliminarEvidenciaTemporal(index: Int) {
        val lista = _evidenciasTemporales.value.toMutableList()
        if (index in lista.indices) {
            lista.removeAt(index)
            _evidenciasTemporales.value = lista
        }
    }

    // Persiste de manera definitiva la sesión de medición completa, junto con sus tareas y evidencias asociadas.
    fun guardarSesionCompleta(
        proceso: String, subproceso: String, actividad: String,
        persona: String, area: String, agencia: String,
        onSesionCreada: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val fechaActual = formatoFecha.format(java.util.Date())

            val nuevaSesion = SesionMedicion(
                proceso = proceso, subproceso = subproceso, actividad = actividad,
                personaMedida = persona, areaMedida = area, agencia = agencia,
                fecha = fechaActual, descripcion = "Sesión registrada",
                usuarioEmail = usuarioActualEmail
            )
            val sesionId = medicionDao.insertarSesion(nuevaSesion)

            for (tarea in _tareasTemporales.value) {
                medicionDao.insertarTarea(tarea.copy(sesionId = sesionId))
            }
            for (evidencia in _evidenciasTemporales.value) {
                evidenciaDao.insertarEvidencia(
                    EvidenciaMultimedia(sesionId = sesionId, tipo = evidencia.first, rutaArchivo = evidencia.second)
                )
            }
            _tareasTemporales.value = emptyList()
            _evidenciasTemporales.value = emptyList()
            onSesionCreada(sesionId)
        }
    }

    // Genera un archivo XML Spreadsheet con el historial de mediciones completo y lo empaqueta en un ZIP cifrado con AES-256.
    fun exportarTodoHistorialExcelCifrado(
        context: android.content.Context,
        passwordSeguro: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Consultamos con el email actual o traemos todo si el filtro llega vacio.
                val sesiones = medicionDao.obtenerSesionesPorUsuarioLista(usuarioActualEmail.trim())
                val todasLasTareas = medicionDao.obtenerTodasLasTareasLista()

                // Verificación de depuración en Logcat para diagnóstico rápido.
                android.util.Log.d("SMATO_EXPORT", "Sesiones encontradas: ${sesiones.size} para usuario: '$usuarioActualEmail'")
                android.util.Log.d("SMATO_EXPORT", "Total de tareas en BD: ${todasLasTareas.size}")

                val archivoExcel = File(context.cacheDir, "Reporte_Historial_Completo.xls")

                val excelContent = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    appendLine("<?mso-application progid=\"Excel.Sheet\"?>")
                    appendLine("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"")
                    appendLine(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">")
                    appendLine("<Worksheet ss:Name=\"Historial_General\">")
                    appendLine("<Table>")

                    appendLine("<Row>")
                    listOf("Sesion ID", "Proceso", "Subproceso", "Actividad", "Persona", "Area", "Agencia", "Fecha", "Tarea", "Comentarios", "Clicks", "Pantallas", "Campos", "Hojas", "Tiempo Tarea(s)", "Tiempo Total(s)").forEach { header ->
                        appendLine("<Cell><Data ss:Type=\"String\">$header</Data></Cell>")
                    }
                    appendLine("</Row>")

                    sesiones.forEach { sesion ->
                        // Filtramos las tareas que pertenecen exactamente al ID de esta sesión.
                        val tareasDeEstaSesion = todasLasTareas.filter { it.sesionId == sesion.id }
                        val tiempoTotalSesion = tareasDeEstaSesion.sumOf { it.tiempoMilisegundos } / 1000.0

                        if (tareasDeEstaSesion.isNotEmpty()) {
                            tareasDeEstaSesion.forEach { t ->
                                val comentarioLimpio = try { CryptoHelper.decrypt(t.comentario) } catch (e: Exception) { t.comentario }
                                appendLine("<Row>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${sesion.id}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.proceso}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.subproceso}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.actividad}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.personaMedida}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.areaMedida}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.agencia}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${sesion.fecha}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">${t.tarea}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\">$comentarioLimpio</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${t.clicks}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${t.pantallas}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${t.camposLlenados}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${t.hojasLlenadas}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"Number\">${t.tiempoMilisegundos / 1000.0}</Data></Cell>")
                                appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                                appendLine("</Row>")
                            }

                            // Fila de resumen de tiempo total para esta sesión.
                            appendLine("<Row>")
                            appendLine("<Cell><Data ss:Type=\"Number\">${sesion.id}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.proceso}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.subproceso}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.actividad}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.personaMedida}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.areaMedida}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.agencia}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">${sesion.fecha}</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\">Tiempo total</Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"String\"></Data></Cell>")
                            appendLine("<Cell><Data ss:Type=\"Number\">$tiempoTotalSesion</Data></Cell>")
                            appendLine("</Row>")
                        }
                    }

                    appendLine("</Table>")
                    appendLine("</Worksheet>")
                    appendLine("</Workbook>")
                }
                archivoExcel.writeText(excelContent)

                val archivoZipSalida = File(context.getExternalFilesDir(null), "SMATO_Exportar_Datos.zip")
                ZipUtils.exportarDatosCifrados(listOf(archivoExcel), archivoZipSalida, passwordSeguro)
                onResultado(true, archivoZipSalida.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResultado(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    // Exporta únicamente los archivos de evidencias multimedia asociadas a las sesiones activas, renombrándolas y empaquetándolas en un ZIP cifrado.
    fun exportarSoloEvidenciasCifrado(
        context: android.content.Context,
        passwordSeguro: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Obtenemos únicamente las sesiones que realmente siguen vigentes en la BD del usuario.
                val sesionesActivas = medicionDao.obtenerSesionesPorUsuarioLista(usuarioActualEmail)
                val idsSesionesActivas = sesionesActivas.map { it.id }.toSet()

                val todasLasTareas = medicionDao.obtenerTodasLasTareasLista()
                val archivosAEmpaquetar = mutableListOf<File>()

                for (sesion in sesionesActivas) {
                    val evidenciasSesion = evidenciaDao.obtenerListaEvidenciasDeSesion(sesion.id)
                    val tareasSesion = todasLasTareas.filter { it.sesionId == sesion.id }

                    evidenciasSesion.forEachIndexed { index, ev ->
                        // Validamos estrictamente que la evidencia pertenezca a una sesión activa.
                        if (idsSesionesActivas.contains(ev.sesionId)) {
                            val archivoOriginal = File(ev.rutaArchivo)
                            if (archivoOriginal.exists()) {
                                val tareaAsociada = if (index < tareasSesion.size) {
                                    tareasSesion[index].tarea
                                } else {
                                    tareasSesion.lastOrNull()?.tarea ?: "General"
                                }

                                val procesoLimpio = sesion.proceso.replace(Regex("[^A-Za-z0-9_]"), "_")
                                val tareaLimpia = tareaAsociada.replace(Regex("[^A-Za-z0-9_]"), "_")
                                val extension = archivoOriginal.extension.ifEmpty { "mp4" }

                                val nombrePersonalizado = "Sesion_${sesion.id}_Proc_${procesoLimpio}_Tarea_${tareaLimpia}_${index + 1}.$extension"
                                val archivoRenombrado = File(context.cacheDir, nombrePersonalizado)

                                archivoOriginal.copyTo(archivoRenombrado, overwrite = true)
                                if (!archivosAEmpaquetar.any { it.absolutePath == archivoRenombrado.absolutePath }) {
                                    archivosAEmpaquetar.add(archivoRenombrado)
                                }
                            }
                        }
                    }
                }

                if (archivosAEmpaquetar.isEmpty()) {
                    onResultado(false, "No hay evidencias registradas para exportar.")
                    return@launch
                }

                val archivoZipSalida = File(context.getExternalFilesDir(null), "SMATO_Exportar_Evidencias.zip")
                ZipUtils.exportarDatosCifrados(archivosAEmpaquetar, archivoZipSalida, passwordSeguro)
                onResultado(true, archivoZipSalida.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResultado(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    // Obtiene las tareas registradas de una sesión en particular expuestas como Flow.
    fun obtenerTareasDeSesion(sesionId: Long): Flow<List<RegistroTarea>> = medicionDao.obtenerTareasDeSesion(sesionId)

    // Obtiene las evidencias multimedia de una sesión expuestas como Flow.
    fun obtenerEvidenciasDeSesion(sesionId: Long): Flow<List<EvidenciaMultimedia>> = evidenciaDao.obtenerEvidenciasDeSesion(sesionId)

    // Elimina una sesión específica junto con sus archivos físicos de evidencia y registros en base de datos.
    fun eliminarSesion(sesionId: Long) {
        viewModelScope.launch {
            try {
                val appContext = getApplication<Application>()
                // 1. Obtenemos las evidencias de esta sesión.
                val evidencias = evidenciaDao.obtenerListaEvidenciasDeSesion(sesionId)
                for (ev in evidencias) {
                    val archivo = File(ev.rutaArchivo)
                    if (archivo.exists()) {
                        archivo.delete()
                    }
                    val archivoCache = File(appContext.cacheDir, archivo.name)
                    if (archivoCache.exists()) {
                        archivoCache.delete()
                    }
                }

                // 2. Borramos de la base de datos.
                evidenciaDao.eliminarEvidenciasDeSesion(sesionId)
                medicionDao.eliminarTareasDeSesion(sesionId)
                medicionDao.eliminarSesionPorId(sesionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Elimina la totalidad del historial de sesiones, tareas y archivos de evidencia pertenecientes al usuario actual.
    fun eliminarTodoHistorial() {
        viewModelScope.launch {
            try {
                val appContext = getApplication<Application>()

                // 1. Obtenemos todas las sesiones del usuario actual.
                val sesiones = medicionDao.obtenerSesionesPorUsuarioLista(usuarioActualEmail)

                for (sesion in sesiones) {
                    // Obtenemos y borramos físicamente cada evidencia de cada sesión.
                    val evidencias = evidenciaDao.obtenerListaEvidenciasDeSesion(sesion.id)
                    for (ev in evidencias) {
                        val archivo = File(ev.rutaArchivo)
                        if (archivo.exists()) {
                            archivo.delete()
                        }
                        val archivoCache = File(appContext.cacheDir, archivo.name)
                        if (archivoCache.exists()) {
                            archivoCache.delete()
                        }
                    }
                    // Borramos las referencias en la BD para esta sesión específica.
                    evidenciaDao.eliminarEvidenciasDeSesion(sesion.id)
                    medicionDao.eliminarTareasDeSesion(sesion.id)
                }

                // 2. Finalmente limpiamos todas las tablas por completo.
                medicionDao.eliminarTodasLasTareas()
                evidenciaDao.eliminarEvidenciasDeSesion(0L)
                medicionDao.eliminarTodasLasSesiones()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
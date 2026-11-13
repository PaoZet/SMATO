package com.example.smato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.smato.data.local.entities.SesionMedicion
import com.example.smato.data.local.entities.RegistroTarea

// Objeto de Acceso a Datos (DAO) principal para gestionar las sesiones de medición y sus tareas asociadas.
@Dao
interface MedicionDao {

    // Obtiene un flujo reactivo con todas las sesiones de medición almacenadas en la base de datos.
    @Query("SELECT * FROM sesiones_medicion")
    fun obtenerTodasLasSesiones(): Flow<List<SesionMedicion>>

    // Inserta una nueva sesión y retorna su ID generado de forma asíncrona.
    @Insert
    suspend fun insertarSesion(sesion: SesionMedicion): Long

    // Inserta un nuevo registro de tarea vinculado a una sesión específica.
    @Insert
    suspend fun insertarTarea(tarea: RegistroTarea)

    // Obtiene un flujo reactivo con las tareas correspondientes a una sesión específica mediante su ID.
    @Query("SELECT * FROM registros_tarea WHERE sesionId = :sesionId")
    fun obtenerTareasDeSesion(sesionId: Long): Flow<List<RegistroTarea>>

    // Obtiene un flujo reactivo con absolutamente todas las tareas registradas en la base de datos.
    @Query("SELECT * FROM registros_tarea")
    fun obtenerTodasLasTareas(): Flow<List<RegistroTarea>>

    // Elimina una sesión de medición específica utilizando su ID único.
    @Query("DELETE FROM sesiones_medicion WHERE id = :sesionId")
    suspend fun eliminarSesionPorId(sesionId: Long)

    // Elimina todas las tareas asociadas a una sesión específica.
    @Query("DELETE FROM registros_tarea WHERE sesionId = :sesionId")
    suspend fun eliminarTareasDeSesion(sesionId: Long)

    // Elimina todas las sesiones almacenadas en la base de datos.
    @Query("DELETE FROM sesiones_medicion")
    suspend fun eliminarTodasLasSesiones()

    // Elimina todos los registros de tareas de la base de datos.
    @Query("DELETE FROM registros_tarea")
    suspend fun eliminarTodasLasTareas()

    // Obtiene un flujo reactivo filtrando las sesiones de medición por el correo del usuario actual.
    @Query("SELECT * FROM sesiones_medicion WHERE usuarioEmail = :email")
    fun obtenerSesionesPorUsuario(email: String): Flow<List<SesionMedicion>>

    // Obtiene una lista síncrona de las sesiones filtradas por el correo del usuario, ideal para exportaciones.
    @Query("SELECT * FROM sesiones_medicion WHERE usuarioEmail = :email")
    suspend fun obtenerSesionesPorUsuarioLista(email: String): List<SesionMedicion>

    // Obtiene una lista estática y síncrona de todas las tareas registradas en el sistema.
    @Query("SELECT * FROM registros_tarea")
    suspend fun obtenerTodasLasTareasLista(): List<RegistroTarea>

}
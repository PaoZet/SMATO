package com.example.smato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.smato.data.local.entities.EvidenciaMultimedia

// Objeto de Acceso a Datos (DAO) para gestionar las evidencias multimedia vinculadas a las sesiones de medición.
@Dao
interface EvidenciaDao {

    // Inserta un nuevo registro de evidencia multimedia en la base de datos de forma asíncrona.
    @Insert
    suspend fun insertarEvidencia(evidencia: EvidenciaMultimedia)

    // Obtiene un flujo reactivo (Flow) con la lista de evidencias asociadas a una sesión específica mediante su ID.
    @Query("SELECT * FROM evidencias_multimedia WHERE sesionId = :sesionId")
    fun obtenerEvidenciasDeSesion(sesionId: Long): Flow<List<EvidenciaMultimedia>>

    // Elimina un registro de evidencia específico basándose en su ID único.
    @Query("DELETE FROM evidencias_multimedia WHERE id = :evidenciaId")
    suspend fun eliminarEvidencia(evidenciaId: Long)

    // Elimina todas las evidencias registradas que pertenezcan a una sesión específica.
    @Query("DELETE FROM evidencias_multimedia WHERE sesionId = :sesionId")
    suspend fun eliminarEvidenciasDeSesion(sesionId: Long)

    // Obtiene una lista estática y síncrona de las evidencias de una sesión, ideal para procesos de exportación y limpieza.
    @Query("SELECT * FROM evidencias_multimedia WHERE sesionId = :sesionId")
    suspend fun obtenerListaEvidenciasDeSesion(sesionId: Long): List<EvidenciaMultimedia>
}
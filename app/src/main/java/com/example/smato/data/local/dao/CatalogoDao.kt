package com.example.smato.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.smato.data.local.entities.AreaEntity
import com.example.smato.data.local.entities.PersonaEntity

// Objeto de Acceso a Datos (DAO) para gestionar las operaciones de catálogos locales (Áreas y Personas).
@Dao
interface CatalogoDao {

    // Obtiene un flujo reactivo con la lista de todas las áreas registradas.
    @Query("SELECT * FROM areas")
    fun obtenerAreas(): Flow<List<AreaEntity>>

    // Inserta una nueva área en la base de datos, ignorando duplicados en caso de conflicto.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarArea(area: AreaEntity)

    // Elimina un área específica de la base de datos.
    @Delete
    suspend fun eliminarArea(area: AreaEntity)

    // Obtiene un flujo reactivo con la lista de todas las personas registradas.
    @Query("SELECT * FROM personas")
    fun obtenerPersonas(): Flow<List<PersonaEntity>>

    // Inserta una nueva persona en la base de datos, ignorando duplicados en caso de conflicto.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarPersona(persona: PersonaEntity)

    // Elimina una persona específica de la base de datos.
    @Delete
    suspend fun eliminarPersona(persona: PersonaEntity)
}
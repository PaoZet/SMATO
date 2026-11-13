package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa a las personas evaluadas
// o involucradas en los procesos dentro del catálogo del sistema.
@Entity(tableName = "personas")
data class PersonaEntity(
    // Identificador único autoincrementable que funge como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Nombre completo o identificador de la persona.
    val nombre: String
)
package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa un área de trabajo o departamento disponible en los catálogos.
@Entity(tableName = "areas")
data class AreaEntity(
    // Identificador único autoincrementable que sirve como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Nombre descriptivo del área o departamento.
    val nombre: String
)
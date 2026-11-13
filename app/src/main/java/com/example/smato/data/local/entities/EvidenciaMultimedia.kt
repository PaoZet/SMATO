package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa los archivos multimedia adjuntos
// como evidencias y vinculados directamente a una sesión de medición específica.
@Entity(tableName = "evidencias_multimedia")
data class EvidenciaMultimedia(
    // Identificador único autoincrementable que funge como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Identificador de la sesión de medición a la que pertenece esta evidencia.
    val sesionId: Long,

    // Tipo de contenido multimedia registrado (por ejemplo: "FOTO", "AUDIO", "VIDEO").
    val tipo: String,

    // Ruta de almacenamiento local donde se encuentra guardado físicamente el archivo en el dispositivo.
    val rutaArchivo: String
)
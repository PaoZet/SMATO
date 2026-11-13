package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa los datos detallados recolectados
// durante la ejecución de una tarea específica en una sesión de medición.
@Entity(tableName = "registros_tarea")
data class RegistroTarea(
    // Identificador único autoincrementable que funge como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Identificador de la sesión de medición a la que pertenece este registro de tarea.
    val sesionId: Long,

    // Nombre o descripción de la tarea evaluada.
    val tarea: String,

    // Comentarios u observaciones adicionales sobre la ejecución de la tarea.
    val comentario: String,

    // Cantidad de clics realizados durante la actividad.
    val clicks: Int,

    // Número de pantallas navegadas o utilizadas.
    val pantallas: Int,

    // Cantidad de campos completados en la interfaz o formularios.
    val camposLlenados: Int,

    // Cantidad de hojas o documentos procesados o llenados.
    val hojasLlenadas: Int,

    // Duración total de la tarea medida en milisegundos.
    val tiempoMilisegundos: Long
)
package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa una sesión de medición completa,
// agrupando los metadatos generales del proceso evaluado y el usuario responsable.
@Entity(tableName = "sesiones_medicion")
data class SesionMedicion(
    // Identificador único autoincrementable que funge como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Nombre del proceso principal que se está midiendo.
    val proceso: String,

    // Subproceso o categoría secundaria asociada al proceso.
    val subproceso: String,

    // Actividad específica dentro del flujo de trabajo evaluado.
    val actividad: String,

    // Nombre de la persona evaluada durante la sesión.
    val personaMedida: String,

    // Área de trabajo o departamento donde se realiza la medición.
    val areaMedida: String,

    // Agencia, sucursal o ubicación física de la evaluación.
    val agencia: String,

    // Fecha en la que se llevó a cabo la sesión de medición.
    val fecha: String,

    // Descripción general u observaciones de la sesión.
    val descripcion: String,

    // Correo electrónico del usuario que registró o ejecutó la sesión de medición.
    val usuarioEmail: String = ""
)
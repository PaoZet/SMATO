package com.example.smato.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa métricas registradas en el sistema,
// almacenando información cuantitativa asociada a procesos específicos y marcas de tiempo.
@Entity(tableName = "metrics")
data class MetricEntity(
    // Identificador único autoincrementable que funge como clave primaria de la tabla.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Nombre del proceso evaluado o relacionado con la métrica.
    val processName: String,

    // Valor numérico de la métrica registrada.
    val metricValue: Double,

    // Marca de tiempo en milisegundos que indica el momento exacto del registro.
    val timestamp: Long
)
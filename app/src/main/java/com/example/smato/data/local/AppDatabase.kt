package com.example.smato.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smato.data.local.dao.EvidenciaDao
import com.example.smato.data.local.dao.MedicionDao
import com.example.smato.data.local.dao.UsuarioDao
import com.example.smato.data.local.entities.EvidenciaMultimedia
import com.example.smato.data.local.entities.RegistroTarea
import com.example.smato.data.local.entities.SesionMedicion
import com.example.smato.data.local.entities.UsuarioEntity

// Definición principal de la base de datos local usando Room.
// Agrupa todas las entidades y la versión actual del esquema de datos.
@Database(
    entities = [
        SesionMedicion::class,
        RegistroTarea::class,
        EvidenciaMultimedia::class,
        UsuarioEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Declaración de los DAOs para interactuar con las tablas correspondientes.
    abstract fun medicionDao(): MedicionDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        // Instancia única y volátil para garantizar acceso seguro entre hilos (Thread-safe Singleton).
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Patrón Singleton para obtener o inicializar la instancia de la base de datos.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smato_database"
                )
                    // Permite la reconstrucción limpia de la BD si cambia la versión, evitando bloqueos por migraciones ausentes.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
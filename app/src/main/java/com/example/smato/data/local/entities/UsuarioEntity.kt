package com.example.smato.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad de la base de datos que representa a un usuario registrado en el sistema,
// almacenando sus credenciales de acceso para la autenticación local.
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    // Correo electrónico del usuario, utilizado como clave primaria única (Primary Key).
    @PrimaryKey
    val email: String,

    // Contraseña asociada a la cuenta del usuario.
    val password: String
)
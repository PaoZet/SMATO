package com.example.smato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smato.data.local.entities.UsuarioEntity

// Objeto de Acceso a Datos (DAO) para gestionar la autenticación y el registro de usuarios en la base de datos local.
@Dao
interface UsuarioDao {

    // Busca y retorna un usuario específico utilizando su correo electrónico como filtro único, limitando el resultado a un registro.
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun obtenerUsuarioPorEmail(email: String): UsuarioEntity?

    // Inserta un nuevo usuario en la base de datos o reemplaza el registro existente en caso de que ocurra un conflicto de clave.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarUsuario(usuario: UsuarioEntity)
}
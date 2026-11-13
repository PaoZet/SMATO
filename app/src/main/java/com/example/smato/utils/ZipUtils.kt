package com.example.smato.utils

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

// Objeto de utilidad (Singleton) encargado de comprimir y cifrar colecciones de archivos
// o directorios en un paquete ZIP protegido mediante la librería Zip4j con algoritmo AES-256.
object ZipUtils {

    // Comprime una lista de archivos o carpetas en un archivo ZIP de destino,
    // aplicando cifrado avanzado AES-256 en base a la contraseña de seguridad proporcionada.
    fun exportarDatosCifrados(
        archivosAComprimir: List<File>,
        archivoZipDestino: File,
        passwordSeguro: String
    ) {
        try {
            // Inicializa la instancia de Zip4j con el archivo ZIP de destino y la contraseña convertida a arreglo de caracteres.
            val zipFile = ZipFile(archivoZipDestino, passwordSeguro.toCharArray())

            // Configura los parámetros de compresión y cifrado avanzado AES-256.
            val parameters = ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = net.lingala.zip4j.model.enums.AesKeyStrength.KEY_STRENGTH_256
            }

            // Recorre e integra cada archivo o directorio existente al contenedor ZIP aplicando las directivas de seguridad.
            for (archivo in archivosAComprimir) {
                if (archivo.exists()) {
                    if (archivo.isDirectory) {
                        zipFile.addFolder(archivo, parameters)
                    } else {
                        zipFile.addFile(archivo, parameters)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
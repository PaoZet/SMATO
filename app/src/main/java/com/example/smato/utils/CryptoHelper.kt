package com.example.smato.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Objeto de utilidad (Singleton) encargado de proporcionar funciones de cifrado y descifrado
// simétrico utilizando el algoritmo AES en modo GCM (Galois/Counter Mode) con llaves de 256 bits,
// garantizando la confidencialidad y la integridad de los datos sensibles (como los comentarios de las tareas).
object CryptoHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128

    // Clave secreta generada de forma diferida (lazy) utilizando el algoritmo AES con una longitud de 256 bits.
    private val secretKey: SecretKey by lazy {
        KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()
    }

    // Cifra una cadena de texto plano utilizando AES/GCM y devuelve el resultado codificado en formato Base64.
    fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combina el vector de inicialización (IV) con los bytes cifrados para permitir su posterior descifrado.
            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            // En caso de error durante el cifrado, retorna el texto original como medida de respaldo.
            return plainText
        }
    }

    // Descifra una cadena previamente cifrada en Base64 utilizando el vector de inicialización (IV) y la llave secreta.
    fun decrypt(encryptedText: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size < 12) return encryptedText

            // Extrae el vector de inicialización (IV) de los primeros 12 bytes y los datos cifrados restantes.
            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            // En caso de fallo en el descifrado, retorna el texto cifrado original para evitar excepciones en la interfaz.
            return encryptedText
        }
    }
}
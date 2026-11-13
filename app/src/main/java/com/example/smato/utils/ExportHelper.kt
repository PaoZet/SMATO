package com.example.smato.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.smato.data.local.entities.EvidenciaMultimedia
import com.example.smato.data.local.entities.RegistroTarea
import com.example.smato.data.local.entities.SesionMedicion
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Objeto de utilidad (Singleton) encargado de generar reportes en diferentes formatos (XML, y ZIP)
// a partir de las sesiones de medición, tareas y evidencias multimedia, gestionando su posterior compartición.
object ExportHelper {

    // Genera y exporta un reporte estructurado en formato XML con el detalle de las tareas y la sesión de medición.
    fun exportarCsv(context: Context, sesion: SesionMedicion, tareas: List<RegistroTarea>) {
        try {
            val csvHeader = "Proceso,Subproceso,Actividad,PersonaMedida,AreaMedida,Agencia,Tarea,Comentario,Clicks,Pantallas,CamposLlenados,HojasLlenadas,Tiempo(s),TiempoTotalSesion(s)\n"
            val csvBody = StringBuilder()

            if (tareas.isEmpty()) {
                csvBody.append("${sesion.proceso},${sesion.subproceso},${sesion.actividad},${sesion.personaMedida},${sesion.areaMedida},${sesion.agencia},Sin tareas,-,0,0,0,0,0,0\n")
            } else {
                for (t in tareas) {
                    val tiempoSegundos = t.tiempoMilisegundos / 1000.0
                    // Descifra el comentario para incluirlo de manera legible en el reporte CSV.
                    val comentarioDescifrado = CryptoHelper.decrypt(t.comentario).replace(",", " ")
                    csvBody.append("${sesion.proceso},${sesion.subproceso},${sesion.actividad},${sesion.personaMedida},${sesion.areaMedida},${sesion.agencia},${t.tarea},$comentarioDescifrado,${t.clicks},${t.pantallas},${t.camposLlenados},${t.hojasLlenadas},$tiempoSegundos,\n")
                }

                val tiempoTotalSegundos = tareas.sumOf { t -> t.tiempoMilisegundos } / 1000.0
                csvBody.append("${sesion.proceso},${sesion.subproceso},${sesion.actividad},${sesion.personaMedida},${sesion.areaMedida},${sesion.agencia},Total general,-,,,,,,$tiempoTotalSegundos\n")
            }

            val fileName = "Reporte_Sesion_${sesion.id}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(csvHeader + csvBody.toString())

            compartirArchivo(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Genera un reporte formateado en texto plano (simulando un formato de lectura estructurada) con las tareas y evidencias de la sesión.
    fun exportarPdfTexto(context: Context, sesion: SesionMedicion, tareas: List<RegistroTarea>, evidencias: List<EvidenciaMultimedia>) {
        try {
            val sb = StringBuilder()
            sb.append("=== REPORTE DE SESIÓN DE MEDICIÓN ===\n\n")
            sb.append("ID Sesión: ${sesion.id}\n")
            sb.append("Proceso: ${sesion.proceso}\n")
            sb.append("Subproceso: ${sesion.subproceso}\n")
            sb.append("Actividad: ${sesion.actividad}\n")
            sb.append("Persona Medida: ${sesion.personaMedida}\n")
            sb.append("Área Medida: ${sesion.areaMedida}\n")
            sb.append("Agencia: ${sesion.agencia}\n\n")

            sb.append("--- TAREAS REGISTRADAS (${tareas.size}) ---\n")
            for ((index, t) in tareas.withIndex()) {
                val tiempoSegundos = t.tiempoMilisegundos / 1000.0
                // Descifra el comentario para mostrarlo adecuadamente en el reporte textual.
                val comentarioDescifrado = CryptoHelper.decrypt(t.comentario)

                sb.append("${index + 1}. Tarea: ${t.tarea}\n")
                sb.append("   Comentario: $comentarioDescifrado\n")
                sb.append("   Clicks: ${t.clicks} | Pantallas: ${t.pantallas} | Campos: ${t.camposLlenados} | Hojas: ${t.hojasLlenadas}\n")
                sb.append("   Tiempo (s): $tiempoSegundos\n\n")
            }

            val tiempoTotalSegundos = tareas.sumOf { t -> t.tiempoMilisegundos } / 1000.0
            sb.append("----------------------------------------\n")
            sb.append("TOTAL GENERAL DEL PROCESO: $tiempoTotalSegundos segundos\n\n")

            sb.append("--- EVIDENCIAS MULTIMEDIA (${evidencias.size}) ---\n")
            if (evidencias.isEmpty()) {
                sb.append("Sin evidencias adjuntas.\n")
            } else {
                evidencias.forEachIndexed { idx, ev ->
                    sb.append("${idx + 1}. [${ev.tipo}] ${ev.rutaArchivo}\n")
                }
            }

            val fileName = "Reporte_Sesion_${sesion.id}_${System.currentTimeMillis()}.txt"
            val file = File(context.cacheDir, fileName)
            file.writeText(sb.toString())

            compartirArchivo(context, file, "text/plain")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Empaqueta en un archivo ZIP los datos de la sesión (en formato XML) y todos sus archivos de evidencia multimedia asociados.
    fun exportarZipSesion(context: Context, sesion: SesionMedicion, tareas: List<RegistroTarea>, evidencias: List<EvidenciaMultimedia>) {
        try {
            val zipFileName = "Paquete_Completo_Sesion_${sesion.id}_${System.currentTimeMillis()}.zip"
            val zipFile = File(context.cacheDir, zipFileName)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val csvHeader = "Proceso,Subproceso,Actividad,PersonaMedida,AreaMedida,Agencia,Tarea,Comentario,Clicks,Pantallas,CamposLlenados,HojasLlenadas,Tiempo(s),TiempoTotalSesion(s)\n"
                val csvBody = StringBuilder()
                if (tareas.isNotEmpty()) {
                    for (t in tareas) {
                        val tiempoSegundos = t.tiempoMilisegundos / 1000.0
                        val comentarioDescifrado = CryptoHelper.decrypt(t.comentario).replace(",", " ")
                        csvBody.append("${sesion.proceso},${sesion.subproceso},${sesion.actividad},${sesion.personaMedida},${sesion.areaMedida},${sesion.agencia},${t.tarea},$comentarioDescifrado,${t.clicks},${t.pantallas},${t.camposLlenados},${t.hojasLlenadas},$tiempoSegundos,\n")
                    }
                    val tiempoTotalSegundos = tareas.sumOf { t -> t.tiempoMilisegundos } / 1000.0
                    csvBody.append("${sesion.proceso},${sesion.subproceso},${sesion.actividad},${sesion.personaMedida},${sesion.areaMedida},${sesion.agencia},Total general,-,,,,,,$tiempoTotalSegundos\n")
                }
                zos.putNextEntry(ZipEntry("reporte_sesion.csv"))
                zos.write((csvHeader + csvBody.toString()).toByteArray())
                zos.closeEntry()

                for (ev in evidencias) {
                    val mediaFile = File(ev.rutaArchivo)
                    if (mediaFile.exists()) {
                        zos.putNextEntry(ZipEntry("evidencias/${mediaFile.name}"))
                        mediaFile.inputStream().use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            compartirArchivo(context, zipFile, "application/zip")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Método auxiliar privado para crear un FileProvider URI y lanzar el selector de aplicaciones para compartir el archivo.
    private fun compartirArchivo(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Archivo / Reporte"))
    }
}
package com.example.smato.service

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Objeto singleton que gestiona un cronómetro global basado en corrutinas,
// permitiendo medir el tiempo transcurrido en las tareas de manera reactiva.
object CronometroService {

    // Estado privado y flujo público expuesto para emitir el tiempo acumulado en milisegundos.
    private val _tiempoMilisegundos = MutableStateFlow(0L)
    val tiempoMilisegundos: StateFlow<Long> = _tiempoMilisegundos.asStateFlow()

    // Estado privado y flujo público para indicar si el cronómetro se encuentra activo o en ejecución.
    private val _enEjecucion = MutableStateFlow(false)
    val enEjecucion: StateFlow<Boolean> = _enEjecucion.asStateFlow()

    // Referencia al trabajo (Job) de la corrutina y ámbito de ejecución independiente (CoroutineScope).
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Inicia o reanuda el conteo del cronómetro si no se encuentra activo previamente.
    fun iniciar(context: Context) {
        if (_enEjecucion.value) return

        _enEjecucion.value = true
        job?.cancel()
        job = scope.launch {
            while (_enEjecucion.value) {
                delay(100) // Incrementa el contador cada 100 milisegundos para lograr mayor fluidez en la interfaz.
                _tiempoMilisegundos.value += 100
            }
        }
    }

    // Pausa temporalmente el cronómetro deteniendo la corrutina activa.
    fun pausar(context: Context) {
        _enEjecucion.value = false
        job?.cancel()
    }

    // Reinicia el contador a cero y detiene el cronómetro (comúnmente usado al registrar una tarea completada).
    fun reiniciar(context: Context) {
        _enEjecucion.value = false
        job?.cancel()
        _tiempoMilisegundos.value = 0L
    }

    // Detiene por completo el cronómetro y limpia el estado actual (usado al finalizar y guardar la sesión de medición).
    fun detener(context: Context) {
        _enEjecucion.value = false
        job?.cancel()
        _tiempoMilisegundos.value = 0L
    }
}
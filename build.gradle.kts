// Bloque de configuración de complementos (plugins) a nivel de proyecto raíz (Project-level build.gradle.kts),
// declarando las versiones y dependencias base para la aplicación Android, Kotlin y el compilador KSP (Kotlin Symbol Processing)
// sin aplicarlas directamente de forma global (apply false).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
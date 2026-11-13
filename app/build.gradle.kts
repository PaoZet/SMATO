// Archivo de configuración de Gradle para el módulo de la aplicación (app/build.gradle.kts),
// definiendo los plugins aplicados (Android Application, Kotlin Android y KAPT),
// configuraciones de compilación, SDKs objetivo, opciones de compatibilidad con Java 17,
// el compilador de Jetpack Compose y las dependencias de la aplicación (Room, Biometrics, Zip4j, etc.).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Habilita el procesador de anotaciones Kotlin KAPT para la compilación de esquemas de Room.
}

android {
    namespace = "com.example.smato"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smato"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {

    // Configuración e integración de la base de datos local Room y su compilador mediante KAPT.
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Núcleo de AndroidX, extensiones de ciclo de vida y soporte para actividades con Compose.
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Declaración del Bill of Materials (BOM) y componentes de interfaz gráfica de Jetpack Compose.
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Dependencias para pruebas unitarias e instrumentadas (JUnit y Espresso).
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Soporte para SQLite y consultas de base de datos a bajo nivel.
    implementation("androidx.sqlite:sqlite:2.4.0")

    // Autenticación biométrica para la seguridad de acceso a la aplicación.
    implementation("androidx.biometric:biometric:1.1.0")

    // Integración de ViewModel con Jetpack Compose.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Fragmentos de extensión y utilidades de actividad.
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.biometric:biometric:1.1.0")

    // Librería externa para la compresión y cifrado avanzado de archivos ZIP (Zip4j).
    implementation("net.lingala.zip4j:zip4j:2.11.5")

}
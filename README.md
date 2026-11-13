SMATO - Sistema Móvil de Analítica y Tiempos Operativos

SMATO es una aplicación móvil nativa para Android desarrollada en Kotlin con Jetpack Compose, diseñada para transformar la medición manual de procesos operativos en analítica inteligente, segura y en tiempo real.

Opera bajo una arquitectura MVVM limpia y una filosofía Offline-First, permitiendo a operarios, talleres y empresas registrar tiempos con precisión milimétrica, adjuntar evidencias fotográficas y visualizar indicadores clave de rendimiento (KPIs) sin depender de conexión a internet.

✨ Características Principales

⏱️ Cronometraje Reactivo de Alta Precisión: Motor en memoria (CronometroService) impulsado por Corrutinas de Kotlin y StateFlow, garantizando conteo continuo cada 100ms sin bloqueos de ciclo de vida.

🔐 Cifrado Extremo a Extremo (AES-256): Protección de datos sensibles (comentarios y notas de tareas) mediante cifrado simétrico en reposo (CryptoHelper) antes de su almacenamiento en SQLite.

📦 Exportación Protegida: Generación de reportes y empaquetado de evidencias multimedia en contenedores .zip protegidos con contraseña (ZipUtils y ExportHelper).

📸 Trazabilidad con Evidencia Multimedia: Captura y vinculación estructurada de fotografías directamente a cada sesión de evaluación mediante FileProvider y la entidad EvidenciaMultimedia.

📊 Dashboard Analítico en Tiempo Real: Visualización dinámica de KPIs, sesiones completadas y tiempos acumulados calculados al vuelo mediante flujos reactivos.

🛡️ Aislamiento de Datos por Operario: Consultas SQL filtradas por usuario (usuarioEmail) que garantizan la privacidad e integridad de cada historial.

🏗️ Arquitectura y Estructura del Proyecto

El proyecto sigue el patrón arquitectónico MVVM (Model-View-ViewModel) con separación clara de responsabilidades:

com.example.smato/
├── MainActivity.kt          # Punto de entrada principal y contenedor de navegación
├── ui/                      # Capa de Vista (Interfaz Declarativa)
│   ├── screens/             # Pantallas: Login, Home, Medición, Historial y Dashboard
│   └── theme/               # Configuración de colores, tipografía y temas Material 3
├── viewmodel/               # Capa de Estado Reactivo
│   └── MedicionViewModel.kt # Puente entre la UI y Room mediante StateFlow
├── service/                 # Motores Lógicos en Memoria
│   └── CronometroService.kt # Singleton reactivo para control de tiempos
├── utils/                   # Herramientas Transversales y Seguridad
│   ├── CryptoHelper.kt      # Encriptación y desencriptación AES-256
│   ├── ExportHelper.kt      # Estructuración de reportes de datos
│   └── ZipUtils.kt          # Compresión segura con contraseña
└── data.local/              # Capa de Persistencia Local (Offline-First)
    ├── AppDatabase.kt       # Instancia Singleton de RoomDatabase
    ├── dao/                 # Data Access Objects (MedicionDao, UsuarioDao, etc.)
    └── entities/            # Modelos relacionales SQLite (Sesiones, Tareas, Evidencias)


🛠️ Stack Tecnológico

Lenguaje: Kotlin

UI Toolkit: Jetpack Compose (Material Design 3)

Arquitectura: MVVM (Model-View-ViewModel)

Programación Asíncrona: Kotlin Coroutines & Flow (StateFlow)

Base de Datos Local: Room Persistence Library (SQLite)

Seguridad: Java Cryptography Extension (JCE) con AES-256 + FileProvider

🚀 Instalación y Ejecución Local

Si deseas clonar y ejecutar este proyecto en tu entorno de desarrollo:

Clona el repositorio:

git clone https://github.com/tu-usuario/SMATO.git


Abre el proyecto en Android Studio:

Selecciona File > Open y busca la carpeta raíz del proyecto SMATO.

Sincroniza Gradle:

Espera a que Android Studio descargue las dependencias de Kotlin, Compose y Room.

Ejecuta la aplicación:

Conecta un dispositivo físico Android (con depuración USB activada) o inicia un emulador (API 26+) y presiona Run (Shift + F10).

📄 Licencia

Este proyecto fue desarrollado con fines académicos, profesionales y de optimización operativa. Todos los derechos reservados.

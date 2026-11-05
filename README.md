# AntiFraude - Llamadas en Falso

## Descripción general

Este proyecto es una aplicación Android desarrollada con Kotlin y Jetpack Compose para detectar y mitigar llamadas sospechosas o fraudulentas. La app combina permisos del sistema, servicios de filtrado de llamadas, almacenamiento local en memoria y un panel de configuración para activar reglas de bloqueo según distintos perfiles de protección.

La solución busca emular un escenario real de protección telefónica con tres capas principales:

- filtrado por lista manual de números bloqueados,
- validación basada en reglas operativas (contactos, verificación del número, modo seguro),
- detección heurística de patrones sospechosos con una lógica tipo IA simulada.

## Objetivo del proyecto

El objetivo principal es ofrecer una experiencia de defensa contra llamadas spam, telemarketing o intentos de fraude, permitiendo al usuario:

- activar un servicio de filtrado de llamadas entrantes,
- revisar el historial de llamadas interceptadas,
- gestionar recetas o perfiles de protección,
- exportar o visualizar un resumen de incidentes reportados.

## Stack técnico

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- AndroidX Lifecycle, Navigation 3, DataStore, Room (configuración preparada)
- Coroutines + Flow
- CallScreeningService de Android Telecom
- Permissions y overlays del sistema
- Gradle + Kotlin DSL

## Arquitectura

La app sigue una estructura modular simple orientada a una arquitectura de capas con repositorios y estado reactivo:

```text
UI (Compose Screens)
    ↓
MainApp / Navigation / ViewState
    ↓
Repositories (SettingsRepository, RecipeRepository, CallLogRepository)
    ↓
Android Services / System APIs / DataStore / Flow
```

### Componentes principales

#### 1. MainActivity
Es el punto de entrada de la aplicación. Inicia la UI principal y carga `MainApp()`, que define la navegación entre pantallas y la barra inferior de acceso.

#### 2. MainApp
Centraliza la navegación de la aplicación:

- Home: onboarding y validación de permisos.
- CallLog: lista de llamadas bloqueadas o identificadas.
- RecipeHub: configuración de filtros y perfiles de protección.
- Informe: diálogo de resumen para reportar incidentes.

#### 3. Repositorios
Los repositorios gestionan el estado reactivo usando `StateFlow`:

- `RecipeRepository`: define los filtros activos (por ejemplo, Filtro Agresivo, Modo Confianza, Protección IA).
- `CallLogRepository`: almacena las llamadas registradas y los números bloqueados manualmente.
- `SettingsRepository`: persiste preferencias del usuario, como el correo de notificación/reporte.

#### 4. Servicios del sistema

- `AppCallScreeningService`: servicio principal encargado de decidir si una llamada entrante debe bloquearse o permitirse.
- `CallOverlayService`: muestra una capa visual con información sobre la llamada para identificación del número u origen.
- `CallReceiver`: escucha cambios de estado del teléfono y complementa la lógica de acceso al sistema.

## Flujo de detección de llamadas

Cuando llega una llamada entrante, la app ejecuta el siguiente proceso:

1. `AppCallScreeningService.onScreenCall()` recibe los detalles del número y del estado del teléfono.
2. Se valida si el número está en la lista de bloqueo manual.
3. Se comprueba si el número está en contactos para permitir el paso directo.
4. Se aplican filtros configurados por el usuario:
   - Filtro Agresivo
   - Modo Confianza
   - Protección IA
5. Si la llamada cumple criterios sospechosos, se registra en `CallLogRepository`.
6. Se activa el overlay para informar al usuario y se responde con `CallResponse` (permitir o bloquear).

## Lógica de protección actual

La lógica implementada incluye:

- bloqueo manual por número,
- umbral de verificación del número no verificado,
- exclusión de contactos para evitar falsos positivos,
- detección simple de patrones de spam mediante reglas heurísticas:
  - prefijos conocidos (`800`, `900`, `01800`),
  - secuencias repetitivas (`000`),
  - números excesivamente largos.

La lógica de IA es una simulación de análisis basada en patrones y no un modelo real de aprendizaje automático entrenado.

## Estructura de directorios

```text
app/
├── src/main/java/com/example/myapplication/
│   ├── data/
│   │   ├── CallLogEntry.kt
│   │   ├── CallLogRepository.kt
│   │   ├── RecipeRepository.kt
│   │   ├── SettingsRepository.kt
│   │   └── CallRecipe.kt
│   ├── service/
│   │   ├── AppCallScreeningService.kt
│   │   ├── CallOverlayService.kt
│   │   └── CallReceiver.kt
│   ├── ui/
│   │   ├── HomeScreen.kt
│   │   ├── CallLogScreen.kt
│   │   ├── RecipeHubScreen.kt
│   │   ├── ReportDialog.kt
│   │   └── theme/
│   ├── MainActivity.kt
│   ├── MainApp.kt
│   └── Destinations.kt
├── src/main/AndroidManifest.xml
├── src/main/res/
├── build.gradle.kts
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── gradle.properties
```

## Requisitos

- Android Studio
- JDK 11 o superior
- SDK mínimo: 24
- SDK objetivo: 36
- Dispositivo o emulador Android con permisos del sistema para call screening y overlays

## Cómo ejecutar

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Sincronizar Gradle.
4. Seleccionar un emulador o dispositivo físico.
5. Ejecutar la tarea `app` o presionar Run.

## Permisos relevantes

La aplicación requiere permisos de acceso al estado de llamadas, contactos, bloqueo de batería y visualización sobre otras apps, además del rol del sistema `ROLE_CALL_SCREENING` para actuar como servicio de filtrado.

Estos permisos se revisan y se solicitan desde `HomeScreen`, con flujo guiado para usuarios que no los tengan habilitados.

## Estado actual del proyecto

El proyecto se encuentra en una etapa de prototipo funcional orientado a demostración. La lógica es principalmente local y simula comportamiento real de defensa contra spam. No incluye integración con backend, servicio cloud ni modelo de inteligencia artificial entrenado.

## Limitaciones y mejoras futuras

- Sustituir la lógica heurística por un motor IA real o servicio externo.
- Persistir registros en base de datos local (Room) para trazabilidad y recuperación.
- Añadir análisis de reputación de números, listas negras externas y telemetría.
- Mejorar la gestión de seguridad para evitar falsos positivos con contactos críticos.
- Añadir pruebas unitarias e instrumentadas para validar la lógica de filtrado.
- Centralizar la configuración de reglas y exportación de reportes.

## Conclusión

AntiFraude es un prototipo Android de protección de llamadas basado en Android Telecom y Jetpack Compose. Su valor principal es demostrar cómo una app puede interceptar llamadas entrantes, aplicar reglas de seguridad, identificar patrones sospechosos y presentar al usuario un dashboard operativo simple y visual. Es una base sólida para evolucionar hacia una solución más robusta, segura y escalable.

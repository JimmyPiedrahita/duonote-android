# DuoNote Android

Aplicación móvil para Android que forma parte del ecosistema **DuoNote**, diseñada para sincronizar notas y tareas en tiempo real con la aplicación de escritorio [DuoNote Desktop](https://github.com/JimmyPiedrahita/duonote).

## 📋 Descripción

DuoNote Android es la contraparte móvil del sistema DuoNote. Permite crear, gestionar y sincronizar notas instantáneamente con tu PC. Olvídate de enviarte mensajes a ti mismo o usar métodos complicados para pasar información entre tu teléfono y tu computadora; con DuoNote, tus notas están siempre sincronizadas.

### 🔗 ¿Cómo funciona la conexión?

1. Abre DuoNote Desktop en tu PC
2. Se generará un código QR y un código único de conexión
3. Desde esta app, escanea el QR o ingresa el código manualmente
4. ¡Listo! Tus notas se sincronizarán automáticamente en tiempo real

## ✨ Características

- **Sincronización en Tiempo Real:** Utiliza Firebase Realtime Database para mantener tus notas actualizadas al instante en todos tus dispositivos conectados
- **Conexión Rápida:** Conecta con tu PC escaneando un código QR o ingresando un código único manualmente
- **Gestión Completa de Notas:**
  - Crear nuevas notas rápidamente
  - Marcar como completadas/pendientes con un toque
  - Copiar texto al portapapeles con un botón dedicado
  - Eliminar con doble toque
- **Widget de Escritorio:** Visualiza tus notas pendientes directamente desde la pantalla de inicio de Android
- **Persistencia de Sesión:** Mantiene tu sesión iniciada y recupera tus notas automáticamente

## 🛠️ Tecnologías

Este proyecto está construido 100% en **Kotlin** y utiliza:

- **Firebase Realtime Database** - Almacenamiento y sincronización de datos
- **CameraX & ML Kit** - Escaneo de códigos QR
- **Android Widgets** - Widget en pantalla de inicio
- **Coroutines & Flow** - Manejo de asincronía y reactividad
- **Material Design** - Interfaz de usuario moderna
- **DataStore** - Almacenamiento local de preferencias y sesión

## 📋 Requisitos Previos

1. **Android Studio** (Koala o superior recomendado)
2. Un proyecto en **Firebase Console**
3. El archivo `google-services.json` de tu proyecto de Firebase

## ⚙️ Instalación

1. Clona el repositorio:
   ```bash
   git clone https://github.com/JimmyPiedrahita/duonote-android.git
   ```

2. Abre el proyecto en Android Studio

3. Copia tu archivo `google-services.json` en la carpeta `app/`

4. Sincroniza el proyecto con Gradle

5. Ejecuta la aplicación en tu emulador o dispositivo físico

## 📱 Uso

1. **Conectar:** Al abrir la app, escanea el código QR de DuoNote Desktop o ingresa el código de conexión manualmente

2. **Crear Notas:** Usa el botón flotante (+) para agregar nuevas notas

3. **Gestionar Notas:**
   - Un toque para marcar como completada/pendiente
   - Icono de copiar para copiar el texto
   - Doble toque para eliminar

4. **Widget:** Mantén presionada tu pantalla de inicio → Widgets → DuoNote

## 📂 Estructura del Proyecto

```
duonote-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/    # Código fuente Kotlin
│   │   ├── res/                 # Recursos (layouts, drawables, valores)
│   │   └── AndroidManifest.xml  # Configuración de la app
│   └── build.gradle.kts         # Dependencias del módulo
├── gradle/                      # Configuración de Gradle
└── build.gradle.kts             # Configuración del proyecto
```

## 💻 Aplicación Complementaria

Para aprovechar al máximo DuoNote, instala la aplicación de escritorio:

**[DuoNote Desktop](https://github.com/JimmyPiedrahita/duonote)** - Gestiona tus notas desde tu PC con Windows, ventana fijada siempre visible y generación de códigos QR para conexión instantánea.

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**.

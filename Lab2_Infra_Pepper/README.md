## Guía de preparación y ejecución de proyectos Pepper

### 1. Instalar Android Studio

Usar exactamente:

**Android Studio Arctic Fox | 2020.3.1 Patch 4**

Durante la instalación de Android Studio se pueden dejar las opciones estándar.

---

### 2. Instalar Android SDK API 30

En Android Studio:

**File → Settings → Appearance & Behavior → System Settings → Android SDK**

En **SDK Platforms**, instalar:

```
Android 11.0 (R)
API Level 30
```

En **SDK Tools**, asegurarse de tener los componentes necesarios para API 30, incluyendo:

```
Android SDK Build-Tools 30.0.2
Android SDK Platform-Tools
Android SDK Tools
Android Emulator
```

---

### 3. Instalar Pepper SDK Plugin

En Android Studio:

**File → Settings → Plugins**

Buscar e instalar:

```
Pepper SDK
```

Después debería aparecer el menú:

```
Tools → Pepper SDK
```

---

### 4. Instalar Robot SDK

Ir a:

**Tools → Pepper SDK → Robot SDK Manager**

Instalar:

```
Robot SDK API 7
QiSDK 1.7.5
Tools 2.9.6.27
Android Virtual Image 2019-10-29
```

La combinación que usé y que funcionó fue:

```
API: 7
QiSDK: 1.7.5
Tools: 2.9.6.27
Virtual Image: 2019-10-29
```

---

### 5. Verificar Pepper Emulator

**Tools → Pepper SDK → Emulator**

Deben abrirse:

```
Android Emulator
Robot Viewer
```

---

# Proyecto 1 — HelloHuman

### 6. Abrir HelloHuman

En Android Studio:

**File → Open**

Seleccionar la carpeta completa:

```
HelloHuman
```

---

### 7. Configuración esperada de HelloHuman

El proyecto que dejé funcionando usa:

```
compileSdkVersion 30
minSdkVersion 23
targetSdkVersion 30
Robot SDK API 7
QiSDK 1.7.5
```

En `app/build.gradle` deben existir:

```
implementation 'com.aldebaran:qisdk:1.7.5'
implementation 'com.aldebaran:qisdk-design:1.7.5'
```

Y en `settings.gradle`:

```
maven {
    url 'http://android.aldebaran.com/sdk/maven'
    allowInsecureProtocol = true
}
```

En:

```
app/src/main/assets/robot/robotsdk.xml
```

debe estar:

```
<robot>
    <users-robotsdk minSdkVersion='API 7'/>
</robot>
```

---

### 8. Correr HelloHuman

Primero iniciar:

**Tools → Pepper SDK → Emulator**

Después, arriba en Android Studio:

```
HelloHuman / app
Device: emulator-XXXX
```

Presionar:

**Run**

Para verificar el resultado:

**Robot Viewer → Dialog view**

Debe aparecer:

```
Robot: Hello human!
```

---

# Proyecto 2 — Next Steps / qisdk-tutorials

### 9. Descargar qisdk-tutorials

Repositorio:

```
aldebaran/qisdk-tutorials
```

Descargar como ZIP y extraerlo.

Importante: abrir en Android Studio la carpeta interna que realmente contiene:

```
app
build.gradle
settings.gradle
gradle
```

---

### 10. Abrir qisdk-tutorials

En Android Studio:

**File → Open**

Seleccionar la carpeta raíz real del proyecto.

Esperar a que termine:

```
Gradle Sync
```

Si el Pepper Emulator ya está abierto desde `HelloHuman`, se puede dejar abierto y reutilizarlo.

---

### 11. Correr qisdk-tutorials

Arriba seleccionar:

```
app
Device: emulator-XXXX
```

Presionar:

**Run**

La aplicación debe mostrar las categorías de tutoriales.

Para probar el ejemplo:

```
Let's talk
→ Basic
→ Hello human
```

---

# Proyecto 3 — Pepper Gamepad

### 12. Descargar Pepper Gamepad

Descargar el repositorio:

```
softbankrobotics-labs/pepper-gamepad
```

Extraer el ZIP.

Abrir en Android Studio la carpeta raíz que contiene:

```
pepper-gamepad
sample-app
build.gradle
settings.gradle
```

En nuestro caso el proyecto aparecía como:

```
pepper-gamepad-root
```

---

### 13. Esperar Gradle Sync

Al abrirlo, Android Studio debe mostrar dos módulos principales:

```
pepper-gamepad
sample-app
```

No ejecutar `pepper-gamepad`.

El módulo que se corre es:

```
sample-app
```

---

### 14. Correr Pepper Gamepad

Con Pepper Emulator abierto, arriba seleccionar:

```
sample-app
Device: emulator-XXXX
```

Presionar:

**Run**

En el emulador debe aparecer la pantalla:

```
HOW TO PAIR THE CONTROLLER
```

---

# Proyecto 4 — Pepper Follow Me

### 15. Descargar Pepper Follow Me

Descargar el repositorio:

```
softbankrobotics-labs/pepper-follow-me
```

Extraer el ZIP.

Abrir la carpeta raíz que contiene:

```
follow-me
sample-app
build.gradle
settings.gradle
```

---

### 16. Esperar Gradle Sync

Android Studio debe mostrar dos módulos principales:

```
follow-me
sample-app
```

No ejecutar `follow-me`.

El módulo ejecutable es:

```
sample-app
```

---

### 17. Correr Pepper Follow Me

Con el Pepper Emulator abierto:

```
Run configuration: sample-app
Device: emulator-XXXX
```

Presionar:

**Run**

La tablet virtual debe mostrar:

```
Looking for human...
```

---

# Orden

Ejecutar en este orden:

1. `HelloHuman`

2. `qisdk-tutorials`

3. `pepper-gamepad` → correr `sample-app`

4. `pepper-follow-me` → correr `sample-app`

Recordar que hay que tener instalados:

```
Android Studio Arctic Fox 2020.3.1 Patch 4
Android SDK API 30
Build Tools 30.0.2
Pepper SDK Plugin
Robot SDK API 7
QiSDK 1.7.5
Tools 2.9.6.27
Pepper Virtual Image 2019-10-29
```

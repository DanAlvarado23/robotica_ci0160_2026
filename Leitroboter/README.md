# Leitroboter

Aplicación Android desarrollada para el robot Pepper, cuyo objetivo es ayudar a los usuarios a encontrar lugares dentro del edificio anexo de la ECCI mediante la presentación de mapas e información descriptiva.

Actualmente, la aplicación se encuentra en una primera versión de prueba. El usuario puede escribir el nombre de un lugar y la aplicación muestra su mapa y descripción correspondientes.

## Estructura del proyecto

```text
Leitroboter/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/leitroboter/
│           │   ├── MainActivity.kt
│           │   └── data/
│           │       ├── Place.kt
│           │       └── PlacesCatalog.kt
│           ├── res/
│           │   ├── drawable/
│           │   ├── layout/
│           │   └── values/
│           └── AndroidManifest.xml
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

## Componentes principales

### `MainActivity.kt` — Código principal

Controla el flujo principal de la aplicación y la navegación entre las pantallas.

Sus responsabilidades incluyen:
- Mostrar el menú principal.
- Solicitar el nombre del lugar al usuario.
- Procesar la solicitud y buscar una coincidencia en el catálogo.
- Mostrar el mapa y la descripción del lugar encontrado.
- Gestionar los casos en los que no se encuentra el lugar solicitado.

### `data/Place.kt` — Clase de datos

Define la estructura de información de un lugar.

Cada lugar contiene:
- **name:** nombre del lugar.
- **mapImageRes:** referencia al recurso de imagen del mapa.
- **description:** descripción breve del lugar.
- **aliases:** nombres alternativos que permiten identificar el lugar.

### `data/PlacesCatalog.kt` — Catálogo de lugares

Contiene los datos de los lugares disponibles en la aplicación.

Cada entrada del catálogo relaciona el nombre del lugar con su mapa, descripción y posibles nombres alternativos.

### `drawable/` — Imágenes

Contiene los recursos gráficos utilizados por la aplicación, incluidos los mapas de los lugares y las imágenes de la interfaz.

Los mapas se asocian con los datos definidos en el catálogo de lugares.

### `layout/` — Pantallas de la aplicación

Contiene los archivos XML que definen la estructura visual de las pantallas.

Entre ellas se encuentran:
- `activity_main.xml`: menú principal.
- `activity_listen.xml`: pantalla de entrada de texto para indicar el lugar solicitado.
- `screen_place.xml`: pantalla que presenta el nombre, el mapa y la descripción del lugar.

## Flujo de funcionamiento

1. El usuario abre la aplicación.
2. Selecciona la opción para buscar un lugar.
3. Escribe el nombre del lugar solicitado.
4. La aplicación busca una coincidencia en el catálogo.
5. Si encuentra el lugar, muestra su mapa y descripción.
6. Si no encuentra una coincidencia, informa al usuario.

## Estado actual y desarrollo futuro

La primera versión utiliza entrada de texto para simular la solicitud del usuario.

Las mejoras previstas incluyen:
- Integrar reconocimiento de voz.
- Mejorar la interpretación de solicitudes en lenguaje natural.
- Incorporar un modelo de lenguaje para comprender solicitudes más flexibles.
- Probar e integrar la aplicación con el robot Pepper físico.


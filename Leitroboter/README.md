# Leitroboter

Aplicación Android desarrollada para el robot Pepper, cuyo objetivo es ayudar a los usuarios a encontrar lugares dentro del edificio anexo de la ECCI mediante la presentación de mapas e información descriptiva.

El usuario puede escribir o decir el destino. La aplicación muestra su mapa, descripción e indicaciones.

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
- **id:** identificador compartido con el servidor.
- **name:** nombre del lugar.
- **roomNumber:** número de aula, cuando corresponde.
- **floor:** piso del edificio anexo (1 a 6). Si el destino queda en otro piso, Pepper explica dónde empieza el recorrido antes de señalar el primer giro. `pepper_floor`, en `res/values/config.xml`, indica el piso donde está el robot.
- **group:** (opcional en `destinations.json`) une el mismo tipo de lugar en varios pisos, por ejemplo `banos` o `sala_reuniones`. Si la persona pide uno de esos lugares sin decir el piso, la aplicación muestra la lista de pisos para elegir; si dice el piso ("baños del piso 2"), va directo. Un alias solo puede repetirse entre lugares del mismo grupo.
- **shortName:** (opcional) nombre corto para los botones de «Explorar por piso», por ejemplo "Lab. Robótica Kryscia". Máximo 32 caracteres.
- **mapImageRes:** plano base de su piso, compartido por todos los destinos de ese piso.
- **routes:** recorridos con una etiqueta y una lista de puntos. Las coordenadas van de 0 a 1 respecto al ancho y alto del plano. La consulta general de robótica y las entradas del edificio muestran dos recorridos.
- **description:** descripción breve del lugar.
- **directions:** indicaciones desde el ascensor. Todas usan el mismo marco de referencia: la persona está **de frente al ascensor** (mirando sus puertas), que es como se ve en los mapas, con el ascensor arriba. Así, un giro a la derecha en la indicación es una flecha hacia la derecha en el mapa. Solo el primer giro depende de este marco; los giros y lados siguientes se dicen según la dirección en que la persona va caminando.
- **gestureAnimationRes:** animación de `res/raw` con la que Pepper señala el primer giro mientras dice las indicaciones (`gesture` en `destinations.json`: `right`, `left`, `back` o `null`). Pepper está a la par del ascensor, de frente a la persona, así que para la derecha de la persona levanta su brazo izquierdo (`point_right.qianim`) y viceversa.
- **aliases:** lista de nombres alternativos (`List<String>` en Kotlin). Por ejemplo, «oficina de Kryscia» y «6-4» llevan al mismo destino.

### `data/PlacesCatalog.kt` — Catálogo de lugares

Contiene los datos de los lugares disponibles en la aplicación.

`node scripts/generate-catalog.mjs` genera el catálogo a partir de `destinations.json`. La app usa un índice de alias normalizados para las búsquedas exactas y puede resolverlas sin red. Si se indica un aula o un piso concreto, la app respeta esa selección. Cuando hay varios destinos posibles, pide el piso.

Al añadir destinos, crecen las listas de alias y los recorridos. Cada piso sigue usando un solo plano. Un JSON cargado en la app evitaría regenerar el Kotlin, pero requeriría leer y validar el archivo al iniciar. SQLite permitiría consultar catálogos más grandes por piso o alias y añadiría un esquema que mantener. El catálogo generado es suficiente para los seis pisos del anexo y permite validar los datos antes de compilar.

### `drawable/` — Imágenes

Cada piso tiene un plano base. Los primeros cinco son vectoriales y el sexto usa el PNG del nivel 500. `RouteMapView` dibuja los recorridos, las flechas y las etiquetas sobre el plano usando la misma escala.

### `layout/` — Pantallas de la aplicación

Contiene los archivos XML que definen la estructura visual de las pantallas.

Entre ellas se encuentran:
- `activity_main.xml`: menú principal.
- `activity_listen.xml`: pantalla de entrada por texto o voz para indicar el lugar solicitado.
- `screen_place.xml`: pantalla que presenta el nombre, el mapa y la descripción del lugar.
- `screen_list.xml`: lista de opciones para "Explorar por piso" (pisos y lugares de cada piso) y para elegir el piso cuando un lugar se repite.

## Flujo de funcionamiento

1. El usuario abre la aplicación.
2. Selecciona la opción para buscar un lugar.
3. Escribe o dice el destino solicitado.
4. La aplicación busca una coincidencia en el catálogo.
5. Si encuentra el lugar, muestra su mapa e indicaciones. El botón «Consultar otra ubicación» abre una nueva búsqueda.
6. Si no encuentra una coincidencia, informa al usuario.

## Consultas al modelo

La app resuelve las coincidencias exactas con el catálogo. El servidor valida las demás consultas y pide a Gemini que elija un ID del anexo o devuelva `NO_MATCH`. El texto de la persona se envía como dato separado de las instrucciones, y el servidor valida el ID recibido antes de responder. Una petición como «Reverse una lista en Python» se rechaza con el filtro de entrada antes de llegar al clasificador. Para una consulta por voz, el servidor transcribe el clip primero y aplica el filtro al texto resultante.

## Configuración y pruebas

El servidor usa `server/.env`, siguiendo `server/.env.example`. Para recibir consultas de Pepper debe escuchar en una dirección accesible desde su red y tener un token de al menos 32 caracteres. La clave de Gemini se guarda únicamente en el servidor.

La app toma `DESTINATION_API_URL` y `DESTINATION_API_TOKEN` de las propiedades de Gradle o de `local.properties`. Para Pepper se usa `http://<IP de la computadora>:8787/match` con un APK debug; una versión release requiere HTTPS. `10.0.2.2` corresponde al emulador. Estos archivos de configuración local están excluidos de Git.

Después de editar el catálogo, ejecutar:

```text
node scripts/generate-catalog.mjs
node scripts/generate-catalog.mjs --check
node --test server/matcher.test.mjs
gradlew testDebugUnitTest assembleDebug
```

`SilenceDetector` conserva los primeros bloques para detectar palabras dichas durante la calibración. Una parada manual tampoco envía audio si no hubo voz. Los umbrales se ajustan con el ruido de Pepper; las pruebas unitarias y el simulador web no sustituyen esa comprobación.


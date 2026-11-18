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
- **floor:** piso del edificio anexo (1 a 6). Cuando el lugar no está en el piso de Pepper (`res/values/config.xml`, `pepper_floor`, hoy 4), el título del mapa muestra el piso y Pepper dice "Usted quiere ir a X, en el piso N"; no hace gesto con el brazo. El ascensor está en la misma posición en todos los pisos, así que las indicaciones de cada piso empiezan igual: de frente al ascensor de ese piso.
- **group:** (opcional en `destinations.json`) une el mismo tipo de lugar en varios pisos, por ejemplo `banos` o `sala_reuniones`. Si la persona pide uno de esos lugares sin decir el piso, la aplicación muestra la lista de pisos para elegir; si dice el piso ("baños del piso 2"), va directo. Un alias solo puede repetirse entre lugares del mismo grupo.
- **shortName:** (opcional) nombre corto para los botones de "Explorar por piso" cuando el nombre completo no cabe en dos líneas, por ejemplo "Lab. Robótica Kryscia". Máximo 32 caracteres.
- **mapImageRes:** referencia al recurso de imagen del mapa.
- **description:** descripción breve del lugar.
- **directions:** indicaciones desde el ascensor. Todas usan el mismo marco de referencia: la persona está **de frente al ascensor** (mirando sus puertas), que es como se ve en los mapas, con el ascensor arriba. Así, un giro a la derecha en la indicación es una flecha hacia la derecha en el mapa. Solo el primer giro depende de este marco; los giros y lados siguientes se dicen según la dirección en que la persona va caminando.
- **gestureAnimationRes:** animación de `res/raw` con la que Pepper señala el primer giro mientras dice las indicaciones (`gesture` en `destinations.json`: `right`, `left`, `back` o `null`). Pepper está a la par del ascensor, de frente a la persona, así que para la derecha de la persona levanta su brazo izquierdo (`point_right.qianim`) y viceversa.
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


# Simulador de la tableta

Página web que imita las pantallas de la app en la tableta de Pepper (1280 × 800) para revisar
tamaños y textos sin abrir el emulador. Lee `destinations.json`, los mapas y las fuentes del proyecto.

Desde la carpeta `Leitroboter`:

```
node simulador/server.mjs
```

y abrir http://localhost:8080. Arriba se puede cambiar el piso donde está Pepper; lo que Pepper
diría aparece debajo de la tableta.

Diferencias con la app: la búsqueda es por texto (nombre, número o alias), sin Gemini ni micrófono,
y los tamaños están copiados a mano de `res/layout` (si cambia uno allá, cámbielo en `index.html`).

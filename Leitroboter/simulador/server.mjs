// Simulador web de la tableta de Pepper (1280 x 800). Uso, desde la carpeta Leitroboter:
//   node simulador/server.mjs      y abrir http://localhost:8080
// Lee destinations.json, los mapas y las fuentes directamente del proyecto,
// así que siempre muestra los datos actuales. Solo para revisar la interfaz.
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const res = path.join(root, 'app', 'src', 'main', 'res');
const port = Number(process.env.PORT) || 8080;
const types = { '.html': 'text/html; charset=utf-8', '.json': 'application/json; charset=utf-8', '.jpg': 'image/jpeg', '.png': 'image/png', '.ttf': 'font/ttf' };

function resolve(url) {
  const name = decodeURIComponent(url.split('?')[0]);
  if (name === '/' || name === '/index.html') return path.join(here, 'index.html');
  if (name === '/destinations.json') return path.join(root, 'destinations.json');
  const file = path.basename(name);
  if (name.startsWith('/maps/')) return path.join(res, 'drawable', file);
  if (name.startsWith('/fonts/')) return path.join(res, 'font', file);
  return null;
}

http.createServer(async (req, reply) => {
  const file = resolve(req.url);
  try {
    if (!file) throw new Error('not found');
    // Algunos mapas son .png (p. ej. mapa_default): si no está el .jpg, se busca el .png.
    const body = await readFile(file).catch(() => readFile(file.replace(/\.jpg$/, '.png')));
    reply.writeHead(200, { 'Content-Type': body[0] === 0x89 ? 'image/png' : types[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    reply.end(body);
  } catch {
    reply.writeHead(404); reply.end('No encontrado');
  }
}).listen(port, '127.0.0.1', () => console.log(`Simulador listo en http://localhost:${port}`));

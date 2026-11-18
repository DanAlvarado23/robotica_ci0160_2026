import fs from 'node:fs';

const root = new URL('../', import.meta.url);
const catalog = JSON.parse(fs.readFileSync(new URL('destinations.json', root), 'utf8'));
if (!Array.isArray(catalog) || !catalog.length) throw new Error('Catálogo vacío o inválido');
// Gesto que hace Pepper según el primer giro, visto por la persona de frente al ascensor.
const gestures = {right: 'R.raw.point_right', left: 'R.raw.point_left', back: 'R.raw.point_back'};
// Pisos del edificio anexo. Pepper está en el piso 4; los demás se indican desde el ascensor de cada piso.
const floors = [1, 2, 3, 4, 5, 6];
const identifiers = new Set();
const groupFloors = new Map();
for (const place of catalog) {
  if (!/^[a-z0-9_]{1,64}$/.test(place.id) || identifiers.has(place.id) ||
      !/^[a-z][a-z0-9_]*$/.test(place.map) ||
      !['name','description','directions'].every(key => typeof place[key] === 'string' && place[key].trim()) ||
      !(place.roomNumber === null || typeof place.roomNumber === 'string') ||
      !floors.includes(place.floor) ||
      !(place.group === undefined || /^[a-z0-9_]{1,64}$/.test(place.group)) ||
      // Nombre corto opcional para los botones de "Explorar por piso" cuando el nombre es largo.
      !(place.shortName === undefined || (typeof place.shortName === 'string' && place.shortName.trim() && place.shortName.length <= 32)) ||
      !(place.gesture === null || Object.hasOwn(gestures, place.gesture)) ||
      !Array.isArray(place.aliases) || !place.aliases.length ||
      place.aliases.some(alias => typeof alias !== 'string' || !alias.trim())) {
    throw new Error('Destino inválido: ' + (place.id || '(sin ID)'));
  }
  identifiers.add(place.id);
  // Un grupo reúne el mismo tipo de lugar en varios pisos (ej. baños); no puede repetir piso.
  if (place.group !== undefined) {
    const used = groupFloors.get(place.group) ?? new Set();
    if (used.has(place.floor)) throw new Error('Grupo repetido en el mismo piso: ' + place.id);
    groupFloors.set(place.group, used.add(place.floor));
  }
}

const quote = value => value === null ? 'null' : JSON.stringify(value).replaceAll('$', '\\$');
const entries = catalog.map(p => '        Place(\n' +
  '            id = ' + quote(p.id) + ',\n' +
  '            name = ' + quote(p.name) + ',\n' +
  '            shortName = ' + quote(p.shortName ?? null) + ',\n' +
  '            roomNumber = ' + quote(p.roomNumber) + ',\n' +
  '            floor = ' + p.floor + ',\n' +
  '            group = ' + quote(p.group ?? null) + ',\n' +
  '            mapImageRes = R.drawable.' + p.map + ',\n' +
  '            description = ' + quote(p.description) + ',\n' +
  '            directions = ' + quote(p.directions) + ',\n' +
  '            gestureAnimationRes = ' + (p.gesture === null ? 'null' : gestures[p.gesture]) + ',\n' +
  '            aliases = listOf(' + p.aliases.map(quote).join(', ') + ')\n' +
  '        )').join(',\n');
const output = 'package com.example.leitroboter.data\n\nimport com.example.leitroboter.R\n\n' +
  '// Generado desde destinations.json: node scripts/generate-catalog.mjs\n' +
  'object PlacesCatalog {\n    val places = listOf(\n' + entries + '\n    )\n}\n';
const target = new URL('app/src/main/java/com/example/leitroboter/data/PlacesCatalog.kt', root);
if (process.argv.includes('--check')) {
  if (fs.readFileSync(target, 'utf8').replaceAll('\r\n', '\n') !== output) {
    throw new Error('Catálogo Kotlin desactualizado: ejecutar node scripts/generate-catalog.mjs');
  }
} else {
  fs.mkdirSync(new URL('app/src/main/java/com/example/leitroboter/data/', root), {recursive:true});
  fs.writeFileSync(target, output);
}

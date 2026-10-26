import fs from 'node:fs';

const root = new URL('../', import.meta.url);
const catalog = JSON.parse(fs.readFileSync(new URL('destinations.json', root), 'utf8'));
if (!Array.isArray(catalog) || !catalog.length) throw new Error('Catálogo vacío o inválido');
const identifiers = new Set();
for (const place of catalog) {
  if (!/^[a-z0-9_]{1,64}$/.test(place.id) || identifiers.has(place.id) ||
      !/^[a-z][a-z0-9_]*$/.test(place.map) ||
      !['name','description','directions'].every(key => typeof place[key] === 'string' && place[key].trim()) ||
      !(place.roomNumber === null || typeof place.roomNumber === 'string') ||
      !Array.isArray(place.aliases) || !place.aliases.length ||
      place.aliases.some(alias => typeof alias !== 'string' || !alias.trim())) {
    throw new Error('Destino inválido: ' + (place.id || '(sin ID)'));
  }
  identifiers.add(place.id);
}

const quote = value => value === null ? 'null' : JSON.stringify(value).replaceAll('$', '\\$');
const entries = catalog.map(p => '        Place(\n' +
  '            id = ' + quote(p.id) + ',\n' +
  '            name = ' + quote(p.name) + ',\n' +
  '            roomNumber = ' + quote(p.roomNumber) + ',\n' +
  '            mapImageRes = R.drawable.' + p.map + ',\n' +
  '            description = ' + quote(p.description) + ',\n' +
  '            directions = ' + quote(p.directions) + ',\n' +
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

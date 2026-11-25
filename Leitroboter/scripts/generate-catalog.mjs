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
const hasMap = map => fs.existsSync(new URL(`app/src/main/res/drawable/${map}.xml`, root)) ||
  fs.existsSync(new URL(`app/src/main/res/drawable-nodpi/${map}.png`, root));
for (const place of catalog) {
  if (!/^[a-z0-9_]{1,64}$/.test(place.id) || identifiers.has(place.id) ||
      place.map !== `mapa_piso_${place.floor}` ||
      !hasMap(place.map) ||
      !['name','description','directions'].every(key => typeof place[key] === 'string' && place[key].trim()) ||
      !(place.roomNumber === null || typeof place.roomNumber === 'string') ||
      !floors.includes(place.floor) ||
      !(place.group == null || (typeof place.group === 'string' && /^[a-z0-9_]{1,64}$/.test(place.group))) ||
      // Nombre corto opcional para los botones de "Explorar por piso" cuando el nombre es largo.
      !(place.shortName == null || (typeof place.shortName === 'string' && place.shortName.trim() && place.shortName.length <= 32)) ||
      !(place.gesture === null || Object.hasOwn(gestures, place.gesture)) ||
      !Array.isArray(place.aliases) || !place.aliases.length ||
      place.aliases.some(alias => typeof alias !== 'string' || !alias.trim()) ||
      !Array.isArray(place.routes) || !place.routes.length ||
      place.routes.some(route => typeof route.label !== 'string' || !route.label.trim() || route.label.length > 32 ||
        !Array.isArray(route.points) || !route.points.length || route.points.some(point =>
          !Array.isArray(point) || point.length !== 2 || point.some(value => !Number.isFinite(value) || value < 0 || value > 1)))) {
    throw new Error('Destino inválido: ' + (place.id || '(sin ID)'));
  }
  identifiers.add(place.id);
  // Un grupo reúne el mismo tipo de lugar en varios pisos (ej. baños); no puede repetir piso.
  if (place.group != null) {
    const used = groupFloors.get(place.group) ?? new Set();
    if (used.has(place.floor)) throw new Error('Grupo repetido en el mismo piso: ' + place.id);
    groupFloors.set(place.group, used.add(place.floor));
  }
}

const normalize = value => value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
  .replace(/[^a-z0-9]+/g, ' ').trim();
const aliases = new Map();
for (const place of catalog) {
  for (const alias of [place.name, ...place.aliases]) {
    const previous = aliases.get(normalize(alias));
    if (previous && previous.id !== place.id && (!place.group || previous.group !== place.group)) {
      throw new Error(`Alias ambiguo: ${alias} (${previous.id}, ${place.id})`);
    }
    aliases.set(normalize(alias), place);
  }
}

const quote = value => value === null ? 'null' : JSON.stringify(value).replaceAll('$', '\\$');
const routes = value => 'listOf(' + value.map(route => 'MapRoute(' + quote(route.label) + ', listOf(' +
  route.points.map(([x,y]) => `MapPoint(${x}f, ${y}f)`).join(', ') + '))').join(', ') + ')';
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
   '            aliases = listOf(' + p.aliases.map(quote).join(', ') + '),\n' +
   '            routes = ' + routes(p.routes) + '\n' +
  '        )').join(',\n');
const output = 'package com.example.leitroboter.data\n\nimport com.example.leitroboter.R\n' +
  'import com.example.leitroboter.matching.RequestPolicy\n\n' +
  '// Generado desde destinations.json: node scripts/generate-catalog.mjs\n' +
  'object PlacesCatalog {\n    val places = listOf(\n' + entries + '\n    )\n\n' +
  '    private val byAlias: Map<String, List<Place>> = places\n' +
  '        .flatMap { place -> (listOf(place.name) + place.aliases).map { RequestPolicy.normalize(it) to place } }\n' +
  '        .groupBy({ it.first }, { it.second })\n' +
  '        .mapValues { (_, matches) -> matches.distinctBy { it.id } }\n\n' +
  '    fun exactMatches(request: String): List<Place> =\n' +
  '        byAlias[RequestPolicy.normalize(request)] ?: byAlias[Floors.withoutFloor(request)].orEmpty()\n}\n';
const target = new URL('app/src/main/java/com/example/leitroboter/data/PlacesCatalog.kt', root);
if (process.argv.includes('--check')) {
  if (fs.readFileSync(target, 'utf8').replaceAll('\r\n', '\n') !== output) {
    throw new Error('Catálogo Kotlin desactualizado: ejecutar node scripts/generate-catalog.mjs');
  }
} else {
  fs.mkdirSync(new URL('app/src/main/java/com/example/leitroboter/data/', root), {recursive:true});
  fs.writeFileSync(target, output);
}

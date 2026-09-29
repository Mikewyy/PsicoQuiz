import { readFileSync, writeFileSync } from 'node:fs';

const ruta = 'app/src/main/assets/preguntas.json';
const raw = JSON.parse(readFileSync(ruta, 'utf8'));

// Solo correcciones mecánicas: concordancia de género y una redundancia.
// Los problemas de contenido clínico NO se tocan aquí.
const R = [
  [3, 5, 'just', 'El VERBORREA o habla verborreica', 'La verborrea o habla verborreica'],
  [4, 1, 'just', 'con una rango afectivo restringido', 'con un rango afectivo restringido'],
  [4, 11, 'just', 'pérdida del control sobre los impulsos y las conductas, con pérdida del control de la conducta que antes estaba contenida',
              'pérdida del control sobre los impulsos, con liberación de conductas que antes estaban contenidas'],
];

const CAMPO = { just: 'justificacion', enun: 'enunciado' };
let ok = 0, fail = [];
for (const [n, p, tipo, buscar, nuevo] of R) {
  const q = raw.niveles[n - 1].preguntas[p - 1];
  const campo = CAMPO[tipo];
  if (!q || typeof q[campo] !== 'string' || !q[campo].includes(buscar)) {
    fail.push(`n${n}p${p} ${campo}: NO ENCONTRADO`);
    continue;
  }
  q[campo] = q[campo].replace(buscar, nuevo);
  ok++;
}
writeFileSync(ruta, JSON.stringify(raw, null, 2) + '\n', 'utf8');
console.log(`aplicados: ${ok}`);
if (fail.length) { fail.forEach(f => console.log('  ' + f)); process.exitCode = 1; }

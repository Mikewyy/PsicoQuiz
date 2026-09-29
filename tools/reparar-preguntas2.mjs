import { readFileSync, writeFileSync } from 'node:fs';

const ruta = 'app/src/main/assets/preguntas.json';
const raw = JSON.parse(readFileSync(ruta, 'utf8'));

const CAMPO = { just: 'justificacion', enun: 'enunciado' };

// Segunda tanda: encontrada por las reglas de higiene del validador, no a mano.
const R = [
  // ---------- n3p3 : circunloquio ----------
  [3, 3, 'enun', 'con unaMkkgiga larga', 'con una respuesta larga'],
  [3, 3, 'just', 'el pacienterode around el punto sin llegar a él',
              'el paciente rodea el punto sin llegar a él'],
  [3, 3, 'just', 'Se observa en demencias,’explique-afasia y en el disorder delThought de la esquizofrenia.',
              'Se observa en demencias, en la afasia de conducción y en el trastorno del pensamiento de la esquizofrenia.'],

  // ---------- n4p4 : euforia ----------
  [4, 4, 'enun', 'estado de ánimo abnormally elevado', 'estado de ánimo anormalmente elevado'],
  [4, 4, 'enun', 'con aumento de energia, que puede accompanyar a la manía',
              'con aumento de energía, que puede acompañar a la manía'],
  [4, 4, 'just', 'es el estado de ánimo abnormally elevado', 'es el estado de ánimo anormalmente elevado'],

  // ---------- n5p7 : insight ----------
  [5, 7, 'just', 'psicosis maníaco-depresiva', 'trastorno bipolar en fase psicótica'],
  [5, 7, 'just', 'En los trastornos neurotíquicos (ansiedad, fobias, obsesiones) el insight suele estar PRESENTE: el paciente sabe que le pasa algo, aunque le cueste comprehensive.',
              'En los trastornos neuróticos (ansiedad, fobias, obsesiones) el insight suele estar presente: el paciente sabe que le ocurre algo, aunque le cueste aceptar su condición.'],
];

let ok = 0, ya = 0, fail = [];

for (const [n, p, tipo, buscar, nuevo] of R) {
  const q = raw.niveles[n - 1].preguntas[p - 1];
  if (!q) { fail.push(`n${n}p${p}: no existe`); continue; }

  const campo = CAMPO[tipo];
  if (campo) {
    const v = q[campo];
    if (typeof v !== 'string') { fail.push(`n${n}p${p}: ${campo} no es string`); continue; }
    if (!v.includes(buscar)) {
      if (v.includes(nuevo)) { ya++; continue; }
      fail.push(`n${n}p${p} ${campo}: NO ENCONTRADO -> ${JSON.stringify(buscar.slice(0, 50))}`);
      continue;
    }
    q[campo] = v.replace(buscar, nuevo);
    ok++;
  } else {
    const i = q.opciones.findIndex(o => (o.texto || '').includes(buscar));
    if (i === -1) {
      if (q.opciones.some(o => (o.texto || '').includes(nuevo))) { ya++; continue; }
      fail.push(`n${n}p${p} opcion: NO ENCONTRADA -> ${JSON.stringify(buscar.slice(0, 50))}`);
      continue;
    }
    q.opciones[i].texto = q.opciones[i].texto.replace(buscar, nuevo);
    ok++;
  }
}

writeFileSync(ruta, JSON.stringify(raw, null, 2) + '\n', 'utf8');
console.log(`aplicados: ${ok}   ya aplicados: ${ya}`);
if (fail.length) { console.log(`\nFALLOS (${fail.length}):`); fail.forEach(f => console.log('  ' + f)); process.exitCode = 1; }
else console.log('todos los reemplazos aplicados');

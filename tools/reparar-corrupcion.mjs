#!/usr/bin/env node
/**
 * Repara la corrupcion "tercera y cuarta ola" del banco de preguntas:
 * homoglifos cirilicos, fragmentos en arabe/vietnamita, sustituciones por
 * palabras inglesas y trozos sin sentido.
 *
 * Solo aplica reemplazos EXACTOS e idempotentes. Las frases que no se pueden
 * reconstruir con certeza quedan sin tocar y se listan al final para revision
 * clinica.
 *
 *   node tools/reparar-corrupcion.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const ruta = join(raiz, 'app/src/main/assets/preguntas.json');

// [buscar, reemplazar, nota]
const CAMBIOS = [
  // --- homoglifos y scripts extranos ---
  ['s\u0113meiolog\u00eda', 'semeiolog\u00eda', 'n1p1: macron latin extendido fuera de lugar'],
  ['el m\u00e9dico objetiv\u0430 110', 'el m\u00e9dico objetiva 110', 'n1p11: cirilica a'],
  ['pero lo deform\u0430.', 'pero lo deforma.', 'n2p2: cirilica a'],
  ['Un paciente\u1eabn dice:', 'Un paciente dice:', 'n2p11: vietnamita ẫn'],
  ['se asocia a lasEK de \u0627\u0644\u0627\u0646\u0633\u062d\u0627\u0628 social',
   'se asocia al aislamiento social', 'n4p8: texto arabe'],

  ['\u00bfQu\u00e9.content del pensamiento', '\u00bfQu\u00e9 contenido del pensamiento', 'n2p11: quita ".content"'],
  ['elIRD pat\u00f3logo', 'el pat\u00f3logo', 'n1p12: pegada elIRD'],
  ['en laSemiolog\u00eda psicopatol\u00f3gica', 'en la semiolog\u00eda psicopatol\u00f3gica', 'n2p3: pegada laSemiología'],
  ['Suele associarse a esquizofrenia', 'Suele asociarse a esquizofrenia', 'n2p3: italianismo associarse'],
  ['esquizofrenia. Si la repetici\u00f3n es Snow y diferencia, se denomina colofasia. ' +
   'No debe confundirse con la COURAGEE de la ecolalia, que s\u00ed implica intenci\u00f3n comunicativa.',
   'esquizofrenia.', 'n3p5: elimina dos frases no reconstruibles'],

  // --- sustituciones por ingles ---
  ['Un mismo phenomenon puede ser signo', 'Un mismo fen\u00f3meno puede ser signo', 'n1p1'],
  ['que characterize a una enfermedad concreta', 'que caracteriza a una enfermedad concreta', 'n1p3'],
  ['sin haber explored al paciente', 'sin haber explorado al paciente', 'n1p9'],
  ['la misma phenomenon (taquicardia)', 'el mismo fen\u00f3meno (taquicardia)', 'n1p11'],
  ['se corresponden en un mismo phenomenon.', 'se corresponden en un mismo fen\u00f3meno.', 'n1p11'],
  ['en el syndrome de Charles Bonnet', 'en el s\u00edndrome de Charles Bonnet', 'n2p2'],
  ['(formal del pensamiento) es el disturbance en la l\u00f3gica y el curso del pensamiento',
   '(trastorno formal del pensamiento) es la alteraci\u00f3n de la l\u00f3gica y el curso del pensamiento', 'n2p7'],
  ['Se manifesta como pensamiento il\u00f3gico, tangencial, circunloquio o flight of ideas.',
   'Se manifiesta como pensamiento il\u00f3gico, tangencial, circunloquio o fuga de ideas.', 'n2p7'],
  ['Un belief normal puede revisarse', 'Una creencia normal puede revisarse', 'n2p9'],
  ['las ideas son\'Is Strange\' (el mar vigila', 'las ideas son extra\u00f1as (el mar vigila', 'n2p11'],
  ['no hay sadness reactiva asociada', 'no hay tristeza reactiva asociada', 'n4p8'],
  ['postura imposed por el examinador', 'postura impuesta por el examinador', 'n4p9'],
  ['s\u00edntomaspmbues nucleus del trastorno depresivo', 's\u00edntomas nucleares del trastorno depresivo', 'n4p10'],
  ['Conducta esterotipada repetitiva', 'Conducta estereotipada repetitiva', 'n4p11'],
  ['se asocia a des\u00f3rdenes como el disorder bipolar, la demencia frontotemporal o la personality borderl\u00edne.',
   'se asocia a trastornos como el trastorno bipolar, la demencia frontotemporal o el trastorno l\u00edmite de la personalidad.', 'n4p12'],
  ['atribuye su Nineteen a una causa externa', 'atribuye su enfermedad a una causa externa', 'n5p1'],
  ['La lucidez de la conscience durante la entrevista', 'La lucidez de la conciencia durante la entrevista', 'n5p1'],
  ['(o decline cognitivo)', '(o declive cognitivo)', 'n5p2'],
  ['que puede ser voluntarily recordado', 'que puede ser recordado voluntariamente', 'n5p10'],
  ['La memoria procedural', 'La memoria procedimental', 'n5p9'],
  ['Akinetic mutism', 'Mutismo acin\u00e9tico', 'n4p5'],
  ['es caracter\u00edstico de los PSIQUI\u00c1TRICOS (esquizofrenia',
   'es caracter\u00edstico de los trastornos psic\u00f3ticos (esquizofrenia', 'n5p7'],

  // --- fragmentos sin sentido / pegados ---
  ['palabras nuevas,-Terminus no existentes', 'palabras nuevas, no existentes', 'n3p7'],
  ['Se diferencia de la paraxia (una palabra real deformeada en su estructura)',
   'Se diferencia de la parafasia (una palabra real deformada en su estructura)', 'n3p7'],
  ['que oye voces creeoice que sus vecinos', 'que oye voces cree que sus vecinos', 'n5p3'],
  ['la incapacidad de(select,\u2026) mantener el foco atencional o deLm los est\u00edmulos relevantes',
   'la incapacidad de mantener el foco atencional o de filtrar los est\u00edmulos relevantes', 'n5p6'],
  ['Un paciente con ultrasonido del t\u00e9rmino no para de hablar',
   'Un paciente no para de hablar', 'n4p5'],
  ['la catalepsia es la mantenimiento pasivo de una postura',
   'la catalepsia es el mantenimiento pasivo de una postura', 'n4p5'],
  ['Un paciente en crisis-catalepsia permanece inm\u00f3vil',
   'Un paciente con catalepsia permanece inm\u00f3vil', 'n4p9'],
  ['entre elperts anal affectivo inappropriate y la disforia',
   'entre el afecto inapropiado y la disforia', 'n4p7'],

  // --- concordancia "un delirante" mal usado como sustantivo ---
  ['Los tres elementos que definen un delirante son',
   'Los tres elementos que definen un delirio son', 'n2p5'],
  ['Es un delirante, porque la convicci\u00f3n es firme',
   'Es un delirio, porque la convicci\u00f3n es firme', 'n2p5'],
  ['distinguir un delirante de una alteraci\u00f3n formal',
   'distinguir un delirio de una alteraci\u00f3n formal', 'n2p11'],
  ['Son delirantes, no desorganizaci\u00f3n.', 'Son ideas delirantes, no desorganizaci\u00f3n.', 'n2p11'],
  ['Es una alucinaci\u00f3n, no un delirante', 'Es una alucinaci\u00f3n, no un delirio', 'n2p11'],
  ['No es un delirante porque no hay convicci\u00f3n falsa',
   'No es un delirio porque no hay convicci\u00f3n falsa', 'n3p10'],

  // --- respuestas validas con terminos equivocados ---
  ['"respuestas_validas": "desorganizaci\u00f3n|desorganizacion|delirium|trastorno del pensamiento"',
   '"respuestas_validas": "desorganizaci\u00f3n|desorganizacion|trastorno del pensamiento"', 'n2p7: elimina delirium'],
  ['"respuestas_validas": "anhedonia|anomia"',
   '"respuestas_validas": "anhedonia|incapacidad para sentir placer|p\u00e9rdida de placer"', 'n4p10: elimina anomia'],
];

let texto = readFileSync(ruta, 'utf8');
let aplicados = 0, yaEstaban = 0;
const noEncontrados = [];

for (const [buscar, reemplazar, nota] of CAMBIOS) {
  if (texto.includes(buscar)) {
    texto = texto.split(buscar).join(reemplazar);
    console.log('  APLICADO   ' + nota + '  [' + buscar.slice(0, 45) + '...]');
    aplicados++;
  } else if (texto.includes(reemplazar)) {
    console.log('  ya estaba  ' + nota);
    yaEstaban++;
  } else {
    console.log('  NO HALLADO ' + nota + '  [' + buscar.slice(0, 45) + '...]');
    noEncontrados.push(nota);
  }
}

JSON.parse(texto); // aborta si el reemplazo rompio el JSON
writeFileSync(ruta, texto);
console.log(`\naplicados: ${aplicados} | ya estaban: ${yaEstaban} | no hallados: ${noEncontrados.length}`);
if (noEncontrados.length) console.log('revisar: ' + noEncontrados.join('; '));

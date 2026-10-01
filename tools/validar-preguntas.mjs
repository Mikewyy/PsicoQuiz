#!/usr/bin/env node
/**
 * Valida assets/preguntas.json antes de que llegue al APK.
 *   node tools/validar-preguntas.mjs
 *
 * HIGIENE DE CONTENIDO (por que existe):
 * el generador del banco metio, en oleadas, caracteres CJK, cirilicos,
 * arabes y vietnamitas (homoglifos), y sustituyo palabras castellanas por
 * palabras inglesas ('phenomenon', 'disturbance', 'sadness', ...). El
 * validador estructural no ve nada de eso, asi que aqui van reglas de prosa:
 *
 *   1. scripts no latinos (o latino extendido que el castellano no usa)
 *   2. intrusion de ingles por DICCIONARIO (en_US vs es_ES)
 *   3. palabras pegadas tipo camelCase
 *   4. meta-comentarios del generador
 *
 * LIMITE CONOCIDO: no detecta sinsentidos escritos con palabras castellanas
 * validas (p. ej. 'ultrasonido del termino'). Eso requiere revision humana.
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const ruta = join(raiz, 'app/src/main/assets/preguntas.json');

let errores = 0, avisos = 0;
const err = (m) => { console.log('  FALLO ' + m); errores++; };
const warn = (m) => { console.log('  AVISO ' + m); avisos++; };

// ---------------------------------------------------------------------------
// Diccionarios (hunspell es_ES + lista inglesa del sistema). Si faltan, se
// desactiva solo la comprobacion de ingles para no bloquear en otras maquinas.
// ---------------------------------------------------------------------------
function cargarDicEsp() {
  try {
    const set = new Set();
    for (const l of readFileSync('/usr/share/hunspell/es_ES.dic', 'utf8').split('\n')) {
      if (!l || /^\d+$/.test(l)) continue;
      const w = l.split('/')[0].trim().toLowerCase().replace(/\\/g, '');
      if (w) set.add(w);
    }
    return set;
  } catch { return null; }
}
function cargarListaIng() {
  try {
    const set = new Set();
    for (const l of readFileSync('/usr/share/dict/american-english', 'utf8').split('\n')) {
      const w = l.trim().toLowerCase();
      if (w && /^[a-z'’-]+$/.test(w)) set.add(w);
    }
    return set;
  } catch { return null; }
}
const ES = cargarDicEsp();
const EN = cargarListaIng();
if (!ES || !EN) console.log('AVISO: faltan diccionarios; se omite la deteccion de ingles por diccionario.');

// Palabras que el diccionario ingles conoce pero que son castellanas validas,
// anglicismos aceptados a proposito o nombres propios. Sin esta lista habria
// falsos positivos ('dice', 'bipolar', 'insight'...).
const PERMITIDAS = new Set([
  // castellanas que colisionan con el ingles o que faltan en el dicc. base
  'describe', 'dice', 'late', 'usa', 'cree', 'define', 'ideas', 'idea',
  'bipolar', 'irritable', 'llamas', 'brazos', 'bilateral',
  'amigos', 'cambia', 'confusion', 'detestable', 'divide', 'genera',
  'graves', 'negros', 'padres', 'produce', 'reales', 'rodeos', 'sale',
  'tales', 'traduce', 'verse', 'tics',
  // anglicismos/tecnicismos usados a proposito (van glosados en el texto)
  'insight', 'blunting', 'tests', 'test', 'trail', 'making', 'delirium', 'rem',
  // nombres propios
  'hampton', 'charles', 'bonnet', 'emil', 'bonaparte',
  // numeracion romana de estadios (p. ej. sueno REM III/IV)
  'iii', 'iv',
]);

// ---------------------------------------------------------------------------
// Reglas de higiene
// ---------------------------------------------------------------------------
// Scripts que no pertenecen al castellano + latin extendido A/B y adicional.
const NO_LATINO =
  /[\u0100-\u024F\u1E00-\u1EFF\u0370-\u03FF\u0400-\u04FF\u0530-\u058F\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\u0900-\u097F\u0E00-\u0E7F\u1100-\u11FF\u3000-\u30FF\u4E00-\u9FFF\uAC00-\uD7FF\uF900-\uFAFF\uFB50-\uFDFF\uFE70-\uFEFF]/u;
// Puntuacion CJK de ancho completo (algunas son ASCII, otras no).
const PUNT_CJK = /[\u3001\u3002\uFF01\uFF1F\uFF0C\uFF1A\uFF1B]/;
// Palabras pegadas: minuscula+mayuscula ('asuntosSin') o minuscula+2+mayusculas ('lasEK').
const PEGADAS = /\b[a-záéíóúüñ]{2,}[A-ZÁÉÍÓÚÜÑ]{1,}[a-záéíóúüñ]{2,}\b/g;
const PEGADAS_MAY = /\b[a-záéíóúüñ]{2,}[A-ZÁÉÍÓÚÜÑ]{2,}\b/g;
// Meta-comentarios del generador que jamas deben llegar al alumno.
const META = [
  'Disculpe', 'Corrijo', 'contenía un error', 'mi respuesta', 'como en un,',
  'lo que descrita', 'OpenAI', 'GPT', 'asistente', 'ejemplo inventado',
];

const TOKEN = /[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]{2,}/g;

// Revisa un campo de texto: devuelve nada y acumula errores.
function higieneTexto(donde, campo, texto) {
  if (!texto) return;

  const no = texto.match(NO_LATINO);
  if (no) {
    const c = no[0];
    err(`${donde} ${campo}: caracter no latino '${c}' (U+${c.codePointAt(0).toString(16).toUpperCase()}) en "${recorta(texto)}"`);
  }

  const punct = texto.match(PUNT_CJK);
  if (punct) err(`${donde} ${campo}: puntuacion CJK '${punct[0]}' en "${recorta(texto)}"`);

  if (EN && ES) {
    for (const m of texto.matchAll(TOKEN)) {
      const w = m[0].toLowerCase();
      if (EN.has(w) && !ES.has(w) && !PERMITIDAS.has(w)) {
        err(`${donde} ${campo}: intrusion de ingles "${m[0]}" en "${recorta(texto)}"`);
      }
    }
  }

  for (const meta of META) {
    if (texto.toLowerCase().includes(meta.toLowerCase())) {
      err(`${donde} ${campo}: meta-comentario del generador "${meta}" en "${recorta(texto)}"`);
      break;
    }
  }

  const pegadas = texto.match(PEGADAS) || texto.match(PEGADAS_MAY);
  if (pegadas) {
    err(`${donde} ${campo}: palabras pegadas ${JSON.stringify([...new Set(pegadas)])} en "${recorta(texto)}"`);
  }
}

function recorta(s, n = 70) {
  return s.length > n ? s.slice(0, n) + '...' : s;
}

// ---------------------------------------------------------------------------
// Validacion estructural
// ---------------------------------------------------------------------------
let raw;
try {
  raw = JSON.parse(readFileSync(ruta, 'utf8'));
} catch (e) {
  console.log('JSON INVALIDO: ' + e.message);
  process.exit(1);
}
console.log('JSON valido. version=' + raw.version);

const niveles = raw.niveles;
if (!Array.isArray(niveles) || niveles.length === 0) err('no hay niveles');

let total = 0, totalMcq = 0, totalEscrita = 0;

niveles.forEach((niv, iN) => {
  console.log(`\n[Tramo ${iN + 1}] ${niv.nombre}`);

  ['numero', 'nombre', 'tema'].forEach(k => {
    if (niv[k] === undefined || niv[k] === '') err(`nivel ${iN + 1}: falta '${k}'`);
  });
  if (niv.numero !== iN + 1) warn(`nivel ${iN + 1}: numero=${niv.numero} (deberia ser ${iN + 1})`);
  if (!Array.isArray(niv.preguntas) || niv.preguntas.length === 0) {
    err(`nivel ${iN + 1}: sin preguntas`);
    return;
  }

  for (const campo of ['nombre', 'tema', 'descripcion']) {
    higieneTexto(`nivel ${niv.numero}`, `nivel.${campo}`, niv[campo]);
  }

  let mcq = 0, escrita = 0;

  niv.preguntas.forEach((p, iP) => {
    const donde = `nivel ${niv.numero} pregunta ${iP + 1}`;
    total++;

    if (p.tipo !== 'mcq' && p.tipo !== 'escrita')
      err(`${donde}: tipo invalido '${p.tipo}' (debe ser 'mcq' o 'escrita')`);

    if (!p.enunciado || p.enunciado.length < 10)
      err(`${donde}: enunciado vacio o demasiado corto`);

    if (!p.justificacion || p.justificacion.length < 30)
      err(`${donde}: justificacion vacia o demasiado corta (obligatoria)`);

    if (p.tipo === 'mcq') {
      mcq++;
      if (!Array.isArray(p.opciones) || p.opciones.length < 2)
        err(`${donde}: necesita al menos 2 opciones`);
      else {
        const correctas = p.opciones.filter(o => o.correcta === true).length;
        if (correctas !== 1)
          err(`${donde}: debe haber EXACTAMENTE 1 opcion correcta, hay ${correctas}`);

        const textos = p.opciones.map(o => (o.texto || '').trim().toLowerCase());
        if (new Set(textos).size !== textos.length)
          err(`${donde}: hay opciones con texto duplicado`);

        p.opciones.forEach((o, iO) => {
          if (typeof o.texto !== 'string' || o.texto.trim() === '')
            err(`${donde} opcion ${iO + 1}: texto vacio`);
          if (typeof o.correcta !== 'boolean')
            err(`${donde} opcion ${iO + 1}: 'correcta' debe ser true/false, no ${JSON.stringify(o.correcta)}`);
        });
      }
      if (p.respuestas_validas)
        warn(`${donde}: tipo mcq no deberia tener respuestas_validas`);
    } else {
      escrita++;
      if (!p.respuestas_validas || !p.respuestas_validas.trim())
        err(`${donde}: tipo escrita SIN lista de respuestas_validas (obligatoria)`);
      else {
        const alt = p.respuestas_validas.split('|').map(s => s.trim()).filter(Boolean);
        if (alt.length === 0) err(`${donde}: respuestas_validas vacia`);
        if (alt.some(s => s.length < 2))
          err(`${donde}: hay alternativas demasiado cortas`);
        if (alt.length === 1)
          warn(`${donde}: una sola alternativa '${alt[0]}'; anade sinonimos o plural`);
      }
      if (p.opciones) warn(`${donde}: tipo escrita no deberia tener opciones`);
    }
  });

  totalMcq += mcq; totalEscrita += escrita;
  console.log(`  ${niv.preguntas.length} preguntas (${mcq} mcq, ${escrita} escritas)`);
});

console.log('\n=========================================');
console.log(`Total: ${total} preguntas (${totalMcq} mcq, ${totalEscrita} escritas)`);
console.log(`Niveles: ${niveles.length}`);

if (total < 50) err(`se requieren al menos 50 preguntas, hay ${total}`);
if (niveles.length !== 11) warn(`se esperaban 11 niveles, hay ${niveles.length}`);

niveles.forEach(n => {
  const esc = (n.preguntas || []).filter(p => p.tipo === 'escrita').length;
  if (esc < 2) warn(`nivel ${n.numero}: solo ${esc} pregunta(s) escrita(s)`);
});

// ---------------------------------------------------------------------------
// Higiene de contenido
// ---------------------------------------------------------------------------
console.log('\n--- higiene de contenido ---');

niveles.forEach(n => {
  n.preguntas.forEach((p, iP) => {
    const donde = `nivel ${n.numero} pregunta ${iP + 1}`;
    higieneTexto(donde, 'enunciado', p.enunciado);
    higieneTexto(donde, 'justificacion', p.justificacion);
    higieneTexto(donde, 'respuestas_validas', p.respuestas_validas);
    (p.opciones || []).forEach((o, iO) =>
      higieneTexto(donde, `opcion ${iO + 1}`, o.texto));
  });
});

console.log(`\n${errores} errores, ${avisos} avisos`);
if (errores > 0) { console.log('VALIDACION FALLIDA'); process.exit(1); }
console.log('VALIDACION OK');

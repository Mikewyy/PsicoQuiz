import { readFileSync, writeFileSync } from 'node:fs';

const ruta = 'app/src/main/assets/preguntas.json';
const raw = JSON.parse(readFileSync(ruta, 'utf8'));

// replacements exactos: [nivel, pregunta, tipo, texto buscado, texto nuevo]
const R = [
  // ---------- n2p1 : definicion de alucinacion ----------
  [2, 1, 'opcion', 'Una家人的 idea falsa que el paciente defiende con convicción',
              'Una idea falsa que el paciente defiende con convicción'],
  [2, 1, 'just',   'sin stimulus correspondiente', 'sin estímulo correspondiente'],

  // ---------- n2p4 : delirio ----------
  [2, 4, 'enun',   'no susceptibles de discusión', 'no susceptible de discusión'],
  [2, 4, 'just',   'creer que una persona causa自然灾害 puede ser un delirante en un psychiatric context, pero la misma idea en un niño que juega es normal',
              'creer que una persona causa desastres naturales puede ser un delirio en un contexto psicopatológico, pero la misma idea en un niño que juega es normal'],

  // ---------- n2p5 : delirante persistente ----------
  [2, 5, 'enun',   'lleva seis meses购买 comida nueva cada día',
              'lleva seis meses comprando comida nueva cada día'],
  [2, 5, 'opcion', 'porque perceives un olor', 'porque percibe un olor'],
  [2, 5, 'just',   'porque no hay recognised obsesión y一笑 se sostiene por el delirante',
              'porque no hay obsesión reconocida: la conducta se sostiene por el delirio, no por un ritual'],

  // ---------- n2p6 : alucinacion psicotica ----------
  [2, 6, 'opcion', 'Oir la propia voz thoughts de nadie, como si las pensara pero las oyera ajenas',
              'Oír la propia voz dentro de la cabeza, como si los pensamientos se escucharan desde dentro'],
  [2, 6, 'opcion', 'Oir una voz quethreatBe insulta cuando nadie está',
              'Oír una voz que insulta cuando no hay nadie presente'],
  [2, 6, 'opcion', 'Ver luces色彩 ante los ojos al cerrar los párpados',
              'Ver luces de color al cerrar los párpados'],
  [2, 6, 'just',   'las，大多 son intracranianas o hipnagógicas',
              'las más frecuentes son intracranianas o hipnagógicas'],

  // ---------- n2p10 : ilusion visual ----------
  [2, 10, 'opcion', 'Fobias', 'Fobia'],
  [2, 10, 'just',  'Que el stimulus sea discovered al fijar la atención apoya aún más el diagnóstico de ilusión.',
              'Que el estímulo se descubra al fijar la atención apoya aún más el diagnóstico de ilusión.'],

  // ---------- n3p4 : TANGENCIALIDAD (justificacion invertida) ----------
  [3, 4, 'enun',  'se desvían hacia asuntosSin relación con el tema',
             'se desvían hacia asuntos sin relación con el tema'],
  [3, 4, 'just',  'La TANGENCIALIDAD o pensamiento tangencial es aquella en la que el discurso se desvía de forma abrupta hacia temas sin relación, sin un puente lógico. Si se desvía pero el examinador puede seguir el hilo, se denomina協 行行 (paralaje) o pensamiento porgomor. La tangencialidad es más grave y característica de la esquizofrenia.',
             'La TANGENCIALIDAD o pensamiento tangencial es aquella en la que el discurso se desvía de forma abrupta hacia temas sin relación, sin un puente lógico, y el hilo de la conversación se pierde. Es más grave que la circunstancialidad o el pensamiento por paralaje, en los que el paciente también se desvía del tema pero el examinador todavía puede seguir el hilo y reconducirlo. La tangencialidad es característica de la esquizofrenia.'],

  // ---------- n3p5 : REESCRITA COMPLETA (marcador de respuesta erroneo + meta-comentario) ----------
  [3, 5, 'enun',  '¿Cuál es el nombre del，开口亚洲en que el paciente, durante su discurso, responde a sus propias preguntas con frases que riman entre sí, como en un，投资者 diálogo interno?',
             '¿Cuál es el nombre de la alteración en la que el paciente, durante su discurso, se responde a sí mismo y sus respuestas se forman únicamente con las palabras de la pregunta que acaba de oír?'],
  [3, 5, 'just',  'Disculpe, la pregunta contenía un error. Lo que descrita corresponde a VERBORREA, específicamente verborrea緑autoabsorbida o habla VERBORRÉE. En la verborrea el paciente puede mostrar vocablos]# razonamiento automáticos (Neologismos, paraxios) o tautologías. La ecolalia es la repetición de lo que dice el otro, y la parafasia es la sustitución de una palabra por otra. Corrijo mi respuesta: la opción correcta sería VERBORREA.',
             'El VERBORREA o habla verborreica se caracteriza por un aumento del flujo del habla, con producción de palabras que apenas guardan relación entre sí, y con tautologías y neologismos. Se observa típicamente en la esquizofrenia y en el trastorno bipolar maníaco. Para no confundirla: la ecolalia es la repetición de lo que dice el interlocutor, y la parafasia es la sustitución de una palabra por otra dentro de una frase comprensible.'],

  // ---------- n3p11 : parafrasia semantica ----------
  [3, 11, 'enun', "El paciente B responde 'S、音餐 tengo sed'.",
              "El paciente B responde 'Tengo sed' cuando el A le dice 'Tengo hambre'."],

  // ---------- n3p12 : verborrea ----------
  [3, 12, 'just', 'la verborrea es más轻型 y se acompaña de fuga de ideas',
              'la verborrea es más rápida y se acompaña de fuga de ideas'],

  // ---------- n4p1 : aplanamiento afectivo ----------
  [4, 1, 'opcion', 'La的反应 labilidad emocional exagerada',
              'La labilidad emocional exagerada'],
  [4, 1, 'opcion', 'La expresión de emociones恰当 en contextos inadecuados',
              'La expresión de emociones intensas en contextos inadecuados'],
  [4, 1, 'just',  'Es característico de la esquizofrenia y de la thirteen depresión profunda.',
              'Es característico de la esquizofrenia y de la depresión profunda.'],

  // ---------- n4p2 : labilidad afectiva (enunciado en ingles) ----------
  [4, 2, 'enun', '¿Cómo se llama la crying emotion involuntarily experimentada por el paciente durante una situación que normalmente no la alegraría?',
              '¿Cómo se llama la emoción que el paciente experimenta de forma involuntaria durante una situación que normalmente no la produciría?'],
  [4, 2, 'just', 'Se observa en trastornos dsEC con componente目安(episodios maníacos, disorders borderline) y en Aging cerebrales.',
              'Se observa en trastornos del eje con componente maníaco, como el trastorno bipolar en episodios maníacos y el trastorno límite de la personalidad, y también en enfermedades cerebrovasculares y demencias.'],

  // ---------- n4p3 : sindrome depresivo ----------
  [4, 3, 'enun', 'Un paciente lleva tres semanas conLlenas Match Constantemente triste',
              'Un paciente lleva tres semanas constantemente triste'],
  [4, 3, 'enun', 'sin interés ni北约 energía', 'sin interés ni energía'],
  [4, 3, 'just', 'la ausencia de。父亲 abnormally elevated, de la síntesis de ideación y de Grandiosity',
              'la ausencia de ánimo elevado, de la verborrea y de las grandiosidades'],

  // ---------- n4p6 : agitacion psicomotora ----------
  [4, 6, 'just', 'se manifesta como inquietud, inability para estar quieto,談alking constante, motions estiras',
              'se manifiesta como inquietud, incapacidad para estar quieto, habla constante y movimientos estereotipados'],

  // ---------- n4p11 : desinhibicion ----------
  [4, 11, 'opcion', 'sin普遍的 conciencia del problema',
               'sin conciencia de la magnitud del problema'],
  [4, 11, 'just',   'con activaciónwc de la conducta antes contenido',
               'con pérdida del control de la conducta que antes estaba contenida'],
  [4, 11, 'just',   'Es típica de losHIB frontales (como en la demencia frontotemporal) y de la manía. Se caracteriza por la conciencia figura limitada del paciente sobre la不断的 conducta inapropiada.',
               'Es típica de las lesiones del lóbulo frontal (como en la demencia frontotemporal) y de la manía. Se caracteriza por una conciencia limitada del paciente sobre la magnitud de su conducta inapropiada.'],

  // ---------- n5p9 : funciones ejecutivas ----------
  [5, 9, 'just', 'inhibición de impulsos, monitored Alternating',
              'inhibición de impulsos, monitorización del comportamiento y flexibilidad cognitiva'],
  [5, 9, 'just', 'la triad de los trastornos frontotemporales',
              'la tríada de los trastornos frontotemporales'],
  [5, 9, 'just', 'y perdida de la capacidad de plan。我',
              'y pérdida de la capacidad de planificación'],

  // ---------- n5p11 : ideacion suicida ----------
  [5, 11, 'enun', 'Un pacienteicum depresivo severo', 'Un paciente con depresión severa'],
  [5, 11, 'opcion', 'Laidityvidea de invencibilidad (ideación suicida y 中华)',
               'La ideación suicida y autodescriptiva (ideas de muerte e invariabilidad)'],
];

// tipo -> campo real en el JSON. OJO: 'just'/'enun' son marcadores, no campos.
const CAMPO = { just: 'justificacion', enun: 'enunciado' };

let ok = 0, yaApplied = 0, fail = [];

for (const [n, p, tipo, buscar, nuevo] of R) {
  const q = raw.niveles[n - 1].preguntas[p - 1];
  if (!q) { fail.push(`n${n}p${p}: no existe`); continue; }

  if (tipo === 'opcion') {
    // puede venir el texto COMPLETO de la opcion o solo un fragmento
    const idx = q.opciones.findIndex(o => (o.texto || '').includes(buscar));
    if (idx === -1) {
      // ya reparado en una corrida anterior?
      if (q.opciones.some(o => o.texto === nuevo || (o.texto || '').includes(nuevo))) { yaApplied++; continue; }
      fail.push(`n${n}p${p} opcion: NO ENCONTRADA -> ${JSON.stringify(buscar.slice(0, 45))}`);
      continue;
    }
    const textoViejo = q.opciones[idx].texto;
    q.opciones[idx].texto = textoViejo.replace(buscar, nuevo);
    if (q.opciones[idx].texto === textoViejo) {
      fail.push(`n${n}p${p} opcion: replace sin efecto -> ${JSON.stringify(buscar.slice(0, 45))}`);
      continue;
    }
    ok++;
    continue;
  }

  const campo = CAMPO[tipo];
  if (!campo) { fail.push(`n${n}p${p}: tipo '${tipo}' desconocido`); continue; }

  const valor = q[campo];
  if (typeof valor !== 'string') { fail.push(`n${n}p${p}: ${campo} no es string`); continue; }

  if (!valor.includes(buscar)) {
    if (valor.includes(nuevo)) { yaApplied++; continue; }   // idempotente
    fail.push(`n${n}p${p} ${campo}: NO ENCONTRADO -> ${JSON.stringify(buscar.slice(0, 45))}`);
    continue;
  }
  q[campo] = valor.replace(buscar, nuevo);
  ok++;
}

// n3p5: el marcador de respuesta apuntaba a Neologismo; la justificación dice verborrea
const q35 = raw.niveles[2].preguntas[4];
q35.opciones.forEach(o => { o.correcta = o.texto === 'Verborrea'; });

writeFileSync(ruta, JSON.stringify(raw, null, 2) + '\n', 'utf8');

console.log(`aplicados: ${ok}   ya aplicados: ${yaApplied}`);
if (fail.length) {
  console.log(`\nFALLOS (${fail.length}):`);
  fail.forEach(f => console.log('  ' + f));
  process.exitCode = 1;
} else {
  console.log('todos los reemplazos aplicados');
}

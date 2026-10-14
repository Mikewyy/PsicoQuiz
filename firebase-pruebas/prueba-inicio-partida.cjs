const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');

async function main() {
  const env = await initializeTestEnvironment({
    projectId: 'demo-patotective',
    firestore: {
      host: '127.0.0.1',
      port: 8080,
      rules: fs.readFileSync('firestore.rules', 'utf8')
    }
  });

  try {
    await env.clearFirestore();

    await env.withSecurityRulesDisabled(async ctx => {
      const db = ctx.firestore();

      await db.collection('salas').doc('SALA123').set({
        anfitrionId: 'jugador-1',
        estadoPartida: 'esperando',
        fasePartida: 'esperando',
        preguntaActual: 0,
        maxParticipantes: 10,
        cantidadParticipantes: 2
      });
    });

    const datos = {
      estadoPartida: 'jugando',
      fasePartida: 'pregunta',
      preguntaActual: 0,
      totalPreguntas: 10,
      cantidadPreguntas: 10,
      partidaId: 'partida-prueba',
      preguntasPartida: [],
      inicioPartida: new Date(),
      inicioPregunta: new Date(),
      respuestaCorrectaTexto: '',
      retroalimentacionActual: ''
    };

    const anfitrion = env.authenticatedContext('jugador-1').firestore();
    const invitado = env.authenticatedContext('jugador-2').firestore();

    const sala = db => db.collection('salas').doc('SALA123');

    // Por ahora esperamos que ambas operaciones sean rechazadas.
    await assertFails(sala(anfitrion).update(datos));
    await assertFails(sala(invitado).update(datos));

    console.log('PRUEBA INICIAL SUPERADA: inicio de partida aún bloqueado.');
  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

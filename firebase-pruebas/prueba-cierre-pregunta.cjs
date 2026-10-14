const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds
} = require('@firebase/rules-unit-testing');

async function main() {
  const entorno = await initializeTestEnvironment({
    projectId: 'demo-patotective',
    firestore: {
      host: '127.0.0.1',
      port: 8080,
      rules: fs.readFileSync('firestore.rules', 'utf8')
    }
  });

  try {
    await entorno.clearFirestore();

    const ruta = db => db
      .collection('salas').doc('SALA123')
      .collection('participantes').doc('jugador-1');

    await entorno.withSecurityRulesDisabled(async contexto => {
      await ruta(contexto.firestore()).set({
        uid: 'jugador-1',
        nombre: 'Jugador uno',
        puntosPartida: 100,
        aciertosPartida: 1,
        ultimoPuntaje: 100,
        ultimaCorrecta: true
      });
    });

    const invitado = entorno
      .authenticatedContext('jugador-2').firestore();

    await assertSucceeds(
      ruta(invitado).update({
        puntosPartida: 200,
        aciertosPartida: 2,
        ultimoPuntaje: 100,
        ultimaCorrecta: true
      })
    );

    console.log(
      'PRUEBA SUPERADA: cierre de pregunta permitido.'
    );
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

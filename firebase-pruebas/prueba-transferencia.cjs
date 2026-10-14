const fs = require('node:fs');

const {
  initializeTestEnvironment,
  assertFails
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

    const sala = db => db.collection('salas').doc('SALA123');
    const nuevoAnfitrion = db => sala(db)
      .collection('participantes').doc('jugador-2');

    await entorno.withSecurityRulesDisabled(async contexto => {
      const db = contexto.firestore();

      await sala(db).set({
        anfitrionId: 'jugador-1',
        maxParticipantes: 10,
        cantidadParticipantes: 2
      });

      await nuevoAnfitrion(db).set({
        uid: 'jugador-2',
        anfitrion: false,
        puntosPartida: 100
      });
    });

    const jugador1 = entorno
      .authenticatedContext('jugador-1')
      .firestore();

    // Simula que el anfitrión saliente transfiere el control.
    // Las reglas actuales deberían rechazar esta operación.
    const lote = jugador1.batch();

    lote.update(sala(jugador1), {
      anfitrionId: 'jugador-2'
    });

    lote.update(nuevoAnfitrion(jugador1), {
      anfitrion: true
    });

    await assertFails(lote.commit());

    console.log(
      'PRUEBA SUPERADA: transferencia incompleta bloqueada.'
    );
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

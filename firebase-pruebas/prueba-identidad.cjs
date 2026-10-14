const fs = require('node:fs');

const {
  initializeTestEnvironment,
  assertSucceeds,
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

    const jugador1 = entorno.authenticatedContext('jugador-1').firestore();
    const jugador2 = entorno.authenticatedContext('jugador-2').firestore();

    const participante = db =>
      db.collection('salas')
        .doc('SALA123')
        .collection('participantes')
        .doc('jugador-1');

    // El propietario crea su participante.
    await assertSucceeds(
      participante(jugador1).set({
        uid: 'jugador-1',
        puntosPartida: 100
      })
    );

    // Otro jugador intenta cambiar su identidad.
    await assertFails(
      participante(jugador2).update({
        uid: 'jugador-2'
      })
    );

    // Otro jugador intenta eliminarlo.
    await assertFails(
      participante(jugador2).delete()
    );

    // El propietario puede actualizar sus datos.
    await assertSucceeds(
      participante(jugador1).update({
        nombre: 'Jugador de prueba'
      })
    );

    console.log('PRUEBAS SUPERADAS: identidad protegida.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

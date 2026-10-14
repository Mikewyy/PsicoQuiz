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

    const jugador1 = entorno
      .authenticatedContext('jugador-1')
      .firestore();

    const jugador2 = entorno
      .authenticatedContext('jugador-2')
      .firestore();

    const plaza0 = jugador1
      .collection('salas')
      .doc('SALA123')
      .collection('plazas')
      .doc('0');

    const plaza1 = jugador2
      .collection('salas')
      .doc('SALA123')
      .collection('plazas')
      .doc('1');

    // Jugador 1 reserva su propia plaza.
    await assertSucceeds(plaza0.set({
      uid: 'jugador-1',
      numero: '0'
    }));

    // Jugador 2 intenta modificar la plaza ajena.
    await assertFails(
      jugador2
        .collection('salas')
        .doc('SALA123')
        .collection('plazas')
        .doc('0')
        .update({uid: 'jugador-2'})
    );

    // Jugador 2 reserva una plaza propia.
    await assertSucceeds(plaza1.set({
      uid: 'jugador-2',
      numero: '1'
    }));

    // Jugador 2 intenta crear una plaza a nombre de otro.
    await assertFails(
      jugador2
        .collection('salas')
        .doc('SALA123')
        .collection('plazas')
        .doc('2')
        .set({
          uid: 'jugador-1',
          numero: '2'
        })
    );

    // Jugador 2 no puede liberar la plaza del jugador 1.
    await assertFails(
      jugador2
        .collection('salas')
        .doc('SALA123')
        .collection('plazas')
        .doc('0')
        .delete()
    );

    // Jugador 1 puede liberar su plaza.
    await assertSucceeds(plaza0.delete());

    console.log('PRUEBAS SUPERADAS: propiedad de las plazas protegida.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

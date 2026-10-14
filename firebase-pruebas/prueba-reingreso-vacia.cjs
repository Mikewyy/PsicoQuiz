const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds
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

    const sala = db => db.collection('salas').doc('SALA123');

    // Simulamos una sala en espera cuyo anfitrión abandonó.
    await env.withSecurityRulesDisabled(async ctx => {
      const db = ctx.firestore();

      await sala(db).set({
        anfitrionId: 'jugador-1',
        estadoPartida: 'esperando',
        maxParticipantes: 10,
        cantidadParticipantes: 1
      });

      await sala(db).collection('participantes')
        .doc('jugador-1').set({
          uid: 'jugador-1',
          anfitrion: false,
          estadoConexion: 'abandono',
          plaza: '0'
        });
    });

    const db = env.authenticatedContext('jugador-2').firestore();
    const refSala = sala(db);
    const refParticipante =
      refSala.collection('participantes').doc('jugador-2');
    const refPlaza = refSala.collection('plazas').doc('1');

    // Reingreso legítimo de un jugador cuando el anfitrión abandonó.
    await assertSucceeds(db.runTransaction(async tx => {
      const docs = await Promise.all([
        tx.get(refSala),
        tx.get(refParticipante),
        tx.get(refPlaza)
      ]);

      if (!docs[0].exists || docs[2].exists) {
        throw new Error('Estado de prueba incorrecto.');
      }

      tx.set(refPlaza, {
        uid: 'jugador-2',
        numero: '1'
      });

      tx.set(refParticipante, {
        uid: 'jugador-2',
        nombre: 'Jugador dos',
        anfitrion: true,
        avatarId: 'avatar_01',
        unidoEn: Date.now(),
        plaza: '1',
        puntosPartida: 0,
        estadoConexion: 'activo'
      });

      tx.update(refSala, {
        anfitrionId: 'jugador-2'
      });
    }));

    console.log(
      'PRUEBA SUPERADA: recuperación legítima permitida.'
    );

  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

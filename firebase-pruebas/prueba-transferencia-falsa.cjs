const fs = require('node:fs');
const {
  initializeTestEnvironment,
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
      const sala = db.collection('salas').doc('SALA123');

      await sala.set({
        anfitrionId: 'jugador-1',
        maxParticipantes: 10,
        cantidadParticipantes: 2
      });

      await sala.collection('participantes').doc('jugador-1').set({
        uid: 'jugador-1',
        anfitrion: true,
        estadoConexion: 'activo'
      });

      await sala.collection('participantes').doc('jugador-2').set({
        uid: 'jugador-2',
        anfitrion: false,
        estadoConexion: 'activo'
      });
    });

    const db = env.authenticatedContext('jugador-1').firestore();
    const sala = db.collection('salas').doc('SALA123');

    // El anfitrión abandona, pero NO marca al sucesor como anfitrión.
    const batch = db.batch();

    batch.update(
      sala.collection('participantes').doc('jugador-1'),
      { estadoConexion: 'abandono', anfitrion: false }
    );

    batch.update(sala, {
      anfitrionId: 'jugador-2'
    });

    await assertFails(batch.commit());

    console.log(
      'PRUEBA SUPERADA: transferencia sin confirmar sucesor bloqueada.'
    );
  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

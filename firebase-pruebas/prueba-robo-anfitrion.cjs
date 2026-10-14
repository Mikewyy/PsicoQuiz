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

    const sala = db =>
      db.collection('salas').doc('SALA123');

    await entorno.withSecurityRulesDisabled(async contexto => {
      const db = contexto.firestore();

      await sala(db).set({
        anfitrionId: 'jugador-1',
        maxParticipantes: 10,
        cantidadParticipantes: 2
      });

      await sala(db).collection('participantes')
        .doc('jugador-1').set({
          uid: 'jugador-1',
          anfitrion: true,
          estadoConexion: 'activo'
        });

      await sala(db).collection('participantes')
        .doc('jugador-2').set({
          uid: 'jugador-2',
          anfitrion: false,
          estadoConexion: 'activo'
        });
    });

    const invitado = entorno
      .authenticatedContext('jugador-2').firestore();

    // El invitado intenta convertirse en anfitrión
    // sin que el anfitrión original abandone.
    await assertFails(
      sala(invitado).update({
        anfitrionId: 'jugador-2'
      })
    );

    console.log(
      'PRUEBA SUPERADA: invitado no puede robar el rol de anfitrión.'
    );
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error.message);
  process.exitCode = 1;
});

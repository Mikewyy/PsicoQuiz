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

    const jugador1 = entorno.authenticatedContext('jugador-1').firestore();
    const jugador2 = entorno.authenticatedContext('jugador-2').firestore();

    const ruta = db => db.collection('salas')
      .doc('SALA123')
      .collection('participantes')
      .doc('jugador-1');

    // Crear datos iniciales sin saltarse reglas.
    await entorno.withSecurityRulesDisabled(async contexto => {
      await ruta(contexto.firestore()).set({
        uid: 'jugador-1',
        puntosPartida: 100
      });
    });

    // El jugador 2 intenta regalarle 9000 puntos al jugador 1.
    await assertSucceeds(
      ruta(jugador2).update({ puntosPartida: 9000 })
    );

    console.log('VULNERABILIDAD CONFIRMADA: un usuario modificó puntos ajenos.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('RESULTADO:', error.message);
  process.exitCode = 1;
});

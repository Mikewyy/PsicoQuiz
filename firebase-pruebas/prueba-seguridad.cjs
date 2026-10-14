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
    const jimmy = entorno.authenticatedContext('jugador-1');
    const db = jimmy.firestore();

    await assertFails(
      db.collection('salas').doc('sala-prueba').get()
    );

    await assertFails(
      db.collection('salas').doc('sala-prueba').set({
        maxParticipantes: 10,
        cantidadParticipantes: 1
      })
    );

    console.log('PRUEBAS SUPERADAS: lectura y escritura denegadas.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

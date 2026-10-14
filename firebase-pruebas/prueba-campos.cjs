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

    const j1 = entorno.authenticatedContext('jugador-1').firestore();
    const j2 = entorno.authenticatedContext('jugador-2').firestore();

    const ref = db => db.collection('salas')
      .doc('SALA123')
      .collection('participantes')
      .doc('jugador-1');

    await assertSucceeds(ref(j1).set({
      uid: 'jugador-1',
      nombre: 'Jugador uno',
      puntosPartida: 100,
      estadoConexion: 'conectado'
    }));

    // Otro jugador no puede modificar el nombre.
    await assertFails(ref(j2).update({
      nombre: 'Nombre falso'
    }));

    // Otro jugador no puede modificar su conexión.
    await assertFails(ref(j2).update({
      estadoConexion: 'abandono'
    }));

    // El propietario puede modificar su nombre.
    await assertSucceeds(ref(j1).update({
      nombre: 'Nuevo nombre'
    }));

    // La modificación de puntos sigue permitida
    // temporalmente por compatibilidad.
    await assertFails(ref(j2).update({
      puntosPartida: 150
    }));

    console.log('PRUEBAS SUPERADAS: campos personales protegidos.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
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

    const db = env.authenticatedContext('jugador-1').firestore();

    const sala = db.collection('salas').doc('ABC123');
    const participante = sala.collection('participantes').doc('jugador-1');
    const plaza = sala.collection('plazas').doc('0');

    const datosSala = {
      codigo: 'ABC123',
      nombre: 'Sala de prueba',
      anfitrionId: 'jugador-1',
      maxParticipantes: 10,
      cantidadParticipantes: 1,
      estadoPartida: 'esperando'
    };

    // Caso 1: creación legítima con los tres documentos.
    await assertSucceeds(db.runTransaction(async tx => {
      const existente = await tx.get(sala);

      if (existente.exists) {
        throw new Error('La sala ya existe.');
      }

      tx.set(sala, datosSala);

      tx.set(participante, {
        uid: 'jugador-1',
        nombre: 'Jugador uno',
        anfitrion: true,
        avatarId: 'avatar_01',
        unidoEn: Date.now(),
        plaza: '0',
        puntosPartida: 0,
        estadoConexion: 'activo'
      });

      tx.set(plaza, {
        uid: 'jugador-1',
        numero: '0'
      });
    }));

    console.log('SUPERADA: creación legítima de sala con 10 plazas.');

    // Caso 2: otro usuario intenta crear una sala
    // indicando como anfitrión a jugador-1.
    const atacante = env.authenticatedContext('jugador-2').firestore();

    await assertFails(
      atacante.collection('salas').doc('FALSA1').set({
        codigo: 'FALSA1',
        nombre: 'Sala falsa',
        anfitrionId: 'jugador-1',
        maxParticipantes: 10,
        cantidadParticipantes: 1,
        estadoPartida: 'esperando'
      })
    );

    console.log('SUPERADA: creación con anfitrión ajeno bloqueada.');

  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

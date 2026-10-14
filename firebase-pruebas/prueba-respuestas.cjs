const fs = require('node:fs');
const firebase=require('firebase/compat/app');
require('firebase/compat/firestore');

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
    // La nueva regla exige sala activa y membresía real.
    await entorno.withSecurityRulesDisabled(async ctx => {
      const admin=ctx.firestore();
      await admin.collection('salas').doc('SALA123').set({
        estadoPartida:'jugando',fasePartida:'pregunta',partidaId:'partida1',preguntaActual:0
      });
      await admin.collection('salas').doc('SALA123').collection('participantes')
        .doc('jugador-1').set({uid:'jugador-1',estadoConexion:'activo'});
    });


    const jugador1 = entorno.authenticatedContext('jugador-1').firestore();
    const jugador2 = entorno.authenticatedContext('jugador-2').firestore();

    const respuesta1 = jugador1
      .collection('salas').doc('SALA123')
      .collection('respuestas').doc('partida1_0_jugador-1');

    const datos = {
      uid: 'jugador-1',
      partidaId: 'partida1',
      preguntaIndex: 0,
      opcionIndex: 2,
      correcta: true,
      respondidoEn: firebase.firestore.FieldValue.serverTimestamp()
    };

    // Respuesta legítima.
    await assertSucceeds(respuesta1.set(datos));

    // No puede modificar una respuesta enviada.
    await assertFails(
      respuesta1.update({ correcta: false })
    );

    // Otro jugador no puede borrarla.
    await assertFails(
      jugador2
        .collection('salas').doc('SALA123')
        .collection('respuestas').doc('partida1_0_jugador-1')
        .delete()
    );

    // Otro jugador no puede crear una respuesta
    // declarando un UID ajeno.
    await assertFails(
      jugador2
        .collection('salas').doc('SALA123')
        .collection('respuestas').doc('respuesta-falsa')
        .set(datos)
    );

    console.log('PRUEBAS SUPERADAS: respuestas protegidas.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

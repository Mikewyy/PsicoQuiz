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


    const db = entorno.authenticatedContext('jugador-1').firestore();

    const respuestas = db
      .collection('salas')
      .doc('SALA123')
      .collection('respuestas');

    const datos = {
      uid: 'jugador-1',
      partidaId: 'partida1',
      preguntaIndex: 0,
      opcionIndex: 2,
      correcta: true,
      respondidoEn: firebase.firestore.FieldValue.serverTimestamp()
    };

    // Caso 1: identificador correcto.
    await assertSucceeds(
      respuestas.doc('partida1_0_jugador-1').set(datos)
    );

    // Caso 2: identificador de otro jugador.
    await assertFails(
      respuestas.doc('partida1_0_jugador-2').set(datos)
    );

    // Caso 3: índice diferente al declarado.
    await assertFails(
      respuestas.doc('partida1_5_jugador-1').set(datos)
    );

    // Caso 4: identificador completamente inventado.
    await assertFails(
      respuestas.doc('respuesta-falsa').set(datos)
    );

    console.log('PRUEBAS SUPERADAS: identificadores falsos bloqueados.');
  } finally {
    await entorno.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

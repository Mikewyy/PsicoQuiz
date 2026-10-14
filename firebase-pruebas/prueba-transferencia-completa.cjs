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

  const sala = db => db.collection('salas').doc('SALA123');
  const miembro = (db, uid) =>
    sala(db).collection('participantes').doc(uid);
  const plaza = db =>
    sala(db).collection('plazas').doc('0');

  try {
    await env.clearFirestore();

    await env.withSecurityRulesDisabled(async ctx => {
      const db = ctx.firestore();

      await sala(db).set({
        anfitrionId: 'jugador-1',
        maxParticipantes: 10,
        cantidadParticipantes: 2
      });

      await miembro(db, 'jugador-1').set({
        uid: 'jugador-1',
        plaza: '0',
        anfitrion: true,
        estadoConexion: 'activo',
        puntosPartida: 100,
        aciertosPartida: 1,
        ultimoPuntaje: 100,
        ultimoLatido: 123456
      });

      await miembro(db, 'jugador-2').set({
        uid: 'jugador-2',
        anfitrion: false,
        estadoConexion: 'activo',
        puntosPartida: 50
      });

      await plaza(db).set({
        uid: 'jugador-1',
        numero: '0'
      });
    });

    const db = env.authenticatedContext('jugador-1').firestore();

    // Caso 1: transferencia completa mediante transacción.
    await assertSucceeds(db.runTransaction(async tx => {
      const salaRef = sala(db);
      const salienteRef = miembro(db, 'jugador-1');
      const sucesorRef = miembro(db, 'jugador-2');
      const plazaRef = plaza(db);

      // Todas las lecturas antes de las escrituras.
      const docs = await Promise.all([
        tx.get(salaRef),
        tx.get(salienteRef),
        tx.get(sucesorRef),
        tx.get(plazaRef)
      ]);

      if (!docs.every(d => d.exists)) {
        throw new Error('Faltan documentos de prueba.');
      }

      tx.update(salienteRef, {
        estadoConexion: 'abandono',
        puntosPartida: 0,
        aciertosPartida: 0,
        ultimoPuntaje: 0,
        anfitrion: false,
        ultimoLatido: 0
      });

      tx.delete(plazaRef);
      tx.update(salaRef, { anfitrionId: 'jugador-2' });
      tx.update(sucesorRef, { anfitrion: true });
    }));

    const estado = await sala(db).get();
    const saliente = await miembro(db, 'jugador-1').get();
    const sucesor = await miembro(db, 'jugador-2').get();
    const plazaLiberada = await plaza(db).get();

    if (
      estado.data().anfitrionId !== 'jugador-2' ||
      saliente.data().estadoConexion !== 'abandono' ||
      sucesor.data().anfitrion !== true ||
      plazaLiberada.exists
    ) {
      throw new Error('La transferencia no quedó consistente.');
    }

    console.log('SUPERADA: transferencia completa y plaza liberada.');

    // Caso 2: el antiguo anfitrión ya no puede transferir.
    await assertFails(
      sala(db).update({ anfitrionId: 'jugador-1' })
    );

    console.log('SUPERADA: antiguo anfitrión sin permiso de transferencia.');

  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('ERROR:', error);
  process.exitCode = 1;
});

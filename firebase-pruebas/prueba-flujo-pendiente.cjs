const fs = require('node:fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { Timestamp } = require('firebase/firestore');
// Use the firestore instance's own serverTimestamp via imported modular helper.
const { serverTimestamp } = require('firebase/firestore');

(async () => {
 const env = await initializeTestEnvironment({projectId:'demo-patotective',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('firestore.rules','utf8')}});
 let total=0;
 const ok=async (name, promise, permitted=true)=>{if(permitted) await assertSucceeds(promise); else await assertFails(promise); total++; console.log('OK',name);};
 try {
  await env.clearFirestore();
  const host=env.authenticatedContext('jugador-1').firestore(), guest=env.authenticatedContext('jugador-2').firestore();
  const room=db=>db.collection('salas').doc('SALA123');
  await env.withSecurityRulesDisabled(async ctx=>{
   const db=ctx.firestore();
   await room(db).set({anfitrionId:'jugador-1',estadoPartida:'jugando',fasePartida:'pregunta',preguntaActual:0,partidaId:'partida1',inicioPregunta:Timestamp.fromMillis(Date.now()-40000),maxParticipantes:10,cantidadParticipantes:2});
  });
  const close={fasePartida:'resultado',inicioResultado:serverTimestamp(),respuestaCorrectaTexto:'Opción A',retroalimentacionActual:'Explicación'};
  await ok('Invitado cierra pregunta vencida',room(guest).update(close));
  await ok('No se cierra dos veces la pregunta',room(host).update(close),false);
  await ok('No se avanza resultado antes de 10 s',room(guest).update({fasePartida:'pregunta',preguntaActual:1,inicioPregunta:serverTimestamp(),respuestaCorrectaTexto:'',retroalimentacionActual:''}),false);
  // Confirmation document format used by the Android app.
  const confirmation=room(guest).collection('lecturasResultado').doc('partida1_0_jugador-2');
  await ok('Jugador confirma lectura propia',confirmation.set({partidaId:'partida1',preguntaIndex:0,uid:'jugador-2',listo:true,confirmadoEn:serverTimestamp()}));
  await ok('Invitado no confirma como otro jugador',room(guest).collection('lecturasResultado').doc('partida1_0_jugador-1').set({partidaId:'partida1',preguntaIndex:0,uid:'jugador-1',listo:true,confirmadoEn:serverTimestamp()}),false);
  const rematch=room(guest).collection('revancha').doc('jugador-2');
  await ok('Jugador vota revancha',rematch.set({partidaId:'partida1',uid:'jugador-2',acepta:true,confirmadoEn:serverTimestamp()}));
  await ok('Jugador cambia voto',rematch.set({partidaId:'partida1',uid:'jugador-2',acepta:false,confirmadoEn:serverTimestamp()}));
  await ok('Invitado no falsifica voto ajeno',room(guest).collection('revancha').doc('jugador-1').set({partidaId:'partida1',uid:'jugador-1',acepta:true,confirmadoEn:serverTimestamp()}),false);
  console.log(`\n${total}/${total} comprobaciones adicionales sin error`);
 } finally {await env.cleanup();}
})().catch(e=>{console.error('FALLÓ:', e.message || e);process.exitCode=1});

const fs=require('node:fs');
const firebase=require('firebase/compat/app'); require('firebase/compat/firestore');
const {initializeTestEnvironment,assertSucceeds,assertFails}=require('@firebase/rules-unit-testing');
(async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-patotective',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('firestore.rules','utf8')}});
 try{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
   const d=ctx.firestore(),r=d.collection('salas').doc('PRUEBA');
   await r.set({estadoPartida:'jugando',fasePartida:'pregunta',partidaId:'p1',preguntaActual:0});
   await r.collection('participantes').doc('a').set({uid:'a',estadoConexion:'activo'});
   await r.collection('participantes').doc('b').set({uid:'b',estadoConexion:'abandono'});
  });
  const a=env.authenticatedContext('a').firestore(),b=env.authenticatedContext('b').firestore(),c=env.authenticatedContext('c').firestore();
  const field=()=>({uid:'a',partidaId:'p1',preguntaIndex:0,opcionIndex:1,correcta:false,respondidoEn:firebase.firestore.FieldValue.serverTimestamp()});
  const path=db=>db.collection('salas').doc('PRUEBA').collection('respuestas');
  await assertSucceeds(path(a).doc('p1_0_a').set(field()));
  await assertFails(path(a).doc('p1_0_a').set(field())); // update existing doc rejected
  await assertFails(path(a).doc('p1_2_a').set({...field(),preguntaIndex:2}));
  await assertFails(path(a).doc('p2_0_a').set({...field(),partidaId:'p2'}));
  await assertFails(path(c).doc('p1_0_c').set({...field(),uid:'c'}));
  await assertFails(path(b).doc('p1_0_b').set({...field(),uid:'b'}));
  await assertFails(path(a).doc('p1_0_a_extra').set(field()));
  await env.withSecurityRulesDisabled(async ctx=>ctx.firestore().collection('salas').doc('PRUEBA').update({fasePartida:'resultado'}));
  await assertFails(path(a).doc('p1_0_a').set(field()));
  console.log('OK 8 escenarios de respuesta vinculada a partida activa');
 }finally{await env.cleanup();}
})().catch(e=>{console.error(e);process.exitCode=1});

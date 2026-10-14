const fs=require('node:fs');
const {initializeTestEnvironment,assertSucceeds,assertFails}=require('@firebase/rules-unit-testing');
require('firebase/compat/firestore');
const firebase=require('firebase/compat/app');
(async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-patotective',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('firestore.rules','utf8')}});
 try {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>ctx.firestore().collection('salas').doc('INICIO1').set({anfitrionId:'host',estadoPartida:'esperando',fasePartida:'esperando',preguntaActual:0,maxParticipantes:10,cantidadParticipantes:2}));
  const fields={estadoPartida:'jugando',fasePartida:'pregunta',preguntaActual:0,totalPreguntas:10,cantidadPreguntas:10,partidaId:'p1',preguntasPartida:[],inicioPartida:firebase.firestore.FieldValue.serverTimestamp(),inicioPregunta:firebase.firestore.FieldValue.serverTimestamp(),respuestaCorrectaTexto:'',retroalimentacionActual:''};
  await assertFails(env.authenticatedContext('guest').firestore().collection('salas').doc('INICIO1').update(fields));
  await assertSucceeds(env.authenticatedContext('host').firestore().collection('salas').doc('INICIO1').update(fields));
  console.log('SUPERADA: solo host inicia partida');
 }finally{await env.cleanup();}
})().catch(e=>{console.error(e);process.exitCode=1;});

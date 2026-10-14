const fs=require('node:fs');
const {initializeTestEnvironment,assertSucceeds,assertFails}=require('@firebase/rules-unit-testing');
require('firebase/compat/firestore');
const firebase=require('firebase/compat/app');
async function main(){
 const env=await initializeTestEnvironment({projectId:'demo-patotective',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('firestore.rules','utf8')}});
 try{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
   const db=ctx.firestore();
   await db.doc('salas/SALA123').set({anfitrionId:'jugador-1',estadoPartida:'jugando',fasePartida:'pregunta',preguntaActual:0,inicioPregunta:new Date(Date.now()-35000),maxParticipantes:10,cantidadParticipantes:2});
   for (const uid of ['jugador-1','jugador-2']) await db.doc('salas/SALA123/participantes/'+uid).set({uid,estadoConexion:'activo',puntosPartida:0,aciertosPartida:0,ultimoPuntaje:0,ultimaCorrecta:false,anfitrion:uid==='jugador-1',plaza:uid==='jugador-1'?'0':'1'});
  });
  const db=env.authenticatedContext('jugador-2').firestore();
  const own=db.doc('salas/SALA123/participantes/jugador-2');
  const other=db.doc('salas/SALA123/participantes/jugador-1');
  const sala=db.doc('salas/SALA123');
  await assertFails(other.update({puntosPartida:900000,ultimoPuntaje:900000,ultimaCorrecta:true,aciertosPartida:1}));
  console.log('OK puntos arbitrarios rechazados');
  await assertFails(own.update({puntosPartida:1000,ultimoPuntaje:1000,ultimaCorrecta:true,aciertosPartida:1}));
  console.log('OK puntos fuera de cierre rechazados');
  await assertSucceeds(db.runTransaction(async tx=>{
    const [sd,pd]=await Promise.all([tx.get(sala),tx.get(other)]);
    tx.update(other,{puntosPartida:800,ultimoPuntaje:800,ultimaCorrecta:true,aciertosPartida:1});
    tx.update(sala,{fasePartida:'resultado',inicioResultado:firebase.firestore.FieldValue.serverTimestamp(),respuestaCorrectaTexto:'',retroalimentacionActual:''});
  }));
  console.log('OK cierre con puntaje permitido');
 }finally{await env.cleanup();}
}
main().catch(e=>{console.error(e);process.exitCode=1});

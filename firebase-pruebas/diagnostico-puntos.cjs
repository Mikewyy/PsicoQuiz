const fs=require('node:fs');
const {initializeTestEnvironment,assertSucceeds}=require('@firebase/rules-unit-testing');
async function main(){
 const env=await initializeTestEnvironment({projectId:'demo-patotective',firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('firestore.rules','utf8')}});
 try{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
   await ctx.firestore().collection('salas').doc('SALA123').set({anfitrionId:'jugador-1',estadoPartida:'jugando',fasePartida:'pregunta',partidaId:'partida1',preguntaActual:0});
   await ctx.firestore().collection('salas').doc('SALA123').collection('participantes').doc('jugador-1').set({uid:'jugador-1',anfitrion:true,estadoConexion:'activo',puntosPartida:0,aciertosPartida:0,ultimoPuntaje:0,ultimaCorrecta:false});
   await ctx.firestore().collection('salas').doc('SALA123').collection('participantes').doc('jugador-2').set({uid:'jugador-2',anfitrion:false,estadoConexion:'activo',puntosPartida:0,aciertosPartida:0,ultimoPuntaje:0,ultimaCorrecta:false});
  });
  const invitado=env.authenticatedContext('jugador-2').firestore();
  const otro=invitado.collection('salas').doc('SALA123').collection('participantes').doc('jugador-1');
  await assertSucceeds(otro.update({puntosPartida:900000,aciertosPartida:999,ultimoPuntaje:900000,ultimaCorrecta:true}));
  console.log('VULNERABILIDAD CONFIRMADA: jugador-2 pudo asignar 900000 puntos a jugador-1.');
  const resp=invitado.collection('salas').doc('SALA123').collection('respuestas').doc('partida1_0_jugador-2');
  await assertSucceeds(resp.set({uid:'jugador-2',partidaId:'partida1',preguntaIndex:0,opcionIndex:0,correcta:true,respondidoEn:new Date()}));
  console.log('LIMITACIÓN CONFIRMADA: el cliente puede declarar correcta=true sin validación del servidor.');
  console.log('DIAGNÓSTICO COMPLETO. No se modificó Firebase real.');
 } finally {await env.cleanup();}
}
main().catch(e=>{console.error('DIAGNÓSTICO NO COMPLETADO:',e.message);process.exitCode=1;});

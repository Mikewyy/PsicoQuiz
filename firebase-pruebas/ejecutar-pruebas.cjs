const {spawnSync} = require('node:child_process');
const path = require('node:path');
const fs = require('node:fs');
const list = [
 'prueba-crear-sala.cjs',
 'prueba-plazas.cjs',
 'prueba-identidad.cjs',
 'prueba-campos.cjs',
 'prueba-respuestas.cjs',
 'prueba-ids-falsos.cjs',
 'prueba-transferencia-completa.cjs',
 'prueba-transferencia-sin-plaza.cjs',
 'prueba-transferencia-falsa.cjs',
 'prueba-robo-anfitrion.cjs',
 'prueba-robo-sala-activa.cjs',
 'prueba-reingreso-vacia.cjs',
 'prueba-reingreso-antiguo.cjs',
 'prueba-anfitrion-falso.cjs',
 'prueba-autoproclamacion.cjs',
 'prueba-inicio-partida-integrada.cjs'
];
let failures=0;
for (const f of list) {
  if (!fs.existsSync(path.join(__dirname,f))) { console.log('FALTA:',f);failures++;continue; }
  const r=spawnSync(process.execPath,[path.join(__dirname,f)], {cwd:__dirname, encoding:'utf8'});
  const output=(r.stdout||'')+'\n'+(r.stderr||'');
  // Some old scripts only test their own expected outcome, so do not mark coverage beyond their names.
  console.log(`${r.status===0?'OK':'FALLÓ'} ${f}`);
  if(r.status!==0){failures++;console.log(output.slice(-2500));}
}
console.log(`\n${list.length-failures}/${list.length} pruebas ejecutadas sin error`);
process.exitCode=failures?1:0;

const {spawnSync}=require('node:child_process');
const tests=['prueba-respuestas.cjs','prueba-ids-falsos.cjs','prueba-respuestas-activas.cjs'];
let ok=0;
for(const f of tests){const p=spawnSync(process.execPath,[f],{cwd:__dirname,encoding:'utf8'});console.log((p.status===0?'OK ':'FALLÓ ')+f);if(p.status===0)ok++;else console.log((p.stdout||'')+'\n'+(p.stderr||''));}
console.log(`\n${ok}/${tests.length} grupos de pruebas del refuerzo`);process.exitCode=ok===tests.length?0:1;

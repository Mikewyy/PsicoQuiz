PSICOQUIZ - FIREBASE FASE 4 FINAL
=================================

Instalar desde la raiz del proyecto:
  unzip -o ~/Descargas/PsicoQuiz_Firebase_Fase4_FINAL.zip -d .
  ./gradlew assembleDebug
  ./gradlew installDebug

ARCHIVOS REEMPLAZADOS (una sola vez):
- firebase/FirebaseProgressSyncManager.java
- firebase/FirebaseSecondarySyncManager.java
- firebase/FirebaseProfileSyncManager.java
- data/dao/UsuarioDao.java
- data/dao/MascotaDao.java
- ui/dashboard/MainActivity.java

NO modifica DatabaseHelper ni cambia DB_VERSION 17.
NO toca seccion_explorar.xml.

QUE HACE FASE 4
---------------
1. Fase 2 deja de reemplazar a ciegas el progreso al iniciar sesion.
   Fusiona local + cloud:
   - lectura: maximo
   - completada/aprobado: OR
   - mejores puntajes/porcentajes: maximo
   - puntos/nivel/experiencia: maximo (evita rollback y duplicacion por snapshots)
   - inventarios: usa la fila modificada mas recientemente

2. Desde esta version, cada cambio de cantidad de comida/objetos actualiza
   obtenido_en. Eso permite decidir que dispositivo tiene la version mas nueva.

3. Fase 3 usa union no destructiva:
   - notas: union por creada_en
   - Pomodoro: union por iniciado_en
   - desafio diario: upsert por fecha
   Luego vuelve a subir la union a Firestore.

4. Perfil y mascota se sincronizan tambien cuando cambian, con debounce de 1.8 s:
   alias/avatar, nombre/skin de mascota, hambre/felicidad/energia y accesorio.

5. Al cerrar sesion se fuerza a encolar el ultimo snapshot de progreso y perfil
   antes de cerrar Firebase Auth, evitando perder un cambio que aun estuviera
   esperando el debounce.

LIMITACION DELIBERADA
---------------------
La fusion es conservadora, no un sistema bancario/event-sourcing. Si dos
telefonos modifican exactamente al mismo tiempo la misma cantidad consumible,
se prioriza la version con marca temporal mas reciente; no se suman snapshots,
para evitar duplicar recompensas. Para PsicoQuiz universitario esta estrategia
es apropiada y mantiene el plan simple/eficiente.

PRUEBA 1 - MIGRACION A FASE 4
-----------------------------
1. NO borres datos.
2. Cierra sesion y vuelve a entrar.
3. Ejecuta:
   adb logcat -d | grep -E "PsicoQuizSync|PsicoQuizFirebase|PERMISSION_DENIED|FATAL EXCEPTION"

Esperado:
  Fase 4: progreso local/cloud fusionado sin retrocesos.
  Fase 4: notas, desafio y Pomodoro fusionados local/cloud.

PRUEBA 2 - INSTALACION LIMPIA
-----------------------------
Solo despues de confirmar la prueba 1:
  adb logcat -c
  adb shell pm clear com.utm.semiologia

Inicia sesion con la misma cuenta y comprueba perfil, mascota, progreso,
inventario, notas, desafio y Pomodoro.

PRUEBA 3 - CAMBIO REAL
----------------------
Haz un cambio pequeno (por ejemplo avatar/skin o progreso), espera 2-3 s,
cierra sesion y vuelve a entrar. El cambio debe conservarse.

REGLAS
------
Las reglas actuales con match recursivo usuarios/{uid}/{documento=**} ya son
compatibles. firestore_rules_fase4.txt se incluye solo como referencia.

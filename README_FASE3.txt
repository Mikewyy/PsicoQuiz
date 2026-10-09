PSICOQUIZ - FIREBASE FASE 3 COMPLETA
====================================

Incluye:
- Notas personales: crear, editar, eliminar y restaurar.
- Desafio diario: resultado por fecha y restauracion de la racha derivada.
- Pomodoro: cada bloque de enfoque completado se guarda en SQLite y Firestore.
- Migracion inicial de los datos Fase 3 que ya existan en SQLite.
- Restauracion Firestore -> SQLite en una instalacion limpia.

Rutas Firestore:
usuarios/{uid}/sync/secundario
usuarios/{uid}/notas/{creadaEn}
usuarios/{uid}/actividad/{yyyy-MM-dd}
usuarios/{uid}/pomodoro/{iniciadoEn}

No cambia DatabaseHelper ni la version 17 de SQLite.
No modifica seccion_explorar.xml.
No modifica las Fases 1 y 2.

INSTALACION
-----------
Desde la raiz del proyecto:

unzip -o ~/Descargas/PsicoQuiz_Firebase_Fase3_COMPLETA.zip -d .
./gradlew assembleDebug
./gradlew installDebug

PRIMERA PRUEBA
--------------
1. NO borres datos locales todavia.
2. Abre la app, cierra sesion e inicia sesion otra vez para ejecutar la migracion inicial.
3. Revisa:
   adb logcat -d | grep -E "PsicoQuizSync|PsicoQuizFirebase|PERMISSION_DENIED|FATAL EXCEPTION"
4. Debe aparecer:
   Fase 3: no habia datos secundarios cloud; se migraran desde SQLite.
   Fase 3: datos secundarios sincronizados en Firestore.

PRUEBA DE RESTAURACION
----------------------
Solo despues de confirmar la subida:

adb logcat -c
adb shell pm clear com.utm.semiologia

Inicia sesion con la misma cuenta y revisa el log. Debe aparecer:
Fase 3: notas, desafio y Pomodoro restaurados desde Firestore.

IMPORTANTE
----------
Las reglas actuales de Firestore ya sirven si mantienen el bloque recursivo privado:

match /usuarios/{usuarioId} {
  allow read, create, update, delete:
    if request.auth != null && request.auth.uid == usuarioId;

  match /{documento=**} {
    allow read, create, update, delete:
      if request.auth != null && request.auth.uid == usuarioId;
  }
}

La resolucion avanzada de conflictos simultaneos entre dos dispositivos se deja para Fase 4.

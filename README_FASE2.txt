PSICOQUIZ - FIREBASE FASE 2 COMPLETA

Incluye:
- puntos, nivel, experiencia y racha
- progreso de secciones
- progreso de niveles/quiz
- inventario de alimentos
- accesorios
- objetos/poderes

Firestore:
usuarios/{uid}/sync/progreso

La Fase 2 usa claves portables, no IDs SQLite:
- secciones: camino:orden
- niveles: numero
- alimentos/accesorios: nombre
- objetos: tipo

Los DAO programan una subida con debounce de 1.8 s para agrupar cambios y reducir escrituras.
Al iniciar sesión, si existe progreso cloud se restaura; si no existe, se crea desde SQLite.

IMPORTANTE:
- No borra ni reemplaza FirebaseProfileSyncManager.java.
- No toca DatabaseHelper.java ni incrementa DB_VERSION.
- No sincroniza preguntas/contenido ni contraseñas.
- Conflictos avanzados entre dos teléfonos usados simultáneamente se resolverán en Fase 4.

Instalación desde la raíz del proyecto:
unzip -o ~/Descargas/PsicoQuiz_Firebase_Fase2_COMPLETA.zip -d .
./gradlew assembleDebug
./gradlew installDebug

Logs:
adb logcat -c
# usa la app unos segundos y luego:
adb logcat -d | grep -E "PsicoQuizSync|PsicoQuizFirebase|PERMISSION_DENIED|FATAL EXCEPTION"

Prueba recomendada:
1. Instalar sin borrar datos.
2. Iniciar sesión y comprobar en Firestore usuarios/{uid}/sync/progreso.
3. Completar una sección o quiz / cambiar inventario.
4. Esperar 2-3 segundos y revisar el log.
5. Solo después hacer la prueba de instalación limpia con adb shell pm clear com.utm.semiologia.

package com.utm.semiologia.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.utm.semiologia.data.dao.NivelesDao;

/**
 * Punto único de acceso a la base de datos SQLite local.
 *
 * Esquema relacional normalizado (3FN) para la app de Semiología Psicológica.
 * Convenciones:
 *   - Claves primarias: id INTEGER PRIMARY KEY AUTOINCREMENT
 *   - Claves foráneas:   <tabla>_id INTEGER REFERENCES tabla(id) ON DELETE CASCADE
 *   - Marcas de tiempo:  epoch millis (INTEGER) -> últimas_actividad_fecha
 *                        es la única excepción: TEXT 'YYYY-MM-DD' para agrupar por día.
 *   - Booleanos:         INTEGER 0/1
 *
 * Las 5 tablas pedidas explícitamente son:
 *   usuarios, mascotas, notas, progreso_secciones, pomodoro_historial.
 * Las demás son de soporte para los módulos de contenido, economía e inventario.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "semiologia.db";
    public static final int DB_VERSION = 14;
    public Context getAppContext() {
        return appContext;
    }
    // ------------------------------------------------------------------
    // Nombres de tablas
    // ------------------------------------------------------------------
    public static final String T_USUARIOS = "usuarios";
    public static final String T_MASCOTA = "mascota";
    public static final String T_NOTAS = "notas";
    public static final String T_PROGRESO = "progreso_secciones";
    public static final String T_POMODORO = "pomodoro_historial";
    // Soporte
    public static final String T_SECCIONES = "secciones";
    public static final String T_ALIMENTOS = "alimentos";
    public static final String T_INVENTARIO = "inventario";
    public static final String T_ACCESORIOS = "accesorios";
    public static final String T_INV_ACCESORIOS = "inventario_accesorios";
    public static final String T_ACTIVIDAD = "actividad_diaria";
    public static final String T_GRUPOS = "grupos";
    public static final String T_GRUPO_MIEMBROS = "grupo_miembros";
    public static final String T_NOTAS_COMPARTIDAS = "notas_compartidas";
    public static final String T_COMENTARIOS = "comentarios_nota";
    // Evaluacion tipo Duolingo (v2)
    public static final String T_NIVELES = "niveles";
    public static final String T_PREGUNTAS = "preguntas";
    public static final String T_OPCIONES = "opciones";
    public static final String T_INTENTOS = "intentos";
    public static final String T_RESPUESTAS_DADAS = "respuestas_dadas";
    public static final String T_PROGRESO_NIVELES = "progreso_niveles";
    // Objetos/poderes (v4): baritas magicas, etc.
    public static final String T_INV_OBJETOS = "inventario_objetos";

    private static volatile DatabaseHelper instance;

    /** Contexto de aplicacion, necesario para leer assets/preguntas.json. */
    private final Context appContext;

    public static DatabaseHelper get(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        crearEsquema(db);
        crearTablasEvaluacion(db);
        sembrarDatosIniciales(db);
        NivelesDao.sembrarNiveles(appContext, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
         * IMPORTANTE:
         * Las columnas categoria/orden deben existir antes de ejecutar
         * migraciones antiguas que intenten sembrar los bancos de evaluación.
         *
         * Si la tabla niveles todavía no existe, asegurarColumnasCategoria()
         * simplemente retorna y migrarAV2() la creará con el esquema actual.
         */
        if (oldVersion < 5) {
            asegurarColumnasCategoria(db);
        }

        if (oldVersion < 2) {
            migrarAV2(db);
        }
        if (oldVersion < 3) {
            migrarAV3(db);
        }
        if (oldVersion < 4) {
            migrarAV4(db);
        }
        if (oldVersion < 5) {
            migrarAV5(db);
        }
        if (oldVersion < 6) {
            migrarAV6(db);
        }
        if (oldVersion < 7) {
            migrarAV7(db);
        }
        if (oldVersion < 8) {
            migrarAV8(db);
        }
        if (oldVersion < 9) {
            migrarAV9(db);
        }
        if (oldVersion < 10) {
            migrarAV10(db);
        }
        if (oldVersion < 11) {
            migrarAV11(db);
        }
        if (oldVersion < 12) {
            migrarAV12(db);
        }
        if (oldVersion < 14) {
            recrearBancoSindromes(db);
        }
    }

    // =========================================================
    // V2 - MÓDULO DE EVALUACIÓN
    // =========================================================

    private void migrarAV2(SQLiteDatabase db) {
        crearTablasEvaluacion(db);

        // Semilla de los bancos desde assets.
        NivelesDao.sembrarNiveles(
                appContext,
                db
        );
    }

    // =========================================================
    // V3 - RECREAR BANCO DE EVALUACIÓN
    // =========================================================

    private void migrarAV3(SQLiteDatabase db) {
        crearTablasEvaluacion(db);

        db.beginTransaction();
        try {
            // Orden seguro de borrado.
            db.execSQL("DELETE FROM " + T_RESPUESTAS_DADAS);
            db.execSQL("DELETE FROM " + T_INTENTOS);
            db.execSQL("DELETE FROM " + T_PROGRESO_NIVELES);
            db.execSQL("DELETE FROM " + T_OPCIONES);
            db.execSQL("DELETE FROM " + T_PREGUNTAS);
            db.execSQL("DELETE FROM " + T_NIVELES);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        NivelesDao.sembrarNiveles(
                appContext,
                db
        );
    }

    // =========================================================
    // V4 - INVENTARIO / POMODORO
    // =========================================================

    private void migrarAV4(SQLiteDatabase db) {
        crearTablaObjetos(db);
    }

    // =========================================================
    // V5 - SEGUNDO CAMINO: SÍNDROMES
    // =========================================================

    private void migrarAV5(SQLiteDatabase db) {
        db.execSQL(
                "UPDATE " + T_NIVELES +
                        " SET categoria = 'sintomas' " +
                        "WHERE categoria IS NULL OR categoria = ''"
        );

        db.execSQL(
                "UPDATE " + T_NIVELES +
                        " SET orden = numero " +
                        "WHERE (orden IS NULL OR orden = 0) " +
                        "AND categoria = 'sintomas'"
        );

        NivelesDao.sembrarSindromes(
                appContext,
                db
        );
    }

    // =========================================================
    // V6 - AVATAR DEL USUARIO
    // =========================================================

    private void migrarAV6(SQLiteDatabase db) {
        if (!existeColumna(db, T_USUARIOS, "avatar")) {
            db.execSQL(
                    "ALTER TABLE " + T_USUARIOS +
                            " ADD COLUMN avatar TEXT DEFAULT 'avatar_01'"
            );
        }
    }

    // =========================================================
    // V7 - SKIN NUMÉRICA DE LA MASCOTA
    // =========================================================

    private void migrarAV7(SQLiteDatabase db) {
        if (!existeColumna(db, T_MASCOTA, "skin_id")) {
            db.execSQL(
                    "ALTER TABLE " + T_MASCOTA +
                            " ADD COLUMN skin_id INTEGER NOT NULL DEFAULT 0"
            );
        }
    }

    // =========================================================
    // V8 - DOS CAMINOS EN LA GUÍA
    // =========================================================

    private void migrarAV8(SQLiteDatabase db) {
        if (!existeColumna(db, T_SECCIONES, "camino")) {
            db.execSQL(
                    "ALTER TABLE " + T_SECCIONES +
                            " ADD COLUMN camino TEXT NOT NULL DEFAULT 'sintomas'"
            );
        }
    }

    // =========================================================
    // V9 - ACTUALIZAR GUÍA COMPLETA
    // =========================================================

    private void migrarAV9(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            if (!existeColumna(db, T_SECCIONES, "camino")) {
                db.execSQL(
                        "ALTER TABLE " + T_SECCIONES +
                                " ADD COLUMN camino TEXT NOT NULL DEFAULT 'sintomas'"
                );
            }

            actualizarContenidoGuiaCompleta(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================
    // V10 - PRIMER INTENTO DE SEMBRAR SÍNDROMES
    // =========================================================

    private void migrarAV10(SQLiteDatabase db) {
        NivelesDao.sembrarSindromes(
                appContext,
                db
        );
    }

    // =========================================================
    // V11 - RECREAR SOLO EL BANCO DE SÍNDROMES
    // =========================================================

    private void migrarAV11(SQLiteDatabase db) {
        recrearBancoSindromes(db);
    }

    // =========================================================
    // V12 - FORZAR LA CARGA DEL NUEVO sindromes.json
    // =========================================================

    private void migrarAV12(SQLiteDatabase db) {
        /*
         * V11 ya pudo haberse ejecutado antes de que sindromes.json
         * estuviera completo. V12 fuerza una nueva carga del archivo
         * sin tocar Síntomas, usuario, mascota, guía, notas ni Pomodoro.
         */
        recrearBancoSindromes(db);
    }

    /**
     * Elimina únicamente el banco de evaluación de Síndromes y lo vuelve
     * a sembrar desde assets/sindromes.json.
     *
     * Las preguntas, opciones, intentos y progreso vinculados a esos niveles
     * se eliminan por las claves foráneas ON DELETE CASCADE.
     */
    private void recrearBancoSindromes(SQLiteDatabase db) {
        android.util.Log.d(
                "NivelesDao",
                "Recreando banco de Síndromes desde assets/sindromes.json"
        );

        db.beginTransaction();
        try {
            db.execSQL(
                    "DELETE FROM " + T_NIVELES +
                            " WHERE categoria = 'sindromes'"
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        NivelesDao.sembrarSindromes(
                appContext,
                db
        );

        // Log de comprobación: debe indicar 7 con el JSON actual.
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + T_NIVELES +
                        " WHERE categoria = ?",
                new String[]{"sindromes"}
        )) {
            int total = c.moveToFirst() ? c.getInt(0) : 0;

            android.util.Log.d(
                    "NivelesDao",
                    "Niveles de Síndromes cargados: " + total
            );
        }
    }

    /**
     * Añade las columnas categoria/orden a la tabla niveles si no existen.
     * Debe ser tolerante: en migraciones desde v1 la tabla aún no existe.
     */
    private void asegurarColumnasCategoria(SQLiteDatabase db) {
        if (!existeTabla(db, T_NIVELES)) return;
        if (!existeColumna(db, T_NIVELES, "categoria")) {
            db.execSQL("ALTER TABLE " + T_NIVELES
                    + " ADD COLUMN categoria TEXT NOT NULL DEFAULT 'sintomas'");
        }
        if (!existeColumna(db, T_NIVELES, "orden")) {
            db.execSQL("ALTER TABLE " + T_NIVELES
                    + " ADD COLUMN orden INTEGER NOT NULL DEFAULT 0");
        }
    }

    private boolean existeTabla(SQLiteDatabase db, String tabla) {
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master "
                + "WHERE type = 'table' AND name = ?", new String[]{tabla})) {
            return c.moveToFirst();
        }
    }

    private boolean existeColumna(SQLiteDatabase db, String tabla, String columna) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null)) {
            int idx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (columna.equals(c.getString(idx))) return true;
            }
        }
        return false;
    }

    // ==================================================================
    // ESQUEMA RELACIONAL
    // ==================================================================
    private void crearEsquema(SQLiteDatabase db) {

        // --------------------------------------------------------------
        // 1. GRUPOS  (declarado primero: usuarios y pomodoro lo referencian)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_GRUPOS + " (" +
                "id                INTEGER PRIMARY KEY AUTOINCREMENT," +
                "codigo            TEXT    NOT NULL UNIQUE," +   // código de 6 chars para unirse
                "nombre            TEXT    NOT NULL," +
                "creador_id        INTEGER REFERENCES " + T_USUARIOS + "(id) ON DELETE SET NULL," +
                "creado_en         INTEGER NOT NULL" +
                ")");

        // --------------------------------------------------------------
        // 2. USUARIOS
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_USUARIOS + " (" +
                "id                     INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre                 TEXT    NOT NULL," +
                "avatar                 TEXT             DEFAULT 'avatar_01'," +
                "email                  TEXT    NOT NULL UNIQUE COLLATE NOCASE," +
                "password_hash          TEXT    NOT NULL," +
                "password_salt          TEXT    NOT NULL," +
                "puntos                 INTEGER NOT NULL DEFAULT 0," +
                "nivel                  INTEGER NOT NULL DEFAULT 1," +
                "experiencia            INTEGER NOT NULL DEFAULT 0," +
                "racha_actual           INTEGER NOT NULL DEFAULT 0," +   // días consecutivos
                "racha_maxima           INTEGER NOT NULL DEFAULT 0," +
                "ultima_actividad_fecha TEXT             DEFAULT NULL," + // 'YYYY-MM-DD'
                "grupo_id               INTEGER REFERENCES " + T_GRUPOS + "(id) ON DELETE SET NULL," +
                "creado_en              INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_usuarios_email ON " + T_USUARIOS + "(email)");
        db.execSQL("CREATE INDEX idx_usuarios_grupo ON " + T_USUARIOS + "(grupo_id)");

        // --------------------------------------------------------------
        // 3. SECCIONES  (catálogo de contenido de la guía de estudio)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_SECCIONES + " (" +
                "id                      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "camino                  TEXT    NOT NULL DEFAULT 'sintomas'," +
                "tema                    TEXT    NOT NULL," +
                "titulo                  TEXT    NOT NULL," +
                "contenido               TEXT    NOT NULL," +   // HTML para el lector + resaltado
                "orden                   INTEGER NOT NULL," +
                "duracion_estimada_min   INTEGER NOT NULL DEFAULT 10," +
                "puntos_recompensa       INTEGER NOT NULL DEFAULT 20" +
                ")");

        // --------------------------------------------------------------
        // 4. ACCESORIOS  (catálogo; nivel_requerido = desbloqueo por nivel)
        //    Se declara antes que 'mascota' porque esta referencia su id.
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_ACCESORIOS + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre              TEXT    NOT NULL," +
                "emoji               TEXT    NOT NULL," +
                "nivel_requerido     INTEGER NOT NULL DEFAULT 1," +
                "descripcion         TEXT," +
                "precio_puntos       INTEGER NOT NULL DEFAULT 0" +
                ")");

        // --------------------------------------------------------------
        // 5. MASCOTA  (1 usuario -> 1 mascota)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_MASCOTA + " (" +
                "id                      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id              INTEGER NOT NULL UNIQUE REFERENCES " +
                T_USUARIOS + "(id) ON DELETE CASCADE," +
                "nombre                  TEXT    NOT NULL DEFAULT 'Mateo'," +
                "especie                 TEXT    NOT NULL DEFAULT 'gato'," +
                "skin_id                 INTEGER NOT NULL DEFAULT 0," +
                // Barras 0..100. 'hambre' es INVERSA: 100 = alimentado, 0 = hambriento.
                "hambre                  INTEGER NOT NULL DEFAULT 100," +
                "felicidad               INTEGER NOT NULL DEFAULT 80," +
                "energia                 INTEGER NOT NULL DEFAULT 100," +
                "estado                  TEXT    NOT NULL DEFAULT 'feliz'," +
                "accesorio_equipado_id   INTEGER REFERENCES " + T_ACCESORIOS + "(id) ON DELETE SET NULL," +
                // reloj del decaimiento: marca de la última vez que se aplicó la caída
                "hambre_actualizada_en   INTEGER NOT NULL," +
                "creado_en               INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_mascota_usuario ON " + T_MASCOTA + "(usuario_id)");

        // --------------------------------------------------------------
        // 6. INVENTARIO_ACCESORIOS  (lo que posee el usuario)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_INV_ACCESORIOS + " (" +
                "id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id      INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "accesorio_id    INTEGER NOT NULL REFERENCES " + T_ACCESORIOS + "(id) ON DELETE CASCADE," +
                "equipado        INTEGER NOT NULL DEFAULT 0," +
                "obtenido_en     INTEGER NOT NULL," +
                "UNIQUE(usuario_id, accesorio_id)" +
                ")");

        // --------------------------------------------------------------
        // 7. ALIMENTOS  (catálogo de comida)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_ALIMENTOS + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre              TEXT    NOT NULL," +
                "emoji               TEXT    NOT NULL," +
                "puntos_hambre       INTEGER NOT NULL DEFAULT 10," +  // cuántos puntos de hambre suma
                "puntos_felicidad    INTEGER NOT NULL DEFAULT 5," +
                "precio_puntos       INTEGER NOT NULL DEFAULT 10" +
                ")");

        // --------------------------------------------------------------
        // 8. INVENTARIO  (bocadillo del usuario)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_INVENTARIO + " (" +
                "id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id      INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "alimento_id     INTEGER NOT NULL REFERENCES " + T_ALIMENTOS + "(id) ON DELETE CASCADE," +
                "cantidad        INTEGER NOT NULL DEFAULT 1," +
                "obtenido_en     INTEGER NOT NULL," +
                "UNIQUE(usuario_id, alimento_id)" +
                ")");

        // --------------------------------------------------------------
        // 9. PROGRESO_SECCIONES  (lectura y finalizacion por usuario)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_PROGRESO + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id          INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "seccion_id          INTEGER NOT NULL REFERENCES " + T_SECCIONES + "(id) ON DELETE CASCADE," +
                "lectura             INTEGER NOT NULL DEFAULT 0," +  // porcentaje 0..100
                "completada          INTEGER NOT NULL DEFAULT 0," +
                "puntos_otorgados    INTEGER NOT NULL DEFAULT 0," +
                "comida_otorgada     INTEGER NOT NULL DEFAULT 0," +
                "iniciada_en         INTEGER NOT NULL," +
                "completada_en       INTEGER," +
                "UNIQUE(usuario_id, seccion_id)" +
                ")");
        db.execSQL("CREATE INDEX idx_progreso_usuario ON " + T_PROGRESO + "(usuario_id)");

        // --------------------------------------------------------------
        // 10. NOTAS  (fragmento seleccionado por el usuario en el lector)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_NOTAS + " (" +
                "id                      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id              INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "seccion_id              INTEGER REFERENCES " + T_SECCIONES + "(id) ON DELETE SET NULL," +
                "texto_seleccionado      TEXT    NOT NULL," +  // el fragmento resaltado
                "comentario              TEXT," +               // lo que escribió el usuario
                "color                   TEXT    NOT NULL DEFAULT '#FFEB3B'," +
                "creada_en               INTEGER NOT NULL," +
                "sincronizada            INTEGER NOT NULL DEFAULT 0" + // flag de sync con backend
                ")");
        db.execSQL("CREATE INDEX idx_notas_usuario ON " + T_NOTAS + "(usuario_id)");

        // --------------------------------------------------------------
        // 11. POMODORO_HISTORIAL
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_POMODORO + " (" +
                "id                      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id              INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "modo                    TEXT    NOT NULL DEFAULT 'individual'," + // individual|grupal
                "grupo_id                INTEGER REFERENCES " + T_GRUPOS + "(id) ON DELETE SET NULL," +
                "duracion_foco_min       INTEGER NOT NULL DEFAULT 20," +
                "duracion_descanso_min   INTEGER NOT NULL DEFAULT 5," +
                "minutos_estudiados      INTEGER NOT NULL DEFAULT 0," +
                "ciclos_completados      INTEGER NOT NULL DEFAULT 0," +
                "puntos_ganados          INTEGER NOT NULL DEFAULT 0," +
                "completado              INTEGER NOT NULL DEFAULT 0," +
                "iniciado_en             INTEGER NOT NULL," +
                "finalizado_en           INTEGER" +
                ")");
        db.execSQL("CREATE INDEX idx_pomodoro_usuario ON " + T_POMODORO + "(usuario_id, finalizado_en DESC)");

        // --------------------------------------------------------------
        // 12. ACTIVIDAD_DIALARIA  (sustenta el cálculo de la racha)
        //     Una fila por usuario por día; las rachas se derivan de aquí
        //     para que el "día en curso" nunca rompa la racha.
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_ACTIVIDAD + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id          INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "fecha               TEXT    NOT NULL," +  // 'YYYY-MM-DD' local
                "secciones_leidas    INTEGER NOT NULL DEFAULT 0," +
                "minutos_pomodoro    INTEGER NOT NULL DEFAULT 0," +
                "puntos              INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE(usuario_id, fecha)" +
                ")");

        // --------------------------------------------------------------
        // 13. GRUPO_MIEMBROS
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_GRUPO_MIEMBROS + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "grupo_id            INTEGER NOT NULL REFERENCES " + T_GRUPOS + "(id) ON DELETE CASCADE," +
                "usuario_id          INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "rol                 TEXT    NOT NULL DEFAULT 'miembro'," + // creador|miembro
                "puntos_semanales    INTEGER NOT NULL DEFAULT 0," +
                "unido_en            INTEGER NOT NULL," +
                "UNIQUE(grupo_id, usuario_id)" +
                ")");
        db.execSQL("CREATE INDEX idx_miembros_usuario ON " + T_GRUPO_MIEMBROS + "(usuario_id)");

        // --------------------------------------------------------------
        // 14. NOTAS_COMPARTIDAS  (notas del módulo colaborativo)
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_NOTAS_COMPARTIDAS + " (" +
                "id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "grupo_id            INTEGER NOT NULL REFERENCES " + T_GRUPOS + "(id) ON DELETE CASCADE," +
                "usuario_id          INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "contenido           TEXT    NOT NULL," +
                "creada_en           INTEGER NOT NULL," +
                "sincronizada        INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX idx_notas_comp_grupo ON " + T_NOTAS_COMPARTIDAS + "(grupo_id, creada_en DESC)");

        // --------------------------------------------------------------
        // 15. COMENTARIOS_NOTA
        // --------------------------------------------------------------
        db.execSQL("CREATE TABLE " + T_COMENTARIOS + " (" +
                "id                      INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nota_compartida_id      INTEGER NOT NULL REFERENCES " +
                T_NOTAS_COMPARTIDAS + "(id) ON DELETE CASCADE," +
                "usuario_id              INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "texto                   TEXT    NOT NULL," +
                "creada_en               INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_comentarios_nota ON " + T_COMENTARIOS + "(nota_compartida_id)");

        // --------------------------------------------------------------
        // 16. INVENTARIO_OBJETOS  (poderes: barita mágica, etc.)
        // --------------------------------------------------------------
        crearTablaObjetos(db);
    }

    /** Tabla de objetos/poderes del inventario. Idempotente. */
    private void crearTablaObjetos(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_INV_OBJETOS + " (" +
                "id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id      INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "tipo            TEXT    NOT NULL," +   // 'barita_magica', etc.
                "cantidad        INTEGER NOT NULL DEFAULT 0," +
                "obtenido_en     INTEGER NOT NULL," +
                "UNIQUE(usuario_id, tipo)" +
                ")");
    }

    // ==================================================================
    // MÓDULO DE EVALUACIÓN (v2) - tipo Duolingo
    // ==================================================================

    /**
     * Umbral para aprobar un tramo: 80% de aciertos.
     * Constante en Java y no en la tabla porque el backend debe aplicar
     * exactamente la misma regla.
     */
    public static final float UMBRAL_APROBACION = 0.80f;

    /**
     * Crea las 6 tablas de evaluación.
     * Se invocan tanto desde onCreate (BD nueva) como desde onUpgrade (BD v1),
     * por eso vive separada de {@link #crearEsquema(SQLiteDatabase)}.
     */
    private void crearTablasEvaluacion(SQLiteDatabase db) {

        // --- NIVELES: los "tramos" del camino ---
        // categoria: 'sintomas' | 'sindromes' (dos bancos de preguntas).
        // orden: posicion dentro de la categoria (1..N).
        // numero: numero global unico; por convencion sintomas 1..99 y
        //         sindromes 101..199, de modo que nunca colisionan.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_NIVELES + " (" +
                "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "numero        INTEGER NOT NULL UNIQUE," +
                "categoria     TEXT    NOT NULL DEFAULT 'sintomas'," +
                "orden         INTEGER NOT NULL DEFAULT 0," +
                "nombre        TEXT    NOT NULL," +
                "descripcion   TEXT," +
                "tema          TEXT    NOT NULL," +
                "emoji         TEXT    NOT NULL DEFAULT '⭐'," +
                "total_preguntas INTEGER NOT NULL DEFAULT 12" +
                ")");

        // --- PREGUNTAS ---
        // tipo: 'mcq' (opción múltiple) | 'escrita' (texto libre)
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_PREGUNTAS + " (" +
                "id                INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nivel_id          INTEGER NOT NULL REFERENCES " + T_NIVELES + "(id) ON DELETE CASCADE," +
                "tema              TEXT    NOT NULL," +
                "tipo              TEXT    NOT NULL," +
                "enunciado         TEXT    NOT NULL," +
                "pista             TEXT," +                  // pista opcional antes de responder
                // Para tipo 'escrita': sinónimos separados por '|'.
                // Para tipo 'mcq': se deja NULL y mandan las opciones.
                "respuestas_validas TEXT," +
                "justificacion     TEXT    NOT NULL," +     // siempre: es lo que enseña
                "puntos            INTEGER NOT NULL DEFAULT 15," +
                "orden             INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_preguntas_nivel ON " + T_PREGUNTAS + "(nivel_id, orden)");

        // --- OPCIONES (sólo para tipo 'mcq') ---
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_OPCIONES + " (" +
                "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pregunta_id   INTEGER NOT NULL REFERENCES " + T_PREGUNTAS + "(id) ON DELETE CASCADE," +
                "texto         TEXT    NOT NULL," +
                "es_correcta   INTEGER NOT NULL DEFAULT 0," +
                "orden         INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_opciones_pregunta ON " + T_OPCIONES + "(pregunta_id, orden)");

        // --- INTENTOS: una partida, en modo examen o práctica ---
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_INTENTOS + " (" +
                "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id    INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "nivel_id      INTEGER NOT NULL REFERENCES " + T_NIVELES + "(id) ON DELETE CASCADE," +
                "modo          TEXT    NOT NULL DEFAULT 'examen'," +  // examen|practica
                "total         INTEGER NOT NULL DEFAULT 0," +
                "acertadas     INTEGER NOT NULL DEFAULT 0," +
                "puntos        INTEGER NOT NULL DEFAULT 0," +
                "aprobado      INTEGER NOT NULL DEFAULT 0," +
                "iniciado_en   INTEGER NOT NULL," +
                "finalizado_en INTEGER" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_intentos_usuario ON " + T_INTENTOS + "(usuario_id, iniciado_en DESC)");

        // --- RESPUESTAS_DADAS: auditoría de cada intento ---
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_RESPUESTAS_DADAS + " (" +
                "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "intento_id    INTEGER NOT NULL REFERENCES " + T_INTENTOS + "(id) ON DELETE CASCADE," +
                "pregunta_id   INTEGER NOT NULL REFERENCES " + T_PREGUNTAS + "(id) ON DELETE CASCADE," +
                // texto tal cual lo escribió el estudiante (para auditoría docente)
                "texto_ingresado TEXT," +
                "opcion_id     INTEGER REFERENCES " + T_OPCIONES + "(id) ON DELETE SET NULL," +
                "correcta      INTEGER NOT NULL DEFAULT 0," +
                "puntos        INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_respuestas_intento ON " + T_RESPUESTAS_DADAS + "(intento_id)");

        // --- PROGRESO_NIVELES: estado del tramo por usuario ---
        // UNIQUE impide duplicar filas del mismo tramo.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_PROGRESO_NIVELES + " (" +
                "id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario_id      INTEGER NOT NULL REFERENCES " + T_USUARIOS + "(id) ON DELETE CASCADE," +
                "nivel_id        INTEGER NOT NULL REFERENCES " + T_NIVELES + "(id) ON DELETE CASCADE," +
                "aprobado        INTEGER NOT NULL DEFAULT 0," +
                "mejor_porcentaje INTEGER NOT NULL DEFAULT 0," +  // mejor % de aciertos
                "mejor_puntaje   INTEGER NOT NULL DEFAULT 0," +
                "intentos        INTEGER NOT NULL DEFAULT 0," +
                "completado_en   INTEGER," +
                "UNIQUE(usuario_id, nivel_id)" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_prog_niveles_usuario ON " + T_PROGRESO_NIVELES + "(usuario_id)");
    }

    // ==================================================================
    // DATOS SEMILLA (catálogos). En producción esto se sirve desde la API.
    // ==================================================================
    private void sembrarDatosIniciales(SQLiteDatabase db) {
        long ahora = System.currentTimeMillis();

        // Accesorios desbloqueables por nivel
        db.execSQL("INSERT INTO " + T_ACCESORIOS + " (nombre, emoji, nivel_requerido, descripcion, precio_puntos) VALUES " +
                "('Sin accesorio',   '🚫', 1, 'Tu mascota va desnuda.',        0)," +
                "('Collar básico',   '🔵', 1, 'Un collar sencillo.',          0)," +
                "('Juguete',         '🎾', 2, 'Pelota para jugar.',           25)," +
                "('Bufanda',         '🧣', 3, 'Abriga a tu mascota.',         60)," +
                "('Corona',          '👑', 5, 'Para las mascotas-locker.',   150)," +
                "('Lentes',          '🕶️', 7, 'Estilo semiologo.',            300)");

        // Alimentos
        db.execSQL("INSERT INTO " + T_ALIMENTOS + " (nombre, emoji, puntos_hambre, puntos_felicidad, precio_puntos) VALUES " +
                "('Galleta',  '🍪', 10,  3,  10)," +
                "('Sopa',     '🥣', 20,  8,  20)," +
                "('Pescado',  '🐟', 30, 15,  30)," +
                "('Bowl gourmet','🍲',45, 25,  50)");

        // Guía completa: dos caminos (Síntomas y Síndromes).
        sembrarGuiaCompleta(db);

    }

    private void sembrarGuiaCompleta(SQLiteDatabase db) {
        insertarSeccion(db, "sintomas", "Conciencia", "Psicopatología de la conciencia", 1, 15, 20, contenidoGuia("sintomas_1"));
        insertarSeccion(db, "sintomas", "Orientación", "Psicopatología de la orientación", 2, 12, 20, contenidoGuia("sintomas_2"));
        insertarSeccion(db, "sintomas", "Atención y concentración", "Psicopatología de la atención y concentración", 3, 15, 20, contenidoGuia("sintomas_3"));
        insertarSeccion(db, "sintomas", "Memoria", "Psicopatología de la memoria", 4, 18, 25, contenidoGuia("sintomas_4"));
        insertarSeccion(db, "sintomas", "Percepción", "Percepción, imaginación y sensaciones", 5, 20, 25, contenidoGuia("sintomas_5"));
        insertarSeccion(db, "sintomas", "Pensamiento", "Psicopatología del pensamiento", 6, 22, 30, contenidoGuia("sintomas_6"));
        insertarSeccion(db, "sintomas", "Lenguaje", "Psicopatología del lenguaje", 7, 18, 25, contenidoGuia("sintomas_7"));
        insertarSeccion(db, "sintomas", "Afectividad", "Psicopatología de la afectividad", 8, 20, 25, contenidoGuia("sintomas_8"));
        insertarSeccion(db, "sintomas", "Psicomotricidad", "Psicopatología de la psicomotricidad", 9, 18, 25, contenidoGuia("sintomas_9"));
        insertarSeccion(db, "sintomas", "Voluntad y conducta", "Alteraciones de la voluntad y conducta", 10, 16, 25, contenidoGuia("sintomas_10"));
        insertarSeccion(db, "sintomas", "Funciones fisiológicas", "Alteraciones de las funciones fisiológicas", 11, 15, 20, contenidoGuia("sintomas_11"));
        insertarSeccion(db, "sintomas", "Sueño", "Psicopatología del sueño", 12, 15, 20, contenidoGuia("sintomas_12"));
        insertarSeccion(db, "sintomas", "Apetito e ingesta", "Alteraciones del apetito y la ingesta", 13, 12, 20, contenidoGuia("sintomas_13"));
        insertarSeccion(db, "sintomas", "Sexualidad", "Psicopatología de la sexualidad", 14, 12, 20, contenidoGuia("sintomas_14"));
        insertarSeccion(db, "sintomas", "Funciones de relación", "Alteraciones de las funciones de relación", 15, 15, 25, contenidoGuia("sintomas_15"));
        insertarSeccion(db, "sindromes", "Síndromes orgánicos", "Síndromes cerebrales orgánicos agudos", 1, 12, 20, contenidoGuia("sindromes_1"));
        insertarSeccion(db, "sindromes", "Síndromes orgánicos", "Síndromes cerebrales orgánicos crónicos", 2, 12, 20, contenidoGuia("sindromes_2"));
        insertarSeccion(db, "sindromes", "Esquizofrenia", "Síndrome esquizofrénico", 3, 15, 25, contenidoGuia("sindromes_3"));
        insertarSeccion(db, "sindromes", "Delirios", "Síndrome delirante", 4, 15, 25, contenidoGuia("sindromes_4"));
        insertarSeccion(db, "sindromes", "Afectividad", "Síndromes afectivos", 5, 15, 25, contenidoGuia("sindromes_5"));
        insertarSeccion(db, "sindromes", "Psicomotricidad", "Síndromes discinéticos", 6, 12, 20, contenidoGuia("sindromes_6"));
        insertarSeccion(db, "sindromes", "Hipocondría", "Síndrome hipocondríaco", 7, 10, 20, contenidoGuia("sindromes_7"));
    }

    private void actualizarContenidoGuiaCompleta(SQLiteDatabase db) {
        actualizarSeccionGuia(db, "sintomas", 1, "Conciencia", "Psicopatología de la conciencia", 15, 20);
        actualizarSeccionGuia(db, "sintomas", 2, "Orientación", "Psicopatología de la orientación", 12, 20);
        actualizarSeccionGuia(db, "sintomas", 3, "Atención y concentración", "Psicopatología de la atención y concentración", 15, 20);
        actualizarSeccionGuia(db, "sintomas", 4, "Memoria", "Psicopatología de la memoria", 18, 25);
        actualizarSeccionGuia(db, "sintomas", 5, "Percepción", "Percepción, imaginación y sensaciones", 20, 25);
        actualizarSeccionGuia(db, "sintomas", 6, "Pensamiento", "Psicopatología del pensamiento", 22, 30);
        actualizarSeccionGuia(db, "sintomas", 7, "Lenguaje", "Psicopatología del lenguaje", 18, 25);
        actualizarSeccionGuia(db, "sintomas", 8, "Afectividad", "Psicopatología de la afectividad", 20, 25);
        actualizarSeccionGuia(db, "sintomas", 9, "Psicomotricidad", "Psicopatología de la psicomotricidad", 18, 25);
        actualizarSeccionGuia(db, "sintomas", 10, "Voluntad y conducta", "Alteraciones de la voluntad y conducta", 16, 25);
        actualizarSeccionGuia(db, "sintomas", 11, "Funciones fisiológicas", "Alteraciones de las funciones fisiológicas", 15, 20);
        actualizarSeccionGuia(db, "sintomas", 12, "Sueño", "Psicopatología del sueño", 15, 20);
        actualizarSeccionGuia(db, "sintomas", 13, "Apetito e ingesta", "Alteraciones del apetito y la ingesta", 12, 20);
        actualizarSeccionGuia(db, "sintomas", 14, "Sexualidad", "Psicopatología de la sexualidad", 12, 20);
        actualizarSeccionGuia(db, "sintomas", 15, "Funciones de relación", "Alteraciones de las funciones de relación", 15, 25);
        actualizarSeccionGuia(db, "sindromes", 1, "Síndromes orgánicos", "Síndromes cerebrales orgánicos agudos", 12, 20);
        actualizarSeccionGuia(db, "sindromes", 2, "Síndromes orgánicos", "Síndromes cerebrales orgánicos crónicos", 12, 20);
        actualizarSeccionGuia(db, "sindromes", 3, "Esquizofrenia", "Síndrome esquizofrénico", 15, 25);
        actualizarSeccionGuia(db, "sindromes", 4, "Delirios", "Síndrome delirante", 15, 25);
        actualizarSeccionGuia(db, "sindromes", 5, "Afectividad", "Síndromes afectivos", 15, 25);
        actualizarSeccionGuia(db, "sindromes", 6, "Psicomotricidad", "Síndromes discinéticos", 12, 20);
        actualizarSeccionGuia(db, "sindromes", 7, "Hipocondría", "Síndrome hipocondríaco", 10, 20);
    }

    private void actualizarSeccionGuia(SQLiteDatabase db,
                                       String camino,
                                       int orden,
                                       String tema,
                                       String titulo,
                                       int duracion,
                                       int puntos) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("tema", tema);
        values.put("titulo", titulo);
        values.put("contenido", contenidoGuia(camino + "_" + orden));
        values.put("duracion_estimada_min", duracion);
        values.put("puntos_recompensa", puntos);

        int filas = db.update(
                T_SECCIONES,
                values,
                "camino = ? AND orden = ?",
                new String[]{camino, String.valueOf(orden)}
        );

        if (filas == 0) {
            insertarSeccion(db, camino, tema, titulo, orden, duracion, puntos,
                    contenidoGuia(camino + "_" + orden));
        }
    }

    private void insertarSeccion(SQLiteDatabase db,
                                 String camino,
                                 String tema,
                                 String titulo,
                                 int orden,
                                 int duracion,
                                 int puntos,
                                 String contenido) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("camino", camino);
        values.put("tema", tema);
        values.put("titulo", titulo);
        values.put("contenido", contenido);
        values.put("orden", orden);
        values.put("duracion_estimada_min", duracion);
        values.put("puntos_recompensa", puntos);
        db.insertOrThrow(T_SECCIONES, null, values);
    }

    private String contenidoGuia(String clave) {
        switch (clave) {
            case "sintomas_1":
                return "<h2>Psicopatología de la conciencia</h2><p>La conciencia permite mantener la vigilia, integrar la experiencia y responder al entorno. En semiología se distinguen alteraciones cuantitativas, relacionadas con el grado de alerta, y cualitativas, que modifican la organización global de la experiencia.</p><h3>Alteraciones cuantitativas</h3><p>La hipervigilancia implica un aumento del estado de alerta. En sentido contrario pueden aparecer obnubilación, somnolencia o sopor y estupor, con disminución progresiva de la capacidad para responder a estímulos.</p><h3>Alteraciones cualitativas</h3><p>Los estados confusionales afectan atención, comprensión, memoria y orientación. El delirium es un cuadro agudo de origen orgánico con alteración global de conciencia y cognición. El estado crepuscular presenta un estrechamiento transitorio del campo de conciencia.</p><h3>Para recordar</h3><p>La exploración incluye nivel de vigilia, respuesta a estímulos, atención, orientación y coherencia de la conducta.</p>";
            case "sintomas_2":
                return "<h2>Psicopatología de la orientación</h2><p>La orientación es la capacidad de situarse respecto de uno mismo y del ambiente. Depende de conciencia, atención y memoria.</p><h3>Autopsíquica</h3><p>Corresponde al reconocimiento de la propia identidad y datos personales básicos.</p><h3>Alopsíquica</h3><p>Incluye orientación temporal, espacial y respecto de las personas y circunstancias del entorno.</p><h3>Desorientación</h3><p>Puede ser parcial, global o fluctuante. En algunos cuadros se conserva la identidad personal mientras se pierde la ubicación respecto al ambiente.</p><h3>Exploración</h3><p>Debe valorarse junto con conciencia, memoria reciente, atención y comprensión.</p>";
            case "sintomas_3":
                return "<h2>Psicopatología de la atención y concentración</h2><p>La atención selecciona información relevante y la concentración permite mantener el foco.</p><h3>Distraibilidad</h3><p>Existe dificultad para sostener la atención en un estímulo, tema o tarea; el pensamiento puede desviarse con facilidad.</p><h3>Hipervigilancia</h3><p>La persona permanece pendiente de numerosas señales externas o internas, pero puede tener dificultad para concentrarse de forma estable en una sola.</p><h3>Fatigabilidad y apatía</h3><p>La fatigabilidad produce descenso del rendimiento y más errores al mantener el esfuerzo atencional. La apatía atencional implica escaso interés por estímulos que normalmente captarían la atención.</p><h3>Perplejidad</h3><p>La persona atiende pero tiene dificultad para sintetizar y comprender el contenido de lo observado.</p>";
            case "sintomas_4":
                return "<h2>Psicopatología de la memoria</h2><p>La memoria permite almacenar, conservar y recuperar información.</p><h3>Hipomnesia</h3><p>Es una disminución del rendimiento mnésico. Puede afectar especialmente hechos recientes o la evocación de experiencias previas.</p><h3>Amnesia</h3><p>Es una pérdida importante de recuerdos. La amnesia de fijación dificulta consolidar información nueva; la de conservación afecta recuerdos almacenados; y la de evocación dificulta recuperar información disponible.</p><h3>Extensión</h3><p>La pérdida puede ser global, limitarse a un período concreto o afectar contenidos específicos.</p><h3>Evaluación</h3><p>Se comparan memoria inmediata, reciente y remota considerando también atención, conciencia y estado emocional.</p>";
            case "sintomas_5":
                return "<h2>Percepción, imaginación y sensaciones</h2><p>La percepción organiza información sensorial y le atribuye significado. Sus alteraciones pueden modificar la intensidad, cualidad o interpretación de la experiencia.</p><h3>Distorsiones</h3><p>La sensibilidad puede aumentar o disminuir y también pueden cambiar cualidades de los estímulos percibidos.</p><h3>Ilusiones y alucinaciones</h3><p>En la ilusión existe un estímulo real interpretado incorrectamente. En la alucinación aparece una experiencia perceptiva sin el estímulo externo correspondiente.</p><h3>Desrealización</h3><p>El entorno se siente extraño o poco real aunque la persona pueda reconocer que sigue perteneciendo a la realidad.</p><h3>Despersonalización</h3><p>La extrañeza se centra en la propia identidad, pensamientos, sentimientos o acciones.</p>";
            case "sintomas_6":
                return "<h2>Psicopatología del pensamiento</h2><p>El pensamiento permite elaborar ideas, planificar, evaluar y relacionar información. Se estudian su origen, velocidad, continuidad, estructura y contenido.</p><h3>Origen</h3><p>El material describe el pensamiento autista como un pensamiento fuertemente centrado en vivencias internas y apartado de la realidad compartida.</p><h3>Curso</h3><p>La bradipsiquia corresponde a una producción de ideas enlentecida. También puede existir aceleración, bloqueos, perseveración o cambios frecuentes del hilo asociativo.</p><h3>Organización</h3><p>Se valora si las ideas mantienen relaciones comprensibles, si el discurso llega al objetivo y si conserva continuidad.</p><h3>Contenido</h3><p>Se exploran los temas predominantes, el grado de convicción y la repercusión de las ideas sobre la conducta.</p>";
            case "sintomas_7":
                return "<h2>Psicopatología del lenguaje y habla</h2><p>Las alteraciones pueden afectar comprensión, producción, lectura, escritura, articulación o prosodia.</p><h3>Afasia</h3><p>Es una alteración adquirida del lenguaje asociada a lesión cerebral y puede comprometer producción o comprensión oral y escrita.</p><h3>Otros fenómenos</h3><p>El agramatismo dificulta organizar frases; la anomia dificulta encontrar palabras; la alexia afecta la lectura adquirida y la agrafia la escritura adquirida.</p><h3>Habla</h3><p>La disartria afecta la ejecución motora del habla; la dislalia compromete la articulación de sonidos; la aprosodia afecta entonación y musicalidad.</p><h3>Exploración</h3><p>Se observan lenguaje espontáneo, denominación, comprensión, repetición, lectura, escritura, ritmo y articulación.</p>";
            case "sintomas_8":
                return "<h2>Psicopatología de la afectividad</h2><p>La afectividad incluye emociones, sentimientos, impulsos motivacionales y estados de ánimo.</p><h3>Tono afectivo</h3><p>La eutimia corresponde a un estado equilibrado. La hipertimia representa aumento del tono afectivo y la hipotimia una disminución.</p><h3>Calidad y estabilidad</h3><p>También se estudian labilidad, ambivalencia, discordancia con el contexto y reducción de la respuesta emocional.</p><h3>Observación</h3><p>Se consideran expresión facial, tono de voz, reactividad, duración del estado emocional y congruencia con el tema tratado.</p><h3>Clave</h3><p>Una emoción aislada no define una alteración; importan intensidad, persistencia, contexto y repercusión funcional.</p>";
            case "sintomas_9":
                return "<h2>Psicopatología de la psicomotricidad</h2><p>La psicomotricidad expresa la relación entre actividad mental y movimiento.</p><h3>Cantidad de actividad</h3><p>Puede existir reducción o enlentecimiento del movimiento, o aumento de la actividad hasta grados de inquietud marcada.</p><h3>Estereotipias</h3><p>Son repeticiones persistentes de movimientos o gestos organizados que no resultan necesarios para lograr un objetivo.</p><h3>Automatismos</h3><p>Son secuencias motoras realizadas de manera automática y con escaso control consciente, que pueden aparecer en estados alterados de conciencia.</p><h3>Exploración</h3><p>Se observan postura, gestos, velocidad, finalidad de los movimientos, respuesta a instrucciones y relación con el estado mental.</p>";
            case "sintomas_10":
                return "<h2>Alteraciones de la voluntad y conducta</h2><p>La función conativa permite iniciar, mantener y dirigir acciones hacia objetivos.</p><h3>Abulia e hipobulia</h3><p>La abulia implica una reducción muy marcada de iniciativa y voluntad; la hipobulia representa una disminución menos intensa.</p><h3>Hiperbulia</h3><p>Supone un incremento de la actividad volitiva. Mucha actividad no significa necesariamente que la conducta sea organizada o productiva.</p><h3>Conducta</h3><p>Se valora finalidad, organización, control, adaptación al medio, autocuidado, hábitos e interacción social.</p><h3>Exploración</h3><p>Conviene comparar la capacidad para iniciar y terminar actividades con el funcionamiento habitual de la persona.</p>";
            case "sintomas_11":
                return "<h2>Funciones fisiológicas</h2><p>La evaluación psicopatológica considera funciones biológicas estrechamente relacionadas con el estado mental, especialmente sueño, apetito e ingesta y sexualidad.</p><h3>Sueño</h3><p>Se exploran horario, continuidad, descanso percibido, despertares y fenómenos que aparecen durante el sueño.</p><h3>Apetito e ingesta</h3><p>Se valoran cambios persistentes en apetito, cantidad o patrón de alimentación y su relación con el bienestar general.</p><h3>Sexualidad</h3><p>Se estudian cambios relevantes en interés y funcionamiento desde una perspectiva clínica, privada y respetuosa.</p><h3>Integración</h3><p>Los cambios deben interpretarse junto con causas médicas, estado afectivo, medicamentos y funcionamiento general.</p>";
            case "sintomas_12":
                return "<h2>Psicopatología del sueño</h2><p>La semiología del sueño incluye dificultades para iniciar o mantener el sueño, exceso de somnolencia y fenómenos conductuales durante determinadas fases.</p><h3>Insomnio e hipersomnia</h3><p>El insomnio puede manifestarse al conciliar, mantener o finalizar el sueño. La hipersomnia implica sueño o somnolencia excesivos con repercusión diurna.</p><h3>Parasomnias</h3><p>Incluyen fenómenos como pesadillas, terrores nocturnos y sonambulismo, diferenciables por la fase del sueño, el recuerdo posterior y la conducta observada.</p><h3>Evaluación</h3><p>Se registran horario, duración, frecuencia, factores asociados y consecuencias durante el día.</p>";
            case "sintomas_13":
                return "<h2>Alteraciones del apetito y la ingesta</h2><p>Se estudian cambios en el deseo de comer, la cantidad ingerida y el papel psicológico de la alimentación. La valoración clínica no debe basarse únicamente en la apariencia corporal.</p><h3>Disminución del apetito</h3><p>Puede existir una reducción parcial o marcada del deseo de comer y debe diferenciarse de causas médicas, emocionales y otros factores.</p><h3>Aumento o pérdida de control</h3><p>También pueden aparecer aumentos persistentes del apetito o episodios de ingesta percibidos como difíciles de controlar.</p><h3>Evaluación</h3><p>Se exploran regularidad, cambios recientes, emociones asociadas, señales físicas y repercusión en la salud. Los problemas alimentarios requieren valoración profesional.</p>";
            case "sintomas_14":
                return "<h2>Psicopatología de la sexualidad</h2><p>En semiología clínica la sexualidad se aborda como una función humana relacionada con bienestar, interés, respuesta y vínculos.</p><h3>Cambios del interés</h3><p>El interés puede aumentar o disminuir por estados emocionales, condiciones médicas, medicamentos u otros factores.</p><h3>Evaluación</h3><p>La entrevista se centra en bienestar, consentimiento, seguridad, funcionamiento y cambios significativos, utilizando lenguaje profesional y respetuoso.</p><h3>Integración</h3><p>Los cambios se interpretan junto con afectividad, voluntad, sueño, salud física y otras funciones fisiológicas.</p>";
            case "sintomas_15":
                return "<h2>Alteraciones de las funciones de relación</h2><p>Describen cómo la persona se vincula consigo misma, con otras personas y con sus intereses y actividades.</p><h3>Consigo mismo</h3><p>Se exploran autoconcepto, valoración de cualidades y dificultades y percepción de cómo la valoran los demás.</p><h3>Con otras personas</h3><p>Se estudian patrones de comunicación, confianza, cercanía, conflictos y cambios en las relaciones.</p><h3>Con las cosas e intereses</h3><p>Se investigan intereses, ideales, motivaciones, actividades significativas y uso del tiempo libre.</p><h3>Integración</h3><p>Estas funciones se interpretan junto con personalidad previa, contexto social, afectividad, pensamiento y conducta.</p>";
            case "sindromes_1":
                return "<h2>Síndromes cerebrales orgánicos agudos</h2><p>Son cuadros generalmente bruscos en los que se alteran funciones de síntesis y cognitivas. La disminución o fluctuación del nivel de vigilia es un elemento central.</p><h3>Obnubilación</h3><p>Predominan vigilia disminuida, atención distraíble, memoria reducida, orientación limitada y pensamiento lento.</p><h3>Delirium</h3><p>Combina alteración de vigilia, atención inestable, memoria y comprensión disminuidas, orientación fluctuante y posibles cambios sensoperceptivos y conductuales.</p><h3>Oniroide</h3><p>Existe disminución de la vigilia y marcada absorción en vivencias internas; la orientación respecto del ambiente puede afectarse más que la identidad personal.</p><h3>Otros patrones</h3><p>El estado crepuscular estrecha intensamente la conciencia; la confusión mental compromete profundamente atención, memoria, comprensión y orientación.</p>";
            case "sindromes_2":
                return "<h2>Síndromes cerebrales orgánicos crónicos</h2><p>En contraste con los agudos, la vigilia suele estar relativamente conservada y destacan alteraciones persistentes de capacidades intelectuales, memoria, carácter y organización de la personalidad.</p><h3>Oligofrénico</h3><p>Se describe con dificultades atencionales, capacidades intelectuales muy disminuidas y pensamiento predominantemente concreto.</p><h3>Demencial</h3><p>Implica deterioro adquirido y persistente de funciones cognitivas con repercusión sobre el funcionamiento cotidiano.</p><h3>Amnésico-confabulatorio</h3><p>Predomina la alteración de memoria reciente y pueden aparecer confabulaciones, con conservación relativa de algunos aspectos de orientación.</p><h3>Apatoabúlico</h3><p>Destacan indiferencia, reducción de iniciativa, menor actividad y deterioro de hábitos.</p>";
            case "sindromes_3":
                return "<h2>Síndrome esquizofrénico</h2><p>El documento lo caracteriza por una desorganización importante de las funciones psíquicas y por una pérdida de integración entre pensamiento, afectividad y conducta.</p><h3>Síntesis</h3><p>Vigilia, orientación y algunos aspectos de memoria pueden mantenerse relativamente conservados.</p><h3>Cognición</h3><p>Pueden aparecer experiencias perceptivas anormales, pensamiento centrado en vivencias internas, bloqueos y desorganización asociativa.</p><h3>Afectividad y conducta</h3><p>Puede existir discordancia o ambivalencia afectiva, reducción de la voluntad, aislamiento y conductas difíciles de comprender desde el contexto inmediato.</p><h3>Clave</h3><p>El patrón se reconoce integrando múltiples dominios; ningún síntoma aislado define todo el síndrome.</p>";
            case "sindromes_4":
                return "<h2>Síndrome delirante</h2><p>Su rasgo central es la presencia de ideas delirantes con afectación de las funciones de relación y conservación relativa de varias funciones de síntesis.</p><h3>Paranoico</h3><p>La comunicación y orientación suelen conservarse y el pensamiento puede organizarse alrededor de una idea delirante con argumentación aparentemente lógica.</p><h3>Paranoide</h3><p>Puede existir recelo, hipervigilancia, alteraciones perceptivas y afectación más global de las relaciones.</p><h3>Automatismo psíquico</h3><p>Se describen experiencias de extrañeza respecto al pensamiento, al cuerpo o a la realidad y creencias de influencia sobre la propia actividad mental.</p><h3>Exploración</h3><p>Se consideran convicción, estructura, percepción, orientación, afectividad, conducta y repercusión interpersonal.</p>";
            case "sindromes_5":
                return "<h2>Síndromes afectivos</h2><p>Predominan alteraciones de la afectividad y cambios globales de actividad y necesidades, con sensopercepción relativamente conservada.</p><h3>Maníaco</h3><p>Puede existir aumento del tono afectivo, labilidad, mayor actividad, pensamiento acelerado y fuga de ideas.</p><h3>Depresivo</h3><p>Predominan disminución del tono afectivo, pensamiento más lento, menor iniciativa y actividad, retraimiento y reducción de intereses. También pueden cambiar sueño, apetito y hábitos. En cuadros graves pueden aparecer síntomas de alto riesgo que requieren evaluación profesional inmediata.</p><h3>Afectivo ansioso</h3><p>Se caracteriza por ansiedad, irritabilidad, vigilancia aumentada, preocupación anticipatoria, activación física y posibles cambios del sueño.</p><h3>Comparación</h3><p>Se integran ánimo, velocidad del pensamiento, nivel de actividad, necesidades fisiológicas y relación con el entorno.</p>";
            case "sindromes_6":
                return "<h2>Síndromes discinéticos</h2><p>Se reconocen principalmente por alteraciones marcadas de la actividad psicomotora.</p><h3>Estuporoso</h3><p>Predominan inmovilidad, reducción extrema de actividad y disminución o ausencia de respuesta verbal. El contexto clínico permite diferenciar variantes.</p><h3>Hipercinético</h3><p>Existe aumento importante de la actividad motora, con inquietud que puede ser intensa y afectar la adaptación al medio.</p><h3>Modalidades</h3><p>El material relaciona diferentes formas de aumento psicomotor con cuadros catatónicos, afectivos, disociativos u orgánicos.</p><h3>Exploración</h3><p>Se observan cantidad y finalidad del movimiento, postura, respuesta a instrucciones, lenguaje y relación con conciencia, pensamiento y afectividad.</p>";
            case "sindromes_7":
                return "<h2>Síndrome hipocondríaco</h2><p>Se caracteriza por preocupación excesiva y persistente por la salud y autoobservación continua de sensaciones y funciones corporales.</p><h3>Atención</h3><p>La vigilia y orientación suelen conservarse, mientras la atención se dirige intensamente hacia el propio cuerpo.</p><h3>Afectividad</h3><p>La ansiedad es relevante y la preocupación puede dominar la conversación y la interpretación de sensaciones corporales.</p><h3>Conducta</h3><p>Pueden aparecer consultas reiteradas y vigilancia frecuente de señales físicas.</p><h3>Clave</h3><p>El patrón implica intensidad, persistencia, focalización corporal y repercusión significativa, y debe diferenciarse de condiciones médicas reales.</p>";
            default:
                return "<h2>Contenido no disponible</h2><p>Esta sección todavía no tiene contenido.</p>";
        }
    }
}
package com.utm.semiologia.data.db;

import android.content.Context;
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
    public static final int DB_VERSION = 2;

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
        // Migraciones INCREMENTALES: nunca se hace DROP. Un usuario que ya
        // tenga la app instalada conserva su usuario, mascota, notas y progreso.
        if (oldVersion < 2) {
            migrarAV2(db);
        }
    }

    /**
     * v1 -> v2: módulo de evaluación.
     * Sólo se crean tablas nuevas; ninguna existente se toca.
     */
    private void migrarAV2(SQLiteDatabase db) {
        crearTablasEvaluacion(db);
        // La semilla de niveles/preguntas va aparte (assets/preguntas.json)
        // para que el contenido se pueda editar sin tocar el esquema.
        NivelesDao.sembrarNiveles(appContext, db);
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

        // --- NIVELES: los "tramos" del camino, de 1 a 5 ---
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_NIVELES + " (" +
                "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "numero        INTEGER NOT NULL UNIQUE," +   // 1..5, orden del camino
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

        // Contenido semilla de la guía de estudio (3 secciones, alineadas a los
        // 5 tramos de evaluación). PENDIENTE DE REVISIÓN DOCENTE: el texto es
        // material de apoyo y debe validarse contra el temario del curso.
        db.execSQL("INSERT INTO " + T_SECCIONES +
                " (tema, titulo, orden, duracion_estimada_min, puntos_recompensa, contenido) VALUES " +
                "('Fundamentos', 'La semiología psicopatológica: signo, síntoma y síndrome', 1, 12, 20, " +
                "'<p>La <b>semiología psicopatológica</b> estudia las manifestaciones de los " +
                "trastornos mentales a través de la observación y la entrevista.</p>" +
                "<p><b>Claves para el examen:</b></p>" +
                "<ul><li><b>Signo</b>: manifestación objetiva, observable por el examinador " +
                "(por ejemplo, agitación psicomotora).</li>" +
                "<li><b>Síntoma</b>: experiencia subjetiva referida por el paciente " +
                "(por ejemplo, tristeza o una alucinación).</li>" +
                "<li><b>Síndrome</b>: conjunto de signos y síntomas que se presentan juntos " +
                "y sugieren una entidad clínica.</li></ul>')," +
                "('Percepción y pensamiento', 'Alteraciones de la percepción y del pensamiento', 2, 12, 20, " +
                "'<p><b>Percepción:</b></p>" +
                "<ul><li><b>Alucinación</b>: percepción sin objeto externo correspondiente.</li>" +
                "<li><b>Ilusión</b>: percepción distorsionada de un objeto real.</li></ul>" +
                "<p>Las alucinaciones se clasifican según a quien atribuye el paciente la " +
                "percepción: es psicótica cuando la atribuye a una fuente externa.</p>" +
                "<p><b>Pensamiento:</b></p>" +
                "<ul><li><b>Delirio</b>: convicción falsa, firme e inmodificable ante la evidencia.</li>" +
                "<li><b>Curso</b>: fuga de ideas, tangencialidad, circunstancialidad.</li>" +
                "<li><b>Contenido</b>: delirios y obsesiones.</li></ul>" +
                "<p>Selecciona cualquier fragmento de este texto para crear una nota de estudio.</p>')," +
                "('Lenguaje, afectividad y cognición', 'Lenguaje, afectividad y cognición', 3, 15, 25, " +
                "'<p><b>Lenguaje:</b> parafasias, neologismos, verborrea, ecolalia y mutismo.</p>" +
                "<p><b>Afectividad:</b> aplanamiento afectivo, labilidad emocional y euforia. " +
                "El aplanamiento es la disminución de la expresividad; la labilidad, su " +
                "variación brusca e inmotivada.</p>" +
                "<p><b>Cognición:</b> funciones ejecutivas, memoria, atención, juicio e " +
                "insight. El insight es el grado en que el paciente reconoce su enfermedad.</p>')");
    }
}

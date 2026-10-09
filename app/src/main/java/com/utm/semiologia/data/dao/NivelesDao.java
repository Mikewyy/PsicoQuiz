package com.utm.semiologia.data.dao;

import com.utm.semiologia.firebase.FirebaseProgressSyncManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Intento;
import com.utm.semiologia.data.model.Nivel;
import com.utm.semiologia.data.model.Opcion;
import com.utm.semiologia.data.model.Pregunta;
import com.utm.semiologia.data.model.ProgresoNivel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a los 5 tramos, su banco de preguntas y el progreso del estudiante.
 *
 * La semilla de contenido vive en {@code assets/preguntas.json} y se inserta
 * una sola vez ({@link #sembrarNiveles(Context, SQLiteDatabase)}). El formato
 * de ese archivo esta validado por tools/validar-preguntas.mjs.
 */
public class NivelesDao {

    private final DatabaseHelper helper;

    public NivelesDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    // ==================================================================
    // SEMILLA DESDE ASSETS
    // ==================================================================

    /**
     * Inserta ambos bancos (síntomas + síndromes) desde assets.
     *
     * Idempotente por categoría. Se invoca desde {@code onCreate} y desde las
     * migraciones.
     */
    public static void sembrarNiveles(Context ctx, SQLiteDatabase db) {
        sembrarBanco(ctx, db, Nivel.CAT_SINTOMAS, "preguntas.json");
        sembrarBanco(ctx, db, Nivel.CAT_SINDROMES, "sindromes.json");
    }

    /**
     * Inserta sólo el banco de síndromes. Lo usa la migración v4->v5 para no
     * tocar el banco de síntomas que el usuario ya tiene.
     */
    public static void sembrarSindromes(Context ctx, SQLiteDatabase db) {
        sembrarBanco(ctx, db, Nivel.CAT_SINDROMES, "sindromes.json");
    }

    private static void sembrarBanco(Context ctx, SQLiteDatabase db,
                                     String categoria, String asset) {
        if (contarFilasCategoria(db, categoria) > 0) {
            return; // ya sembrado
        }

        String json = leerAsset(ctx, asset);
        if (json == null || json.isEmpty()) {
            return; // sin assets: la evaluacion queda vacia, no rompemos la app
        }

        try {
            JSONObject raiz = new JSONObject(json);
            JSONArray niveles = raiz.optJSONArray("niveles");
            if (niveles == null || niveles.length() == 0) return;

            db.beginTransaction();
            try {
                for (int i = 0; i < niveles.length(); i++) {
                    JSONObject n = niveles.getJSONObject(i);
                    long nivelId = insertarNivel(db, n, categoria, i + 1);
                    JSONArray preguntas = n.optJSONArray("preguntas");
                    if (preguntas == null) continue;
                    for (int j = 0; j < preguntas.length(); j++) {
                        JSONObject p = preguntas.getJSONObject(j);
                        long preguntaId = insertarPregunta(db, nivelId, p, j);
                        insertarOpciones(db, preguntaId, p.optJSONArray("opciones"));
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            // Un JSON corrupto no debe impedir abrir la app; se reintentara al
            // recrear la base. El validador de tools/ evita que llegue asi.
            android.util.Log.e("NivelesDao", "No se pudo sembrar " + asset, e);
        }
    }

    private static long insertarNivel(SQLiteDatabase db, JSONObject n,
                                      String categoria, int orden) {
        ContentValues cv = new ContentValues();
        cv.put("numero", numeroGlobal(categoria, orden));
        cv.put("categoria", categoria);
        cv.put("orden", orden);
        cv.put("nombre", n.optString("nombre"));
        cv.put("descripcion", n.optString("descripcion", null));
        cv.put("tema", n.optString("tema"));
        cv.put("emoji", n.optString("emoji", "⭐"));
        JSONArray preguntas = n.optJSONArray("preguntas");
        cv.put("total_preguntas", preguntas == null ? 0 : preguntas.length());
        return db.insert(DatabaseHelper.T_NIVELES, null, cv);
    }

    /**
     * numero global único por categoría: síntomas 1..99, síndromes 101..199.
     * El orden dentro de la categoría es el que se ordena, muestra y desbloquea.
     */
    private static int numeroGlobal(String categoria, int orden) {
        return (Nivel.CAT_SINDROMES.equals(categoria) ? 100 : 0) + orden;
    }

    private static int contarFilasCategoria(SQLiteDatabase db, String categoria) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.T_NIVELES
                + " WHERE categoria = ?", new String[]{categoria})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static long insertarPregunta(SQLiteDatabase db, long nivelId, JSONObject p, int orden) {
        ContentValues cv = new ContentValues();
        cv.put("nivel_id", nivelId);
        cv.put("tema", p.optString("tema"));
        cv.put("tipo", p.optString("tipo"));
        cv.put("enunciado", p.optString("enunciado"));
        cv.put("pista", p.optString("pista", null));
        cv.put("respuestas_validas", p.optString("respuestas_validas", null));
        cv.put("justificacion", p.optString("justificacion"));
        cv.put("puntos", p.optInt("puntos", 15));
        cv.put("orden", p.optInt("orden", orden));
        return db.insert(DatabaseHelper.T_PREGUNTAS, null, cv);
    }

    private static void insertarOpciones(SQLiteDatabase db, long preguntaId, JSONArray opciones) {
        if (opciones == null) return;
        for (int k = 0; k < opciones.length(); k++) {
            JSONObject o = opciones.optJSONObject(k);
            if (o == null) continue;
            ContentValues cv = new ContentValues();
            cv.put("pregunta_id", preguntaId);
            cv.put("texto", o.optString("texto"));
            cv.put("es_correcta", o.optBoolean("correcta", false) ? 1 : 0);
            cv.put("orden", k);
            db.insert(DatabaseHelper.T_OPCIONES, null, cv);
        }
    }

    // ==================================================================
    // NIVELES
    // ==================================================================

    public List<Nivel> listarNiveles() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Nivel> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_NIVELES, null, null, null,
                null, null, "categoria ASC, orden ASC")) {
            while (c.moveToNext()) lista.add(mapearNivel(c));
        }
        return lista;
    }

    public Nivel obtenerNivel(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_NIVELES, null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? mapearNivel(c) : null;
        }
    }

    public Nivel obtenerNivelSiguiente(String categoria, int orden) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_NIVELES, null,
                "categoria = ? AND orden = ?",
                new String[]{categoria, String.valueOf(orden)}, null, null, null)) {
            return c.moveToFirst() ? mapearNivel(c) : null;
        }
    }

    public Nivel obtenerNivelPorNumero(int numero) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_NIVELES, null, "numero = ?",
                new String[]{String.valueOf(numero)}, null, null, null)) {
            return c.moveToFirst() ? mapearNivel(c) : null;
        }
    }

    private Nivel mapearNivel(Cursor c) {
        Nivel n = new Nivel();
        n.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        n.setNumero(c.getInt(c.getColumnIndexOrThrow("numero")));
        n.setCategoria(c.getString(c.getColumnIndexOrThrow("categoria")));
        n.setOrden(c.getInt(c.getColumnIndexOrThrow("orden")));
        n.setNombre(c.getString(c.getColumnIndexOrThrow("nombre")));
        n.setDescripcion(c.getString(c.getColumnIndexOrThrow("descripcion")));
        n.setTema(c.getString(c.getColumnIndexOrThrow("tema")));
        n.setEmoji(c.getString(c.getColumnIndexOrThrow("emoji")));
        n.setTotalPreguntas(c.getInt(c.getColumnIndexOrThrow("total_preguntas")));
        return n;
    }

    // ==================================================================
    // PREGUNTAS
    // ==================================================================

    public List<Pregunta> listarPreguntas(long nivelId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Pregunta> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_PREGUNTAS, null, "nivel_id = ?",
                new String[]{String.valueOf(nivelId)}, null, null, "orden ASC")) {
            while (c.moveToNext()) {
                Pregunta p = mapearPregunta(c);
                p.setOpciones(listarOpciones(db, p.getId()));
                lista.add(p);
            }
        }
        return lista;
    }

    private Pregunta mapearPregunta(Cursor c) {
        Pregunta p = new Pregunta();
        p.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        p.setNivelId(c.getLong(c.getColumnIndexOrThrow("nivel_id")));
        p.setTema(c.getString(c.getColumnIndexOrThrow("tema")));
        p.setTipo(c.getString(c.getColumnIndexOrThrow("tipo")));
        p.setEnunciado(c.getString(c.getColumnIndexOrThrow("enunciado")));
        p.setPista(c.getString(c.getColumnIndexOrThrow("pista")));
        p.setRespuestasValidas(c.getString(c.getColumnIndexOrThrow("respuestas_validas")));
        p.setJustificacion(c.getString(c.getColumnIndexOrThrow("justificacion")));
        p.setPuntos(c.getInt(c.getColumnIndexOrThrow("puntos")));
        p.setOrden(c.getInt(c.getColumnIndexOrThrow("orden")));
        return p;
    }

    private List<Opcion> listarOpciones(SQLiteDatabase db, long preguntaId) {
        List<Opcion> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_OPCIONES, null, "pregunta_id = ?",
                new String[]{String.valueOf(preguntaId)}, null, null, "orden ASC")) {
            while (c.moveToNext()) {
                Opcion o = new Opcion();
                o.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                o.setPreguntaId(preguntaId);
                o.setTexto(c.getString(c.getColumnIndexOrThrow("texto")));
                o.setCorrecta(c.getInt(c.getColumnIndexOrThrow("es_correcta")) == 1);
                o.setOrden(c.getInt(c.getColumnIndexOrThrow("orden")));
                lista.add(o);
            }
        }
        return lista;
    }

    // ==================================================================
    // PROGRESO DEL USUARIO
    // ==================================================================

    /** El camino de una categoría, con el estado del usuario y el bloqueo. */
    public List<ProgresoNivel> listarProgreso(long usuarioId, String categoria) {

        // Usamos writable porque, si faltan los síndromes,
        // los vamos a sembrar automáticamente.
        SQLiteDatabase db = helper.getWritableDatabase();


        // =========================================================
        // ASEGURAR BANCO DE SÍNDROMES
        // =========================================================

        if (Nivel.CAT_SINDROMES.equals(categoria)) {

            int cantidadSindromes =
                    contarFilasCategoria(
                            db,
                            Nivel.CAT_SINDROMES
                    );


            android.util.Log.d(
                    "NivelesDao",
                    "Síndromes antes de comprobar: " + cantidadSindromes
            );


            // Si la tabla no tiene niveles de síndromes,
            // intentar cargarlos directamente desde assets/sindromes.json.
            if (cantidadSindromes == 0) {

                android.util.Log.d(
                        "NivelesDao",
                        "No hay síndromes. Cargando assets/sindromes.json..."
                );


                sembrarSindromes(
                        helper.getAppContext(),
                        db
                );


                cantidadSindromes =
                        contarFilasCategoria(
                                db,
                                Nivel.CAT_SINDROMES
                        );


                android.util.Log.d(
                        "NivelesDao",
                        "Síndromes después de sembrar: " + cantidadSindromes
                );
            }
        }


        // =========================================================
        // CONSULTAR CAMINO
        // =========================================================

        String sql =
                "SELECT n.id AS nivel_id, " +
                        "n.numero, " +
                        "n.categoria, " +
                        "n.orden, " +
                        "n.nombre, " +
                        "n.emoji, " +
                        "n.tema, " +
                        "n.descripcion, " +
                        "n.total_preguntas, " +

                        "COALESCE(p.aprobado, 0) AS aprobado, " +
                        "COALESCE(p.mejor_porcentaje, 0) AS mejor_porcentaje, " +
                        "COALESCE(p.mejor_puntaje, 0) AS mejor_puntaje, " +
                        "COALESCE(p.intentos, 0) AS intentos, " +
                        "p.completado_en AS completado_en " +

                        "FROM " + DatabaseHelper.T_NIVELES + " n " +

                        "LEFT JOIN " +
                        DatabaseHelper.T_PROGRESO_NIVELES + " p " +

                        "ON p.nivel_id = n.id " +
                        "AND p.usuario_id = ? " +

                        "WHERE n.categoria = ? " +

                        "ORDER BY n.orden ASC";


        List<ProgresoNivel> lista =
                new ArrayList<>();


        try (
                Cursor c =
                        db.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(usuarioId),
                                        categoria
                                }
                        )
        ) {

            boolean anteriorAprobado = true;


            while (c.moveToNext()) {

                ProgresoNivel pn =
                        new ProgresoNivel();


                pn.setNivelId(
                        c.getLong(
                                c.getColumnIndexOrThrow(
                                        "nivel_id"
                                )
                        )
                );


                pn.setNumero(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "numero"
                                )
                        )
                );


                pn.setCategoria(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "categoria"
                                )
                        )
                );


                pn.setOrden(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "orden"
                                )
                        )
                );


                pn.setNombre(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "nombre"
                                )
                        )
                );


                pn.setEmoji(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "emoji"
                                )
                        )
                );


                pn.setTema(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "tema"
                                )
                        )
                );


                pn.setDescripcion(
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "descripcion"
                                )
                        )
                );


                pn.setTotalPreguntas(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "total_preguntas"
                                )
                        )
                );


                pn.setAprobado(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "aprobado"
                                )
                        ) == 1
                );


                pn.setMejorPorcentaje(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "mejor_porcentaje"
                                )
                        )
                );


                pn.setMejorPuntaje(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "mejor_puntaje"
                                )
                        )
                );


                pn.setIntentos(
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "intentos"
                                )
                        )
                );


                int completadoIndex =
                        c.getColumnIndexOrThrow(
                                "completado_en"
                        );


                if (!c.isNull(completadoIndex)) {

                    pn.setCompletadoEn(
                            c.getLong(
                                    completadoIndex
                            )
                    );
                }


                // Primer nivel siempre abierto.
                // Los siguientes dependen del nivel anterior.
                pn.setBloqueado(
                        !anteriorAprobado
                );


                anteriorAprobado =
                        pn.isAprobado();


                lista.add(
                        pn
                );
            }
        }


        android.util.Log.d(
                "NivelesDao",
                "listarProgreso(" + categoria + ") = "
                        + lista.size()
                        + " niveles"
        );


        return lista;
    }

    public ProgresoNivel obtenerProgreso(long usuarioId, long nivelId) {
        Nivel n = obtenerNivel(nivelId);
        if (n == null) return null;
        for (ProgresoNivel pn : listarProgreso(usuarioId, n.getCategoria())) {
            if (pn.getNivelId() == nivelId) return pn;
        }
        return null;
    }

    public boolean estaDesbloqueado(long usuarioId, long nivelId) {
        ProgresoNivel pn = obtenerProgreso(usuarioId, nivelId);
        return pn != null && !pn.isBloqueado();
    }

    // ==================================================================
    // INTENTOS Y RESPUESTAS
    // ==================================================================

    public long registrarIntento(Intento i) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("usuario_id", i.getUsuarioId());
        cv.put("nivel_id", i.getNivelId());
        cv.put("modo", i.getModo());
        cv.put("total", i.getTotal());
        cv.put("acertadas", i.getAciertos());
        cv.put("puntos", i.getPuntos());
        cv.put("aprobado", i.isAprobado() ? 1 : 0);
        cv.put("iniciado_en", i.getIniciadoEn());
        cv.put("finalizado_en", i.getFinalizadoEn());
        return db.insert(DatabaseHelper.T_INTENTOS, null, cv);
    }

    public long registrarRespuesta(long intentoId, long preguntaId, String textoIngresado,
                                   Long opcionId, boolean correcta, int puntos) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("intento_id", intentoId);
        cv.put("pregunta_id", preguntaId);
        cv.put("texto_ingresado", textoIngresado);
        if (opcionId == null) cv.putNull("opcion_id");
        else cv.put("opcion_id", opcionId);
        cv.put("correcta", correcta ? 1 : 0);
        cv.put("puntos", puntos);
        return db.insert(DatabaseHelper.T_RESPUESTAS_DADAS, null, cv);
    }

    /**
     * Actualiza el progreso del tramo tras un intento de tipo examen.
     * Sólo mejora el mejor porcentaje/puntaje, nunca lo empeora, e incrementa
     * el contador de intentos. Devuelve true si el tramo queda aprobado.
     */
    public boolean actualizarProgresoTrasExamen(long usuarioId, long nivelId,
                                                int porcentaje, int puntaje, boolean aprobado) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int intentosPrevios = 0;
            int mejorPorcentaje = 0;
            int mejorPuntaje = 0;
            Long completadoEn = null;

            try (Cursor c = db.query(DatabaseHelper.T_PROGRESO_NIVELES, null,
                    "usuario_id = ? AND nivel_id = ?",
                    new String[]{String.valueOf(usuarioId), String.valueOf(nivelId)},
                    null, null, null)) {
                if (c.moveToFirst()) {
                    intentosPrevios = c.getInt(c.getColumnIndexOrThrow("intentos"));
                    mejorPorcentaje = c.getInt(c.getColumnIndexOrThrow("mejor_porcentaje"));
                    mejorPuntaje = c.getInt(c.getColumnIndexOrThrow("mejor_puntaje"));
                    int ce = c.getColumnIndexOrThrow("completado_en");
                    completadoEn = c.isNull(ce) ? null : c.getLong(ce);
                }
            }

            boolean yaAprobado = completadoEn != null;
            int nuevoMejorPorcentaje = Math.max(mejorPorcentaje, porcentaje);
            int nuevoMejorPuntaje = Math.max(mejorPuntaje, puntaje);
            if (aprobado && completadoEn == null) completadoEn = System.currentTimeMillis();

            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("nivel_id", nivelId);
            cv.put("aprobado", (aprobado || yaAprobado) ? 1 : 0);
            cv.put("mejor_porcentaje", nuevoMejorPorcentaje);
            cv.put("mejor_puntaje", nuevoMejorPuntaje);
            cv.put("intentos", intentosPrevios + 1);
            if (completadoEn == null) cv.putNull("completado_en");
            else cv.put("completado_en", completadoEn);

            if (intentosPrevios == 0) {
                db.insert(DatabaseHelper.T_PROGRESO_NIVELES, null, cv);
            } else {
                db.update(DatabaseHelper.T_PROGRESO_NIVELES, cv,
                        "usuario_id = ? AND nivel_id = ?",
                        new String[]{String.valueOf(usuarioId), String.valueOf(nivelId)});
            }
            db.setTransactionSuccessful();
            FirebaseProgressSyncManager.programarSubida(helper.getAppContext(), usuarioId);
            return aprobado;
        } finally {
            db.endTransaction();
        }
    }

    // ==================================================================
    // UTILIDADES
    // ==================================================================

    public int totalNiveles() {
        return contarFilas(helper.getReadableDatabase(), DatabaseHelper.T_NIVELES);
    }

    public int totalPreguntas() {
        return contarFilas(helper.getReadableDatabase(), DatabaseHelper.T_PREGUNTAS);
    }

    private static int contarFilas(SQLiteDatabase db, String tabla) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + tabla, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static String leerAsset(Context ctx, String nombre) {
        try (InputStream in = ctx.getAssets().open(nombre);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            android.util.Log.e("NivelesDao", "No se pudo leer assets/" + nombre, e);
            return null;
        }
    }
}

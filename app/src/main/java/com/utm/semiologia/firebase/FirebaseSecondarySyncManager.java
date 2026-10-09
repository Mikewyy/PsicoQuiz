package com.utm.semiologia.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.SesionPomodoro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase 3: sincronizacion de datos secundarios del usuario.
 *
 * Rutas Firestore:
 * usuarios/{uid}/sync/secundario          -> marcador de esquema
 * usuarios/{uid}/notas/{creadaEn}        -> notas personales
 * usuarios/{uid}/actividad/{yyyy-MM-dd}   -> resultado del desafio diario
 * usuarios/{uid}/pomodoro/{iniciadoEn}    -> sesiones Pomodoro finalizadas
 *
 * Las colecciones son privadas por UID gracias a las reglas recursivas de usuarios.
 */
public class FirebaseSecondarySyncManager {

    private static final String TAG = "PsicoQuizSync";
    private static final int SCHEMA_VERSION = 4;
    private static volatile boolean restaurandoDesdeNube = false;

    private final Context appContext;
    private final DatabaseHelper helper;
    private final FirebaseFirestore firestore;

    public FirebaseSecondarySyncManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        helper = DatabaseHelper.get(appContext);
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Primera vez con Fase 3: migra SQLite -> Firestore.
     * Siguientes inicios: Firestore -> SQLite, igual que la Fase 2.
     */
    public void sincronizarAlEntrar(long usuarioId, @Nullable SyncCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid().trim().isEmpty()) {
            error(callback, new IllegalStateException("No hay una sesion Firebase valida."));
            return;
        }

        String uid = user.getUid();
        firestore.collection("usuarios").document(uid)
                .collection("sync").document("secundario")
                .get()
                .addOnSuccessListener(meta -> {
                    if (!meta.exists()) {
                        Log.d(TAG, "Fase 3: no habia datos secundarios cloud; se migraran desde SQLite. UID: " + uid);
                        subirTodoLocal(usuarioId, callback);
                    } else {
                        restaurarTodoDesdeNube(usuarioId, uid, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fase 3: no se pudo leer el marcador cloud. UID: " + uid, e);
                    error(callback, e);
                });
    }

    /** Una sola migracion inicial de notas, desafios y Pomodoro ya existentes. */
    public void subirTodoLocal(long usuarioId, @Nullable SyncCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid().trim().isEmpty()) {
            error(callback, new IllegalStateException("No hay una sesion Firebase valida."));
            return;
        }

        String uid = user.getUid();
        List<Task<?>> tareas = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        // Notas
        String sqlNotas = "SELECT n.id, n.seccion_id, n.texto_seleccionado, n.comentario, n.color, n.creada_en, " +
                "s.camino, s.orden FROM " + DatabaseHelper.T_NOTAS + " n LEFT JOIN " +
                DatabaseHelper.T_SECCIONES + " s ON s.id = n.seccion_id WHERE n.usuario_id = ?";
        try (Cursor c = db.rawQuery(sqlNotas, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                long creadaEn = c.getLong(5);
                Map<String, Object> m = new HashMap<>();
                m.put("textoSeleccionado", c.getString(2));
                m.put("comentario", c.isNull(3) ? null : c.getString(3));
                m.put("color", c.getString(4));
                m.put("creadaEn", creadaEn);
                m.put("seccionClave", c.isNull(6) ? null : c.getString(6) + ":" + c.getInt(7));
                m.put("actualizadoEn", FieldValue.serverTimestamp());
                tareas.add(firestore.collection("usuarios").document(uid)
                        .collection("notas").document(String.valueOf(creadaEn)).set(m));
            }
        }

        // Desafios diarios
        try (Cursor c = db.query(DatabaseHelper.T_ACTIVIDAD,
                new String[]{"fecha", DatabaseHelper.COL_DESAFIO},
                "usuario_id = ? AND " + DatabaseHelper.COL_DESAFIO + " IS NOT NULL",
                new String[]{String.valueOf(usuarioId)}, null, null, null)) {
            while (c.moveToNext()) {
                String fecha = c.getString(0);
                Map<String, Object> m = new HashMap<>();
                m.put("fecha", fecha);
                m.put("desafio", c.getString(1));
                m.put("actualizadoEn", FieldValue.serverTimestamp());
                tareas.add(firestore.collection("usuarios").document(uid)
                        .collection("actividad").document(fecha).set(m));
            }
        }

        // Pomodoro finalizado
        try (Cursor c = db.query(DatabaseHelper.T_POMODORO, null,
                "usuario_id = ? AND finalizado_en IS NOT NULL",
                new String[]{String.valueOf(usuarioId)}, null, null, null)) {
            while (c.moveToNext()) {
                long iniciadoEn = c.getLong(c.getColumnIndexOrThrow("iniciado_en"));
                tareas.add(firestore.collection("usuarios").document(uid)
                        .collection("pomodoro").document(String.valueOf(iniciadoEn))
                        .set(mapearPomodoroCursor(c)));
            }
        }

        Task<?> datos = tareas.isEmpty() ? Tasks.forResult(null) : Tasks.whenAll(tareas);
        datos.addOnSuccessListener(unused -> {
            Map<String, Object> meta = new HashMap<>();
            meta.put("schemaVersion", SCHEMA_VERSION);
            meta.put("creadoEn", FieldValue.serverTimestamp());
            firestore.collection("usuarios").document(uid)
                    .collection("sync").document("secundario")
                    .set(meta)
                    .addOnSuccessListener(v -> {
                        marcarNotasSincronizadas(usuarioId);
                        Log.d(TAG, "Fase 3: datos secundarios sincronizados en Firestore. UID: " + uid);
                        exito(callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Fase 3: fallo al crear el marcador cloud. UID: " + uid, e);
                        error(callback, e);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Fase 3: fallo la migracion inicial a Firestore. UID: " + uid, e);
            error(callback, e);
        });
    }

    private void restaurarTodoDesdeNube(long usuarioId, String uid, @Nullable SyncCallback callback) {
        Task<QuerySnapshot> notas = firestore.collection("usuarios").document(uid).collection("notas").get();
        Task<QuerySnapshot> actividad = firestore.collection("usuarios").document(uid).collection("actividad").get();
        Task<QuerySnapshot> pomodoro = firestore.collection("usuarios").document(uid).collection("pomodoro").get();

        Tasks.whenAllSuccess(notas, actividad, pomodoro)
                .addOnSuccessListener(resultados -> {
                    try {
                        aplicarCloud(usuarioId,
                                (QuerySnapshot) resultados.get(0),
                                (QuerySnapshot) resultados.get(1),
                                (QuerySnapshot) resultados.get(2));
                        subirTodoLocal(usuarioId, new SyncCallback() {
                            @Override public void onSuccess() {
                                Log.d(TAG, "Fase 4: notas, desafio y Pomodoro fusionados local/cloud. UID: " + uid);
                                exito(callback);
                            }
                            @Override public void onError(@NonNull Exception error) {
                                error(callback, error);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Fase 3: no se pudieron aplicar los datos cloud. UID: " + uid, e);
                        error(callback, e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fase 3: no se pudieron descargar los datos secundarios. UID: " + uid, e);
                    error(callback, e);
                });
    }

    private void aplicarCloud(long usuarioId, QuerySnapshot notas, QuerySnapshot actividad, QuerySnapshot pomodoro) {
        SQLiteDatabase db = helper.getWritableDatabase();
        restaurandoDesdeNube = true;
        db.beginTransaction();
        try {
            // Fase 4: union no destructiva. Los documentos cloud se incorporan
            // sin borrar datos locales creados por otro dispositivo/offline.

            for (DocumentSnapshot d : notas.getDocuments()) {
                ContentValues cv = new ContentValues();
                cv.put("usuario_id", usuarioId);
                Long seccionId = buscarSeccionPorClave(db, d.getString("seccionClave"));
                if (seccionId == null) cv.putNull("seccion_id"); else cv.put("seccion_id", seccionId);
                cv.put("texto_seleccionado", valorTexto(d.get("textoSeleccionado"), ""));
                Object comentario = d.get("comentario");
                if (comentario == null) cv.putNull("comentario"); else cv.put("comentario", String.valueOf(comentario));
                cv.put("color", valorTexto(d.get("color"), "#FFEB3B"));
                cv.put("creada_en", numeroLong(d.get("creadaEn"), parseLong(d.getId(), System.currentTimeMillis())));
                cv.put("sincronizada", 1);
                long creadaEn = cv.getAsLong("creada_en");
                try (Cursor existe = db.query(DatabaseHelper.T_NOTAS, new String[]{"id"},
                        "usuario_id = ? AND creada_en = ?",
                        new String[]{String.valueOf(usuarioId), String.valueOf(creadaEn)},
                        null, null, null, "1")) {
                    if (!existe.moveToFirst()) db.insert(DatabaseHelper.T_NOTAS, null, cv);
                }
            }

            for (DocumentSnapshot d : actividad.getDocuments()) {
                String fecha = valorTexto(d.get("fecha"), d.getId());
                String resultado = d.getString("desafio");
                if (resultado == null) continue;
                ContentValues cv = new ContentValues();
                cv.put("usuario_id", usuarioId);
                cv.put("fecha", fecha);
                cv.put(DatabaseHelper.COL_DESAFIO, resultado);
                int n = db.update(DatabaseHelper.T_ACTIVIDAD, cv,
                        "usuario_id = ? AND fecha = ?",
                        new String[]{String.valueOf(usuarioId), fecha});
                if (n == 0) db.insert(DatabaseHelper.T_ACTIVIDAD, null, cv);
            }

            for (DocumentSnapshot d : pomodoro.getDocuments()) {
                ContentValues cv = new ContentValues();
                cv.put("usuario_id", usuarioId);
                cv.put("modo", valorTexto(d.get("modo"), SesionPomodoro.MODO_INDIVIDUAL));
                cv.putNull("grupo_id"); // los grupos son locales/multijugador y se sincronizan aparte
                cv.put("duracion_foco_min", numeroInt(d.get("duracionFocoMin"), 20));
                cv.put("duracion_descanso_min", numeroInt(d.get("duracionDescansoMin"), 5));
                cv.put("minutos_estudiados", Math.max(0, numeroInt(d.get("minutosEstudiados"), 0)));
                cv.put("ciclos_completados", Math.max(0, numeroInt(d.get("ciclosCompletados"), 0)));
                cv.put("puntos_ganados", Math.max(0, numeroInt(d.get("puntosGanados"), 0)));
                cv.put("completado", booleano(d.get("completado")) ? 1 : 0);
                cv.put("iniciado_en", numeroLong(d.get("iniciadoEn"), parseLong(d.getId(), System.currentTimeMillis())));
                Object fin = d.get("finalizadoEn");
                if (fin instanceof Number) cv.put("finalizado_en", ((Number) fin).longValue()); else cv.putNull("finalizado_en");
                long iniciadoEn = cv.getAsLong("iniciado_en");
                try (Cursor existe = db.query(DatabaseHelper.T_POMODORO, new String[]{"id"},
                        "usuario_id = ? AND iniciado_en = ?",
                        new String[]{String.valueOf(usuarioId), String.valueOf(iniciadoEn)},
                        null, null, null, "1")) {
                    if (!existe.moveToFirst()) db.insert(DatabaseHelper.T_POMODORO, null, cv);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            restaurandoDesdeNube = false;
        }
    }

    // -----------------------------------------------------------------
    // Sincronizacion incremental desde los DAO
    // -----------------------------------------------------------------

    public static void sincronizarNota(@NonNull Context context, long notaId) {
        if (restaurandoDesdeNube) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseHelper helper = DatabaseHelper.get(context.getApplicationContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT n.usuario_id, n.texto_seleccionado, n.comentario, n.color, n.creada_en, " +
                "s.camino, s.orden FROM " + DatabaseHelper.T_NOTAS + " n LEFT JOIN " +
                DatabaseHelper.T_SECCIONES + " s ON s.id = n.seccion_id WHERE n.id = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(notaId)})) {
            if (!c.moveToFirst()) return;
            long creadaEn = c.getLong(4);
            Map<String, Object> m = new HashMap<>();
            m.put("textoSeleccionado", c.getString(1));
            m.put("comentario", c.isNull(2) ? null : c.getString(2));
            m.put("color", c.getString(3));
            m.put("creadaEn", creadaEn);
            m.put("seccionClave", c.isNull(5) ? null : c.getString(5) + ":" + c.getInt(6));
            m.put("actualizadoEn", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                    .collection("notas").document(String.valueOf(creadaEn)).set(m)
                    .addOnSuccessListener(v -> marcarNotaSincronizada(helper, notaId))
                    .addOnFailureListener(e -> Log.e(TAG, "Fase 3: no se pudo sincronizar la nota " + notaId, e));
        }
    }

    public static void eliminarNotaCloud(@NonNull Context context, long creadaEn) {
        if (restaurandoDesdeNube) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || creadaEn <= 0) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("notas").document(String.valueOf(creadaEn)).delete()
                .addOnFailureListener(e -> Log.e(TAG, "Fase 3: no se pudo eliminar la nota cloud " + creadaEn, e));
    }

    public static void sincronizarDesafio(@NonNull Context context, long usuarioId,
                                           @NonNull String fecha, @NonNull String resultado) {
        if (restaurandoDesdeNube) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("fecha", fecha);
        m.put("desafio", resultado);
        m.put("actualizadoEn", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("actividad").document(fecha).set(m)
                .addOnFailureListener(e -> Log.e(TAG, "Fase 3: no se pudo sincronizar el desafio " + fecha, e));
    }

    public static void sincronizarPomodoro(@NonNull Context context, @NonNull SesionPomodoro s) {
        if (restaurandoDesdeNube || s.getFinalizadoEn() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("pomodoro").document(String.valueOf(s.getIniciadoEn()))
                .set(mapearPomodoro(s))
                .addOnFailureListener(e -> Log.e(TAG, "Fase 3: no se pudo sincronizar Pomodoro " + s.getIniciadoEn(), e));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static Map<String, Object> mapearPomodoro(@NonNull SesionPomodoro s) {
        Map<String, Object> m = new HashMap<>();
        m.put("modo", s.getModo());
        m.put("duracionFocoMin", s.getDuracionFocoMin());
        m.put("duracionDescansoMin", s.getDuracionDescansoMin());
        m.put("minutosEstudiados", s.getMinutosEstudiados());
        m.put("ciclosCompletados", s.getCiclosCompletados());
        m.put("puntosGanados", s.getPuntosGanados());
        m.put("completado", s.isCompletado());
        m.put("iniciadoEn", s.getIniciadoEn());
        m.put("finalizadoEn", s.getFinalizadoEn());
        m.put("actualizadoEn", FieldValue.serverTimestamp());
        return m;
    }

    private Map<String, Object> mapearPomodoroCursor(Cursor c) {
        Map<String, Object> m = new HashMap<>();
        m.put("modo", c.getString(c.getColumnIndexOrThrow("modo")));
        m.put("duracionFocoMin", c.getInt(c.getColumnIndexOrThrow("duracion_foco_min")));
        m.put("duracionDescansoMin", c.getInt(c.getColumnIndexOrThrow("duracion_descanso_min")));
        m.put("minutosEstudiados", c.getInt(c.getColumnIndexOrThrow("minutos_estudiados")));
        m.put("ciclosCompletados", c.getInt(c.getColumnIndexOrThrow("ciclos_completados")));
        m.put("puntosGanados", c.getInt(c.getColumnIndexOrThrow("puntos_ganados")));
        m.put("completado", c.getInt(c.getColumnIndexOrThrow("completado")) == 1);
        m.put("iniciadoEn", c.getLong(c.getColumnIndexOrThrow("iniciado_en")));
        int fin = c.getColumnIndexOrThrow("finalizado_en");
        m.put("finalizadoEn", c.isNull(fin) ? null : c.getLong(fin));
        m.put("actualizadoEn", FieldValue.serverTimestamp());
        return m;
    }

    @Nullable
    private Long buscarSeccionPorClave(SQLiteDatabase db, @Nullable String clave) {
        if (clave == null) return null;
        String[] p = clave.split(":", 2);
        if (p.length != 2) return null;
        try (Cursor c = db.query(DatabaseHelper.T_SECCIONES, new String[]{"id"},
                "camino = ? AND orden = ?", new String[]{p[0], p[1]}, null, null, null, "1")) {
            return c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    private void marcarNotasSincronizadas(long usuarioId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sincronizada", 1);
        db.update(DatabaseHelper.T_NOTAS, cv, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
    }

    private static void marcarNotaSincronizada(DatabaseHelper helper, long notaId) {
        ContentValues cv = new ContentValues();
        cv.put("sincronizada", 1);
        helper.getWritableDatabase().update(DatabaseHelper.T_NOTAS, cv, "id = ?", new String[]{String.valueOf(notaId)});
    }

    private static String valorTexto(@Nullable Object o, String defecto) {
        return o == null ? defecto : String.valueOf(o);
    }

    private static int numeroInt(@Nullable Object o, int defecto) {
        return o instanceof Number ? ((Number) o).intValue() : defecto;
    }

    private static long numeroLong(@Nullable Object o, long defecto) {
        return o instanceof Number ? ((Number) o).longValue() : defecto;
    }

    private static boolean booleano(@Nullable Object o) {
        return o instanceof Boolean ? (Boolean) o : (o instanceof Number && ((Number) o).intValue() != 0);
    }

    private static long parseLong(String s, long defecto) {
        try { return Long.parseLong(s); } catch (Exception ignored) { return defecto; }
    }

    private void exito(@Nullable SyncCallback callback) {
        if (callback != null) callback.onSuccess();
    }

    private void error(@Nullable SyncCallback callback, @NonNull Exception e) {
        if (callback != null) callback.onError(e);
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(@NonNull Exception error);
    }
}

package com.utm.semiologia.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utm.semiologia.data.db.DatabaseHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fase 2 completa: sincroniza en un solo documento Firestore el progreso
 * académico y la economía del usuario.
 *
 * Ruta: usuarios/{uid}/sync/progreso
 *
 * Incluye:
 * - puntos, nivel, experiencia y racha
 * - progreso de secciones (clave estable camino:orden)
 * - progreso de niveles/quiz (clave estable numero)
 * - inventario de alimentos (clave estable nombre)
 * - accesorios (clave estable nombre)
 * - objetos/poderes (clave estable tipo)
 *
 * No sube preguntas, contenido, contraseñas ni IDs SQLite locales.
 */
public class FirebaseProgressSyncManager {

    private static final String TAG = "PsicoQuizSync";
    private static final int SCHEMA_VERSION = 4;
    private static final long DEBOUNCE_MS = 1800L;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<Long, Runnable> PENDIENTES = new ConcurrentHashMap<>();
    private static volatile boolean restaurandoDesdeNube = false;

    private final Context appContext;
    private final DatabaseHelper helper;
    private final FirebaseFirestore firestore;

    public FirebaseProgressSyncManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        helper = DatabaseHelper.get(appContext);
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Fase 4: al entrar ya no hacemos un reemplazo ciego. Se fusiona el
     * progreso local con el cloud y se vuelve a guardar el resultado.
     *
     * Reglas conservadoras:
     * - lectura/mejores resultados nunca retroceden;
     * - completado/aprobado se conserva si existe en cualquiera;
     * - puntos/experiencia/nivel usan el mayor valor para evitar rollback o
     *   duplicar recompensas al reabrir el mismo progreso en dos equipos;
     * - inventarios usan la versión con marca obtenidoEn más reciente.
     */
    public void sincronizarAlEntrar(long usuarioId, @Nullable SyncCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getUid().trim().isEmpty()) {
            error(callback, new IllegalStateException("No hay una sesión Firebase válida."));
            return;
        }

        String uid = firebaseUser.getUid();
        Map<String, Object> local;
        try {
            local = construirSnapshot(usuarioId);
        } catch (Exception e) {
            error(callback, e);
            return;
        }

        firestore.collection("usuarios")
                .document(uid)
                .collection("sync")
                .document("progreso")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.d(TAG, "Fase 4: no habia progreso cloud; se crea desde SQLite. UID: " + uid);
                        subirAhora(usuarioId, callback);
                        return;
                    }

                    try {
                        Map<String, Object> fusionado = fusionarSnapshots(local, doc.getData());
                        aplicarMapaCloud(usuarioId, fusionado);
                        firestore.collection("usuarios").document(uid)
                                .collection("sync").document("progreso")
                                .set(fusionado)
                                .addOnSuccessListener(v -> {
                                    FirebaseProfileSyncManager.programarSubidaDesdeSQLite(appContext, usuarioId);
                                    Log.d(TAG, "Fase 4: progreso local/cloud fusionado sin retrocesos. UID: " + uid);
                                    exito(callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Fase 4: no se pudo guardar la fusion de progreso. UID: " + uid, e);
                                    error(callback, e);
                                });
                    } catch (Exception e) {
                        Log.e(TAG, "Fase 4: no se pudo fusionar el progreso. UID: " + uid, e);
                        error(callback, e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fase 4: no se pudo leer el progreso cloud. UID: " + uid, e);
                    error(callback, e);
                });
    }

    /** Sube inmediatamente una fotografía completa de la Fase 2. Una escritura. */
    public void subirAhora(long usuarioId, @Nullable SyncCallback callback) {
        if (restaurandoDesdeNube) {
            exito(callback);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getUid().trim().isEmpty()) {
            error(callback, new IllegalStateException("No hay una sesión Firebase válida."));
            return;
        }

        String uid = firebaseUser.getUid();
        Map<String, Object> snapshot;
        try {
            snapshot = construirSnapshot(usuarioId);
        } catch (Exception e) {
            Log.e(TAG, "Fase 2: no se pudo construir el snapshot SQLite.", e);
            error(callback, e);
            return;
        }

        firestore.collection("usuarios")
                .document(uid)
                .collection("sync")
                .document("progreso")
                .set(snapshot)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Fase 2: progreso sincronizado en Firestore. UID: " + uid);
                    exito(callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fase 2: no se pudo subir el progreso. UID: " + uid, e);
                    error(callback, e);
                });
    }

    /**
     * Agrupa muchas mutaciones cercanas en una sola escritura Firestore.
     * Se usa desde los DAO para no escribir por cada movimiento del usuario.
     */
    public static void programarSubida(@NonNull Context context, long usuarioId) {
        if (usuarioId <= 0 || restaurandoDesdeNube) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        Runnable anterior = PENDIENTES.remove(usuarioId);
        if (anterior != null) HANDLER.removeCallbacks(anterior);

        Context app = context.getApplicationContext();
        Runnable nuevo = () -> {
            PENDIENTES.remove(usuarioId);
            new FirebaseProgressSyncManager(app).subirAhora(usuarioId, null);
        };
        PENDIENTES.put(usuarioId, nuevo);
        HANDLER.postDelayed(nuevo, DEBOUNCE_MS);
    }

    /** Encola inmediatamente el ultimo estado antes de cerrar sesion. */
    public static void forzarSubida(@NonNull Context context, long usuarioId) {
        Runnable pendiente = PENDIENTES.remove(usuarioId);
        if (pendiente != null) HANDLER.removeCallbacks(pendiente);
        if (usuarioId > 0 && FirebaseAuth.getInstance().getCurrentUser() != null) {
            new FirebaseProgressSyncManager(context.getApplicationContext()).subirAhora(usuarioId, null);
        }
    }

    private Map<String, Object> construirSnapshot(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("schemaVersion", SCHEMA_VERSION);
        raiz.put("actualizadoEn", FieldValue.serverTimestamp());

        raiz.put("gamificacion", leerGamificacion(db, usuarioId));
        raiz.put("secciones", leerProgresoSecciones(db, usuarioId));
        raiz.put("niveles", leerProgresoNiveles(db, usuarioId));
        raiz.put("alimentos", leerAlimentos(db, usuarioId));
        raiz.put("accesorios", leerAccesorios(db, usuarioId));
        raiz.put("objetos", leerObjetos(db, usuarioId));
        return raiz;
    }

    private Map<String, Object> leerGamificacion(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Cursor c = db.query(DatabaseHelper.T_USUARIOS,
                new String[]{"puntos", "nivel", "experiencia", "racha_actual", "racha_maxima", "ultima_actividad_fecha"},
                "id = ?", new String[]{String.valueOf(usuarioId)}, null, null, null, "1")) {
            if (c.moveToFirst()) {
                out.put("puntos", c.getInt(0));
                out.put("nivel", c.getInt(1));
                out.put("experiencia", c.getInt(2));
                out.put("rachaActual", c.getInt(3));
                out.put("rachaMaxima", c.getInt(4));
                out.put("ultimaActividadFecha", c.isNull(5) ? null : c.getString(5));
            }
        }
        return out;
    }

    private Map<String, Object> leerProgresoSecciones(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        String sql = "SELECT s.camino, s.orden, p.lectura, p.completada, p.puntos_otorgados, " +
                "p.comida_otorgada, p.iniciada_en, p.completada_en " +
                "FROM " + DatabaseHelper.T_PROGRESO + " p JOIN " + DatabaseHelper.T_SECCIONES +
                " s ON s.id = p.seccion_id WHERE p.usuario_id = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                String clave = c.getString(0) + ":" + c.getInt(1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("lectura", c.getInt(2));
                m.put("completada", c.getInt(3) == 1);
                m.put("puntosOtorgados", c.getInt(4));
                m.put("comidaOtorgada", c.getInt(5));
                m.put("iniciadaEn", c.getLong(6));
                m.put("completadaEn", c.isNull(7) ? null : c.getLong(7));
                out.put(clave, m);
            }
        }
        return out;
    }

    private Map<String, Object> leerProgresoNiveles(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        String sql = "SELECT n.numero, p.aprobado, p.mejor_porcentaje, p.mejor_puntaje, p.intentos, p.completado_en " +
                "FROM " + DatabaseHelper.T_PROGRESO_NIVELES + " p JOIN " + DatabaseHelper.T_NIVELES +
                " n ON n.id = p.nivel_id WHERE p.usuario_id = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("aprobado", c.getInt(1) == 1);
                m.put("mejorPorcentaje", c.getInt(2));
                m.put("mejorPuntaje", c.getInt(3));
                m.put("intentos", c.getInt(4));
                m.put("completadoEn", c.isNull(5) ? null : c.getLong(5));
                out.put(String.valueOf(c.getInt(0)), m);
            }
        }
        return out;
    }

    private Map<String, Object> leerAlimentos(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        String sql = "SELECT a.nombre, i.cantidad, i.obtenido_en FROM " + DatabaseHelper.T_INVENTARIO +
                " i JOIN " + DatabaseHelper.T_ALIMENTOS + " a ON a.id = i.alimento_id WHERE i.usuario_id = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("cantidad", c.getInt(1));
                m.put("obtenidoEn", c.getLong(2));
                out.put(c.getString(0), m);
            }
        }
        return out;
    }

    private Map<String, Object> leerAccesorios(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        String sql = "SELECT a.nombre, i.equipado, i.obtenido_en FROM " + DatabaseHelper.T_INV_ACCESORIOS +
                " i JOIN " + DatabaseHelper.T_ACCESORIOS + " a ON a.id = i.accesorio_id WHERE i.usuario_id = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("equipado", c.getInt(1) == 1);
                m.put("obtenidoEn", c.getLong(2));
                out.put(c.getString(0), m);
            }
        }
        return out;
    }

    private Map<String, Object> leerObjetos(SQLiteDatabase db, long usuarioId) {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Cursor c = db.query(DatabaseHelper.T_INV_OBJETOS,
                new String[]{"tipo", "cantidad", "obtenido_en"}, "usuario_id = ?",
                new String[]{String.valueOf(usuarioId)}, null, null, null)) {
            while (c.moveToNext()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("cantidad", c.getInt(1));
                m.put("obtenidoEn", c.getLong(2));
                out.put(c.getString(0), m);
            }
        }
        return out;
    }

    private Map<String, Object> fusionarSnapshots(@NonNull Map<String, Object> local,
                                                       @Nullable Map<String, Object> cloud) {
        if (cloud == null || cloud.isEmpty()) return local;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("schemaVersion", 4);
        out.put("actualizadoEn", FieldValue.serverTimestamp());
        out.put("gamificacion", fusionarGamificacion(mapa(local.get("gamificacion")), mapa(cloud.get("gamificacion"))));
        out.put("secciones", fusionarSecciones(mapa(local.get("secciones")), mapa(cloud.get("secciones"))));
        out.put("niveles", fusionarNiveles(mapa(local.get("niveles")), mapa(cloud.get("niveles"))));
        out.put("alimentos", fusionarInventario(mapa(local.get("alimentos")), mapa(cloud.get("alimentos"))));
        out.put("accesorios", fusionarAccesorios(mapa(local.get("accesorios")), mapa(cloud.get("accesorios"))));
        out.put("objetos", fusionarInventario(mapa(local.get("objetos")), mapa(cloud.get("objetos"))));
        return out;
    }

    private Map<String, Object> fusionarGamificacion(@Nullable Map<String, Object> a, @Nullable Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("puntos", Math.max(entero(valor(a,"puntos"),0), entero(valor(b,"puntos"),0)));
        out.put("nivel", Math.max(1, Math.max(entero(valor(a,"nivel"),1), entero(valor(b,"nivel"),1))));
        out.put("experiencia", Math.max(entero(valor(a,"experiencia"),0), entero(valor(b,"experiencia"),0)));
        out.put("rachaMaxima", Math.max(entero(valor(a,"rachaMaxima"),0), entero(valor(b,"rachaMaxima"),0)));
        String fa = textoNullable(valor(a,"ultimaActividadFecha"));
        String fb = textoNullable(valor(b,"ultimaActividadFecha"));
        String fecha = maxFecha(fa, fb);
        int racha;
        if (fecha != null && fecha.equals(fa) && !fecha.equals(fb)) racha = entero(valor(a,"rachaActual"),0);
        else if (fecha != null && fecha.equals(fb) && !fecha.equals(fa)) racha = entero(valor(b,"rachaActual"),0);
        else racha = Math.max(entero(valor(a,"rachaActual"),0), entero(valor(b,"rachaActual"),0));
        out.put("rachaActual", Math.max(0, racha));
        out.put("ultimaActividadFecha", fecha);
        return out;
    }

    private Map<String, Object> fusionarSecciones(@Nullable Map<String, Object> a, @Nullable Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b == null) return out;
        for (Map.Entry<String,Object> e : b.entrySet()) {
            Map<String,Object> mb = mapa(e.getValue());
            Map<String,Object> ma = mapa(out.get(e.getKey()));
            if (ma == null) { out.put(e.getKey(), e.getValue()); continue; }
            if (mb == null) continue;
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("lectura", Math.max(entero(ma.get("lectura"),0), entero(mb.get("lectura"),0)));
            m.put("completada", booleano(ma.get("completada")) || booleano(mb.get("completada")));
            m.put("puntosOtorgados", Math.max(entero(ma.get("puntosOtorgados"),0), entero(mb.get("puntosOtorgados"),0)));
            m.put("comidaOtorgada", Math.max(entero(ma.get("comidaOtorgada"),0), entero(mb.get("comidaOtorgada"),0)));
            m.put("iniciadaEn", minPositivo(largo(ma.get("iniciadaEn"),0), largo(mb.get("iniciadaEn"),0)));
            m.put("completadaEn", minNullable(ma.get("completadaEn"), mb.get("completadaEn")));
            out.put(e.getKey(), m);
        }
        return out;
    }

    private Map<String, Object> fusionarNiveles(@Nullable Map<String, Object> a, @Nullable Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b == null) return out;
        for (Map.Entry<String,Object> e : b.entrySet()) {
            Map<String,Object> mb = mapa(e.getValue());
            Map<String,Object> ma = mapa(out.get(e.getKey()));
            if (ma == null) { out.put(e.getKey(), e.getValue()); continue; }
            if (mb == null) continue;
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("aprobado", booleano(ma.get("aprobado")) || booleano(mb.get("aprobado")));
            m.put("mejorPorcentaje", Math.max(entero(ma.get("mejorPorcentaje"),0), entero(mb.get("mejorPorcentaje"),0)));
            m.put("mejorPuntaje", Math.max(entero(ma.get("mejorPuntaje"),0), entero(mb.get("mejorPuntaje"),0)));
            m.put("intentos", Math.max(entero(ma.get("intentos"),0), entero(mb.get("intentos"),0)));
            m.put("completadoEn", minNullable(ma.get("completadoEn"), mb.get("completadoEn")));
            out.put(e.getKey(), m);
        }
        return out;
    }

    private Map<String, Object> fusionarAccesorios(@Nullable Map<String, Object> local, @Nullable Map<String, Object> cloud) {
        Map<String,Object> out = new LinkedHashMap<>();
        if (cloud != null) out.putAll(cloud);
        if (local == null) return out;
        for (Map.Entry<String,Object> e : local.entrySet()) {
            Map<String,Object> ml = mapa(e.getValue());
            Map<String,Object> mc = mapa(out.get(e.getKey()));
            if (mc == null) { out.put(e.getKey(), e.getValue()); continue; }
            if (ml == null) continue;
            long tl = largo(ml.get("obtenidoEn"),0);
            long tc = largo(mc.get("obtenidoEn"),0);
            // En empate conservamos cloud para no mezclar dos equipamientos antiguos.
            if (tl > tc) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /** Para cantidades/equipamiento gana la fila modificada mas recientemente. */
    private Map<String, Object> fusionarInventario(@Nullable Map<String, Object> local, @Nullable Map<String, Object> cloud) {
        Map<String,Object> out = new LinkedHashMap<>();
        if (cloud != null) out.putAll(cloud);
        if (local == null) return out;
        for (Map.Entry<String,Object> e : local.entrySet()) {
            Map<String,Object> ml = mapa(e.getValue());
            Map<String,Object> mc = mapa(out.get(e.getKey()));
            if (mc == null) { out.put(e.getKey(), e.getValue()); continue; }
            if (ml == null) continue;
            long tl = largo(ml.get("obtenidoEn"),0);
            long tc = largo(mc.get("obtenidoEn"),0);
            if (tl > tc) out.put(e.getKey(), e.getValue());
            else if (tl == tc) {
                // Compatibilidad con datos de Fase 2/3, donde obtenidoEn aun no
                // se actualizaba en cada cambio. Preferimos no incrementar por suma.
                Map<String,Object> m = new LinkedHashMap<>(mc);
                if (ml.containsKey("cantidad")) m.put("cantidad", Math.max(entero(ml.get("cantidad"),0), entero(mc.get("cantidad"),0)));
                if (ml.containsKey("equipado")) m.put("equipado", booleano(ml.get("equipado")) || booleano(mc.get("equipado")));
                m.put("obtenidoEn", Math.max(tl, tc));
                out.put(e.getKey(), m);
            }
        }
        return out;
    }

    @Nullable private Object valor(@Nullable Map<String,Object> m, String k) { return m == null ? null : m.get(k); }
    @Nullable private String textoNullable(@Nullable Object o) { if (o == null) return null; String s=String.valueOf(o).trim(); return s.isEmpty()?null:s; }
    @Nullable private String maxFecha(@Nullable String a, @Nullable String b) { if (a==null) return b; if (b==null) return a; return a.compareTo(b)>=0?a:b; }
    private long minPositivo(long a,long b) { if(a<=0)return b; if(b<=0)return a; return Math.min(a,b); }
    @Nullable private Object minNullable(@Nullable Object a,@Nullable Object b) { long x=largo(a,0), y=largo(b,0); long m=minPositivo(x,y); return m<=0?null:m; }

    @SuppressWarnings("unchecked")
    private void aplicarDocumentoCloud(long usuarioId, @NonNull DocumentSnapshot doc) {
        aplicarMapaCloud(usuarioId, doc.getData());
    }

    private void aplicarMapaCloud(long usuarioId, @Nullable Map<String, Object> raiz) {
        if (raiz == null) return;
        SQLiteDatabase db = helper.getWritableDatabase();
        restaurandoDesdeNube = true;
        db.beginTransaction();
        try {
            Map<String, Object> gam = mapa(raiz.get("gamificacion"));
            if (gam != null) aplicarGamificacion(db, usuarioId, gam);

            db.delete(DatabaseHelper.T_PROGRESO, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
            db.delete(DatabaseHelper.T_PROGRESO_NIVELES, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
            db.delete(DatabaseHelper.T_INVENTARIO, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
            db.delete(DatabaseHelper.T_INV_ACCESORIOS, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
            db.delete(DatabaseHelper.T_INV_OBJETOS, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});

            aplicarSecciones(db, usuarioId, mapa(raiz.get("secciones")));
            aplicarNiveles(db, usuarioId, mapa(raiz.get("niveles")));
            aplicarAlimentos(db, usuarioId, mapa(raiz.get("alimentos")));
            aplicarAccesorios(db, usuarioId, mapa(raiz.get("accesorios")));
            aplicarObjetos(db, usuarioId, mapa(raiz.get("objetos")));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            restaurandoDesdeNube = false;
        }
    }

    private void aplicarGamificacion(SQLiteDatabase db, long usuarioId, Map<String, Object> m) {
        ContentValues cv = new ContentValues();
        cv.put("puntos", entero(m.get("puntos"), 0));
        cv.put("nivel", Math.max(1, entero(m.get("nivel"), 1)));
        cv.put("experiencia", Math.max(0, entero(m.get("experiencia"), 0)));
        cv.put("racha_actual", Math.max(0, entero(m.get("rachaActual"), 0)));
        cv.put("racha_maxima", Math.max(0, entero(m.get("rachaMaxima"), 0)));
        Object fecha = m.get("ultimaActividadFecha");
        if (fecha == null) cv.putNull("ultima_actividad_fecha"); else cv.put("ultima_actividad_fecha", String.valueOf(fecha));
        db.update(DatabaseHelper.T_USUARIOS, cv, "id = ?", new String[]{String.valueOf(usuarioId)});
    }

    private void aplicarSecciones(SQLiteDatabase db, long usuarioId, @Nullable Map<String, Object> datos) {
        if (datos == null) return;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            Map<String, Object> m = mapa(e.getValue());
            if (m == null) continue;
            String[] partes = e.getKey().split(":", 2);
            if (partes.length != 2) continue;
            long seccionId = buscarId(db, DatabaseHelper.T_SECCIONES, "camino = ? AND orden = ?",
                    new String[]{partes[0], partes[1]});
            if (seccionId <= 0) continue;
            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("seccion_id", seccionId);
            cv.put("lectura", limitar(entero(m.get("lectura"), 0), 0, 100));
            cv.put("completada", booleano(m.get("completada")) ? 1 : 0);
            cv.put("puntos_otorgados", Math.max(0, entero(m.get("puntosOtorgados"), 0)));
            cv.put("comida_otorgada", Math.max(0, entero(m.get("comidaOtorgada"), 0)));
            cv.put("iniciada_en", largo(m.get("iniciadaEn"), System.currentTimeMillis()));
            ponerLongNullable(cv, "completada_en", m.get("completadaEn"));
            db.insertWithOnConflict(DatabaseHelper.T_PROGRESO, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void aplicarNiveles(SQLiteDatabase db, long usuarioId, @Nullable Map<String, Object> datos) {
        if (datos == null) return;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            Map<String, Object> m = mapa(e.getValue());
            if (m == null) continue;
            long nivelId = buscarId(db, DatabaseHelper.T_NIVELES, "numero = ?", new String[]{e.getKey()});
            if (nivelId <= 0) continue;
            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("nivel_id", nivelId);
            cv.put("aprobado", booleano(m.get("aprobado")) ? 1 : 0);
            cv.put("mejor_porcentaje", limitar(entero(m.get("mejorPorcentaje"), 0), 0, 100));
            cv.put("mejor_puntaje", Math.max(0, entero(m.get("mejorPuntaje"), 0)));
            cv.put("intentos", Math.max(0, entero(m.get("intentos"), 0)));
            ponerLongNullable(cv, "completado_en", m.get("completadoEn"));
            db.insertWithOnConflict(DatabaseHelper.T_PROGRESO_NIVELES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void aplicarAlimentos(SQLiteDatabase db, long usuarioId, @Nullable Map<String, Object> datos) {
        if (datos == null) return;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            Map<String, Object> m = mapa(e.getValue());
            if (m == null) continue;
            int cantidad = Math.max(0, entero(m.get("cantidad"), 0));
            if (cantidad == 0) continue;
            long alimentoId = buscarId(db, DatabaseHelper.T_ALIMENTOS, "nombre = ?", new String[]{e.getKey()});
            if (alimentoId <= 0) continue;
            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("alimento_id", alimentoId);
            cv.put("cantidad", cantidad);
            cv.put("obtenido_en", largo(m.get("obtenidoEn"), System.currentTimeMillis()));
            db.insertWithOnConflict(DatabaseHelper.T_INVENTARIO, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void aplicarAccesorios(SQLiteDatabase db, long usuarioId, @Nullable Map<String, Object> datos) {
        if (datos == null) return;
        Long equipadoId = null;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            Map<String, Object> m = mapa(e.getValue());
            if (m == null) continue;
            long accesorioId = buscarId(db, DatabaseHelper.T_ACCESORIOS, "nombre = ?", new String[]{e.getKey()});
            if (accesorioId <= 0) continue;
            boolean equipado = booleano(m.get("equipado"));
            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("accesorio_id", accesorioId);
            cv.put("equipado", equipado ? 1 : 0);
            cv.put("obtenido_en", largo(m.get("obtenidoEn"), System.currentTimeMillis()));
            db.insertWithOnConflict(DatabaseHelper.T_INV_ACCESORIOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            if (equipado) equipadoId = accesorioId;
        }
        ContentValues mascota = new ContentValues();
        if (equipadoId == null) mascota.putNull("accesorio_equipado_id"); else mascota.put("accesorio_equipado_id", equipadoId);
        db.update(DatabaseHelper.T_MASCOTA, mascota, "usuario_id = ?", new String[]{String.valueOf(usuarioId)});
    }

    private void aplicarObjetos(SQLiteDatabase db, long usuarioId, @Nullable Map<String, Object> datos) {
        if (datos == null) return;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            Map<String, Object> m = mapa(e.getValue());
            if (m == null) continue;
            int cantidad = Math.max(0, entero(m.get("cantidad"), 0));
            if (cantidad == 0) continue;
            ContentValues cv = new ContentValues();
            cv.put("usuario_id", usuarioId);
            cv.put("tipo", e.getKey());
            cv.put("cantidad", cantidad);
            cv.put("obtenido_en", largo(m.get("obtenidoEn"), System.currentTimeMillis()));
            db.insertWithOnConflict(DatabaseHelper.T_INV_OBJETOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private long buscarId(SQLiteDatabase db, String tabla, String where, String[] args) {
        try (Cursor c = db.query(tabla, new String[]{"id"}, where, args, null, null, null, "1")) {
            return c.moveToFirst() ? c.getLong(0) : -1L;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(@Nullable Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private boolean booleano(@Nullable Object o) {
        return o instanceof Boolean ? (Boolean) o : (o instanceof Number && ((Number) o).intValue() != 0);
    }

    private int entero(@Nullable Object o, int defecto) {
        return o instanceof Number ? ((Number) o).intValue() : defecto;
    }

    private long largo(@Nullable Object o, long defecto) {
        return o instanceof Number ? ((Number) o).longValue() : defecto;
    }

    private int limitar(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private void ponerLongNullable(ContentValues cv, String campo, @Nullable Object o) {
        if (o instanceof Number) cv.put(campo, ((Number) o).longValue()); else cv.putNull(campo);
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

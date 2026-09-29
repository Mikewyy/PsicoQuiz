package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Mascota;

/**
 * Acceso a la tabla mascota.
 *
 * Nota sobre el decaimiento: {@link #obtenerYActualizar(long)} es el único
 * punto donde se materializa el paso del tiempo. Se llama al abrir el
 * dashboard y al volver del segundo plano, y persiste el nuevo nivel de
 * hambre junto con el nuevo reloj, de modo que el cálculo nunca se duplica.
 */
public class MascotaDao {

    private final DatabaseHelper helper;

    public MascotaDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    @Nullable
    public Mascota obtenerYActualizar(long usuarioId) {
        Mascota m = obtener(usuarioId);
        if (m == null) return null;
        int perdidos = m.aplicarDecaimiento();
        // Persistimos siempre: aunque la pérdida sea 0, el reloj avanza
        // internamente y esto mantiene la operación atómica.
        actualizarEstado(m);
        return m;
    }

    @Nullable
    public Mascota obtener(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_MASCOTA, null,
                "usuario_id = ?", new String[]{String.valueOf(usuarioId)},
                null, null, null, "1")) {
            return c.moveToFirst() ? mapear(c) : null;
        }
    }

    public long insertar(Mascota m) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insertOrThrow(DatabaseHelper.T_MASCOTA, null, toValues(m));
    }

    public void actualizarEstado(Mascota m) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("hambre", m.getHambre());
        cv.put("felicidad", m.getFelicidad());
        cv.put("energia", m.getEnergia());
        cv.put("estado", m.getEstado());
        cv.put("hambre_actualizada_en", m.getHambreActualizadaEn());
        if (m.getAccesorioEquipadoId() != null) {
            cv.put("accesorio_equipado_id", m.getAccesorioEquipadoId());
        }
        db.update(DatabaseHelper.T_MASCOTA, cv, "usuario_id = ?",
                new String[]{String.valueOf(m.getUsuarioId())});
    }

    public void renombrar(long usuarioId, String nombre) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        db.update(DatabaseHelper.T_MASCOTA, cv, "usuario_id = ?",
                new String[]{String.valueOf(usuarioId)});
    }

    public void equiparAccesorio(long usuarioId, Long accesorioId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            if (accesorioId == null) cv.putNull("accesorio_equipado_id");
            else cv.put("accesorio_equipado_id", accesorioId);
            db.update(DatabaseHelper.T_MASCOTA, cv, "usuario_id = ?",
                    new String[]{String.valueOf(usuarioId)});

            // Sólo un accesorio equipado a la vez
            db.execSQL("UPDATE " + DatabaseHelper.T_INV_ACCESORIOS +
                            " SET equipado = CASE WHEN accesorio_id = ? THEN 1 ELSE 0 END " +
                            "WHERE usuario_id = ?",
                    new Object[]{accesorioId == null ? -1L : accesorioId, usuarioId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---- Inventario de comida ----

    /** Resta cantidad del inventario y alimenta a la mascota, todo atómico. */
    public boolean consumirAlimentoYAlimentar(long usuarioId, long alimentoId,
                                               int puntosHambre, int puntosFelicidad, int puntosEnergia) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int disponibles = cantidadAlimento(usuarioId, alimentoId);
            if (disponibles <= 0) return false;

            Mascota m = obtener(usuarioId);
            if (m == null) return false;

            m.alimentar(puntosHambre, puntosFelicidad, puntosEnergia);

            ContentValues cv = new ContentValues();
            cv.put("cantidad", disponibles - 1);
            db.update(DatabaseHelper.T_INVENTARIO, cv,
                    "usuario_id = ? AND alimento_id = ?",
                    new String[]{String.valueOf(usuarioId), String.valueOf(alimentoId)});

            ContentValues cvM = new ContentValues();
            cvM.put("hambre", m.getHambre());
            cvM.put("felicidad", m.getFelicidad());
            cvM.put("energia", m.getEnergia());
            cvM.put("estado", m.getEstado());
            cvM.put("hambre_actualizada_en", m.getHambreActualizadaEn());
            db.update(DatabaseHelper.T_MASCOTA, cvM, "usuario_id = ?",
                    new String[]{String.valueOf(usuarioId)});

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public int cantidadAlimento(long usuarioId, long alimentoId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_INVENTARIO, new String[]{"cantidad"},
                "usuario_id = ? AND alimento_id = ?",
                new String[]{String.valueOf(usuarioId), String.valueOf(alimentoId)},
                null, null, null, "1")) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Otorga comida al usuario (por completar secciones, logros, etc.). */
    public void otorgarAlimento(long usuarioId, long alimentoId, int cantidad) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO " + DatabaseHelper.T_INVENTARIO +
                        " (usuario_id, alimento_id, cantidad, obtenido_en) VALUES (?,?,?,?) " +
                        "ON CONFLICT(usuario_id, alimento_id) DO UPDATE SET " +
                        "cantidad = cantidad + excluded.cantidad",
                new Object[]{usuarioId, alimentoId, cantidad, System.currentTimeMillis()});
    }

    public Cursor listarInventario(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery("SELECT a.id, a.nombre, a.emoji, a.puntos_hambre, a.puntos_felicidad, " +
                        "COALESCE(i.cantidad, 0) AS cantidad " +
                        "FROM " + DatabaseHelper.T_ALIMENTOS + " a " +
                        "LEFT JOIN " + DatabaseHelper.T_INVENTARIO + " i " +
                        "  ON i.alimento_id = a.id AND i.usuario_id = ?",
                new String[]{String.valueOf(usuarioId)});
    }

    public Cursor listarAccesorios(long usuarioId, int nivelUsuario) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery("SELECT ac.id, ac.nombre, ac.emoji, ac.nivel_requerido, ac.descripcion, " +
                        "CASE WHEN ia.id IS NULL THEN 0 ELSE 1 END AS_poseido, " +
                        "COALESCE(ia.equipado, 0) AS equipado " +
                        "FROM " + DatabaseHelper.T_ACCESORIOS + " ac " +
                        "LEFT JOIN " + DatabaseHelper.T_INV_ACCESORIOS + " ia " +
                        "  ON ia.accesorio_id = ac.id AND ia.usuario_id = ? " +
                        "WHERE ac.nivel_requerido <= ? OR ia.id IS NOT NULL " +
                        "ORDER BY ac.nivel_requerido",
                new String[]{String.valueOf(usuarioId), String.valueOf(nivelUsuario + 1)});
    }

    public void otorgarAccesorio(long usuarioId, long accesorioId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("INSERT OR IGNORE INTO " + DatabaseHelper.T_INV_ACCESORIOS +
                        " (usuario_id, accesorio_id, equipado, obtenido_en) VALUES (?,?,0,?)",
                new Object[]{usuarioId, accesorioId, System.currentTimeMillis()});
    }

    // ---- Mapeo ----
    private Mascota mapear(Cursor c) {
        Mascota m = new Mascota();
        m.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        m.setUsuarioId(c.getLong(c.getColumnIndexOrThrow("usuario_id")));
        m.setNombre(c.getString(c.getColumnIndexOrThrow("nombre")));
        m.setEspecie(c.getString(c.getColumnIndexOrThrow("especie")));
        m.setHambre(c.getInt(c.getColumnIndexOrThrow("hambre")));
        m.setFelicidad(c.getInt(c.getColumnIndexOrThrow("felicidad")));
        m.setEnergia(c.getInt(c.getColumnIndexOrThrow("energia")));
        m.setEstado(c.getString(c.getColumnIndexOrThrow("estado")));
        int ai = c.getColumnIndexOrThrow("accesorio_equipado_id");
        m.setAccesorioEquipadoId(c.isNull(ai) ? null : c.getLong(ai));
        m.setHambreActualizadaEn(c.getLong(c.getColumnIndexOrThrow("hambre_actualizada_en")));
        m.setCreadoEn(c.getLong(c.getColumnIndexOrThrow("creado_en")));
        return m;
    }

    private ContentValues toValues(Mascota m) {
        ContentValues cv = new ContentValues();
        cv.put("usuario_id", m.getUsuarioId());
        cv.put("nombre", m.getNombre());
        cv.put("especie", m.getEspecie());
        cv.put("hambre", m.getHambre());
        cv.put("felicidad", m.getFelicidad());
        cv.put("energia", m.getEnergia());
        cv.put("estado", m.getEstado());
        if (m.getAccesorioEquipadoId() != null) {
            cv.put("accesorio_equipado_id", m.getAccesorioEquipadoId());
        }
        cv.put("hambre_actualizada_en", m.getHambreActualizadaEn());
        cv.put("creado_en", m.getCreadoEn());
        return cv;
    }
}

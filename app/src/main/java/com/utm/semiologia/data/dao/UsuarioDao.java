package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.util.FechaUtil;
import com.utm.semiologia.util.Gamificacion;

import java.util.ArrayList;
import java.util.List;

/** CRUD + lógica de rachas sobre la tabla usuarios. */
public class UsuarioDao {

    private final DatabaseHelper helper;

    public UsuarioDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    // ------------------------------------------------------------------
    // Lecturas
    // ------------------------------------------------------------------
    @Nullable
    public Usuario buscarPorId(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_USUARIOS, null,
                "id = ?", new String[]{String.valueOf(id)},
                null, null, null, "1")) {
            return c.moveToFirst() ? mapear(c) : null;
        }
    }

    @Nullable
    public Usuario buscarPorEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_USUARIOS, null,
                "email = ?", new String[]{email},
                null, null, null, "1")) {
            return c.moveToFirst() ? mapear(c) : null;
        }
    }

    public List<Usuario> rankingGlobal(int limite) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Usuario> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_USUARIOS, null,
                null, null, null, null, "puntos DESC", String.valueOf(limite))) {
            while (c.moveToNext()) lista.add(mapear(c));
        }
        return lista;
    }

    /** Top de un grupo por puntos semanales. */
    public List<Usuario> rankingGrupo(long grupoId, int limite) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.* FROM " + DatabaseHelper.T_USUARIOS + " u " +
                "JOIN " + DatabaseHelper.T_GRUPO_MIEMBROS + " m ON m.usuario_id = u.id " +
                "WHERE m.grupo_id = ? ORDER BY m.puntos_semanales DESC LIMIT ?";
        try (Cursor c = db.rawQuery(sql,
                new String[]{String.valueOf(grupoId), String.valueOf(limite)})) {
            while (c.moveToNext()) lista.add(mapear(c));
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Escrituras
    // ------------------------------------------------------------------

    /** @return id del nuevo usuario, o -1 si el email ya existía. */
    public long insertar(Usuario u) {
        if (buscarPorEmail(u.getEmail()) != null) return -1L;
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insertOrThrow(DatabaseHelper.T_USUARIOS, null, toValues(u));
    }

    public void actualizar(Usuario u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.update(DatabaseHelper.T_USUARIOS, toValues(u), "id = ?",
                new String[]{String.valueOf(u.getId())});
    }

    public void actualizarPuntosYRacha(long usuarioId, int puntos, int nivel, int racha) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("puntos", puntos);
        cv.put("nivel", nivel);
        cv.put("racha_actual", racha);
        db.update(DatabaseHelper.T_USUARIOS, cv, "id = ?", new String[]{String.valueOf(usuarioId)});
    }

    public void asignarGrupo(long usuarioId, long grupoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("grupo_id", grupoId);
        db.update(DatabaseHelper.T_USUARIOS, cv, "id = ?", new String[]{String.valueOf(usuarioId)});
    }

    public void sumarPuntosSemanales(long grupoId, long usuarioId, int puntos) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.T_GRUPO_MIEMBROS +
                        " SET puntos_semanales = puntos_semanales + ? WHERE grupo_id = ? AND usuario_id = ?",
                new Object[]{puntos, grupoId, usuarioId});
    }

    /**
     * Registra actividad de estudio de HOY en una sola transacción:
     *  1. upsert de actividad_diaria
     *  2. recalculo de racha + racha máxima
     *  3. persistencia de la racha
     *
     * @return la nueva racha actual
     */
    public int registrarActividad(long usuarioId, int seccionesLeidas, int minutosPomodoro, int puntos) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String hoy = FechaUtil.hoy();

            // 1) upsert actividad diaria
            db.execSQL("INSERT INTO " + DatabaseHelper.T_ACTIVIDAD +
                            " (usuario_id, fecha, secciones_leidas, minutos_pomodoro, puntos) VALUES (?,?,?,?,?) " +
                            "ON CONFLICT(usuario_id, fecha) DO UPDATE SET " +
                            "secciones_leidas = secciones_leidas + excluded.secciones_leidas, " +
                            "minutos_pomodoro = minutos_pomodoro + excluded.minutos_pomodoro, " +
                            "puntos = puntos + excluded.puntos",
                    new Object[]{usuarioId, hoy, seccionesLeidas, minutosPomodoro, puntos});

            // 2) recalcular racha
            Usuario u = buscarPorId(usuarioId);
            int nuevaRacha = Gamificacion.calcularRacha(
                    u != null ? u.getRachaActual() : 0,
                    u != null ? u.getUltimaActividadFecha() : null);

            ContentValues cv = new ContentValues();
            cv.put("racha_actual", nuevaRacha);
            cv.put("ultima_actividad_fecha", hoy);
            if (u != null && nuevaRacha > u.getRachaMaxima()) {
                cv.put("racha_maxima", nuevaRacha);
            }
            db.update(DatabaseHelper.T_USUARIOS, cv, "id = ?", new String[]{String.valueOf(usuarioId)});

            db.setTransactionSuccessful();
            return nuevaRacha;
        } finally {
            db.endTransaction();
        }
    }

    /** Días consecutivos de racha (para mostrar en el dashboard). */
    public int diasDeRachaViva(long usuarioId) {
        Usuario u = buscarPorId(usuarioId);
        if (u == null || u.getUltimaActividadFecha() == null) return 0;
        return u.getRachaActual();
    }

    // ------------------------------------------------------------------
    // Mapeo
    // ------------------------------------------------------------------
    private Usuario mapear(Cursor c) {
        Usuario u = new Usuario();
        u.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        u.setNombre(c.getString(c.getColumnIndexOrThrow("nombre")));
        u.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        u.setPasswordHash(c.getString(c.getColumnIndexOrThrow("password_hash")));
        u.setPasswordSalt(c.getString(c.getColumnIndexOrThrow("password_salt")));
        u.setPuntos(c.getInt(c.getColumnIndexOrThrow("puntos")));
        u.setNivel(c.getInt(c.getColumnIndexOrThrow("nivel")));
        u.setExperiencia(c.getInt(c.getColumnIndexOrThrow("experiencia")));
        u.setRachaActual(c.getInt(c.getColumnIndexOrThrow("racha_actual")));
        u.setRachaMaxima(c.getInt(c.getColumnIndexOrThrow("racha_maxima")));
        u.setUltimaActividadFecha(c.getString(c.getColumnIndexOrThrow("ultima_actividad_fecha")));
        int gi = c.getColumnIndexOrThrow("grupo_id");
        u.setGrupoId(c.isNull(gi) ? null : c.getLong(gi));
        u.setCreadoEn(c.getLong(c.getColumnIndexOrThrow("creado_en")));
        return u;
    }

    private ContentValues toValues(Usuario u) {
        ContentValues cv = new ContentValues();
        cv.put("nombre", u.getNombre());
        cv.put("email", u.getEmail());
        cv.put("password_hash", u.getPasswordHash());
        cv.put("password_salt", u.getPasswordSalt());
        cv.put("puntos", u.getPuntos());
        cv.put("nivel", u.getNivel());
        cv.put("experiencia", u.getExperiencia());
        cv.put("racha_actual", u.getRachaActual());
        cv.put("racha_maxima", u.getRachaMaxima());
        cv.put("ultima_actividad_fecha", u.getUltimaActividadFecha());
        if (u.getGrupoId() != null) cv.put("grupo_id", u.getGrupoId());
        cv.put("creado_en", u.getCreadoEn());
        return cv;
    }
}

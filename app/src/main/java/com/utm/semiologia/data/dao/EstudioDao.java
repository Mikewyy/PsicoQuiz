package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Nota;
import com.utm.semiologia.data.model.ProgresoSeccion;

import java.util.ArrayList;
import java.util.List;

/** Acceso a notas y al progreso de lectura de secciones. */
public class EstudioDao {

    private final DatabaseHelper helper;

    public EstudioDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    // ==================================================================
    // NOTAS
    // ==================================================================

    public long insertarNota(Nota n) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("usuario_id", n.getUsuarioId());
        if (n.getSeccionId() != null) cv.put("seccion_id", n.getSeccionId());
        cv.put("texto_seleccionado", n.getTextoSeleccionado());
        cv.put("comentario", n.getComentario());
        cv.put("color", n.getColor());
        cv.put("creada_en", n.getCreadaEn());
        cv.put("sincronizada", 0);
        return db.insertOrThrow(DatabaseHelper.T_NOTAS, null, cv);
    }

    public void actualizarNota(long notaId, String comentario, String color) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("comentario", comentario);
        cv.put("color", color);
        cv.put("sincronizada", 0);
        db.update(DatabaseHelper.T_NOTAS, cv, "id = ?", new String[]{String.valueOf(notaId)});
    }

    public void eliminarNota(long notaId, long usuarioId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.T_NOTAS, "id = ? AND usuario_id = ?",
                new String[]{String.valueOf(notaId), String.valueOf(usuarioId)});
    }

    public List<Nota> listarNotas(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Nota> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_NOTAS, null,
                "usuario_id = ?", new String[]{String.valueOf(usuarioId)},
                null, null, "creada_en DESC")) {
            while (c.moveToNext()) lista.add(mapearNota(c));
        }
        return lista;
    }

    /** Notas de una sección concreta (para mostrarlas al lado del lector). */
    public List<Nota> listarNotasDeSeccion(long usuarioId, long seccionId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Nota> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_NOTAS, null,
                "usuario_id = ? AND seccion_id = ?",
                new String[]{String.valueOf(usuarioId), String.valueOf(seccionId)},
                null, null, "creada_en DESC")) {
            while (c.moveToNext()) lista.add(mapearNota(c));
        }
        return lista;
    }

    /** Marcar como sincronizadas tras subir al backend. */
    public void marcarNotasSincronizadas(List<Long> ids) {
        if (ids.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) sb.append(i > 0 ? "," : "").append(ids.get(i));
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.T_NOTAS + " SET sincronizada = 1 WHERE id IN (" + sb + ")");
    }

    // ==================================================================
    // PROGRESO DE SECCIONES
    // ==================================================================

    /** Crea la fila de progreso si no existe y devuelve el estado actual. */
    public ProgresoSeccion obtenerProgreso(long usuarioId, long seccionId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.T_PROGRESO, null,
                "usuario_id = ? AND seccion_id = ?",
                new String[]{String.valueOf(usuarioId), String.valueOf(seccionId)},
                null, null, null, "1")) {
            if (c.moveToFirst()) return mapearProgreso(c);
        }
        ProgresoSeccion nuevo = ProgresoSeccion.iniciar(usuarioId, seccionId);
        insertarProgreso(nuevo);
        return nuevo;
    }

    public long insertarProgreso(ProgresoSeccion p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("usuario_id", p.getUsuarioId());
        cv.put("seccion_id", p.getSeccionId());
        cv.put("lectura", p.getLectura());
        cv.put("completada", p.isCompletada() ? 1 : 0);
        cv.put("puntos_otorgados", p.getPuntosOtorgados());
        cv.put("comida_otorgada", p.getComidaOtorgada());
        cv.put("iniciada_en", p.getIniciadaEn());
        if (p.getCompletadaEn() != null) cv.put("completada_en", p.getCompletadaEn());
        return db.insertOrThrow(DatabaseHelper.T_PROGRESO, null, cv);
    }

    public void actualizarProgreso(ProgresoSeccion p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("lectura", p.getLectura());
        cv.put("completada", p.isCompletada() ? 1 : 0);
        cv.put("puntos_otorgados", p.getPuntosOtorgados());
        cv.put("comida_otorgada", p.getComidaOtorgada());
        if (p.getCompletadaEn() != null) cv.put("completada_en", p.getCompletadaEn());
        db.update(DatabaseHelper.T_PROGRESO, cv, "usuario_id = ? AND seccion_id = ?",
                new String[]{String.valueOf(p.getUsuarioId()), String.valueOf(p.getSeccionId())});
    }

    /** Catálogo completo de secciones con el progreso del usuario (LEFT JOIN). */
    public List<ProgresoSeccion> listarSeccionesConProgreso(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<ProgresoSeccion> lista = new ArrayList<>();
        String sql = "SELECT s.id AS seccion_id, s.titulo, s.tema, s.puntos_recompensa, " +
                "COALESCE(p.lectura, 0) AS lectura, COALESCE(p.completada, 0) AS completada, " +
                "p.id AS progreso_id " +
                "FROM " + DatabaseHelper.T_SECCIONES + " s " +
                "LEFT JOIN " + DatabaseHelper.T_PROGRESO + " p " +
                "  ON p.seccion_id = s.id AND p.usuario_id = ? " +
                "ORDER BY s.orden";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) {
                ProgresoSeccion p = new ProgresoSeccion();
                p.setUsuarioId(usuarioId);
                p.setSeccionId(c.getLong(c.getColumnIndexOrThrow("seccion_id")));
                p.setTitulo(c.getString(c.getColumnIndexOrThrow("titulo")));
                p.setTema(c.getString(c.getColumnIndexOrThrow("tema")));
                p.setPuntosRecompensa(c.getInt(c.getColumnIndexOrThrow("puntos_recompensa")));
                p.setLectura(c.getInt(c.getColumnIndexOrThrow("lectura")));
                p.setCompletada(c.getInt(c.getColumnIndexOrThrow("completada")) == 1);
                lista.add(p);
            }
        }
        return lista;
    }

    public int seccionesCompletadas(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.T_PROGRESO +
                " WHERE usuario_id = ? AND completada = 1", new String[]{String.valueOf(usuarioId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int seccionesTotales() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.T_SECCIONES, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // ==================================================================
    // Mapeo
    // ==================================================================
    private Nota mapearNota(Cursor c) {
        Nota n = new Nota();
        n.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        n.setUsuarioId(c.getLong(c.getColumnIndexOrThrow("usuario_id")));
        int si = c.getColumnIndexOrThrow("seccion_id");
        n.setSeccionId(c.isNull(si) ? null : c.getLong(si));
        n.setTextoSeleccionado(c.getString(c.getColumnIndexOrThrow("texto_seleccionado")));
        n.setComentario(c.getString(c.getColumnIndexOrThrow("comentario")));
        n.setColor(c.getString(c.getColumnIndexOrThrow("color")));
        n.setCreadaEn(c.getLong(c.getColumnIndexOrThrow("creada_en")));
        n.setSincronizada(c.getInt(c.getColumnIndexOrThrow("sincronizada")) == 1);
        return n;
    }

    private ProgresoSeccion mapearProgreso(Cursor c) {
        ProgresoSeccion p = new ProgresoSeccion();
        p.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        p.setUsuarioId(c.getLong(c.getColumnIndexOrThrow("usuario_id")));
        p.setSeccionId(c.getLong(c.getColumnIndexOrThrow("seccion_id")));
        p.setLectura(c.getInt(c.getColumnIndexOrThrow("lectura")));
        p.setCompletada(c.getInt(c.getColumnIndexOrThrow("completada")) == 1);
        p.setPuntosOtorgados(c.getInt(c.getColumnIndexOrThrow("puntos_otorgados")));
        p.setComidaOtorgada(c.getInt(c.getColumnIndexOrThrow("comida_otorgada")));
        p.setIniciadaEn(c.getLong(c.getColumnIndexOrThrow("iniciada_en")));
        int ce = c.getColumnIndexOrThrow("completada_en");
        p.setCompletadaEn(c.isNull(ce) ? null : c.getLong(ce));
        return p;
    }
}

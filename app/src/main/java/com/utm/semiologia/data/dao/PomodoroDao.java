package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.SesionPomodoro;
import com.utm.semiologia.firebase.FirebaseSecondarySyncManager;

import java.util.ArrayList;
import java.util.List;

/** Historial de sesiones Pomodoro y ranking semanal. */
public class PomodoroDao {

    private final DatabaseHelper helper;

    public PomodoroDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    public long insertar(SesionPomodoro s) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("usuario_id", s.getUsuarioId());
        cv.put("modo", s.getModo());
        if (s.getGrupoId() != null) cv.put("grupo_id", s.getGrupoId());
        cv.put("duracion_foco_min", s.getDuracionFocoMin());
        cv.put("duracion_descanso_min", s.getDuracionDescansoMin());
        cv.put("minutos_estudiados", s.getMinutosEstudiados());
        cv.put("ciclos_completados", s.getCiclosCompletados());
        cv.put("puntos_ganados", s.getPuntosGanados());
        cv.put("completado", s.isCompletado() ? 1 : 0);
        cv.put("iniciado_en", s.getIniciadoEn());
        if (s.getFinalizadoEn() != null) cv.put("finalizado_en", s.getFinalizadoEn());
        long id = db.insertOrThrow(DatabaseHelper.T_POMODORO, null, cv);
        s.setId(id);
        FirebaseSecondarySyncManager.sincronizarPomodoro(helper.getAppContext(), s);
        return id;
    }

    public void actualizar(SesionPomodoro s) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("minutos_estudiados", s.getMinutosEstudiados());
        cv.put("ciclos_completados", s.getCiclosCompletados());
        cv.put("puntos_ganados", s.getPuntosGanados());
        cv.put("completado", s.isCompletado() ? 1 : 0);
        if (s.getFinalizadoEn() != null) cv.put("finalizado_en", s.getFinalizadoEn());
        db.update(DatabaseHelper.T_POMODORO, cv, "id = ?", new String[]{String.valueOf(s.getId())});
        FirebaseSecondarySyncManager.sincronizarPomodoro(helper.getAppContext(), s);
    }

    public List<SesionPomodoro> historial(long usuarioId, int limite) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<SesionPomodoro> lista = new ArrayList<>();
        try (Cursor c = db.query(DatabaseHelper.T_POMODORO, null,
                "usuario_id = ? AND finalizado_en IS NOT NULL",
                new String[]{String.valueOf(usuarioId)},
                null, null, "iniciado_en DESC", String.valueOf(limite))) {
            while (c.moveToNext()) lista.add(mapear(c));
        }
        return lista;
    }

    /** Total de minutos en modo foco en los últimos 7 días. */
    public int minutosUltimaSemana(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        long desde = System.currentTimeMillis() - 7L * 86_400_000L;
        try (Cursor c = db.rawQuery("SELECT COALESCE(SUM(minutos_estudiados),0) FROM " +
                DatabaseHelper.T_POMODORO + " WHERE usuario_id = ? AND iniciado_en >= ?",
                new String[]{String.valueOf(usuarioId), String.valueOf(desde)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int totalCiclos(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COALESCE(SUM(ciclos_completados),0) FROM " +
                DatabaseHelper.T_POMODORO + " WHERE usuario_id = ?",
                new String[]{String.valueOf(usuarioId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Reinicia los puntos semanales de todos los miembros del grupo. */
    public void reiniciarRankingSemanal(long grupoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.T_GRUPO_MIEMBROS +
                " SET puntos_semanales = 0 WHERE grupo_id = ?", new Object[]{grupoId});
    }

    private SesionPomodoro mapear(Cursor c) {
        SesionPomodoro s = new SesionPomodoro();
        s.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        s.setUsuarioId(c.getLong(c.getColumnIndexOrThrow("usuario_id")));
        s.setModo(c.getString(c.getColumnIndexOrThrow("modo")));
        int gi = c.getColumnIndexOrThrow("grupo_id");
        s.setGrupoId(c.isNull(gi) ? null : c.getLong(gi));
        s.setDuracionFocoMin(c.getInt(c.getColumnIndexOrThrow("duracion_foco_min")));
        s.setDuracionDescansoMin(c.getInt(c.getColumnIndexOrThrow("duracion_descanso_min")));
        s.setMinutosEstudiados(c.getInt(c.getColumnIndexOrThrow("minutos_estudiados")));
        s.setCiclosCompletados(c.getInt(c.getColumnIndexOrThrow("ciclos_completados")));
        s.setPuntosGanados(c.getInt(c.getColumnIndexOrThrow("puntos_ganados")));
        s.setCompletado(c.getInt(c.getColumnIndexOrThrow("completado")) == 1);
        s.setIniciadoEn(c.getLong(c.getColumnIndexOrThrow("iniciado_en")));
        int fe = c.getColumnIndexOrThrow("finalizado_en");
        s.setFinalizadoEn(c.isNull(fe) ? null : c.getLong(fe));
        return s;
    }
}

package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.firebase.FirebaseSecondarySyncManager;
import com.utm.semiologia.util.FechaUtil;

/**
 * Desafío diario: resultado de la pregunta del día y racha de aciertos.
 *
 * El resultado y la racha viven en la fila UNIQUE(usuario_id, fecha) de
 * actividad_diaria. Solo se puede jugar una vez por día: una vez escrito
 * 'ok' o 'error' en la fecha de hoy, la racha no vuelve a consultar.
 */
public class DesafioDao {

    public static final String OK    = "ok";
    public static final String ERROR = "error";

    private final DatabaseHelper helper;

    public DesafioDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    /**
     * Resultado ya guardado del día. Devuelve null si el usuario todavía no
     * ha respondido el desafío de esa fecha.
     */
    public String resultadoHoy(long usuarioId, String fecha) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT " + DatabaseHelper.COL_DESAFIO +
                        " FROM " + DatabaseHelper.T_ACTIVIDAD +
                        " WHERE usuario_id = ? AND fecha = ?",
                new String[]{String.valueOf(usuarioId), fecha})) {
            if (c.moveToFirst()) {
                return c.isNull(0) ? null : c.getString(0);
            }
        }
        return null;
    }

    /** Guarda el resultado ('ok' o 'error') del desafío de una fecha. */
    public void marcarResultado(long usuarioId, String fecha, String resultado) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("usuario_id", usuarioId);
        cv.put("fecha", fecha);
        cv.put(DatabaseHelper.COL_DESAFIO, resultado);

        int actualizadas = db.update(
                DatabaseHelper.T_ACTIVIDAD,
                cv,
                "usuario_id = ? AND fecha = ?",
                new String[]{String.valueOf(usuarioId), fecha}
        );

        if (actualizadas == 0) {
            db.insert(DatabaseHelper.T_ACTIVIDAD, null, cv);
        }

        FirebaseSecondarySyncManager.sincronizarDesafio(
                helper.getAppContext(),
                usuarioId,
                fecha,
                resultado
        );
    }

    /**
     * Racha actual: días consecutivos acertados contando desde hoy (si el
     * desafío de hoy ya se ganó) o desde ayer (si hoy aún no se juega).
     * Un día sin jugar o fallado corta la racha.
     */
    public int rachaActual(long usuarioId, String hoy) {
        int nivel = OK.equals(resultadoHoy(usuarioId, hoy)) ? 0 : 1;
        String fecha = FechaUtil.haceDias(nivel);
        int racha = 0;
        while (OK.equals(resultadoHoy(usuarioId, fecha))) {
            racha++;
            if (racha > 366) break;
            nivel++;
            fecha = FechaUtil.haceDias(nivel);
        }
        return racha;
    }
}
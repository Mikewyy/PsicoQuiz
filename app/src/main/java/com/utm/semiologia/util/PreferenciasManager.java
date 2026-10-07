package com.utm.semiologia.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferencias generales de la app (independientes de la sesión).
 *
 * Por ahora solo guarda el interruptor de notificaciones del desafío diario.
 */
public final class PreferenciasManager {

    private static final String PREFS = "semiologia_prefs";
    private static final String K_NOTIFICACIONES = "notificaciones_desafio";

    private PreferenciasManager() {
    }

    public static boolean notificacionesActivas(Context context) {
        return prefs(context).getBoolean(K_NOTIFICACIONES, false);
    }

    public static void setNotificaciones(Context context, boolean activas) {
        prefs(context).edit().putBoolean(K_NOTIFICACIONES, activas).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
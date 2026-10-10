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
    private static final String K_AVISO_NOTIFICACIONES =
            "aviso_notificaciones_mostrado";

    private static final String K_TEMA = "tema_app";

    public static final int TEMA_SISTEMA = 0;
    public static final int TEMA_CLARO = 1;
    public static final int TEMA_OSCURO = 2;

    private PreferenciasManager() {
    }

    public static boolean notificacionesActivas(Context context) {
        return prefs(context).getBoolean(K_NOTIFICACIONES, false);
    }

    public static void setNotificaciones(Context context, boolean activas) {
        prefs(context).edit().putBoolean(K_NOTIFICACIONES, activas).apply();
    }

    /** Si el aviso de activación ya se mostró alguna vez (una sola, global). */
    public static boolean avisoNotificacionesMostrado(Context context) {
        return prefs(context).getBoolean(K_AVISO_NOTIFICACIONES, false);
    }

    public static void marcarAvisoNotificaciones(Context context) {
        prefs(context).edit().putBoolean(K_AVISO_NOTIFICACIONES, true).apply();
    }


    /** Tema visual elegido por el usuario. Por defecto sigue al sistema. */
    public static int temaApp(Context context) {
        return prefs(context).getInt(K_TEMA, TEMA_SISTEMA);
    }

    public static void setTemaApp(Context context, int tema) {
        prefs(context).edit().putInt(K_TEMA, tema).apply();
    }

    public static int modoNocheAppCompat(Context context) {
        switch (temaApp(context)) {
            case TEMA_CLARO:
                return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
            case TEMA_OSCURO:
                return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
            case TEMA_SISTEMA:
            default:
                return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static String nombreTema(Context context) {
        switch (temaApp(context)) {
            case TEMA_CLARO:
                return "Claro";
            case TEMA_OSCURO:
                return "Oscuro";
            default:
                return "Sistema";
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
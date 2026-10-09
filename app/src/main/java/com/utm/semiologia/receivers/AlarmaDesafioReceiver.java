package com.utm.semiologia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.util.FechaUtil;
import com.utm.semiologia.util.NotificacionesUtil;
import com.utm.semiologia.util.PreferenciasManager;

public class AlarmaDesafioReceiver extends BroadcastReceiver {

    private static final String TAG = "PSICO_NOTIF";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e(TAG, "1. Receiver ejecutado");

        // Comprobar si las notificaciones están activadas
        boolean notificacionesActivas =
                PreferenciasManager.notificacionesActivas(context);

        Log.e(
                TAG,
                "2. Notificaciones activas = " + notificacionesActivas
        );

        if (!notificacionesActivas) {

            Log.e(
                    TAG,
                    "DETENIDO: las notificaciones están desactivadas"
            );

            return;
        }

        // Obtener el usuario actualmente conectado
        long usuarioId =
                SemiologiaApp
                        .getSesion()
                        .getUsuarioId();

        Log.e(
                TAG,
                "3. Usuario ID = " + usuarioId
        );

        if (usuarioId <= 0) {

            Log.e(
                    TAG,
                    "DETENIDO: no existe un usuario activo"
            );

            return;
        }

        // Obtener fecha actual
        String fecha = FechaUtil.hoy();

        Log.e(
                TAG,
                "4. Fecha actual = " + fecha
        );

        // Comprobar si el desafío ya fue realizado hoy
        String resultado =
                SemiologiaApp
                        .getRepositorio()
                        .desafios()
                        .resultadoHoy(
                                usuarioId,
                                fecha
                        );

        Log.e(
                TAG,
                "5. Resultado del desafío = " + resultado
        );

        // Si ya existe resultado, no mostrar notificación
        if (resultado != null) {

            Log.e(
                    TAG,
                    "DETENIDO: el desafío de hoy ya fue realizado"
            );

            return;
        }

        Log.e(
                TAG,
                "6. Llamando a NotificacionesUtil.notificarDesafio()"
        );

        // Mostrar la notificación real de PsicoQuiz
        NotificacionesUtil.notificarDesafio(context);

        Log.e(
                TAG,
                "7. notificarDesafio() terminó correctamente"
        );
    }
}
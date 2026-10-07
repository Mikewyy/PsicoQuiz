package com.utm.semiologia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.util.FechaUtil;
import com.utm.semiologia.util.NotificacionesUtil;
import com.utm.semiologia.util.PreferenciasManager;

/**
 * Dispara el recordatorio del desafío diario.
 *
 * Solo notifica si las notificaciones siguen activas, hay sesión abierta y el
 * desafío de hoy todavía no se ha jugado.
 */
public class AlarmaDesafioReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (
                !PreferenciasManager.notificacionesActivas(context)
                        || SemiologiaApp.getSesion().getUsuarioId() <= 0
        ) {
            return;
        }

        String resultado =
                SemiologiaApp
                        .getRepositorio()
                        .desafios()
                        .resultadoHoy(
                                SemiologiaApp.getSesion().getUsuarioId(),
                                FechaUtil.hoy()
                        );

        if (resultado != null) {
            return;
        }

        NotificacionesUtil.notificarDesafio(context);
    }
}
package com.utm.semiologia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.utm.semiologia.util.NotificacionesUtil;

/**
 * Reprograma el recordatorio del desafío tras reiniciar el dispositivo.
 *
 * Las alarmas de AlarmManager no sobreviven a un reinicio, así que se vuelven
 * a agendar si la preferencia de notificaciones sigue activa.
 */
public class ReinicioAlarmasReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            NotificacionesUtil.programarSiActivo(context);
        }
    }
}
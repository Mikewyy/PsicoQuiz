package com.utm.semiologia.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.utm.semiologia.R;
import com.utm.semiologia.receivers.AlarmaDesafioReceiver;
import com.utm.semiologia.ui.common.NavegacionInferior;
import com.utm.semiologia.ui.dashboard.MainActivity;

import java.util.Calendar;

/**
 * Notificaciones locales del desafío diario.
 *
 * Se usa AlarmManager (sin dependencias externas) para lanzar un recordatorio
 * en horario fijo cada día. El receptor solo publica la notificación si el
 * desafío de hoy aún no se ha jugado.
 */
public final class NotificacionesUtil {

    public static final String CANAL_DESAFIO = "desafio_diario";

    private static final String ACCION_RECORDATORIO =
            "com.utm.semiologia.ALARMA_DESAFIO";

    private static final int CODIGO_RECORDATORIO = 202;
    private static final int CODIGO_ABRIR_APP = 203;

    /** Hora (local) en la que se recuerda el desafío: 07:00. */
    private static final int HORA_RECORDATORIO = 7;

    private NotificacionesUtil() {
    }

    public static void crearCanal(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager nm =
                context.getSystemService(NotificationManager.class);

        if (nm == null) {
            return;
        }

        NotificationChannel canal =
                new NotificationChannel(
                        CANAL_DESAFIO,
                        "Desafío diario",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        canal.setDescription(
                "Recordatorio del desafío diario de PsicoQuiz"
        );

        nm.createNotificationChannel(canal);
    }

    /** Programa el recordatorio diario solo si la preferencia está activa. */
    public static void programarSiActivo(Context context) {

        if (PreferenciasManager.notificacionesActivas(context)) {
            programarRecordatorio(context);
        }
    }

    public static void programarRecordatorio(Context context) {

        if (!PreferenciasManager.notificacionesActivas(context)) {
            return;
        }

        AlarmManager am =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (am == null) {
            return;
        }

        Calendar proximo =
                Calendar.getInstance();

        proximo.set(
                Calendar.HOUR_OF_DAY,
                HORA_RECORDATORIO
        );

        proximo.set(
                Calendar.MINUTE,
                0
        );

        proximo.set(
                Calendar.SECOND,
                0
        );

        proximo.set(
                Calendar.MILLISECOND,
                0
        );

        if (!proximo.after(Calendar.getInstance())) {
            proximo.add(
                    Calendar.DAY_OF_YEAR,
                    1
            );
        }

        am.setInexactRepeating(
                AlarmManager.RTC,
                proximo.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent(context)
        );
    }

    public static void cancelarRecordatorio(Context context) {

        AlarmManager am =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (am == null) {
            return;
        }

        am.cancel(
                pendingIntent(context)
        );
    }

    /** Publica la notificación del desafío diario. */
    public static void notificarDesafio(Context context) {

        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        Intent abrir =
                new Intent(
                        context,
                        MainActivity.class
                );

        abrir.putExtra(
                NavegacionInferior.EXTRA_SECCION,
                NavegacionInferior.SECCION_DESAFIOS
        );

        abrir.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent piAbrir =
                PendingIntent.getActivity(
                        context,
                        CODIGO_ABRIR_APP,
                        abrir,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CANAL_DESAFIO
                )
                        .setSmallIcon(R.drawable.ic_nav_desafios)
                        .setContentTitle(
                                "¡Ya está disponible el desafío de hoy!"
                        )
                        .setContentText(
                                "Responde la pregunta del día y gana galletas para tu mascota."
                        )
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(
                                                "Responde la pregunta del día y gana galletas para tu mascota."
                                        )
                        )
                        .setAutoCancel(true)
                        .setContentIntent(piAbrir);

        NotificationManager nm =
                context.getSystemService(NotificationManager.class);

        if (nm != null) {
            nm.notify(
                    CODIGO_RECORDATORIO,
                    builder.build()
            );
        }
    }

    private static PendingIntent pendingIntent(Context context) {

        Intent intent =
                new Intent(
                        context,
                        AlarmaDesafioReceiver.class
                );

        intent.setAction(
                ACCION_RECORDATORIO
        );

        return PendingIntent.getBroadcast(
                context,
                CODIGO_RECORDATORIO,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
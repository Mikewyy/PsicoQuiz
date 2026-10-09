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
 * Se usa AlarmManager para lanzar un recordatorio diario.
 * El receptor solo publica la notificación si el desafío
 * de hoy todavía no ha sido jugado.
 */
public final class NotificacionesUtil {

    public static final String CANAL_DESAFIO = "desafio_diario";

    private static final String ACCION_RECORDATORIO =
            "com.utm.semiologia.ALARMA_DESAFIO";

    private static final int CODIGO_RECORDATORIO = 202;
    private static final int CODIGO_ABRIR_APP = 203;

    /** Hora local del recordatorio diario. */
    private static final int HORA_RECORDATORIO = 7;

    private NotificacionesUtil() {
    }

    /**
     * Crea el canal de notificaciones para Android 8 o superior.
     */
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
                "Recordatorios del desafío diario de PsicoQuiz"
        );

        /*
         * El canal usa el comportamiento normal del sistema.
         * No se configura ningún diseño personalizado.
         */
        nm.createNotificationChannel(canal);
    }

    /**
     * Programa el recordatorio si las notificaciones están activadas.
     */
    public static void programarSiActivo(Context context) {

        if (PreferenciasManager.notificacionesActivas(context)) {
            programarRecordatorio(context);
        }
    }

    /**
     * Programa el recordatorio diario.
     */
    public static void programarRecordatorio(Context context) {

        if (!PreferenciasManager.notificacionesActivas(context)) {
            return;
        }

        AlarmManager am =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

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

        /*
         * Si la hora de hoy ya pasó,
         * se programa para el día siguiente.
         */
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

    /**
     * Cancela el recordatorio diario.
     */
    public static void cancelarRecordatorio(Context context) {

        AlarmManager am =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (am == null) {
            return;
        }

        am.cancel(
                pendingIntent(context)
        );
    }

    /**
     * Publica la notificación estándar de Android.
     */
    public static void notificarDesafio(Context context) {

        /*
         * Android 13 o superior requiere permiso explícito.
         */
        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        /*
         * Abre directamente la sección de desafíos.
         */
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
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent piAbrir =
                PendingIntent.getActivity(
                        context,
                        CODIGO_ABRIR_APP,
                        abrir,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * NOTIFICACIÓN NORMAL DEL SISTEMA
         */
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CANAL_DESAFIO
                )

                        // Icono pequeño obligatorio
                        .setSmallIcon(
                                R.drawable.ic_nav_desafios
                        )

                        // Título
                        .setContentTitle(
                                "Desafío diario disponible"
                        )

                        // Mensaje
                        .setContentText(
                                "Responde la pregunta del día y gana galletas para tu mascota."
                        )

                        // Permite mostrar todo el mensaje al expandir
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(
                                                "Responde la pregunta del día y gana galletas para tu mascota."
                                        )
                        )

                        /*
                         * IMPORTANTE:
                         * Oculta la hora de publicación.
                         */
                        .setShowWhen(false)

                        /*
                         * Comportamiento estándar de Android.
                         */
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )

                        .setCategory(
                                NotificationCompat.CATEGORY_REMINDER
                        )

                        /*
                         * Al tocarla se elimina.
                         */
                        .setAutoCancel(true)

                        /*
                         * Abre PsicoQuiz.
                         */
                        .setContentIntent(piAbrir);

        NotificationManager nm =
                context.getSystemService(
                        NotificationManager.class
                );

        if (nm != null) {

            nm.notify(
                    CODIGO_RECORDATORIO,
                    builder.build()
            );
        }
    }

    /**
     * PendingIntent utilizado por AlarmManager.
     */
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
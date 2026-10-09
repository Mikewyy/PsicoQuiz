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
 * Gestiona las notificaciones locales del desafío diario.
 */
public final class NotificacionesUtil {

    // =========================================================
    // CONSTANTES
    // =========================================================

    /**
     * Canal de notificaciones del desafío diario.
     */
    public static final String CANAL_DESAFIO =
            "desafio_diario_v2";

    /**
     * Acción utilizada por la alarma.
     */
    private static final String ACCION_RECORDATORIO =
            "com.utm.semiologia.ALARMA_DESAFIO";

    /**
     * Código del PendingIntent de la alarma.
     */
    private static final int CODIGO_RECORDATORIO = 202;

    /**
     * Código del PendingIntent que abre PsicoQuiz.
     */
    private static final int CODIGO_ABRIR_APP = 203;

    /**
     * Hora del recordatorio diario.
     *
     * 7 = 7:00 a. m.
     */
    private static final int HORA_RECORDATORIO = 7;

    private NotificacionesUtil() {
        // Evita crear instancias de esta clase.
    }


    // =========================================================
    // CANAL DE NOTIFICACIONES
    // =========================================================

    /**
     * Crea el canal utilizado para el desafío diario.
     */
    public static void crearCanal(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager nm =
                context.getSystemService(
                        NotificationManager.class
                );

        if (nm == null) {
            return;
        }

        NotificationChannel canal =
                new NotificationChannel(
                        CANAL_DESAFIO,
                        "Desafío diario",
                        NotificationManager.IMPORTANCE_HIGH
                );

        canal.setDescription(
                "Recordatorios del desafío diario de PsicoQuiz"
        );

        canal.enableVibration(true);

        nm.createNotificationChannel(canal);
    }


    // =========================================================
    // PROGRAMAR SI ESTÁN ACTIVADAS
    // =========================================================

    /**
     * Programa el recordatorio solamente si el usuario
     * tiene activadas las notificaciones.
     */
    public static void programarSiActivo(Context context) {

        if (
                PreferenciasManager
                        .notificacionesActivas(context)
        ) {

            programarRecordatorio(context);
        }
    }


    // =========================================================
    // PROGRAMAR PRÓXIMO RECORDATORIO
    // =========================================================

    /**
     * Programa el próximo recordatorio para las 7:00 a. m.
     *
     * Si las 7:00 a. m. de hoy ya pasaron,
     * se programa para mañana.
     *
     * Se utiliza RTC_WAKEUP para que la alarma pueda ejecutarse
     * aunque el dispositivo se encuentre en reposo.
     *
     * Esta alarma NO es repetitiva. El receiver programa
     * nuevamente la alarma para el día siguiente.
     */
    public static void programarRecordatorio(Context context) {

        if (
                !PreferenciasManager
                        .notificacionesActivas(context)
        ) {
            return;
        }


        AlarmManager am =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (am == null) {
            return;
        }


        // -----------------------------------------------------
        // CALCULAR PRÓXIMAS 7:00 A. M.
        // -----------------------------------------------------

        Calendar ahora =
                Calendar.getInstance();

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
         * Si las 7:00 a. m. ya pasaron hoy,
         * programamos para mañana.
         */
        if (!proximo.after(ahora)) {

            proximo.add(
                    Calendar.DAY_OF_YEAR,
                    1
            );
        }


        // -----------------------------------------------------
        // PROGRAMAR ALARMA
        // -----------------------------------------------------

        PendingIntent pi =
                pendingIntent(context);


        /*
         * setAndAllowWhileIdle permite que la alarma pueda
         * ejecutarse incluso durante Doze.
         *
         * No requiere el permiso especial de alarmas exactas.
         */
        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.M
        ) {

            am.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    proximo.getTimeInMillis(),
                    pi
            );

        } else {

            /*
             * Compatibilidad con Android antiguos.
             */
            am.set(
                    AlarmManager.RTC_WAKEUP,
                    proximo.getTimeInMillis(),
                    pi
            );
        }
    }


    // =========================================================
    // CANCELAR RECORDATORIO
    // =========================================================

    /**
     * Cancela el próximo recordatorio programado.
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


    // =========================================================
    // MOSTRAR NOTIFICACIÓN
    // =========================================================

    /**
     * Publica la notificación del desafío diario.
     */
    public static void notificarDesafio(Context context) {

        // -----------------------------------------------------
        // PERMISO ANDROID 13+
        // -----------------------------------------------------

        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.TIRAMISU

                        && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            return;
        }


        // -----------------------------------------------------
        // ASEGURAR QUE EL CANAL EXISTA
        // -----------------------------------------------------

        crearCanal(context);


        // -----------------------------------------------------
        // INTENT PARA ABRIR PSICOQUIZ
        // -----------------------------------------------------

        Intent abrir =
                new Intent(
                        context,
                        MainActivity.class
                );

        /*
         * Al tocar la notificación,
         * abre directamente Desafíos.
         */
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


        // =====================================================
        // CONSTRUIR NOTIFICACIÓN
        // =====================================================

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CANAL_DESAFIO
                )

                        /*
                         * Android necesita un icono pequeño
                         * para todas las notificaciones.
                         */
                        .setSmallIcon(
                                R.drawable.ic_nav_desafios
                        )

                        .setContentTitle(
                                "Desafío diario disponible"
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

                        /*
                         * No mostrar la hora de publicación.
                         */
                        .setShowWhen(false)

                        /*
                         * Prioridad alta.
                         */
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )

                        /*
                         * Android identifica esta notificación
                         * como un recordatorio.
                         */
                        .setCategory(
                                NotificationCompat.CATEGORY_REMINDER
                        )

                        /*
                         * La notificación desaparece cuando
                         * el usuario la toca.
                         */
                        .setAutoCancel(true)

                        /*
                         * Acción al tocar la notificación.
                         */
                        .setContentIntent(
                                piAbrir
                        );


        // =====================================================
        // PUBLICAR NOTIFICACIÓN
        // =====================================================

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


    // =========================================================
    // PENDING INTENT DE LA ALARMA
    // =========================================================

    /**
     * PendingIntent que ejecutará AlarmaDesafioReceiver.
     */
    private static PendingIntent pendingIntent(
            Context context
    ) {

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
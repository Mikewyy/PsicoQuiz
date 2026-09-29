package com.utm.semiologia.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fechas en formato 'YYYY-MM-DD' (hora LOCAL del usuario).
 *
 * Importante para las rachas: se usa el calendario local, no UTC, porque
 * "estudiar hoy" significa según el reloj del estudiante, no según Greenwich.
 */
public final class FechaUtil {

    private static final String PATRON = "yyyy-MM-dd";
    private static final TimeZone ZONA = TimeZone.getDefault();

    private FechaUtil() {
    }

    private static SimpleDateFormat formateador() {
        SimpleDateFormat sdf = new SimpleDateFormat(PATRON, Locale.US);
        sdf.setTimeZone(ZONA);
        return sdf;
    }

    /** Fecha de hoy en 'YYYY-MM-DD'. */
    public static String hoy() {
        return formateador().format(new Date());
    }

    /** Fecha de hace {@code dias} días, en 'YYYY-MM-DD'. */
    public static String haceDias(int dias) {
        Calendar c = Calendar.getInstance(ZONA);
        c.add(Calendar.DAY_OF_YEAR, -dias);
        return formateador().format(c.getTime());
    }

    /**
     * Número de días entre dos fechas 'YYYY-MM-DD' (b - a).
     * Devuelve un valor negativo si b es anterior a a.
     */
    public static int diasEntre(String a, String b) {
        if (a == null || b == null) return 0;
        try {
            SimpleDateFormat sdf = formateador();
            long msA = sdf.parse(a).getTime();
            long msB = sdf.parse(b).getTime();
            return (int) Math.round((msB - msA) / 86_400_000.0);
        } catch (java.text.ParseException e) {
            return 0;
        }
    }

    /** true si la fecha es hoy. */
    public static boolean esHoy(String fecha) {
        return hoy().equals(fecha);
    }

    /** true si la fecha es exactamente ayer. */
    public static boolean esAyer(String fecha) {
        return haceDias(1).equals(fecha);
    }
}

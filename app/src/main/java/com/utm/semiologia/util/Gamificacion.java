package com.utm.semiologia.util;

/**
 * Reglas de la economía de puntos y del cálculo de rachas.
 *
 * Todos los valores están en un solo lugar para que el equipo de balance
 * pueda tunearlos, y para que el backend replique exactamente la misma tabla.
 */
public final class Gamificacion {

    // ---- Recompensas ----
    public static final int PUNTOS_POR_SECCION_LEIDA   = 20;
    public static final int COMIDA_POR_SECCION_LEIDA    = 2;   // unidades de comida
    public static final int COMIDA_BONUS_RACHA_DIARIA     = 1;   // recompensa por mantener racha
    public static final int PUNTOS_POR_MINUTO_POMODORO  = 1;
    public static final int PUNTOS_POR_CICLO_COMPLETO   = 25;
    public static final int PUNTOS_BONUS_RACHA          = 10;  // extra por cada 7 días
    public static final int PUNTOS_CASO_CLINICO_CORRECTO = 15;
    public static final int PUNTOS_CASO_CLINICO_INCORRECTO = 3;
    public static final int PUNTOS_NOTA_COMPARTIDA      = 20;

    // ---- Recompensas del modulo de evaluacion (tramos) ----
    /** Bonus en puntos la PRIMERA vez que se aprueba un tramo (>= 80%). */
    public static final int PUNTOS_BONUS_TRAMO_APROBADO = 50;
    /** Unidades de comida que se regalan al aprobar un tramo por primera vez. */
    public static final int COMIDA_POR_TRAMO_APROBADO   = 2;
    /** Fila 'Galleta' de la tabla alimentos, la comida por defecto. */
    public static final long ALIMENTO_GALLETA_ID        = 1L;
    /** Energía de la mascota que cuesta empezar un intento de preguntas. */
    public static final int ENERGIA_POR_INTENTO         = 5;

    // ---- Recompensas del sistema Pomodoro ----
    /** Probabilidad (0..1) de ganar una barita mágica al completar un pomodoro. */
    public static final float PROBABILIDAD_BARITA_MAGICA = 0.50f;
    /** Unidades de barita que se otorgan cuando toca. */
    public static final int BARITAS_POR_POMODORO        = 1;
    /** Tipo de objeto guardado en inventario_objetos. */
    public static final String OBJETO_BARITA_MAGICA     = "barita_magica";

    private Gamificacion() {
    }

    /**
     * Calcula la nueva racha tras estudiar hoy.
     *
     * Reglas:
     *  - Primera actividad registrada -> racha = 1
     *  - Ya estudió hoy          -> racha NO cambia (no se infla)
     *  - Estudió ayer           -> racha + 1
     *  - Pasó 2+ días           -> racha = 1 (se rompe)
     *
     * @param rachaActual      racha guardada en el usuario
     * @param ultimaActividad  'YYYY-MM-DD' de la última actividad, o null
     * @return la nueva racha
     */
    public static int calcularRacha(int rachaActual, String ultimaActividad) {
        return calcularRacha(rachaActual, ultimaActividad, FechaUtil.hoy());
    }

    /**
     * Igual que {@link #calcularRacha(int, String)} pero con la fecha de
     * referencia explícita. Existe para poder verificar de forma determinista
     * secuencias de varios días sin depender del reloj del sistema.
     */
    public static int calcularRacha(int rachaActual, String ultimaActividad, String hoy) {
        if (ultimaActividad == null) {
            return 1;
        }
        int dias = FechaUtil.diasEntre(ultimaActividad, hoy);
        if (dias <= 0) {
            return Math.max(1, rachaActual);   // ya estudió hoy: no penalizar
        }
        if (dias == 1) {
            return rachaActual + 1;            // día consecutivo
        }
        return 1;                              // racha rota
    }

    /** Bonus adicional cada 7 días de racha mantenida. */
    public static int bonusRacha(int rachaActual) {
        return (rachaActual / 7) * PUNTOS_BONUS_RACHA;
    }

    /** Calcula la comida otorgada por mantener o incrementar la racha. */
    public static int calcularComidaRacha(int rachaActual) {
        return COMIDA_BONUS_RACHA_DIARIA;
    }

    /**
     * ¿La racha sigue viva? Se usa para mostrar el aviso "¡no pierdas tu racha!"
     * en el dashboard aunque el usuario no haya estudiado hoy.
     */
    public static boolean rachaEnRiesgo(String ultimaActividad) {
        if (ultimaActividad == null) return false;
        return FechaUtil.diasEntre(ultimaActividad, FechaUtil.hoy()) >= 1;
    }
}

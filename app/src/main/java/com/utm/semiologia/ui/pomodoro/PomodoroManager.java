package com.utm.semiologia.ui.pomodoro;

import android.content.Context;
import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador global del Pomodoro.
 *
 * Mantiene el temporizador aunque cambies de Activity dentro de la app,
 * siempre que el proceso de la aplicación siga activo.
 */
public class PomodoroManager {

    public enum Estado {
        LISTO,
        ENFOQUE,
        DESCANSO,
        PAUSADO_ENFOQUE,
        PAUSADO_DESCANSO
    }

    public interface Listener {
        void onPomodoroActualizado();
        void onEnfoqueCompletado();
        void onDescansoCompletado();
    }

    private static final int MIN_MINUTOS = 10;
    private static final int MAX_MINUTOS = 60;
    private static final int DESCANSO_MINUTOS = 5;

    private static PomodoroManager instance;

    private final Context appContext;

    private final List<Listener> listeners = new ArrayList<>();

    private CountDownTimer timer;

    private Estado estado = Estado.LISTO;

    private int minutosEnfoque = 20;
    private int ciclosCompletados = 0;

    private long tiempoRestanteMs = 20 * 60_000L;

    /**
     * Hora exacta en la que debería terminar el periodo actual.
     * Permite calcular correctamente el tiempo aunque la Activity
     * deje de estar visible durante un momento.
     */
    private long finalizaEn = 0L;

    private PomodoroManager(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new PomodoroManager(
                    context.getApplicationContext()
            );
        }
    }

    public static synchronized PomodoroManager get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "PomodoroManager no ha sido inicializado"
            );
        }

        return instance;
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    public void agregarListener(Listener listener) {
        if (listener == null) return;

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }

        listener.onPomodoroActualizado();
    }

    public void quitarListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notificarActualizacion() {
        List<Listener> copia = new ArrayList<>(listeners);

        for (Listener listener : copia) {
            listener.onPomodoroActualizado();
        }
    }

    private void notificarEnfoqueCompletado() {
        List<Listener> copia = new ArrayList<>(listeners);

        for (Listener listener : copia) {
            listener.onEnfoqueCompletado();
        }
    }

    private void notificarDescansoCompletado() {
        List<Listener> copia = new ArrayList<>(listeners);

        for (Listener listener : copia) {
            listener.onDescansoCompletado();
        }
    }

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    public void setMinutosEnfoque(int minutos) {
        if (estaActivo() || estaPausado()) {
            return;
        }

        if (minutos < MIN_MINUTOS) {
            minutos = MIN_MINUTOS;
        }

        if (minutos > MAX_MINUTOS) {
            minutos = MAX_MINUTOS;
        }

        // Redondear a múltiplos de 5.
        minutos = Math.round(minutos / 5f) * 5;

        minutosEnfoque = minutos;
        tiempoRestanteMs = minutosEnfoque * 60_000L;

        notificarActualizacion();
    }

    // =========================================================
    // INICIAR
    // =========================================================

    public void iniciarEnfoque() {
        cancelarTimerInterno();

        estado = Estado.ENFOQUE;

        tiempoRestanteMs =
                minutosEnfoque * 60_000L;

        iniciarTimer();
    }

    private void iniciarDescanso() {
        cancelarTimerInterno();

        estado = Estado.DESCANSO;

        tiempoRestanteMs =
                DESCANSO_MINUTOS * 60_000L;

        iniciarTimer();
    }

    // =========================================================
    // TIMER
    // =========================================================

    private void iniciarTimer() {
        cancelarTimerInterno();

        if (tiempoRestanteMs <= 0) {
            tiempoRestanteMs = 1000;
        }

        finalizaEn =
                System.currentTimeMillis()
                        + tiempoRestanteMs;

        timer = new CountDownTimer(
                tiempoRestanteMs,
                1000
        ) {
            @Override
            public void onTick(long millisUntilFinished) {

                tiempoRestanteMs =
                        Math.max(
                                0,
                                finalizaEn
                                        - System.currentTimeMillis()
                        );

                notificarActualizacion();
            }

            @Override
            public void onFinish() {
                tiempoRestanteMs = 0;
                finalizaEn = 0;

                manejarFinalizacion();
            }
        }.start();

        notificarActualizacion();
    }

    private void manejarFinalizacion() {

        if (estado == Estado.ENFOQUE) {

            ciclosCompletados++;

            notificarActualizacion();
            notificarEnfoqueCompletado();

            // El descanso comienza automáticamente.
            iniciarDescanso();

        } else if (estado == Estado.DESCANSO) {

            estado = Estado.LISTO;

            tiempoRestanteMs =
                    minutosEnfoque * 60_000L;

            notificarActualizacion();
            notificarDescansoCompletado();
        }
    }

    // =========================================================
    // PAUSA
    // =========================================================

    public void pausar() {

        if (estado != Estado.ENFOQUE
                && estado != Estado.DESCANSO) {
            return;
        }

        tiempoRestanteMs =
                Math.max(
                        0,
                        finalizaEn
                                - System.currentTimeMillis()
                );

        cancelarTimerInterno();

        if (estado == Estado.ENFOQUE) {
            estado = Estado.PAUSADO_ENFOQUE;
        } else {
            estado = Estado.PAUSADO_DESCANSO;
        }

        finalizaEn = 0;

        notificarActualizacion();
    }

    // =========================================================
    // REANUDAR
    // =========================================================

    public void reanudar() {

        if (estado == Estado.PAUSADO_ENFOQUE) {

            estado = Estado.ENFOQUE;
            iniciarTimer();

        } else if (estado == Estado.PAUSADO_DESCANSO) {

            estado = Estado.DESCANSO;
            iniciarTimer();
        }
    }

    // =========================================================
    // REINICIAR
    // =========================================================

    public void reiniciar() {
        cancelarTimerInterno();

        estado = Estado.LISTO;

        tiempoRestanteMs =
                minutosEnfoque * 60_000L;

        finalizaEn = 0;

        notificarActualizacion();
    }

    /**
     * Cancela completamente el Pomodoro.
     *
     * Conservamos este método porque ya existía en tu proyecto
     * y puede estar siendo utilizado al cerrar sesión.
     */
    public void cancelar() {
        cancelarTimerInterno();

        estado = Estado.LISTO;

        ciclosCompletados = 0;

        tiempoRestanteMs =
                minutosEnfoque * 60_000L;

        finalizaEn = 0;

        notificarActualizacion();
    }

    private void cancelarTimerInterno() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Estado getEstado() {
        return estado;
    }

    public int getMinutosEnfoque() {
        return minutosEnfoque;
    }

    public int getMinutosDescanso() {
        return DESCANSO_MINUTOS;
    }

    public int getCiclosCompletados() {
        return ciclosCompletados;
    }

    public long getTiempoRestanteMs() {

        if (estado == Estado.ENFOQUE
                || estado == Estado.DESCANSO) {

            tiempoRestanteMs =
                    Math.max(
                            0,
                            finalizaEn
                                    - System.currentTimeMillis()
                    );
        }

        return tiempoRestanteMs;
    }

    public boolean estaActivo() {
        return estado == Estado.ENFOQUE
                || estado == Estado.DESCANSO;
    }

    public boolean estaPausado() {
        return estado == Estado.PAUSADO_ENFOQUE
                || estado == Estado.PAUSADO_DESCANSO;
    }

    public boolean estaEnDescanso() {
        return estado == Estado.DESCANSO
                || estado == Estado.PAUSADO_DESCANSO;
    }

    public boolean estaEnEnfoque() {
        return estado == Estado.ENFOQUE
                || estado == Estado.PAUSADO_ENFOQUE;
    }
}
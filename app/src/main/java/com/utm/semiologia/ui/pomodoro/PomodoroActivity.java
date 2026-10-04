package com.utm.semiologia.ui.pomodoro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.utm.semiologia.R;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.SesionPomodoro;
import com.utm.semiologia.util.SesionManager;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private static final int MIN_MINUTOS = 10;
    private static final int MAX_MINUTOS = 60;
    private static final int PASO_MINUTOS = 5;
    private static final int DESCANSO_MINUTOS = 5;

    private TextView tvTiempo;
    private TextView tvModo;
    private TextView tvMinutosSeleccionados;
    private TextView tvCiclos;

    private SeekBar seekTiempo;

    private Button btnIniciar;
    private Button btnPausar;
    private Button btnReiniciar;

    private Repositorio repo;
    private SesionManager sesionManager;

    private CountDownTimer countDownTimer;

    private int minutosSeleccionados = 20;

    private long tiempoRestanteMs;
    private boolean temporizadorActivo = false;
    private boolean pausado = false;
    private boolean enDescanso = false;

    private int ciclosCompletados = 0;

    private SesionPomodoro sesionActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        repo = Repositorio.get(this);
        sesionManager = new SesionManager(this);

        enlazarVistas();
        configurarBarra();
        configurarBotones();

        actualizarTiempoInicial();
        actualizarCiclos();
    }

    private void enlazarVistas() {
        tvTiempo = findViewById(R.id.tvTiempo);
        tvModo = findViewById(R.id.tvModo);
        tvMinutosSeleccionados = findViewById(R.id.tvMinutosSeleccionados);
        tvCiclos = findViewById(R.id.tvCiclos);

        seekTiempo = findViewById(R.id.seekTiempo);

        btnIniciar = findViewById(R.id.btnIniciar);
        btnPausar = findViewById(R.id.btnPausar);
        btnReiniciar = findViewById(R.id.btnReiniciar);
    }

    private void configurarBarra() {

        int cantidadPasos =
                (MAX_MINUTOS - MIN_MINUTOS) / PASO_MINUTOS;

        seekTiempo.setMax(cantidadPasos);

        int progresoInicial =
                (minutosSeleccionados - MIN_MINUTOS)
                        / PASO_MINUTOS;

        seekTiempo.setProgress(progresoInicial);

        actualizarTextoSeleccion();

        seekTiempo.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        minutosSeleccionados =
                                MIN_MINUTOS
                                        + (progress * PASO_MINUTOS);

                        actualizarTextoSeleccion();

                        if (!temporizadorActivo && !pausado) {
                            actualizarTiempoInicial();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );
    }

    private void configurarBotones() {

        btnIniciar.setOnClickListener(v -> {

            if (!temporizadorActivo && !pausado) {
                iniciarNuevaSesion();
            }
        });

        btnPausar.setOnClickListener(v -> {

            if (temporizadorActivo) {
                pausar();
            } else if (pausado) {
                reanudar();
            }
        });

        btnReiniciar.setOnClickListener(v -> reiniciar());
    }

    private void iniciarNuevaSesion() {

        long usuarioId = sesionManager.getUsuarioId();

        if (usuarioId <= 0) {
            Toast.makeText(
                    this,
                    "No se encontró una sesión activa",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        ciclosCompletados = 0;
        enDescanso = false;

        sesionActual = SesionPomodoro.iniciar(
                usuarioId,
                SesionPomodoro.MODO_INDIVIDUAL,
                null,
                minutosSeleccionados,
                DESCANSO_MINUTOS
        );

        long id = repo.pomodoro().insertar(sesionActual);
        sesionActual.setId(id);

        tiempoRestanteMs =
                minutosSeleccionados * 60_000L;

        seekTiempo.setEnabled(false);

        tvModo.setText("Tiempo de enfoque");

        actualizarCiclos();

        iniciarTemporizador();
    }

    private void iniciarTemporizador() {

        cancelarTimerInterno();

        temporizadorActivo = true;
        pausado = false;

        btnIniciar.setEnabled(false);

        btnPausar.setEnabled(true);
        btnPausar.setText("Pausar");

        countDownTimer = new CountDownTimer(
                tiempoRestanteMs,
                1000
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                tiempoRestanteMs =
                        millisUntilFinished;

                actualizarReloj();
            }

            @Override
            public void onFinish() {

                tiempoRestanteMs = 0;

                actualizarReloj();

                temporizadorActivo = false;
                pausado = false;

                if (enDescanso) {
                    terminarDescanso();
                } else {
                    completarFoco();
                }
            }
        }.start();
    }

    private void pausar() {

        if (!temporizadorActivo) {
            return;
        }

        cancelarTimerInterno();

        temporizadorActivo = false;
        pausado = true;

        btnPausar.setText("Reanudar");

        tvModo.setText(
                enDescanso
                        ? "Descanso pausado"
                        : "Enfoque pausado"
        );
    }

    private void reanudar() {

        if (!pausado) {
            return;
        }

        tvModo.setText(
                enDescanso
                        ? "Tiempo de descanso"
                        : "Tiempo de enfoque"
        );

        iniciarTemporizador();
    }

    private void completarFoco() {

        ciclosCompletados++;

        if (sesionActual != null) {

            sesionActual.setCiclosCompletados(
                    ciclosCompletados
            );

            sesionActual.setMinutosEstudiados(
                    ciclosCompletados
                            * minutosSeleccionados
            );

            repo.pomodoro().actualizar(sesionActual);
        }

        actualizarCiclos();

        Toast.makeText(
                this,
                "¡Ciclo completado! Es hora de descansar.",
                Toast.LENGTH_LONG
        ).show();

        iniciarDescanso();
    }

    private void iniciarDescanso() {

        enDescanso = true;

        tiempoRestanteMs =
                DESCANSO_MINUTOS * 60_000L;

        tvModo.setText("Tiempo de descanso");

        iniciarTemporizador();
    }

    private void terminarDescanso() {

        enDescanso = false;

        Toast.makeText(
                this,
                "Descanso terminado. Puedes iniciar otro ciclo.",
                Toast.LENGTH_LONG
        ).show();

        tiempoRestanteMs =
                minutosSeleccionados * 60_000L;

        tvModo.setText("Listo para continuar");

        actualizarReloj();

        btnIniciar.setEnabled(true);
        btnIniciar.setText("Iniciar siguiente ciclo");

        btnPausar.setEnabled(false);

        // Permitimos iniciar otro ciclo de la misma sesión
        btnIniciar.setOnClickListener(v -> iniciarSiguienteCiclo());
    }

    private void iniciarSiguienteCiclo() {

        if (temporizadorActivo) {
            return;
        }

        enDescanso = false;

        tiempoRestanteMs =
                minutosSeleccionados * 60_000L;

        tvModo.setText("Tiempo de enfoque");

        btnIniciar.setEnabled(false);

        iniciarTemporizador();
    }

    private void reiniciar() {

        cancelarTimerInterno();

        // Si había una sesión, la cerramos con los ciclos
        // que realmente hayan sido completados.
        finalizarSesionActual();

        temporizadorActivo = false;
        pausado = false;
        enDescanso = false;

        ciclosCompletados = 0;

        seekTiempo.setEnabled(true);

        btnIniciar.setEnabled(true);
        btnIniciar.setText("Iniciar sesión");

        btnPausar.setEnabled(false);
        btnPausar.setText("Pausar");

        // Restauramos el listener original.
        btnIniciar.setOnClickListener(v -> {
            if (!temporizadorActivo && !pausado) {
                iniciarNuevaSesion();
            }
        });

        tvModo.setText("Tiempo de enfoque");

        actualizarTiempoInicial();
        actualizarCiclos();
    }

    private void finalizarSesionActual() {

        if (sesionActual == null) {
            return;
        }

        sesionActual.setCiclosCompletados(
                ciclosCompletados
        );

        sesionActual.finalizar();

        repo.pomodoro().actualizar(
                sesionActual
        );

        sesionActual = null;
    }

    private void actualizarTiempoInicial() {

        tiempoRestanteMs =
                minutosSeleccionados * 60_000L;

        actualizarReloj();
    }

    private void actualizarTextoSeleccion() {

        tvMinutosSeleccionados.setText(
                minutosSeleccionados
                        + " min de enfoque"
        );
    }

    private void actualizarCiclos() {

        tvCiclos.setText(
                String.valueOf(ciclosCompletados)
        );
    }

    private void actualizarReloj() {

        long segundosTotales =
                (tiempoRestanteMs + 999) / 1000;

        long minutos =
                segundosTotales / 60;

        long segundos =
                segundosTotales % 60;

        tvTiempo.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutos,
                        segundos
                )
        );
    }

    private void cancelarTimerInterno() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelarTimerInterno();
        super.onDestroy();
    }
}
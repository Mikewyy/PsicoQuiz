package com.utm.semiologia.ui.estudio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.ProgresoSeccion;
import com.utm.semiologia.ui.common.BaseActivity;

import java.util.Locale;

public class LectorActivity extends BaseActivity {

    private static final String EXTRA_SECCION_ID = "extra_seccion_id";

    // Todos los temas duran exactamente 5 minutos.
    private static final int MINUTOS_ESTUDIO = 3;

    private static final int PROGRESO_INICIAL = 10;
    private static final int PROGRESO_PARA_COMPLETAR = 95;

    private Repositorio repo;

    private long usuarioId;
    private long seccionId;

    private ProgresoSeccion seccion;

    private NestedScrollView scrollLector;

    private TextView tvTitulo;
    private TextView tvTema;
    private TextView tvDuracion;
    private TextView tvContenido;
    private TextView tvProgresoLectura;
    private TextView tvEstado;
    private TextView tvTemporizador;
    private TextView tvEstadoTiempo;

    private LinearProgressIndicator barLectura;

    private MaterialButton btnCompletar;

    private CountDownTimer countDownTimer;

    private long tiempoRestanteMs;

    private boolean tiempoCompletado = false;

    private int ultimoProgresoGuardado = -1;


    // =========================================================
    // INTENT
    // =========================================================

    public static Intent nuevoIntent(
            Context context,
            long seccionId
    ) {

        Intent intent =
                new Intent(
                        context,
                        LectorActivity.class
                );

        intent.putExtra(
                EXTRA_SECCION_ID,
                seccionId
        );

        return intent;
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_lector
        );

        repo =
                SemiologiaApp.getRepositorio();

        usuarioId =
                SemiologiaApp
                        .getSesion()
                        .getUsuarioId();

        seccionId =
                getIntent()
                        .getLongExtra(
                                EXTRA_SECCION_ID,
                                -1
                        );

        if (seccionId <= 0) {

            Toast.makeText(
                    this,
                    "No se pudo abrir este tema.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        enlazarVistas();

        configurarListeners();

        cargarSeccion();
    }


    // =========================================================
    // VISTAS
    // =========================================================

    private void enlazarVistas() {

        scrollLector =
                findViewById(
                        R.id.scrollLector
                );

        tvTitulo =
                findViewById(
                        R.id.tvTituloLector
                );

        tvTema =
                findViewById(
                        R.id.tvTemaLector
                );

        tvDuracion =
                findViewById(
                        R.id.tvDuracionLector
                );

        tvContenido =
                findViewById(
                        R.id.tvContenidoLector
                );

        tvProgresoLectura =
                findViewById(
                        R.id.tvProgresoLectura
                );

        tvEstado =
                findViewById(
                        R.id.tvEstadoLectura
                );

        tvTemporizador =
                findViewById(
                        R.id.tvTemporizador
                );

        tvEstadoTiempo =
                findViewById(
                        R.id.tvEstadoTiempo
                );

        barLectura =
                findViewById(
                        R.id.barLectura
                );

        btnCompletar =
                findViewById(
                        R.id.btnCompletarLectura
                );
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {

        findViewById(
                R.id.btnVolver
        ).setOnClickListener(
                v -> finish()
        );

        btnCompletar.setOnClickListener(
                v -> completarLectura()
        );

        scrollLector.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener)
                        (
                                v,
                                scrollX,
                                scrollY,
                                oldScrollX,
                                oldScrollY
                        ) -> calcularProgresoScroll()
        );
    }


    // =========================================================
    // CARGAR SECCIÓN
    // =========================================================

    private void cargarSeccion() {

        seccion =
                repo.estudio()
                        .obtenerSeccionConProgreso(
                                usuarioId,
                                seccionId
                        );

        if (seccion == null) {

            Toast.makeText(
                    this,
                    "El tema no está disponible.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        mostrarSeccion();

        registrarLecturaInicial();

        prepararTemporizador();
    }


    // =========================================================
    // MOSTRAR SECCIÓN
    // =========================================================

    private void mostrarSeccion() {

        tvTitulo.setText(
                valorSeguro(
                        seccion.getTitulo(),
                        "Tema de estudio"
                )
        );

        tvTema.setText(
                valorSeguro(
                        seccion.getTema(),
                        "Psicopatología"
                )
        );

        tvDuracion.setText(
                MINUTOS_ESTUDIO + " min de estudio"
        );

        mostrarContenidoHTML();

        actualizarEstadoVisual();
    }


    // =========================================================
    // CONTENIDO
    // =========================================================

    private void mostrarContenidoHTML() {

        String contenido =
                seccion.getContenido();

        if (
                contenido == null ||
                        contenido.trim().isEmpty()
        ) {

            tvContenido.setText(
                    "El contenido de este tema todavía no está disponible."
            );

            return;
        }

        Spanned texto =
                Html.fromHtml(
                        contenido,
                        Html.FROM_HTML_MODE_LEGACY
                );

        tvContenido.setText(
                texto
        );
    }


    // =========================================================
    // TEMPORIZADOR
    // =========================================================

    private void prepararTemporizador() {

        if (seccion.isCompletada()) {

            tiempoCompletado = true;

            tiempoRestanteMs = 0;

            tvTemporizador.setText(
                    "00:00"
            );

            tvEstadoTiempo.setText(
                    "Completado"
            );

            actualizarEstadoVisual();

            return;
        }

        tiempoRestanteMs =
                MINUTOS_ESTUDIO
                        * 60L
                        * 1000L;

        mostrarTiempo(
                tiempoRestanteMs
        );

        iniciarTemporizador();
    }


    private void iniciarTemporizador() {

        cancelarTemporizador();

        if (tiempoRestanteMs <= 0) {

            marcarTiempoCompletado();

            return;
        }

        tvEstadoTiempo.setText(
                "Estudiando"
        );

        countDownTimer =
                new CountDownTimer(
                        tiempoRestanteMs,
                        1000
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished
                    ) {

                        tiempoRestanteMs =
                                millisUntilFinished;

                        mostrarTiempo(
                                tiempoRestanteMs
                        );

                        actualizarEstadoVisual();
                    }

                    @Override
                    public void onFinish() {

                        tiempoRestanteMs = 0;

                        marcarTiempoCompletado();
                    }
                };

        countDownTimer.start();
    }


    private void marcarTiempoCompletado() {

        tiempoCompletado = true;

        tiempoRestanteMs = 0;

        tvTemporizador.setText(
                "00:00"
        );

        tvEstadoTiempo.setText(
                "Completado"
        );

        actualizarEstadoVisual();

        Toast.makeText(
                this,
                "Tiempo de estudio completado.",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void mostrarTiempo(
            long tiempoMs
    ) {

        long segundosTotales =
                Math.max(
                        0,
                        tiempoMs / 1000
                );

        long minutos =
                segundosTotales / 60;

        long segundos =
                segundosTotales % 60;

        String texto =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutos,
                        segundos
                );

        tvTemporizador.setText(
                texto
        );
    }


    private void cancelarTemporizador() {

        if (countDownTimer != null) {

            countDownTimer.cancel();

            countDownTimer = null;
        }
    }


    // =========================================================
    // PROGRESO INICIAL
    // =========================================================

    private void registrarLecturaInicial() {

        if (seccion.isCompletada()) {

            ultimoProgresoGuardado = 100;

            return;
        }

        int lecturaActual =
                seccion.getLectura();

        if (lecturaActual < PROGRESO_INICIAL) {

            guardarProgreso(
                    PROGRESO_INICIAL
            );

        } else {

            ultimoProgresoGuardado =
                    lecturaActual;
        }

        actualizarEstadoVisual();
    }


    // =========================================================
    // PROGRESO POR SCROLL
    // =========================================================

    private void calcularProgresoScroll() {

        if (
                seccion == null ||
                        seccion.isCompletada()
        ) {
            return;
        }

        View contenidoScroll =
                scrollLector.getChildAt(0);

        if (contenidoScroll == null) {
            return;
        }

        int alturaContenido =
                contenidoScroll.getHeight();

        int alturaVisible =
                scrollLector.getHeight();

        int desplazamiento =
                scrollLector.getScrollY();

        int desplazamientoMaximo =
                alturaContenido - alturaVisible;

        if (desplazamientoMaximo <= 0) {
            return;
        }

        float proporcion =
                desplazamiento
                        / (float) desplazamientoMaximo;

        proporcion =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                proporcion
                        )
                );

        int progreso =
                PROGRESO_INICIAL
                        +
                        Math.round(
                                proporcion
                                        *
                                        (100 - PROGRESO_INICIAL)
                        );

        progreso =
                Math.max(
                        PROGRESO_INICIAL,
                        Math.min(
                                100,
                                progreso
                        )
                );

        if (progreso < seccion.getLectura()) {

            progreso =
                    seccion.getLectura();
        }

        if (progreso > seccion.getLectura()) {

            seccion.setLectura(
                    progreso
            );

            actualizarEstadoVisual();
        }

        if (
                ultimoProgresoGuardado < 0 ||
                        progreso >= ultimoProgresoGuardado + 5 ||
                        progreso >= PROGRESO_PARA_COMPLETAR
        ) {

            guardarProgreso(
                    progreso
            );
        }
    }


    // =========================================================
    // GUARDAR PROGRESO
    // =========================================================

    private void guardarProgreso(
            int progreso
    ) {

        if (seccion == null) {
            return;
        }

        progreso =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progreso
                        )
                );

        if (progreso < seccion.getLectura()) {

            progreso =
                    seccion.getLectura();
        }

        repo.estudio()
                .actualizarLectura(
                        usuarioId,
                        seccionId,
                        progreso
                );

        seccion.setLectura(
                progreso
        );

        ultimoProgresoGuardado =
                progreso;

        actualizarEstadoVisual();
    }


    // =========================================================
    // COMPLETAR
    // =========================================================

    private void completarLectura() {

        if (seccion == null) {
            return;
        }

        if (seccion.isCompletada()) {

            Toast.makeText(
                    this,
                    "Este tema ya está completado.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!tiempoCompletado) {

            Toast.makeText(
                    this,
                    "Completa primero los 5 minutos de estudio.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                seccion.getLectura()
                        < PROGRESO_PARA_COMPLETAR
        ) {

            Toast.makeText(
                    this,
                    "Continúa leyendo hasta llegar al final.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        seccion.completar(
                seccion.getPuntosRecompensa(),
                0
        );

        /*
         * EstudioDao administra la recompensa.
         * No entregamos comida directamente aquí
         * para evitar duplicados.
         */
        repo.estudio()
                .actualizarProgreso(
                        seccion,
                        repo.mascotas(),
                        usuarioId
                );

        ultimoProgresoGuardado = 100;

        actualizarEstadoVisual();

        Toast.makeText(
                this,
                "Tema completado. Recompensa conseguida.",
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // ESTADO VISUAL
    // =========================================================

    private void actualizarEstadoVisual() {

        if (seccion == null) {
            return;
        }

        int progreso =
                Math.max(
                        0,
                        Math.min(
                                100,
                                seccion.getLectura()
                        )
                );

        barLectura.setProgress(
                progreso
        );

        tvProgresoLectura.setText(
                progreso + "%"
        );


        // YA COMPLETADO

        if (seccion.isCompletada()) {

            tvEstado.setText(
                    "Tema completado"
            );

            btnCompletar.setText(
                    "Tema completado"
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.65f
            );

            return;
        }


        // FALTA TIEMPO

        if (!tiempoCompletado) {

            tvEstado.setText(
                    "Estudiando el tema"
            );

            btnCompletar.setText(
                    "Continúa estudiando · "
                            + formatearTiempoBoton()
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.60f
            );

            return;
        }


        // TIEMPO LISTO, PERO FALTA LECTURA

        if (
                progreso
                        < PROGRESO_PARA_COMPLETAR
        ) {

            tvEstado.setText(
                    "Tiempo completado · continúa leyendo"
            );

            btnCompletar.setText(
                    "Llega al final de la lectura"
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.60f
            );

            return;
        }


        // TODO LISTO

        tvEstado.setText(
                "Listo para completar"
        );

        btnCompletar.setText(
                "Completar tema"
        );

        btnCompletar.setEnabled(
                true
        );

        btnCompletar.setAlpha(
                1f
        );
    }


    private String formatearTiempoBoton() {

        long segundosTotales =
                Math.max(
                        0,
                        tiempoRestanteMs / 1000
                );

        long minutos =
                segundosTotales / 60;

        long segundos =
                segundosTotales % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
        );
    }


    // =========================================================
    // PAUSA
    // =========================================================

    @Override
    protected void onPause() {

        cancelarTemporizador();

        if (
                seccion != null &&
                        !seccion.isCompletada()
        ) {

            int progreso =
                    seccion.getLectura();

            if (
                    progreso
                            > ultimoProgresoGuardado
            ) {

                repo.estudio()
                        .actualizarLectura(
                                usuarioId,
                                seccionId,
                                progreso
                        );

                ultimoProgresoGuardado =
                        progreso;
            }
        }

        super.onPause();
    }


    // =========================================================
    // REANUDAR
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (
                seccion != null &&
                        !seccion.isCompletada() &&
                        !tiempoCompletado &&
                        tiempoRestanteMs > 0 &&
                        countDownTimer == null
        ) {

            iniciarTemporizador();
        }
    }


    // =========================================================
    // DESTRUIR
    // =========================================================

    @Override
    protected void onDestroy() {

        cancelarTemporizador();

        super.onDestroy();
    }


    // =========================================================
    // UTILIDAD
    // =========================================================

    private String valorSeguro(
            String valor,
            String defecto
    ) {

        if (
                valor == null ||
                        valor.trim().isEmpty()
        ) {

            return defecto;
        }

        return valor;
    }
}
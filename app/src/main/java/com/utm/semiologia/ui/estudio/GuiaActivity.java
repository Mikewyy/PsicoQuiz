package com.utm.semiologia.ui.estudio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.dao.EstudioDao;
import com.utm.semiologia.ui.common.BaseActivity;

/**
 * Pantalla principal de la Guía de Estudio.
 *
 * Desde aquí el usuario puede elegir entre:
 *
 * 1. Síntomas y funciones psicológicas.
 * 2. Síndromes psicopatológicos.
 */
public class GuiaActivity extends BaseActivity {

    private Repositorio repo;
    private long usuarioId;

    // ---------------------------------------------------------
    // Progreso general
    // ---------------------------------------------------------

    private LinearProgressIndicator barProgresoGeneral;
    private TextView tvProgresoGeneral;
    private TextView tvResumenProgreso;

    // ---------------------------------------------------------
    // Camino de síntomas
    // ---------------------------------------------------------

    private LinearProgressIndicator barProgresoSintomas;
    private TextView tvProgresoSintomas;
    private TextView tvResumenSintomas;

    // ---------------------------------------------------------
    // Camino de síndromes
    // ---------------------------------------------------------

    private LinearProgressIndicator barProgresoSindromes;
    private TextView tvProgresoSindromes;
    private TextView tvResumenSindromes;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_guia);

        repo = SemiologiaApp.getRepositorio();
        usuarioId = SemiologiaApp.getSesion().getUsuarioId();

        enlazarVistas();
        configurarListeners();
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Se vuelve a calcular cada vez que regresamos
         * de una ruta o del lector.
         */
        cargarProgreso();
    }


    // =========================================================
    // VISTAS
    // =========================================================

    private void enlazarVistas() {

        // Progreso general

        barProgresoGeneral =
                findViewById(R.id.barProgresoGeneral);

        tvProgresoGeneral =
                findViewById(R.id.tvProgresoGeneral);

        tvResumenProgreso =
                findViewById(R.id.tvResumenProgreso);


        // Camino síntomas

        barProgresoSintomas =
                findViewById(R.id.barProgresoSintomas);

        tvProgresoSintomas =
                findViewById(R.id.tvProgresoSintomas);

        tvResumenSintomas =
                findViewById(R.id.tvResumenSintomas);


        // Camino síndromes

        barProgresoSindromes =
                findViewById(R.id.barProgresoSindromes);

        tvProgresoSindromes =
                findViewById(R.id.tvProgresoSindromes);

        tvResumenSindromes =
                findViewById(R.id.tvResumenSindromes);
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {

        findViewById(R.id.btnVolver)
                .setOnClickListener(
                        v -> finish()
                );


        /*
         * CAMINO 1
         *
         * Síntomas y funciones psicológicas.
         */
        findViewById(R.id.cardSintomas)
                .setOnClickListener(
                        v -> abrirCamino(
                                EstudioDao.CAMINO_SINTOMAS
                        )
                );


        /*
         * CAMINO 2
         *
         * Síndromes psicopatológicos.
         */
        findViewById(R.id.cardSindromes)
                .setOnClickListener(
                        v -> abrirCamino(
                                EstudioDao.CAMINO_SINDROMES
                        )
                );
    }


    // =========================================================
    // PROGRESO
    // =========================================================

    private void cargarProgreso() {

        cargarProgresoSintomas();
        cargarProgresoSindromes();
        cargarProgresoGeneral();
    }


    /**
     * Actualiza el progreso del camino largo:
     * Síntomas y funciones psicológicas.
     */
    private void cargarProgresoSintomas() {

        int total =
                repo.estudio()
                        .seccionesTotales(
                                EstudioDao.CAMINO_SINTOMAS
                        );

        int completadas =
                repo.estudio()
                        .seccionesCompletadas(
                                usuarioId,
                                EstudioDao.CAMINO_SINTOMAS
                        );

        int progreso =
                repo.estudio()
                        .progresoCamino(
                                usuarioId,
                                EstudioDao.CAMINO_SINTOMAS
                        );


        barProgresoSintomas.setProgress(
                progreso
        );

        tvProgresoSintomas.setText(
                progreso + "%"
        );

        tvResumenSintomas.setText(
                completadas
                        + " de "
                        + total
                        + " temas completados"
        );
    }


    /**
     * Actualiza el progreso del camino corto:
     * Síndromes psicopatológicos.
     */
    private void cargarProgresoSindromes() {

        int total =
                repo.estudio()
                        .seccionesTotales(
                                EstudioDao.CAMINO_SINDROMES
                        );

        int completadas =
                repo.estudio()
                        .seccionesCompletadas(
                                usuarioId,
                                EstudioDao.CAMINO_SINDROMES
                        );

        int progreso =
                repo.estudio()
                        .progresoCamino(
                                usuarioId,
                                EstudioDao.CAMINO_SINDROMES
                        );


        barProgresoSindromes.setProgress(
                progreso
        );

        tvProgresoSindromes.setText(
                progreso + "%"
        );

        tvResumenSindromes.setText(
                completadas
                        + " de "
                        + total
                        + " temas completados"
        );
    }


    /**
     * Calcula el progreso de toda la guía.
     *
     * No hacemos simplemente:
     *
     * (progresoSintomas + progresoSindromes) / 2
     *
     * porque Síntomas tiene más secciones.
     *
     * Se calcula usando el total real de temas.
     */
    private void cargarProgresoGeneral() {

        int totalSintomas =
                repo.estudio()
                        .seccionesTotales(
                                EstudioDao.CAMINO_SINTOMAS
                        );

        int totalSindromes =
                repo.estudio()
                        .seccionesTotales(
                                EstudioDao.CAMINO_SINDROMES
                        );

        int completadasSintomas =
                repo.estudio()
                        .seccionesCompletadas(
                                usuarioId,
                                EstudioDao.CAMINO_SINTOMAS
                        );

        int completadasSindromes =
                repo.estudio()
                        .seccionesCompletadas(
                                usuarioId,
                                EstudioDao.CAMINO_SINDROMES
                        );

        int progresoSintomas =
                repo.estudio()
                        .progresoCamino(
                                usuarioId,
                                EstudioDao.CAMINO_SINTOMAS
                        );

        int progresoSindromes =
                repo.estudio()
                        .progresoCamino(
                                usuarioId,
                                EstudioDao.CAMINO_SINDROMES
                        );


        int total =
                totalSintomas
                        + totalSindromes;

        int completadas =
                completadasSintomas
                        + completadasSindromes;


        int porcentajeGeneral = 0;

        if (total > 0) {

            /*
             * Promedio ponderado.
             *
             * Síntomas tiene mayor peso porque
             * contiene más secciones.
             */
            porcentajeGeneral =
                    (
                            progresoSintomas
                                    * totalSintomas

                                    +

                                    progresoSindromes
                                            * totalSindromes
                    )
                            / total;
        }


        barProgresoGeneral.setProgress(
                porcentajeGeneral
        );

        tvProgresoGeneral.setText(
                porcentajeGeneral + "%"
        );

        tvResumenProgreso.setText(
                completadas
                        + " de "
                        + total
                        + " temas completados"
        );
    }


    // =========================================================
    // NAVEGACIÓN
    // =========================================================

    /**
     * Abre la pantalla que contiene las secciones
     * del camino seleccionado.
     */
    private void abrirCamino(
            String camino
    ) {

        Intent intent =
                new Intent(
                        this,
                        CaminoEstudioActivity.class
                );

        intent.putExtra(
                CaminoEstudioActivity.EXTRA_CAMINO,
                camino
        );

        startActivity(intent);
    }
}
package com.utm.semiologia.ui.estudio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.dao.EstudioDao;
import com.utm.semiologia.data.model.ProgresoSeccion;
import com.utm.semiologia.ui.common.BaseActivity;
import com.utm.semiologia.ui.common.NavegacionInferior;

import java.util.ArrayList;
import java.util.List;

/**
 * Muestra las secciones pertenecientes a un camino
 * concreto de la Guía de Estudio.
 *
 * Caminos disponibles:
 *
 * - sintomas
 * - sindromes
 */
public class CaminoEstudioActivity extends BaseActivity
        implements SeccionAdapter.OnSeccionClickListener {

    public static final String EXTRA_CAMINO = "extra_camino";

    private RecyclerView recyclerSecciones;

    private LinearProgressIndicator barProgresoCamino;

    private TextView tvTituloCamino;
    private TextView tvDescripcionCamino;
    private TextView tvProgresoCamino;
    private TextView tvResumenCamino;
    private View tvSinSecciones;

    private SeccionAdapter adapter;

    private Repositorio repo;

    private long usuarioId;

    private String camino;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camino_estudio);

        repo = SemiologiaApp.getRepositorio();
        usuarioId = SemiologiaApp.getSesion().getUsuarioId();

        /*
         * Recuperamos el camino enviado desde GuiaActivity.
         */
        camino = getIntent().getStringExtra(EXTRA_CAMINO);

        /*
         * Si alguien intenta abrir esta Activity sin indicar
         * un camino válido, simplemente la cerramos.
         */
        if (!esCaminoValido(camino)) {

            android.widget.Toast.makeText(
                    this,
                    "Camino recibido: " + camino,
                    android.widget.Toast.LENGTH_LONG
            ).show();

            return;
        }

        enlazarVistas();
        configurarRecycler();
        configurarListeners();
        configurarCabecera();

        // Se configura al final: si el camino no es valido la pantalla se
        // cierra antes, asi que la barra no llega a verse en ese caso.
        configurarNavInferior(
                NavegacionInferior.SECCION_EXPLORAR
        );
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Se vuelve a cargar al regresar del lector
         * para actualizar inmediatamente el progreso.
         */
        cargarSecciones();
    }


    // =========================================================
    // VISTAS
    // =========================================================

    private void enlazarVistas() {

        recyclerSecciones =
                findViewById(R.id.recyclerSecciones);

        barProgresoCamino =
                findViewById(R.id.barProgresoCamino);

        tvTituloCamino =
                findViewById(R.id.tvTituloCamino);

        tvDescripcionCamino =
                findViewById(R.id.tvDescripcionCamino);

        tvProgresoCamino =
                findViewById(R.id.tvProgresoCamino);

        tvResumenCamino =
                findViewById(R.id.tvResumenCamino);

        tvSinSecciones =
                findViewById(R.id.tvSinSecciones);
    }


    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private void configurarRecycler() {

        adapter =
                new SeccionAdapter(this);

        recyclerSecciones.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerSecciones.setAdapter(
                adapter
        );

        recyclerSecciones.setHasFixedSize(
                false
        );
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {

        findViewById(R.id.btnVolver)
                .setOnClickListener(
                        v -> finish()
                );
    }


    // =========================================================
    // CABECERA
    // =========================================================

    private void configurarCabecera() {

        if (EstudioDao.CAMINO_SINTOMAS.equals(camino)) {

            tvTituloCamino.setText(
                    "Síntomas y funciones"
            );

            tvDescripcionCamino.setText(
                    "Semiología de los procesos y funciones psicológicas"
            );

        } else {

            tvTituloCamino.setText(
                    "Síndromes"
            );

            tvDescripcionCamino.setText(
                    "Principales síndromes psicopatológicos"
            );
        }
    }


    // =========================================================
    // CARGAR SECCIONES
    // =========================================================

    private void cargarSecciones() {

        List<ProgresoSeccion> secciones =
                repo.estudio()
                        .listarSeccionesConProgreso(
                                usuarioId,
                                camino
                        );

        if (secciones == null) {

            secciones =
                    new ArrayList<>();
        }


        adapter.setSecciones(
                secciones
        );


        boolean vacio =
                secciones.isEmpty();


        tvSinSecciones.setVisibility(
                vacio
                        ? View.VISIBLE
                        : View.GONE
        );


        recyclerSecciones.setVisibility(
                vacio
                        ? View.GONE
                        : View.VISIBLE
        );


        actualizarProgreso(
                secciones
        );
    }


    // =========================================================
    // PROGRESO
    // =========================================================

    private void actualizarProgreso(
            List<ProgresoSeccion> secciones
    ) {

        int total =
                secciones.size();


        if (total == 0) {

            barProgresoCamino.setProgress(
                    0
            );

            tvProgresoCamino.setText(
                    "0%"
            );

            tvResumenCamino.setText(
                    "No hay temas disponibles"
            );

            return;
        }


        int completadas = 0;

        int sumaLectura = 0;


        for (ProgresoSeccion seccion : secciones) {

            sumaLectura +=
                    seccion.getLectura();


            if (seccion.isCompletada()) {

                completadas++;
            }
        }


        /*
         * Ejemplo:
         *
         * Tema 1 = 100%
         * Tema 2 = 50%
         * Tema 3 = 0%
         *
         * Progreso:
         *
         * (100 + 50 + 0) / 3 = 50%
         */
        int porcentaje =
                sumaLectura / total;


        barProgresoCamino.setProgress(
                porcentaje
        );


        tvProgresoCamino.setText(
                porcentaje + "%"
        );


        tvResumenCamino.setText(
                completadas
                        + " de "
                        + total
                        + " temas completados"
        );
    }


    // =========================================================
    // CLICK EN UNA SECCIÓN
    // =========================================================

    @Override
    public void onSeccionClick(
            ProgresoSeccion seccion
    ) {

        if (seccion == null) {
            return;
        }


        startActivity(
                LectorActivity.nuevoIntent(
                        this,
                        seccion.getSeccionId()
                )
        );
    }


    // =========================================================
    // VALIDACIÓN
    // =========================================================

    private boolean esCaminoValido(
            String camino
    ) {

        return EstudioDao.CAMINO_SINTOMAS.equals(
                camino
        )
                ||
                EstudioDao.CAMINO_SINDROMES.equals(
                        camino
                );
    }
}
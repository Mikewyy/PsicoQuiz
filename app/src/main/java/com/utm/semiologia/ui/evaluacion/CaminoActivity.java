package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utm.semiologia.R;
import com.utm.semiologia.data.model.Nivel;
import com.utm.semiologia.data.model.ProgresoNivel;
import com.utm.semiologia.ui.common.BaseActivity;

/**
 * Pantalla de casos clínicos.
 *
 * Permite cambiar entre:
 * - Síntomas
 * - Síndromes
 */
public class CaminoActivity extends BaseActivity
        implements TramoAdapter.OnTramoClick {

    private static final String EXTRA_CATEGORIA = "categoria";

    private CaminoViewModel vm;
    private TramoAdapter adapter;

    private TextView tvPuntos;
    private TextView tvTitulo;

    private Button btnSintomas;
    private Button btnSindromes;

    private RecyclerView recyclerTramos;

    private String categoria = Nivel.CAT_SINTOMAS;


    // =========================================================
    // INTENT
    // =========================================================

    public static Intent nuevoIntent(
            Context ctx,
            String categoria
    ) {

        Intent intent =
                new Intent(
                        ctx,
                        CaminoActivity.class
                );

        intent.putExtra(
                EXTRA_CATEGORIA,
                categoria
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
                R.layout.activity_camino
        );


        // -----------------------------------------------------
        // CATEGORÍA INICIAL
        // -----------------------------------------------------

        String extra =
                getIntent()
                        .getStringExtra(
                                EXTRA_CATEGORIA
                        );

        if (
                Nivel.CAT_SINDROMES.equals(
                        extra
                )
        ) {

            categoria =
                    Nivel.CAT_SINDROMES;

        } else {

            categoria =
                    Nivel.CAT_SINTOMAS;
        }


        // -----------------------------------------------------
        // ENLAZAR VISTAS
        // -----------------------------------------------------

        tvTitulo =
                findViewById(
                        R.id.tvTituloCamino
                );


        tvPuntos =
                findViewById(
                        R.id.tvPuntos
                );


        btnSintomas =
                findViewById(
                        R.id.btnCaminoSintomas
                );


        btnSindromes =
                findViewById(
                        R.id.btnCaminoSindromes
                );


        recyclerTramos =
                findViewById(
                        R.id.recyclerTramos
                );


        findViewById(
                R.id.btnVolver
        ).setOnClickListener(

                v -> finish()

        );


        // -----------------------------------------------------
        // VIEW MODEL
        // -----------------------------------------------------

        vm =
                new ViewModelProvider(
                        this
                )
                        .get(
                                CaminoViewModel.class
                        );


        // -----------------------------------------------------
        // RECYCLER
        // -----------------------------------------------------

        adapter =
                new TramoAdapter(
                        this
                );


        recyclerTramos.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );


        recyclerTramos.setAdapter(
                adapter
        );


        // -----------------------------------------------------
        // BOTÓN SÍNTOMAS
        // -----------------------------------------------------

        btnSintomas.setOnClickListener(

                v -> cambiarCategoria(
                        Nivel.CAT_SINTOMAS
                )

        );


        // -----------------------------------------------------
        // BOTÓN SÍNDROMES
        // -----------------------------------------------------

        btnSindromes.setOnClickListener(

                v -> cambiarCategoria(
                        Nivel.CAT_SINDROMES
                )

        );


        // -----------------------------------------------------
        // OBSERVAR ESTADO
        // -----------------------------------------------------

        vm.getEstado()
                .observe(
                        this,
                        estado -> {

                            if (
                                    estado == null
                            ) {

                                return;
                            }


                            tvPuntos.setText(

                                    getString(
                                            R.string.camino_puntos,
                                            estado.puntos
                                    )

                            );


                            adapter.setTramos(
                                    estado.tramos
                            );
                        }
                );


        actualizarInterfazCategoria();
    }


    // =========================================================
    // CAMBIAR CAMINO
    // =========================================================

    private void cambiarCategoria(
            String nuevaCategoria
    ) {

        if (
                nuevaCategoria == null
        ) {

            return;
        }


        if (
                nuevaCategoria.equals(
                        categoria
                )
        ) {

            return;
        }


        categoria =
                nuevaCategoria;


        actualizarInterfazCategoria();


        vm.cargar(
                categoria
        );
    }


    // =========================================================
    // ACTUALIZAR INTERFAZ
    // =========================================================

    private void actualizarInterfazCategoria() {

        boolean esSindromes =
                Nivel.CAT_SINDROMES.equals(
                        categoria
                );


        // -----------------------------------------------------
        // TÍTULO
        // -----------------------------------------------------

        tvTitulo.setText(

                esSindromes
                        ? "Casos clínicos · Síndromes"
                        : "Casos clínicos · Síntomas"

        );


        // -----------------------------------------------------
        // SÍNTOMAS SELECCIONADO
        // -----------------------------------------------------

        if (!esSindromes) {

            btnSintomas.setBackgroundTintList(

                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#6C5CE7"
                            )
                    )

            );


            btnSintomas.setTextColor(
                    Color.WHITE
            );


            btnSindromes.setBackgroundTintList(

                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#ECE9FA"
                            )
                    )

            );


            btnSindromes.setTextColor(

                    Color.parseColor(
                            "#665F7A"
                    )

            );

        }

        // -----------------------------------------------------
        // SÍNDROMES SELECCIONADO
        // -----------------------------------------------------

        else {

            btnSindromes.setBackgroundTintList(

                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#6C5CE7"
                            )
                    )

            );


            btnSindromes.setTextColor(
                    Color.WHITE
            );


            btnSintomas.setBackgroundTintList(

                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#ECE9FA"
                            )
                    )

            );


            btnSintomas.setTextColor(

                    Color.parseColor(
                            "#665F7A"
                    )

            );
        }


        btnSintomas.setEnabled(
                true
        );


        btnSindromes.setEnabled(
                true
        );


        btnSintomas.setAlpha(
                1f
        );


        btnSindromes.setAlpha(
                1f
        );
    }


    // =========================================================
    // RECARGAR
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (
                vm != null
        ) {

            vm.cargar(
                    categoria
            );
        }
    }


    // =========================================================
    // CLICK EN NIVEL
    // =========================================================

    @Override
    public void onTramo(
            ProgresoNivel tramo
    ) {

        if (
                tramo == null
        ) {

            return;
        }


        if (
                tramo.isBloqueado()
        ) {

            Toast.makeText(
                    this,
                    R.string.tramo_desbloqueado_msg,
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        startActivity(

                QuizActivity.nuevoIntent(
                        this,
                        tramo
                )

        );
    }
}
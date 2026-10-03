package com.utm.semiologia.ui.estudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utm.semiologia.R;
import com.utm.semiologia.data.model.ProgresoSeccion;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de las secciones de la Guía de Estudio.
 *
 * Cada tarjeta muestra:
 * - número de sección;
 * - título;
 * - tema;
 * - duración aproximada;
 * - recompensa;
 * - progreso de lectura;
 * - estado completado.
 */
public class SeccionAdapter
        extends RecyclerView.Adapter<SeccionAdapter.SeccionViewHolder> {

    private final List<ProgresoSeccion> secciones =
            new ArrayList<>();

    private final OnSeccionClickListener listener;


    // =========================================================
    // LISTENER
    // =========================================================

    public interface OnSeccionClickListener {

        void onSeccionClick(ProgresoSeccion seccion);
    }


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SeccionAdapter(OnSeccionClickListener listener) {

        this.listener = listener;
    }


    // =========================================================
    // ACTUALIZAR LISTA
    // =========================================================

    public void setSecciones(List<ProgresoSeccion> nuevasSecciones) {

        secciones.clear();

        if (nuevasSecciones != null) {
            secciones.addAll(nuevasSecciones);
        }

        notifyDataSetChanged();
    }


    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

    @NonNull
    @Override
    public SeccionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_seccion,
                        parent,
                        false
                );

        return new SeccionViewHolder(view);
    }


    // =========================================================
    // MOSTRAR INFORMACIÓN
    // =========================================================

    @Override
    public void onBindViewHolder(
            @NonNull SeccionViewHolder holder,
            int position
    ) {

        ProgresoSeccion seccion =
                secciones.get(position);

        holder.bind(
                seccion,
                position
        );
    }


    // =========================================================
    // CANTIDAD
    // =========================================================

    @Override
    public int getItemCount() {

        return secciones.size();
    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    class SeccionViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView tvNumeroSeccion;
        private final TextView tvTituloSeccion;
        private final TextView tvTemaSeccion;

        private final TextView tvDuracion;
        private final TextView tvRecompensa;

        private final TextView tvEstadoSeccion;
        private final TextView tvPorcentajeSeccion;

        private final ProgressBar barProgresoSeccion;

        private final View indicadorCompletado;


        public SeccionViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            tvNumeroSeccion =
                    itemView.findViewById(
                            R.id.tvNumeroSeccion
                    );

            tvTituloSeccion =
                    itemView.findViewById(
                            R.id.tvTituloSeccion
                    );

            tvTemaSeccion =
                    itemView.findViewById(
                            R.id.tvTemaSeccion
                    );

            tvDuracion =
                    itemView.findViewById(
                            R.id.tvDuracion
                    );

            tvRecompensa =
                    itemView.findViewById(
                            R.id.tvRecompensa
                    );

            tvEstadoSeccion =
                    itemView.findViewById(
                            R.id.tvEstadoSeccion
                    );

            tvPorcentajeSeccion =
                    itemView.findViewById(
                            R.id.tvPorcentajeSeccion
                    );

            barProgresoSeccion =
                    itemView.findViewById(
                            R.id.barProgresoSeccion
                    );

            indicadorCompletado =
                    itemView.findViewById(
                            R.id.indicadorCompletado
                    );
        }


        // =====================================================
        // PINTAR TARJETA
        // =====================================================

        void bind(
                ProgresoSeccion seccion,
                int position
        ) {

            /*
             * Número visual de sección.
             *
             * Ejemplo:
             * 01
             * 02
             * 03
             */
            int numero = position + 1;

            tvNumeroSeccion.setText(
                    String.format(
                            java.util.Locale.getDefault(),
                            "%02d",
                            numero
                    )
            );


            // -------------------------------------------------
            // TÍTULO
            // -------------------------------------------------

            String titulo =
                    seccion.getTitulo();

            if (titulo == null ||
                    titulo.trim().isEmpty()) {

                titulo = "Sección " + numero;
            }

            tvTituloSeccion.setText(titulo);


            // -------------------------------------------------
            // TEMA
            // -------------------------------------------------

            String tema =
                    seccion.getTema();

            if (tema == null ||
                    tema.trim().isEmpty()) {

                tvTemaSeccion.setVisibility(
                        View.GONE
                );

            } else {

                tvTemaSeccion.setVisibility(
                        View.VISIBLE
                );

                tvTemaSeccion.setText(tema);
            }


            // -------------------------------------------------
            // DURACIÓN
            // -------------------------------------------------

            int duracion =
                    seccion.getDuracionEstimadaMin();

            if (duracion > 0) {

                tvDuracion.setText(
                        duracion + " min"
                );

                tvDuracion.setVisibility(
                        View.VISIBLE
                );

            } else {

                tvDuracion.setVisibility(
                        View.GONE
                );
            }


            // -------------------------------------------------
            // RECOMPENSA
            // -------------------------------------------------

            int puntos =
                    seccion.getPuntosRecompensa();

            if (puntos > 0) {

                tvRecompensa.setText(
                        "+" + puntos + " pts"
                );

                tvRecompensa.setVisibility(
                        View.VISIBLE
                );

            } else {

                tvRecompensa.setVisibility(
                        View.GONE
                );
            }


            // -------------------------------------------------
            // PROGRESO
            // -------------------------------------------------

            int progreso =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    seccion.getLectura()
                            )
                    );

            barProgresoSeccion.setMax(100);
            barProgresoSeccion.setProgress(progreso);


            // -------------------------------------------------
            // COMPLETADA
            // -------------------------------------------------

            if (seccion.isCompletada()) {

                tvEstadoSeccion.setText(
                        "Completada"
                );

                tvPorcentajeSeccion.setText(
                        "100%"
                );

                indicadorCompletado.setVisibility(
                        View.VISIBLE
                );

            } else {

                indicadorCompletado.setVisibility(
                        View.GONE
                );


                if (progreso > 0) {

                    tvEstadoSeccion.setText(
                            "En progreso"
                    );

                    tvPorcentajeSeccion.setText(
                            progreso + "%"
                    );

                } else {

                    tvEstadoSeccion.setText(
                            "Sin comenzar"
                    );

                    tvPorcentajeSeccion.setText(
                            "0%"
                    );
                }
            }


            // -------------------------------------------------
            // CLICK
            // -------------------------------------------------

            itemView.setOnClickListener(v -> {

                int adapterPosition =
                        getBindingAdapterPosition();

                if (adapterPosition ==
                        RecyclerView.NO_POSITION) {

                    return;
                }

                if (listener != null) {

                    listener.onSeccionClick(
                            secciones.get(
                                    adapterPosition
                            )
                    );
                }
            });
        }
    }
}
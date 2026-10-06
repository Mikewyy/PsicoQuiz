package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utm.semiologia.R;
import com.utm.semiologia.data.model.ProgresoNivel;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja los niveles del camino de aprendizaje.
 *
 * Todos los niveles se muestran centrados y alineados
 * verticalmente para evitar que las tarjetas se recorten.
 */
public class TramoAdapter
        extends RecyclerView.Adapter<TramoAdapter.VH> {

    public interface OnTramoClick {
        void onTramo(ProgresoNivel tramo);
    }

    private final List<ProgresoNivel> tramos =
            new ArrayList<>();

    private final OnTramoClick listener;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TramoAdapter(
            OnTramoClick listener
    ) {

        this.listener =
                listener;
    }


    // =========================================================
    // ACTUALIZAR LISTA
    // =========================================================

    public void setTramos(
            List<ProgresoNivel> nuevos
    ) {

        tramos.clear();

        if (
                nuevos != null
        ) {

            tramos.addAll(
                    nuevos
            );
        }

        notifyDataSetChanged();
    }


    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_tramo,
                                parent,
                                false
                        );

        return new VH(
                view
        );
    }


    // =========================================================
    // PINTAR NIVEL
    // =========================================================

    @Override
    public void onBindViewHolder(
            @NonNull VH h,
            int position
    ) {

        ProgresoNivel tramo =
                tramos.get(
                        position
                );

        Context ctx =
                h.itemView
                        .getContext();


        // -----------------------------------------------------
        // DATOS
        // -----------------------------------------------------

        h.tvEmoji.setText(
                tramo.getEmoji()
        );


        h.tvNombre.setText(
                tramo.getNombre()
        );


        h.tvTema.setText(
                tramo.getTema()
        );


        h.tvEstrellas.setText(
                estrellas(
                        tramo.estrellas()
                )
        );


        h.tvContenido.setText(

                ctx.getString(
                        R.string.tramo_contenido,
                        tramo.getTotalPreguntas()
                )

        );


        // =====================================================
        // POSICIÓN DEL NIVEL
        // =====================================================

        /*
         * Antes existía un zigzag:
         *
         * +70dp
         * -70dp
         *
         * Eso hacía que las tarjetas nuevas se salieran
         * de la pantalla.
         *
         * Ahora todos los nodos quedan centrados.
         */

        h.contenedorNodo.setTranslationX(
                0f
        );


        // -----------------------------------------------------
        // CONECTOR
        // -----------------------------------------------------

        if (
                position == 0
        ) {

            h.ivConector.setVisibility(
                    View.GONE
            );

        } else {

            h.ivConector.setVisibility(
                    View.VISIBLE
            );

            /*
             * El ImageView funciona simplemente como
             * línea vertical, sin imagen.
             */

            h.ivConector.setImageDrawable(
                    null
            );

            h.ivConector.setBackgroundColor(

                    android.graphics.Color.parseColor(
                            "#B8AEF6"
                    )

            );
        }


        // =====================================================
        // ESTADO DEL NIVEL
        // =====================================================

        if (
                tramo.isBloqueado()
        ) {

            // -------------------------------------------------
            // BLOQUEADO
            // -------------------------------------------------

            h.tvEstado.setText(
                    R.string.tramo_desbloqueado_msg
            );


            h.btn.setText(
                    R.string.tramo_bloqueado
            );


            h.btn.setEnabled(
                    false
            );


            h.tvEmoji.setBackgroundResource(
                    R.drawable.nodo_bloqueado
            );


            /*
             * Antes estaba en 0.6 y hacía todo demasiado
             * transparente.
             *
             * 0.82 mantiene claro que está bloqueado
             * sin volver ilegible el texto.
             */

            h.itemView.setAlpha(
                    0.82f
            );


        } else {

            // -------------------------------------------------
            // DESBLOQUEADO
            // -------------------------------------------------

            h.itemView.setAlpha(
                    1f
            );


            h.btn.setEnabled(
                    true
            );


            if (
                    tramo.isAprobado()
            ) {

                // ---------------------------------------------
                // APROBADO
                // ---------------------------------------------

                h.tvEstado.setText(

                        ctx.getString(
                                R.string.tramo_aprobado,
                                tramo.getMejorPorcentaje()
                        )

                );


                h.btn.setText(
                        R.string.tramo_practicar
                );


                h.tvEmoji.setBackgroundResource(
                        R.drawable.nodo_completado
                );


            } else if (
                    tramo.getIntentos() > 0
            ) {

                // ---------------------------------------------
                // YA INTENTADO
                // ---------------------------------------------

                h.tvEstado.setText(

                        ctx.getString(
                                R.string.tramo_mejor,
                                tramo.getMejorPorcentaje(),
                                tramo.getIntentos()
                        )

                );


                h.btn.setText(
                        R.string.tramo_repetir
                );


                h.tvEmoji.setBackgroundResource(
                        R.drawable.nodo_actual
                );


            } else {

                // ---------------------------------------------
                // NUEVO
                // ---------------------------------------------

                h.tvEstado.setText(
                        R.string.tramo_nuevo
                );


                h.btn.setText(
                        R.string.tramo_empezar
                );


                h.tvEmoji.setBackgroundResource(
                        R.drawable.nodo_actual
                );
            }
        }


        // =====================================================
        // CLICK
        // =====================================================

        View.OnClickListener click =
                view -> {

                    if (
                            listener != null
                    ) {

                        listener.onTramo(
                                tramo
                        );
                    }
                };


        h.itemView.setOnClickListener(
                click
        );


        h.btn.setOnClickListener(
                click
        );
    }


    // =========================================================
    // ESTRELLAS
    // =========================================================

    private static String estrellas(
            int cantidad
    ) {

        StringBuilder sb =
                new StringBuilder(
                        3
                );


        for (
                int i = 0;
                i < 3;
                i++
        ) {

            sb.append(

                    i < cantidad
                            ? '\u2605'
                            : '\u2606'

            );
        }


        return sb.toString();
    }


    // =========================================================
    // CANTIDAD
    // =========================================================

    @Override
    public int getItemCount() {

        return tramos.size();
    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    static class VH
            extends RecyclerView.ViewHolder {

        final TextView tvEmoji;

        final TextView tvNombre;

        final TextView tvTema;

        final TextView tvEstrellas;

        final TextView tvEstado;

        final TextView tvContenido;

        final Button btn;

        final ImageView ivConector;

        final View contenedorNodo;


        VH(
                @NonNull View view
        ) {

            super(
                    view
            );


            tvEmoji =
                    view.findViewById(
                            R.id.tvEmoji
                    );


            tvNombre =
                    view.findViewById(
                            R.id.tvNombre
                    );


            tvTema =
                    view.findViewById(
                            R.id.tvTema
                    );


            tvEstrellas =
                    view.findViewById(
                            R.id.tvEstrellas
                    );


            tvEstado =
                    view.findViewById(
                            R.id.tvEstado
                    );


            tvContenido =
                    view.findViewById(
                            R.id.tvContenido
                    );


            btn =
                    view.findViewById(
                            R.id.btnTramo
                    );


            ivConector =
                    view.findViewById(
                            R.id.ivConector
                    );


            contenedorNodo =
                    view.findViewById(
                            R.id.contenedorNodo
                    );
        }
    }
}
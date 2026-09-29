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

/** Dibuja un nodo por tramo del camino de aprendizaje (zigzag). */
public class TramoAdapter extends RecyclerView.Adapter<TramoAdapter.VH> {

    public interface OnTramoClick {
        void onTramo(ProgresoNivel tramo);
    }

    private final List<ProgresoNivel> tramos = new ArrayList<>();
    private final OnTramoClick listener;

    public TramoAdapter(OnTramoClick listener) {
        this.listener = listener;
    }

    public void setTramos(List<ProgresoNivel> nuevos) {
        tramos.clear();
        if (nuevos != null) tramos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tramo, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProgresoNivel t = tramos.get(position);
        Context ctx = h.itemView.getContext();

        h.tvEmoji.setText(t.getEmoji());
        h.tvNombre.setText(t.getNombre());
        h.tvTema.setText(t.getTema());
        h.tvEstrellas.setText(estrellas(t.estrellas()));
        h.tvContenido.setText(ctx.getString(R.string.tramo_contenido, t.getTotalPreguntas()));

        // ---- Zigzag y conector ----
        float dp = ctx.getResources().getDisplayMetrics().density;
        boolean derecha = position % 2 == 0;
        h.contenedorNodo.setTranslationX((derecha ? 70 : -70) * dp);

        h.ivConector.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        h.ivConector.setImageResource(derecha ? R.drawable.conn_dr : R.drawable.conn_dl);

        // ---- Estado ----
        if (t.isBloqueado()) {
            h.tvEstado.setText(R.string.tramo_desbloqueado_msg);
            h.btn.setText(R.string.tramo_bloqueado);
            h.btn.setEnabled(false);
            h.tvEmoji.setBackgroundResource(R.drawable.nodo_bloqueado);
            h.itemView.setAlpha(0.6f);
        } else {
            h.itemView.setAlpha(1f);
            h.btn.setEnabled(true);
            if (t.isAprobado()) {
                h.tvEstado.setText(ctx.getString(R.string.tramo_aprobado, t.getMejorPorcentaje()));
                h.btn.setText(R.string.tramo_practicar);
                h.tvEmoji.setBackgroundResource(R.drawable.nodo_completado);
            } else if (t.getIntentos() > 0) {
                h.tvEstado.setText(ctx.getString(
                        R.string.tramo_mejor, t.getMejorPorcentaje(), t.getIntentos()));
                h.btn.setText(R.string.tramo_repetir);
                h.tvEmoji.setBackgroundResource(R.drawable.nodo_actual);
            } else {
                h.tvEstado.setText(R.string.tramo_nuevo);
                h.btn.setText(R.string.tramo_empezar);
                h.tvEmoji.setBackgroundResource(R.drawable.nodo_actual);
            }
        }

        View.OnClickListener click = v -> {
            if (listener != null) listener.onTramo(t);
        };
        h.itemView.setOnClickListener(click);
        h.btn.setOnClickListener(click);
    }

    private static String estrellas(int n) {
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < 3; i++) sb.append(i < n ? '\u2605' : '\u2606');
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return tramos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvEmoji, tvNombre, tvTema, tvEstrellas, tvEstado, tvContenido;
        final Button btn;
        final ImageView ivConector;
        final View contenedorNodo;

        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvTema = v.findViewById(R.id.tvTema);
            tvEstrellas = v.findViewById(R.id.tvEstrellas);
            tvEstado = v.findViewById(R.id.tvEstado);
            tvContenido = v.findViewById(R.id.tvContenido);
            btn = v.findViewById(R.id.btnTramo);
            ivConector = v.findViewById(R.id.ivConector);
            contenedorNodo = v.findViewById(R.id.contenedorNodo);
        }
    }
}
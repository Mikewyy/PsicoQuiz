package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.utm.semiologia.R;
import com.utm.semiologia.data.model.ProgresoNivel;

import java.util.ArrayList;
import java.util.List;

/** Dibuja una tarjeta por tramo del camino de aprendizaje. */
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

        if (t.isBloqueado()) {
            h.tvEstado.setText(R.string.tramo_desbloqueado_msg);
            h.btn.setText(R.string.tramo_bloqueado);
            h.btn.setEnabled(false);
            h.itemView.setAlpha(0.5f);
        } else {
            h.itemView.setAlpha(1f);
            h.btn.setEnabled(true);
            if (t.isAprobado()) {
                h.tvEstado.setText(ctx.getString(R.string.tramo_aprobado, t.getMejorPorcentaje()));
                h.btn.setText(R.string.tramo_practicar);
            } else if (t.getIntentos() > 0) {
                h.tvEstado.setText(ctx.getString(
                        R.string.tramo_mejor, t.getMejorPorcentaje(), t.getIntentos()));
                h.btn.setText(R.string.tramo_repetir);
            } else {
                h.tvEstado.setText(R.string.tramo_nuevo);
                h.btn.setText(R.string.tramo_empezar);
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
        final MaterialButton btn;

        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvTema = v.findViewById(R.id.tvTema);
            tvEstrellas = v.findViewById(R.id.tvEstrellas);
            tvEstado = v.findViewById(R.id.tvEstado);
            tvContenido = v.findViewById(R.id.tvContenido);
            btn = v.findViewById(R.id.btnTramo);
        }
    }
}

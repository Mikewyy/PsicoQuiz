package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

/** Lista de tramos; cada tarjeta abre el examen o la práctica del tramo. */
public class CaminoActivity extends BaseActivity
        implements TramoAdapter.OnTramoClick {

    private static final String EXTRA_CATEGORIA = "categoria";

    private CaminoViewModel vm;
    private TramoAdapter adapter;
    private TextView tvPuntos;
    private String categoria = Nivel.CAT_SINTOMAS;

    public static Intent nuevoIntent(Context ctx, String categoria) {
        Intent i = new Intent(ctx, CaminoActivity.class);
        i.putExtra(EXTRA_CATEGORIA, categoria);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camino);

        String extra = getIntent().getStringExtra(EXTRA_CATEGORIA);
        if (Nivel.CAT_SINDROMES.equals(extra)) {
            categoria = Nivel.CAT_SINDROMES;
        }

        TextView tvTitulo = findViewById(R.id.tvTituloCamino);
        tvTitulo.setText(Nivel.CAT_SINDROMES.equals(categoria)
                ? R.string.camino_titulo_sindromes
                : R.string.camino_titulo_sintomas);

        vm = new ViewModelProvider(this).get(CaminoViewModel.class);
        tvPuntos = findViewById(R.id.tvPuntos);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        adapter = new TramoAdapter(this);
        RecyclerView recycler = findViewById(R.id.recyclerTramos);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        vm.getEstado().observe(this, estado -> {
            if (estado == null) return;
            tvPuntos.setText(getString(R.string.camino_puntos, estado.puntos));
            adapter.setTramos(estado.tramos);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.cargar(categoria);
    }

    @Override
    public void onTramo(ProgresoNivel tramo) {
        if (tramo.isBloqueado()) {
            Toast.makeText(this, R.string.tramo_desbloqueado_msg, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(QuizActivity.nuevoIntent(this, tramo));
    }
}

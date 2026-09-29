package com.utm.semiologia.ui.evaluacion;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utm.semiologia.R;
import com.utm.semiologia.data.model.ProgresoNivel;

/** Lista de tramos; cada tarjeta abre el examen o la práctica del tramo. */
public class CaminoActivity extends AppCompatActivity
        implements TramoAdapter.OnTramoClick {

    private CaminoViewModel vm;
    private TramoAdapter adapter;
    private TextView tvPuntos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camino);

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
        vm.cargar();
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

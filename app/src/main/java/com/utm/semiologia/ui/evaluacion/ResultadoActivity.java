package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.utm.semiologia.R;
import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Intento;

/** Pantalla de cierre: aciertos, aprobado/suspendido y recompensas ganadas. */
public class ResultadoActivity extends AppCompatActivity {

    private static final String EXTRA_NIVEL_ID     = "nivel_id";
    private static final String EXTRA_NIVEL_NUMERO = "nivel_numero";
    private static final String EXTRA_NIVEL_NOMBRE = "nivel_nombre";
    private static final String EXTRA_ACIERTOS     = "aciertos";
    private static final String EXTRA_TOTAL        = "total";
    private static final String EXTRA_PORCENTAJE   = "porcentaje";
    private static final String EXTRA_APROBADO     = "aprobado";
    private static final String EXTRA_BONUS        = "bonus";
    private static final String EXTRA_COMIDA       = "comida";
    private static final String EXTRA_SIGUIENTE    = "siguiente";

    private long nivelId;
    private int nivelNumero;
    private String nivelNombre;
    private boolean aprobado;

    public static Intent nuevoIntent(Context ctx, ResultadoQuiz r) {
        Intent i = new Intent(ctx, ResultadoActivity.class);
        i.putExtra(EXTRA_NIVEL_ID, r.nivelId);
        i.putExtra(EXTRA_NIVEL_NUMERO, r.nivelNumero);
        i.putExtra(EXTRA_NIVEL_NOMBRE, r.nivelNombre);
        i.putExtra(EXTRA_ACIERTOS, r.aciertos);
        i.putExtra(EXTRA_TOTAL, r.total);
        i.putExtra(EXTRA_PORCENTAJE, r.porcentaje);
        i.putExtra(EXTRA_APROBADO, r.aprobado);
        i.putExtra(EXTRA_BONUS, r.bonusPuntos);
        i.putExtra(EXTRA_COMIDA, r.comida);
        i.putExtra(EXTRA_SIGUIENTE, r.siguienteNumero);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        nivelId = getIntent().getLongExtra(EXTRA_NIVEL_ID, -1);
        nivelNumero = getIntent().getIntExtra(EXTRA_NIVEL_NUMERO, 0);
        nivelNombre = getIntent().getStringExtra(EXTRA_NIVEL_NOMBRE);
        aprobado = getIntent().getBooleanExtra(EXTRA_APROBADO, false);
        int aciertos = getIntent().getIntExtra(EXTRA_ACIERTOS, 0);
        int total = getIntent().getIntExtra(EXTRA_TOTAL, 0);
        int porcentaje = getIntent().getIntExtra(EXTRA_PORCENTAJE, 0);
        int bonus = getIntent().getIntExtra(EXTRA_BONUS, 0);
        int comida = getIntent().getIntExtra(EXTRA_COMIDA, 0);
        int siguiente = getIntent().getIntExtra(EXTRA_SIGUIENTE, 0);

        TextView tvIcono = findViewById(R.id.tvIcono);
        TextView tvTitulo = findViewById(R.id.tvTitulo);
        TextView tvPorcentaje = findViewById(R.id.tvPorcentaje);
        TextView tvAciertos = findViewById(R.id.tvAciertos);
        TextView tvBonus = findViewById(R.id.tvBonus);
        TextView tvComida = findViewById(R.id.tvComida);
        TextView tvSiguiente = findViewById(R.id.tvSiguiente);

        tvIcono.setText(aprobado ? "\uD83C\uDF89" : "\uD83D\uDCAA");
        tvTitulo.setText(aprobado ? R.string.resultado_aprobado : R.string.resultado_fallado);
        tvPorcentaje.setText(getString(R.string.resultado_porcentaje, porcentaje));
        tvAciertos.setText(getString(R.string.resultado_aciertos, aciertos, total));

        if (bonus > 0) {
            tvBonus.setVisibility(View.VISIBLE);
            tvBonus.setText(getString(R.string.resultado_bonus_puntos, bonus));
        } else {
            tvBonus.setVisibility(View.GONE);
        }
        if (comida > 0) {
            tvComida.setVisibility(View.VISIBLE);
            tvComida.setText(getString(R.string.resultado_comida, comida));
        } else {
            tvComida.setVisibility(View.GONE);
        }
        if (siguiente > 0) {
            tvSiguiente.setVisibility(View.VISIBLE);
            tvSiguiente.setText(getString(R.string.resultado_siguiente, siguiente));
        } else if (!aprobado) {
            int umbral = Math.round(DatabaseHelper.UMBRAL_APROBACION * 100f);
            tvSiguiente.setVisibility(View.VISIBLE);
            tvSiguiente.setText(getString(R.string.resultado_umbral, umbral));
        } else {
            tvSiguiente.setVisibility(View.GONE);
        }

        MaterialButton btnReintentar = findViewById(R.id.btnReintentar);
        btnReintentar.setText(aprobado ? R.string.tramo_practicar : R.string.resultado_reintentar);
        btnReintentar.setOnClickListener(v -> {
            String modo = aprobado ? Intento.MODO_PRACTICA : Intento.MODO_EXAMEN;
            startActivity(QuizActivity.nuevoIntent(this, nivelId, nivelNumero, nivelNombre, modo));
            finish();
        });

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }
}

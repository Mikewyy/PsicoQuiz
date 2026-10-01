package com.utm.semiologia.ui.evaluacion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.data.model.Intento;
import com.utm.semiologia.data.model.Opcion;
import com.utm.semiologia.data.model.ProgresoNivel;

/** Pantalla de un intento: muestra preguntas MCQ o escritas y da feedback. */
public class QuizActivity extends AppCompatActivity {

    private static final String EXTRA_NIVEL_ID     = "nivel_id";
    private static final String EXTRA_NIVEL_NUMERO = "nivel_numero";
    private static final String EXTRA_NIVEL_NOMBRE = "nivel_nombre";
    private static final String EXTRA_MODO         = "modo";

    private QuizViewModel vm;
    private TextView tvProgreso, tvTema, tvEnunciado, tvPista, tvFeedback, tvJustificacion;
    private LinearProgressIndicator barra;
    private LinearLayout opcionesContainer, feedbackContainer;
    private EditText etRespuesta;
    private MaterialButton btnAccion;

    private EstadoQuiz ultimo;
    private Long opcionSeleccionada;

    public static Intent nuevoIntent(Context ctx, ProgresoNivel tramo) {
        return nuevoIntent(ctx, tramo.getNivelId(), tramo.getNumero(), tramo.getNombre(),
                tramo.isAprobado() ? Intento.MODO_PRACTICA : Intento.MODO_EXAMEN);
    }

    public static Intent nuevoIntent(Context ctx, long nivelId, int numero,
                                     String nombre, String modo) {
        Intent i = new Intent(ctx, QuizActivity.class);
        i.putExtra(EXTRA_NIVEL_ID, nivelId);
        i.putExtra(EXTRA_NIVEL_NUMERO, numero);
        i.putExtra(EXTRA_NIVEL_NOMBRE, nombre);
        i.putExtra(EXTRA_MODO, modo);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        long nivelId = getIntent().getLongExtra(EXTRA_NIVEL_ID, -1);
        int numero = getIntent().getIntExtra(EXTRA_NIVEL_NUMERO, 0);
        String nombre = getIntent().getStringExtra(EXTRA_NIVEL_NOMBRE);
        String modo = getIntent().getStringExtra(EXTRA_MODO);
        if (nivelId <= 0) {
            finish();
            return;
        }

        tvProgreso = findViewById(R.id.tvProgreso);
        tvTema = findViewById(R.id.tvTema);
        tvEnunciado = findViewById(R.id.tvEnunciado);
        tvPista = findViewById(R.id.tvPista);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvJustificacion = findViewById(R.id.tvJustificacion);
        barra = findViewById(R.id.barra);
        opcionesContainer = findViewById(R.id.opcionesContainer);
        feedbackContainer = findViewById(R.id.feedbackContainer);
        etRespuesta = findViewById(R.id.etRespuesta);
        btnAccion = findViewById(R.id.btnAccion);

        vm = new ViewModelProvider(this).get(QuizViewModel.class);
        if (!vm.iniciar(nivelId, numero, nombre, modo)) {
            Toast.makeText(this, R.string.quiz_sin_energia, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.btnCerrar).setOnClickListener(v -> confirmarSalida());
        btnAccion.setOnClickListener(v -> onAccion());

        vm.getEstado().observe(this, this::render);
    }

    @Override
    public void onBackPressed() {
        confirmarSalida();
    }

    private void confirmarSalida() {
        EstadoQuiz e = ultimo;
        if (e != null && e.respondida && e.esUltima()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.quiz_salir)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (d, w) -> finish())
                .show();
    }

    private void onAccion() {
        EstadoQuiz e = ultimo;
        if (e == null || e.pregunta == null) return;

        if (!e.respondida) {
            if (e.esEscrita) {
                vm.comprobarTexto(etRespuesta.getText().toString());
            } else if (opcionSeleccionada == null) {
                Toast.makeText(this, R.string.quiz_elige_opcion, Toast.LENGTH_SHORT).show();
            } else {
                vm.comprobarOpcion(opcionSeleccionada);
            }
        } else if (e.esUltima()) {
            ResultadoQuiz r = vm.terminar();
            startActivity(ResultadoActivity.nuevoIntent(this, r));
            finish();
        } else {
            vm.avanzar();
        }
    }

    private void render(EstadoQuiz e) {
        ultimo = e;
        if (e.pregunta == null) {
            tvProgreso.setText("");
            tvTema.setText("");
            tvEnunciado.setText(R.string.quiz_sin_preguntas);
            tvPista.setVisibility(View.GONE);
            opcionesContainer.removeAllViews();
            etRespuesta.setVisibility(View.GONE);
            feedbackContainer.setVisibility(View.GONE);
            btnAccion.setEnabled(false);
            return;
        }

        btnAccion.setEnabled(true);
        barra.setMax(e.total);
        barra.setProgress(e.indice + 1);
        tvProgreso.setText(getString(R.string.quiz_progreso, e.indice + 1, e.total));
        tvTema.setText(e.pregunta.getTema());
        tvEnunciado.setText(e.pregunta.getEnunciado());

        String pista = e.pregunta.getPista();
        if (pista != null && !pista.trim().isEmpty()) {
            tvPista.setVisibility(View.VISIBLE);
            tvPista.setText(getString(R.string.quiz_pista, pista));
        } else {
            tvPista.setVisibility(View.GONE);
        }

        pintarRespuestas(e);

        if (e.respondida) {
            feedbackContainer.setVisibility(View.VISIBLE);
            tvFeedback.setText(e.correcta ? R.string.quiz_correcto : R.string.quiz_incorrecto);
            tvFeedback.setTextColor(getColor(
                    e.correcta ? R.color.secondary : R.color.hambre_bajo));
            String just = e.justificacion;
            if (just != null && !just.trim().isEmpty()) {
                tvJustificacion.setVisibility(View.VISIBLE);
                tvJustificacion.setText(just);
            } else {
                tvJustificacion.setVisibility(View.GONE);
            }
            btnAccion.setText(e.esUltima() ? R.string.quiz_terminar : R.string.quiz_continuar);
        } else {
            feedbackContainer.setVisibility(View.GONE);
            btnAccion.setText(R.string.quiz_comprobar);
        }
    }

    private void pintarRespuestas(EstadoQuiz e) {
        opcionesContainer.removeAllViews();

        if (e.esEscrita) {
            etRespuesta.setVisibility(View.VISIBLE);
            etRespuesta.setEnabled(!e.respondida);
            if (!e.respondida) {
                opcionSeleccionada = null;
                etRespuesta.setText("");
            }
            return;
        }

        etRespuesta.setVisibility(View.GONE);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        for (Opcion o : e.opciones) {
            RadioButton rb = new RadioButton(this);
            rb.setText(o.getTexto());
            rb.setTag(o.getId());
            rb.setEnabled(!e.respondida);
            if (opcionSeleccionada != null && o.getId() == opcionSeleccionada) {
                rb.setChecked(true);
            }
            if (e.respondida) {
                if (o.isCorrecta()) {
                    rb.setTextColor(getColor(R.color.secondary));
                } else if (opcionSeleccionada != null && o.getId() == opcionSeleccionada) {
                    rb.setTextColor(getColor(R.color.hambre_bajo));
                }
            }
            rb.setOnClickListener(v -> opcionSeleccionada = (Long) v.getTag());
            group.addView(rb);
        }
        opcionesContainer.addView(group);
    }
}

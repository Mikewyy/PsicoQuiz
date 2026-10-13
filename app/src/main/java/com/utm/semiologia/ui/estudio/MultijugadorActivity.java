package com.utm.semiologia.ui.estudio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.utm.semiologia.R;
import com.utm.semiologia.firebase.SalaRepository;
import com.utm.semiologia.firebase.model.SalaEstudio;
import com.utm.semiologia.ui.common.BaseActivity;
import com.utm.semiologia.ui.common.NavegacionInferior;
import com.utm.semiologia.util.UiFeedback;

import java.util.Locale;


public class MultijugadorActivity extends BaseActivity {


    // =========================================================
    // FIREBASE
    // =========================================================

    private SalaRepository salaRepository;

    private static final String PREFS_MULTIJUGADOR = "patotective_multijugador";
    private static final String KEY_NOMBRE_JUGADOR = "nombre_jugador";


    // =========================================================
    // VISTAS
    // =========================================================

    private EditText etNombreUsuario;

    private EditText etNombreSala;

    private EditText etCodigoSala;


    private MaterialButton btnCrearSala;

    private MaterialButton btnUnirseSala;

    private MaterialButton btnCopiarCodigo;


    private LinearLayout panelResultado;


    private TextView tvEstado;

    private TextView tvCodigoResultado;

    private TextView tvNombreSalaResultado;


    private ProgressBar progressFirebase;


    // =========================================================
    // CREACIÓN
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_multijugador
        );

        configurarNavInferior(
                NavegacionInferior.SECCION_EXPLORAR
        );


        salaRepository =
                new SalaRepository();


        enlazarVistas();
        cargarNombreGuardado();

        configurarListeners();
        aplicarReinicioSiCorresponde();
    }


    // =========================================================
    // ENLAZAR VISTAS
    // =========================================================

    private void enlazarVistas() {


        etNombreUsuario =
                findViewById(
                        R.id.etNombreUsuario
                );


        etNombreSala =
                findViewById(
                        R.id.etNombreSala
                );


        etCodigoSala =
                findViewById(
                        R.id.etCodigoSala
                );


        btnCrearSala =
                findViewById(
                        R.id.btnCrearSala
                );


        btnUnirseSala =
                findViewById(
                        R.id.btnUnirseSala
                );


        btnCopiarCodigo =
                findViewById(
                        R.id.btnCopiarCodigo
                );


        panelResultado =
                findViewById(
                        R.id.panelResultado
                );


        tvEstado =
                findViewById(
                        R.id.tvEstado
                );


        tvCodigoResultado =
                findViewById(
                        R.id.tvCodigoResultado
                );


        tvNombreSalaResultado =
                findViewById(
                        R.id.tvNombreSalaResultado
                );


        progressFirebase =
                findViewById(
                        R.id.progressFirebase
                );
    }


    /** Recuerda el nombre del jugador entre sesiones y nuevas salas. */
    private void cargarNombreGuardado() {
        SharedPreferences preferencias = getSharedPreferences(PREFS_MULTIJUGADOR, MODE_PRIVATE);
        String nombre = preferencias.getString(KEY_NOMBRE_JUGADOR, "");
        if (nombre != null && !nombre.isEmpty() && etNombreUsuario != null) {
            etNombreUsuario.setText(nombre);
            etNombreUsuario.setSelection(nombre.length());
        }
    }

    private void guardarNombreJugador(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return;
        getSharedPreferences(PREFS_MULTIJUGADOR, MODE_PRIVATE)
                .edit()
                .putString(KEY_NOMBRE_JUGADOR, nombre.trim())
                .apply();
    }

    private void aplicarReinicioSiCorresponde() {

        if (!getIntent().getBooleanExtra("extra_reiniciar_multijugador", false)) {
            return;
        }

        if (etCodigoSala != null) {
            etCodigoSala.setText("");
        }

        if (etNombreSala != null) {
            etNombreSala.setText("");
        }

        if (panelResultado != null) {
            panelResultado.setVisibility(View.GONE);
        }

        if (tvEstado != null) {
            tvEstado.setText("Crea una nueva sala o únete con un código.");
        }

        if (progressFirebase != null) {
            progressFirebase.setVisibility(View.GONE);
        }
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {


        // -----------------------------------------------------
        // VOLVER
        // -----------------------------------------------------

        findViewById(
                R.id.btnVolver
        ).setOnClickListener(
                v -> finish()
        );


        // -----------------------------------------------------
        // CREAR SALA
        // -----------------------------------------------------

        btnCrearSala.setOnClickListener(
                v -> crearSala()
        );


        // -----------------------------------------------------
        // UNIRSE A SALA
        // -----------------------------------------------------

        btnUnirseSala.setOnClickListener(
                v -> unirseSala()
        );


        // -----------------------------------------------------
        // COPIAR CÓDIGO
        // -----------------------------------------------------

        btnCopiarCodigo.setOnClickListener(
                v -> copiarCodigo()
        );
    }


    // =========================================================
    // CREAR SALA
    // =========================================================

    private void crearSala() {


        String nombreUsuario =
                obtenerTexto(
                        etNombreUsuario
                );


        String nombreSala =
                obtenerTexto(
                        etNombreSala
                );


        // -----------------------------------------------------
        // VALIDAR NOMBRE DEL USUARIO
        // -----------------------------------------------------

        if (nombreUsuario.isEmpty()) {

            UiFeedback.mostrarErrorCampo(etNombreUsuario, "Escribe tu nombre");


            etNombreUsuario.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // VALIDAR NOMBRE DE LA SALA
        // -----------------------------------------------------

        if (nombreSala.isEmpty()) {

            UiFeedback.mostrarErrorCampo(etNombreSala, "Escribe un nombre para la sala");


            etNombreSala.requestFocus();

            return;
        }


        // Guardar el nombre elegido para próximas partidas.
        guardarNombreJugador(nombreUsuario);

        // -----------------------------------------------------
        // MOSTRAR CARGANDO
        // -----------------------------------------------------

        mostrarCargando(
                true
        );


        // -----------------------------------------------------
        // FIREBASE
        // -----------------------------------------------------

        salaRepository.crearSala(
                nombreSala,
                nombreUsuario,
                new SalaRepository.SalaCallback() {


                    @Override
                    public void onSuccess(
                            SalaEstudio sala
                    ) {

                        runOnUiThread(
                                () -> {

                                    mostrarCargando(
                                            false
                                    );


                                    mostrarSalaCreada(
                                            sala
                                    );
                                }
                        );
                    }


                    @Override
                    public void onError(
                            String mensaje
                    ) {

                        runOnUiThread(
                                () -> {

                                    mostrarCargando(
                                            false
                                    );


                                    mostrarError(
                                            mensaje
                                    );
                                }
                        );
                    }
                }
        );
    }


    // =========================================================
    // UNIRSE A SALA
    // =========================================================

    private void unirseSala() {


        String nombreUsuario =
                obtenerTexto(
                        etNombreUsuario
                );


        String codigo =
                obtenerTexto(
                        etCodigoSala
                )
                        .replace(
                                " ",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );


        // -----------------------------------------------------
        // VALIDAR NOMBRE
        // -----------------------------------------------------

        if (nombreUsuario.isEmpty()) {

            UiFeedback.mostrarErrorCampo(etNombreUsuario, "Escribe tu nombre");


            etNombreUsuario.requestFocus();

            return;
        }


        // -----------------------------------------------------
        // VALIDAR CÓDIGO
        // -----------------------------------------------------

        if (codigo.isEmpty()) {

            UiFeedback.mostrarErrorCampo(etCodigoSala, "Escribe el código de la sala");


            etCodigoSala.requestFocus();

            return;
        }


        if (codigo.length() != 6) {

            UiFeedback.mostrarErrorCampo(etCodigoSala, "El código debe tener 6 caracteres");


            etCodigoSala.requestFocus();

            return;
        }


        // Guardar el nombre elegido para próximas partidas.
        guardarNombreJugador(nombreUsuario);

        // -----------------------------------------------------
        // MOSTRAR CARGANDO
        // -----------------------------------------------------

        mostrarCargando(
                true
        );


        // -----------------------------------------------------
        // FIREBASE
        // -----------------------------------------------------

        salaRepository.unirseSala(
                codigo,
                nombreUsuario,
                new SalaRepository.SalaCallback() {


                    @Override
                    public void onSuccess(
                            SalaEstudio sala
                    ) {

                        runOnUiThread(
                                () -> {

                                    mostrarCargando(
                                            false
                                    );


                                    mostrarSalaUnida(
                                            sala
                                    );
                                }
                        );
                    }


                    @Override
                    public void onError(
                            String mensaje
                    ) {

                        runOnUiThread(
                                () -> {

                                    mostrarCargando(
                                            false
                                    );


                                    mostrarError(
                                            mensaje
                                    );
                                }
                        );
                    }
                }
        );
    }


    // =========================================================
    // SALA CREADA
    // =========================================================

    private void mostrarSalaCreada(
            SalaEstudio sala
    ) {


        if (sala == null) {

            mostrarError(
                    "No se pudo abrir la sala."
            );

            return;
        }


        String codigo =
                sala.getCodigo();


        if (
                codigo == null ||
                        codigo.trim().isEmpty()
        ) {

            mostrarError(
                    "La sala no tiene un código válido."
            );

            return;
        }


        // -----------------------------------------------------
        // MOSTRAMOS RESULTADO BREVEMENTE
        // -----------------------------------------------------

        panelResultado.setVisibility(
                View.VISIBLE
        );


        tvEstado.setText(
                "Sala creada correctamente"
        );


        tvCodigoResultado.setText(
                codigo
        );


        tvNombreSalaResultado.setText(
                "Entrando a la sala..."
        );


        btnCopiarCodigo.setVisibility(
                View.VISIBLE
        );


        Toast.makeText(
                this,
                "Sala creada correctamente",
                Toast.LENGTH_SHORT
        ).show();


        // -----------------------------------------------------
        // ABRIR SALA
        // -----------------------------------------------------

        abrirSala(
                codigo
        );
    }


    // =========================================================
    // SALA UNIDA
    // =========================================================

    private void mostrarSalaUnida(
            SalaEstudio sala
    ) {


        if (sala == null) {

            mostrarError(
                    "No se pudo abrir la sala."
            );

            return;
        }


        String codigo =
                sala.getCodigo();


        if (
                codigo == null ||
                        codigo.trim().isEmpty()
        ) {

            mostrarError(
                    "La sala no tiene un código válido."
            );

            return;
        }


        // -----------------------------------------------------
        // MOSTRAR RESULTADO
        // -----------------------------------------------------

        panelResultado.setVisibility(
                View.VISIBLE
        );


        tvEstado.setText(
                "Te uniste a la sala"
        );


        tvCodigoResultado.setText(
                codigo
        );


        tvNombreSalaResultado.setText(
                "Entrando a la sala..."
        );


        btnCopiarCodigo.setVisibility(
                View.GONE
        );


        Toast.makeText(
                this,
                "Te uniste correctamente",
                Toast.LENGTH_SHORT
        ).show();


        // -----------------------------------------------------
        // ABRIR SALA
        // -----------------------------------------------------

        abrirSala(
                codigo
        );
    }


    // =========================================================
    // ABRIR SALA
    // =========================================================

    private void abrirSala(
            String codigo
    ) {


        Intent intent =
                new Intent(
                        this,
                        SalaActivity.class
                );


        intent.putExtra(
                SalaActivity.EXTRA_CODIGO_SALA,
                codigo
        );


        startActivity(
                intent
        );
    }


    // =========================================================
    // COPIAR CÓDIGO
    // =========================================================

    private void copiarCodigo() {


        String codigo =
                tvCodigoResultado
                        .getText()
                        .toString()
                        .trim();


        if (codigo.isEmpty()) {

            return;
        }


        ClipboardManager clipboardManager =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );


        if (clipboardManager == null) {

            Toast.makeText(
                    this,
                    "No se pudo copiar el código.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ClipData clip =
                ClipData.newPlainText(
                        "Código de sala",
                        codigo
                );


        clipboardManager.setPrimaryClip(
                clip
        );


        Toast.makeText(
                this,
                "Código copiado",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // CARGANDO
    // =========================================================

    private void mostrarCargando(
            boolean cargando
    ) {


        if (cargando) {


            progressFirebase.setVisibility(
                    View.VISIBLE
            );


            btnCrearSala.setEnabled(
                    false
            );


            btnUnirseSala.setEnabled(
                    false
            );


        } else {


            progressFirebase.setVisibility(
                    View.GONE
            );


            btnCrearSala.setEnabled(
                    true
            );


            btnUnirseSala.setEnabled(
                    true
            );
        }
    }


    // =========================================================
    // MOSTRAR ERROR
    // =========================================================

    private void mostrarError(
            String mensaje
    ) {


        if (
                mensaje == null ||
                        mensaje.trim().isEmpty()
        ) {

            mensaje =
                    "Ocurrió un error al comunicarse con Firebase.";
        }


        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // OBTENER TEXTO
    // =========================================================

    private String obtenerTexto(
            EditText editText
    ) {


        if (
                editText == null ||
                        editText.getText() == null
        ) {

            return "";
        }


        return editText
                .getText()
                .toString()
                .trim();
    }
}
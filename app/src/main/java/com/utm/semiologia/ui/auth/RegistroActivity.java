package com.utm.semiologia.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.ui.dashboard.MainActivity;
import com.utm.semiologia.util.HashUtil;

/**
 * Registro de un estudiante nuevo.
 *
 * Crea:
 * - Usuario
 * - Mascota inicial
 * - 3 alimentos
 * - Accesorio inicial
 *
 * No permite entrar al Dashboard si la mascota
 * no pudo crearse correctamente.
 */
public class RegistroActivity extends AppCompatActivity {

    // =========================================================
    // CONFIGURACIÓN INICIAL
    // =========================================================

    private static final long ALIMENTO_INICIAL_ID = 1L;
    private static final int CANTIDAD_INICIAL_ALIMENTO = 3;

    private static final long ACCESORIO_INICIAL_ID = 1L;

    private static final int MIN_PASSWORD_LENGTH = 6;


    // =========================================================
    // VISTAS
    // =========================================================

    private EditText etNombre;
    private EditText etEmail;
    private EditText etPassword;

    private Button btnCrear;

    private ImageButton btnVerPassword;

    private TextView linkLogin;

    /** true cuando la contraseña se está mostrando en claro. */
    private boolean passwordVisible = false;


    // =========================================================
    // CREACIÓN
    // =========================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_registro
        );


        enlazarVistas();

        setupListeners();
    }


    // =========================================================
    // ENLAZAR VISTAS
    // =========================================================

    private void enlazarVistas() {

        etNombre =
                findViewById(
                        R.id.etNombre
                );

        etEmail =
                findViewById(
                        R.id.etEmail
                );

        etPassword =
                findViewById(
                        R.id.etPassword
                );

        btnCrear =
                findViewById(
                        R.id.btnCrear
                );

        btnVerPassword =
                findViewById(
                        R.id.btnVerPassword
                );

        linkLogin =
                findViewById(
                        R.id.linkLogin
                );
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        btnCrear.setOnClickListener(
                v -> intentarRegistro()
        );

        btnVerPassword.setOnClickListener(
                v -> alternarVisibilidadPassword()
        );


        linkLogin.setOnClickListener(
                v -> confirmarSalida()
        );
    }


    // =========================================================
    // BOTÓN ATRÁS
    // =========================================================

    @Override
    public void onBackPressed() {

        confirmarSalida();
    }


    // =========================================================
    // CONFIRMAR SALIDA
    // =========================================================

    private void confirmarSalida() {

        View vistaBase =
                obtenerVistaAnchor();


        Snackbar snackbar =
                Snackbar.make(
                                vistaBase,
                                "¿Deseas salir del registro?",
                                Snackbar.LENGTH_LONG
                        )
                        .setAction(
                                "SÍ, SALIR",
                                v -> finish()
                        )
                        .setActionTextColor(
                                Color.parseColor("#FBBF24")
                        );


        View snackView =
                snackbar.getView();


        snackView.setBackgroundColor(
                Color.parseColor("#1E293B")
        );


        TextView textView =
                snackView.findViewById(
                        com.google.android.material.R.id.snackbar_text
                );


        if (textView != null) {

            textView.setTextColor(
                    Color.WHITE
            );

            textView.setTextSize(
                    14
            );
        }


        snackbar.show();
    }


    // =========================================================
    // INTENTAR REGISTRO
    // =========================================================

    private void intentarRegistro() {

        String nombre =
                etNombre
                        .getText()
                        .toString()
                        .trim();


        String email =
                etEmail
                        .getText()
                        .toString()
                        .trim();


        String password =
                etPassword
                        .getText()
                        .toString();


        // -----------------------------------------------------
        // VALIDAR
        // -----------------------------------------------------

        if (!validarEntradas(
                nombre,
                email,
                password
        )) {

            return;
        }


        Repositorio repo =
                SemiologiaApp.getRepositorio();


        // -----------------------------------------------------
        // EMAIL DUPLICADO
        // -----------------------------------------------------

        if (
                repo.usuarios()
                        .buscarPorEmail(email)
                        != null
        ) {

            mostrarSnackbarError(
                    getString(
                            R.string.error_email_duplicado
                    )
            );

            return;
        }


        // -----------------------------------------------------
        // CREAR CUENTA
        // -----------------------------------------------------

        crearCuentaYRegistrarMascota(
                repo,
                nombre,
                email,
                password
        );
    }


    // =========================================================
    // VALIDAR ENTRADAS
    // =========================================================

    private void alternarVisibilidadPassword() {

        passwordVisible = !passwordVisible;


        if (passwordVisible) {

            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            etPassword.setSelection(
                    etPassword.getText().length()
            );

            btnVerPassword.setImageResource(
                    R.drawable.ic_ojo_cerrado
            );

            btnVerPassword.setContentDescription(
                    getString(
                            R.string.ocultar_contrasena
                    )
            );

        } else {

            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            btnVerPassword.setImageResource(
                    R.drawable.ic_ojo
            );

            btnVerPassword.setContentDescription(
                    getString(
                            R.string.ver_contrasena
                    )
            );

        }
    }


    private boolean validarEntradas(
            String nombre,
            String email,
            String password
    ) {


        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------

        if (TextUtils.isEmpty(nombre)) {

            etNombre.setError(
                    "Ingresa tu nombre completo"
            );

            etNombre.requestFocus();


            mostrarSnackbarError(
                    getString(
                            R.string.error_campos_vacios
                    )
            );

            return false;
        }


        // -----------------------------------------------------
        // EMAIL
        // -----------------------------------------------------

        if (TextUtils.isEmpty(email)) {

            etEmail.setError(
                    "Ingresa tu correo electrónico"
            );

            etEmail.requestFocus();


            mostrarSnackbarError(
                    getString(
                            R.string.error_campos_vacios
                    )
            );

            return false;
        }


        if (
                !Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
        ) {

            etEmail.setError(
                    "Correo electrónico inválido"
            );

            etEmail.requestFocus();


            mostrarSnackbarError(
                    getString(
                            R.string.error_email_invalido
                    )
            );

            return false;
        }


        // -----------------------------------------------------
        // CONTRASEÑA
        // -----------------------------------------------------

        if (
                TextUtils.isEmpty(password)
                        ||
                        password.length()
                                < MIN_PASSWORD_LENGTH
        ) {

            etPassword.setError(
                    "La contraseña debe tener al menos "
                            + MIN_PASSWORD_LENGTH
                            + " caracteres"
            );

            etPassword.requestFocus();


            mostrarSnackbarError(
                    getString(
                            R.string.error_password_corta
                    )
            );

            return false;
        }


        return true;
    }


    // =========================================================
    // CREAR CUENTA Y MASCOTA
    // =========================================================

    private void crearCuentaYRegistrarMascota(
            Repositorio repo,
            String nombre,
            String email,
            String password
    ) {


        // -----------------------------------------------------
        // SEGURIDAD
        // -----------------------------------------------------

        String salt =
                HashUtil.nuevoSalt();


        String passwordHasheada =
                HashUtil.hashear(
                        password,
                        salt
                );


        // -----------------------------------------------------
        // CREAR USUARIO
        // -----------------------------------------------------

        Usuario nuevoUsuario =
                Usuario.crear(
                        nombre,
                        email,
                        passwordHasheada,
                        salt
                );


        long usuarioId =
                repo.usuarios()
                        .insertar(
                                nuevoUsuario
                        );


        if (usuarioId <= 0) {

            mostrarSnackbarError(
                    getString(
                            R.string.error_email_duplicado
                    )
            );

            return;
        }


        // -----------------------------------------------------
        // CREAR MASCOTA Y REGALOS
        // -----------------------------------------------------

        boolean mascotaCreada =
                otorgarRegalosBienvenida(
                        repo,
                        usuarioId,
                        nombre
                );


        /*
         * MUY IMPORTANTE:
         *
         * No dejamos entrar al Dashboard si no existe
         * una mascota asociada al usuario.
         */
        if (!mascotaCreada) {

            mostrarSnackbarError(
                    "No se pudo crear la mascota inicial. Intenta nuevamente."
            );

            return;
        }


        // -----------------------------------------------------
        // INICIAR SESIÓN
        // -----------------------------------------------------

        iniciarSesionYNavegar(
                email,
                salt,
                usuarioId
        );
    }


    // =========================================================
    // CREAR MASCOTA + REGALOS
    // =========================================================

    private boolean otorgarRegalosBienvenida(
            Repositorio repo,
            long usuarioId,
            String nombreUsuario
    ) {


        // -----------------------------------------------------
        // MASCOTA INICIAL
        // -----------------------------------------------------

        Mascota mascotaInicial =
                Mascota.crearPorDefecto(
                        usuarioId,
                        nombreUsuario
                );


        long mascotaId =
                repo.mascotas()
                        .insertar(
                                mascotaInicial
                        );


        /*
         * Si SQLite no pudo insertar la mascota,
         * detenemos el registro.
         */
        if (mascotaId <= 0) {

            return false;
        }


        // -----------------------------------------------------
        // COMIDA INICIAL
        // -----------------------------------------------------

        repo.mascotas()
                .otorgarAlimento(
                        usuarioId,
                        ALIMENTO_INICIAL_ID,
                        CANTIDAD_INICIAL_ALIMENTO
                );


        // -----------------------------------------------------
        // ACCESORIO INICIAL
        // -----------------------------------------------------

        repo.mascotas()
                .otorgarAccesorio(
                        usuarioId,
                        ACCESORIO_INICIAL_ID
                );


        return true;
    }


    // =========================================================
    // INICIAR SESIÓN
    // =========================================================

    private void iniciarSesionYNavegar(
            String email,
            String salt,
            long usuarioId
    ) {


        SemiologiaApp
                .getSesion()
                .iniciarSesion(
                        usuarioId,
                        email,
                        salt
                );


        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );


        intent.addFlags(

                Intent.FLAG_ACTIVITY_NEW_TASK

                        |

                        Intent.FLAG_ACTIVITY_CLEAR_TASK

        );


        startActivity(
                intent
        );


        finish();
    }


    // =========================================================
    // VISTA PARA SNACKBAR
    // =========================================================

    private View obtenerVistaAnchor() {

        View vista =
                findViewById(
                        R.id.btnCrear
                );


        if (vista == null) {

            vista =
                    findViewById(
                            android.R.id.content
                    );
        }


        return vista;
    }


    // =========================================================
    // SNACKBAR ERROR
    // =========================================================

    private void mostrarSnackbarError(
            String mensaje
    ) {


        View vistaBase =
                obtenerVistaAnchor();


        Snackbar snackbar =
                Snackbar.make(
                        vistaBase,
                        mensaje,
                        Snackbar.LENGTH_LONG
                );


        View snackView =
                snackbar.getView();


        snackView.setBackgroundColor(
                Color.parseColor("#EF4444")
        );


        TextView textView =
                snackView.findViewById(
                        com.google.android.material.R.id.snackbar_text
                );


        if (textView != null) {

            textView.setTextColor(
                    Color.WHITE
            );

            textView.setTextSize(
                    14
            );
        }


        snackbar.show();
    }


    // =========================================================
    // SNACKBAR ÉXITO
    // =========================================================

    private void mostrarSnackbarExito(
            String mensaje
    ) {


        View vistaBase =
                obtenerVistaAnchor();


        Snackbar snackbar =
                Snackbar.make(
                        vistaBase,
                        mensaje,
                        Snackbar.LENGTH_LONG
                );


        View snackView =
                snackbar.getView();


        snackView.setBackgroundColor(
                Color.parseColor("#6366F1")
        );


        TextView textView =
                snackView.findViewById(
                        com.google.android.material.R.id.snackbar_text
                );


        if (textView != null) {

            textView.setTextColor(
                    Color.WHITE
            );

            textView.setTextSize(
                    14
            );
        }


        snackbar.show();
    }
}
package com.utm.semiologia.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
 * Otorga automáticamente una mascota inicial y suministros de bienvenida.
 */
public class RegistroActivity extends AppCompatActivity {

    private static final long ALIMENTO_INICIAL_ID = 1L; // Galleta
    private static final int CANTIDAD_INICIAL_ALIMENTO = 3;
    private static final long ACCESORIO_INICIAL_ID = 1L; // Collar básico
    private static final int MIN_PASSWORD_LENGTH = 6;

    private EditText etNombre, etEmail, etPassword;
    private Button btnCrear;
    private TextView linkLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar vistas mediante findViewById
        etNombre   = findViewById(R.id.etNombre);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnCrear   = findViewById(R.id.btnCrear);
        linkLogin  = findViewById(R.id.linkLogin);

        setupListeners();
    }

    private void setupListeners() {
        btnCrear.setOnClickListener(v -> intentarRegistro());
        // Al presionar "¿Ya tienes cuenta? Ingresa" pide confirmación con Snackbar
        linkLogin.setOnClickListener(v -> confirmarSalida());
    }

    /**
     * Intercepta el botón físico/gesto "Atrás" del dispositivo
     */
    @Override
    public void onBackPressed() {
        confirmarSalida();
    }

    /**
     * Muestra una Snackbar flotante para confirmar si desea salir del registro
     */
    private void confirmarSalida() {
        View vistaBase = obtenerVistaAnchor();

        Snackbar snackbar = Snackbar.make(vistaBase, "¿Deseas salir del registro?", Snackbar.LENGTH_LONG)
                .setAction("SÍ, SALIR", v -> finish())
                .setActionTextColor(Color.parseColor("#FBBF24")); // Amarillo llamativo

        View snackView = snackbar.getView();
        snackView.setBackgroundColor(Color.parseColor("#1E293B")); // Gris oscuro/Azul noche

        TextView textView = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }

    private void intentarRegistro() {
        String nombre   = etNombre.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (!validarEntradas(nombre, email, password)) {
            return;
        }

        Repositorio repo = SemiologiaApp.getRepositorio();
        if (repo.usuarios().buscarPorEmail(email) != null) {
            mostrarSnackbarError(getString(R.string.error_email_duplicado));
            return;
        }

        crearCuentaYRegistrarMascota(repo, nombre, email, password);
    }

    private boolean validarEntradas(String nombre, String email, String password) {
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingresa tu nombre completo");
            etNombre.requestFocus();
            mostrarSnackbarError(getString(R.string.error_campos_vacios));
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo electrónico");
            etEmail.requestFocus();
            mostrarSnackbarError(getString(R.string.error_campos_vacios));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo electrónico inválido");
            etEmail.requestFocus();
            mostrarSnackbarError(getString(R.string.error_email_invalido));
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < MIN_PASSWORD_LENGTH) {
            etPassword.setError("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
            etPassword.requestFocus();
            mostrarSnackbarError(getString(R.string.error_password_corta));
            return false;
        }

        return true;
    }

    private void crearCuentaYRegistrarMascota(Repositorio repo, String nombre, String email, String password) {
        String salt = HashUtil.nuevoSalt();
        String passwordHasheada = HashUtil.hashear(password, salt);

        Usuario nuevoUsuario = Usuario.crear(nombre, email, passwordHasheada, salt);
        long usuarioId = repo.usuarios().insertar(nuevoUsuario);

        if (usuarioId <= 0) {
            mostrarSnackbarError(getString(R.string.error_email_duplicado));
            return;
        }

        otorgarRegalosBienvenida(repo, usuarioId, nombre);
        iniciarSesionYNavegar(email, salt, usuarioId);
    }

    private void otorgarRegalosBienvenida(Repositorio repo, long usuarioId, String nombreUsuario) {
        Mascota mascotaInicial = Mascota.crearPorDefecto(usuarioId, nombreUsuario);
        repo.mascotas().insertar(mascotaInicial);
        repo.mascotas().otorgarAlimento(usuarioId, ALIMENTO_INICIAL_ID, CANTIDAD_INICIAL_ALIMENTO);
        repo.mascotas().otorgarAccesorio(usuarioId, ACCESORIO_INICIAL_ID);
    }

    private void iniciarSesionYNavegar(String email, String salt, long usuarioId) {
        SemiologiaApp.getSesion().iniciarSesion(usuarioId, email, salt);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Garantiza obtener una vista válida para anclar la Snackbar
     */
    private View obtenerVistaAnchor() {
        View vista = findViewById(R.id.btnCrear);
        if (vista == null) {
            vista = findViewById(android.R.id.content);
        }
        return vista;
    }

    /**
     * Muestra una Snackbar flotante de error (Color Rojo #EF4444).
     */
    private void mostrarSnackbarError(String mensaje) {
        View vistaBase = obtenerVistaAnchor();
        Snackbar snackbar = Snackbar.make(vistaBase, mensaje, Snackbar.LENGTH_LONG);

        View snackView = snackbar.getView();
        snackView.setBackgroundColor(Color.parseColor("#EF4444")); // Rojo Material

        TextView textView = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }

    /**
     * Muestra una Snackbar flotante informativa/éxito (Color Morado #6366F1).
     */
    private void mostrarSnackbarExito(String mensaje) {
        View vistaBase = obtenerVistaAnchor();
        Snackbar snackbar = Snackbar.make(vistaBase, mensaje, Snackbar.LENGTH_LONG);

        View snackView = snackbar.getView();
        snackView.setBackgroundColor(Color.parseColor("#6366F1")); // Morado PsicoQuiz

        TextView textView = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }
}
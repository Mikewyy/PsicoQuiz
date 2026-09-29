package com.utm.semiologia.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
 * Al crear la cuenta se otorga automáticamente una mascota (regla 1 usuario
 * -> 1 mascota) y 3 galletas de partida, para que el jugador pueda usar
 * la mecánica de comida desde el primer día.
 */
public class RegistroActivity extends AppCompatActivity {

    private static final long ALIMENTO_INICIAL = 1L; // Galleta
    private static final int CANTIDAD_INICIAL  = 3;

    private EditText etNombre, etEmail, etPassword;
    private Button btnCrear;
    private TextView linkLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etNombre   = findViewById(R.id.etNombre);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnCrear   = findViewById(R.id.btnCrear);
        linkLogin  = findViewById(R.id.linkLogin);

        btnCrear.setOnClickListener(v -> intentarRegistro());
        linkLogin.setOnClickListener(v -> finish());
    }

    private void intentarRegistro() {
        String nombre   = etNombre.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_campos_vacios, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_email_invalido, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_password_corta, Toast.LENGTH_SHORT).show();
            return;
        }

        Repositorio repo = SemiologiaApp.getRepositorio();
        if (repo.usuarios().buscarPorEmail(email) != null) {
            Toast.makeText(this, R.string.error_email_duplicado, Toast.LENGTH_SHORT).show();
            return;
        }

        String salt = HashUtil.nuevoSalt();
        Usuario u = Usuario.crear(nombre, email, HashUtil.hashear(password, salt), salt);
        long usuarioId = repo.usuarios().insertar(u);

        if (usuarioId <= 0) {
            Toast.makeText(this, R.string.error_email_duplicado, Toast.LENGTH_SHORT).show();
            return;
        }

        // Regalos de bienvenida
        Mascota m = Mascota.crearPorDefecto(usuarioId, nombre);
        repo.mascotas().insertar(m);
        repo.mascotas().otorgarAlimento(usuarioId, ALIMENTO_INICIAL, CANTIDAD_INICIAL);
        repo.mascotas().otorgarAccesorio(usuarioId, 1L);   // Collar básico

        SemiologiaApp.getSesion().iniciarSesion(usuarioId, email, salt);

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

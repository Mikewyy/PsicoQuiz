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
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.ui.dashboard.MainActivity;
import com.utm.semiologia.util.HashUtil;

/** Inicio de sesión de estudiantes. */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnIngresar;
    private TextView linkRegistro;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya hay sesión abierta, no mostrar el login.
        if (SemiologiaApp.getSesion().haySesion()) {
            irAlDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnIngresar = findViewById(R.id.btnIngresar);
        linkRegistro = findViewById(R.id.linkRegistro);

        btnIngresar.setOnClickListener(v -> intentarIngreso());
        linkRegistro.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));
    }

    private void intentarIngreso() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_campos_vacios, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_email_invalido, Toast.LENGTH_SHORT).show();
            return;
        }

        Repositorio repo = SemiologiaApp.getRepositorio();
        Usuario u = repo.usuarios().buscarPorEmail(email);

        // El usuario escribe mal el correo con frecuencia, asi que conviene
        // distinguir "no existe" de "existe pero la clave no coincide".
        if (u == null) {
            Toast.makeText(this, R.string.error_usuario_no_registrado, Toast.LENGTH_LONG).show();
            etEmail.setError(getString(R.string.error_email_no_registrado));
            return;
        }

        if (!HashUtil.verificar(password, u.getPasswordSalt(), u.getPasswordHash())) {
            Toast.makeText(this, R.string.error_password_incorrecta, Toast.LENGTH_SHORT).show();
            etPassword.setText("");
            return;
        }

        SemiologiaApp.getSesion().iniciarSesion(u.getId(), u.getEmail(), u.getPasswordSalt());
        irAlDashboard();
    }

    private void irAlDashboard() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

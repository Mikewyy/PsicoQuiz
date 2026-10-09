package com.utm.semiologia.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        CheckBox cbRecordar = findViewById(R.id.cbRecordar);

        btnIngresar.setOnClickListener(v -> intentarIngreso(cbRecordar.isChecked()));
        linkRegistro.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));


        ImageView ivTogglePassword= findViewById(R.id.ivTogglePassword);
        configurarTogglePassword(etPassword, ivTogglePassword);

        configurarTeclado();
    }


    /** Mantiene visible el campo que se escribe cuando el teclado se abre. */
    private void configurarTeclado() {
        ScrollView scroll = findViewById(R.id.scrollLogin);
        View contenido = scroll.getChildAt(0);
        final int paddingTopBase = contenido.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int top = paddingTopBase + bars.top;
            contenido.setPadding(0, top, 0, ime.bottom);

            if (ime.bottom > 0) {
                revelarCampoEnfocado(scroll, ime.bottom);
            }
            return insets;
        });

        View.OnFocusChangeListener alEnfocar = (v, hasFocus) -> {
            if (!hasFocus || v.getRootWindowInsets() == null) {
                return;
            }
            int ime = WindowInsetsCompat.toWindowInsetsCompat(v.getRootWindowInsets())
                    .getInsets(WindowInsetsCompat.Type.ime()).bottom;
            if (ime > 0) {
                revelarCampoEnfocado(scroll, ime);
            }
        };
        etEmail.setOnFocusChangeListener(alEnfocar);
        etPassword.setOnFocusChangeListener(alEnfocar);
    }


    private void revelarCampoEnfocado(ScrollView scroll, int alturaTeclado) {
        View enfocado = getCurrentFocus();
        if (enfocado == null) {
            return;
        }

        int[] pos = new int[2];
        enfocado.getLocationOnScreen(pos);
        int campoAbajo = pos[1] + enfocado.getHeight();

        int[] posScroll = new int[2];
        scroll.getLocationOnScreen(posScroll);
        int visibleAbajo = posScroll[1] + scroll.getHeight() - alturaTeclado;

        int margen = (int) (16 * getResources().getDisplayMetrics().density);
        int distancia = (campoAbajo - visibleAbajo) + margen;
        if (distancia > 0) {
            scroll.smoothScrollBy(0, distancia);
        }
    }


    private void configurarTogglePassword(EditText editText, ImageView imageView) {
        imageView.setOnClickListener(v -> {
            // Verifica si la contraseña actualmente está oculta
            if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Mostrar contraseña
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                imageView.setImageResource(R.drawable.ic_ojo); // Tu ícono de ojo abierto
            } else {
                // Ocultar contraseña
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                imageView.setImageResource(R.drawable.ic_ojo_cerrado); // Tu ícono de ojo cerrado
            }

            // Mueve el cursor al final del texto para que el usuario pueda seguir escribiendo cómodamente
            editText.setSelection(editText.getText().length());
        });
    }



    private void intentarIngreso(boolean recordarSesion) {
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

        SemiologiaApp.getSesion().iniciarSesion(
                u.getId(),
                u.getEmail(),
                u.getPasswordSalt(),
                recordarSesion
        );
        irAlDashboard();
    }

    private void irAlDashboard() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.firebase.FirebaseAuthManager;
import com.utm.semiologia.firebase.FirebaseProfileSyncManager;
import com.utm.semiologia.firebase.FirebaseProgressSyncManager;
import com.utm.semiologia.firebase.FirebaseSecondarySyncManager;
import com.utm.semiologia.ui.dashboard.MainActivity;
import com.utm.semiologia.util.HashUtil;

/**
 * Registro de un estudiante nuevo.
 *
 * Firebase Authentication:
 * - Vincula la sesión anónima existente con Email/Password cuando es posible.
 * - Si no existe sesión anónima, crea una cuenta Email/Password.
 *
 * SQLite local:
 * - Usuario
 * - Mascota inicial
 * - 3 alimentos
 * - Accesorio inicial
 */
public class RegistroActivity extends AppCompatActivity {

    private static final long ALIMENTO_INICIAL_ID = 1L;
    private static final int CANTIDAD_INICIAL_ALIMENTO = 3;
    private static final long ACCESORIO_INICIAL_ID = 1L;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private EditText etNombre;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnCrear;
    private ImageButton btnVerPassword;
    private TextView linkLogin;

    private boolean passwordVisible = false;
    private boolean registroEnProceso = false;

    private FirebaseAuthManager firebaseAuthManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registro);

        firebaseAuthManager = new FirebaseAuthManager();

        enlazarVistas();
        setupListeners();
        configurarTeclado();
    }

    /** Mantiene visible el campo que se escribe cuando el teclado se abre. */
    private void configurarTeclado() {
        ScrollView scroll = findViewById(R.id.scrollRegistro);
        View contenido = scroll.getChildAt(0);

        final int paddingTopBase = contenido.getPaddingTop();
        final int paddingBottomBase = contenido.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                scroll,
                (v, insets) -> {
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    int top = paddingTopBase + bars.top;
                    int bottom = ime.bottom > 0 ? ime.bottom : paddingBottomBase;

                    contenido.setPadding(0, top, 0, bottom);

                    if (ime.bottom > 0) {
                        revelarCampoEnfocado(scroll, ime.bottom);
                    }

                    return insets;
                }
        );

        View.OnFocusChangeListener alEnfocar =
                (v, hasFocus) -> {
                    if (!hasFocus || v.getRootWindowInsets() == null) {
                        return;
                    }

                    int ime = WindowInsetsCompat
                            .toWindowInsetsCompat(v.getRootWindowInsets())
                            .getInsets(WindowInsetsCompat.Type.ime())
                            .bottom;

                    if (ime > 0) {
                        revelarCampoEnfocado(scroll, ime);
                    }
                };

        etNombre.setOnFocusChangeListener(alEnfocar);
        etEmail.setOnFocusChangeListener(alEnfocar);
        etPassword.setOnFocusChangeListener(alEnfocar);
    }

    private void revelarCampoEnfocado(
            ScrollView scroll,
            int alturaTeclado
    ) {
        View enfocado = getCurrentFocus();

        if (enfocado == null) {
            return;
        }

        int[] pos = new int[2];
        enfocado.getLocationOnScreen(pos);

        int campoAbajo = pos[1] + enfocado.getHeight();

        int[] posScroll = new int[2];
        scroll.getLocationOnScreen(posScroll);

        int visibleAbajo =
                posScroll[1]
                        + scroll.getHeight()
                        - alturaTeclado;

        int margen =
                (int) (
                        16
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        int distancia =
                (campoAbajo - visibleAbajo)
                        + margen;

        if (distancia > 0) {
            scroll.smoothScrollBy(0, distancia);
        }
    }

    private void enlazarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnCrear = findViewById(R.id.btnCrear);
        btnVerPassword = findViewById(R.id.btnVerPassword);
        linkLogin = findViewById(R.id.linkLogin);
    }

    private void setupListeners() {
        btnCrear.setOnClickListener(v -> intentarRegistro());

        btnVerPassword.setOnClickListener(
                v -> alternarVisibilidadPassword()
        );

        linkLogin.setOnClickListener(
                v -> {
                    if (!registroEnProceso) {
                        confirmarSalida();
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        if (registroEnProceso) {
            mostrarSnackbarError(
                    "Espera mientras se crea tu cuenta."
            );
            return;
        }

        confirmarSalida();
    }

    private void confirmarSalida() {
        View vistaBase = obtenerVistaAnchor();

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

        View snackView = snackbar.getView();

        snackView.setBackgroundColor(
                Color.parseColor("#1E293B")
        );

        TextView textView =
                snackView.findViewById(
                        com.google.android.material.R.id.snackbar_text
                );

        if (textView != null) {
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }

    // =========================================================
    // REGISTRO
    // =========================================================

    private void intentarRegistro() {
        if (registroEnProceso) {
            return;
        }

        String nombre =
                etNombre
                        .getText()
                        .toString()
                        .trim();

        String email =
                etEmail
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        String password =
                etPassword
                        .getText()
                        .toString();

        if (!validarEntradas(nombre, email, password)) {
            return;
        }

        Repositorio repo =
                SemiologiaApp.getRepositorio();

        /*
         * Evitamos crear en Firebase una cuenta cuyo correo
         * ya pertenece a un usuario local.
         */
        if (
                repo.usuarios()
                        .buscarPorEmail(email)
                        != null
        ) {
            mostrarSnackbarError(
                    getString(R.string.error_email_duplicado)
            );
            return;
        }

        registrarEnFirebase(
                repo,
                nombre,
                email,
                password
        );
    }

    private void registrarEnFirebase(
            Repositorio repo,
            String nombre,
            String email,
            String password
    ) {
        establecerRegistroEnProceso(true);

        firebaseAuthManager.registrarConEmail(
                email,
                password,
                new FirebaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(
                            FirebaseUser usuario
                    ) {
                        /*
                         * Firebase ya confirmó la identidad.
                         * Ahora conservamos la estructura local
                         * que utiliza el resto de PsicoQuiz.
                         */
                        crearCuentaYRegistrarMascota(
                                repo,
                                nombre,
                                email,
                                password,
                                usuario
                        );
                    }

                    @Override
                    public void onError(
                            Exception error
                    ) {
                        establecerRegistroEnProceso(false);
                        manejarErrorFirebase(error);
                    }
                }
        );
    }

    private void establecerRegistroEnProceso(
            boolean enProceso
    ) {
        registroEnProceso = enProceso;

        btnCrear.setEnabled(!enProceso);
        btnVerPassword.setEnabled(!enProceso);
        etNombre.setEnabled(!enProceso);
        etEmail.setEnabled(!enProceso);
        etPassword.setEnabled(!enProceso);
        linkLogin.setEnabled(!enProceso);

        btnCrear.setText(
                enProceso
                        ? "Creando cuenta..."
                        : "Crear cuenta"
        );
    }

    private void manejarErrorFirebase(
            Exception error
    ) {
        if (
                error
                        instanceof FirebaseAuthUserCollisionException
        ) {
            etEmail.setError(
                    "Este correo ya tiene una cuenta"
            );
            etEmail.requestFocus();

            mostrarSnackbarError(
                    "Ya existe una cuenta registrada con este correo."
            );
            return;
        }

        if (
                error
                        instanceof FirebaseAuthInvalidCredentialsException
        ) {
            etEmail.setError(
                    "Revisa el correo electrónico"
            );
            etEmail.requestFocus();

            mostrarSnackbarError(
                    "El correo electrónico no es válido."
            );
            return;
        }

        String detalle =
                error != null
                        ? error.getMessage()
                        : null;

        if (
                detalle != null
                        && !detalle.trim().isEmpty()
        ) {
            mostrarSnackbarError(
                    "Firebase: " + detalle
            );
        } else {
            mostrarSnackbarError(
                    "No se pudo crear la cuenta. Revisa tu conexión e intenta nuevamente."
            );
        }
    }

    // =========================================================
    // VALIDACIONES
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
                    getString(R.string.ocultar_contrasena)
            );

        } else {
            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            etPassword.setSelection(
                    etPassword.getText().length()
            );

            btnVerPassword.setImageResource(
                    R.drawable.ic_ojo
            );

            btnVerPassword.setContentDescription(
                    getString(R.string.ver_contrasena)
            );
        }
    }

    private boolean validarEntradas(
            String nombre,
            String email,
            String password
    ) {
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError(
                    "Ingresa tu nombre completo"
            );
            etNombre.requestFocus();

            mostrarSnackbarError(
                    getString(R.string.error_campos_vacios)
            );
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(
                    "Ingresa tu correo electrónico"
            );
            etEmail.requestFocus();

            mostrarSnackbarError(
                    getString(R.string.error_campos_vacios)
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
                    getString(R.string.error_email_invalido)
            );
            return false;
        }

        if (
                TextUtils.isEmpty(password)
                        || password.length()
                        < MIN_PASSWORD_LENGTH
        ) {
            etPassword.setError(
                    "La contraseña debe tener al menos "
                            + MIN_PASSWORD_LENGTH
                            + " caracteres"
            );
            etPassword.requestFocus();

            mostrarSnackbarError(
                    getString(R.string.error_password_corta)
            );
            return false;
        }

        return true;
    }

    // =========================================================
    // SQLITE: USUARIO + MASCOTA
    // =========================================================

    private void crearCuentaYRegistrarMascota(
            Repositorio repo,
            String nombre,
            String email,
            String password,
            FirebaseUser usuarioFirebase
    ) {
        String salt =
                HashUtil.nuevoSalt();

        String passwordHasheada =
                HashUtil.hashear(
                        password,
                        salt
                );

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
            /*
             * Firebase ya creó/vinculó la cuenta.
             * No intentamos borrarla automáticamente aquí:
             * eliminar una cuenta vinculada que antes era
             * anónima podría destruir su identidad Firebase.
             */
            establecerRegistroEnProceso(false);

            mostrarSnackbarError(
                    "La cuenta se creó en Firebase, pero no se pudo crear el perfil local."
            );
            return;
        }

        boolean mascotaCreada =
                otorgarRegalosBienvenida(
                        repo,
                        usuarioId,
                        nombre
                );

        if (!mascotaCreada) {
            establecerRegistroEnProceso(false);

            mostrarSnackbarError(
                    "La cuenta se creó, pero no se pudo crear la mascota inicial."
            );
            return;
        }

        boolean uidVinculado =
                repo.usuarios().vincularFirebaseUid(
                        usuarioId,
                        usuarioFirebase.getUid()
                );

        if (!uidVinculado) {
            establecerRegistroEnProceso(false);
            mostrarSnackbarError(
                    "La cuenta se creó, pero no se pudo vincular el perfil local con Firebase."
            );
            return;
        }

        Usuario usuarioLocal =
                repo.usuarios().buscarPorId(usuarioId);

        Mascota mascotaLocal =
                repo.mascotas().obtener(usuarioId);

        if (usuarioLocal == null) {
            establecerRegistroEnProceso(false);
            mostrarSnackbarError(
                    "La cuenta se creó, pero no se pudo recuperar el perfil local para sincronizarlo."
            );
            return;
        }

        /*
         * Primera copia del perfil en Firestore. Perfil + mascota se guardan
         * en un único documento para reducir escrituras del plan gratuito.
         */
        new FirebaseProfileSyncManager().subirPerfilLocal(
                usuarioFirebase,
                usuarioLocal,
                mascotaLocal,
                new FirebaseProfileSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // Fase 2: crea también la primera copia de progreso e inventarios.
                        new FirebaseProgressSyncManager(RegistroActivity.this).subirAhora(
                                usuarioId,
                                new FirebaseProgressSyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        sincronizarFase3YNavegar(email, salt, usuarioId);
                                    }

                                    @Override
                                    public void onError(@androidx.annotation.NonNull Exception error) {
                                        mostrarSnackbarError(
                                                "Cuenta creada. El progreso cloud se sincronizará después."
                                        );
                                        iniciarSesionYNavegar(email, salt, usuarioId);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        /*
                         * La cuenta Firebase y el perfil SQLite ya existen.
                         * No destruimos datos por un fallo temporal de red o
                         * reglas de Firestore; se podrá reintentar después.
                         */
                        mostrarSnackbarError(
                                "Cuenta creada. No se pudo sincronizar la nube en este momento."
                        );
                        iniciarSesionYNavegar(email, salt, usuarioId);
                    }
                }
        );
    }

    private void sincronizarFase3YNavegar(
            String email,
            String salt,
            long usuarioId
    ) {
        new FirebaseSecondarySyncManager(this).subirTodoLocal(
                usuarioId,
                new FirebaseSecondarySyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        iniciarSesionYNavegar(email, salt, usuarioId);
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception error) {
                        mostrarSnackbarError(
                                "Cuenta creada. Notas, desafío y Pomodoro se sincronizarán después."
                        );
                        iniciarSesionYNavegar(email, salt, usuarioId);
                    }
                }
        );
    }

    private boolean otorgarRegalosBienvenida(
            Repositorio repo,
            long usuarioId,
            String nombreUsuario
    ) {
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

        if (mascotaId <= 0) {
            return false;
        }

        repo.mascotas()
                .otorgarAlimento(
                        usuarioId,
                        ALIMENTO_INICIAL_ID,
                        CANTIDAD_INICIAL_ALIMENTO
                );

        repo.mascotas()
                .otorgarAccesorio(
                        usuarioId,
                        ACCESORIO_INICIAL_ID
                );

        return true;
    }

    // =========================================================
    // SESIÓN Y NAVEGACIÓN
    // =========================================================

    private void iniciarSesionYNavegar(
            String email,
            String salt,
            long usuarioId
    ) {
        establecerRegistroEnProceso(false);

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
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // =========================================================
    // SNACKBARS
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
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }

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
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(14);
        }

        snackbar.show();
    }
}

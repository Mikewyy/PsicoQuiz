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

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
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
 * Inicio de sesión de estudiantes.
 *
 * Firebase Authentication valida:
 * - Correo electrónico
 * - Contraseña
 *
 * SQLite conserva:
 * - ID local
 * - Perfil
 * - Mascota
 * - Progreso
 * - Racha
 * - Datos de gamificación
 */
public class LoginActivity extends AppCompatActivity {

    // =========================================================
    // VISTAS
    // =========================================================

    private EditText etEmail;
    private EditText etPassword;

    private Button btnIngresar;

    private TextView linkRegistro;

    private CheckBox cbRecordar;

    private ImageView ivTogglePassword;


    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuthManager firebaseAuthManager;


    // =========================================================
    // ESTADO
    // =========================================================

    /**
     * Evita ejecutar dos intentos de inicio de sesión
     * simultáneamente.
     */
    private boolean loginEnProceso = false;


    // =========================================================
    // CREACIÓN
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);


        // -----------------------------------------------------
        // SESIÓN LOCAL EXISTENTE
        // -----------------------------------------------------

        /*
         * La sesión principal de PsicoQuiz sigue utilizando
         * SesionManager porque el resto de la aplicación
         * necesita el usuarioId local.
         */
        if (
                SemiologiaApp
                        .getSesion()
                        .haySesion()
        ) {

            irAlDashboard();

            return;
        }


        setContentView(
                R.layout.activity_login
        );


        // -----------------------------------------------------
        // FIREBASE
        // -----------------------------------------------------

        firebaseAuthManager =
                new FirebaseAuthManager();


        // -----------------------------------------------------
        // VISTAS
        // -----------------------------------------------------

        enlazarVistas();


        // -----------------------------------------------------
        // LISTENERS
        // -----------------------------------------------------

        configurarListeners();

        configurarTogglePassword(
                etPassword,
                ivTogglePassword
        );

        configurarTeclado();
    }


    // =========================================================
    // ENLAZAR VISTAS
    // =========================================================

    private void enlazarVistas() {

        etEmail =
                findViewById(
                        R.id.etEmail
                );

        etPassword =
                findViewById(
                        R.id.etPassword
                );

        btnIngresar =
                findViewById(
                        R.id.btnIngresar
                );

        linkRegistro =
                findViewById(
                        R.id.linkRegistro
                );

        cbRecordar =
                findViewById(
                        R.id.cbRecordar
                );

        ivTogglePassword =
                findViewById(
                        R.id.ivTogglePassword
                );
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {

        btnIngresar.setOnClickListener(
                v -> intentarIngreso(
                        cbRecordar.isChecked()
                )
        );


        linkRegistro.setOnClickListener(
                v -> {

                    if (loginEnProceso) {
                        return;
                    }


                    startActivity(
                            new Intent(
                                    LoginActivity.this,
                                    RegistroActivity.class
                            )
                    );
                }
        );
    }


    // =========================================================
    // TECLADO
    // =========================================================

    /**
     * Mantiene visible el campo que se escribe cuando
     * el teclado se abre.
     */
    private void configurarTeclado() {

        ScrollView scroll =
                findViewById(
                        R.id.scrollLogin
                );


        View contenido =
                scroll.getChildAt(0);


        final int paddingTopBase =
                contenido.getPaddingTop();


        ViewCompat.setOnApplyWindowInsetsListener(
                scroll,
                (v, insets) -> {

                    Insets ime =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.ime()
                            );


                    Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );


                    int top =
                            paddingTopBase
                                    + bars.top;


                    contenido.setPadding(
                            0,
                            top,
                            0,
                            ime.bottom
                    );


                    if (ime.bottom > 0) {

                        revelarCampoEnfocado(
                                scroll,
                                ime.bottom
                        );
                    }


                    return insets;
                }
        );


        View.OnFocusChangeListener alEnfocar =
                (v, hasFocus) -> {

                    if (
                            !hasFocus
                                    ||
                                    v.getRootWindowInsets() == null
                    ) {

                        return;
                    }


                    int ime =
                            WindowInsetsCompat
                                    .toWindowInsetsCompat(
                                            v.getRootWindowInsets()
                                    )
                                    .getInsets(
                                            WindowInsetsCompat.Type.ime()
                                    )
                                    .bottom;


                    if (ime > 0) {

                        revelarCampoEnfocado(
                                scroll,
                                ime
                        );
                    }
                };


        etEmail.setOnFocusChangeListener(
                alEnfocar
        );


        etPassword.setOnFocusChangeListener(
                alEnfocar
        );
    }


    private void revelarCampoEnfocado(
            ScrollView scroll,
            int alturaTeclado
    ) {

        View enfocado =
                getCurrentFocus();


        if (enfocado == null) {

            return;
        }


        int[] pos =
                new int[2];


        enfocado.getLocationOnScreen(
                pos
        );


        int campoAbajo =
                pos[1]
                        + enfocado.getHeight();


        int[] posScroll =
                new int[2];


        scroll.getLocationOnScreen(
                posScroll
        );


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

            scroll.smoothScrollBy(
                    0,
                    distancia
            );
        }
    }


    // =========================================================
    // MOSTRAR / OCULTAR CONTRASEÑA
    // =========================================================

    private void configurarTogglePassword(
            EditText editText,
            ImageView imageView
    ) {

        imageView.setOnClickListener(
                v -> {

                    if (
                            editText.getTransformationMethod()
                                    instanceof PasswordTransformationMethod
                    ) {

                        // Mostrar contraseña
                        editText.setTransformationMethod(
                                HideReturnsTransformationMethod
                                        .getInstance()
                        );


                        imageView.setImageResource(
                                R.drawable.ic_ojo
                        );

                    } else {

                        // Ocultar contraseña
                        editText.setTransformationMethod(
                                PasswordTransformationMethod
                                        .getInstance()
                        );


                        imageView.setImageResource(
                                R.drawable.ic_ojo_cerrado
                        );
                    }


                    editText.setSelection(
                            editText
                                    .getText()
                                    .length()
                    );
                }
        );
    }


    // =========================================================
    // INTENTAR LOGIN
    // =========================================================

    private void intentarIngreso(
            boolean recordarSesion
    ) {

        if (loginEnProceso) {

            return;
        }


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


        // -----------------------------------------------------
        // CAMPOS VACÍOS
        // -----------------------------------------------------

        if (
                TextUtils.isEmpty(email)
                        ||
                        TextUtils.isEmpty(password)
        ) {

            Toast.makeText(
                    this,
                    R.string.error_campos_vacios,
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        // -----------------------------------------------------
        // EMAIL VÁLIDO
        // -----------------------------------------------------

        if (
                !Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
        ) {

            Toast.makeText(
                    this,
                    R.string.error_email_invalido,
                    Toast.LENGTH_SHORT
            ).show();


            etEmail.setError(
                    getString(
                            R.string.error_email_invalido
                    )
            );


            return;
        }


        // -----------------------------------------------------
        // PERFIL LOCAL (PUEDE NO EXISTIR EN ESTE DISPOSITIVO)
        // -----------------------------------------------------

        Repositorio repo = SemiologiaApp.getRepositorio();
        Usuario usuarioLocal = repo.usuarios().buscarPorEmail(email);

        /*
         * Fase 1C:
         * Firebase Authentication se consulta SIEMPRE, aunque SQLite todavía
         * no tenga el perfil. Así una cuenta puede entrar en un teléfono nuevo
         * y recuperar su perfil desde Firestore.
         */
        iniciarSesionFirebase(
                usuarioLocal,
                email,
                password,
                recordarSesion
        );
    }


    // =========================================================
    // LOGIN FIREBASE
    // =========================================================

    private void iniciarSesionFirebase(
            @Nullable Usuario usuarioLocal,
            String email,
            String password,
            boolean recordarSesion
    ) {

        establecerLoginEnProceso(true);

        firebaseAuthManager.iniciarSesionConEmail(
                email,
                password,
                new FirebaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser usuarioFirebase) {
                        resolverPerfilTrasAutenticacion(
                                usuarioFirebase,
                                usuarioLocal,
                                password,
                                recordarSesion
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        establecerLoginEnProceso(false);
                        manejarErrorFirebase(error);
                    }
                }
        );
    }


    // =========================================================
    // RESOLVER PERFIL LOCAL O RESTAURAR DESDE FIRESTORE
    // =========================================================

    private void resolverPerfilTrasAutenticacion(
            FirebaseUser usuarioFirebase,
            @Nullable Usuario usuarioLocalPorEmail,
            String password,
            boolean recordarSesion
    ) {
        Repositorio repo = SemiologiaApp.getRepositorio();

        // El UID tiene prioridad sobre el correo porque es la identidad estable.
        Usuario usuarioLocal = repo.usuarios().buscarPorFirebaseUid(
                usuarioFirebase.getUid()
        );

        if (usuarioLocal == null) {
            usuarioLocal = usuarioLocalPorEmail;
        }

        if (usuarioLocal != null) {
            vincularYSincronizarPerfil(
                    usuarioFirebase,
                    usuarioLocal,
                    recordarSesion
            );
            return;
        }

        // No hay perfil SQLite: estamos en una instalación/dispositivo nuevo.
        restaurarPerfilDesdeNube(
                usuarioFirebase,
                password,
                recordarSesion
        );
    }


    // =========================================================
    // PERFIL LOCAL EXISTENTE -> VINCULAR + SUBIR
    // =========================================================

    private void vincularYSincronizarPerfil(
            FirebaseUser usuarioFirebase,
            Usuario usuarioLocal,
            boolean recordarSesion
    ) {
        Repositorio repo = SemiologiaApp.getRepositorio();

        boolean vinculado = repo.usuarios().vincularFirebaseUid(
                usuarioLocal.getId(),
                usuarioFirebase.getUid()
        );

        if (!vinculado) {
            establecerLoginEnProceso(false);
            Toast.makeText(
                    this,
                    "Firebase inició sesión, pero no se pudo vincular el perfil local.",
                    Toast.LENGTH_LONG
            ).show();
            firebaseAuthManager.cerrarSesion();
            return;
        }

        /*
         * IMPORTANTE: si este dispositivo ya tiene SQLite NO debemos subirlo
         * inmediatamente, porque podría ser una copia antigua y sobrescribir
         * cambios realizados en otro dispositivo. Firestore tiene prioridad
         * durante el login. Solo subimos SQLite si aún no existe perfil cloud.
         */
        new FirebaseProfileSyncManager().descargarPerfil(
                usuarioFirebase,
                new FirebaseProfileSyncManager.DownloadCallback() {
                    @Override
                    public void onSuccess(Usuario usuarioNube, @Nullable Mascota mascotaNube) {
                        boolean perfilAplicado = repo.usuarios().aplicarPerfilDesdeNube(
                                usuarioLocal.getId(),
                                usuarioNube
                        );

                        if (!perfilAplicado) {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Sesión iniciada. No se pudo actualizar el perfil local desde la nube.",
                                    Toast.LENGTH_LONG
                            ).show();
                            iniciarSesionLocal(usuarioLocal, recordarSesion);
                            return;
                        }

                        if (mascotaNube != null) {
                            repo.mascotas().aplicarDesdeNube(usuarioLocal.getId(), mascotaNube);
                        }

                        Usuario usuarioActualizado = repo.usuarios().buscarPorId(usuarioLocal.getId());
                        iniciarSesionLocal(
                                usuarioActualizado != null ? usuarioActualizado : usuarioLocal,
                                recordarSesion
                        );
                    }

                    @Override
                    public void onNotFound() {
                        // Primera sincronización de una instalación antigua.
                        Mascota mascota = repo.mascotas().obtener(usuarioLocal.getId());
                        new FirebaseProfileSyncManager().subirPerfilLocal(
                                usuarioFirebase,
                                usuarioLocal,
                                mascota,
                                new FirebaseProfileSyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        iniciarSesionLocal(usuarioLocal, recordarSesion);
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Sesión iniciada. No se pudo sincronizar la nube en este momento.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                        iniciarSesionLocal(usuarioLocal, recordarSesion);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        // No sobrescribimos Firestore con datos potencialmente antiguos
                        // cuando la descarga falla por red u otro error temporal.
                        Toast.makeText(
                                LoginActivity.this,
                                "Sesión iniciada. No se pudo actualizar el perfil desde la nube en este momento.",
                                Toast.LENGTH_LONG
                        ).show();
                        iniciarSesionLocal(usuarioLocal, recordarSesion);
                    }
                }
        );
    }


    // =========================================================
    // FIRESTORE -> CREAR PERFIL SQLITE EN DISPOSITIVO NUEVO
    // =========================================================

    private void restaurarPerfilDesdeNube(
            FirebaseUser usuarioFirebase,
            String password,
            boolean recordarSesion
    ) {
        new FirebaseProfileSyncManager().descargarPerfil(
                usuarioFirebase,
                new FirebaseProfileSyncManager.DownloadCallback() {
                    @Override
                    public void onSuccess(Usuario usuarioNube, @Nullable Mascota mascotaNube) {
                        Repositorio repo = SemiologiaApp.getRepositorio();

                        /*
                         * password_hash/password_salt son columnas heredadas de SQLite.
                         * La autenticación real ya la hizo Firebase; aun así generamos
                         * valores locales válidos para respetar el esquema NOT NULL.
                         * Estos datos NUNCA se suben a Firestore.
                         */
                        String saltLocal = HashUtil.nuevoSalt();
                        usuarioNube.setPasswordSalt(saltLocal);
                        usuarioNube.setPasswordHash(
                                HashUtil.hashear(password, saltLocal)
                        );

                        String emailFirebase = usuarioFirebase.getEmail();
                        if (emailFirebase != null && !emailFirebase.trim().isEmpty()) {
                            usuarioNube.setEmail(emailFirebase.trim().toLowerCase());
                        }

                        long usuarioId;
                        try {
                            usuarioId = repo.usuarios().insertar(usuarioNube);
                        } catch (Exception e) {
                            establecerLoginEnProceso(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Se encontró tu perfil en la nube, pero no se pudo crear la copia local.",
                                    Toast.LENGTH_LONG
                            ).show();
                            firebaseAuthManager.cerrarSesion();
                            return;
                        }

                        if (usuarioId <= 0) {
                            establecerLoginEnProceso(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Se encontró tu perfil en la nube, pero no se pudo crear la copia local.",
                                    Toast.LENGTH_LONG
                            ).show();
                            firebaseAuthManager.cerrarSesion();
                            return;
                        }

                        boolean uidVinculado = repo.usuarios().vincularFirebaseUid(
                                usuarioId,
                                usuarioFirebase.getUid()
                        );

                        if (!uidVinculado) {
                            establecerLoginEnProceso(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Se restauró el perfil, pero no se pudo vincular con Firebase.",
                                    Toast.LENGTH_LONG
                            ).show();
                            firebaseAuthManager.cerrarSesion();
                            return;
                        }

                        Mascota mascotaRestaurada = mascotaNube;
                        if (mascotaRestaurada == null) {
                            mascotaRestaurada = Mascota.crearPorDefecto(
                                    usuarioId,
                                    usuarioNube.getNombre()
                            );
                        } else {
                            mascotaRestaurada.setUsuarioId(usuarioId);
                            mascotaRestaurada.setId(0L);
                        }

                        try {
                            repo.mascotas().insertar(mascotaRestaurada);
                        } catch (Exception e) {
                            establecerLoginEnProceso(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Se restauró el usuario, pero no se pudo restaurar la mascota.",
                                    Toast.LENGTH_LONG
                            ).show();
                            firebaseAuthManager.cerrarSesion();
                            return;
                        }

                        Usuario usuarioRestaurado = repo.usuarios().buscarPorId(usuarioId);
                        if (usuarioRestaurado == null) {
                            establecerLoginEnProceso(false);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "No se pudo abrir el perfil restaurado.",
                                    Toast.LENGTH_LONG
                            ).show();
                            firebaseAuthManager.cerrarSesion();
                            return;
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                "Perfil recuperado desde la nube.",
                                Toast.LENGTH_SHORT
                        ).show();

                        iniciarSesionLocal(usuarioRestaurado, recordarSesion);
                    }

                    @Override
                    public void onNotFound() {
                        establecerLoginEnProceso(false);
                        Toast.makeText(
                                LoginActivity.this,
                                "La cuenta existe en Firebase, pero todavía no tiene un perfil guardado en la nube.",
                                Toast.LENGTH_LONG
                        ).show();
                        firebaseAuthManager.cerrarSesion();
                    }

                    @Override
                    public void onError(Exception error) {
                        establecerLoginEnProceso(false);
                        Toast.makeText(
                                LoginActivity.this,
                                "No se pudo recuperar tu perfil desde la nube. Revisa tu conexión e inténtalo otra vez.",
                                Toast.LENGTH_LONG
                        ).show();
                        firebaseAuthManager.cerrarSesion();
                    }
                }
        );
    }


    // =========================================================
    // SESIÓN LOCAL
    // =========================================================

    private void iniciarSesionLocal(
            Usuario usuario,
            boolean recordarSesion
    ) {

        /*
         * Fase 2: antes de abrir el Dashboard restauramos el progreso real
         * (secciones, niveles, puntos e inventarios). Si todavía no existe
         * una copia cloud, este mismo paso crea la primera desde SQLite.
         */
        new FirebaseProgressSyncManager(this).sincronizarAlEntrar(
                usuario.getId(),
                new FirebaseProgressSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        sincronizarFase3YFinalizar(usuario, recordarSesion);
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception error) {
                        // Un problema temporal de Firestore no bloquea el acceso local.
                        Toast.makeText(
                                LoginActivity.this,
                                "Sesión iniciada. El progreso cloud se sincronizará cuando vuelva la conexión.",
                                Toast.LENGTH_LONG
                        ).show();
                        finalizarSesionLocal(usuario, recordarSesion);
                    }
                }
        );
    }

    private void sincronizarFase3YFinalizar(
            Usuario usuario,
            boolean recordarSesion
    ) {
        new FirebaseSecondarySyncManager(this).sincronizarAlEntrar(
                usuario.getId(),
                new FirebaseSecondarySyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        finalizarSesionLocal(usuario, recordarSesion);
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception error) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Sesión iniciada. Notas, desafío y Pomodoro se sincronizarán cuando vuelva la conexión.",
                                Toast.LENGTH_LONG
                        ).show();
                        finalizarSesionLocal(usuario, recordarSesion);
                    }
                }
        );
    }

    private void finalizarSesionLocal(
            Usuario usuario,
            boolean recordarSesion
    ) {
        SemiologiaApp
                .getSesion()
                .iniciarSesion(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getPasswordSalt(),
                        recordarSesion
                );

        establecerLoginEnProceso(false);
        irAlDashboard();
    }

    // =========================================================
    // ERRORES FIREBASE
    // =========================================================

    private void manejarErrorFirebase(
            Exception error
    ) {

        /*
         * Dependiendo de la versión de Firebase Auth,
         * credenciales incorrectas pueden llegar como
         * InvalidCredentials.
         */
        if (
                error
                        instanceof FirebaseAuthInvalidCredentialsException
        ) {

            Toast.makeText(
                    this,
                    R.string.error_password_incorrecta,
                    Toast.LENGTH_SHORT
            ).show();


            etPassword.setText("");


            etPassword.requestFocus();


            return;
        }


        if (
                error
                        instanceof FirebaseAuthInvalidUserException
        ) {

            Toast.makeText(
                    this,
                    R.string.error_usuario_no_registrado,
                    Toast.LENGTH_LONG
            ).show();


            etEmail.setError(
                    getString(
                            R.string.error_email_no_registrado
                    )
            );


            return;
        }


        /*
         * Firebase puede ocultar deliberadamente si el correo
         * existe o si la contraseña es incorrecta.
         * Por eso usamos un mensaje general para los demás
         * errores de autenticación.
         */
        Toast.makeText(
                this,
                "No se pudo iniciar sesión. Revisa tu correo, contraseña y conexión.",
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // BLOQUEAR / DESBLOQUEAR LOGIN
    // =========================================================

    private void establecerLoginEnProceso(
            boolean enProceso
    ) {

        loginEnProceso =
                enProceso;


        btnIngresar.setEnabled(
                !enProceso
        );


        etEmail.setEnabled(
                !enProceso
        );


        etPassword.setEnabled(
                !enProceso
        );


        cbRecordar.setEnabled(
                !enProceso
        );


        ivTogglePassword.setEnabled(
                !enProceso
        );


        linkRegistro.setEnabled(
                !enProceso
        );


        btnIngresar.setText(
                enProceso
                        ? "Ingresando..."
                        : "Ingresar"
        );
    }


    // =========================================================
    // DASHBOARD
    // =========================================================

    private void irAlDashboard() {

        Intent i =
                new Intent(
                        this,
                        MainActivity.class
                );


        i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(
                i
        );


        finish();
    }
}
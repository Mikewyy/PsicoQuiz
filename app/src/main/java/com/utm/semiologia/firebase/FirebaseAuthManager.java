package com.utm.semiologia.firebase;

import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Administrador central de Firebase Authentication para PsicoQuiz.
 *
 * Permite:
 * - Autenticación anónima.
 * - Convertir una cuenta anónima en cuenta Email/Password.
 * - Crear cuentas Email/Password.
 * - Iniciar sesión con Email/Password.
 * - Cerrar sesión.
 * - Consultar el usuario Firebase actual.
 */
public class FirebaseAuthManager {

    private static final String TAG = "PsicoQuizFirebase";

    private final FirebaseAuth auth;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public FirebaseAuthManager() {

        auth = FirebaseAuth.getInstance();
    }


    // =========================================================
    // REGISTRO CON EMAIL Y CONTRASEÑA
    // =========================================================

    /**
     * Registra al usuario utilizando correo y contraseña.
     *
     * Si actualmente existe una sesión Firebase anónima,
     * intenta vincular Email/Password a esa cuenta para
     * conservar el mismo UID.
     *
     * Si no existe una sesión anónima, crea una cuenta
     * Firebase nueva normalmente.
     */
    public void registrarConEmail(
            String email,
            String password,
            AuthCallback callback
    ) {

        // -----------------------------------------------------
        // VALIDACIONES
        // -----------------------------------------------------

        if (
                email == null
                        || email.trim().isEmpty()
        ) {

            enviarError(
                    callback,
                    new IllegalArgumentException(
                            "El correo electrónico es obligatorio."
                    )
            );

            return;
        }


        if (
                password == null
                        || password.isEmpty()
        ) {

            enviarError(
                    callback,
                    new IllegalArgumentException(
                            "La contraseña es obligatoria."
                    )
            );

            return;
        }


        String correoLimpio =
                email.trim().toLowerCase();


        FirebaseUser usuarioActual =
                auth.getCurrentUser();


        // =====================================================
        // CASO 1:
        // EXISTE UNA CUENTA ANÓNIMA
        // =====================================================

        if (
                usuarioActual != null
                        && usuarioActual.isAnonymous()
        ) {

            vincularCuentaAnonima(
                    usuarioActual,
                    correoLimpio,
                    password,
                    callback
            );

            return;
        }


        // =====================================================
        // CASO 2:
        // NO EXISTE SESIÓN FIREBASE
        // =====================================================

        if (usuarioActual == null) {

            crearCuentaNueva(
                    correoLimpio,
                    password,
                    callback
            );

            return;
        }


        // =====================================================
        // CASO 3:
        // YA HAY UN USUARIO EMAIL/PASSWORD AUTENTICADO
        // =====================================================

        /*
         * Esto puede ocurrir durante pruebas si quedó una
         * sesión Firebase anterior abierta.
         *
         * Cerramos solamente la sesión Firebase anterior
         * antes de crear la nueva cuenta.
         */
        auth.signOut();


        crearCuentaNueva(
                correoLimpio,
                password,
                callback
        );
    }


    // =========================================================
    // VINCULAR CUENTA ANÓNIMA
    // =========================================================

    /**
     * Convierte la cuenta Firebase anónima actual en una
     * cuenta con Email/Password.
     *
     * De esta forma se conserva el UID Firebase.
     */
    private void vincularCuentaAnonima(
            FirebaseUser usuarioAnonimo,
            String email,
            String password,
            AuthCallback callback
    ) {

        AuthCredential credential =
                EmailAuthProvider.getCredential(
                        email,
                        password
                );


        String uidAnterior =
                usuarioAnonimo.getUid();


        usuarioAnonimo
                .linkWithCredential(
                        credential
                )
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser usuario =
                                        auth.getCurrentUser();


                                if (usuario != null) {

                                    Log.d(
                                            TAG,
                                            "Cuenta anónima vinculada con Email/Password. "
                                                    + "UID conservado: "
                                                    + usuario.getUid()
                                    );


                                    if (callback != null) {

                                        callback.onSuccess(
                                                usuario
                                        );
                                    }

                                } else {

                                    enviarError(
                                            callback,
                                            new IllegalStateException(
                                                    "Firebase vinculó la cuenta, "
                                                            + "pero no devolvió el usuario."
                                            )
                                    );
                                }

                            } else {

                                Exception error =
                                        task.getException();


                                Log.e(
                                        TAG,
                                        "Error al vincular la cuenta anónima.",
                                        error
                                );


                                enviarError(
                                        callback,
                                        error
                                );
                            }
                        }
                );
    }


    // =========================================================
    // CREAR CUENTA NUEVA
    // =========================================================

    /**
     * Crea una cuenta Firebase nueva con Email/Password.
     */
    private void crearCuentaNueva(
            String email,
            String password,
            AuthCallback callback
    ) {

        auth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser usuario =
                                        auth.getCurrentUser();


                                if (usuario != null) {

                                    Log.d(
                                            TAG,
                                            "Usuario registrado con Email/Password. UID: "
                                                    + usuario.getUid()
                                    );


                                    if (callback != null) {

                                        callback.onSuccess(
                                                usuario
                                        );
                                    }

                                } else {

                                    enviarError(
                                            callback,
                                            new IllegalStateException(
                                                    "Firebase creó la cuenta, "
                                                            + "pero no devolvió el usuario."
                                            )
                                    );
                                }

                            } else {

                                Exception error =
                                        task.getException();


                                Log.e(
                                        TAG,
                                        "Error al crear usuario con Email/Password.",
                                        error
                                );


                                enviarError(
                                        callback,
                                        error
                                );
                            }
                        }
                );
    }


    // =========================================================
    // LOGIN CON EMAIL Y CONTRASEÑA
    // =========================================================

    /**
     * Inicia sesión utilizando correo electrónico
     * y contraseña.
     */
    public void iniciarSesionConEmail(
            String email,
            String password,
            AuthCallback callback
    ) {

        // -----------------------------------------------------
        // VALIDACIONES
        // -----------------------------------------------------

        if (
                email == null
                        || email.trim().isEmpty()
        ) {

            enviarError(
                    callback,
                    new IllegalArgumentException(
                            "El correo electrónico es obligatorio."
                    )
            );

            return;
        }


        if (
                password == null
                        || password.isEmpty()
        ) {

            enviarError(
                    callback,
                    new IllegalArgumentException(
                            "La contraseña es obligatoria."
                    )
            );

            return;
        }


        String correoLimpio =
                email.trim().toLowerCase();


        // -----------------------------------------------------
        // LOGIN FIREBASE
        // -----------------------------------------------------

        auth.signInWithEmailAndPassword(
                        correoLimpio,
                        password
                )
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser usuario =
                                        auth.getCurrentUser();


                                if (usuario != null) {

                                    Log.d(
                                            TAG,
                                            "Sesión Email/Password iniciada. UID: "
                                                    + usuario.getUid()
                                    );


                                    if (callback != null) {

                                        callback.onSuccess(
                                                usuario
                                        );
                                    }

                                } else {

                                    enviarError(
                                            callback,
                                            new IllegalStateException(
                                                    "Firebase inició sesión, "
                                                            + "pero no devolvió el usuario."
                                            )
                                    );
                                }

                            } else {

                                Exception error =
                                        task.getException();


                                Log.e(
                                        TAG,
                                        "Error al iniciar sesión con Email/Password.",
                                        error
                                );


                                enviarError(
                                        callback,
                                        error
                                );
                            }
                        }
                );
    }


    // =========================================================
    // AUTENTICACIÓN ANÓNIMA
    // =========================================================

    /**
     * Inicia una sesión anónima solamente cuando no existe
     * ya una sesión Firebase.
     *
     * Si existe una sesión Email/Password, la conserva.
     */
    public void iniciarSesionAnonima(
            AuthCallback callback
    ) {

        FirebaseUser usuarioActual =
                auth.getCurrentUser();


        // -----------------------------------------------------
        // YA EXISTE SESIÓN
        // -----------------------------------------------------

        if (usuarioActual != null) {

            Log.d(
                    TAG,
                    "Firebase ya tiene una sesión. UID: "
                            + usuarioActual.getUid()
            );


            if (callback != null) {

                callback.onSuccess(
                        usuarioActual
                );
            }


            return;
        }


        // -----------------------------------------------------
        // CREAR SESIÓN ANÓNIMA
        // -----------------------------------------------------

        auth.signInAnonymously()
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser usuario =
                                        auth.getCurrentUser();


                                if (usuario != null) {

                                    Log.d(
                                            TAG,
                                            "Sesión anónima iniciada. UID: "
                                                    + usuario.getUid()
                                    );


                                    if (callback != null) {

                                        callback.onSuccess(
                                                usuario
                                        );
                                    }

                                } else {

                                    enviarError(
                                            callback,
                                            new IllegalStateException(
                                                    "Firebase no devolvió "
                                                            + "el usuario anónimo."
                                            )
                                    );
                                }

                            } else {

                                Exception error =
                                        task.getException();


                                Log.e(
                                        TAG,
                                        "Error al iniciar sesión anónima.",
                                        error
                                );


                                enviarError(
                                        callback,
                                        error
                                );
                            }
                        }
                );
    }


    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    /**
     * Cierra la sesión Firebase actual.
     */
    public void cerrarSesion() {

        auth.signOut();


        Log.d(
                TAG,
                "Sesión Firebase cerrada."
        );
    }


    // =========================================================
    // USUARIO ACTUAL
    // =========================================================

    /**
     * Devuelve el usuario Firebase actualmente autenticado.
     */
    public FirebaseUser getUsuarioActual() {

        return auth.getCurrentUser();
    }


    /**
     * Devuelve el UID Firebase actual.
     *
     * Devuelve null si no existe una sesión.
     */
    public String getUid() {

        FirebaseUser usuario =
                auth.getCurrentUser();


        if (usuario == null) {

            return null;
        }


        return usuario.getUid();
    }


    // =========================================================
    // ESTADO DE AUTENTICACIÓN
    // =========================================================

    /**
     * Indica si existe actualmente un usuario Firebase.
     */
    public boolean estaAutenticado() {

        return auth.getCurrentUser()
                != null;
    }


    /**
     * Indica si el usuario Firebase actual es anónimo.
     */
    public boolean esAnonimo() {

        FirebaseUser usuario =
                auth.getCurrentUser();


        return usuario != null
                && usuario.isAnonymous();
    }


    // =========================================================
    // CALLBACK DE ERROR
    // =========================================================

    private void enviarError(
            AuthCallback callback,
            Exception error
    ) {

        if (callback == null) {

            return;
        }


        if (error != null) {

            callback.onError(
                    error
            );

            return;
        }


        callback.onError(
                new Exception(
                        "Ocurrió un error desconocido "
                                + "en Firebase Authentication."
                )
        );
    }


    // =========================================================
    // CALLBACK
    // =========================================================

    public interface AuthCallback {

        /**
         * La operación Firebase terminó correctamente.
         */
        void onSuccess(
                FirebaseUser usuario
        );


        /**
         * La operación Firebase falló.
         */
        void onError(
                Exception error
        );
    }
}

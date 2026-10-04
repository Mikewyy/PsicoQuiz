package com.utm.semiologia.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    private static final String TAG = "PsicoQuizFirebase";

    private final FirebaseAuth auth;

    public FirebaseAuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public void iniciarSesionAnonima(AuthCallback callback) {

        FirebaseUser usuarioActual = auth.getCurrentUser();

        // Ya existe una sesión de Firebase
        if (usuarioActual != null) {
            Log.d(TAG, "Firebase conectado. UID: " + usuarioActual.getUid());

            if (callback != null) {
                callback.onSuccess(usuarioActual);
            }
            return;
        }

        // Crear sesión anónima
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser usuario = auth.getCurrentUser();

                        if (usuario != null) {
                            Log.d(TAG,
                                    "Sesión anónima iniciada. UID: "
                                            + usuario.getUid());

                            if (callback != null) {
                                callback.onSuccess(usuario);
                            }
                        }

                    } else {

                        Exception error = task.getException();

                        Log.e(TAG,
                                "Error al iniciar sesión anónima",
                                error);

                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                });
    }

    public FirebaseUser getUsuarioActual() {
        return auth.getCurrentUser();
    }

    public String getUid() {
        FirebaseUser usuario = auth.getCurrentUser();

        return usuario != null
                ? usuario.getUid()
                : null;
    }

    public boolean estaAutenticado() {
        return auth.getCurrentUser() != null;
    }

    public interface AuthCallback {

        void onSuccess(FirebaseUser usuario);

        void onError(Exception error);
    }
}
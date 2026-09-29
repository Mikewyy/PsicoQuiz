package com.utm.semiologia.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Sesión activa del usuario.
 *
 * Se guarda sólo el id y el email: la contraseña nunca se persiste en claro
 * ni se relee de la base en cada arranque. Para una app real, conviene
 * migrar esto a un DataStore cifrado con EncryptedSharedPreferences.
 */
public class SesionManager {

    private static final String PREFS = "semiologia_sesion";
    private static final String K_USUARIO_ID = "usuario_id";
    private static final String K_EMAIL      = "email";
    private static final String K_SALT       = "password_salt";

    private final SharedPreferences prefs;

    public SesionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciarSesion(long usuarioId, String email, String salt) {
        prefs.edit()
                .putLong(K_USUARIO_ID, usuarioId)
                .putString(K_EMAIL, email)
                .putString(K_SALT, salt)
                .apply();
    }

    public long getUsuarioId() {
        return prefs.getLong(K_USUARIO_ID, -1L);
    }

    public String getEmail() {
        return prefs.getString(K_EMAIL, "");
    }

    public String getSalt() {
        return prefs.getString(K_SALT, "");
    }

    public boolean haySesion() {
        return getUsuarioId() > 0;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }
}

package com.utm.semiologia.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Sesión activa del usuario.
 *
 * Se guarda sólo el id y el email: la contraseña nunca se persiste en claro
 * ni se relee de la base en cada arranque. Para una app real, conviene
 * migrar esto a un DataStore cifrado con EncryptedSharedPreferences.
 *
 * Si el usuario desmarca "Recordar sesión", la sesión solo vive en memoria:
 * se pierde al cerrar la app pero sigue activa durante todo el proceso.
 */
public class SesionManager {

    private static final String PREFS = "semiologia_sesion";
    private static final String K_USUARIO_ID = "usuario_id";
    private static final String K_EMAIL      = "email";
    private static final String K_SALT       = "password_salt";

    private final SharedPreferences prefs;

    private long sesionMemoriaId = -1L;
    private String sesionMemoriaEmail = "";
    private String sesionMemoriaSalt = "";

    public SesionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Inicia sesión guardándola en memoria y en disco (comportamiento por defecto). */
    public void iniciarSesion(long usuarioId, String email, String salt) {
        iniciarSesion(usuarioId, email, salt, true);
    }

    /** Inicia sesión. Si {@code recordar} es false, no se persiste en disco. */
    public void iniciarSesion(long usuarioId, String email, String salt,
                              boolean recordar) {

        sesionMemoriaId = usuarioId;
        sesionMemoriaEmail = email;
        sesionMemoriaSalt = salt;

        if (recordar) {
            prefs.edit()
                    .putLong(K_USUARIO_ID, usuarioId)
                    .putString(K_EMAIL, email)
                    .putString(K_SALT, salt)
                    .apply();
        } else {
            prefs.edit().clear().apply();
        }
    }

    public long getUsuarioId() {
        long persistido = prefs.getLong(K_USUARIO_ID, -1L);
        return persistido > 0 ? persistido : sesionMemoriaId;
    }

    public String getEmail() {
        String persistido = prefs.getString(K_EMAIL, "");
        return !persistido.isEmpty() ? persistido : sesionMemoriaEmail;
    }

    public String getSalt() {
        String persistido = prefs.getString(K_SALT, "");
        return !persistido.isEmpty() ? persistido : sesionMemoriaSalt;
    }

    public boolean haySesion() {
        return getUsuarioId() > 0;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
        sesionMemoriaId = -1L;
        sesionMemoriaEmail = "";
        sesionMemoriaSalt = "";
    }
}

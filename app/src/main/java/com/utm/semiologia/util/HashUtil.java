package com.utm.semiologia.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Hash de contraseñas con salt.
 *
 * En producción esto debe reemplazarse por bcrypt/scrypt/Argon2 (o delegar en
 * la autenticación del backend). Se mantiene SHA-256 + salt porque es lo que
 * Android trae en la plataforma sin dependencias externas, y porque sirve
 * como placeholder funcional mientras la API no esté lista.
 */
public final class HashUtil {

    private static final String ALGORITMO = "SHA-256";
    private static final int LONGITUD_SALT = 16;

    private HashUtil() {
    }

    /** Genera un salt aleatorio en hexadecimal. */
    public static String nuevoSalt() {
        byte[] bytes = new byte[LONGITUD_SALT];
        new SecureRandom().nextBytes(bytes);
        return aHex(bytes);
    }

    /** Concatena salt + contraseña y hashea el resultado. */
    public static String hashear(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITMO);
            byte[] digest = md.digest((salt + password).getBytes("UTF-8"));
            return aHex(digest);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo hashear la contraseña", e);
        }
    }

    /** Comparación en tiempo constante para no filtrar información por timing. */
    public static boolean verificar(String password, String salt, String hashEsperado) {
        if (hashEsperado == null) return false;
        String calculado = hashear(password, salt);
        if (calculado.length() != hashEsperado.length()) return false;
        int diff = 0;
        for (int i = 0; i < calculado.length(); i++) {
            diff |= calculado.charAt(i) ^ hashEsperado.charAt(i);
        }
        return diff == 0;
    }

    private static String aHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }
}

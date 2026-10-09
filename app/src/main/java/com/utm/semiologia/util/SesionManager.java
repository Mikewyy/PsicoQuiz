package com.utm.semiologia.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestiona la sesión activa del usuario.
 *
 * Existen dos tipos de sesión:
 *
 * 1. RECORDAR SESIÓN
 *    - Se guarda en SharedPreferences.
 *    - Sobrevive al cierre completo de la aplicación.
 *
 * 2. NO RECORDAR SESIÓN
 *    - Se mantiene únicamente en memoria.
 *    - Todas las instancias de SesionManager comparten
 *      esa misma sesión durante el proceso actual.
 *    - Al cerrar completamente la aplicación, desaparece.
 *
 * La contraseña nunca se almacena en texto plano.
 */
public class SesionManager {

    // =========================================================
    // SHARED PREFERENCES
    // =========================================================

    private static final String PREFS =
            "semiologia_sesion";

    private static final String K_USUARIO_ID =
            "usuario_id";

    private static final String K_EMAIL =
            "email";

    private static final String K_SALT =
            "password_salt";


    // =========================================================
    // SESIÓN TEMPORAL EN MEMORIA
    // =========================================================

    /*
     * IMPORTANTE:
     *
     * Estos campos son static para que todas las instancias
     * de SesionManager dentro del mismo proceso compartan
     * exactamente la misma sesión temporal.
     *
     * Esto es necesario porque varios ViewModel crean su propia
     * instancia de SesionManager.
     *
     * Si "Recordar sesión" está desactivado:
     *
     * LoginActivity
     *      ↓
     * sesionMemoriaId
     *      ↓
     * DashboardViewModel
     * QuizViewModel
     * CaminoViewModel
     *
     * Todos reciben el mismo usuario.
     *
     * Cuando Android mata el proceso, estos campos desaparecen,
     * que es precisamente el comportamiento esperado para
     * "No recordar".
     */

    private static long sesionMemoriaId =
            -1L;

    private static String sesionMemoriaEmail =
            "";

    private static String sesionMemoriaSalt =
            "";


    // =========================================================
    // PREFERENCIAS
    // =========================================================

    private final SharedPreferences prefs;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SesionManager(
            Context context
    ) {

        this.prefs =
                context
                        .getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );
    }


    // =========================================================
    // INICIAR SESIÓN
    // =========================================================

    /**
     * Inicia una sesión persistente.
     *
     * Este comportamiento se conserva para los lugares
     * antiguos de la aplicación que no especifican
     * explícitamente si desean recordar la sesión.
     */
    public void iniciarSesion(
            long usuarioId,
            String email,
            String salt
    ) {

        iniciarSesion(
                usuarioId,
                email,
                salt,
                true
        );
    }


    /**
     * Inicia una sesión.
     *
     * Si recordar == true:
     * - Se mantiene en memoria.
     * - Se guarda también en SharedPreferences.
     *
     * Si recordar == false:
     * - Se mantiene únicamente en memoria.
     * - Se elimina cualquier sesión persistente anterior.
     */
    public void iniciarSesion(
            long usuarioId,
            String email,
            String salt,
            boolean recordar
    ) {

        // -----------------------------------------------------
        // SESIÓN EN MEMORIA
        // -----------------------------------------------------

        sesionMemoriaId =
                usuarioId;

        sesionMemoriaEmail =
                email != null
                        ? email
                        : "";

        sesionMemoriaSalt =
                salt != null
                        ? salt
                        : "";


        // -----------------------------------------------------
        // SESIÓN PERSISTENTE
        // -----------------------------------------------------

        if (recordar) {

            prefs
                    .edit()
                    .putLong(
                            K_USUARIO_ID,
                            usuarioId
                    )
                    .putString(
                            K_EMAIL,
                            sesionMemoriaEmail
                    )
                    .putString(
                            K_SALT,
                            sesionMemoriaSalt
                    )
                    .apply();

        } else {

            /*
             * Si anteriormente había una cuenta con
             * "Recordarme" activado, debemos eliminarla.
             *
             * De esta forma no reaparecerá después de
             * cerrar completamente la aplicación.
             */
            prefs
                    .edit()
                    .clear()
                    .apply();
        }
    }


    // =========================================================
    // USUARIO ID
    // =========================================================

    public long getUsuarioId() {

        long persistido =
                prefs.getLong(
                        K_USUARIO_ID,
                        -1L
                );


        if (persistido > 0) {

            return persistido;
        }


        return sesionMemoriaId;
    }


    // =========================================================
    // EMAIL
    // =========================================================

    public String getEmail() {

        String persistido =
                prefs.getString(
                        K_EMAIL,
                        ""
                );


        if (
                persistido != null
                        &&
                        !persistido.isEmpty()
        ) {

            return persistido;
        }


        return sesionMemoriaEmail;
    }


    // =========================================================
    // SALT
    // =========================================================

    public String getSalt() {

        String persistido =
                prefs.getString(
                        K_SALT,
                        ""
                );


        if (
                persistido != null
                        &&
                        !persistido.isEmpty()
        ) {

            return persistido;
        }


        return sesionMemoriaSalt;
    }


    // =========================================================
    // COMPROBAR SESIÓN
    // =========================================================

    public boolean haySesion() {

        return getUsuarioId() > 0;
    }


    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    public void cerrarSesion() {

        // -----------------------------------------------------
        // BORRAR PERSISTENCIA
        // -----------------------------------------------------

        prefs
                .edit()
                .clear()
                .apply();


        // -----------------------------------------------------
        // BORRAR SESIÓN TEMPORAL
        // -----------------------------------------------------

        sesionMemoriaId =
                -1L;

        sesionMemoriaEmail =
                "";

        sesionMemoriaSalt =
                "";
    }
}
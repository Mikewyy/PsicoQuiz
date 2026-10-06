package com.utm.semiologia.ui.common;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad base para las pantallas de la aplicación.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Marca la barra de navegación inferior con la sección que corresponde a
     * la pantalla. No hace nada si el layout no incluye la barra.
     */
    protected void configurarNavInferior(int seccionActual) {
        NavegacionInferior.configurar(
                this,
                seccionActual
        );
    }
}
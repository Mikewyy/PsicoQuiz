package com.utm.semiologia.ui.common;

import android.content.Intent;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.utm.semiologia.R;
import com.utm.semiologia.ui.dashboard.MainActivity;

/**
 * Barra de navegación inferior compartida por las pantallas raíz:
 * MainActivity, Guía de estudio, Estudio compartido, Casos clínicos y
 * Camino de estudio.
 *
 * Hay dos destinos posibles al tocar un icono:
 * - Si la pantalla actual es MainActivity, se cambia de sección sin recrear
 *   nada, de modo que el scroll y el Pomodoro sobreviven.
 * - Si no, se vuelve a MainActivity con CLEAR_TOP y se le indica en qué
 *   sección debe aterrizar.
 *
 * El Pomodoro de MainActivity usa coordenadas absolutas respecto a su
 * FrameLayout padre, así que la barra no le afecta: vive en un hermano
 * superior y ese FrameLayout queda por encima de la barra.
 */
public final class NavegacionInferior {

    public static final int SECCION_INICIO = 0;
    public static final int SECCION_EXPLORAR = 1;
    public static final int SECCION_DESAFIOS = 2;
    public static final int SECCION_AYUDA = 3;

    /** Clave con la que MainActivity recibe la sección que debe mostrar. */
    public static final String EXTRA_SECCION = "seccion_nav";

    /**
     * La implementa MainActivity para que la barra resuelva la navegación sin
     * que esta clase tenga que conocer su implementación.
     */
    public interface Navegador {
        void irASeccion(int seccion);
    }

    private NavegacionInferior() {
    }


    /**
     * Sección marcada en la barra. Se guarda en un objeto mutable porque el
     * listener la actualiza y la lambda necesita capturarla.
     */
    private static final class SeccionVisible {
        int seccion;
    }


    /**
     * Enlaza la barra incluida en el layout de la pantalla y la deja marcada
     * en la sección que le corresponde.
     */
    public static void configurar(
            AppCompatActivity activity,
            int seccionActual
    ) {

        if (activity == null) {
            return;
        }

        BottomNavigationView barra =
                activity.findViewById(R.id.bottomNav);

        if (barra == null) {
            return;
        }

        aplicarPaddingSistema(barra);

        final SeccionVisible visible =
                new SeccionVisible();

        visible.seccion = seccionActual;

        // El item marcado se fija ANTES de registrar el listener:
        // setSelectedItemId dispara el callback y no queremos que eso cuente
        // como una navegación.
        barra.setSelectedItemId(
                idItemDeSeccion(seccionActual)
        );

        barra.setOnItemSelectedListener(item -> {

            int seccion =
                    seccionDeIdItem(item.getItemId());

            // Volver a tocar la sección ya activa no navega: varias de estas
            // pantallas recargan datos en onResume y sin este retorno se
            // encadenarian recargas.
            if (seccion == visible.seccion) {
                return true;
            }

            visible.seccion = seccion;

            if (activity instanceof Navegador) {

                ((Navegador) activity).irASeccion(seccion);

                return true;
            }

            irAMainActivity(activity, seccion);

            return true;
        });
    }


    /** Vuelve al dashboard abriendo la sección indicada. */
    public static void irAMainActivity(
            AppCompatActivity activity,
            int seccion
    ) {

        Intent intent =
                new Intent(
                        activity,
                        MainActivity.class
                );

        intent.putExtra(
                EXTRA_SECCION,
                seccion
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        activity.startActivity(intent);
    }


    /**
     * Con targetSdk 35 Android 15 dibuja la app detrás de la barra de gestos,
     * así que la barra inferior se quedaría medio tapada. Sumamos la altura
     * real del inset al padding que Material ya trae de fábrica.
     *
     * En Android 14 y anteriores la ventana ya viene recortada por el decor y
     * el inset llega en 0, así que no hay doble padding.
     */
    private static void aplicarPaddingSistema(
            BottomNavigationView barra
    ) {

        final int izq = barra.getPaddingLeft();
        final int arr = barra.getPaddingTop();
        final int der = barra.getPaddingRight();
        final int aba = barra.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                barra,
                (v, insets) -> {

                    int abajoSistema =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            ).bottom;

                    v.setPadding(
                            izq,
                            arr,
                            der,
                            aba + abajoSistema
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(barra);
    }


    public static int seccionDeIdItem(int idItem) {

        if (idItem == R.id.nav_explorar) {
            return SECCION_EXPLORAR;
        }

        if (idItem == R.id.nav_desafios) {
            return SECCION_DESAFIOS;
        }

        if (idItem == R.id.nav_ayuda) {
            return SECCION_AYUDA;
        }

        return SECCION_INICIO;
    }


    public static int idItemDeSeccion(int seccion) {

        if (seccion == SECCION_EXPLORAR) {
            return R.id.nav_explorar;
        }

        if (seccion == SECCION_DESAFIOS) {
            return R.id.nav_desafios;
        }

        if (seccion == SECCION_AYUDA) {
            return R.id.nav_ayuda;
        }

        return R.id.nav_inicio;
    }
}
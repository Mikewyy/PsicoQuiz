
package com.utm.semiologia.ui.common;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.utm.semiologia.R;
import com.utm.semiologia.ui.dashboard.MainActivity;

public final class NavegacionInferior {

    public static final int SECCION_INICIO = 0;
    public static final int SECCION_EXPLORAR = 1;
    public static final int SECCION_DESAFIOS = 2;
    public static final int SECCION_AYUDA = 3;


    public static void actualizarSeccion(
            AppCompatActivity activity,
            int seccion
    ) {
        if (activity == null) {
            return;
        }

        View[] botones = {
                activity.findViewById(R.id.nav_inicio),
                activity.findViewById(R.id.nav_explorar),
                activity.findViewById(R.id.nav_desafios),
                activity.findViewById(R.id.nav_ayuda)
        };

        ImageView[] iconos = {
                activity.findViewById(R.id.navIconInicio),
                activity.findViewById(R.id.navIconExplorar),
                activity.findViewById(R.id.navIconDesafios),
                activity.findViewById(R.id.navIconAyuda)
        };

        TextView[] textos = {
                activity.findViewById(R.id.navTextoInicio),
                activity.findViewById(R.id.navTextoExplorar),
                activity.findViewById(R.id.navTextoDesafios),
                activity.findViewById(R.id.navTextoAyuda)
        };

        actualizarSeleccion(
                botones,
                iconos,
                textos,
                seccion
        );
    }


    public static final String EXTRA_SECCION = "seccion_nav";

    private static final int COLOR_ACTIVO =
            Color.rgb(108, 92, 231);

    private static final int COLOR_INACTIVO =
            Color.rgb(133, 129, 158);

    public interface Navegador {
        void irASeccion(int seccion);
    }

    private NavegacionInferior() {
    }

    public static void configurar(
            AppCompatActivity activity,
            int seccionActual
    ) {
        if (activity == null) {
            return;
        }

        View barra = activity.findViewById(R.id.bottomNav);

        if (barra == null) {
            return;
        }

        View[] botones = {
                activity.findViewById(R.id.nav_inicio),
                activity.findViewById(R.id.nav_explorar),
                activity.findViewById(R.id.nav_desafios),
                activity.findViewById(R.id.nav_ayuda)
        };

        ImageView[] iconos = {
                activity.findViewById(R.id.navIconInicio),
                activity.findViewById(R.id.navIconExplorar),
                activity.findViewById(R.id.navIconDesafios),
                activity.findViewById(R.id.navIconAyuda)
        };

        TextView[] textos = {
                activity.findViewById(R.id.navTextoInicio),
                activity.findViewById(R.id.navTextoExplorar),
                activity.findViewById(R.id.navTextoDesafios),
                activity.findViewById(R.id.navTextoAyuda)
        };

        aplicarPaddingSistema(barra);

        int seleccionInicial = Math.max(
                SECCION_INICIO,
                Math.min(SECCION_AYUDA, seccionActual)
        );

        actualizarSeleccion(
                botones,
                iconos,
                textos,
                seleccionInicial
        );

        for (int i = 0; i < botones.length; i++) {

            final int seccion = i;

            if (botones[i] == null) {
                continue;
            }

            botones[i].setOnClickListener(v -> {

                /*
                 * No mantenemos una segunda variable con la pestaña activa.
                 * irASeccion()/actualizarSeccion() pueden cambiarla desde
                 * onBackPressed, onNewIntent o al recrear la Activity por el
                 * tema. El estado seleccionado de la propia vista es la fuente
                 * de verdad y evita que un toque quede ignorado.
                 */
                if (v.isSelected()) {
                    return;
                }

                actualizarSeleccion(
                        botones,
                        iconos,
                        textos,
                        seccion
                );

                if (activity instanceof Navegador) {

                    ((Navegador) activity)
                            .irASeccion(seccion);

                } else {

                    irAMainActivity(activity, seccion);
                }
            });
        }
    }

    private static void actualizarSeleccion(
            View[] botones,
            ImageView[] iconos,
            TextView[] textos,
            int seleccion
    ) {

        for (int i = 0; i < botones.length; i++) {

            boolean activo = i == seleccion;

            int color = activo
                    ? COLOR_ACTIVO
                    : COLOR_INACTIVO;

            if (botones[i] != null) {

                if (activo) {
                    botones[i].setBackgroundResource(
                            R.drawable.bg_nav_seleccionado
                    );
                } else {
                    botones[i].setBackgroundColor(
                            Color.TRANSPARENT
                    );
                }

                botones[i].setSelected(activo);
            }

            if (iconos[i] != null) {
                iconos[i].setColorFilter(color);
            }

            if (textos[i] != null) {
                textos[i].setTextColor(color);

                textos[i].setTypeface(
                        null,
                        activo
                                ? android.graphics.Typeface.BOLD
                                : android.graphics.Typeface.NORMAL
                );
            }
        }
    }

    public static void irAMainActivity(
            AppCompatActivity activity,
            int seccion
    ) {

        Intent intent = new Intent(
                activity,
                MainActivity.class
        );

        intent.putExtra(EXTRA_SECCION, seccion);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        activity.startActivity(intent);
    }

    private static void aplicarPaddingSistema(View barra) {

        final int izquierda = barra.getPaddingLeft();
        final int arriba = barra.getPaddingTop();
        final int derecha = barra.getPaddingRight();
        final int abajo = barra.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                barra,
                (v, insets) -> {

                    int abajoSistema = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    ).bottom;

                    v.setPadding(
                            izquierda,
                            arriba,
                            derecha,
                            abajo + abajoSistema
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

package com.utm.semiologia.util;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.utm.semiologia.R;

/** Utilidades visuales coherentes para avisos, errores y confirmaciones breves. */
public final class UiFeedback {

    private UiFeedback() {}

    public static void mostrarError(android.view.View anchor, CharSequence mensaje) {
        mostrarSnackbar(anchor, mensaje, R.drawable.bg_feedback_error, R.color.feedback_on_error);
    }

    public static void mostrarExito(android.view.View anchor, CharSequence mensaje) {
        mostrarSnackbar(anchor, mensaje, R.drawable.bg_feedback_success, R.color.feedback_on_success);
    }

    public static void mostrarInfo(android.view.View anchor, CharSequence mensaje) {
        mostrarSnackbar(anchor, mensaje, R.drawable.bg_feedback_info, R.color.feedback_on_info);
    }

    public static void mostrarErrorCampo(EditText campo, CharSequence mensaje) {
        if (campo == null) return;
        campo.setError(null); // evita el popup blanco nativo de EditText
        campo.requestFocus();
        mostrarError(campo, mensaje);
    }

    private static void mostrarSnackbar(android.view.View anchor, CharSequence mensaje,
                                         int fondoDrawable, @ColorRes int texto) {
        if (anchor == null) return;
        Snackbar snackbar = Snackbar.make(anchor, mensaje, Snackbar.LENGTH_LONG);
        android.view.View vista = snackbar.getView();
        Context c = anchor.getContext();

        // Material Snackbar puede imponer un tint claro desde el tema. Lo anulamos
        // explícitamente para que el mensaje nunca vuelva a verse blanco en modo oscuro.
        int colorFondo;
        if (fondoDrawable == R.drawable.bg_feedback_error) {
            colorFondo = ContextCompat.getColor(c, R.color.feedback_error_bg);
        } else if (fondoDrawable == R.drawable.bg_feedback_success) {
            colorFondo = ContextCompat.getColor(c, R.color.feedback_success_bg);
        } else {
            colorFondo = ContextCompat.getColor(c, R.color.feedback_info_bg);
        }
        snackbar.setBackgroundTint(colorFondo);
        vista.setElevation(dp(c, 8));

        int colorTexto = ContextCompat.getColor(c, texto);
        snackbar.setTextColor(colorTexto);
        TextView tv = vista.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(colorTexto);
            tv.setTextSize(14f);
            tv.setMaxLines(4);
            tv.setAlpha(1f);
        }
        snackbar.setActionTextColor(ContextCompat.getColor(c, R.color.app_primary_ui));
        snackbar.show();
    }

    private static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }
}

package com.utm.semiologia.ui.evaluacion;

import android.app.Activity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.utm.semiologia.util.PreferenciasManager;

/**
 * Diálogo previo al quiz individual.
 *
 * Permite activar o desactivar las pistas de las preguntas de escritura.
 * La elección se recuerda con {@link PreferenciasManager} y solo se abre el
 * quiz si el usuario confirma con "Comenzar".
 */
public final class ConfigQuiz {

    private ConfigQuiz() {
    }

    public static void confirmarConPistas(
            Activity activity,
            Runnable alComenzar
    ) {

        if (activity == null || alComenzar == null) {
            return;
        }

        final SwitchMaterial swPistas =
                new SwitchMaterial(activity);

        swPistas.setText(
                "Mostrar pistas en las preguntas de escritura"
        );

        swPistas.setChecked(
                PreferenciasManager.pistasActivas(activity)
        );

        int padding =
                Math.round(
                        24
                                * activity.getResources()
                                .getDisplayMetrics()
                                .density
                );

        swPistas.setPadding(
                padding,
                padding / 2,
                padding,
                padding / 2
        );

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Configuración de la partida")
                .setView(swPistas)
                .setPositiveButton(
                        "Comenzar",
                        (dialog, which) -> {

                            PreferenciasManager.setPistas(
                                    activity,
                                    swPistas.isChecked()
                            );

                            alComenzar.run();
                        }
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .show();
    }
}

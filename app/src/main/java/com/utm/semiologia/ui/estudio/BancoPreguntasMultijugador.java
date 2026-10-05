package com.utm.semiologia.ui.estudio;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga el banco local y prepara una partida aleatoria.
 *
 * IMPORTANTE:
 * El anfitrión ejecuta este método una sola vez al iniciar.
 * Luego la lista YA BARAJADA se guarda en Firestore, por lo
 * que todos los jugadores de esa sala reciben exactamente
 * las mismas preguntas y exactamente el mismo orden de opciones.
 */
public final class BancoPreguntasMultijugador {

    private static final String ARCHIVO = "preguntas_multijugador.json";

    private BancoPreguntasMultijugador() {
    }

    public static List<Map<String, Object>> seleccionarPreguntas(
            Context context,
            int cantidad
    ) throws Exception {

        String json = leerAsset(context);
        JSONArray arreglo = new JSONArray(json);

        if (arreglo.length() == 0) {
            throw new IllegalStateException(
                    "El banco de preguntas está vacío."
            );
        }

        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < arreglo.length(); i++) {
            indices.add(i);
        }

        Collections.shuffle(indices);

        int total =
                Math.min(
                        cantidad,
                        arreglo.length()
                );

        List<Map<String, Object>> seleccion =
                new ArrayList<>();

        for (int i = 0; i < total; i++) {

            JSONObject original =
                    arreglo.getJSONObject(
                            indices.get(i)
                    );

            seleccion.add(
                    convertirPregunta(original)
            );
        }

        return seleccion;
    }

    private static Map<String, Object> convertirPregunta(
            JSONObject original
    ) throws Exception {

        Map<String, Object> pregunta =
                new HashMap<>();

        pregunta.put(
                "id",
                original.optLong("id", 0)
        );

        pregunta.put(
                "area",
                original.optString(
                        "area",
                        "Psicopatología"
                )
        );

        pregunta.put(
                "pregunta",
                original.optString(
                        "pregunta",
                        ""
                )
        );

        pregunta.put(
                "tipoCorrecto",
                original.optString(
                        "tipo_correcto",
                        ""
                )
        );

        pregunta.put(
                "retroalimentacion",
                original.optString(
                        "retroalimentacion",
                        ""
                )
        );

        JSONObject opcionesJson =
                original.getJSONObject(
                        "opciones"
                );

        String letraCorrecta =
                original.getString(
                        "respuesta_correcta"
                );

        List<OpcionTemporal> opciones =
                new ArrayList<>();

        String[] letras = {
                "A", "B", "C", "D"
        };

        for (String letra : letras) {

            String texto =
                    opcionesJson.getString(
                            letra
                    );

            opciones.add(
                    new OpcionTemporal(
                            texto,
                            letra.equals(
                                    letraCorrecta
                            )
                    )
            );
        }

        /*
         * Cada partida también cambia el orden de las opciones.
         * Como el resultado barajado se guarda en Firestore,
         * TODOS los dispositivos ven el mismo orden.
         */
        Collections.shuffle(opciones);

        List<String> textos =
                new ArrayList<>();

        int indiceCorrecto = -1;

        for (int i = 0; i < opciones.size(); i++) {

            OpcionTemporal opcion =
                    opciones.get(i);

            textos.add(
                    opcion.texto
            );

            if (opcion.correcta) {
                indiceCorrecto = i;
            }
        }

        if (indiceCorrecto < 0) {
            throw new IllegalStateException(
                    "Una pregunta no tiene respuesta correcta."
            );
        }

        pregunta.put(
                "opciones",
                textos
        );

        pregunta.put(
                "indiceCorrecto",
                indiceCorrecto
        );

        return pregunta;
    }

    private static String leerAsset(
            Context context
    ) throws Exception {

        StringBuilder contenido =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        context
                                                .getAssets()
                                                .open(ARCHIVO),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String linea;

            while (
                    (linea = reader.readLine())
                            != null
            ) {
                contenido
                        .append(linea)
                        .append('\n');
            }
        }

        return contenido.toString();
    }

    private static final class OpcionTemporal {

        private final String texto;
        private final boolean correcta;

        private OpcionTemporal(
                String texto,
                boolean correcta
        ) {
            this.texto = texto;
            this.correcta = correcta;
        }
    }
}

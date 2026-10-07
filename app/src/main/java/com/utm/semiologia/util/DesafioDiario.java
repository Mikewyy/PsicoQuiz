package com.utm.semiologia.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Banco local del desafío diario.
 *
 * - La pregunta del día es la misma para todos los usuarios: se elige por
 *   fecha (días desde una base) y se recorre el banco en orden.
 * - Las opciones A/B/C se barajan con una semilla derivada de la fecha, así
 *   un mismo día todos ven el mismo orden, pero la letra de la correcta
 *   cambia de un día a otro.
 *
 * Sin java.time (minSdk 24): se usa FechaUtil para los días transcurridos.
 */
public final class DesafioDiario {

    private static final String ARCHIVO = "desafios_diarios.json";

    /** Los días se cuentan desde esta base; la pregunta del día es el módulo del banco. */
    private static final String FECHA_BASE = "2026-01-01";

    private static final List<Pregunta> BANCO = new ArrayList<>();

    private DesafioDiario() {
    }

    /**
     * Una pregunta del banco. `correcta` es la letra original (A/B/C) tal y
     * como se guardó en el JSON.
     */
    public static final class Pregunta {
        private final String enunciado;
        private final Map<String, String> opciones;
        private final String correcta;
        private final String justificacion;
        private final String fuente;

        Pregunta(String enunciado, Map<String, String> opciones,
                 String correcta, String justificacion, String fuente) {
            this.enunciado = enunciado;
            this.opciones = opciones;
            this.correcta = correcta;
            this.justificacion = justificacion;
            this.fuente = fuente;
        }

        public String getEnunciado() {
            return enunciado;
        }

        public String getOpcion(String letra) {
            return opciones.get(letra);
        }

        public String getCorrecta() {
            return correcta;
        }

        public String getJustificacion() {
            return justificacion;
        }

        public String getFuente() {
            return fuente;
        }
    }

    /**
     * Presentación lista para pintar: opciones en el orden mostrado (A, B, C)
     * y la letra con la respuesta ganadora dentro de ese orden mezclado.
     */
    public static final class Presentacion {
        private final Pregunta pregunta;
        private final String[] opciones;      // índice 0,1,2 = letras A,B,C
        private final int letraCorrecta;      // índice 0-2 de la correcta

        Presentacion(Pregunta pregunta, String[] opciones, int letraCorrecta) {
            this.pregunta = pregunta;
            this.opciones = opciones;
            this.letraCorrecta = letraCorrecta;
        }

        public Pregunta getPregunta() {
            return pregunta;
        }

        public String getOpcion(int indice) {
            return opciones[indice];
        }

        public int getLetraCorrecta() {
            return letraCorrecta;
        }

        public String getLetraCorrectaComoLetra() {
            return String.valueOf((char) ('A' + letraCorrecta));
        }
    }

    /** Carga el banco una sola vez y lo cachea en memoria. */
    private static List<Pregunta> banco(Context context) {
        if (BANCO.isEmpty()) {
            String json = leerAsset(context);
            if (json != null) {
                parsearBanco(json);
            }
        }
        return BANCO;
    }

    /** Pregunta presentada para la fecha indicada (o null si el banco está vacío). */
    public static Presentacion preguntaDe(Context context, String fecha) {
        List<Pregunta> banco = banco(context);
        if (banco.isEmpty()) {
            return null;
        }

        int indice = indiceDelDia(fecha, banco.size());
        Pregunta base = banco.get(indice);
        return barajar(base, semillaDelDia(fecha));
    }

    /** Devuelve cuál pregunta del banco le toca a una fecha: módulo sobre el tamaño. */
    public static int indiceDelDia(String fecha, int tamano) {
        int dias = FechaUtil.diasEntre(FECHA_BASE, fecha);
        if (dias < 0) dias = 0;
        return Math.floorMod(dias, tamano);
    }

    private static long semillaDelDia(String fecha) {
        return FechaUtil.diasEntre(FECHA_BASE, fecha);
    }

    /**
     * Permuta las tres opciones con Fisher-Yates usando la semilla del día:
     * mismo resultado para todos los usuarios ese día.
     */
    private static Presentacion barajar(Pregunta p, long semilla) {
        List<String> letras = new ArrayList<>();
        letras.add("A");
        letras.add("B");
        letras.add("C");

        Random rnd = new Random(semilla);
        for (int i = letras.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            String tmp = letras.get(i);
            letras.set(i, letras.get(j));
            letras.set(j, tmp);
        }

        String[] opciones = new String[3];
        int letraCorrecta = 0;
        for (int i = 0; i < 3; i++) {
            String letraMostrada = letras.get(i);
            opciones[i] = p.getOpcion(letraMostrada);
            if (letraMostrada.equals(p.getCorrecta())) {
                letraCorrecta = i;
            }
        }
        return new Presentacion(p, opciones, letraCorrecta);
    }

    private static void parsearBanco(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject q = arr.getJSONObject(i);
                JSONObject opcionesObj = q.getJSONObject("opciones");

                Map<String, String> opciones = new HashMap<>();
                opciones.put("A", opcionesObj.getString("A"));
                opciones.put("B", opcionesObj.getString("B"));
                opciones.put("C", opcionesObj.getString("C"));

                BANCO.add(new Pregunta(
                        q.getString("enunciado"),
                        opciones,
                        q.getString("correcta"),
                        q.optString("justificacion"),
                        q.optString("fuente")
                ));
            }
        } catch (Exception e) {
            android.util.Log.e("DesafioDiario", "Banco inválido o ilegible", e);
        }
    }

    private static String leerAsset(Context context) {
        try (InputStream in = context.getAssets().open(ARCHIVO);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            android.util.Log.e("DesafioDiario", "No se pudo leer assets/" + ARCHIVO, e);
            return null;
        }
    }
}
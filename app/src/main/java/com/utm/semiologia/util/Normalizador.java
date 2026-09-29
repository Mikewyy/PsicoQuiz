package com.utm.semiologia.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalización y comparación de respuestas escritas.
 *
 * DECISIÓN DE PRODUCTO: la corrección es NORMALIZADA, no exacta.
 * El documento original pedía respetar "signos de puntuación exactos", pero
 * exigir que el estudiante escriba el mismo punto y la misma tilde castiga
 * la ortografía, no el conocimiento, y hace que la app se sienta injusta.
 * Aquí se ignora:
 *   - mayúsculas / minúsculas
 *   - tildes y diéresis
 *   - espacios sobrantes al inicio, al final o entre palabras
 *   - puntuación de los extremos
 *   - singular vs plural
 *
 * Se sigue exigiendo el término correcto: no es una comparación difusa ni
 * acepta cualquier redacción. Lasezgo de la opción libre vive en la lista de
 * sinónimos que trae cada pregunta en la base de datos.
 *
 * IMPORTANTE: se usa Locale.ROOT y no el locale del sistema. Con locale
 * turco, "I".toLowerCase() devuelve "ı" (punto sin punto) y toda palabra
 * que empiece por i se compararía mal.
 */
public final class Normalizador {

    /** Separador de alternativas en el campo respuestas_validas. */
    public static final String SEPARADOR_ALTERNATIVAS = "|";

    /** Caracteres de puntuación que se eliminan de los extremos. */
    private static final Pattern PUNTUACION_EXTREMOS =
            Pattern.compile("^[\\p{Punct}\\p{IsPunctuation}\\s]+|[\\p{Punct}\\p{IsPunctuation}\\s]+$");

    /** Caracteres de puntuación que se eliminan en cualquier posición. */
    private static final Pattern PUNTUACION_INTERNA =
            Pattern.compile("[\\p{Punct}\\p{IsPunctuation}]+");

    private Normalizador() {
    }

    /**
     * Reduce un texto a su forma comparable.
     * Ej.: "  ¿Delusión? " -> "delusion"
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = texto.trim().toLowerCase(Locale.ROOT);

        // Quitar diacríticos: NFD separa la letra de su tilde, y luego
        // se descartan los marcas combinantes (categoría Mn).
        t = Normalizer.normalize(t, Normalizer.Form.NFD);
        t = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(t).replaceAll("");

        // Puntuación: primero en los extremos, luego en cualquier posición
        // (para que "delusion,¿?" y "delusion" sean iguales).
        t = PUNTUACION_EXTREMOS.matcher(t).replaceAll("");
        t = PUNTUACION_INTERNA.matcher(t).replaceAll(" ");

        // Colapsar espacios y quitar la ñ -> n de forma estable.
        t = t.replaceAll("\\s+", " ").trim();
        t = t.replace('ñ', 'n');

        return t;
    }

    /**
     * Quita la marca de plural de la última palabra.
     * "alucinaciones" -> "alucinacion", "ideas" -> "idea".
     *
     * No aplica a toda palabra en singular terminada en 's' (p. ej. "déficit",
     * "compás", "psicosis"), así que se protege una lista corta de terminaciones
     * ambiguas.
     */
    private static boolean esPluralSeguro(String palabra) {
        return palabra.endsWith("s")
                && !palabra.endsWith("is")   // psicosis, paresis
                && !palabra.endsWith("us")   // consensus
                && !palabra.endsWith("as")   // crisis
                && !palabra.endsWith("os");  // psychosis
    }

    /** Normaliza y además aplica la reducción de plural. */
    public static String normalizarSinPlural(String texto) {
        String base = normalizar(texto);
        if (base.isEmpty()) return base;

        int espacio = base.lastIndexOf(' ');
        String cabeza = espacio < 0 ? "" : base.substring(0, espacio + 1);
        String ultima = espacio < 0 ? base : base.substring(espacio + 1);

        if (esPluralSeguro(ultima)) {
            String singular;
            if (ultima.endsWith("es") && ultima.length() > 3) {
                singular = ultima.substring(0, ultima.length() - 2); // "alucinaciones" -> "alucinacion"
            } else {
                singular = ultima.substring(0, ultima.length() - 1); // "ideas" -> "idea"
            }
            return cabeza + singular;
        }
        return base;
    }

    /**
     * Compara la respuesta del estudiante contra la lista de respuestas válidas.
     *
     * @param respuestaUsuario  lo que escribió el estudiante
     * @param respuestasValidas alternativas separadas por '|'.
     *                          Ej: "alucinacion|alucinaciones|idea delirante"
     * @return true si alguna alternativa coincide
     */
    public static boolean coincide(String respuestaUsuario, String respuestasValidas) {
        String dada = normalizar(respuestaUsuario);
        if (dada.isEmpty()) {
            return false;
        }
        if (respuestasValidas == null) {
            return false;
        }

        String[] alternativas = respuestasValidas.split("\\" + SEPARADOR_ALTERNATIVAS);
        for (String alternativa : alternativas) {
            String a = normalizar(alternativa);
            if (a.isEmpty()) continue;

            if (dada.equals(a)) {
                return true;
            }
            // Segunda pasada permitiendo que falte la 's' final en una de las dos.
            if (normalizarSinPlural(dada).equals(normalizarSinPlural(a))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve cuál de las alternativas acertó el estudiante, para poder
     * mostrarle en pantalla la que él escribió. Null si falló.
     */
    public static String alternativaAcierta(String respuestaUsuario, String respuestasValidas) {
        String dada = normalizar(respuestaUsuario);
        if (dada.isEmpty() || respuestasValidas == null) return null;

        for (String alternativa : respuestasValidas.split("\\" + SEPARADOR_ALTERNATIVAS)) {
            if (normalizar(alternativa).isEmpty()) continue;
            if (coincide(respuestaUsuario, alternativa)) {
                return alternativa.trim();
            }
        }
        return null;
    }
}

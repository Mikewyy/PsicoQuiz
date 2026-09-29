package com.utm.semiologia.ui.evaluacion;

import com.utm.semiologia.data.model.Opcion;
import com.utm.semiologia.data.model.Pregunta;

import java.util.Collections;
import java.util.List;

/** Estado inmutable de la pantalla de un intento (examen o práctica). */
public class EstadoQuiz {

    public final Pregunta pregunta;      // null si el tramo no tiene preguntas
    public final List<Opcion> opciones;  // vacío para preguntas escritas
    public final int indice;             // 0-based
    public final int total;
    public final int aciertos;
    public final int puntos;
    public final boolean respondida;
    public final boolean correcta;
    public final boolean esEscrita;
    public final boolean esExamen;
    public final String justificacion;

    public EstadoQuiz(Pregunta pregunta, List<Opcion> opciones, int indice, int total,
                      int aciertos, int puntos, boolean respondida, boolean correcta,
                      boolean esEscrita, boolean esExamen, String justificacion) {
        this.pregunta = pregunta;
        this.opciones = opciones;
        this.indice = indice;
        this.total = total;
        this.aciertos = aciertos;
        this.puntos = puntos;
        this.respondida = respondida;
        this.correcta = correcta;
        this.esEscrita = esEscrita;
        this.esExamen = esExamen;
        this.justificacion = justificacion;
    }

    public boolean esUltima() {
        return total > 0 && indice >= total - 1;
    }

    public static EstadoQuiz vacio(boolean esExamen) {
        return new EstadoQuiz(null, Collections.emptyList(), 0, 0,
                0, 0, false, false, false, esExamen, null);
    }
}

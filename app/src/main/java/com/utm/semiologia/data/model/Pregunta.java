package com.utm.semiologia.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Una pregunta del módulo de evaluación.
 *
 * TIPOS
 *  - MCQ     : se elige una de las 4 opciones de {@link #getOpciones()}.
 *  - ESCRITA : se escribe el término; la corrección la hace
 *              {@link com.utm.semiologia.util.Normalizador} contra
 *              {@link #getRespuestasValidas()}.
 */
public class Pregunta {

    public static final String TIPO_MCQ     = "mcq";
    public static final String TIPO_ESCRITA = "escrita";

    private long     id;
    private long     nivelId;
    private String   tema;
    private String   tipo;
    private String   enunciado;
    private String   pista;
    private String   respuestasValidas;   // separadas por '|', sólo para 'escrita'
    private String   justificacion;
    private int      puntos;
    private int      orden;

    private List<Opcion> opciones = new ArrayList<>();

    public Pregunta() {
    }

    public boolean esEscrita() {
        return TIPO_ESCRITA.equals(tipo);
    }

    public boolean esMcq() {
        return TIPO_MCQ.equals(tipo);
    }

    /** La opción marcada como correcta. null si la pregunta no es MCQ. */
    public Opcion opcionCorrecta() {
        for (Opcion o : opciones) {
            if (o.isCorrecta()) return o;
        }
        return null;
    }

    /** Baraja las opciones para que la correcta no caiga siempre en la misma posición. */
    public List<Opcion> opcionesBarajadas(java.util.Random rnd) {
        List<Opcion> copia = new ArrayList<>(opciones);
        java.util.Collections.shuffle(copia, rnd);
        return copia;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNivelId() { return nivelId; }
    public void setNivelId(long v) { this.nivelId = v; }

    public String getTema() { return tema; }
    public void setTema(String v) { this.tema = v; }

    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String v) { this.enunciado = v; }

    public String getPista() { return pista; }
    public void setPista(String v) { this.pista = v; }

    public String getRespuestasValidas() { return respuestasValidas; }
    public void setRespuestasValidas(String v) { this.respuestasValidas = v; }

    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String v) { this.justificacion = v; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int v) { this.puntos = v; }

    public int getOrden() { return orden; }
    public void setOrden(int v) { this.orden = v; }

    public List<Opcion> getOpciones() { return opciones; }
    public void setOpciones(List<Opcion> v) { this.opciones = v; }
}

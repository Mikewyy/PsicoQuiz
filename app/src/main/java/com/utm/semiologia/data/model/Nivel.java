package com.utm.semiologia.data.model;

/** Un tramo ("nivel") del camino de aprendizaje, tipo Duolingo. */
public class Nivel {

    private long    id;
    private int     numero;          // 1..5, orden en el camino
    private String  nombre;
    private String  descripcion;
    private String  tema;
    private String  emoji;
    private int     totalPreguntas;

    public Nivel() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int n) { this.numero = n; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public String getTema() { return tema; }
    public void setTema(String v) { this.tema = v; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String v) { this.emoji = v; }

    public int getTotalPreguntas() { return totalPreguntas; }
    public void setTotalPreguntas(int v) { this.totalPreguntas = v; }
}

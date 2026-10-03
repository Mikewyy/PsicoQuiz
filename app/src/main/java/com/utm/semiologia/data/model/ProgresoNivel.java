package com.utm.semiologia.data.model;

/** Una fila del camino: el tramo con el estado del usuario. */
public class ProgresoNivel {

    private long    nivelId;
    private String  nombre;
    private String  emoji;
    private String  tema;
    private String  descripcion;
    private int     numero;
    private String  categoria;
    private int     orden;
    private int     totalPreguntas;

    private boolean aprobado;
    private int     mejorPorcentaje;
    private int     mejorPuntaje;
    private int     intentos;
    private Long    completadoEn;

    /** Estado para pintar: el anterior está aprobado y éste no. */
    private boolean bloqueado;

    public ProgresoNivel() {
    }

    public long getNivelId() { return nivelId; }
    public void setNivelId(long v) { this.nivelId = v; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String v) { this.emoji = v; }

    public String getTema() { return tema; }
    public void setTema(String v) { this.tema = v; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public int getNumero() { return numero; }
    public void setNumero(int v) { this.numero = v; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String v) { this.categoria = v; }

    public int getOrden() { return orden; }
    public void setOrden(int v) { this.orden = v; }

    public int getTotalPreguntas() { return totalPreguntas; }
    public void setTotalPreguntas(int v) { this.totalPreguntas = v; }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean v) { this.aprobado = v; }

    public int getMejorPorcentaje() { return mejorPorcentaje; }
    public void setMejorPorcentaje(int v) { this.mejorPorcentaje = v; }

    public int getMejorPuntaje() { return mejorPuntaje; }
    public void setMejorPuntaje(int v) { this.mejorPuntaje = v; }

    public int getIntentos() { return intentos; }
    public void setIntentos(int v) { this.intentos = v; }

    public Long getCompletadoEn() { return completadoEn; }
    public void setCompletadoEn(Long v) { this.completadoEn = v; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean v) { this.bloqueado = v; }

    /** Estrellas ganadas según el porcentaje (máx 3). */
    public int estrellas() {
        if (mejorPorcentaje >= 100) return 3;
        if (mejorPorcentaje >= 80)  return 2;
        if (mejorPorcentaje >= 60)  return 1;
        return 0;
    }
}

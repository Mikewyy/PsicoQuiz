package com.utm.semiologia.data.model;

/** Una opción de respuesta de una pregunta de opción múltiple. */
public class Opcion {

    private long    id;
    private long    preguntaId;
    private String  texto;
    private boolean correcta;
    private int     orden;

    public Opcion() {
    }

    public Opcion(String texto, boolean correcta, int orden) {
        this.texto = texto;
        this.correcta = correcta;
        this.orden = orden;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPreguntaId() { return preguntaId; }
    public void setPreguntaId(long v) { this.preguntaId = v; }

    public String getTexto() { return texto; }
    public void setTexto(String v) { this.texto = v; }

    public boolean isCorrecta() { return correcta; }
    public void setCorrecta(boolean v) { this.correcta = v; }

    public int getOrden() { return orden; }
    public void setOrden(int v) { this.orden = v; }
}

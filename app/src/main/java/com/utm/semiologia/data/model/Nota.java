package com.utm.semiologia.data.model;

/** Fragmento de texto resaltado por el usuario dentro de una sección de la guía. */
public class Nota {

    private long    id;
    private long    usuarioId;
    private Long    seccionId;      // null si la sección fue borrada
    private String  textoSeleccionado;
    private String  comentario;
    private String  color;
    private long    creadaEn;
    private boolean sincronizada;   // pendiente de subir al backend

    public Nota() {
        this.color = "#FFEB3B";
    }

    public static Nota crear(long usuarioId, Long seccionId, String textoSeleccionado, String comentario) {
        Nota n = new Nota();
        n.usuarioId = usuarioId;
        n.seccionId = seccionId;
        n.textoSeleccionado = textoSeleccionado;
        n.comentario = comentario;
        n.creadaEn = System.currentTimeMillis();
        n.sincronizada = false;
        return n;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long u) { this.usuarioId = u; }

    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long s) { this.seccionId = s; }

    public String getTextoSeleccionado() { return textoSeleccionado; }
    public void setTextoSeleccionado(String t) { this.textoSeleccionado = t; }

    public String getComentario() { return comentario; }
    public void setComentario(String c) { this.comentario = c; }

    public String getColor() { return color; }
    public void setColor(String c) { this.color = c; }

    public long getCreadaEn() { return creadaEn; }
    public void setCreadaEn(long t) { this.creadaEn = t; }

    public boolean isSincronizada() { return sincronizada; }
    public void setSincronizada(boolean s) { this.sincronizada = s; }
}

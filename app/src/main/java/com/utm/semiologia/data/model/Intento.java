package com.utm.semiologia.data.model;

/** Una partida del módulo de evaluación. */
public class Intento {

    public static final String MODO_EXAMEN  = "examen";   // cuenta para aprobar el tramo
    public static final String MODO_PRACTICA = "practica"; // libre, no otorga puntos

    private long   id;
    private long   usuarioId;
    private long   nivelId;
    private String modo;
    private int    total;
    private int    acertadas;
    private int    puntos;
    private boolean aprobado;
    private long   iniciadoEn;
    private Long   finalizadoEn;

    public Intento() {
        this.modo = MODO_EXAMEN;
    }

    public static Intento iniciar(long usuarioId, long nivelId, String modo, int total) {
        Intento i = new Intento();
        i.usuarioId = usuarioId;
        i.nivelId = nivelId;
        i.modo = modo;
        i.total = total;
        i.iniciadoEn = System.currentTimeMillis();
        return i;
    }

    public void finalizar(int acertadas, int puntos, float umbral) {
        this.acertadas = acertadas;
        this.puntos = puntos;
        this.finalizadoEn = System.currentTimeMillis();
        this.aprobado = total > 0 && (acertadas / (float) total) >= umbral;
    }

    public float porcentaje() {
        return total > 0 ? (acertadas / (float) total) * 100f : 0f;
    }

    public boolean esExamen() {
        return MODO_EXAMEN.equals(modo);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long v) { this.usuarioId = v; }

    public long getNivelId() { return nivelId; }
    public void setNivelId(long v) { this.nivelId = v; }

    public String getModo() { return modo; }
    public void setModo(String v) { this.modo = v; }

    public int getTotal() { return total; }
    public void setTotal(int v) { this.total = v; }

    public int getAciertos() { return acertadas; }
    public void setAciertos(int v) { this.acertadas = v; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int v) { this.puntos = v; }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean v) { this.aprobado = v; }

    public long getIniciadoEn() { return iniciadoEn; }
    public void setIniciadoEn(long v) { this.iniciadoEn = v; }

    public Long getFinalizadoEn() { return finalizadoEn; }
    public void setFinalizadoEn(Long v) { this.finalizadoEn = v; }
}

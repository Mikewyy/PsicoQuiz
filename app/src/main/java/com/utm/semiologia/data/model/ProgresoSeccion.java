package com.utm.semiologia.data.model;

/** Estado de lectura de una sección por parte de un usuario. */
public class ProgresoSeccion {

    private long    id;
    private long    usuarioId;
    private long    seccionId;
    private int     lectura;           // porcentaje 0..100
    private boolean completada;
    private int     puntosOtorgados;
    private int     comidaOtorgada;
    private long    iniciadaEn;
    private Long    completadaEn;

    // Datos desnormalizados del catálogo, sólo para pintar la lista
    private String  titulo;
    private String  tema;
    private int     puntosRecompensa;

    public ProgresoSeccion() {
    }

    public static ProgresoSeccion iniciar(long usuarioId, long seccionId) {
        ProgresoSeccion p = new ProgresoSeccion();
        p.usuarioId = usuarioId;
        p.seccionId = seccionId;
        p.lectura = 0;
        p.completada = false;
        p.iniciadaEn = System.currentTimeMillis();
        return p;
    }

    /** Marca la sección como leída. Idempotente: no vuelve a otorgar premios. */
    public void completar(int puntos, int comida) {
        if (this.completada) return;   // <- evita farmear puntos
        this.completada = true;
        this.lectura = 100;
        this.completadaEn = System.currentTimeMillis();
        this.puntosOtorgados += puntos;
        this.comidaOtorgada += comida;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long u) { this.usuarioId = u; }

    public long getSeccionId() { return seccionId; }
    public void setSeccionId(long s) { this.seccionId = s; }

    public int getLectura() { return lectura; }
    public void setLectura(int l) { this.lectura = Math.max(0, Math.min(100, l)); }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean c) { this.completada = c; }

    public int getPuntosOtorgados() { return puntosOtorgados; }
    public void setPuntosOtorgados(int p) { this.puntosOtorgados = p; }

    public int getComidaOtorgada() { return comidaOtorgada; }
    public void setComidaOtorgada(int c) { this.comidaOtorgada = c; }

    public long getIniciadaEn() { return iniciadaEn; }
    public void setIniciadaEn(long t) { this.iniciadaEn = t; }

    public Long getCompletadaEn() { return completadaEn; }
    public void setCompletadaEn(Long t) { this.completadaEn = t; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String t) { this.titulo = t; }

    public String getTema() { return tema; }
    public void setTema(String t) { this.tema = t; }

    public int getPuntosRecompensa() { return puntosRecompensa; }
    public void setPuntosRecompensa(int p) { this.puntosRecompensa = p; }
}

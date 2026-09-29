package com.utm.semiologia.data.model;

/** Registro histórico de una sesión Pomodoro (individual o grupal). */
public class SesionPomodoro {

    public static final String MODO_INDIVIDUAL = "individual";
    public static final String MODO_GRUPAL     = "grupal";

    /** 20 min de estudio / 5 de descanso, según el requerimiento. */
    public static final int FOCO_POR_DEFECTO_MIN     = 20;
    public static final int DESCANSO_POR_DEFECTO_MIN = 5;

    private long    id;
    private long    usuarioId;
    private String  modo;
    private Long    grupoId;
    private int     duracionFocoMin;
    private int     duracionDescansoMin;
    private int     minutosEstudiados;
    private int     ciclosCompletados;
    private int     puntosGanados;
    private boolean completado;
    private long    iniciadoEn;
    private Long    finalizadoEn;

    public SesionPomodoro() {
        this.modo = MODO_INDIVIDUAL;
        this.duracionFocoMin = FOCO_POR_DEFECTO_MIN;
        this.duracionDescansoMin = DESCANSO_POR_DEFECTO_MIN;
    }

    public static SesionPomodoro iniciar(long usuarioId, String modo, Long grupoId,
                                          int focoMin, int descansoMin) {
        SesionPomodoro s = new SesionPomodoro();
        s.usuarioId = usuarioId;
        s.modo = modo;
        s.grupoId = grupoId;
        s.duracionFocoMin = focoMin;
        s.duracionDescansoMin = descansoMin;
        s.iniciadoEn = System.currentTimeMillis();
        return s;
    }

    /** Cierra la sesión. Sólo otorga puntos si se completó al menos un ciclo. */
    public void finalizar() {
        this.finalizadoEn = System.currentTimeMillis();
        this.minutosEstudiados = this.ciclosCompletados * this.duracionFocoMin;
        this.completado = this.ciclosCompletados > 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long u) { this.usuarioId = u; }

    public String getModo() { return modo; }
    public void setModo(String m) { this.modo = m; }

    public Long getGrupoId() { return grupoId; }
    public void setGrupoId(Long g) { this.grupoId = g; }

    public int getDuracionFocoMin() { return duracionFocoMin; }
    public void setDuracionFocoMin(int d) { this.duracionFocoMin = d; }

    public int getDuracionDescansoMin() { return duracionDescansoMin; }
    public void setDuracionDescansoMin(int d) { this.duracionDescansoMin = d; }

    public int getMinutosEstudiados() { return minutosEstudiados; }
    public void setMinutosEstudiados(int m) { this.minutosEstudiados = m; }

    public int getCiclosCompletados() { return ciclosCompletados; }
    public void setCiclosCompletados(int c) { this.ciclosCompletados = c; }

    public int getPuntosGanados() { return puntosGanados; }
    public void setPuntosGanados(int p) { this.puntosGanados = p; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean c) { this.completado = c; }

    public long getIniciadoEn() { return iniciadoEn; }
    public void setIniciadoEn(long t) { this.iniciadoEn = t; }

    public Long getFinalizadoEn() { return finalizadoEn; }
    public void setFinalizadoEn(Long t) { this.finalizadoEn = t; }
}

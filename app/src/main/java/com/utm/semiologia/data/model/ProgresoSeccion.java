package com.utm.semiologia.data.model;

/**
 * Estado de lectura de una sección por parte de un usuario.
 *
 * También contiene algunos datos desnormalizados del catálogo de secciones
 * para poder mostrar la Guía de Estudio y el lector sin crear modelos
 * adicionales.
 */
public class ProgresoSeccion {

    // =========================================================
    // PROGRESO DEL USUARIO
    // =========================================================

    private long id;
    private long usuarioId;
    private long seccionId;

    /** Porcentaje de lectura entre 0 y 100. */
    private int lectura;

    private boolean completada;
    private int puntosOtorgados;
    private int comidaOtorgada;

    private long iniciadaEn;
    private Long completadaEn;


    // =========================================================
    // DATOS DE LA SECCIÓN
    // =========================================================

    /** Título visible de la sección. */
    private String titulo;

    /** Tema o categoría a la que pertenece. */
    private String tema;

    /** Contenido HTML que se mostrará en el lector. */
    private String contenido;

    /** Duración estimada de lectura en minutos. */
    private int duracionEstimadaMin;

    /** Puntos que entrega la sección al completarla. */
    private int puntosRecompensa;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ProgresoSeccion() {
    }


    // =========================================================
    // CREACIÓN DE PROGRESO
    // =========================================================

    public static ProgresoSeccion iniciar(long usuarioId, long seccionId) {

        ProgresoSeccion p = new ProgresoSeccion();

        p.usuarioId = usuarioId;
        p.seccionId = seccionId;

        p.lectura = 0;
        p.completada = false;

        p.puntosOtorgados = 0;
        p.comidaOtorgada = 0;

        p.iniciadaEn = System.currentTimeMillis();
        p.completadaEn = null;

        return p;
    }


    // =========================================================
    // COMPLETAR SECCIÓN
    // =========================================================

    /**
     * Marca la sección como completada.
     *
     * Es idempotente:
     * si ya estaba completada no vuelve a sumar puntos ni comida.
     */
    public void completar(int puntos, int comida) {

        if (this.completada) {
            return;
        }

        this.completada = true;
        this.lectura = 100;
        this.completadaEn = System.currentTimeMillis();

        this.puntosOtorgados += puntos;
        this.comidaOtorgada += comida;
    }


    // =========================================================
    // GETTERS Y SETTERS
    // =========================================================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(long usuarioId) {
        this.usuarioId = usuarioId;
    }


    public long getSeccionId() {
        return seccionId;
    }

    public void setSeccionId(long seccionId) {
        this.seccionId = seccionId;
    }


    public int getLectura() {
        return lectura;
    }

    public void setLectura(int lectura) {
        this.lectura = Math.max(0, Math.min(100, lectura));
    }


    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }


    public int getPuntosOtorgados() {
        return puntosOtorgados;
    }

    public void setPuntosOtorgados(int puntosOtorgados) {
        this.puntosOtorgados = puntosOtorgados;
    }


    public int getComidaOtorgada() {
        return comidaOtorgada;
    }

    public void setComidaOtorgada(int comidaOtorgada) {
        this.comidaOtorgada = comidaOtorgada;
    }


    public long getIniciadaEn() {
        return iniciadaEn;
    }

    public void setIniciadaEn(long iniciadaEn) {
        this.iniciadaEn = iniciadaEn;
    }


    public Long getCompletadaEn() {
        return completadaEn;
    }

    public void setCompletadaEn(Long completadaEn) {
        this.completadaEn = completadaEn;
    }


    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }


    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }


    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }


    public int getDuracionEstimadaMin() {
        return duracionEstimadaMin;
    }

    public void setDuracionEstimadaMin(int duracionEstimadaMin) {
        this.duracionEstimadaMin = Math.max(0, duracionEstimadaMin);
    }


    public int getPuntosRecompensa() {
        return puntosRecompensa;
    }

    public void setPuntosRecompensa(int puntosRecompensa) {
        this.puntosRecompensa = Math.max(0, puntosRecompensa);
    }


    // =========================================================
    // MÉTODOS ÚTILES PARA LA INTERFAZ
    // =========================================================

    /**
     * Texto que podemos mostrar directamente en la lista.
     *
     * Ejemplos:
     * "Completada"
     * "40% leído"
     * "Sin comenzar"
     */
    public String getEstadoLectura() {

        if (completada) {
            return "Completada";
        }

        if (lectura > 0) {
            return lectura + "% leído";
        }

        return "Sin comenzar";
    }


    /**
     * Indica si el usuario ya comenzó a leer esta sección.
     */
    public boolean estaIniciada() {
        return lectura > 0 || completada;
    }
}
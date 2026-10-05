package com.utm.semiologia.firebase.model;

import com.google.firebase.Timestamp;

public class SalaEstudio {

    private String codigo;
    private String nombre;
    private String anfitrionId;

    private int maxParticipantes;
    private int cantidadParticipantes;

    private String estadoPomodoro;
    private int duracionPomodoro;
    private int duracionDescanso;
    private Timestamp inicioPomodoro;

    // MULTIJUGADOR / QUIZ
    private String estadoPartida;
    private int preguntaActual;
    private int totalPreguntas;
    private String partidaId;
    private Timestamp inicioPartida;

    private long creadoEn;

    public SalaEstudio() {
    }

    public SalaEstudio(
            String codigo,
            String nombre,
            String anfitrionId
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;

        this.maxParticipantes = 5;
        this.cantidadParticipantes = 1;

        this.estadoPomodoro = "detenido";
        this.duracionPomodoro = 25 * 60;
        this.duracionDescanso = 5 * 60;
        this.inicioPomodoro = null;

        this.estadoPartida = "esperando";
        this.preguntaActual = 0;
        this.totalPreguntas = 10;
        this.partidaId = null;
        this.inicioPartida = null;

        this.creadoEn = System.currentTimeMillis();
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAnfitrionId() {
        return anfitrionId;
    }

    public void setAnfitrionId(String anfitrionId) {
        this.anfitrionId = anfitrionId;
    }

    public int getMaxParticipantes() {
        return maxParticipantes;
    }

    public void setMaxParticipantes(int maxParticipantes) {
        this.maxParticipantes = maxParticipantes;
    }

    public int getCantidadParticipantes() {
        return cantidadParticipantes;
    }

    public void setCantidadParticipantes(int cantidadParticipantes) {
        this.cantidadParticipantes = cantidadParticipantes;
    }

    public String getEstadoPomodoro() {
        return estadoPomodoro;
    }

    public void setEstadoPomodoro(String estadoPomodoro) {
        this.estadoPomodoro = estadoPomodoro;
    }

    public int getDuracionPomodoro() {
        return duracionPomodoro;
    }

    public void setDuracionPomodoro(int duracionPomodoro) {
        this.duracionPomodoro = duracionPomodoro;
    }

    public int getDuracionDescanso() {
        return duracionDescanso;
    }

    public void setDuracionDescanso(int duracionDescanso) {
        this.duracionDescanso = duracionDescanso;
    }

    public Timestamp getInicioPomodoro() {
        return inicioPomodoro;
    }

    public void setInicioPomodoro(Timestamp inicioPomodoro) {
        this.inicioPomodoro = inicioPomodoro;
    }

    public String getEstadoPartida() {
        return estadoPartida;
    }

    public void setEstadoPartida(String estadoPartida) {
        this.estadoPartida = estadoPartida;
    }

    public int getPreguntaActual() {
        return preguntaActual;
    }

    public void setPreguntaActual(int preguntaActual) {
        this.preguntaActual = preguntaActual;
    }

    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    public String getPartidaId() {
        return partidaId;
    }

    public void setPartidaId(String partidaId) {
        this.partidaId = partidaId;
    }

    public Timestamp getInicioPartida() {
        return inicioPartida;
    }

    public void setInicioPartida(Timestamp inicioPartida) {
        this.inicioPartida = inicioPartida;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}

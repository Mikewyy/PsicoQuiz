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

    private long creadoEn;

    // ------------------------------------------------------------
    // CONSTRUCTOR VACÍO
    // Firestore lo necesita para convertir documentos en objetos.
    // ------------------------------------------------------------

    public SalaEstudio() {
    }

    // ------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------

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

        // 25 minutos
        this.duracionPomodoro = 25 * 60;

        // 5 minutos
        this.duracionDescanso = 5 * 60;

        this.inicioPomodoro = null;

        this.creadoEn = System.currentTimeMillis();
    }

    // ------------------------------------------------------------
    // GETTERS / SETTERS
    // ------------------------------------------------------------

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

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}
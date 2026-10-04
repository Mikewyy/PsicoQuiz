package com.utm.semiologia.firebase.model;

public class ParticipanteSala {

    private String uid;
    private String nombre;
    private boolean anfitrion;
    private long unidoEn;

    public ParticipanteSala() {
    }

    public ParticipanteSala(
            String uid,
            String nombre,
            boolean anfitrion
    ) {
        this.uid = uid;
        this.nombre = nombre;
        this.anfitrion = anfitrion;
        this.unidoEn = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isAnfitrion() {
        return anfitrion;
    }

    public void setAnfitrion(boolean anfitrion) {
        this.anfitrion = anfitrion;
    }

    public long getUnidoEn() {
        return unidoEn;
    }

    public void setUnidoEn(long unidoEn) {
        this.unidoEn = unidoEn;
    }
}
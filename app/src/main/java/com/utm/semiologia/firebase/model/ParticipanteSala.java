package com.utm.semiologia.firebase.model;

public class ParticipanteSala {

    private String uid;
    private String nombre;
    private boolean anfitrion;
    private String avatarId;
    private long unidoEn;

    public ParticipanteSala() {
    }

    public ParticipanteSala(
            String uid,
            String nombre,
            boolean anfitrion
    ) {
        this(uid, nombre, anfitrion, "avatar_01");
    }

    public ParticipanteSala(
            String uid,
            String nombre,
            boolean anfitrion,
            String avatarId
    ) {
        this.uid = uid;
        this.nombre = nombre;
        this.anfitrion = anfitrion;
        this.avatarId = normalizarAvatar(avatarId);
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


    public String getAvatarId() {
        return normalizarAvatar(avatarId);
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = normalizarAvatar(avatarId);
    }

    private static String normalizarAvatar(String avatarId) {
        if (avatarId == null) return "avatar_01";
        String value = avatarId.trim();
        if (!value.matches("avatar_0[1-6]")) return "avatar_01";
        return value;
    }

    public long getUnidoEn() {
        return unidoEn;
    }

    public void setUnidoEn(long unidoEn) {
        this.unidoEn = unidoEn;
    }
}
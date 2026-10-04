package com.utm.semiologia.firebase.model;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class SalaFirebase {

    private String id;
    private String codigo;
    private String nombre;
    private String creadorUid;
    private Timestamp creadaEn;

    // uid -> nombre del miembro
    private Map<String, String> miembros;


    // =========================================================
    // CONSTRUCTOR VACÍO
    // Firebase lo necesita obligatoriamente
    // =========================================================

    public SalaFirebase() {
        miembros = new HashMap<>();
    }


    // =========================================================
    // CONSTRUCTOR PRINCIPAL
    // =========================================================

    public SalaFirebase(
            String codigo,
            String nombre,
            String creadorUid,
            Timestamp creadaEn
    ) {

        this.codigo = codigo;
        this.nombre = nombre;
        this.creadorUid = creadorUid;
        this.creadaEn = creadaEn;

        this.miembros = new HashMap<>();
    }


    // =========================================================
    // GETTERS Y SETTERS
    // =========================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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


    public String getCreadorUid() {
        return creadorUid;
    }

    public void setCreadorUid(String creadorUid) {
        this.creadorUid = creadorUid;
    }


    public Timestamp getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(Timestamp creadaEn) {
        this.creadaEn = creadaEn;
    }


    public Map<String, String> getMiembros() {
        return miembros;
    }

    public void setMiembros(Map<String, String> miembros) {

        if (miembros == null) {
            this.miembros = new HashMap<>();
        } else {
            this.miembros = miembros;
        }
    }


    // =========================================================
    // UTILIDADES
    // =========================================================

    public void agregarMiembro(
            String uid,
            String nombre
    ) {

        if (miembros == null) {
            miembros = new HashMap<>();
        }

        miembros.put(
                uid,
                nombre
        );
    }


    public boolean tieneMiembro(
            String uid
    ) {

        return miembros != null
                && miembros.containsKey(uid);
    }


    public int cantidadMiembros() {

        return miembros == null
                ? 0
                : miembros.size();
    }
}
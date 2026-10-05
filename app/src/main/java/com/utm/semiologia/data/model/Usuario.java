package com.utm.semiologia.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Estudiante registrado. 1 usuario -> 1 mascota, N notas, N secciones en progreso.
 *
 * La racha se materializa en dos columnas (racha_actual / racha_maxima) que se
 * actualizan de forma atómica en cada acción que cuenta como "estudio".
 * La fuente de verdad histórica es la tabla actividad_diaria.
 */
public class Usuario {

    private long    id;
    private String  nombre;
    @Nullable private String  nombreMostrado;  // alias elegido por el usuario
    private String  email;
    private String  passwordHash;
    private String  passwordSalt;
    private int     puntos;
    private int     nivel;
    private int     experiencia;
    private int     rachaActual;
    private int     rachaMaxima;
    @Nullable private String ultimaActividadFecha;  // 'YYYY-MM-DD'
    @Nullable private Long  grupoId;
    private String  avatar;
    private long    creadoEn;

    public Usuario() {
    }

    public static Usuario crear(String nombre, String email, String passwordHash, String passwordSalt) {
        long ahora = System.currentTimeMillis();
        Usuario u = new Usuario();
        u.nombre = nombre;
        u.email = email;
        u.passwordHash = passwordHash;
        u.passwordSalt = passwordSalt;
        u.puntos = 0;
        u.nivel = 1;
        u.experiencia = 0;
        u.rachaActual = 0;
        u.rachaMaxima = 0;
        u.creadoEn = ahora;
        return u;
    }

    /**
     * Suma puntos y recalcula el nivel.
     * El nivel se deriva de los puntos acumulados: nivel n requiere
     * 100*n*(n-1)/2 puntos, es decir, un crecimiento aproximadamente cuadrático
     * para que los primeros niveles lleguen rápido y los finales cuesten.
     */
    public void sumarPuntos(int puntos) {
        this.puntos += puntos;
        this.experiencia += puntos;
        this.nivel = nivelPara(this.puntos);
    }

    public void restarPuntos(int puntos) {
        this.puntos = Math.max(0, this.puntos - puntos);
        this.nivel = nivelPara(this.puntos);
    }

    /** Puntos necesarios para pasar del nivel actual al siguiente. */
    public int puntosParaSiguienteNivel() {
        int siguiente = this.nivel + 1;
        return puntosMinimosNivel(siguiente) - this.puntos;
    }

    /** Progreso 0..100 dentro del nivel actual. */
    public int progresoNivel() {
        int base = puntosMinimosNivel(this.nivel);
        int techo = puntosMinimosNivel(this.nivel + 1);
        if (techo <= base) return 100;
        return (int) ((puntos - base) * 100L / (techo - base));
    }

    // ---- Fórmulas de nivel (compartidas con el backend) ----
    public static int puntosMinimosNivel(int nivel) {
        return 100 * nivel * (nivel - 1) / 2;
    }

    public static int nivelPara(int puntosAcumulados) {
        int n = 1;
        while (puntosMinimosNivel(n + 1) <= puntosAcumulados && n < 999) {
            n++;
        }
        return n;
    }

    // ---- Getters / Setters ----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Nullable public String getNombreMostrado() { return nombreMostrado; }

    public void setNombreMostrado(String nombreMostrado) {
        this.nombreMostrado = nombreMostrado;
    }

    /**
     * Nombre tal y como debe verse en la app.
     *
     * Si el usuario eligió un alias se usa tal cual (puede ser compuesto,
     * no se recorta al primer nombre); si no, se cae al nombre del registro.
     */
    @NonNull public String getNombreParaSaludo() {
        if (nombreMostrado != null && !nombreMostrado.trim().isEmpty()) {
            return nombreMostrado.trim();
        }
        return nombre != null ? nombre : "";
    }

    @NonNull public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @NonNull public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) { this.passwordHash = h; }

    @NonNull public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String s) { this.passwordSalt = s; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int p) { this.puntos = p; }

    public int getNivel() { return nivel; }
    public void setNivel(int n) { this.nivel = n; }

    public int getExperiencia() { return experiencia; }
    public void setExperiencia(int e) { this.experiencia = e; }

    public int getRachaActual() { return rachaActual; }
    public void setRachaActual(int r) { this.rachaActual = r; }

    public int getRachaMaxima() { return rachaMaxima; }
    public void setRachaMaxima(int r) { this.rachaMaxima = r; }

    @Nullable public String getUltimaActividadFecha() { return ultimaActividadFecha; }
    public void setUltimaActividadFecha(@Nullable String f) { this.ultimaActividadFecha = f; }

    @Nullable public Long getGrupoId() { return grupoId; }
    public void setGrupoId(@Nullable Long g) { this.grupoId = g; }

    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long t) { this.creadoEn = t; }

    public String getAvatar() { return avatar != null ? avatar : "👤"; }
    public void setAvatar(String a) { this.avatar = a; }

    /** Iniciales para el avatar del dashboard: "Ana Lucía Pérez" -> "AL" */
    @NonNull
    public String getIniciales() {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }
}

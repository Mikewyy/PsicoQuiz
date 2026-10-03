package com.utm.semiologia.data.model;

import java.util.Locale;

/**
 * Mascota virtual del usuario. 1 usuario -> 1 mascota.
 *
 * REGLA DE ORO DEL DECAIMIENTO
 * ---------------------------
 * El hambre NO se descuenta "en vivo" con un temporizador: se persiste el nivel
 * de hambre junto con la marca de tiempo {@link #hambreActualizadaEn} y el
 * descuento se calcula por diferencia al abrir la app.
 *
 * Esto evita que la app pierda el progreso si el proceso muere, el dispositivo
 * se reinicia o el usuario hace "force stop", y permite que el backend resuelva
 * el mismo cálculo si el usuario cambia de dispositivo.
 *
 * Con el requerimiento de 1 punto por hora:
 *   - 1 punto cada 60 min
 *   - las fracciones de hora se ACUMULAN (no se pierden entre dos visitas)
 *   - el método es idempotente: llamarlo N veces en el mismo instante no descuenta N veces
 */
public class Mascota {

    // ---- Parámetros de balance (centralizados para poder tunearlos) ----
    public static final int PUNTOS_HAMBRE_POR_HORA   = 1;    // requerimiento: 1 punto/hora
    public static final long MS_POR_HORA             = 3_600_000L;
    public static final long MS_POR_DIA              = 86_400_000L;

    /** Cada cuántas horas de hambre baja la felicidad (por estar hambrienta). */
    public static final long HORAS_POR_PENALIZACION = 6;
    /** Puntos de felicidad perdidos por cada periodo de penalización. */
    public static final int PENALIZACION_FELICIDAD = 5;
    /** Puntos de energía recuperados por hora en reposo. */
    public static final int ENERGIA_POR_HORA_REPOSO = 2;
    /** Por debajo de este valor de hambre la mascota entra en modo crítico. */
    public static final int UMBRAL_HAMBRE_CRITICO = 20;
    /** Por debajo de este valor la mascota está en riesgo de desmayarse. */
    public static final int UMBRAL_HAMBRE_MINIMO = 5;

    // ---- Estados posibles ----
    public static final String ESTADO_FELIZ       = "feliz";
    public static final String ESTADO_HAMBRIENTO  = "hambriento";
    public static final String ESTADO_CRITICO     = "critico";
    public static final String ESTADO_TRISTE      = "triste";
    public static final String ESTADO_CANSADO     = "cansado";

    // ---- Campos ----
    private long    id;
    private long    usuarioId;
    private String  nombre;
    private String  especie;
    private int     hambre;        // 0..100 (100 = alimentado)
    private int     felicidad;     // 0..100
    private int     energia;       // 0..100
    private String  estado;
    private Long    accesorioEquipadoId;
    private int     skinId;
    private long    hambreActualizadaEn;  // reloj del decaimiento
    private long    creadoEn;

    public Mascota() {
    }

    /** Crea la mascota que se otorga al registrarse. */
    public static Mascota crearPorDefecto(long usuarioId, String nombreUsuario) {
        long ahora = System.currentTimeMillis();
        Mascota m = new Mascota();
        m.usuarioId = usuarioId;
        m.nombre = "Mateo";
        m.especie = "gato";
        m.skinId = 0;
        m.hambre = 100;
        m.felicidad = 80;
        m.energia = 100;
        m.estado = ESTADO_FELIZ;
        m.hambreActualizadaEn = ahora;
        m.creadoEn = ahora;
        return m;
    }

    // ==================================================================
    // LÓGICA DE DECAIMIENTO POR TIEMPO TRANSCURRIDO
    // ==================================================================

    /**
     * Aplica el decaimiento acumulado desde la última actualización.
     *
     * <p>Devuelve cuántos puntos de hambre se perdieron en esta llamada.
     * Llamarla reiteradamente es seguro: sólo descuenta el tiempo que realmente
     * ha pasado, porque {@link #hambreActualizadaEn} avanza por horas
     * completas y conserva el resto (los minutos sobrantes).
     *
     * @param ahoraMs marca de tiempo actual (epoch millis)
     * @return puntos de hambre perdidos en esta llamada
     */
    public int aplicarDecaimiento(long ahoraMs) {
        if (ahoraMs <= hambreActualizadaEn) {
            // Reloj del dispositivo atrasado, o dos llamadas en el mismo
            // instante: no hay tiempo que cobrar.
            recalcularEstado();
            return 0;
        }

        long transcurrido = ahoraMs - hambreActualizadaEn;
        long horasCompletas = transcurrido / MS_POR_HORA;

        int perdidos = 0;

        if (horasCompletas > 0) {
            perdidos = (int) Math.min(horasCompletas, Integer.MAX_VALUE) * PUNTOS_HAMBRE_POR_HORA;

            // AVANCE POR HORAS COMPLETAS: así el resto (minutos sueltos)
            // se acumula para la próxima llamada en vez de evaporarse.
            hambreActualizadaEn += horasCompletas * MS_POR_HORA;

            hambre = limitar(hambre - perdidos, 0, 100);

            // Hambrienta por tiempo prolongado => la felicidad también cae.
            if (hambre < UMBRAL_HAMBRE_CRITICO) {
                int periodos = (int) Math.min(
                        (horasCompletas / HORAS_POR_PENALIZACION), Integer.MAX_VALUE);
                if (periodos > 0) {
                    felicidad = limitar(felicidad - periodos * PENALIZACION_FELICIDAD, 0, 100);
                }
            }
        }

        // Energía: se recupera lentamente si está alimentada; se agota si no.
        if (hambre >= 50) {
            energia = limitar(energia + (int) Math.min(horasCompletas, Integer.MAX_VALUE) * ENERGIA_POR_HORA_REPOSO, 0, 100);
        }

        recalcularEstado();
        return perdidos;
    }

    /** Atajo de {@link #aplicarDecaimiento(long)} usando el reloj del sistema. */
    public int aplicarDecaimiento() {
        return aplicarDecaimiento(System.currentTimeMillis());
    }

    /**
     * Proyecta cuántos puntos de hambre le faltan para llegar a 0.
     * Útil para mostrar avisos ("seikipará en 2 días") sin tocar la BD.
     */
    public long horasHastaHambrientoTotal() {
        if (hambre <= 0) return 0;
        return (long) Math.ceil((double) hambre / PUNTOS_HAMBRE_POR_HORA);
    }

    /**
     * Alimenta a la mascota. Devuelve false si no queda comida.
     * Alimentar también sube la energía (de ahí el nombre "galletita de la suerte").
     */
    public boolean alimentar(int puntosHambre, int puntosFelicidad, int puntosEnergia) {
        if (puntosHambre <= 0) return false;
        hambre = limitar(hambre + puntosHambre, 0, 100);
        felicidad = limitar(felicidad + puntosFelicidad, 0, 100);
        energia = limitar(energia + puntosEnergia, 0, 100);
        // Comer reinicia el reloj del decaimiento.
        hambreActualizadaEn = System.currentTimeMillis();
        recalcularEstado();
        return true;
    }

    /**
     * Gasta energía (p. ej. al empezar un intento). NUNCA falla: si no alcanza,
     * la energía baja a 0. Jugar no debe bloquearse nunca por falta de energía.
     */
    public void consumirEnergia(int costo) {
        if (costo <= 0) return;
        energia = limitar(energia - Math.min(costo, energia), 0, 100);
        recalcularEstado();
    }

    /** Minijuego rápido: sube felicidad, gasta energía. */
    public boolean jugar(int puntosFelicidad, int costoEnergia) {
        if (energia < costoEnergia) return false;
        energia = limitar(energia - costoEnergia, 0, 100);
        felicidad = limitar(felicidad + puntosFelicidad, 0, 100);
        recalcularEstado();
        return true;
    }

    /** Deriva el estado textual a partir de las tres barras. */
    public void recalcularEstado() {
        if (hambre <= UMBRAL_HAMBRE_MINIMO) {
            estado = ESTADO_CRITICO;
        } else if (hambre < UMBRAL_HAMBRE_CRITICO) {
            estado = ESTADO_HAMBRIENTO;
        } else if (energia < 20) {
            estado = ESTADO_CANSADO;
        } else if (felicidad < 30) {
            estado = ESTADO_TRISTE;
        } else {
            estado = ESTADO_FELIZ;
        }
    }

    /** Color de la barra de hambre: verde -> ámbar -> rojo. */
    public int colorHambre() {
        if (hambre < UMBRAL_HAMBRE_CRITICO) return 0xFFE53935;   // rojo
        if (hambre < 50)                 return 0xFFFFA726;       // ámbar
        return 0xFF43A047;                                        // verde
    }

    public boolean estaCritica() {
        return hambre <= UMBRAL_HAMBRE_CRITICO;
    }

    private static int limitar(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    // ==================================================================
    // Getters / Setters
    // ==================================================================
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public int getHambre() { return hambre; }
    public void setHambre(int hambre) { this.hambre = hambre; }

    public int getFelicidad() { return felicidad; }
    public void setFelicidad(int felicidad) { this.felicidad = felicidad; }

    public int getEnergia() { return energia; }
    public void setEnergia(int energia) { this.energia = energia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getAccesorioEquipadoId() { return accesorioEquipadoId; }
    public void setAccesorioEquipadoId(Long id) { this.accesorioEquipadoId = id; }

    public long getHambreActualizadaEn() { return hambreActualizadaEn; }
    public void setHambreActualizadaEn(long t) { this.hambreActualizadaEn = t; }

    public int getSkinId() { return skinId; }
    public void setSkinId(int id) { this.skinId = id; }

    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long t) { this.creadoEn = t; }

    /** "gato" -> "Gato" para mostrar en la UI. */
    public String getEspecieFormateada() {
        if (especie == null || especie.isEmpty()) return "";
        return especie.substring(0, 1).toUpperCase(Locale.ROOT) + especie.substring(1);
    }
}

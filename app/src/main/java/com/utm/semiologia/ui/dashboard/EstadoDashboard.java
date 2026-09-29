package com.utm.semiologia.ui.dashboard;

import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;

/**
 * Fotografía completa del dashboard en un solo objeto.
 *
 * Se agrupa todo en un valor único para que la Activity observe un solo
 * LiveData y pinnee la pantalla una sola vez, evitando el parpadeo que
 * aparecería si cada barra fuera un observer independiente.
 */
public class EstadoDashboard {

    public Usuario usuario;
    public Mascota mascota;

    // Métricas de estudio para la cabecera
    public int    seccionesCompletadas;
    public int    seccionesTotales;
    public int    minutosSemana;
    public int    ciclosTotales;

    // Señales a la UI
    public boolean rachaEnRiesgo;      // estudió ayer pero todavía no hoy
    public boolean mascotaCritica;
    public String  mensajeEvento;      // feedback de la última acción (toast)

    public EstadoDashboard(Usuario usuario, Mascota mascota, int seccionesCompletadas,
                           int seccionesTotales, int minutosSemana, int ciclosTotales,
                           boolean rachaEnRiesgo, boolean mascotaCritica, String mensajeEvento) {
        this.usuario = usuario;
        this.mascota = mascota;
        this.seccionesCompletadas = seccionesCompletadas;
        this.seccionesTotales = seccionesTotales;
        this.minutosSemana = minutosSemana;
        this.ciclosTotales = ciclosTotales;
        this.rachaEnRiesgo = rachaEnRiesgo;
        this.mascotaCritica = mascotaCritica;
        this.mensajeEvento = mensajeEvento;
    }

    /** Progreso de lectura 0..100 para el indicador de la guía. */
    public int progresoLectura() {
        if (seccionesTotales == 0) return 0;
        return Math.round(seccionesCompletadas * 100f / seccionesTotales);
    }
}

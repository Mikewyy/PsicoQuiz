package com.utm.semiologia.ui.evaluacion;

import com.utm.semiologia.data.model.ProgresoNivel;

import java.util.List;

/** Estado inmutable de la pantalla "Camino de aprendizaje". */
public class EstadoCamino {

    public final int puntos;
    public final List<ProgresoNivel> tramos;

    public EstadoCamino(int puntos, List<ProgresoNivel> tramos) {
        this.puntos = puntos;
        this.tramos = tramos;
    }
}

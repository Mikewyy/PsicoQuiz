package com.utm.semiologia;

import android.app.Application;

import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.util.SesionManager;

/**
 * Punto de entrada de la aplicación. Inicializa singletons que deben existir
 * durante todo el ciclo de vida (repositorio y sesión).
 */
public class SemiologiaApp extends Application {

    private static Repositorio  repositorio;
    private static SesionManager sesion;

    @Override
    public void onCreate() {
        super.onCreate();
        repositorio = Repositorio.get(this);
        sesion = new SesionManager(this);
    }

    public static Repositorio getRepositorio() {
        return repositorio;
    }

    public static SesionManager getSesion() {
        return sesion;
    }
}

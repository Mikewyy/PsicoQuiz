package com.utm.semiologia;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.ui.pomodoro.PomodoroManager;
import com.utm.semiologia.util.NotificacionesUtil;
import com.utm.semiologia.util.SesionManager;

/**
 * Punto de entrada de la aplicación. Inicializa singletons que deben existir
 * durante todo el ciclo de vida (repositorio y sesión).
 */
public class SemiologiaApp extends Application implements DefaultLifecycleObserver {

    private static Repositorio  repositorio;
    private static SesionManager sesion;

    @Override
    public void onCreate() {
        super.onCreate();
        repositorio = Repositorio.get(this);
        sesion = new SesionManager(this);

        NotificacionesUtil.crearCanal(this);

        PomodoroManager.init(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // Sin servicio en segundo plano: si toda la app pasa a segundo plano,
        // el pomodoro se cancela.
        PomodoroManager.get().cancelar();
    }

    public static Repositorio getRepositorio() {
        return repositorio;
    }

    public static SesionManager getSesion() {
        return sesion;
    }
}

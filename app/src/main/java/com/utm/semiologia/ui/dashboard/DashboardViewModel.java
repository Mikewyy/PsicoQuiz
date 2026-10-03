package com.utm.semiologia.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.util.Gamificacion;
import com.utm.semiologia.util.SesionManager;

/**
 * ViewModel del dashboard.
 *
 * Concentra la lectura/escritura de datos para que la Activity no toque SQL
 * y sobreviva a las rotaciones sin volver a consultar la base.
 *
 * IMPORTANTE: el decaimiento del hambre se materializa aquí, en
 * {@link #cargar()}, que es el único punto de entrada tras un arranque o un
 * regreso del segundo plano. Nunca se corre un hilo en background para
 * "contar" el tiempo: el tiempo se calcula por diferencia de marcas de tiempo.
 */
public class DashboardViewModel extends AndroidViewModel {

    /** Alimento que se consume con el botón "Alimentar" del dashboard. */
    private static final long ALIMENTO_POR_DEFECTO = 1L; // Galleta
    private static final int PUNTOS_HAMBRE_GALLETA  = 10;
    private static final int PUNTOS_FELICIDAD_GALLETA = 3;
    private static final int PUNTOS_ENERGIA_GALLETA   = 10;

    private final Repositorio  repo;
    private final SesionManager sesion;

    private final MutableLiveData<EstadoDashboard> estado = new MutableLiveData<>();
    private String mensajePendiente;

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        this.repo = Repositorio.get(app);
        this.sesion = new SesionManager(app);
    }

    public LiveData<EstadoDashboard> getEstado() {
        return estado;
    }

    /** Relee todo y aplica el decaimiento del hambre. Llamar en onResume. */
    public void cargar() {
        long usuarioId = sesion.getUsuarioId();
        if (usuarioId <= 0) return;

        Usuario u = repo.usuarios().buscarPorId(usuarioId);
        if (u == null) {
            // El usuario se borró de la BD (o se desinstaló): cerrar sesión.
            sesion.cerrarSesion();
            return;
        }

        // Aquí se materializa el paso del tiempo de la mascota.
        Mascota m = repo.mascotas().obtenerYActualizar(usuarioId);

        int completadas = repo.estudio().seccionesCompletadas(usuarioId);
        int totales = repo.estudio().seccionesTotales();
        int minutosSemana = repo.pomodoro().minutosUltimaSemana(usuarioId);
        int ciclos = repo.pomodoro().totalCiclos(usuarioId);

        boolean rachaRiesgo = Gamificacion.rachaEnRiesgo(u.getUltimaActividadFecha());

        String mensaje = mensajePendiente;
        mensajePendiente = null;

        estado.setValue(new EstadoDashboard(u, m, completadas, totales, minutosSemana,
                ciclos, rachaRiesgo, m != null && m.estaCritica(), mensaje));
    }

    /** Consume una galleta del inventario y alimenta a la mascota. */
    public boolean alimentar() {
        long usuarioId = sesion.getUsuarioId();
        Mascota m = repo.mascotas().obtener(usuarioId);
        if (m == null) return false;
        if (m.getHambre() >= 100 && m.getEnergia() >= 100) {
            mensajePendiente = "Tu mascota ya está bien alimentada y descansada";
            cargar();
            return false;
        }
        boolean ok = repo.mascotas().consumirAlimentoYAlimentar(usuarioId, ALIMENTO_POR_DEFECTO,
                PUNTOS_HAMBRE_GALLETA, PUNTOS_FELICIDAD_GALLETA, PUNTOS_ENERGIA_GALLETA);
        mensajePendiente = ok ? "¡Tu mascota está feliz!" : "No tienes comida. ¡Estudia para ganarla!";
        cargar();
        return ok;
    }
}

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

    // Mapeo de Skins (id -> drawable)
    private static final java.util.Map<Integer, Integer> SKIN_MAP = new java.util.HashMap<>() {{
        put(0, com.utm.semiologia.R.drawable.pet_cat_nuevo);
        put(1, com.utm.semiologia.R.drawable.pet_cat_blue);
        put(2, com.utm.semiologia.R.drawable.pet_cat_purple);
        put(3, com.utm.semiologia.R.drawable.pet_cat_student);
        put(4, com.utm.semiologia.R.drawable.pet_cat_space);
        put(5, com.utm.semiologia.R.drawable.pet_cat_golden);
    }};

    // Mapeo de Avatares (id -> drawable)
    private static final java.util.Map<String, Integer> AVATAR_MAP = new java.util.HashMap<>() {{
        put("avatar_01", com.utm.semiologia.R.drawable.avatar_01);
        put("avatar_02", com.utm.semiologia.R.drawable.avatar_02);
        put("avatar_03", com.utm.semiologia.R.drawable.avatar_03);
        put("avatar_04", com.utm.semiologia.R.drawable.avatar_04);
        put("avatar_05", com.utm.semiologia.R.drawable.avatar_05);
        put("avatar_06", com.utm.semiologia.R.drawable.avatar_06);
    }};

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
    public void cambiarAvatar(String avatar) {
        long usuarioId = sesion.getUsuarioId();
        if (usuarioId <= 0) return;
        repo.usuarios().actualizarAvatar(usuarioId, avatar);
        cargar();
    }

    public void cambiarNombreMascota(String nuevoNombre) {
        long usuarioId = sesion.getUsuarioId();
        if (usuarioId <= 0) return;
        repo.mascotas().actualizarNombre(usuarioId, nuevoNombre);
        cargar();
    }

    public void cambiarSkinMascota(int skinId) {
        long usuarioId = sesion.getUsuarioId();
        if (usuarioId <= 0) return;
        repo.mascotas().actualizarSkin(usuarioId, skinId);
        cargar();
    }

    public int getDrawableForSkin(int skinId) {
        return SKIN_MAP.getOrDefault(skinId, com.utm.semiologia.R.drawable.pet_cat_nuevo);
    }

    public int getDrawableForAvatar(String avatarId) {
        return AVATAR_MAP.getOrDefault(avatarId, com.utm.semiologia.R.drawable.avatar_01);
    }

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

    public int getCantidadComida(long usuarioId) {
        return repo.mascotas().cantidadAlimento(usuarioId, ALIMENTO_POR_DEFECTO);
    }
}

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
 */
public class DashboardViewModel extends AndroidViewModel {

    private static final long ALIMENTO_POR_DEFECTO = 1L;

    private static final int PUNTOS_HAMBRE_GALLETA = 10;
    private static final int PUNTOS_FELICIDAD_GALLETA = 3;
    private static final int PUNTOS_ENERGIA_GALLETA = 10;

    private final Repositorio repo;
    private final SesionManager sesion;

    private final MutableLiveData<EstadoDashboard> estado =
            new MutableLiveData<>();

    private String mensajePendiente;


    // =========================================================
    // SKINS
    // =========================================================

    private static final java.util.Map<Integer, Integer> SKIN_MAP =
            new java.util.HashMap<Integer, Integer>() {{

                put(
                        0,
                        com.utm.semiologia.R.drawable.pet_cat_nuevo
                );

                put(
                        1,
                        com.utm.semiologia.R.drawable.pet_cat_blue
                );

                put(
                        2,
                        com.utm.semiologia.R.drawable.pet_cat_purple
                );

                put(
                        3,
                        com.utm.semiologia.R.drawable.pet_cat_student
                );

                put(
                        4,
                        com.utm.semiologia.R.drawable.pet_cat_space
                );

                put(
                        5,
                        com.utm.semiologia.R.drawable.pet_cat_golden
                );
            }};


    // =========================================================
    // AVATARES
    // =========================================================

    private static final java.util.Map<String, Integer> AVATAR_MAP =
            new java.util.HashMap<String, Integer>() {{

                put(
                        "avatar_01",
                        com.utm.semiologia.R.drawable.avatar_01
                );

                put(
                        "avatar_02",
                        com.utm.semiologia.R.drawable.avatar_02
                );

                put(
                        "avatar_03",
                        com.utm.semiologia.R.drawable.avatar_03
                );

                put(
                        "avatar_04",
                        com.utm.semiologia.R.drawable.avatar_04
                );

                put(
                        "avatar_05",
                        com.utm.semiologia.R.drawable.avatar_05
                );

                put(
                        "avatar_06",
                        com.utm.semiologia.R.drawable.avatar_06
                );
            }};


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardViewModel(
            @NonNull Application app
    ) {

        super(app);

        this.repo =
                Repositorio.get(app);

        this.sesion =
                new SesionManager(app);
    }


    // =========================================================
    // ESTADO
    // =========================================================

    public LiveData<EstadoDashboard> getEstado() {

        return estado;
    }


    // =========================================================
    // CARGAR DASHBOARD
    // =========================================================

    public void cargar() {

        long usuarioId =
                sesion.getUsuarioId();


        if (usuarioId <= 0) {

            return;
        }


        // -----------------------------------------------------
        // USUARIO
        // -----------------------------------------------------

        Usuario u =
                repo.usuarios()
                        .buscarPorId(
                                usuarioId
                        );


        if (u == null) {

            sesion.cerrarSesion();

            return;
        }


        // =====================================================
        // MASCOTA
        // =====================================================

        Mascota m =
                repo.mascotas()
                        .obtenerYActualizar(
                                usuarioId
                        );


        /*
         * IMPORTANTE:
         *
         * Si por una migración anterior existe el usuario
         * pero no existe su mascota, la creamos automáticamente.
         */
        if (m == null) {

            Mascota mascotaNueva =
                    Mascota.crearPorDefecto(
                            usuarioId,
                            u.getNombre()
                    );


            long mascotaId =
                    repo.mascotas()
                            .insertar(
                                    mascotaNueva
                            );


            /*
             * Si logramos crear la mascota,
             * entregamos los alimentos iniciales.
             */
            if (mascotaId > 0) {

                repo.mascotas()
                        .otorgarAlimento(
                                usuarioId,
                                ALIMENTO_POR_DEFECTO,
                                3
                        );


                mensajePendiente =
                        "Tu mascota fue restaurada correctamente";
            }


            /*
             * Volvemos a leer la mascota desde SQLite.
             */
            m =
                    repo.mascotas()
                            .obtenerYActualizar(
                                    usuarioId
                            );
        }


        // =====================================================
        // PROGRESO
        // =====================================================

        int completadas =
                repo.estudio()
                        .seccionesCompletadas(
                                usuarioId
                        );


        int totales =
                repo.estudio()
                        .seccionesTotales();


        int minutosSemana =
                repo.pomodoro()
                        .minutosUltimaSemana(
                                usuarioId
                        );


        int ciclos =
                repo.pomodoro()
                        .totalCiclos(
                                usuarioId
                        );


        boolean rachaRiesgo =
                Gamificacion.rachaEnRiesgo(
                        u.getUltimaActividadFecha()
                );


        String mensaje =
                mensajePendiente;


        mensajePendiente =
                null;


        // =====================================================
        // ACTUALIZAR INTERFAZ
        // =====================================================

        estado.setValue(

                new EstadoDashboard(

                        u,

                        m,

                        completadas,

                        totales,

                        minutosSemana,

                        ciclos,

                        rachaRiesgo,

                        m != null
                                && m.estaCritica(),

                        mensaje
                )
        );
    }


    // =========================================================
    // CAMBIAR AVATAR
    // =========================================================

    public void cambiarAvatar(
            String avatar
    ) {

        long usuarioId =
                sesion.getUsuarioId();


        if (usuarioId <= 0) {

            return;
        }


        repo.usuarios()
                .actualizarAvatar(
                        usuarioId,
                        avatar
                );


        cargar();
    }


    // =========================================================
    // CAMBIAR NOMBRE DE MASCOTA
    // =========================================================

    public void cambiarNombreMascota(
            String nuevoNombre
    ) {

        long usuarioId =
                sesion.getUsuarioId();


        if (usuarioId <= 0) {

            return;
        }


        repo.mascotas()
                .actualizarNombre(
                        usuarioId,
                        nuevoNombre
                );


        cargar();
    }


    /**
     * Guarda el alias que el usuario quiere ver en la app. Si viene vacío se
     * guarda como NULL y la app vuelve a mostrar el nombre real.
     */
    public void cambiarNombreVisible(
            String nuevoNombre
    ) {

        long usuarioId =
                sesion.getUsuarioId();


        if (usuarioId <= 0) {

            return;
        }


        repo.usuarios()
                .actualizarNombreMostrado(
                        usuarioId,
                        nuevoNombre
                );


        cargar();
    }


    // =========================================================
    // CAMBIAR SKIN DE MASCOTA
    // =========================================================

    public void cambiarSkinMascota(
            int skinId
    ) {

        long usuarioId =
                sesion.getUsuarioId();


        if (usuarioId <= 0) {

            return;
        }


        if (skinId < 0 || skinId > 5) {

            skinId = 0;
        }


        repo.mascotas()
                .actualizarSkin(
                        usuarioId,
                        skinId
                );


        cargar();
    }


    // =========================================================
    // DRAWABLE DE MASCOTA
    // =========================================================

    public int getDrawableForSkin(
            int skinId
    ) {

        return SKIN_MAP.getOrDefault(

                skinId,

                com.utm.semiologia.R.drawable.pet_cat_nuevo
        );
    }


    // =========================================================
    // DRAWABLE DE AVATAR
    // =========================================================

    public int getDrawableForAvatar(
            String avatarId
    ) {

        return AVATAR_MAP.getOrDefault(

                avatarId,

                com.utm.semiologia.R.drawable.avatar_01
        );
    }


    // =========================================================
    // ALIMENTAR MASCOTA
    // =========================================================

    public boolean alimentar() {

        long usuarioId =
                sesion.getUsuarioId();


        Mascota m =
                repo.mascotas()
                        .obtener(
                                usuarioId
                        );


        if (m == null) {

            mensajePendiente =
                    "No se encontró la mascota";

            cargar();

            return false;
        }


        /*
         * Si hambre y energía ya están llenas,
         * no gastamos comida.
         */
        if (
                m.getHambre() >= 100
                        &&
                        m.getEnergia() >= 100
        ) {

            mensajePendiente =
                    "Tu mascota ya está bien alimentada y descansada";


            cargar();


            return false;
        }


        boolean ok =
                repo.mascotas()
                        .consumirAlimentoYAlimentar(

                                usuarioId,

                                ALIMENTO_POR_DEFECTO,

                                PUNTOS_HAMBRE_GALLETA,

                                PUNTOS_FELICIDAD_GALLETA,

                                PUNTOS_ENERGIA_GALLETA
                        );


        mensajePendiente =
                ok
                        ? "¡Tu mascota está feliz!"
                        : "No tienes comida. ¡Estudia para ganarla!";


        cargar();


        return ok;
    }


    // =========================================================
    // CANTIDAD DE COMIDA
    // =========================================================

    public int getCantidadComida(
            long usuarioId
    ) {

        return repo.mascotas()
                .cantidadAlimento(

                        usuarioId,

                        ALIMENTO_POR_DEFECTO
                );
    }
}
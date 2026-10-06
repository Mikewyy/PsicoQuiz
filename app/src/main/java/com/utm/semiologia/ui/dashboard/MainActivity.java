package com.utm.semiologia.ui.dashboard;
import com.utm.semiologia.ui.pomodoro.PomodoroManager;
import android.widget.SeekBar;
import android.widget.LinearLayout;
import android.view.MotionEvent;

import java.util.Locale;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.ui.auth.LoginActivity;
import com.utm.semiologia.ui.common.BaseActivity;
import com.utm.semiologia.ui.common.NavegacionInferior;
import com.utm.semiologia.ui.estudio.GuiaActivity;
import com.utm.semiologia.ui.evaluacion.CaminoActivity;



/**
 * Dashboard principal del estudiante.
 */
public class MainActivity extends BaseActivity
        implements NavegacionInferior.Navegador {


    // =========================================================
    // VISTAS
    // =========================================================
    private int dpPomodoro(
            int valor
    ) {

        return Math.round(
                valor
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
    private TextView tvSaludo;

    private TextView tvPuntos;
    private TextView tvNivel;
    private TextView tvNivelProgreso;
    private TextView tvRacha;

    private TextView tvAvisoRacha;
    private TextView tvMascotaNombre;
    private TextView tvMascotaEstado;

    private TextView tvHambreValor;
    private TextView tvFelicidadValor;
    private TextView tvEnergiaValor;

    private TextView tvProgresoLectura;
    private TextView tvPomodoroResumen;
    private TextView tvComidaCantidad;

    private LinearProgressIndicator barHambre;
    private LinearProgressIndicator barFelicidad;
    private LinearProgressIndicator barEnergia;
    private LinearProgressIndicator barProgreso;

    private Button btnAlimentar;
    private Button btnJugar;

    private ImageView ivAvatar;
    private ImageView ivMascota;

    private DashboardViewModel viewModel;
    // =========================================================
// POMODORO FLOTANTE
// =========================================================
    private float pomodoroDownX;
    private float pomodoroDownY;

    private float pomodoroInicioX;
    private float pomodoroInicioY;

    private boolean arrastrandoPomodoro = false;
    private LinearLayout panelPomodoro;
    private LinearLayout pestanaPomodoro;

    private TextView tvPomodoroModo;
    private TextView tvPomodoroTiempo;
    private TextView tvPomodoroSeleccion;
    private TextView tvPomodoroCiclos;
    private TextView tvPomodoroMiniTiempo;

    private SeekBar seekPomodoro;

    private Button btnPomodoroIniciar;
    private Button btnPomodoroPausar;
    private Button btnPomodoroReiniciar;

    private PomodoroManager pomodoroManager;

    private boolean pomodoroMinimizado = false;

    /**
     * Lado en el que quedó anclada la pestaña del pomodoro. El panel se abre
     * y se cierra siempre hacia este mismo lado.
     */
    private boolean pomodoroEnBordeIzquierdo = false;

    private static final String ESTADO_POMODORO_IZQUIERDA =
            "pomodoro_borde_izquierdo";

    private static final String ESTADO_POMODORO_X =
            "pomodoro_x_pestana";


    // =========================================================
    // SECCIONES DE LA BARRA INFERIOR
    // =========================================================

    /**
     * Las cuatro secciones viven como hijas del mismo FrameLayout: solo una
     * está visible a la vez. Desafíos y Ayuda comparten una única tarjeta y
     * solo cambian sus textos.
     */
    private View seccionInicio;
    private View seccionExplorar;
    private View seccionProximamente;

    private TextView tvProxTitulo;
    private TextView tvProxEtiqueta;
    private TextView tvProxEmoji;
    private TextView tvProxMensaje;
    private TextView tvProxBadge;

    private int seccionActual = NavegacionInferior.SECCION_INICIO;

    private BottomNavigationView barraInferior;

    private static final String ESTADO_SECCION = "seccion_nav_activa";


    // =========================================================
    // CREACIÓN
    // =========================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        if (!SemiologiaApp.getSesion().haySesion()) {

            irAlLogin();

            return;
        }


        setContentView(
                R.layout.activity_main
        );

        //Boton informacion Hambre

        ImageView btnInfoHambre = findViewById(R.id.btn_info_hambre);
        TextView tvHambreHint = findViewById(R.id.tvHambreHint);

        btnInfoHambre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvHambreHint.getVisibility() == View.GONE) {
                    tvHambreHint.setVisibility(View.VISIBLE);
                } else {
                    tvHambreHint.setVisibility(View.GONE);
                }
            }
        });

        //Boton informacion Felicidad

        ImageView btnInfoFelicidad = findViewById(R.id.btn_info_felicidad);
        TextView tvFelicidadHint = findViewById(R.id.tvFelicidadHint);

        btnInfoFelicidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvFelicidadHint.getVisibility() == View.GONE) {
                    tvFelicidadHint.setVisibility(View.VISIBLE);
                } else {
                    tvFelicidadHint.setVisibility(View.GONE);
                }
            }
        });

        //Boton informacion Energia

        ImageView btnInfoEnergia = findViewById(R.id.btn_info_energia);
        TextView tvEnergiaHint = findViewById(R.id.tvEnergiaHint);

        btnInfoEnergia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tvEnergiaHint.getVisibility() == View.GONE) {
                    tvEnergiaHint.setVisibility(View.VISIBLE);
                } else {
                    tvEnergiaHint.setVisibility(View.GONE);
                }
            }
        });

        PomodoroManager.init(this);
        pomodoroManager = PomodoroManager.get();

        viewModel =
                new ViewModelProvider(this)
                        .get(
                                DashboardViewModel.class
                        );

        enlazarVistas();
        configurarPomodoro();
        configurarListeners();

        /*
         * Si venimos desde una pantalla interna con la barra, el extra dice en
         * qué sección hay que caer. Se lee antes de configurar la barra para
         * que quede marcada directamente en esa sección.
         */
        int seccionPedida =
                getIntent()
                        .getIntExtra(
                                NavegacionInferior.EXTRA_SECCION,
                                NavegacionInferior.SECCION_INICIO
                        );

        seccionActual = seccionPedida;
        irASeccion(seccionPedida);

        configurarNavInferior(seccionActual);


        viewModel
                .getEstado()
                .observe(
                        this,
                        this::pintar
                );


        viewModel.cargar();
    }


    // =========================================================
    // NAVEGACIÓN ENTRE SECCIONES
    // =========================================================

    /**
     * La barra pide un cambio de sección. Solo alterna la visibilidad de los
     * contenedores: ni el scroll ni el pomodoro se tocan.
     */
    @Override
    public void irASeccion(int seccion) {

        seccionActual = seccion;

        if (seccionInicio != null) {
            seccionInicio.setVisibility(
                    seccion == NavegacionInferior.SECCION_INICIO
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (seccionExplorar != null) {
            seccionExplorar.setVisibility(
                    seccion == NavegacionInferior.SECCION_EXPLORAR
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        boolean esProximamente =
                seccion == NavegacionInferior.SECCION_DESAFIOS
                        || seccion == NavegacionInferior.SECCION_AYUDA;

        if (seccionProximamente != null) {
            seccionProximamente.setVisibility(
                    esProximamente
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (esProximamente) {
            pintarProximamente(seccion);
        }

        /*
         * El item marcado se actualiza aquí y no solo desde el listener porque
         * la sección también puede cambiar por el botón atrás o por un onNewIntent.
         * Volver a marcar el que ya estaba no dispara el listener: Material
         * enruta ese caso al onItemReselectedListener.
         */
        if (barraInferior != null) {
            barraInferior.setSelectedItemId(
                    NavegacionInferior.idItemDeSeccion(seccion)
            );
        }
    }


    private void pintarProximamente(int seccion) {

        boolean desafios =
                seccion == NavegacionInferior.SECCION_DESAFIOS;

        tvProxTitulo.setText(
                desafios
                        ? R.string.desafios_titulo
                        : R.string.ayuda_titulo
        );

        tvProxEmoji.setText(desafios ? "🏆" : "💡");

        tvProxMensaje.setText(
                desafios
                        ? R.string.desafios_mensaje
                        : R.string.ayuda_mensaje
        );
    }


    /**
     * Al volver de una pantalla interna, MainActivity se reutiliza en lugar de
     * recrearse (launchMode singleTop + CLEAR_TOP), así que el scroll y el
     * pomodoro sobreviven intactos.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        setIntent(intent);

        int seccion =
                intent.getIntExtra(
                        NavegacionInferior.EXTRA_SECCION,
                        NavegacionInferior.SECCION_INICIO
                );

        irASeccion(seccion);
    }


    /**
     * Si no estamos en Inicio, el botón atrás vuelve a Inicio en lugar de
     * dejar la app.
     */
    @Override
    public void onBackPressed() {

        if (seccionActual != NavegacionInferior.SECCION_INICIO) {
            irASeccion(NavegacionInferior.SECCION_INICIO);
            return;
        }

        super.onBackPressed();
    }


    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        /*
         * Recargamos el Dashboard cuando volvemos
         * de otra pantalla.
         */
        if (viewModel != null) {

            viewModel.cargar();
        }
    }


    // =========================================================
    // GUARDAR EL LADO DEL POMODORO AL ROTAR
    // =========================================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putBoolean(
                ESTADO_POMODORO_IZQUIERDA,
                pomodoroEnBordeIzquierdo
        );

        outState.putFloat(
                ESTADO_POMODORO_X,
                pestanaPomodoro.getX()
        );

        outState.putInt(
                ESTADO_SECCION,
                seccionActual
        );
    }


    @Override
    protected void onRestoreInstanceState(Bundle state) {

        super.onRestoreInstanceState(state);

        /*
         * Antes del guard del pomodoro: si se restaura con instancia nueva,
         * la sección guardada es la única fuente de verdad y de las dos solo
         * debe quedar una aplicada.
         */
        if (state.containsKey(ESTADO_SECCION)) {
            irASeccion(
                    state.getInt(ESTADO_SECCION)
            );
        }

        if (!state.containsKey(ESTADO_POMODORO_IZQUIERDA)) {
            return;
        }

        boolean izquierda =
                state.getBoolean(ESTADO_POMODORO_IZQUIERDA);

        aplicarLadoPomodoro(izquierda);

        aplicarFormaBordePestana(izquierda);

        pestanaPomodoro.setX(
                state.getFloat(ESTADO_POMODORO_X, xAncladoPestana())
        );
    }


    // =========================================================
    // ENLAZAR VISTAS
    // =========================================================

    private void enlazarVistas() {


        ivAvatar =
                findViewById(
                        R.id.ivAvatar
                );


        tvSaludo =
                findViewById(
                        R.id.tvSaludo
                );


        tvPuntos =
                findViewById(
                        R.id.tvPuntos
                );


        tvNivel =
                findViewById(
                        R.id.tvNivel
                );


        tvNivelProgreso =
                findViewById(
                        R.id.tvNivelProgreso
                );


        tvRacha =
                findViewById(
                        R.id.tvRacha
                );


        tvAvisoRacha =
                findViewById(
                        R.id.tvAvisoRacha
                );


        tvMascotaNombre =
                findViewById(
                        R.id.tvMascotaNombre
                );


        tvMascotaEstado =
                findViewById(
                        R.id.tvMascotaEstado
                );


        tvHambreValor =
                findViewById(
                        R.id.tvHambreValor
                );


        tvFelicidadValor =
                findViewById(
                        R.id.tvFelicidadValor
                );


        tvEnergiaValor =
                findViewById(
                        R.id.tvEnergiaValor
                );


        tvProgresoLectura =
                findViewById(
                        R.id.tvProgresoLectura
                );


        tvPomodoroResumen =
                findViewById(
                        R.id.tvPomodoroResumen
                );


        tvComidaCantidad =
                findViewById(
                        R.id.tvComidaCantidad
                );


        seccionInicio =
                findViewById(
                        R.id.dashboardScroll
                );

        seccionExplorar =
                findViewById(
                        R.id.explorarScroll
                );

        seccionProximamente =
                findViewById(
                        R.id.seccionProximamente
                );

        barraInferior =
                findViewById(
                        R.id.bottomNav
                );

        if (seccionProximamente != null) {
            tvProxTitulo = seccionProximamente.findViewById(R.id.tvProxTitulo);
            tvProxEtiqueta = seccionProximamente.findViewById(R.id.tvProxEtiqueta);
            tvProxEmoji = seccionProximamente.findViewById(R.id.tvProxEmoji);
            tvProxMensaje = seccionProximamente.findViewById(R.id.tvProxMensaje);
            tvProxBadge = seccionProximamente.findViewById(R.id.tvProxBadge);
        }


        barHambre =
                findViewById(
                        R.id.barHambre
                );


        barFelicidad =
                findViewById(
                        R.id.barFelicidad
                );


        barEnergia =
                findViewById(
                        R.id.barEnergia
                );


        barProgreso =
                findViewById(
                        R.id.barProgreso
                );


        btnAlimentar =
                findViewById(
                        R.id.btnAlimentar
                );


        btnJugar =
                findViewById(
                        R.id.btnJugar
                );


        ivMascota =
                findViewById(
                        R.id.ivMascota
                );
        // -----------------------------------------------------
// POMODORO FLOTANTE
// -----------------------------------------------------

        panelPomodoro =
                findViewById(
                        R.id.panelPomodoro
                );

        pestanaPomodoro =
                findViewById(
                        R.id.pestanaPomodoro
                );

        tvPomodoroModo =
                findViewById(
                        R.id.tvPomodoroModo
                );

        tvPomodoroTiempo =
                findViewById(
                        R.id.tvPomodoroTiempo
                );

        tvPomodoroSeleccion =
                findViewById(
                        R.id.tvPomodoroSeleccion
                );

        tvPomodoroCiclos =
                findViewById(
                        R.id.tvPomodoroCiclos
                );

        tvPomodoroMiniTiempo =
                findViewById(
                        R.id.tvPomodoroMiniTiempo
                );

        seekPomodoro =
                findViewById(
                        R.id.seekPomodoro
                );

        btnPomodoroIniciar =
                findViewById(
                        R.id.btnPomodoroIniciar
                );

        btnPomodoroPausar =
                findViewById(
                        R.id.btnPomodoroPausar
                );

        btnPomodoroReiniciar =
                findViewById(
                        R.id.btnPomodoroReiniciar
                );
    }
    // =========================================================
// CONFIGURAR POMODORO FLOTANTE
// =========================================================

    private void configurarPomodoro() {

        // 10, 15, 20 ... 60
        seekPomodoro.setMax(10);

        int progresoInicial =
                (pomodoroManager.getMinutosEnfoque() - 10) / 5;

        seekPomodoro.setProgress(
                Math.max(
                        0,
                        Math.min(
                                10,
                                progresoInicial
                        )
                )
        );
// -----------------------------------------------------
// INICIAR MINIMIZADO
// -----------------------------------------------------

        panelPomodoro.setVisibility(
                View.GONE
        );

        pestanaPomodoro.setVisibility(
                View.VISIBLE
        );

        pomodoroMinimizado = true;


// -----------------------------------------------------
// PERMITIR MOVER LA PESTAÑA
// -----------------------------------------------------

        configurarArrastrePomodoro();

        // -----------------------------------------------------
        // CAMBIAR DURACIÓN
        // -----------------------------------------------------

        seekPomodoro.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        if (!fromUser) {
                            return;
                        }

                        int minutos =
                                10 + (progress * 5);

                        pomodoroManager.setMinutosEnfoque(
                                minutos
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );


        // -----------------------------------------------------
        // INICIAR
        // -----------------------------------------------------

        btnPomodoroIniciar.setOnClickListener(
                v -> {

                    if (!pomodoroManager.estaActivo()
                            && !pomodoroManager.estaPausado()) {

                        pomodoroManager.iniciarEnfoque();

                        // Al iniciar, se esconde automáticamente
                        // hacia el filo.
                        minimizarPomodoro();
                    }
                }
        );


        // -----------------------------------------------------
        // PAUSAR / REANUDAR
        // -----------------------------------------------------

        btnPomodoroPausar.setOnClickListener(
                v -> {

                    if (pomodoroManager.estaActivo()) {

                        pomodoroManager.pausar();

                    } else if (pomodoroManager.estaPausado()) {

                        pomodoroManager.reanudar();
                    }
                }
        );


        // -----------------------------------------------------
        // REINICIAR
        // -----------------------------------------------------

        btnPomodoroReiniciar.setOnClickListener(
                v -> {

                    pomodoroManager.reiniciar();

                    expandirPomodoro();
                }
        );


        // -----------------------------------------------------
        // MINIMIZAR
        // -----------------------------------------------------

        findViewById(
                R.id.btnMinimizarPomodoro
        ).setOnClickListener(
                v -> minimizarPomodoro()
        );



        // -----------------------------------------------------
        // RESUMEN DEL DASHBOARD TAMBIÉN ABRE EL POMODORO
        // -----------------------------------------------------

        tvPomodoroResumen.setOnClickListener(
                v -> expandirPomodoro()
        );


        // -----------------------------------------------------
        // ESCUCHAR EL MANAGER
        // -----------------------------------------------------

        pomodoroManager.agregarListener(
                pomodoroListener
        );

        actualizarPomodoroUI();
    }
// =========================================================
// LISTENER DEL POMODORO
// =========================================================

    private final PomodoroManager.Listener pomodoroListener =
            new PomodoroManager.Listener() {

                @Override
                public void onPomodoroActualizado() {

                    runOnUiThread(
                            () -> actualizarPomodoroUI()
                    );
                }


                @Override
                public void onEnfoqueCompletado() {

                    runOnUiThread(
                            () -> {

                                expandirPomodoro();

                                Toast.makeText(
                                        MainActivity.this,
                                        "¡Enfoque completado! Comienza tu descanso.",
                                        Toast.LENGTH_LONG
                                ).show();

                                if (viewModel != null) {
                                    viewModel.cargar();
                                }
                            }
                    );
                }


                @Override
                public void onDescansoCompletado() {

                    runOnUiThread(
                            () -> {

                                expandirPomodoro();

                                Toast.makeText(
                                        MainActivity.this,
                                        "Descanso terminado. Puedes iniciar otro ciclo.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                    );
                }
            };
    // =========================================================
// ACTUALIZAR INTERFAZ DEL POMODORO
// =========================================================

    private void actualizarPomodoroUI() {

        if (pomodoroManager == null) {
            return;
        }

        long restante =
                pomodoroManager.getTiempoRestanteMs();

        String tiempo =
                formatearTiempoPomodoro(
                        restante
                );

        tvPomodoroTiempo.setText(
                tiempo
        );

        tvPomodoroMiniTiempo.setText(
                tiempo
        );


        tvPomodoroSeleccion.setText(
                pomodoroManager.getMinutosEnfoque()
                        + " min de enfoque"
        );


        int ciclos =
                pomodoroManager.getCiclosCompletados();

        tvPomodoroCiclos.setText(
                ciclos
                        + (ciclos == 1
                        ? " ciclo completado"
                        : " ciclos completados")
        );


        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        switch (pomodoroManager.getEstado()) {

            case ENFOQUE:

                tvPomodoroModo.setText(
                        "Tiempo de enfoque"
                );

                btnPomodoroPausar.setText(
                        "Pausar"
                );

                break;


            case DESCANSO:

                tvPomodoroModo.setText(
                        "Tiempo de descanso"
                );

                btnPomodoroPausar.setText(
                        "Pausar"
                );

                break;


            case PAUSADO_ENFOQUE:

                tvPomodoroModo.setText(
                        "Enfoque pausado"
                );

                btnPomodoroPausar.setText(
                        "Reanudar"
                );

                break;


            case PAUSADO_DESCANSO:

                tvPomodoroModo.setText(
                        "Descanso pausado"
                );

                btnPomodoroPausar.setText(
                        "Reanudar"
                );

                break;


            case LISTO:
            default:

                tvPomodoroModo.setText(
                        "Listo para estudiar"
                );

                btnPomodoroPausar.setText(
                        "Pausar"
                );

                break;
        }


        // -----------------------------------------------------
        // CONTROLES
        // -----------------------------------------------------

        boolean ocupado =
                pomodoroManager.estaActivo()
                        || pomodoroManager.estaPausado();

        seekPomodoro.setEnabled(
                !ocupado
        );

        btnPomodoroIniciar.setEnabled(
                !ocupado
        );

        btnPomodoroPausar.setEnabled(
                ocupado
        );


        if (pomodoroManager.estaEnDescanso()) {

            btnPomodoroIniciar.setText(
                    "Descansando"
            );

        } else if (pomodoroManager.estaEnEnfoque()) {

            btnPomodoroIniciar.setText(
                    "En curso"
            );

        } else {

            btnPomodoroIniciar.setText(
                    ciclos > 0
                            ? "Iniciar otro ciclo"
                            : "Iniciar"
            );
        }
    }
    // =========================================================
// MINIMIZAR POMODORO
// =========================================================

    private void minimizarPomodoro() {

        if (pomodoroMinimizado) {
            return;
        }

        pomodoroMinimizado = true;


        // El panel se va por el lado en el que está anclado
        float salidaPanel =
                desplazamientoPanelPomodoro();


        panelPomodoro
                .animate()
                .translationX(salidaPanel)
                .alpha(0f)
                .setDuration(260)
                .withEndAction(
                        () -> {

                            panelPomodoro.setVisibility(
                                    View.GONE
                            );

                            panelPomodoro.setAlpha(
                                    1f
                            );

                            panelPomodoro.setTranslationX(
                                    0f
                            );

                            pestanaPomodoro.setAlpha(
                                    0f
                            );


                            // La pestaña se queda donde está anclada: solo se desliza
                            // desde ese mismo borde.
                            pestanaPomodoro.setX(
                                    xEntradaPestanaPomodoro()
                            );

                            pestanaPomodoro.setVisibility(
                                    View.VISIBLE
                            );

                            pestanaPomodoro
                                    .animate()
                                    .x(xAncladoPestana())
                                    .alpha(1f)
                                    .setDuration(220)
                                    .start();
                        }
                )
                .start();
    }


// =========================================================
// EXPANDIR POMODORO
// =========================================================

    private void expandirPomodoro() {

        if (!pomodoroMinimizado) {

            panelPomodoro.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        pomodoroMinimizado = false;


        float entradaPanel =
                desplazamientoPanelPomodoro();


        // El panel se coloca en el lado de la pestaña ANTES de hacerlo
        // visible: si no, cambiar el gravity se vería como un salto.
        aplicarLadoPomodoro(pomodoroEnBordeIzquierdo);


        float xSalidaPestana =
                xEntradaPestanaPomodoro();


        pestanaPomodoro
                .animate()
                .x(xSalidaPestana)
                .alpha(0f)
                .setDuration(180)
                .withEndAction(
                        () -> {

                            pestanaPomodoro.setVisibility(
                                    View.GONE
                            );

                            pestanaPomodoro.setAlpha(
                                    1f
                            );

                            pestanaPomodoro.setX(
                                    xAncladoPestana()
                            );


                            panelPomodoro.setAlpha(
                                    0f
                            );


                            // Entra desde el lado en el que está anclado
                            panelPomodoro.setTranslationX(
                                    entradaPanel
                            );

                            panelPomodoro.setVisibility(
                                    View.VISIBLE
                            );


                            panelPomodoro
                                    .animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(260)
                                    .start();
                        }
                )
                .start();
    }
    private String formatearTiempoPomodoro(
            long milisegundos
    ) {

        long segundosTotales =
                (milisegundos + 999L) / 1000L;

        long minutos =
                segundosTotales / 60L;

        long segundos =
                segundosTotales % 60L;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
        );
    }
    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {


        // -----------------------------------------------------
        // ALIMENTAR MASCOTA
        // -----------------------------------------------------

        btnAlimentar.setOnClickListener(
                v -> viewModel.alimentar()
        );


        // -----------------------------------------------------
        // JUGAR / EVALUACIÓN
        // -----------------------------------------------------

        btnJugar.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                CaminoActivity.class
                        )
                )
        );


        // -----------------------------------------------------
        // TOCAR AVATAR
        // -----------------------------------------------------

        ivAvatar.setOnClickListener(
                v -> mostrarSelectorAvatar()
        );


        // -----------------------------------------------------
        // TOCAR NOMBRE DE MASCOTA
        // -----------------------------------------------------

        tvMascotaNombre.setOnClickListener(
                v -> mostrarEditorNombreMascota()
        );


        // -----------------------------------------------------
        // TOCAR MASCOTA
        // -----------------------------------------------------

        ivMascota.setOnClickListener(
                v -> mostrarSelectorSkins()
        );


        // -----------------------------------------------------
        // BOTONES OCULTOS DE COMPATIBILIDAD
        // -----------------------------------------------------

        findViewById(
                R.id.btnEditarPerfil
        ).setOnClickListener(
                v -> mostrarEditorPerfil()
        );


        findViewById(
                R.id.btnPersonalizarMascota
        ).setOnClickListener(
                v -> mostrarSelectorSkins()
        );


        // -----------------------------------------------------
        // MENÚ DE TRES PUNTOS
        // -----------------------------------------------------

        findViewById(
                R.id.btnSalir
        ).setOnClickListener(
                this::mostrarMenuPrincipal
        );


        // -----------------------------------------------------
        // GUÍA DE ESTUDIO
        // -----------------------------------------------------

        findViewById(
                R.id.modGuia
        ).setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                GuiaActivity.class
                        )
                )
        );


        // -----------------------------------------------------
        // GRUPOS
        // -----------------------------------------------------

        findViewById(
                R.id.modGrupos
        ).setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                com.utm.semiologia.ui.estudio.MultijugadorActivity.class
                        )
                )
        );


        // -----------------------------------------------------
        // EVALUACIÓN
        // -----------------------------------------------------

        findViewById(
                R.id.modEvaluacion
        ).setOnClickListener(
                v -> startActivity(
                        new Intent(
                                this,
                                CaminoActivity.class
                        )
                )
        );
    }


    // =========================================================
    // MENÚ PRINCIPAL
    // =========================================================

    private void mostrarMenuPrincipal(View anchor) {


        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this
                );


        View vista =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.bottom_sheet_menu_perfil,
                                null
                        );


        dialog.setContentView(
                vista
        );


        // -----------------------------------------------------
        // EDITAR AVATAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.opcionEditarPerfil
        ).setOnClickListener(
                v -> {

                    dialog.dismiss();

                    mostrarEditorPerfil();
                }
        );


        // -----------------------------------------------------
        // PERSONALIZAR MASCOTA
        // -----------------------------------------------------

        vista.findViewById(
                R.id.opcionPersonalizarMascota
        ).setOnClickListener(
                v -> {

                    dialog.dismiss();

                    mostrarSelectorSkins();
                }
        );


        // -----------------------------------------------------
        // CAMBIAR NOMBRE
        // -----------------------------------------------------

        vista.findViewById(
                R.id.opcionNombreMascota
        ).setOnClickListener(
                v -> {

                    dialog.dismiss();

                    mostrarEditorNombreMascota();
                }
        );


        // -----------------------------------------------------
        // CERRAR SESIÓN
        // -----------------------------------------------------

        vista.findViewById(
                R.id.opcionCerrarSesion
        ).setOnClickListener(
                v -> {

                    dialog.dismiss();

                    confirmarSalir();
                }
        );


        prepararBottomSheetRedondeado(
                dialog
        );


        dialog.show();
    }


    // =========================================================
    // EDITAR PERFIL: AVATAR + NOMBRE VISIBLE
    // =========================================================

    /**
     * Panel "Editar perfil".
     *
     * El avatar conserva el comportamiento actual: al tocar uno se guarda al
     * instante y el panel se cierra. El nombre visible se guarda con su propio
     * botón; además, si el usuario escribe un nombre válido y luego cierra el
     * panel por otra vía (por ejemplo, tocando un avatar), se guarda igualmente
     * para que no se pierda lo escrito.
     */
    private void mostrarEditorPerfil() {


        final String[] avatares = {

                "avatar_01",
                "avatar_02",
                "avatar_03",
                "avatar_04",
                "avatar_05",
                "avatar_06"
        };


        final int[] resIds = {

                R.drawable.avatar_01,
                R.drawable.avatar_02,
                R.drawable.avatar_03,
                R.drawable.avatar_04,
                R.drawable.avatar_05,
                R.drawable.avatar_06
        };


        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this
                );


        View vista =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.bottom_sheet_editar_perfil,
                                null
                        );


        dialog.setContentView(
                vista
        );


        GridLayout grid =
                vista.findViewById(
                        R.id.gridAvatares
                );


        EditText inputNombre =
                vista.findViewById(
                        R.id.etNombreVisible
                );


        TextView contadorNombre =
                vista.findViewById(
                        R.id.tvContadorNombreVisible
                );


        Usuario usuario =
                null;

        if (
                viewModel
                        .getEstado()
                        .getValue() != null
        ) {

            usuario =
                    viewModel
                            .getEstado()
                            .getValue()
                            .usuario;
        }


        // -----------------------------------------------------
        // NOMBRE VISIBLE ACTUAL
        // -----------------------------------------------------

        final String nombreGuardado =
                usuario != null
                        ? usuario.getNombreParaSaludo()
                        : "";


        if (usuario != null) {

            inputNombre.setText(
                    nombreGuardado
            );

            inputNombre.setSelection(
                    nombreGuardado.length()
            );
        }


        contadorNombre.setText(
                inputNombre.length() + "/20"
        );


        // -----------------------------------------------------
        // CONTADOR DE CARACTERES
        // -----------------------------------------------------

        inputNombre.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {

                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        contadorNombre.setText(
                                s.length() + "/20"
                        );
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {

                    }
                }
        );


        // -----------------------------------------------------
        // GUARDAR EL NOMBRE
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnGuardarPerfil
        ).setOnClickListener(
                v -> {

                    String nuevoNombre =
                            inputNombre
                                    .getText()
                                    .toString()
                                    .trim();


                    if (nuevoNombre.isEmpty()) {

                        inputNombre.setError(
                                "Escribe un nombre"
                        );

                        return;
                    }


                    if (nuevoNombre.length() > 20) {

                        inputNombre.setError(
                                "Máximo 20 caracteres"
                        );

                        return;
                    }


                    viewModel.cambiarNombreVisible(
                            nuevoNombre
                    );


                    Toast.makeText(
                            this,
                            "Nombre actualizado",
                            Toast.LENGTH_SHORT
                    ).show();


                    dialog.dismiss();
                }
        );


        // -----------------------------------------------------
        // CANCELAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnCerrarPerfil
        ).setOnClickListener(
                v -> dialog.dismiss()
        );


        // -----------------------------------------------------
        // AVATARES: se guardan al instante, como antes
        // -----------------------------------------------------

        for (
                int i = 0;
                        i < avatares.length;
                        i++
        ) {


            final int index =
                    i;


            View item =
                    LayoutInflater
                            .from(this)
                            .inflate(
                                    R.layout.item_avatar_selector,
                                    grid,
                                    false
                            );


            ImageView imagen =
                    item.findViewById(
                            R.id.ivAvatarOpcion
                    );


            imagen.setImageResource(
                    resIds[index]
            );


            item.setOnClickListener(
                    v -> {

                        viewModel.cambiarAvatar(
                                avatares[index]
                        );


                        /*
                         * Si el usuario ya había escrito un nombre válido,
                         * se guarda antes de cerrar el panel para que no
                         * se pierda.
                         */
                        String escrito =
                                inputNombre
                                        .getText()
                                        .toString()
                                        .trim();

                        if (!escrito.isEmpty()
                                && !escrito.equals(nombreGuardado)) {

                            viewModel.cambiarNombreVisible(
                                    escrito
                            );
                        }


                        dialog.dismiss();
                    }
            );


            grid.addView(
                    item
            );
        }


        prepararBottomSheetRedondeado(
                dialog
        );


        dialog.show();
    }


    // =========================================================
// SELECTOR DE AVATAR
// =========================================================

    private void mostrarSelectorAvatar() {


        final String[] avatares = {

                "avatar_01",
                "avatar_02",
                "avatar_03",
                "avatar_04",
                "avatar_05",
                "avatar_06"
        };


        final int[] resIds = {

                R.drawable.avatar_01,
                R.drawable.avatar_02,
                R.drawable.avatar_03,
                R.drawable.avatar_04,
                R.drawable.avatar_05,
                R.drawable.avatar_06
        };


        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this
                );


        View vista =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.bottom_sheet_avatar,
                                null
                        );


        dialog.setContentView(
                vista
        );


        GridLayout grid =
                vista.findViewById(
                        R.id.gridAvatares
                );


        // -----------------------------------------------------
        // CREAR LAS 6 OPCIONES
        // -----------------------------------------------------

        for (
                int i = 0;
                i < avatares.length;
                i++
        ) {


            final int index =
                    i;


            View item =
                    LayoutInflater
                            .from(this)
                            .inflate(
                                    R.layout.item_avatar_selector,
                                    grid,
                                    false
                            );


            ImageView imagen =
                    item.findViewById(
                            R.id.ivAvatarOpcion
                    );


            imagen.setImageResource(
                    resIds[index]
            );


            item.setOnClickListener(
                    v -> {


                        viewModel.cambiarAvatar(
                                avatares[index]
                        );


                        dialog.dismiss();
                    }
            );


            grid.addView(
                    item
            );
        }


        // -----------------------------------------------------
        // CERRAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnCerrarAvatar
        ).setOnClickListener(
                v -> dialog.dismiss()
        );


        prepararBottomSheetRedondeado(
                dialog
        );


        dialog.show();
    }


    // =========================================================
    // SELECTOR DE SKIN MODERNO
    // =========================================================

    private void mostrarSelectorSkins() {


        final String[] nombres = {

                "Gato",
                "Perro",
                "Conejo",
                "Búho",
                "Ajolote",
                "Zorro"
        };


        final int[] resIds = {

                R.drawable.pet_cat_nuevo,
                R.drawable.pet_cat_blue,
                R.drawable.pet_cat_purple,
                R.drawable.pet_cat_student,
                R.drawable.pet_cat_space,
                R.drawable.pet_cat_golden
        };


        final int[] ids = {

                0,
                1,
                2,
                3,
                4,
                5
        };


        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this
                );


        View vista =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.bottom_sheet_mascota,
                                null
                        );


        dialog.setContentView(
                vista
        );


        GridLayout grid =
                vista.findViewById(
                        R.id.gridMascotas
                );


        // -----------------------------------------------------
        // CREAR LAS 6 SKINS
        // -----------------------------------------------------

        for (
                int i = 0;
                i < nombres.length;
                i++
        ) {


            final int index =
                    i;


            View item =
                    LayoutInflater
                            .from(this)
                            .inflate(
                                    R.layout.item_skin_selector,
                                    grid,
                                    false
                            );


            ImageView imagen =
                    item.findViewById(
                            R.id.ivSkinOpcion
                    );


            TextView nombre =
                    item.findViewById(
                            R.id.tvNombreSkin
                    );


            imagen.setImageResource(
                    resIds[index]
            );


            nombre.setText(
                    nombres[index]
            );


            item.setOnClickListener(
                    v -> {


                        viewModel.cambiarSkinMascota(
                                ids[index]
                        );


                        dialog.dismiss();
                    }
            );


            grid.addView(
                    item
            );
        }


        // -----------------------------------------------------
        // CERRAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnCerrarMascota
        ).setOnClickListener(
                v -> dialog.dismiss()
        );


        prepararBottomSheetRedondeado(
                dialog
        );


        dialog.show();
    }


    // =========================================================
    // EDITOR DE NOMBRE MODERNO
    // =========================================================

    private void mostrarEditorNombreMascota() {


        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this
                );


        View vista =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.bottom_sheet_nombre_mascota,
                                null
                        );


        dialog.setContentView(
                vista
        );


        EditText input =
                vista.findViewById(
                        R.id.etNombreMascota
                );


        TextView contador =
                vista.findViewById(
                        R.id.tvContadorNombre
                );


        // -----------------------------------------------------
        // OBTENER MASCOTA ACTUAL
        // -----------------------------------------------------

        Mascota mascota =
                null;


        if (
                viewModel
                        .getEstado()
                        .getValue() != null
        ) {


            mascota =
                    viewModel
                            .getEstado()
                            .getValue()
                            .mascota;
        }


        // -----------------------------------------------------
        // MOSTRAR NOMBRE ACTUAL
        // -----------------------------------------------------

        if (mascota != null) {


            input.setText(
                    mascota.getNombre()
            );


            input.setSelection(
                    input
                            .getText()
                            .length()
            );


            contador.setText(
                    input.length()
                            + "/20"
            );


        } else {


            contador.setText(
                    "0/20"
            );
        }


        // -----------------------------------------------------
        // CONTADOR DE CARACTERES
        // -----------------------------------------------------

        input.addTextChangedListener(
                new TextWatcher() {


                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {

                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {


                        contador.setText(
                                s.length()
                                        + "/20"
                        );
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {

                    }
                }
        );


        // -----------------------------------------------------
        // CANCELAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnCancelarNombre
        ).setOnClickListener(
                v -> dialog.dismiss()
        );


        // -----------------------------------------------------
        // GUARDAR
        // -----------------------------------------------------

        vista.findViewById(
                R.id.btnGuardarNombre
        ).setOnClickListener(
                v -> {


                    String nuevoNombre =
                            input
                                    .getText()
                                    .toString()
                                    .trim();


                    if (nuevoNombre.isEmpty()) {


                        input.setError(
                                "Escribe un nombre"
                        );


                        return;
                    }


                    if (nuevoNombre.length() > 20) {


                        input.setError(
                                "Máximo 20 caracteres"
                        );


                        return;
                    }


                    viewModel.cambiarNombreMascota(
                            nuevoNombre
                    );


                    dialog.dismiss();
                }
        );


        prepararBottomSheetRedondeado(
                dialog
        );


        dialog.show();
    }


    // =========================================================
    // PREPARAR BOTTOM SHEET REDONDEADO
    // =========================================================

    private void prepararBottomSheetRedondeado(
            BottomSheetDialog dialog
    ) {


        dialog.setOnShowListener(
                d -> {


                    View bottomSheet =
                            dialog.findViewById(
                                    com.google.android.material.R.id.design_bottom_sheet
                            );


                    if (bottomSheet != null) {


                        bottomSheet.setBackground(
                                new ColorDrawable(
                                        Color.TRANSPARENT
                                )
                        );
                    }
                }
        );
    }


    // =========================================================
    // MÓDULO EN CONSTRUCCIÓN
    // =========================================================

    private void moduloEnConstruccion(
            int id,
            String nombreModulo
    ) {


        findViewById(
                id
        ).setOnClickListener(
                v -> Toast.makeText(
                        this,
                        getString(
                                R.string.en_desarrollo,
                                nombreModulo
                        ),
                        Toast.LENGTH_SHORT
                ).show()
        );
    }


    // =========================================================
    // PINTAR ESTADO COMPLETO
    // =========================================================

    private void pintar(
            EstadoDashboard e
    ) {


        if (
                e == null
                        ||
                        e.usuario == null
        ) {


            return;
        }


        pintarUsuario(
                e.usuario
        );


        pintarMascota(
                e.mascota
        );


        pintarProgreso(
                e
        );
    }


    // =========================================================
    // PINTAR USUARIO
    // =========================================================

    private void pintarUsuario(
            Usuario u
    ) {


        String avatarId =
                u.getAvatar();


        // -----------------------------------------------------
        // AVATAR
        // -----------------------------------------------------

        if (
                avatarId != null
                        &&
                        !avatarId
                                .trim()
                                .isEmpty()
        ) {


            ivAvatar.setImageResource(
                    viewModel.getDrawableForAvatar(
                            avatarId
                    )
            );


        } else {


            ivAvatar.setImageResource(
                    R.drawable.avatar_01
            );
        }


        // -----------------------------------------------------
        // SALUDO
        // -----------------------------------------------------

        tvSaludo.setText(
                getString(
                        R.string.saludo,
                        u.getNombreParaSaludo()
                )
        );


        // -----------------------------------------------------
        // PUNTOS
        // -----------------------------------------------------

        tvPuntos.setText(
                getString(
                        R.string.etiqueta_puntos,
                        u.getPuntos()
                )
        );


        // -----------------------------------------------------
        // NIVEL
        // -----------------------------------------------------

        tvNivel.setText(
                getString(
                        R.string.etiqueta_nivel,
                        u.getNivel()
                )
        );


        int faltan =
                u.puntosParaSiguienteNivel();


        tvNivelProgreso.setText(

                faltan > 0

                        ? faltan
                        + " pts al nivel "
                        + (u.getNivel() + 1)

                        : "¡Nivel máximo alcanzado!"
        );


        // -----------------------------------------------------
        // RACHA
        // -----------------------------------------------------

        tvRacha.setText(
                getString(
                        R.string.etiqueta_racha,
                        u.getRachaActual()
                )
        );
    }


    // =========================================================
    // PINTAR MASCOTA
    // =========================================================

    private void pintarMascota(
            Mascota m
    ) {


        if (m == null) {


            tvMascotaNombre.setText(
                    "—"
            );


            tvMascotaEstado.setText(
                    ""
            );


            return;
        }


        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------
        String tipoMascota;

        switch (m.getSkinId()) {
            case 1:
                tipoMascota = "Perro";
                break;

            case 2:
                tipoMascota = "Conejo";
                break;

            case 3:
                tipoMascota = "Búho";
                break;

            case 4:
                tipoMascota = "Ajolote";
                break;

            case 5:
                tipoMascota = "Zorro";
                break;

            case 0:
            default:
                tipoMascota = "Gato";
                break;
        }

        tvMascotaNombre.setText(
                m.getNombre() + " " + tipoMascota
        );
        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        tvMascotaEstado.setText(
                estadoLegible(
                        m.getEstado()
                )
        );


        // -----------------------------------------------------
        // SKIN
        // -----------------------------------------------------

        ivMascota.setImageResource(
                viewModel.getDrawableForSkin(
                        m.getSkinId()
                )
        );


        // -----------------------------------------------------
        // COMIDA
        // -----------------------------------------------------

        int comida =
                viewModel.getCantidadComida(
                        m.getUsuarioId()
                );


        tvComidaCantidad.setText(
                "x " + comida
        );


        // -----------------------------------------------------
        // HAMBRE
        // -----------------------------------------------------

        barHambre.setIndicatorColor(
                m.colorHambre()
        );


        setBarra(
                barHambre,
                tvHambreValor,
                m.getHambre()
        );


        // -----------------------------------------------------
        // FELICIDAD
        // -----------------------------------------------------

        setBarra(
                barFelicidad,
                tvFelicidadValor,
                m.getFelicidad()
        );


        // -----------------------------------------------------
        // ENERGÍA
        // -----------------------------------------------------

        setBarra(
                barEnergia,
                tvEnergiaValor,
                m.getEnergia()
        );


        // -----------------------------------------------------
        // ESTADO CRÍTICO
        // -----------------------------------------------------

        if (m.estaCritica()) {


            ivMascota.setAlpha(
                    0.55f
            );


            btnAlimentar.setAlpha(
                    1f
            );


        } else {


            ivMascota.setAlpha(
                    1f
            );


            btnAlimentar.setAlpha(
                    1f
            );
        }
    }


    // =========================================================
    // PROGRESO
    // =========================================================

    private void pintarProgreso(
            EstadoDashboard e
    ) {


        tvProgresoLectura.setText(
                e.seccionesCompletadas
                        + "/"
                        + e.seccionesTotales
        );


        setBarra(
                barProgreso,
                null,
                e.progresoLectura()
        );


        tvPomodoroResumen.setText(
                e.minutosSemana
                        + " min esta semana · "
                        + e.ciclosTotales
                        + " ciclos"
        );


        tvAvisoRacha.setVisibility(

                e.rachaEnRiesgo

                        ? View.VISIBLE

                        : View.GONE
        );


        if (e.mensajeEvento != null) {


            Toast.makeText(
                    this,
                    e.mensajeEvento,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // BARRAS
    // =========================================================

    private void setBarra(
            LinearProgressIndicator barra,
            TextView etiqueta,
            int valor
    ) {


        int v =
                Math.max(
                        0,
                        Math.min(
                                100,
                                valor
                        )
                );


        if (
                barra.getProgress() != v
        ) {


            barra.setProgressCompat(
                    v,
                    true
            );
        }


        if (etiqueta != null) {


            etiqueta.setText(
                    v + "/100"
            );
        }
    }


    // =========================================================
    // ESTADO DE MASCOTA
    // =========================================================

    private String estadoLegible(
            String estado
    ) {


        if (estado == null) {

            return "";
        }


        switch (estado) {


            case Mascota.ESTADO_FELIZ:

                return "Feliz y con energía";


            case Mascota.ESTADO_HAMBRIENTO:

                return "Tiene hambre";


            case Mascota.ESTADO_CRITICO:

                return "¡Muy hambrienta! Aliméntala";


            case Mascota.ESTADO_TRISTE:

                return "Un poco triste";


            case Mascota.ESTADO_CANSADO:

                return "Está cansada";


            default:

                return estado;
        }
    }


    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    private void confirmarSalir() {


        /*
         * Por ahora conservamos la confirmación existente.
         * La lógica de cierre de sesión no cambia.
         */

        new AlertDialog.Builder(
                this
        )

                .setMessage(
                        R.string.confirmar_salir
                )

                .setPositiveButton(
                        R.string.aceptar,

                        (d, w) -> {


                            PomodoroManager
                                    .get()
                                    .cancelar();


                            SemiologiaApp
                                    .getSesion()
                                    .cerrarSesion();


                            irAlLogin();
                        }
                )

                .setNegativeButton(
                        R.string.cancelar,
                        null
                )

                .show();
    }


    // =========================================================
    // IR AL LOGIN
    // =========================================================

    private void irAlLogin() {


        Intent i =
                new Intent(
                        this,
                        LoginActivity.class
                );


        i.addFlags(

                Intent.FLAG_ACTIVITY_NEW_TASK

                        |

                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(
                i
        );


        finish();
    }
    @Override
    protected void onDestroy() {

        if (pomodoroManager != null) {

            pomodoroManager.quitarListener(
                    pomodoroListener
            );
        }

        super.onDestroy();
    }
    // =========================================================
// ARRASTRE DEL POMODORO
// =========================================================

    private void configurarArrastrePomodoro() {

        pestanaPomodoro.setOnTouchListener(
                (view, event) -> {

                    switch (event.getActionMasked()) {

                        case MotionEvent.ACTION_DOWN:

                            pomodoroDownX =
                                    event.getRawX();

                            pomodoroDownY =
                                    event.getRawY();

                            pomodoroInicioX =
                                    view.getX();

                            pomodoroInicioY =
                                    view.getY();

                            arrastrandoPomodoro =
                                    false;

                            return true;


                        case MotionEvent.ACTION_MOVE:

                            float deltaX =
                                    event.getRawX()
                                            - pomodoroDownX;

                            float deltaY =
                                    event.getRawY()
                                            - pomodoroDownY;


                            if (
                                    Math.abs(deltaX) > 8
                                            ||
                                            Math.abs(deltaY) > 8
                            ) {

                                if (!arrastrandoPomodoro) {
                                    // Solo en el frame en que empieza el arrastre
                                    levantarPestanaPomodoro();
                                }

                                arrastrandoPomodoro =
                                        true;
                            }


                            View padre =
                                    (View)
                                            view.getParent();


                            float nuevaX =
                                    pomodoroInicioX
                                            + deltaX;


                            float nuevaY =
                                    pomodoroInicioY
                                            + deltaY;


                            float maxX =
                                    padre.getWidth()
                                            - view.getWidth();


                            float maxY =
                                    padre.getHeight()
                                            - view.getHeight();


                            nuevaX =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    nuevaX,
                                                    maxX
                                            )
                                    );


                            nuevaY =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    nuevaY,
                                                    maxY
                                            )
                                    );


                            view.setX(
                                    nuevaX
                            );


                            view.setY(
                                    nuevaY
                            );


                            return true;


                        case MotionEvent.ACTION_UP:

                            if (!arrastrandoPomodoro) {

                                expandirPomodoro();

                                return true;
                            }


                            pegarPomodoroAlBorde();

                            return true;


                        case MotionEvent.ACTION_CANCEL:

                            if (arrastrandoPomodoro) {
                                pegarPomodoroAlBorde();
                            }

                            return true;
                    }


                    return false;
                }
        );
    }


// =========================================================
// PEGAR POMODORO AL BORDE
// =========================================================

    /**
     * Estado "suelta": mientras se arrastra la pestaña no está pegada a ningún
     * borde, así que se le ponen las cuatro esquinas redondeadas y se escala un
     * poco, para que se lea que está "en la mano".
     */
    private void levantarPestanaPomodoro() {

        pestanaPomodoro.animate().cancel();

        pestanaPomodoro.setBackgroundResource(
                R.drawable.bg_pomodoro_pestana_libre
        );


        // Padding simétrico: el contenido se mantiene centrado al soltar
        pestanaPomodoro.setPadding(
                dpPomodoro(6),
                dpPomodoro(6),
                dpPomodoro(6),
                dpPomodoro(6)
        );


        pestanaPomodoro
                .animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(120)
                .start();
    }


/**
     * Devuelve a la pestaña su forma de borde: el canto recto queda pegado a la
     * pantalla. La vuelta a escala 1 se anima en {@link #pegarPomodoroAlBorde()},
     * en el mismo animador que el deslizamiento, para que no se pisen.
     */
    private void aplicarFormaBordePestana(boolean izquierda) {

        pestanaPomodoro.animate().cancel();

        if (izquierda) {

            pestanaPomodoro.setBackgroundResource(
                    R.drawable.bg_pomodoro_pestana_izquierda
            );

            pestanaPomodoro.setPadding(
                    dpPomodoro(4),
                    dpPomodoro(6),
                    dpPomodoro(10),
                    dpPomodoro(6)
            );

        } else {

            pestanaPomodoro.setBackgroundResource(
                    R.drawable.bg_pomodoro_pestana
            );

            pestanaPomodoro.setPadding(
                    dpPomodoro(10),
                    dpPomodoro(6),
                    dpPomodoro(4),
                    dpPomodoro(6)
            );
        }
    }


    /**
     * Fija el lado del pomodoro. El panel es un hijo del FrameLayout raíz, así
     * que cambiar su gravity lo acerca al borde correspondiente. Se llama con
     * el panel en GONE para que el recolocado no se vea como un salto.
     */
    private void aplicarLadoPomodoro(boolean izquierda) {

        pomodoroEnBordeIzquierdo = izquierda;


        ViewParent padre = panelPomodoro.getParent();

        if (padre instanceof FrameLayout) {

            FrameLayout.LayoutParams lp =
                    (FrameLayout.LayoutParams) panelPomodoro.getLayoutParams();

            lp.gravity =
                    (izquierda ? Gravity.START : Gravity.END)
                            | Gravity.CENTER_VERTICAL;

            lp.leftMargin = dpPomodoro(12);
            lp.rightMargin = dpPomodoro(12);

            panelPomodoro.setLayoutParams(lp);
        }


        // La flecha de minimizar mira hacia el lado donde se va la ventana
        View btnMinimizar =
                panelPomodoro.findViewById(R.id.btnMinimizarPomodoro);

        if (btnMinimizar instanceof TextView) {
            ((TextView) btnMinimizar).setText(izquierda ? "‹" : "›");
        }
    }


    /** Distancia con signo desde la que el panel entra o sale de la pantalla. */
    private float desplazamientoPanelPomodoro() {

        float ancho = panelPomodoro.getWidth();

        if (ancho <= 0) {
            ViewParent padre = panelPomodoro.getParent();
            float anchoPadre =
                    padre instanceof FrameLayout
                            ? ((FrameLayout) padre).getWidth()
                            : 0f;
            ancho = anchoPadre > 0
                    ? anchoPadre * 0.8f
                    : dpPomodoro(300);
        }

        return pomodoroEnBordeIzquierdo
                ? -ancho - dpPomodoro(20)
                : ancho + dpPomodoro(20);
    }


    /**
     * Distancia con signo desde la que la pestaña entra o sale al borde.
     *
     * OJO: la pestaña tiene layout_gravity="end", así que su left en el layout
     * NO es 0. Por eso todo lo que la mueve se hace con coordenadas X
     * absolutas (setX / animate().x()) y nunca con translationX a secas.
     */
    private float desplazamientoPestanaPomodoro() {

        float ancho = pestanaPomodoro.getWidth();

        if (ancho <= 0) {
            ancho = dpPomodoro(72);
        }

        return pomodoroEnBordeIzquierdo ? -ancho : ancho;
    }


    /** Coordenada X a la que la pestaña queda pegada en su borde. */
    private float xAncladoPestana() {

        View padre = (View) pestanaPomodoro.getParent();

        if (pomodoroEnBordeIzquierdo) {
            return 0f;
        }

        return padre.getWidth() - pestanaPomodoro.getWidth();
    }


    /** Coordenada X desde la que la pestaña asoma por su borde. */
    private float xEntradaPestanaPomodoro() {

        return xAncladoPestana()
                + desplazamientoPestanaPomodoro();
    }


    private void pegarPomodoroAlBorde() {

        View padre =
                (View) pestanaPomodoro.getParent();


        float centroPomodoro =
                pestanaPomodoro.getX()
                        +
                        (
                                pestanaPomodoro.getWidth()
                                        / 2f
                        );


        float centroPantalla =
                padre.getWidth()
                        / 2f;


        boolean izquierda =
                centroPomodoro < centroPantalla;


        aplicarLadoPomodoro(izquierda);

        aplicarFormaBordePestana(izquierda);


        // Se asienta (vuelve a escala 1 y recupera su forma de borde) mientras
        // se desliza hasta el borde: todo en el mismo animador.
        pestanaPomodoro
                .animate()
                .scaleX(1f)
                .scaleY(1f)
                .x(xAncladoPestana())
                .setDuration(180)
                .start();
    }

}
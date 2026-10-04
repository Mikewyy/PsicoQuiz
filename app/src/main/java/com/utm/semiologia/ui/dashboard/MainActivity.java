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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.ui.auth.LoginActivity;
import com.utm.semiologia.ui.common.BaseActivity;
import com.utm.semiologia.ui.estudio.GuiaActivity;
import com.utm.semiologia.ui.evaluacion.CaminoActivity;



/**
 * Dashboard principal del estudiante.
 */
public class MainActivity extends BaseActivity {


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


        viewModel
                .getEstado()
                .observe(
                        this,
                        this::pintar
                );


        viewModel.cargar();
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

        panelPomodoro
                .animate()
                .translationX(
                        panelPomodoro.getWidth() + 40f
                )
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

                            pestanaPomodoro.setTranslationX(
                                    pestanaPomodoro.getWidth()
                            );

                            pestanaPomodoro.setVisibility(
                                    View.VISIBLE
                            );

                            pestanaPomodoro
                                    .animate()
                                    .translationX(0f)
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

        pestanaPomodoro
                .animate()
                .translationX(
                        pestanaPomodoro.getWidth()
                )
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

                            pestanaPomodoro.setTranslationX(
                                    0f
                            );


                            panelPomodoro.setAlpha(
                                    0f
                            );

                            panelPomodoro.setTranslationX(
                                    panelPomodoro.getWidth()
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
                v -> mostrarSelectorAvatar()
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
        );findViewById(
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

                    mostrarSelectorAvatar();
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
    // SELECTOR DE AVATAR MODERNO
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
                        primerNombre(
                                u.getNombre()
                        )
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
    // PRIMER NOMBRE
    // =========================================================

    private String primerNombre(
            String nombreCompleto
    ) {


        if (
                nombreCompleto == null
                        ||
                        nombreCompleto
                                .trim()
                                .isEmpty()
        ) {


            return "";
        }


        return nombreCompleto
                .trim()
                .split("\\s+")[0];
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


        float destinoX;


        if (centroPomodoro < centroPantalla) {

            // =====================================================
            // LADO IZQUIERDO
            // =====================================================

            destinoX = 0f;


            pestanaPomodoro.setBackgroundResource(
                    R.drawable.bg_pomodoro_pestana_izquierda
            );


            // El contenido queda un poquito separado del borde
            pestanaPomodoro.setPadding(
                    dpPomodoro(4),
                    dpPomodoro(6),
                    dpPomodoro(10),
                    dpPomodoro(6)
            );

        } else {

            // =====================================================
            // LADO DERECHO
            // =====================================================

            destinoX =
                    padre.getWidth()
                            -
                            pestanaPomodoro.getWidth();


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


        pestanaPomodoro
                .animate()
                .x(destinoX)
                .setDuration(180)
                .start();
    }

}
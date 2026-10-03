package com.utm.semiologia.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.utm.semiologia.ui.common.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.ui.auth.LoginActivity;
import com.utm.semiologia.ui.evaluacion.JugarActivity;
import com.utm.semiologia.ui.pomodoro.PomodoroManager;

/**
 * Dashboard: pantalla de inicio del estudiante.
 *
 * RESPONSABILIDADES
 *  - Pedirle los datos al {@link DashboardViewModel} (nunca a la BD directamente).
 *  - Observar un único LiveData y pintar el estado completo.
 *  - Reaccionar a los clics y devolver el control al ViewModel.
 *
 * NOTA SOBRE EL HAMBRE: al volver del segundo plano se llama a
 * {@code viewModel.cargar()} en onResume(), que es donde el ViewModel calcula
 * cuántos puntos de hambre se perdieron por el tiempo transcurrido. La
 * Activity no lleva ningún temporizador.
 */
public class MainActivity extends BaseActivity {

    // ---- Vistas ----
    private TextView tvAvatar, tvSaludo, tvPuntos, tvNivel, tvNivelProgreso, tvRacha;
    private TextView tvAvisoRacha, tvMascotaNombre, tvMascotaEstado;
    private TextView tvHambreValor, tvFelicidadValor, tvEnergiaValor;
    private TextView tvProgresoLectura, tvPomodoroResumen;
    private LinearProgressIndicator barHambre, barFelicidad, barEnergia, barProgreso;
    private Button btnAlimentar, btnJugar;
    private ImageView ivMascota;

    private DashboardViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guarda de seguridad: si no hay sesión, volver al login.
        if (!SemiologiaApp.getSesion().haySesion()) {
            irAlLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        enlazarVistas();
        configurarListeners();

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.getEstado().observe(this, this::pintar);

        viewModel.cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recalcular el decaimiento del hambre al volver a primer plano:
        // el usuario pudo dejar la app abierta horas.
        if (viewModel != null) {
            viewModel.cargar();
        }
    }

    // ==================================================================
    // Configuración de la vista
    // ==================================================================
    private void enlazarVistas() {
        tvAvatar          = findViewById(R.id.tvAvatar);
        tvSaludo          = findViewById(R.id.tvSaludo);
        tvPuntos          = findViewById(R.id.tvPuntos);
        tvNivel           = findViewById(R.id.tvNivel);
        tvNivelProgreso   = findViewById(R.id.tvNivelProgreso);
        tvRacha           = findViewById(R.id.tvRacha);
        tvAvisoRacha      = findViewById(R.id.tvAvisoRacha);
        tvMascotaNombre   = findViewById(R.id.tvMascotaNombre);
        tvMascotaEstado   = findViewById(R.id.tvMascotaEstado);
        tvHambreValor     = findViewById(R.id.tvHambreValor);
        tvFelicidadValor  = findViewById(R.id.tvFelicidadValor);
        tvEnergiaValor    = findViewById(R.id.tvEnergiaValor);
        tvProgresoLectura = findViewById(R.id.tvProgresoLectura);
        tvPomodoroResumen = findViewById(R.id.tvPomodoroResumen);
        barHambre         = findViewById(R.id.barHambre);
        barFelicidad      = findViewById(R.id.barFelicidad);
        barEnergia        = findViewById(R.id.barEnergia);
        barProgreso       = findViewById(R.id.barProgreso);
        btnAlimentar      = findViewById(R.id.btnAlimentar);
        btnJugar          = findViewById(R.id.btnJugar);
        ivMascota         = findViewById(R.id.ivMascota);
    }

    private void configurarListeners() {
        btnAlimentar.setOnClickListener(v -> viewModel.alimentar());
        btnJugar.setOnClickListener(v -> startActivity(new Intent(this, JugarActivity.class)));

        findViewById(R.id.btnSalir).setOnClickListener(v -> confirmarSalir());

        // Los módulos restantes se implementan en la siguiente iteración;
        // de momento se confirma que el acceso rápido está cableado.
        moduloEnConstruccion(R.id.modGuia, getString(R.string.modulo_guia));
        moduloEnConstruccion(R.id.modGrupos, getString(R.string.modulo_grupos));
        moduloEnConstruccion(R.id.modEvaluacion, getString(R.string.modulo_evaluacion));
    }

    private void moduloEnConstruccion(int id, String nombreModulo) {
        findViewById(id).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.en_desarrollo, nombreModulo),
                        Toast.LENGTH_SHORT).show());
    }

    // ==================================================================
    // Renderizado del estado
    // ==================================================================
    private void pintar(EstadoDashboard e) {
        if (e == null || e.usuario == null) return;

        pintarUsuario(e.usuario);
        pintarMascota(e.mascota);
        pintarProgreso(e);
    }

    private void pintarUsuario(Usuario u) {
        tvAvatar.setText(u.getIniciales());
        tvSaludo.setText(getString(R.string.saludo, primerNombre(u.getNombre())));
        tvPuntos.setText(getString(R.string.etiqueta_puntos, u.getPuntos()));
        tvNivel.setText(getString(R.string.etiqueta_nivel, u.getNivel()));

        int faltan = u.puntosParaSiguienteNivel();
        tvNivelProgreso.setText(faltan > 0
                ? faltan + " pts al nivel " + (u.getNivel() + 1)
                : "¡Nivel máximo alcanzado!");

        tvRacha.setText(getString(R.string.etiqueta_racha, u.getRachaActual()));
    }

    private void pintarMascota(Mascota m) {
        if (m == null) {
            tvMascotaNombre.setText("—");
            tvMascotaEstado.setText("");
            return;
        }

        tvMascotaNombre.setText(m.getNombre() + " · " + m.getEspecieFormateada());
        tvMascotaEstado.setText(estadoLegible(m.getEstado()));

        // El color de la barra de hambre cambia con el valor:
        // rojo en crítico, ámbar en aviso, verde en normal.
        barHambre.setIndicatorColor(m.colorHambre());
        setBarra(barHambre, tvHambreValor, m.getHambre());
        setBarra(barFelicidad, tvFelicidadValor, m.getFelicidad());
        setBarra(barEnergia, tvEnergiaValor, m.getEnergia());

        if (m.estaCritica()) {
            ivMascota.setAlpha(0.55f);
            btnAlimentar.setAlpha(1f);
        } else {
            ivMascota.setAlpha(1f);
            btnAlimentar.setAlpha(1f);
        }
    }

    private void pintarProgreso(EstadoDashboard e) {
        tvProgresoLectura.setText(e.seccionesCompletadas + "/" + e.seccionesTotales);
        setBarra(barProgreso, null, e.progresoLectura());
        tvPomodoroResumen.setText(e.minutosSemana + " min esta semana · " + e.ciclosTotales + " ciclos");

        tvAvisoRacha.setVisibility(e.rachaEnRiesgo ? View.VISIBLE : View.GONE);

        if (e.mensajeEvento != null) {
            Toast.makeText(this, e.mensajeEvento, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pinta una barra de estado al 0..100 y su etiqueta numérica.
     * Se anima sólo cuando el valor cambia, para no reiniciar la animación
     * en cada refresco.
     */
    private void setBarra(LinearProgressIndicator barra, TextView etiqueta, int valor) {
        int v = Math.max(0, Math.min(100, valor));
        if (barra.getProgress() != v) {
            barra.setProgressCompat(v, true);
        }
        if (etiqueta != null) {
            etiqueta.setText(v + "/100");
        }
    }

    private String primerNombre(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) return "";
        return nombreCompleto.trim().split("\\s+")[0];
    }

    private String estadoLegible(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case Mascota.ESTADO_FELIZ:      return "Feliz y con energía";
            case Mascota.ESTADO_HAMBRIENTO: return "Tiene hambre";
            case Mascota.ESTADO_CRITICO:    return "¡Muy hambrienta! Aliméntala";
            case Mascota.ESTADO_TRISTE:     return "Un poco triste";
            case Mascota.ESTADO_CANSADO:    return "Está cansada";
            default:                        return estado;
        }
    }

    // ==================================================================
    // Navegación
    // ==================================================================
    private void confirmarSalir() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirmar_salir)
                .setPositiveButton(R.string.aceptar, (d, w) -> {
                    PomodoroManager.get().cancelar();
                    SemiologiaApp.getSesion().cerrarSesion();
                    irAlLogin();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void irAlLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

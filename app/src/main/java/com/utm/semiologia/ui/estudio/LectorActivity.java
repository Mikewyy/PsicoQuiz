package com.utm.semiologia.ui.estudio;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utm.semiologia.R;
import com.utm.semiologia.SemiologiaApp;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.ProgresoSeccion;
import com.utm.semiologia.ui.common.BaseActivity;

import java.util.Locale;

public class LectorActivity extends BaseActivity {

    private static final String EXTRA_SECCION_ID = "extra_seccion_id";

    // Duración de estudio existente: no altera las recompensas.
    private static final int MINUTOS_ESTUDIO = 3;

    private static final int PROGRESO_INICIAL = 10;
    private static final int PROGRESO_PARA_COMPLETAR = 95;
    private static final String ESTADO_ACTIVIDADES = "lector_actividades_abiertas";
    private boolean[] actividadesAbiertas = new boolean[5];

    private Repositorio repo;

    private long usuarioId;
    private long seccionId;

    private ProgresoSeccion seccion;

    private NestedScrollView scrollLector;

    private TextView tvTitulo;
    private TextView tvTema;
    private TextView tvDuracion;
    private TextView tvContenido;
    private TextView tvProgresoLectura;
    private TextView tvEstado;
    private TextView tvTemporizador;

    private LinearProgressIndicator barLectura;

    private MaterialButton btnCompletar;

    private CountDownTimer countDownTimer;

    private long tiempoRestanteMs;
    private long finTemporizadorElapsedMs;

    private boolean tiempoCompletado = false;

    private int ultimoProgresoGuardado = -1;


    // =========================================================
    // INTENT
    // =========================================================

    public static Intent nuevoIntent(
            Context context,
            long seccionId
    ) {

        Intent intent =
                new Intent(
                        context,
                        LectorActivity.class
                );

        intent.putExtra(
                EXTRA_SECCION_ID,
                seccionId
        );

        return intent;
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            boolean[] abiertas = savedInstanceState.getBooleanArray(ESTADO_ACTIVIDADES);
            if (abiertas != null && abiertas.length == actividadesAbiertas.length) {
                actividadesAbiertas = abiertas;
            }
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(
                R.layout.activity_lector
        );

        repo =
                SemiologiaApp.getRepositorio();

        usuarioId =
                SemiologiaApp
                        .getSesion()
                        .getUsuarioId();

        seccionId =
                getIntent()
                        .getLongExtra(
                                EXTRA_SECCION_ID,
                                -1
                        );

        if (seccionId <= 0) {

            Toast.makeText(
                    this,
                    "No se pudo abrir este tema.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        enlazarVistas();

        configurarListeners();

        cargarSeccion();
    }


    // =========================================================
    // VISTAS
    // =========================================================

    private void enlazarVistas() {

        scrollLector =
                findViewById(
                        R.id.scrollLector
                );

        tvTitulo =
                findViewById(
                        R.id.tvTituloLector
                );

        tvTema =
                findViewById(
                        R.id.tvTemaLector
                );

        tvDuracion =
                findViewById(
                        R.id.tvDuracionLector
                );

        tvContenido =
                findViewById(
                        R.id.tvContenidoLector
                );

        tvProgresoLectura =
                findViewById(
                        R.id.tvProgresoLectura
                );

        tvEstado =
                findViewById(
                        R.id.tvEstadoLectura
                );

        tvTemporizador =
                findViewById(
                        R.id.tvTemporizador
                );

        barLectura =
                findViewById(
                        R.id.barLectura
                );

        btnCompletar =
                findViewById(
                        R.id.btnCompletarLectura
                );
        ViewCompat.setAccessibilityHeading(tvTitulo, true);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.tituloDefinicionLector), true);
        View contenedor = findViewById(R.id.contenedorLector);
        ViewCompat.setOnApplyWindowInsetsListener(contenedor, (v, windowInsets) -> {
            Insets barras = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            contenedor.setPadding(barras.left, barras.top, barras.right, 0);
            scrollLector.setPadding(0, 0, 0, barras.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(contenedor);
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {

        findViewById(
                R.id.btnVolver
        ).setOnClickListener(
                v -> finish()
        );

        btnCompletar.setOnClickListener(
                v -> completarLectura()
        );

        scrollLector.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener)
                        (
                                v,
                                scrollX,
                                scrollY,
                                oldScrollX,
                                oldScrollY
                        ) -> calcularProgresoScroll()
        );
    }


    // =========================================================
    // CARGAR SECCIÓN
    // =========================================================

    private void cargarSeccion() {

        seccion =
                repo.estudio()
                        .obtenerSeccionConProgreso(
                                usuarioId,
                                seccionId
                        );

        if (seccion == null) {

            Toast.makeText(
                    this,
                    "El tema no está disponible.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        mostrarSeccion();

        registrarLecturaInicial();

        prepararTemporizador();
        OneShotPreDrawListener.add(scrollLector, this::calcularProgresoScroll);
    }


    // =========================================================
    // MOSTRAR SECCIÓN
    // =========================================================

    private void mostrarSeccion() {

        tvTitulo.setText(
                valorSeguro(
                        seccion.getTitulo(),
                        "Tema de estudio"
                )
        );

        tvTema.setText(
                valorSeguro(
                        seccion.getTema(),
                        "Psicopatología"
                )
        );

        tvDuracion.setText(
                getString(R.string.lector_duracion, MINUTOS_ESTUDIO)
        );

        mostrarContenidoHTML();
        construirRepasoInteractivo();

        actualizarEstadoVisual();
    }


    // =========================================================
    // CONTENIDO
    // =========================================================

    private void mostrarContenidoHTML() {

        String contenido =
                seccion.getContenido();

        if (
                contenido == null ||
                        contenido.trim().isEmpty()
        ) {

            tvContenido.setText(
                    "El contenido de este tema todavía no está disponible."
            );

            return;
        }

        // Conserva el HTML y sus énfasis, con encabezados menos desproporcionados.
        SpannableStringBuilder texto = new SpannableStringBuilder(
                Html.fromHtml(contenido, Html.FROM_HTML_MODE_LEGACY));
        for (RelativeSizeSpan encabezado : texto.getSpans(0, texto.length(), RelativeSizeSpan.class)) {
            if (encabezado.getSizeChange() > 1f) {
                int inicio = texto.getSpanStart(encabezado);
                int fin = texto.getSpanEnd(encabezado);
                int flags = texto.getSpanFlags(encabezado);
                texto.removeSpan(encabezado);
                float proporcion = 1f + (encabezado.getSizeChange() - 1f) * 0.5f;
                texto.setSpan(new RelativeSizeSpan(proporcion), inicio, fin, flags);
            }
        }

        tvContenido.setText(
                texto
        );
    }


    // Centro de aprendizaje local: independiente de Firebase, rachas y recompensas.
    private int dp(int n) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                n, getResources().getDisplayMetrics());
    }

    private int colorGuia(int id) { return ContextCompat.getColor(this, id); }

    private TextView textoRepaso(String s, int size, boolean negrita) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        t.setTextColor(colorGuia(R.color.app_text_primary));
        if (negrita) t.setTypeface(null, Typeface.BOLD);
        t.setLineSpacing(dp(4), 1f);
        return t;
    }

    private GradientDrawable fondoGuia(int color, int borde, int radio) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colorGuia(color));
        d.setCornerRadius(dp(radio));
        d.setStroke(dp(1), colorGuia(borde));
        return d;
    }

    private LinearLayout actividadDesplegable(LinearLayout parent, int indice,
                                              int tituloId, int detalleId, int ilustracionId) {
        MaterialCardView card = (MaterialCardView) getLayoutInflater().inflate(
                R.layout.item_lector_actividad, parent, false);
        // Los IDs internos se reutilizan; la apertura se restaura explícitamente.
        card.setSaveFromParentEnabled(false);
        LinearLayout cabecera = card.findViewById(R.id.cabeceraActividadLector);
        LinearLayout contenido = card.findViewById(R.id.contenidoActividadLector);
        ImageView ilustracion = card.findViewById(R.id.ilustracionActividadLector);
        ImageView flecha = card.findViewById(R.id.flechaActividadLector);
        TextView paso = card.findViewById(R.id.pasoActividadLector);
        TextView titulo = card.findViewById(R.id.tituloActividadLector);
        TextView detalle = card.findViewById(R.id.detalleActividadLector);
        paso.setText(getString(R.string.lector_paso, indice + 1));
        titulo.setText(tituloId);
        detalle.setText(detalleId);
        ilustracion.setImageResource(ilustracionId);
        cabecera.setContentDescription(paso.getText() + ". " + getString(tituloId)
                + ". " + getString(detalleId));
        ViewCompat.setAccessibilityDelegate(cabecera, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                         @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
        Runnable mostrarEstado = () -> {
            boolean abierta = actividadesAbiertas[indice];
            contenido.setVisibility(abierta ? View.VISIBLE : View.GONE);
            card.setStrokeColor(colorGuia(abierta ? R.color.app_primary_ui : R.color.app_outline));
            ViewCompat.setStateDescription(cabecera,
                    getString(abierta ? R.string.lector_abierta : R.string.lector_cerrada));
        };
        mostrarEstado.run();
        flecha.setRotation(actividadesAbiertas[indice] ? 180f : 0f);
        cabecera.setOnClickListener(v -> {
            actividadesAbiertas[indice] = !actividadesAbiertas[indice];
            mostrarEstado.run();
            flecha.animate().rotation(actividadesAbiertas[indice] ? 180f : 0f)
                    .setDuration(180).start();
            if (actividadesAbiertas[indice]) {
                contenido.setAlpha(0f);
                contenido.animate().alpha(1f).setDuration(160).start();
            }
        });
        parent.addView(card);
        return contenido;
    }

    private MaterialButton botonGuia(String nombre) {
        MaterialButton b = new MaterialButton(this);
        b.setText(nombre);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setCornerRadius(dp(15));
        b.setMinHeight(dp(54));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setInsetTop(dp(4));
        b.setInsetBottom(dp(4));
        b.setTextColor(colorGuia(R.color.app_text_primary));
        b.setBackgroundTintList(ColorStateList.valueOf(colorGuia(R.color.app_surface_alt)));
        b.setStrokeColor(ColorStateList.valueOf(colorGuia(R.color.app_outline)));
        b.setStrokeWidth(dp(1));
        return b;
    }

    private void iconoEnBoton(MaterialButton boton, int drawableId, int colorId) {
        boton.setIconResource(drawableId);
        boton.setIconTint(ColorStateList.valueOf(colorGuia(colorId)));
        boton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        boton.setIconPadding(dp(10));
    }

    private void accionPrincipal(MaterialButton boton) {
        boton.setBackgroundTintList(ColorStateList.valueOf(colorGuia(R.color.primary)));
        boton.setTextColor(colorGuia(R.color.on_primary));
        boton.setStrokeWidth(0);
        boton.setGravity(Gravity.CENTER);
    }

    private void espacio(LinearLayout destino, int arriba) {
        View v = new View(this);
        destino.addView(v, new LinearLayout.LayoutParams(1, dp(arriba)));
    }

    private void bloqueDesplegable(LinearLayout parent, String titulo, String texto) {
        MaterialButton boton = botonGuia(titulo);
        iconoEnBoton(boton,R.drawable.ic_lector_chevron,R.color.app_primary_ui);
        boton.setIconGravity(MaterialButton.ICON_GRAVITY_END);
        parent.addView(boton, new LinearLayout.LayoutParams(-1,-2));
        TextView contenido = textoRepaso(texto,14,false);
        contenido.setBackground(fondoGuia(R.color.app_surface_alt, R.color.app_outline, 16));
        contenido.setPadding(dp(14),dp(16),dp(14),dp(17));
        contenido.setVisibility(View.GONE);
        contenido.setTextIsSelectable(true);
        parent.addView(contenido);
        ViewCompat.setStateDescription(boton, getString(R.string.lector_cerrada));
        boton.setOnClickListener(v -> {
            boolean mostrar = contenido.getVisibility()!=View.VISIBLE;
            contenido.setVisibility(mostrar?View.VISIBLE:View.GONE);
            iconoEnBoton(boton,mostrar?R.drawable.ic_guia_collapse:R.drawable.ic_lector_chevron,R.color.app_primary_ui);
            boton.setIconGravity(MaterialButton.ICON_GRAVITY_END);
            ViewCompat.setStateDescription(boton,
                    getString(mostrar ? R.string.lector_abierta : R.string.lector_cerrada));
        });
        espacio(parent, 9);
    }

    private void construirRepasoInteractivo() {
        LinearLayout destino = findViewById(R.id.contenedorRepasoInteractivo);
        destino.removeAllViews();
        try (InputStream stream = getAssets().open("guia_interactiva.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while((n=stream.read(buf))!=-1) out.write(buf,0,n);
            JSONObject indice = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            JSONObject tema = indice.optJSONObject(seccion.getTitulo());
            if(tema==null) return;
            final String temaKey = "guia_"+seccionId;
            final android.content.SharedPreferences prefs = getSharedPreferences(
                    "pato_guia_premium_"+usuarioId, MODE_PRIVATE);

            View cabecera = getLayoutInflater().inflate(R.layout.lector_centro_aprendizaje, destino, false);
            destino.addView(cabecera);
            ViewCompat.setAccessibilityHeading(cabecera.findViewById(R.id.tituloCentroLector), true);
            MaterialButton favorito = cabecera.findViewById(R.id.favoritoLector);
            Runnable actualizarFavorito = () -> {
                boolean guardado = prefs.getBoolean(temaKey+"_fav",false);
                favorito.setText(guardado ? R.string.lector_favorito_guardado : R.string.lector_favorito);
                ViewCompat.setStateDescription(favorito, getString(guardado
                        ? R.string.lector_favorito_guardado : R.string.lector_favorito));
            };
            actualizarFavorito.run();
            favorito.setOnClickListener(v -> {
                boolean nuevo=!prefs.getBoolean(temaKey+"_fav",false);
                prefs.edit().putBoolean(temaKey+"_fav",nuevo).apply();
                actualizarFavorito.run();
            });
            TextView avance = cabecera.findViewById(R.id.avancePracticasLector);
            LinearProgressIndicator progresoActividades = cabecera.findViewById(R.id.progresoPracticasLector);
            Runnable actualizar = () -> {
                int valor=(prefs.getBoolean(temaKey+"_flash",false)?1:0)
                        +(prefs.getBoolean(temaKey+"_quiz",false)?1:0)
                        +(prefs.getBoolean(temaKey+"_caso",false)?1:0);
                avance.setText(getString(R.string.lector_practicas, valor));
                progresoActividades.setProgress(valor);
                progresoActividades.setContentDescription(avance.getText());
            };
            actualizar.run();

            LinearLayout teoria = actividadDesplegable(destino, 0, R.string.lector_explora,
                    R.string.lector_explora_detalle, R.drawable.il_lector_explora);
            TextView introduccion = textoRepaso(tema.optString("ampliacion"),15,false);
            teoria.addView(introduccion);
            espacio(teoria,14);
            JSONArray bloques=tema.optJSONArray("bloques");
            if(bloques!=null) for(int i=0;i<bloques.length();i++) {
                JSONObject bloque=bloques.optJSONObject(i);
                if(bloque!=null) bloqueDesplegable(teoria,bloque.optString("titulo"),bloque.optString("texto"));
            }

            LinearLayout memoriza = actividadDesplegable(destino, 1, R.string.lector_memoria,
                    R.string.lector_memoria_detalle, R.drawable.il_lector_memoria);
            TextView pista=textoRepaso(tema.optString("flashFrente",tema.optString("titulo")),16,true);
            pista.setGravity(Gravity.CENTER);
            pista.setPadding(dp(12),dp(20),dp(12),dp(12));
            pista.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 20));
            TextView instruccionMemoria = textoRepaso(getString(R.string.lector_recordar),13,false);
            instruccionMemoria.setTextColor(colorGuia(R.color.app_text_secondary));
            memoriza.addView(instruccionMemoria);
            espacio(memoriza, 12);
            memoriza.addView(pista);
            MaterialButton voltear=botonGuia(getString(R.string.lector_revelar));
            accionPrincipal(voltear);
            memoriza.addView(voltear);
            TextView trasera=textoRepaso(tema.optString("flashReverso",tema.optString("ampliacion")),14,false);
            trasera.setPadding(dp(10),dp(14),dp(10),dp(14));
            trasera.setVisibility(View.GONE);
            memoriza.addView(trasera);
            voltear.setOnClickListener(v -> {
                boolean ver=trasera.getVisibility()!=View.VISIBLE;
                trasera.setVisibility(ver?View.VISIBLE:View.GONE);
                voltear.setText(ver ? R.string.lector_ocultar_respuesta : R.string.lector_revelar);
                if(ver) {prefs.edit().putBoolean(temaKey+"_flash",true).apply();actualizar.run();}
            });

            LinearLayout compara = actividadDesplegable(destino, 2, R.string.lector_compara,
                    R.string.lector_compara_detalle, R.drawable.il_lector_compara);
            TextView comparacion = textoRepaso(tema.optString("comparacion",tema.optString("ampliacion")),15,false);
            comparacion.setTextIsSelectable(true);
            compara.addView(comparacion);

            LinearLayout caso = actividadDesplegable(destino, 3, R.string.lector_caso,
                    R.string.lector_caso_detalle, R.drawable.il_lector_caso);
            TextView escenario = textoRepaso(tema.optString("caso"), 14, false);
            escenario.setPadding(dp(16),dp(17),dp(16),dp(17));
            escenario.setBackground(fondoGuia(R.color.app_surface_alt, R.color.app_outline, 16));
            caso.addView(escenario);
            espacio(caso,12);
            MaterialButton verCaso=botonGuia(getString(R.string.lector_interpretar));
            accionPrincipal(verCaso);
            caso.addView(verCaso);
            TextView solucionCaso=textoRepaso(tema.optString("casoRespuesta"),14,false);
            solucionCaso.setPadding(dp(10),dp(12),dp(10),dp(10));
            solucionCaso.setVisibility(View.GONE);
            caso.addView(solucionCaso);
            verCaso.setOnClickListener(v -> {
                boolean abrir=solucionCaso.getVisibility()!=View.VISIBLE;
                solucionCaso.setVisibility(abrir?View.VISIBLE:View.GONE);
                verCaso.setText(abrir ? R.string.lector_ocultar_interpretacion : R.string.lector_interpretar);
                if(abrir) {prefs.edit().putBoolean(temaKey+"_caso",true).apply();actualizar.run();}
            });

            LinearLayout cuestionario = actividadDesplegable(destino, 4, R.string.lector_quiz,
                    R.string.lector_quiz_detalle, R.drawable.il_lector_quiz);
            cuestionario.addView(textoRepaso(tema.optString("pregunta"),15,true));
            espacio(cuestionario,10);
            JSONArray opciones=tema.optJSONArray("opciones");
            if(opciones==null) return;
            java.util.ArrayList<MaterialButton> botones=new java.util.ArrayList<>();
            TextView comentario=textoRepaso(getString(R.string.lector_quiz_ayuda),13,false);
            comentario.setTextColor(colorGuia(R.color.app_text_secondary));
            int correcta=tema.optInt("correcta",-1);
            for(int i=0;i<opciones.length();i++) {
                final int j=i;
                MaterialButton boton=botonGuia(getString(R.string.lector_opcion, i+1, opciones.optString(i)));
                botones.add(boton);
                cuestionario.addView(boton);
                espacio(cuestionario,8);
                boton.setOnClickListener(v -> {
                    for(int k=0;k<botones.size();k++) {
                        MaterialButton actual=botones.get(k);
                        actual.setBackgroundTintList(ColorStateList.valueOf(colorGuia(
                                k==correcta?R.color.guia_exito_fondo:
                                k==j?R.color.guia_error_fondo:R.color.guia_acordeon_fondo)));
                        actual.setTextColor(colorGuia(k==correcta?R.color.guia_exito_texto:
                                k==j?R.color.guia_error_texto:R.color.app_text_primary));
                        actual.setStrokeWidth(dp(k==correcta || k==j ? 2:1));
                        actual.setStrokeColor(ColorStateList.valueOf(colorGuia(
                                k==correcta?R.color.guia_exito_texto:
                                k==j?R.color.guia_error_texto:R.color.app_outline)));
                        actual.setText(getString(R.string.lector_opcion, k+1, opciones.optString(k)));
                        if (k==correcta || k==j) {
                            iconoEnBoton(actual, k==correcta?R.drawable.ic_guia_check:R.drawable.ic_guia_wrong,
                                    k==correcta?R.color.guia_exito_texto:R.color.guia_error_texto);
                        } else {
                            actual.setIcon(null);
                        }
                    }
                    boolean bien=j==correcta;
                    comentario.setText((bien?"¡Correcto!  ":"Respuesta incorrecta.  ")+tema.optString("explicacion"));
                    comentario.setTextColor(colorGuia(bien?R.color.guia_exito_texto:R.color.guia_error_texto));
                    comentario.setBackground(fondoGuia(bien?R.color.guia_exito_fondo:R.color.guia_error_fondo,
                            bien?R.color.guia_exito_texto:R.color.guia_error_texto,15));
                    prefs.edit().putBoolean(temaKey+"_quiz",true).apply();
                    actualizar.run();
                });
            }
            comentario.setPadding(dp(16),dp(15),dp(16),dp(15));
            comentario.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 15));
            cuestionario.addView(comentario);
        } catch(Exception e) {
            android.util.Log.e("GuiaInteractiva", "No se pudo mostrar el centro de aprendizaje",e);
        }
    }

    // =========================================================
    // TEMPORIZADOR
    // =========================================================

    private void prepararTemporizador() {

        if (seccion.isCompletada()) {

            tiempoCompletado = true;

            tiempoRestanteMs = 0;

            tvTemporizador.setText(
                    "00:00"
            );

            actualizarEstadoVisual();

            return;
        }

        tiempoRestanteMs =
                MINUTOS_ESTUDIO
                        * 60L
                        * 1000L;

        mostrarTiempo(
                tiempoRestanteMs
        );

        iniciarTemporizador();
    }


    private void iniciarTemporizador() {
        cancelarTemporizador();
        if (tiempoCompletado || tiempoRestanteMs <= 0) {
            if (!tiempoCompletado) marcarTiempoCompletado();
            return;
        }
        // Reloj monotónico: no depende de que Android entregue un onTick cada segundo.
        finTemporizadorElapsedMs = SystemClock.elapsedRealtime() + tiempoRestanteMs;
        mostrarTiempo(tiempoRestanteMs);
        actualizarEstadoVisual();
        countDownTimer = new CountDownTimer(tiempoRestanteMs, 250) {
            @Override public void onTick(long ignorado) {
                tiempoRestanteMs = Math.max(0L,
                        finTemporizadorElapsedMs - SystemClock.elapsedRealtime());
                mostrarTiempo(tiempoRestanteMs);
                actualizarEstadoVisual();
            }
            @Override public void onFinish() {
                tiempoRestanteMs = 0;
                marcarTiempoCompletado();
            }
        }.start();
    }

    private void marcarTiempoCompletado() {

        tiempoCompletado = true;

        tiempoRestanteMs = 0;

        tvTemporizador.setText(
                "00:00"
        );

        actualizarEstadoVisual();

        Toast.makeText(
                this,
                "Tiempo de estudio completado.",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void mostrarTiempo(
            long tiempoMs
    ) {

        long segundosTotales =
                Math.max(
                        0,
                        tiempoMs / 1000
                );

        long minutos =
                segundosTotales / 60;

        long segundos =
                segundosTotales % 60;

        String texto =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutos,
                        segundos
                );

        tvTemporizador.setText(
                texto
        );
    }


    private void cancelarTemporizador() {

        if (countDownTimer != null) {

            countDownTimer.cancel();

            countDownTimer = null;
        }
    }


    // =========================================================
    // PROGRESO INICIAL
    // =========================================================

    private void registrarLecturaInicial() {

        if (seccion.isCompletada()) {

            ultimoProgresoGuardado = 100;

            return;
        }

        int lecturaActual =
                seccion.getLectura();

        if (lecturaActual < PROGRESO_INICIAL) {

            guardarProgreso(
                    PROGRESO_INICIAL
            );

        } else {

            ultimoProgresoGuardado =
                    lecturaActual;
        }

        actualizarEstadoVisual();
    }


    // =========================================================
    // PROGRESO POR SCROLL
    // =========================================================

    private void calcularProgresoScroll() {

        if (
                seccion == null ||
                        seccion.isCompletada()
        ) {
            return;
        }

        ViewGroup contenidoScroll = (ViewGroup) scrollLector.getChildAt(0);

        if (contenidoScroll == null) {
            return;
        }

        int alturaVisible = scrollLector.getHeight()
                - scrollLector.getPaddingTop() - scrollLector.getPaddingBottom();
        if (alturaVisible <= 0) {
            return;
        }

        // El repaso es desplegable: su altura no debe adelantar ni bloquear la lectura.
        View definicion = findViewById(R.id.cardDefinicionLector);
        Rect limitesLectura = new Rect();
        definicion.getDrawingRect(limitesLectura);
        contenidoScroll.offsetDescendantRectToMyCoords(definicion, limitesLectura);

        int desplazamiento =
                scrollLector.getScrollY();

        int desplazamientoMaximo = limitesLectura.bottom - alturaVisible;
        // Si la definición cabe completa, ya está visible sin necesidad de desplazar.
        float proporcion = desplazamientoMaximo <= 0 ? 1f
                : desplazamiento / (float) desplazamientoMaximo;

        proporcion =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                proporcion
                        )
                );

        int progreso =
                PROGRESO_INICIAL
                        +
                        Math.round(
                                proporcion
                                        *
                                        (100 - PROGRESO_INICIAL)
                        );

        progreso =
                Math.max(
                        PROGRESO_INICIAL,
                        Math.min(
                                100,
                                progreso
                        )
                );

        if (progreso < seccion.getLectura()) {

            progreso =
                    seccion.getLectura();
        }

        if (progreso > seccion.getLectura()) {

            seccion.setLectura(
                    progreso
            );

            actualizarEstadoVisual();
        }

        if (
                ultimoProgresoGuardado < 0 ||
                        progreso >= ultimoProgresoGuardado + 5 ||
                        progreso >= PROGRESO_PARA_COMPLETAR
        ) {

            guardarProgreso(
                    progreso
            );
        }
    }


    // =========================================================
    // GUARDAR PROGRESO
    // =========================================================

    private void guardarProgreso(
            int progreso
    ) {

        if (seccion == null) {
            return;
        }

        progreso =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progreso
                        )
                );

        if (progreso < seccion.getLectura()) {

            progreso =
                    seccion.getLectura();
        }

        repo.estudio()
                .actualizarLectura(
                        usuarioId,
                        seccionId,
                        progreso
                );

        seccion.setLectura(
                progreso
        );

        ultimoProgresoGuardado =
                progreso;

        actualizarEstadoVisual();
    }


    // =========================================================
    // COMPLETAR
    // =========================================================

    private void completarLectura() {

        if (seccion == null) {
            return;
        }

        if (seccion.isCompletada()) {

            Toast.makeText(
                    this,
                    "Este tema ya está completado.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!tiempoCompletado) {

            Toast.makeText(
                    this,
                    "Completa primero los " + MINUTOS_ESTUDIO + " minutos de estudio.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                seccion.getLectura()
                        < PROGRESO_PARA_COMPLETAR
        ) {

            Toast.makeText(
                    this,
                    "Continúa leyendo hasta llegar al final.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        seccion.completar(
                seccion.getPuntosRecompensa(),
                0
        );

        /*
         * EstudioDao administra la recompensa.
         * No entregamos comida directamente aquí
         * para evitar duplicados.
         */
        repo.estudio()
                .actualizarProgreso(
                        seccion,
                        repo.mascotas(),
                        usuarioId
                );

        ultimoProgresoGuardado = 100;

        actualizarEstadoVisual();

        Toast.makeText(
                this,
                "Tema completado. Recompensa conseguida.",
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // ESTADO VISUAL
    // =========================================================

    private void actualizarEstadoVisual() {

        if (seccion == null) {
            return;
        }

        int progreso =
                Math.max(
                        0,
                        Math.min(
                                100,
                                seccion.getLectura()
                        )
                );

        barLectura.setProgress(
                progreso
        );

        tvProgresoLectura.setText(
                progreso + "%"
        );


        // YA COMPLETADO

        if (seccion.isCompletada()) {

            tvEstado.setText(
                    "Tema completado"
            );

            btnCompletar.setText(
                    "Tema completado"
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.65f
            );

            return;
        }


        // FALTA TIEMPO

        if (!tiempoCompletado) {

            tvEstado.setText(
                    "Estudiando el tema"
            );

            btnCompletar.setText(
                    "Continúa estudiando · "
                            + formatearTiempoBoton()
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.60f
            );

            return;
        }


        // TIEMPO LISTO, PERO FALTA LECTURA

        if (
                progreso
                        < PROGRESO_PARA_COMPLETAR
        ) {

            tvEstado.setText(
                    "Tiempo completado · continúa leyendo"
            );

            btnCompletar.setText(
                    "Llega al final de la lectura"
            );

            btnCompletar.setEnabled(
                    false
            );

            btnCompletar.setAlpha(
                    0.60f
            );

            return;
        }


        // TODO LISTO

        tvEstado.setText(
                "Listo para completar"
        );

        btnCompletar.setText(
                "Completar tema"
        );

        btnCompletar.setEnabled(
                true
        );

        btnCompletar.setAlpha(
                1f
        );
    }


    private String formatearTiempoBoton() {

        long segundosTotales =
                Math.max(
                        0,
                        tiempoRestanteMs / 1000
                );

        long minutos =
                segundosTotales / 60;

        long segundos =
                segundosTotales % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
        );
    }


    // =========================================================
    // PAUSA
    // =========================================================

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBooleanArray(ESTADO_ACTIVIDADES, actividadesAbiertas);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {

        if (countDownTimer != null && !tiempoCompletado) {
            tiempoRestanteMs = Math.max(0L,
                    finTemporizadorElapsedMs - SystemClock.elapsedRealtime());
        }
        cancelarTemporizador();

        if (
                seccion != null &&
                        !seccion.isCompletada()
        ) {

            int progreso =
                    seccion.getLectura();

            if (
                    progreso
                            > ultimoProgresoGuardado
            ) {

                repo.estudio()
                        .actualizarLectura(
                                usuarioId,
                                seccionId,
                                progreso
                        );

                ultimoProgresoGuardado =
                        progreso;
            }
        }

        super.onPause();
    }


    // =========================================================
    // REANUDAR
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (seccion != null && !seccion.isCompletada() && !tiempoCompletado) {
            if (tiempoRestanteMs > 0 && countDownTimer == null) {
                iniciarTemporizador();
            } else if (tiempoRestanteMs <= 0) {
                marcarTiempoCompletado();
            }
        }
    }


    // =========================================================
    // DESTRUIR
    // =========================================================

    @Override
    protected void onDestroy() {

        cancelarTemporizador();

        super.onDestroy();
    }


    // =========================================================
    // UTILIDAD
    // =========================================================

    private String valorSeguro(
            String valor,
            String defecto
    ) {

        if (
                valor == null ||
                        valor.trim().isEmpty()
        ) {

            return defecto;
        }

        return valor;
    }
}

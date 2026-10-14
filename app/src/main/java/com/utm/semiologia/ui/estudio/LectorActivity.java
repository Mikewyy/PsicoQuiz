package com.utm.semiologia.ui.estudio;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.ProgressBar;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.view.Gravity;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
    private TextView tvEstadoTiempo;

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

        tvEstadoTiempo =
                findViewById(
                        R.id.tvEstadoTiempo
                );

        barLectura =
                findViewById(
                        R.id.barLectura
                );

        btnCompletar =
                findViewById(
                        R.id.btnCompletarLectura
                );
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
                MINUTOS_ESTUDIO + " min de estudio"
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

        Spanned texto =
                Html.fromHtml(
                        contenido,
                        Html.FROM_HTML_MODE_LEGACY
                );

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

    private TextView etiquetaGuia(String etiqueta) {
        TextView t = textoRepaso(etiqueta, 11, true);
        t.setTextColor(colorGuia(R.color.app_primary_ui));
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        t.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 14));
        return t;
    }

    private MaterialCardView tarjetaGuia(LinearLayout parent) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(23));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(colorGuia(R.color.app_surface_elevated));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(colorGuia(R.color.app_outline));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(18);
        parent.addView(card, lp);
        return card;
    }

    private LinearLayout contenidoTarjeta(MaterialCardView card) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20),dp(21),dp(20),dp(22));
        card.addView(layout);
        return layout;
    }

    private MaterialButton botonGuia(String nombre) {
        MaterialButton b = new MaterialButton(this);
        b.setText(nombre);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setCornerRadius(dp(15));
        b.setMinHeight(dp(54));
        b.setTextColor(colorGuia(R.color.app_text_primary));
        b.setBackgroundTintList(ColorStateList.valueOf(colorGuia(R.color.guia_acordeon_fondo)));
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

    private TextView encabezadoConIcono(String nombre, int drawableId) {
        TextView titulo = textoRepaso(nombre, 18, true);
        Drawable icono = ContextCompat.getDrawable(this, drawableId);
        if (icono != null) {
            icono = icono.mutate();
            icono.setTint(colorGuia(R.color.app_primary_ui));
            icono.setBounds(0, 0, dp(23), dp(23));
            titulo.setCompoundDrawables(icono, null, null, null);
            titulo.setCompoundDrawablePadding(dp(11));
        }
        titulo.setPadding(0, dp(3), 0, dp(3));
        return titulo;
    }

    private void espacio(LinearLayout destino, int arriba) {
        View v = new View(this);
        destino.addView(v, new LinearLayout.LayoutParams(1, dp(arriba)));
    }

    private void bloqueDesplegable(LinearLayout parent, String titulo, String texto) {
        MaterialButton boton = botonGuia("Explorar  ·  " + titulo);
        iconoEnBoton(boton,R.drawable.ic_guia_expand,R.color.app_primary_ui);
        parent.addView(boton, new LinearLayout.LayoutParams(-1,-2));
        TextView contenido = textoRepaso(texto,14,false);
        contenido.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 16));
        contenido.setPadding(dp(14),dp(16),dp(14),dp(17));
        contenido.setVisibility(View.GONE);
        contenido.setTextIsSelectable(true);
        parent.addView(contenido);
        boton.setOnClickListener(v -> {
            boolean mostrar = contenido.getVisibility()!=View.VISIBLE;
            contenido.setVisibility(mostrar?View.VISIBLE:View.GONE);
            boton.setText((mostrar ? "Ocultar  ·  " : "Explorar  ·  ") + titulo);
            iconoEnBoton(boton,mostrar?R.drawable.ic_guia_collapse:R.drawable.ic_guia_expand,R.color.app_primary_ui);
            boton.setContentDescription((mostrar?"Cerrar ":"Abrir ")+titulo);
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
            JSONObject indice = new JSONObject(out.toString("UTF-8"));
            JSONObject tema = indice.optJSONObject(seccion.getTitulo());
            if(tema==null) return;
            final String temaKey = "guia_"+seccionId;
            final android.content.SharedPreferences prefs = getSharedPreferences(
                    "pato_guia_premium_"+usuarioId, MODE_PRIVATE);

            LinearLayout cabecera = contenidoTarjeta(tarjetaGuia(destino));
            TextView marcador = textoRepaso("CENTRO DE APRENDIZAJE  ·  ESTUDIO GUIADO",11,true);
            marcador.setTextColor(colorGuia(R.color.app_primary_ui));
            cabecera.addView(marcador);
            espacio(cabecera, 12);
            cabecera.addView(etiquetaGuia("LECTURA  •  MEMORIZACIÓN  •  PRÁCTICA"));
            espacio(cabecera,8);
            cabecera.addView(textoRepaso(tema.optString("titulo",seccion.getTitulo()),21,true));
            espacio(cabecera,7);
            TextView introduccion = textoRepaso(tema.optString("ampliacion"),14,false);
            introduccion.setTextColor(colorGuia(R.color.app_text_secondary));
            cabecera.addView(introduccion);
            espacio(cabecera,14);
            MaterialButton favorito = botonGuia(prefs.getBoolean(temaKey+"_fav",false)
                    ? "★  Guardado en favoritos" : "☆  Guardar en favoritos");
            favorito.setContentDescription("Marcar o desmarcar tema favorito");
            cabecera.addView(favorito);
            iconoEnBoton(favorito,R.drawable.ic_guia_star,R.color.app_primary_ui);
            favorito.setOnClickListener(v -> {
                boolean nuevo=!prefs.getBoolean(temaKey+"_fav",false);
                prefs.edit().putBoolean(temaKey+"_fav",nuevo).apply();
                favorito.setText(nuevo?"★  Guardado en favoritos":"☆  Guardar en favoritos");
            });
            espacio(cabecera,8);
            TextView avance = textoRepaso("",12,true);
            avance.setTextColor(colorGuia(R.color.app_primary_ui));
            cabecera.addView(avance);
            espacio(cabecera, 8);
            ProgressBar progresoActividades = new ProgressBar(this, null,
                    android.R.attr.progressBarStyleHorizontal);
            progresoActividades.setMax(3);
            progresoActividades.setProgressTintList(ColorStateList.valueOf(colorGuia(R.color.app_primary_ui)));
            progresoActividades.setProgressBackgroundTintList(ColorStateList.valueOf(colorGuia(R.color.guia_acordeon_fondo)));
            cabecera.addView(progresoActividades, new LinearLayout.LayoutParams(-1, dp(9)));
            Runnable actualizar = () -> {
                int valor=(prefs.getBoolean(temaKey+"_flash",false)?1:0)
                        +(prefs.getBoolean(temaKey+"_quiz",false)?1:0)
                        +(prefs.getBoolean(temaKey+"_caso",false)?1:0);
                avance.setText("Tu progreso de práctica  ·  " + valor + " de 3  ·  " +(valor*100/3)+"%");
                progresoActividades.setProgress(valor);
            };
            actualizar.run();

            LinearLayout teoria = contenidoTarjeta(tarjetaGuia(destino));
            teoria.addView(encabezadoConIcono("01  ·  Explora y comprende",R.drawable.ic_guia_book));
            espacio(teoria,9);
            TextView ayuda = textoRepaso("Abre los apartados para profundizar. Los conceptos esenciales de la lectura original siguen disponibles arriba.",13,false);
            ayuda.setTextColor(colorGuia(R.color.app_text_secondary));
            teoria.addView(ayuda);
            espacio(teoria,13);
            JSONArray bloques=tema.optJSONArray("bloques");
            if(bloques!=null) for(int i=0;i<bloques.length();i++) {
                JSONObject bloque=bloques.optJSONObject(i);
                if(bloque!=null) bloqueDesplegable(teoria,bloque.optString("titulo"),bloque.optString("texto"));
            }

            LinearLayout memoriza=contenidoTarjeta(tarjetaGuia(destino));
            memoriza.addView(encabezadoConIcono("02  ·  Tarjeta de memoria",R.drawable.ic_guia_flash));
            espacio(memoriza,10);
            TextView pista=textoRepaso(tema.optString("flashFrente",tema.optString("titulo")),16,true);
            pista.setGravity(Gravity.CENTER);
            pista.setPadding(dp(12),dp(20),dp(12),dp(12));
            pista.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 20));
            pista.setCompoundDrawablePadding(dp(10));
            memoriza.addView(etiquetaGuia("PIENSA ANTES DE REVELAR"));
            espacio(memoriza, 12);
            memoriza.addView(pista);
            MaterialButton voltear=botonGuia("Revelar respuesta  ↻");
            memoriza.addView(voltear);
            iconoEnBoton(voltear,R.drawable.ic_guia_flash,R.color.app_primary_ui);
            TextView trasera=textoRepaso(tema.optString("flashReverso",tema.optString("ampliacion")),14,false);
            trasera.setPadding(dp(10),dp(14),dp(10),dp(14));
            trasera.setVisibility(View.GONE);
            memoriza.addView(trasera);
            voltear.setOnClickListener(v -> {
                boolean ver=trasera.getVisibility()!=View.VISIBLE;
                trasera.setVisibility(ver?View.VISIBLE:View.GONE);
                voltear.setText(ver?"Ocultar respuesta  ↻":"Revelar respuesta  ↻");
                if(ver) {prefs.edit().putBoolean(temaKey+"_flash",true).apply();actualizar.run();}
            });

            LinearLayout compara=contenidoTarjeta(tarjetaGuia(destino));
            compara.addView(encabezadoConIcono("03  ·  Compara y diferencia",R.drawable.ic_guia_compare));
            espacio(compara,12);
            bloqueDesplegable(compara,"Ver las diferencias importantes",tema.optString("comparacion",tema.optString("ampliacion")));

            LinearLayout caso=contenidoTarjeta(tarjetaGuia(destino));
            caso.addView(encabezadoConIcono("04  ·  Situación para analizar",R.drawable.ic_guia_case));
            espacio(caso,8);
            TextView escenario = textoRepaso(tema.optString("caso"), 14, false);
            escenario.setPadding(dp(16),dp(17),dp(16),dp(17));
            escenario.setBackground(fondoGuia(R.color.guia_acordeon_fondo, R.color.app_outline, 16));
            caso.addView(escenario);
            espacio(caso,12);
            MaterialButton verCaso=botonGuia("Mostrar interpretación  ↓");
            caso.addView(verCaso);
            iconoEnBoton(verCaso,R.drawable.ic_guia_case,R.color.app_primary_ui);
            TextView solucionCaso=textoRepaso(tema.optString("casoRespuesta"),14,false);
            solucionCaso.setPadding(dp(10),dp(12),dp(10),dp(10));
            solucionCaso.setVisibility(View.GONE);
            caso.addView(solucionCaso);
            verCaso.setOnClickListener(v -> {
                boolean abrir=solucionCaso.getVisibility()!=View.VISIBLE;
                solucionCaso.setVisibility(abrir?View.VISIBLE:View.GONE);
                verCaso.setText(abrir?"Ocultar interpretación  ↑":"Mostrar interpretación  ↓");
                if(abrir) {prefs.edit().putBoolean(temaKey+"_caso",true).apply();actualizar.run();}
            });

            LinearLayout cuestionario=contenidoTarjeta(tarjetaGuia(destino));
            cuestionario.addView(encabezadoConIcono("05  ·  Comprueba lo aprendido",R.drawable.ic_guia_quiz));
            espacio(cuestionario,10);
            cuestionario.addView(textoRepaso(tema.optString("pregunta"),15,true));
            espacio(cuestionario,10);
            JSONArray opciones=tema.optJSONArray("opciones");
            if(opciones==null) return;
            java.util.ArrayList<MaterialButton> botones=new java.util.ArrayList<>();
            TextView comentario=textoRepaso("Elige una opción para comprobar tu respuesta.",13,false);
            comentario.setTextColor(colorGuia(R.color.app_text_secondary));
            int correcta=tema.optInt("correcta",-1);
            for(int i=0;i<opciones.length();i++) {
                final int j=i;
                MaterialButton boton=botonGuia((i+1)+".  "+opciones.optString(i));
                botones.add(boton);
                cuestionario.addView(boton);
                iconoEnBoton(boton,R.drawable.ic_guia_quiz,R.color.app_primary_ui);
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
                        actual.setText((k+1)+".  "+opciones.optString(k));
                        iconoEnBoton(actual, k==correcta?R.drawable.ic_guia_check:
                                k==j?R.drawable.ic_guia_wrong:R.drawable.ic_guia_quiz,
                                k==correcta?R.color.guia_exito_texto:
                                k==j?R.color.guia_error_texto:R.color.app_primary_ui);
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

            tvEstadoTiempo.setText(
                    "Completado"
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
        tvEstadoTiempo.setText("Estudiando");
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

        tvEstadoTiempo.setText(
                "Completado"
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

        View contenidoScroll =
                scrollLector.getChildAt(0);

        if (contenidoScroll == null) {
            return;
        }

        int alturaContenido =
                contenidoScroll.getHeight();

        int alturaVisible =
                scrollLector.getHeight();

        int desplazamiento =
                scrollLector.getScrollY();

        int desplazamientoMaximo =
                alturaContenido - alturaVisible;

        if (desplazamientoMaximo <= 0) {
            return;
        }

        float proporcion =
                desplazamiento
                        / (float) desplazamientoMaximo;

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
                    "Completa primero los 5 minutos de estudio.",
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
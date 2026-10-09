package com.utm.semiologia.ui.estudio;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.utm.semiologia.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class QuizMultijugadorActivity
        extends AppCompatActivity {

    public static final String EXTRA_CODIGO_SALA =
            "extra_codigo_sala";

    private static final long TIEMPO_PREGUNTA_MS =
            30_000L;

    private static final long TIEMPO_RESULTADO_MS =
            10_000L;

    private static final int[] PUNTOS_POR_LUGAR = {
            1000,
            800,
            600,
            400,
            250
    };

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private ListenerRegistration listenerSala;
    private ListenerRegistration listenerParticipantes;
    private ListenerRegistration listenerRespuestas;
    private ListenerRegistration listenerLecturasResultado;
    private ListenerRegistration listenerRevancha;

    private String codigoSala;
    private String partidaId = "";
    private String anfitrionId = "";

    private boolean soyAnfitrion = false;
    private boolean respuestaEnviada = false;
    private boolean finalizandoPregunta = false;
    private boolean avanzandoTrasLecturas = false;

    private int cantidadParticipantes = 0;
    private int respuestasPreguntaActual = 0;
    private int preguntaActual = -1;
    private int indiceRespuestaUsuario = -1;

    private String clavePantalla = "";

    private List<Map<String, Object>> preguntasPartida =
            new ArrayList<>();

    private CountDownTimer timerPregunta;
    private CountDownTimer timerResultado;

    private TextView tvCodigoPartida;
    private TextView tvProgresoPregunta;
    private TextView tvAreaPregunta;
    private TextView tvTiempo;
    private TextView tvPregunta;
    private TextView tvEstadoRespuesta;
    private TextView tvResultadoTitulo;
    private TextView tvRetroalimentacion;
    private TextView tvRanking;
    private TextView tvRespuestasJugadores;
    private TextView tvEsperaRespuestas;
    private TextView tvLecturasResultado;
    private TextView tvRevanchaEstado;

    private TextView tvPodioNombre1;
    private TextView tvPodioPuntos1;
    private TextView tvPodioNombre2;
    private TextView tvPodioPuntos2;
    private TextView tvPodioNombre3;
    private TextView tvPodioPuntos3;

    private LinearProgressIndicator barRespuestasJugadores;

    private LinearLayout contenedorOpciones;
    private LinearLayout panelResultado;
    private LinearLayout panelPodioFinal;
    private View podioPuesto1;
    private View podioPuesto2;
    private View podioPuesto3;
    private View cardRankingTexto;
    private View cardRevancha;
    private View cardRevisionRespuestas;

    private LinearLayout contenedorRevisionRespuestas;

    private List<DocumentSnapshot> jugadoresRankingActual =
            new ArrayList<>();

    private MaterialButton btnOpcionA;
    private MaterialButton btnOpcionB;
    private MaterialButton btnOpcionC;
    private MaterialButton btnOpcionD;
    private MaterialButton btnContinuarRespuesta;
    private MaterialButton btnListoResultado;
    private MaterialButton btnAceptarRevancha;
    private MaterialButton btnRechazarRevancha;
    private MaterialButton btnVolverJugar;
    private MaterialButton btnSalirPartida;
    private MaterialButton btnRevisarRespuestas;

    private LinearLayout contenedorAccionesFinales;

    private MaterialButton[] botonesOpciones;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_quiz_multijugador
        );

        firestore =
                FirebaseFirestore.getInstance();

        auth =
                FirebaseAuth.getInstance();

        codigoSala =
                getIntent()
                        .getStringExtra(
                                EXTRA_CODIGO_SALA
                        );

        if (codigoSala == null) {
            codigoSala = "";
        }

        codigoSala =
                codigoSala
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (codigoSala.isEmpty()) {

            Toast.makeText(
                    this,
                    "No se recibió el código de la partida.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        enlazarVistas();

        tvCodigoPartida.setText(
                "Sala " + codigoSala
        );

        escucharParticipantes();
        escucharSala();
        escucharRespuestas();
        escucharLecturasResultado();
    }

    private void enlazarVistas() {

        tvCodigoPartida =
                findViewById(
                        R.id.tvCodigoPartida
                );

        tvProgresoPregunta =
                findViewById(
                        R.id.tvProgresoPregunta
                );

        tvAreaPregunta =
                findViewById(
                        R.id.tvAreaPregunta
                );

        tvTiempo =
                findViewById(
                        R.id.tvTiempo
                );

        tvPregunta =
                findViewById(
                        R.id.tvPreguntaMultijugador
                );

        tvEstadoRespuesta =
                findViewById(
                        R.id.tvEstadoRespuesta
                );

        tvResultadoTitulo =
                findViewById(
                        R.id.tvResultadoTitulo
                );

        tvRetroalimentacion =
                findViewById(
                        R.id.tvRetroalimentacion
                );

        tvRanking =
                findViewById(
                        R.id.tvRanking
                );

        tvRespuestasJugadores =
                findViewById(
                        R.id.tvRespuestasJugadores
                );

        tvEsperaRespuestas =
                findViewById(
                        R.id.tvEsperaRespuestas
                );

        tvLecturasResultado =
                findViewById(
                        R.id.tvLecturasResultado
                );

        barRespuestasJugadores =
                findViewById(
                        R.id.barRespuestasJugadores
                );

        panelPodioFinal =
                findViewById(
                        R.id.panelPodioFinal
                );

        podioPuesto1 =
                findViewById(
                        R.id.podioPuesto1
                );

        podioPuesto2 =
                findViewById(
                        R.id.podioPuesto2
                );

        podioPuesto3 =
                findViewById(
                        R.id.podioPuesto3
                );

        tvPodioNombre1 =
                findViewById(
                        R.id.tvPodioNombre1
                );

        tvPodioPuntos1 =
                findViewById(
                        R.id.tvPodioPuntos1
                );

        tvPodioNombre2 =
                findViewById(
                        R.id.tvPodioNombre2
                );

        tvPodioPuntos2 =
                findViewById(
                        R.id.tvPodioPuntos2
                );

        tvPodioNombre3 =
                findViewById(
                        R.id.tvPodioNombre3
                );

        tvPodioPuntos3 =
                findViewById(
                        R.id.tvPodioPuntos3
                );

        cardRankingTexto =
                findViewById(
                        R.id.cardRankingTexto
                );

        contenedorOpciones =
                findViewById(
                        R.id.contenedorOpciones
                );

        panelResultado =
                findViewById(
                        R.id.panelResultado
                );

        btnOpcionA =
                findViewById(
                        R.id.btnOpcionA
                );

        btnOpcionB =
                findViewById(
                        R.id.btnOpcionB
                );

        btnOpcionC =
                findViewById(
                        R.id.btnOpcionC
                );

        btnOpcionD =
                findViewById(
                        R.id.btnOpcionD
                );

        btnContinuarRespuesta =
                findViewById(
                        R.id.btnContinuarRespuesta
                );

        btnListoResultado =
                findViewById(
                        R.id.btnListoResultado
                );

        cardRevancha =
                findViewById(
                        R.id.cardRevancha
                );

        tvRevanchaEstado =
                findViewById(
                        R.id.tvRevanchaEstado
                );

        btnAceptarRevancha =
                findViewById(
                        R.id.btnAceptarRevancha
                );

        btnRechazarRevancha =
                findViewById(
                        R.id.btnRechazarRevancha
                );

        btnVolverJugar =
                findViewById(
                        R.id.btnVolverJugar
                );

        btnSalirPartida =
                findViewById(
                        R.id.btnSalirPartida
                );

        contenedorAccionesFinales =
                findViewById(
                        R.id.contenedorAccionesFinales
                );

        btnRevisarRespuestas =
                findViewById(
                        R.id.btnRevisarRespuestas
                );

        cardRevisionRespuestas =
                findViewById(
                        R.id.cardRevisionRespuestas
                );

        contenedorRevisionRespuestas =
                findViewById(
                        R.id.contenedorRevisionRespuestas
                );

        botonesOpciones =
                new MaterialButton[]{
                        btnOpcionA,
                        btnOpcionB,
                        btnOpcionC,
                        btnOpcionD
                };

        for (
                int i = 0;
                i < botonesOpciones.length;
                i++
        ) {

            final int indice = i;

            botonesOpciones[i]
                    .setOnClickListener(
                            v ->
                                    enviarRespuesta(
                                            indice
                                    )
                    );
        }

        /*
         * PASO 1: después de enviar la respuesta, el jugador decide
         * cuándo continuar a la pantalla de espera. Esto NO avanza
         * la pregunta global: Firebase sigue esperando a los demás.
         */
        btnContinuarRespuesta.setOnClickListener(
                v -> {
                    btnContinuarRespuesta.setVisibility(
                            View.GONE
                    );

                    contenedorOpciones.setVisibility(
                            View.GONE
                    );

                    tvEstadoRespuesta.setText(
                            "Respuesta registrada · esperando a los demás jugadores"
                    );
                }
        );

        btnListoResultado.setOnClickListener(
                v -> confirmarLecturaResultado()
        );

        btnAceptarRevancha.setOnClickListener(
                v -> confirmarRevancha(true)
        );

        btnRechazarRevancha.setOnClickListener(
                v -> confirmarRevancha(false)
        );

        escucharRevancha();

        btnVolverJugar.setOnClickListener(
                v -> volverAJugar()
        );

        btnRevisarRespuestas.setOnClickListener(
                v -> alternarRevisionRespuestas()
        );

        btnSalirPartida.setOnClickListener(
                v -> finish()
        );
    }

    private void escucharSala() {

        listenerSala =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        tvEstadoRespuesta
                                                .setText(
                                                        "Error de conexión con la sala."
                                                );

                                        return;
                                    }

                                    if (
                                            snapshot == null ||
                                                    !snapshot.exists()
                                    ) {
                                        return;
                                    }

                                    aplicarEstadoSala(
                                            snapshot
                                    );
                                }
                        );
    }

    @SuppressWarnings("unchecked")
    private void aplicarEstadoSala(
            DocumentSnapshot snapshot
    ) {

        anfitrionId =
                snapshot.getString(
                        "anfitrionId"
                );

        soyAnfitrion =
                auth.getCurrentUser() != null
                        &&
                        anfitrionId != null
                        &&
                        anfitrionId.equals(
                                auth
                                        .getCurrentUser()
                                        .getUid()
                        );

        String nuevoPartidaId =
                snapshot.getString(
                        "partidaId"
                );

        if (nuevoPartidaId != null) {
            partidaId =
                    nuevoPartidaId;
        }

        Object preguntasRaw =
                snapshot.get(
                        "preguntasPartida"
                );

        if (
                preguntasRaw instanceof
                        List
        ) {

            preguntasPartida =
                    (List<Map<String, Object>>)
                            preguntasRaw;
        }

        String estadoPartida =
                snapshot.getString(
                        "estadoPartida"
                );

        String fasePartida =
                snapshot.getString(
                        "fasePartida"
                );

        Long indice =
                snapshot.getLong(
                        "preguntaActual"
                );

        int nuevoIndice =
                indice != null
                        ? indice.intValue()
                        : 0;

        if (
                "finalizada".equalsIgnoreCase(
                        estadoPartida
                )
        ) {

            mostrarFinal();
            return;
        }

        if (
                !"jugando".equalsIgnoreCase(
                        estadoPartida
                )
        ) {
            return;
        }

        if (fasePartida == null) {
            fasePartida =
                    "pregunta";
        }

        String nuevaClave =
                fasePartida
                        + "_"
                        + nuevoIndice
                        + "_"
                        + partidaId;

        preguntaActual =
                nuevoIndice;

        if (
                nuevaClave.equals(
                        clavePantalla
                )
        ) {
            return;
        }

        clavePantalla =
                nuevaClave;

        if (
                "pregunta".equalsIgnoreCase(
                        fasePartida
                )
        ) {

            mostrarPregunta();

        } else if (
                "resultado".equalsIgnoreCase(
                        fasePartida
                )
        ) {

            String correcta =
                    snapshot.getString(
                            "respuestaCorrectaTexto"
                    );

            String retro =
                    snapshot.getString(
                            "retroalimentacionActual"
                    );

            mostrarResultado(
                    correcta,
                    retro
            );
        }
    }

    private void escucharParticipantes() {

        listenerParticipantes =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("participantes")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null ||
                                                    snapshot == null
                                    ) {
                                        return;
                                    }

                                    cantidadParticipantes =
                                            snapshot.size();

                                    actualizarProgresoRespuestas(
                                            respuestasPreguntaActual
                                    );

                                    List<DocumentSnapshot> jugadores =
                                            new ArrayList<>(
                                                    snapshot
                                                            .getDocuments()
                                            );

                                    jugadores.sort(
                                            (a, b) -> {

                                                int porPuntos =
                                                        Long.compare(
                                                                puntosDe(b),
                                                                puntosDe(a)
                                                        );

                                                if (porPuntos != 0) {
                                                    return porPuntos;
                                                }

                                                int porAciertos =
                                                        Long.compare(
                                                                aciertosDe(b),
                                                                aciertosDe(a)
                                                        );

                                                if (porAciertos != 0) {
                                                    return porAciertos;
                                                }

                                                /*
                                                 * Último desempate estable:
                                                 * si tienen exactamente los mismos puntos
                                                 * y aciertos, se ordenan por nombre para
                                                 * que el podio no cambie aleatoriamente
                                                 * entre actualizaciones de Firebase.
                                                 */
                                                return nombreDe(a)
                                                        .compareToIgnoreCase(
                                                                nombreDe(b)
                                                        );
                                            }
                                    );

                                    jugadoresRankingActual =
                                            new ArrayList<>(
                                                    jugadores
                                            );

                                    if (
                                            panelPodioFinal != null &&
                                                    panelPodioFinal.getVisibility() == View.VISIBLE
                                    ) {
                                        actualizarPodioFinal(
                                                jugadoresRankingActual
                                        );
                                        return;
                                    }

                                    StringBuilder ranking =
                                            new StringBuilder();

                                    for (
                                            int i = 0;
                                            i < jugadores.size();
                                            i++
                                    ) {

                                        DocumentSnapshot jugador =
                                                jugadores.get(i);

                                        String nombre =
                                                jugador.getString(
                                                        "nombre"
                                                );

                                        if (
                                                nombre == null ||
                                                        nombre.trim().isEmpty()
                                        ) {
                                            nombre =
                                                    "Jugador";
                                        }

                                        long puntos =
                                                puntosDe(
                                                        jugador
                                                );

                                        ranking
                                                .append(
                                                        i + 1
                                                )
                                                .append(
                                                        ". "
                                                )
                                                .append(
                                                        nombre
                                                )
                                                .append(
                                                        "  ·  "
                                                )
                                                .append(
                                                        puntos
                                                )
                                                .append(
                                                        " pts"
                                                );

                                        if (
                                                i <
                                                        jugadores.size() - 1
                                        ) {
                                            ranking.append(
                                                    '\n'
                                            );
                                        }
                                    }

                                    tvRanking.setText(
                                            ranking.toString()
                                    );
                                }
                        );
    }

    private long puntosDe(
            DocumentSnapshot documento
    ) {

        Long puntos =
                documento.getLong(
                        "puntosPartida"
                );

        return puntos != null
                ? puntos
                : 0L;
    }

    private long aciertosDe(
            DocumentSnapshot documento
    ) {

        Long aciertos =
                documento.getLong(
                        "aciertosPartida"
                );

        return aciertos != null
                ? aciertos
                : 0L;
    }

    private void escucharRespuestas() {

        listenerRespuestas =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("respuestas")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null ||
                                                    snapshot == null ||
                                                    preguntaActual < 0 ||
                                                    partidaId.isEmpty()
                                    ) {
                                        return;
                                    }

                                    int respuestasActuales =
                                            0;

                                    for (
                                            DocumentSnapshot documento :
                                            snapshot.getDocuments()
                                    ) {

                                        String partida =
                                                documento.getString(
                                                        "partidaId"
                                                );

                                        Long indice =
                                                documento.getLong(
                                                        "preguntaIndex"
                                                );

                                        if (
                                                partidaId.equals(
                                                        partida
                                                )
                                                        &&
                                                        indice != null
                                                        &&
                                                        indice.intValue()
                                                                ==
                                                                preguntaActual
                                        ) {
                                            respuestasActuales++;
                                        }
                                    }

                                    respuestasPreguntaActual =
                                            respuestasActuales;

                                    actualizarProgresoRespuestas(
                                            respuestasActuales
                                    );

                                    if (
                                            soyAnfitrion &&
                                                    !finalizandoPregunta &&
                                                    cantidadParticipantes >= 2 &&
                                                    respuestasActuales >=
                                                            cantidadParticipantes
                                    ) {
                                        finalizarPregunta();
                                    }
                                }
                        );
    }

    private void escucharLecturasResultado() {

        listenerLecturasResultado =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("lecturasResultado")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null ||
                                                    snapshot == null ||
                                                    preguntaActual < 0 ||
                                                    partidaId.isEmpty()
                                    ) {
                                        return;
                                    }

                                    int confirmados = 0;

                                    for (
                                            DocumentSnapshot documento :
                                            snapshot.getDocuments()
                                    ) {

                                        String partida =
                                                documento.getString(
                                                        "partidaId"
                                                );

                                        Long indice =
                                                documento.getLong(
                                                        "preguntaIndex"
                                                );

                                        Boolean listo =
                                                documento.getBoolean(
                                                        "listo"
                                                );

                                        if (
                                                partidaId.equals(partida) &&
                                                        indice != null &&
                                                        indice.intValue() == preguntaActual &&
                                                        Boolean.TRUE.equals(listo)
                                        ) {
                                            confirmados++;
                                        }
                                    }

                                    actualizarLecturasResultado(
                                            confirmados
                                    );
                                }
                        );
    }

    private void confirmarLecturaResultado() {

        if (
                auth.getCurrentUser() == null ||
                        partidaId.isEmpty() ||
                        preguntaActual < 0
        ) {
            return;
        }

        btnListoResultado.setEnabled(false);
        btnListoResultado.setText(
                "Confirmando..."
        );

        String uid =
                auth.getCurrentUser().getUid();

        String idConfirmacion =
                partidaId
                        + "_"
                        + preguntaActual
                        + "_"
                        + uid;

        Map<String, Object> datos =
                new HashMap<>();

        datos.put(
                "partidaId",
                partidaId
        );
        datos.put(
                "preguntaIndex",
                preguntaActual
        );
        datos.put(
                "uid",
                uid
        );
        datos.put(
                "listo",
                true
        );
        datos.put(
                "confirmadoEn",
                FieldValue.serverTimestamp()
        );

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("lecturasResultado")
                .document(idConfirmacion)
                .set(datos)
                .addOnSuccessListener(
                        unused -> {
                            btnListoResultado.setText(
                                    "Listo · esperando a los demás"
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {
                            btnListoResultado.setEnabled(true);
                            btnListoResultado.setText(
                                    "Listo · ya leí la justificación"
                            );

                            Toast.makeText(
                                    this,
                                    "No se pudo confirmar. Intenta otra vez.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }

    private void actualizarLecturasResultado(
            int confirmados
    ) {

        if (
                tvLecturasResultado == null ||
                        tvLecturasResultado.getVisibility() != View.VISIBLE
        ) {
            return;
        }

        int total =
                Math.max(
                        cantidadParticipantes,
                        0
                );

        int listos =
                total > 0
                        ? Math.min(confirmados, total)
                        : confirmados;

        if (total <= 0) {
            tvLecturasResultado.setText(
                    "Esperando a los jugadores..."
            );
            return;
        }

        if (listos >= total) {

            tvLecturasResultado.setText(
                    "Todos están listos · continuando..."
            );

            if (
                    soyAnfitrion &&
                            !avanzandoTrasLecturas
            ) {
                avanzandoTrasLecturas = true;
                avanzarPregunta();
            }

        } else {

            int faltan =
                    total - listos;

            tvLecturasResultado.setText(
                    listos
                            + " de "
                            + total
                            + " ya leyeron · "
                            + (
                            faltan == 1
                                    ? "falta 1 jugador"
                                    : "faltan "
                                    + faltan
                                    + " jugadores"
                    )
            );
        }
    }

    private void actualizarProgresoRespuestas(
            int respuestasActuales
    ) {

        int total =
                Math.max(
                        cantidadParticipantes,
                        0
                );

        int respondieron =
                Math.max(
                        respuestasActuales,
                        0
                );

        if (total > 0) {
            respondieron =
                    Math.min(
                            respondieron,
                            total
                    );
        }

        if (total <= 0) {
            tvRespuestasJugadores.setText(
                    "0 de 0"
            );
        } else {
            tvRespuestasJugadores.setText(
                    respondieron
                            + " de "
                            + total
            );
        }

        int progreso =
                total > 0
                        ? Math.round(
                        (respondieron * 100f)
                                / total
                )
                        : 0;

        // Animación suave para que el avance se vea en tiempo real.
        barRespuestasJugadores.setProgressCompat(
                progreso,
                true
        );

        if (total <= 0) {

            barRespuestasJugadores.setIndicatorColor(
                    Color.parseColor("#B8B5C9")
            );

            tvRespuestasJugadores.setTextColor(
                    Color.parseColor("#7C7893")
            );

            tvEsperaRespuestas.setTextColor(
                    Color.parseColor("#8B88A8")
            );

            tvEsperaRespuestas.setText(
                    "Conectando con los jugadores..."
            );

        } else if (respondieron >= total) {

            // Verde cuando todos terminaron.
            barRespuestasJugadores.setIndicatorColor(
                    Color.parseColor("#32B879")
            );

            tvRespuestasJugadores.setTextColor(
                    Color.parseColor("#249562")
            );

            tvEsperaRespuestas.setTextColor(
                    Color.parseColor("#249562")
            );

            tvEsperaRespuestas.setText(
                    "Todos respondieron. Preparando resultado..."
            );

        } else if (respondieron == 0) {

            barRespuestasJugadores.setIndicatorColor(
                    Color.parseColor("#6C5CE7")
            );

            tvRespuestasJugadores.setTextColor(
                    Color.parseColor("#5A46D6")
            );

            tvEsperaRespuestas.setTextColor(
                    Color.parseColor("#8B88A8")
            );

            tvEsperaRespuestas.setText(
                    "Esperando las primeras respuestas..."
            );

        } else {

            barRespuestasJugadores.setIndicatorColor(
                    Color.parseColor("#6C5CE7")
            );

            tvRespuestasJugadores.setTextColor(
                    Color.parseColor("#5A46D6")
            );

            tvEsperaRespuestas.setTextColor(
                    Color.parseColor("#6F6A86")
            );

            int faltan =
                    total - respondieron;

            tvEsperaRespuestas.setText(
                    faltan == 1
                            ? "Falta 1 jugador por responder"
                            : "Faltan "
                            + faltan
                            + " jugadores por responder"
            );
        }
    }

    private void mostrarPregunta() {

        cancelarTimerResultado();
        cancelarTimerPregunta();

        finalizandoPregunta =
                false;

        respuestaEnviada =
                false;

        indiceRespuestaUsuario =
                -1;

        respuestasPreguntaActual =
                0;

        actualizarProgresoRespuestas(
                0
        );

        restaurarColoresOpciones();

        contenedorAccionesFinales.setVisibility(
                View.GONE
        );

        if (cardRevisionRespuestas != null) {
            cardRevisionRespuestas.setVisibility(
                    View.GONE
            );
        }

        if (contenedorRevisionRespuestas != null) {
            contenedorRevisionRespuestas.removeAllViews();
        }

        if (btnRevisarRespuestas != null) {
            btnRevisarRespuestas.setText(
                    "Revisar mis respuestas"
            );
            btnRevisarRespuestas.setEnabled(
                    true
            );
        }

        panelResultado.setVisibility(
                View.GONE
        );

        panelPodioFinal.setVisibility(
                View.GONE
        );

        cardRankingTexto.setVisibility(
                View.VISIBLE
        );

        contenedorOpciones.setVisibility(
                View.VISIBLE
        );

        btnContinuarRespuesta.setVisibility(
                View.GONE
        );

        btnListoResultado.setVisibility(
                View.GONE
        );

        tvLecturasResultado.setVisibility(
                View.GONE
        );

        avanzandoTrasLecturas = false;

        tvEstadoRespuesta.setText(
                "Elige una opción. ¡La rapidez cuenta!"
        );

        habilitarOpciones(
                true
        );

        if (
                preguntaActual < 0 ||
                        preguntaActual >=
                                preguntasPartida.size()
        ) {

            tvPregunta.setText(
                    "Esperando pregunta..."
            );

            return;
        }

        Map<String, Object> pregunta =
                preguntasPartida.get(
                        preguntaActual
                );

        String area =
                textoMapa(
                        pregunta,
                        "area"
                );

        String enunciado =
                textoMapa(
                        pregunta,
                        "pregunta"
                );

        tvAreaPregunta.setText(
                area
        );

        tvPregunta.setText(
                enunciado
        );

        tvProgresoPregunta.setText(
                "Pregunta "
                        + (preguntaActual + 1)
                        + " de "
                        + preguntasPartida.size()
        );

        List<String> opciones =
                opcionesDe(
                        pregunta
                );

        String[] letras = {
                "A", "B", "C", "D"
        };

        for (
                int i = 0;
                i < botonesOpciones.length;
                i++
        ) {

            String texto =
                    i < opciones.size()
                            ? opciones.get(i)
                            : "";

            botonesOpciones[i]
                    .setText(
                            letras[i]
                                    + ". "
                                    + texto
                    );
        }

        iniciarTimerPregunta();
    }

    private void iniciarTimerPregunta() {

        timerPregunta =
                new CountDownTimer(
                        TIEMPO_PREGUNTA_MS,
                        250L
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished
                    ) {

                        long segundos =
                                (
                                        millisUntilFinished
                                                + 999L
                                )
                                        / 1000L;

                        tvTiempo.setText(
                                segundos + " s"
                        );
                    }

                    @Override
                    public void onFinish() {

                        tvTiempo.setText(
                                "0 s"
                        );

                        habilitarOpciones(
                                false
                        );

                        if (!respuestaEnviada) {

                            tvEstadoRespuesta
                                    .setText(
                                            "Tiempo agotado"
                                    );
                        }

                        if (soyAnfitrion) {
                            finalizarPregunta();
                        }
                    }
                };

        timerPregunta.start();
    }

    private void enviarRespuesta(
            int indiceOpcion
    ) {

        if (
                respuestaEnviada ||
                        preguntaActual < 0 ||
                        preguntaActual >=
                                preguntasPartida.size() ||
                        auth.getCurrentUser() == null ||
                        partidaId.isEmpty()
        ) {
            return;
        }

        Map<String, Object> pregunta =
                preguntasPartida.get(
                        preguntaActual
                );

        int indiceCorrecto =
                enteroMapa(
                        pregunta,
                        "indiceCorrecto"
                );

        boolean correcta =
                indiceOpcion ==
                        indiceCorrecto;

        String uid =
                auth
                        .getCurrentUser()
                        .getUid();

        /*
         * Feedback INMEDIATO:
         * al tocar una opción se pinta amarilla para que el jugador
         * sepa que el toque sí fue registrado.
         */
        respuestaEnviada =
                true;

        indiceRespuestaUsuario =
                indiceOpcion;

        marcarOpcionSeleccionada(
                indiceOpcion
        );

        habilitarOpciones(
                false
        );

        tvEstadoRespuesta.setText(
                "Respuesta seleccionada · enviando..."
        );

        String idRespuesta =
                partidaId
                        + "_"
                        + preguntaActual
                        + "_"
                        + uid;

        DocumentReference respuestaRef =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("respuestas")
                        .document(idRespuesta);

        firestore
                .runTransaction(
                        transaction -> {

                            DocumentSnapshot existente =
                                    transaction.get(
                                            respuestaRef
                                    );

                            if (
                                    existente.exists()
                            ) {
                                return false;
                            }

                            Map<String, Object> respuesta =
                                    new HashMap<>();

                            respuesta.put(
                                    "partidaId",
                                    partidaId
                            );

                            respuesta.put(
                                    "preguntaIndex",
                                    preguntaActual
                            );

                            respuesta.put(
                                    "uid",
                                    uid
                            );

                            respuesta.put(
                                    "opcionIndex",
                                    indiceOpcion
                            );

                            respuesta.put(
                                    "correcta",
                                    correcta
                            );

                            respuesta.put(
                                    "respondidoEn",
                                    FieldValue.serverTimestamp()
                            );

                            transaction.set(
                                    respuestaRef,
                                    respuesta
                            );

                            return true;
                        }
                )
                .addOnSuccessListener(
                        guardada -> {

                            if (
                                    Boolean.TRUE.equals(
                                            guardada
                                    )
                            ) {

                                tvEstadoRespuesta
                                        .setText(
                                                "Respuesta enviada correctamente"
                                        );

                                btnContinuarRespuesta.setVisibility(
                                        View.VISIBLE
                                );

                            } else {

                                tvEstadoRespuesta
                                        .setText(
                                                "Tu respuesta ya estaba registrada"
                                        );

                                btnContinuarRespuesta.setVisibility(
                                        View.VISIBLE
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {

                            /*
                             * Si Firebase falla, devolvemos el botón a su estado
                             * normal para que el jugador pueda intentar de nuevo.
                             */
                            respuestaEnviada =
                                    false;

                            btnContinuarRespuesta.setVisibility(
                                    View.GONE
                            );

                            indiceRespuestaUsuario =
                                    -1;

                            restaurarColoresOpciones();

                            habilitarOpciones(
                                    true
                            );

                            tvEstadoRespuesta
                                    .setText(
                                            "No se pudo enviar. Intenta otra vez."
                                    );

                            Toast.makeText(
                                    this,
                                    "Error al responder: "
                                            + (
                                            error.getMessage() != null
                                                    ? error.getMessage()
                                                    : "desconocido"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void finalizarPregunta() {

        if (
                !soyAnfitrion ||
                        finalizandoPregunta ||
                        preguntaActual < 0 ||
                        preguntaActual >=
                                preguntasPartida.size() ||
                        partidaId.isEmpty()
        ) {
            return;
        }

        finalizandoPregunta =
                true;

        cancelarTimerPregunta();

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("respuestas")
                .get()
                .addOnSuccessListener(
                        respuestasSnapshot -> {

                            List<DocumentSnapshot> respuestas =
                                    new ArrayList<>();

                            for (
                                    DocumentSnapshot documento :
                                    respuestasSnapshot
                                            .getDocuments()
                            ) {

                                String partida =
                                        documento.getString(
                                                "partidaId"
                                        );

                                Long indice =
                                        documento.getLong(
                                                "preguntaIndex"
                                        );

                                if (
                                        partidaId.equals(
                                                partida
                                        )
                                                &&
                                                indice != null
                                                &&
                                                indice.intValue()
                                                        ==
                                                        preguntaActual
                                ) {

                                    respuestas.add(
                                            documento
                                    );
                                }
                            }

                            respuestas.sort(
                                    Comparator.comparingLong(
                                            this::momentoRespuesta
                                    )
                            );

                            calcularYGuardarPuntos(
                                    respuestas
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {
                            finalizandoPregunta =
                                    false;

                            Toast.makeText(
                                    this,
                                    "No se pudieron calcular los resultados.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }

    private long momentoRespuesta(
            DocumentSnapshot respuesta
    ) {

        Timestamp timestamp =
                respuesta.getTimestamp(
                        "respondidoEn"
                );

        return timestamp != null
                ? timestamp.toDate().getTime()
                : Long.MAX_VALUE;
    }

    private void calcularYGuardarPuntos(
            List<DocumentSnapshot> respuestas
    ) {

        Map<String, Integer> puntosPorUid =
                new HashMap<>();

        Map<String, Boolean> correctaPorUid =
                new HashMap<>();

        int lugarCorrecto = 0;

        for (
                DocumentSnapshot respuesta :
                respuestas
        ) {

            String uid =
                    respuesta.getString(
                            "uid"
                    );

            Boolean correcta =
                    respuesta.getBoolean(
                            "correcta"
                    );

            if (uid == null) {
                continue;
            }

            if (
                    Boolean.TRUE.equals(
                            correcta
                    )
            ) {

                int puntos =
                        lugarCorrecto <
                                PUNTOS_POR_LUGAR.length
                                ? PUNTOS_POR_LUGAR[
                                lugarCorrecto
                                ]
                                : 0;

                puntosPorUid.put(
                        uid,
                        puntos
                );

                correctaPorUid.put(
                        uid,
                        true
                );

                lugarCorrecto++;

            } else {

                puntosPorUid.put(
                        uid,
                        0
                );

                correctaPorUid.put(
                        uid,
                        false
                );
            }
        }

        DocumentReference salaRef =
                firestore
                        .collection("salas")
                        .document(codigoSala);

        salaRef
                .collection("participantes")
                .get()
                .addOnSuccessListener(
                        participantes -> {

                            WriteBatch batch =
                                    firestore.batch();

                            for (
                                    DocumentSnapshot jugador :
                                    participantes.getDocuments()
                            ) {

                                String uid =
                                        jugador.getId();

                                int puntos =
                                        puntosPorUid.getOrDefault(
                                                uid,
                                                0
                                        );

                                boolean correcta =
                                        correctaPorUid.getOrDefault(
                                                uid,
                                                false
                                        );

                                Map<String, Object> cambio =
                                        new HashMap<>();

                                cambio.put(
                                        "puntosPartida",
                                        FieldValue.increment(
                                                puntos
                                        )
                                );

                                cambio.put(
                                        "ultimoPuntaje",
                                        puntos
                                );

                                cambio.put(
                                        "ultimaCorrecta",
                                        correcta
                                );

                                if (correcta) {
                                    cambio.put(
                                            "aciertosPartida",
                                            FieldValue.increment(1)
                                    );
                                }

                                batch.update(
                                        jugador.getReference(),
                                        cambio
                                );
                            }

                            batch.commit()
                                    .addOnSuccessListener(
                                            unused ->
                                                    publicarResultado(
                                                            salaRef
                                                    )
                                    )
                                    .addOnFailureListener(
                                            error ->
                                                    falloFinalizar()
                                    );
                        }
                )
                .addOnFailureListener(
                        error ->
                                falloFinalizar()
                );
    }

    private void publicarResultado(
            DocumentReference salaRef
    ) {

        Map<String, Object> pregunta =
                preguntasPartida.get(
                        preguntaActual
                );

        List<String> opciones =
                opcionesDe(
                        pregunta
                );

        int indiceCorrecto =
                enteroMapa(
                        pregunta,
                        "indiceCorrecto"
                );

        String respuestaCorrecta =
                indiceCorrecto >= 0
                        &&
                        indiceCorrecto <
                                opciones.size()
                        ? opciones.get(
                        indiceCorrecto
                )
                        : "";

        String retro =
                textoMapa(
                        pregunta,
                        "retroalimentacion"
                );

        Map<String, Object> datos =
                new HashMap<>();

        datos.put(
                "fasePartida",
                "resultado"
        );

        datos.put(
                "respuestaCorrectaTexto",
                respuestaCorrecta
        );

        datos.put(
                "retroalimentacionActual",
                retro
        );

        salaRef
                .update(datos)
                .addOnFailureListener(
                        error ->
                                falloFinalizar()
                );
    }

    private void falloFinalizar() {

        finalizandoPregunta =
                false;

        Toast.makeText(
                this,
                "No se pudo cerrar la ronda.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void mostrarResultado(
            String correcta,
            String retro
    ) {

        cancelarTimerPregunta();

        /*
         * Dejamos las opciones visibles:
         * - correcta = verde
         * - selección incorrecta del jugador = rojo
         */
        contenedorOpciones.setVisibility(
                View.VISIBLE
        );

        habilitarOpciones(
                false
        );

        panelResultado.setVisibility(
                View.VISIBLE
        );

        btnContinuarRespuesta.setVisibility(
                View.GONE
        );

        btnListoResultado.setVisibility(
                View.VISIBLE
        );
        btnListoResultado.setEnabled(true);
        btnListoResultado.setText(
                "Listo · ya leí la justificación"
        );

        tvLecturasResultado.setVisibility(
                View.VISIBLE
        );
        tvLecturasResultado.setText(
                "Esperando confirmación de los jugadores..."
        );

        avanzandoTrasLecturas = false;

        panelPodioFinal.setVisibility(
                View.GONE
        );

        cardRankingTexto.setVisibility(
                View.VISIBLE
        );

        tvTiempo.setText(
                "Resultado"
        );

        tvResultadoTitulo.setText(
                "Respuesta correcta: "
                        + (
                        correcta != null
                                ? correcta
                                : ""
                )
        );

        tvRetroalimentacion.setText(
                retro != null
                        ? retro
                        : ""
        );

        pintarResultadoOpciones();

        /*
         * Si la Activity se recreó o llegó tarde al resultado,
         * recuperamos la opción que eligió este jugador.
         */
        cargarRespuestaDelJugador();

        /*
         * Mostramos si acertó y cuántos puntos ganó en esta ronda.
         */
        cargarResultadoDelJugador();

        // La siguiente pregunta ya no avanza por tiempo.
        // Avanza únicamente cuando TODOS confirman que leyeron la justificación.
    }

    private void iniciarTimerResultado() {

        cancelarTimerResultado();

        timerResultado =
                new CountDownTimer(
                        TIEMPO_RESULTADO_MS,
                        1000L
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished
                    ) {

                        long segundos =
                                (
                                        millisUntilFinished
                                                + 999L
                                )
                                        / 1000L;

                        tvTiempo.setText(
                                "Siguiente en "
                                        + segundos
                                        + " s"
                        );
                    }

                    @Override
                    public void onFinish() {
                        avanzarPregunta();
                    }
                };

        timerResultado.start();
    }

    private void avanzarPregunta() {

        if (!soyAnfitrion) {
            return;
        }

        DocumentReference salaRef =
                firestore
                        .collection("salas")
                        .document(codigoSala);

        int siguiente =
                preguntaActual + 1;

        if (
                siguiente >=
                        preguntasPartida.size()
        ) {

            Map<String, Object> finalPartida =
                    new HashMap<>();

            finalPartida.put(
                    "estadoPartida",
                    "finalizada"
            );

            finalPartida.put(
                    "fasePartida",
                    "final"
            );

            salaRef.update(
                    finalPartida
            );

            return;
        }

        Map<String, Object> siguientePregunta =
                new HashMap<>();

        siguientePregunta.put(
                "preguntaActual",
                siguiente
        );

        siguientePregunta.put(
                "fasePartida",
                "pregunta"
        );

        siguientePregunta.put(
                "inicioPregunta",
                FieldValue.serverTimestamp()
        );

        siguientePregunta.put(
                "respuestaCorrectaTexto",
                ""
        );

        siguientePregunta.put(
                "retroalimentacionActual",
                ""
        );

        salaRef.update(
                siguientePregunta
        );
    }

    private void mostrarFinal() {

        cancelarTimerPregunta();
        cancelarTimerResultado();

        clavePantalla =
                "final_" + partidaId;

        tvProgresoPregunta.setText(
                "Partida finalizada"
        );

        tvAreaPregunta.setText(
                "RESULTADOS FINALES"
        );

        tvTiempo.setText(
                "Fin"
        );

        tvPregunta.setText(
                "¡Desafío Clínico completado!"
        );

        contenedorOpciones.setVisibility(
                View.GONE
        );

        btnContinuarRespuesta.setVisibility(
                View.GONE
        );

        btnListoResultado.setVisibility(
                View.GONE
        );

        tvLecturasResultado.setVisibility(
                View.GONE
        );

        panelResultado.setVisibility(
                View.VISIBLE
        );

        panelPodioFinal.setVisibility(
                View.VISIBLE
        );

        actualizarPodioFinal(
                jugadoresRankingActual
        );

        tvResultadoTitulo.setText(
                "Clasificación final"
        );

        tvRetroalimentacion.setText(
                "El ranking de abajo muestra el puntaje total de la partida."
        );

        tvEstadoRespuesta.setText(
                "Decidan si quieren volver a jugar."
        );

        contenedorAccionesFinales.setVisibility(
                View.VISIBLE
        );

        if (btnRevisarRespuestas != null) {
            btnRevisarRespuestas.setVisibility(
                    View.VISIBLE
            );
            btnRevisarRespuestas.setEnabled(
                    true
            );
            btnRevisarRespuestas.setText(
                    "Revisar mis respuestas"
            );
        }

        if (cardRevisionRespuestas != null) {
            cardRevisionRespuestas.setVisibility(
                    View.GONE
            );
        }

        if (contenedorRevisionRespuestas != null) {
            contenedorRevisionRespuestas.removeAllViews();
        }

        if (cardRevancha != null) {
            cardRevancha.setVisibility(
                    View.VISIBLE
            );
        }

        btnAceptarRevancha.setEnabled(
                true
        );

        btnRechazarRevancha.setEnabled(
                true
        );

        btnAceptarRevancha.setText(
                "Sí, jugar otra vez"
        );

        btnRechazarRevancha.setText(
                "No por ahora"
        );

        /*
         * El anfitrión NO puede iniciar solo.
         * El botón se habilita y aparece desde escucharRevancha()
         * únicamente cuando todos los jugadores aceptan.
         */
        btnVolverJugar.setVisibility(
                View.GONE
        );

        actualizarEstadoRevancha(
                0,
                0
        );
    }

    private void actualizarPodioFinal(
            List<DocumentSnapshot> jugadores
    ) {

        if (
                panelPodioFinal == null ||
                        jugadores == null
        ) {
            return;
        }

        int totalJugadores = jugadores.size();

        /*
         * El podio se adapta al número real de participantes:
         * 1 jugador  -> solo primer lugar.
         * 2 jugadores -> primer y segundo lugar.
         * 3 o más -> podio completo.
         */
        if (podioPuesto1 != null) {
            podioPuesto1.setVisibility(
                    totalJugadores >= 1
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (podioPuesto2 != null) {
            podioPuesto2.setVisibility(
                    totalJugadores >= 2
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (podioPuesto3 != null) {
            podioPuesto3.setVisibility(
                    totalJugadores >= 3
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (totalJugadores >= 1) {
            colocarJugadorPodio(
                    jugadores,
                    0,
                    tvPodioNombre1,
                    tvPodioPuntos1
            );
        }

        if (totalJugadores >= 2) {
            colocarJugadorPodio(
                    jugadores,
                    1,
                    tvPodioNombre2,
                    tvPodioPuntos2
            );
        }

        if (totalJugadores >= 3) {
            colocarJugadorPodio(
                    jugadores,
                    2,
                    tvPodioNombre3,
                    tvPodioPuntos3
            );
        }

        StringBuilder restantes =
                new StringBuilder();

        for (
                int i = 3;
                i < totalJugadores;
                i++
        ) {

            DocumentSnapshot jugador =
                    jugadores.get(i);

            restantes
                    .append(i + 1)
                    .append(". ")
                    .append(nombreDe(jugador))
                    .append("  ·  ")
                    .append(puntosDe(jugador))
                    .append(" pts");

            if (i < totalJugadores - 1) {
                restantes.append('\n');
            }
        }

        if (cardRankingTexto != null) {
            if (totalJugadores > 3) {

                cardRankingTexto.setVisibility(
                        View.VISIBLE
                );

                tvRanking.setText(
                        restantes.toString()
                );

            } else {

                cardRankingTexto.setVisibility(
                        View.GONE
                );
            }
        }
    }

    private void colocarJugadorPodio(
            List<DocumentSnapshot> jugadores,
            int posicion,
            TextView tvNombre,
            TextView tvPuntos
    ) {

        if (posicion < jugadores.size()) {

            DocumentSnapshot jugador =
                    jugadores.get(posicion);

            tvNombre.setText(
                    nombreDe(jugador)
            );

            tvPuntos.setText(
                    puntosDe(jugador) + " pts"
            );

            tvNombre.setVisibility(
                    View.VISIBLE
            );

            tvPuntos.setVisibility(
                    View.VISIBLE
            );

        } else {

            tvNombre.setText(
                    "—"
            );

            tvPuntos.setText(
                    ""
            );
        }
    }

    private String nombreDe(
            DocumentSnapshot jugador
    ) {

        String nombre =
                jugador.getString(
                        "nombre"
                );

        if (
                nombre == null ||
                        nombre.trim().isEmpty()
        ) {
            return "Jugador";
        }

        return nombre.trim();
    }


    // =========================================================
    // REVANCHA: TODOS DEBEN ESTAR DE ACUERDO
    // =========================================================

    // =========================================================
    // REVISIÓN FINAL DE RESPUESTAS
    // =========================================================

    private void alternarRevisionRespuestas() {

        if (
                cardRevisionRespuestas == null ||
                        contenedorRevisionRespuestas == null ||
                        btnRevisarRespuestas == null
        ) {
            return;
        }

        if (
                cardRevisionRespuestas.getVisibility() ==
                        View.VISIBLE
        ) {

            cardRevisionRespuestas.setVisibility(
                    View.GONE
            );

            btnRevisarRespuestas.setText(
                    "Revisar mis respuestas"
            );

            return;
        }

        cargarRevisionRespuestas();
    }

    private void cargarRevisionRespuestas() {

        if (
                auth.getCurrentUser() == null ||
                        partidaId.isEmpty() ||
                        preguntasPartida == null ||
                        preguntasPartida.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Todavía no hay respuestas para revisar.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnRevisarRespuestas.setEnabled(
                false
        );

        btnRevisarRespuestas.setText(
                "Cargando respuestas..."
        );

        String uid =
                auth.getCurrentUser().getUid();

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("respuestas")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            Map<Integer, DocumentSnapshot> respuestasUsuario =
                                    new HashMap<>();

                            for (
                                    DocumentSnapshot documento :
                                    snapshot.getDocuments()
                            ) {

                                String idPartida =
                                        documento.getString(
                                                "partidaId"
                                        );

                                String uidRespuesta =
                                        documento.getString(
                                                "uid"
                                        );

                                Long indice =
                                        documento.getLong(
                                                "preguntaIndex"
                                        );

                                if (
                                        partidaId.equals(idPartida) &&
                                                uid.equals(uidRespuesta) &&
                                                indice != null
                                ) {
                                    respuestasUsuario.put(
                                            indice.intValue(),
                                            documento
                                    );
                                }
                            }

                            renderizarRevisionRespuestas(
                                    respuestasUsuario
                            );

                            cardRevisionRespuestas.setVisibility(
                                    View.VISIBLE
                            );

                            btnRevisarRespuestas.setEnabled(
                                    true
                            );

                            btnRevisarRespuestas.setText(
                                    "Ocultar revisión"
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            btnRevisarRespuestas.setEnabled(
                                    true
                            );

                            btnRevisarRespuestas.setText(
                                    "Revisar mis respuestas"
                            );

                            Toast.makeText(
                                    this,
                                    "No se pudieron cargar tus respuestas.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void renderizarRevisionRespuestas(
            Map<Integer, DocumentSnapshot> respuestasUsuario
    ) {

        contenedorRevisionRespuestas.removeAllViews();

        for (
                int i = 0;
                i < preguntasPartida.size();
                i++
        ) {

            Map<String, Object> pregunta =
                    preguntasPartida.get(i);

            DocumentSnapshot respuesta =
                    respuestasUsuario.get(i);

            int indiceCorrecto =
                    enteroMapa(
                            pregunta,
                            "indiceCorrecto"
                    );

            int indiceElegido =
                    -1;

            boolean correcta =
                    false;

            if (respuesta != null) {

                Long opcion =
                        respuesta.getLong(
                                "opcionIndex"
                        );

                if (opcion != null) {
                    indiceElegido =
                            opcion.intValue();
                }

                Boolean fueCorrecta =
                        respuesta.getBoolean(
                                "correcta"
                        );

                correcta =
                        Boolean.TRUE.equals(
                                fueCorrecta
                        );
            }

            List<String> opciones =
                    opcionesDe(
                            pregunta
                    );

            String respuestaJugador =
                    textoOpcionRevision(
                            opciones,
                            indiceElegido
                    );

            String respuestaCorrecta =
                    textoOpcionRevision(
                            opciones,
                            indiceCorrecto
                    );

            String justificacion =
                    textoMapa(
                            pregunta,
                            "retroalimentacion"
                    );

            agregarTarjetaRevision(
                    i + 1,
                    textoMapa(
                            pregunta,
                            "pregunta"
                    ),
                    respuestaJugador,
                    respuestaCorrecta,
                    justificacion,
                    respuesta != null,
                    correcta
            );
        }
    }

    private String textoOpcionRevision(
            List<String> opciones,
            int indice
    ) {

        if (
                indice < 0 ||
                        indice >= opciones.size()
        ) {
            return "Sin respuesta";
        }

        String[] letras = {
                "A", "B", "C", "D"
        };

        String letra =
                indice < letras.length
                        ? letras[indice]
                        : String.valueOf(
                        indice + 1
                );

        return letra
                + ". "
                + opciones.get(indice);
    }

    private void agregarTarjetaRevision(
            int numeroPregunta,
            String pregunta,
            String respuestaJugador,
            String respuestaCorrecta,
            String justificacion,
            boolean respondida,
            boolean correcta
    ) {

        MaterialCardView tarjeta =
                new MaterialCardView(
                        this
                );

        LinearLayout.LayoutParams paramsTarjeta =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsTarjeta.bottomMargin =
                dp(12);

        tarjeta.setLayoutParams(
                paramsTarjeta
        );

        tarjeta.setRadius(
                dp(18)
        );

        tarjeta.setCardElevation(
                dp(1)
        );

        tarjeta.setStrokeWidth(
                dp(1)
        );

        tarjeta.setCardBackgroundColor(
                Color.WHITE
        );

        tarjeta.setStrokeColor(
                Color.parseColor(
                        correcta
                                ? "#BFE8D0"
                                : respondida
                                ? "#F3C8C8"
                                : "#DED7FF"
                )
        );

        LinearLayout contenido =
                new LinearLayout(
                        this
                );

        contenido.setOrientation(
                LinearLayout.VERTICAL
        );

        contenido.setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
        );

        TextView estado =
                crearTextoRevision(
                        respondida
                                ? correcta
                                ? "✓ Correcta"
                                : "✕ Incorrecta"
                                : "Sin respuesta",
                        12,
                        correcta
                                ? "#218A57"
                                : respondida
                                ? "#C94A4A"
                                : "#777386",
                        true
                );

        contenido.addView(
                estado
        );

        TextView tituloPregunta =
                crearTextoRevision(
                        "Pregunta "
                                + numeroPregunta
                                + " · "
                                + pregunta,
                        14,
                        "#29263A",
                        true
                );

        LinearLayout.LayoutParams paramsTitulo =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsTitulo.topMargin =
                dp(7);

        tituloPregunta.setLayoutParams(
                paramsTitulo
        );

        contenido.addView(
                tituloPregunta
        );

        TextView tuRespuesta =
                crearTextoRevision(
                        "Tu respuesta: "
                                + respuestaJugador,
                        12,
                        correcta
                                ? "#218A57"
                                : respondida
                                ? "#C94A4A"
                                : "#777386",
                        false
                );

        LinearLayout.LayoutParams paramsRespuesta =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsRespuesta.topMargin =
                dp(9);

        tuRespuesta.setLayoutParams(
                paramsRespuesta
        );

        contenido.addView(
                tuRespuesta
        );

        TextView correctaView =
                crearTextoRevision(
                        "Respuesta correcta: "
                                + respuestaCorrecta,
                        12,
                        "#218A57",
                        true
                );

        LinearLayout.LayoutParams paramsCorrecta =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsCorrecta.topMargin =
                dp(5);

        correctaView.setLayoutParams(
                paramsCorrecta
        );

        contenido.addView(
                correctaView
        );

        TextView justificacionView =
                crearTextoRevision(
                        "Justificación: "
                                + (
                                justificacion.isEmpty()
                                        ? "Sin justificación disponible."
                                        : justificacion
                        ),
                        11,
                        "#6C648E",
                        false
                );

        LinearLayout.LayoutParams paramsJustificacion =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsJustificacion.topMargin =
                dp(8);

        justificacionView.setLayoutParams(
                paramsJustificacion
        );

        contenido.addView(
                justificacionView
        );

        tarjeta.addView(
                contenido
        );

        contenedorRevisionRespuestas.addView(
                tarjeta
        );
    }

    private TextView crearTextoRevision(
            String texto,
            int tamanoSp,
            String colorHex,
            boolean negrita
    ) {

        TextView vista =
                new TextView(
                        this
                );

        vista.setText(
                texto
        );

        vista.setTextSize(
                tamanoSp
        );

        vista.setTextColor(
                Color.parseColor(
                        colorHex
                )
        );

        vista.setLineSpacing(
                dp(2),
                1.0f
        );

        if (negrita) {
            vista.setTypeface(
                    vista.getTypeface(),
                    android.graphics.Typeface.BOLD
            );
        }

        return vista;
    }

    private int dp(
            int valor
    ) {
        return Math.round(
                valor *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    private void escucharRevancha() {

        if (listenerRevancha != null) {
            listenerRevancha.remove();
            listenerRevancha = null;
        }

        listenerRevancha =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("revancha")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (
                                            error != null ||
                                                    snapshot == null ||
                                                    partidaId.isEmpty()
                                    ) {
                                        return;
                                    }

                                    int aceptaron = 0;
                                    int rechazaron = 0;

                                    for (
                                            DocumentSnapshot documento :
                                            snapshot.getDocuments()
                                    ) {

                                        String idPartida =
                                                documento.getString(
                                                        "partidaId"
                                                );

                                        if (
                                                !partidaId.equals(
                                                        idPartida
                                                )
                                        ) {
                                            continue;
                                        }

                                        Boolean acepta =
                                                documento.getBoolean(
                                                        "acepta"
                                                );

                                        if (
                                                Boolean.TRUE.equals(
                                                        acepta
                                                )
                                        ) {
                                            aceptaron++;

                                        } else if (
                                                Boolean.FALSE.equals(
                                                        acepta
                                                )
                                        ) {
                                            rechazaron++;
                                        }
                                    }

                                    actualizarEstadoRevancha(
                                            aceptaron,
                                            rechazaron
                                    );
                                }
                        );
    }

    private void confirmarRevancha(
            boolean acepta
    ) {

        if (
                auth.getCurrentUser() == null ||
                        partidaId.isEmpty()
        ) {
            return;
        }

        String uid =
                auth
                        .getCurrentUser()
                        .getUid();

        Map<String, Object> datos =
                new HashMap<>();

        datos.put(
                "partidaId",
                partidaId
        );

        datos.put(
                "uid",
                uid
        );

        datos.put(
                "acepta",
                acepta
        );

        datos.put(
                "confirmadoEn",
                FieldValue.serverTimestamp()
        );

        btnAceptarRevancha.setEnabled(
                false
        );

        btnRechazarRevancha.setEnabled(
                false
        );

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("revancha")
                .document(uid)
                .set(datos)
                .addOnSuccessListener(
                        unused -> {

                            btnAceptarRevancha.setEnabled(
                                    true
                            );

                            btnRechazarRevancha.setEnabled(
                                    true
                            );

                            if (acepta) {

                                btnAceptarRevancha.setText(
                                        "Aceptado"
                                );

                                btnRechazarRevancha.setText(
                                        "Cambiar a no"
                                );

                            } else {

                                btnAceptarRevancha.setText(
                                        "Cambiar a sí"
                                );

                                btnRechazarRevancha.setText(
                                        "No por ahora"
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {

                            btnAceptarRevancha.setEnabled(
                                    true
                            );

                            btnRechazarRevancha.setEnabled(
                                    true
                            );

                            Toast.makeText(
                                    this,
                                    "No se pudo guardar tu decisión. Intenta otra vez.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void actualizarEstadoRevancha(
            int aceptaron,
            int rechazaron
    ) {

        if (
                tvRevanchaEstado == null ||
                        btnVolverJugar == null
        ) {
            return;
        }

        int total =
                Math.max(
                        cantidadParticipantes,
                        0
                );

        int aceptadosSeguros =
                total > 0
                        ? Math.min(
                        Math.max(
                                aceptaron,
                                0
                        ),
                        total
                )
                        : Math.max(
                        aceptaron,
                        0
                );

        if (total <= 0) {

            tvRevanchaEstado.setText(
                    "Esperando jugadores..."
            );

            btnVolverJugar.setVisibility(
                    View.GONE
            );

            return;
        }

        int faltan =
                Math.max(
                        total - aceptadosSeguros,
                        0
                );

        if (
                aceptadosSeguros >= total
        ) {

            tvRevanchaEstado.setText(
                    total
                            + " de "
                            + total
                            + " jugadores aceptaron · todos listos"
            );

            btnVolverJugar.setVisibility(
                    soyAnfitrion
                            ? View.VISIBLE
                            : View.GONE
            );

            btnVolverJugar.setEnabled(
                    soyAnfitrion
            );

            tvEstadoRespuesta.setText(
                    soyAnfitrion
                            ? "Todos aceptaron. Ya puedes iniciar la nueva partida."
                            : "Todos aceptaron. Esperando que el anfitrión inicie."
            );

        } else {

            String texto =
                    aceptadosSeguros
                            + " de "
                            + total
                            + " jugadores aceptaron";

            if (rechazaron > 0) {
                texto +=
                        " · "
                                + rechazaron
                                + (
                                rechazaron == 1
                                        ? " no acepta por ahora"
                                        : " no aceptan por ahora"
                        );

            } else if (faltan > 0) {
                texto +=
                        " · faltan "
                                + faltan;
            }

            tvRevanchaEstado.setText(
                    texto
            );

            btnVolverJugar.setVisibility(
                    View.GONE
            );
        }
    }

    private void volverAJugar() {

        if (!soyAnfitrion) {
            return;
        }

        /*
         * Verificación final contra Firebase.
         * Aunque el botón esté visible, volvemos a comprobar que TODOS
         * aceptaron para evitar que el anfitrión pueda iniciar por accidente.
         */
        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("revancha")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            int aceptaron = 0;

                            for (
                                    DocumentSnapshot documento :
                                    snapshot.getDocuments()
                            ) {

                                String idPartida =
                                        documento.getString(
                                                "partidaId"
                                        );

                                Boolean acepta =
                                        documento.getBoolean(
                                                "acepta"
                                        );

                                if (
                                        partidaId.equals(
                                                idPartida
                                        )
                                                &&
                                                Boolean.TRUE.equals(
                                                        acepta
                                                )
                                ) {
                                    aceptaron++;
                                }
                            }

                            int total =
                                    Math.max(
                                            cantidadParticipantes,
                                            0
                                    );

                            if (
                                    total <= 0 ||
                                            aceptaron < total
                            ) {

                                Toast.makeText(
                                        this,
                                        "Aún no todos los jugadores aceptaron volver a jugar.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                actualizarEstadoRevancha(
                                        aceptaron,
                                        0
                                );

                                return;
                            }

                            reiniciarPartidaConfirmada();
                        }
                )
                .addOnFailureListener(
                        error ->
                                Toast.makeText(
                                        this,
                                        "No se pudo verificar la revancha. Intenta otra vez.",
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }

    private void reiniciarPartidaConfirmada() {

        if (!soyAnfitrion) {
            return;
        }

        btnVolverJugar.setEnabled(
                false
        );

        btnVolverJugar.setText(
                "Preparando nueva partida..."
        );

        final List<Map<String, Object>> nuevasPreguntas;

        try {

            nuevasPreguntas =
                    BancoPreguntasMultijugador
                            .seleccionarPreguntas(
                                    this,
                                    10
                            );

        } catch (Exception error) {

            btnVolverJugar.setEnabled(
                    true
            );

            btnVolverJugar.setText(
                    "Volver a jugar"
            );

            Toast.makeText(
                    this,
                    "No se pudo preparar otra partida: "
                            + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        DocumentReference salaRef =
                firestore
                        .collection("salas")
                        .document(codigoSala);

        salaRef
                .collection("participantes")
                .get()
                .addOnSuccessListener(
                        participantes -> {

                            WriteBatch batch =
                                    firestore.batch();

                            for (
                                    DocumentSnapshot jugador :
                                    participantes.getDocuments()
                            ) {

                                Map<String, Object> reinicio =
                                        new HashMap<>();

                                reinicio.put(
                                        "puntosPartida",
                                        0
                                );

                                reinicio.put(
                                        "ultimoPuntaje",
                                        0
                                );

                                reinicio.put(
                                        "ultimaCorrecta",
                                        false
                                );

                                reinicio.put(
                                        "aciertosPartida",
                                        0
                                );

                                batch.update(
                                        jugador.getReference(),
                                        reinicio
                                );
                            }

                            batch.commit()
                                    .addOnSuccessListener(
                                            unused -> {

                                                String nuevaPartidaId =
                                                        UUID
                                                                .randomUUID()
                                                                .toString();

                                                Map<String, Object> datos =
                                                        new HashMap<>();

                                                datos.put(
                                                        "estadoPartida",
                                                        "jugando"
                                                );

                                                datos.put(
                                                        "fasePartida",
                                                        "pregunta"
                                                );

                                                datos.put(
                                                        "preguntaActual",
                                                        0
                                                );

                                                datos.put(
                                                        "totalPreguntas",
                                                        nuevasPreguntas.size()
                                                );

                                                datos.put(
                                                        "partidaId",
                                                        nuevaPartidaId
                                                );

                                                datos.put(
                                                        "preguntasPartida",
                                                        nuevasPreguntas
                                                );

                                                datos.put(
                                                        "inicioPartida",
                                                        FieldValue.serverTimestamp()
                                                );

                                                datos.put(
                                                        "inicioPregunta",
                                                        FieldValue.serverTimestamp()
                                                );

                                                datos.put(
                                                        "respuestaCorrectaTexto",
                                                        ""
                                                );

                                                datos.put(
                                                        "retroalimentacionActual",
                                                        ""
                                                );

                                                /*
                                                 * Al cambiar partidaId, todos los
                                                 * dispositivos detectan automáticamente
                                                 * una partida nueva y muestran la pregunta 1.
                                                 */
                                                salaRef
                                                        .update(
                                                                datos
                                                        )
                                                        .addOnFailureListener(
                                                                error ->
                                                                        errorReinicio(
                                                                                error
                                                                        )
                                                        );
                                            }
                                    )
                                    .addOnFailureListener(
                                            this::errorReinicio
                                    );
                        }
                )
                .addOnFailureListener(
                        this::errorReinicio
                );
    }

    private void errorReinicio(
            Exception error
    ) {

        btnVolverJugar.setEnabled(
                true
        );

        btnVolverJugar.setText(
                "Volver a jugar"
        );

        Toast.makeText(
                this,
                "No se pudo reiniciar la partida: "
                        + (
                        error.getMessage() != null
                                ? error.getMessage()
                                : "error desconocido"
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    private void marcarOpcionSeleccionada(
            int indice
    ) {

        restaurarColoresOpciones();

        if (
                indice >= 0 &&
                        indice < botonesOpciones.length
        ) {

            botonesOpciones[indice]
                    .setBackgroundTintList(
                            ColorStateList.valueOf(
                                    Color.parseColor(
                                            "#F4B942"
                                    )
                            )
                    );
        }
    }

    private void pintarResultadoOpciones() {

        restaurarColoresOpciones();

        if (
                preguntaActual < 0 ||
                        preguntaActual >=
                                preguntasPartida.size()
        ) {
            return;
        }

        Map<String, Object> pregunta =
                preguntasPartida.get(
                        preguntaActual
                );

        int indiceCorrecto =
                enteroMapa(
                        pregunta,
                        "indiceCorrecto"
                );

        if (
                indiceCorrecto >= 0 &&
                        indiceCorrecto <
                                botonesOpciones.length
        ) {

            botonesOpciones[
                    indiceCorrecto
                    ].setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#22A06B"
                            )
                    )
            );
        }

        if (
                indiceRespuestaUsuario >= 0 &&
                        indiceRespuestaUsuario <
                                botonesOpciones.length &&
                        indiceRespuestaUsuario !=
                                indiceCorrecto
        ) {

            botonesOpciones[
                    indiceRespuestaUsuario
                    ].setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#D64545"
                            )
                    )
            );
        }
    }

    private void restaurarColoresOpciones() {

        if (
                botonesOpciones == null
        ) {
            return;
        }

        for (
                MaterialButton boton :
                botonesOpciones
        ) {

            boton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#6C5CE7"
                            )
                    )
            );

            boton.setTextColor(
                    Color.WHITE
            );
        }
    }

    private void cargarRespuestaDelJugador() {

        if (
                auth.getCurrentUser() == null ||
                        partidaId.isEmpty() ||
                        preguntaActual < 0
        ) {
            return;
        }

        String uid =
                auth
                        .getCurrentUser()
                        .getUid();

        String idRespuesta =
                partidaId
                        + "_"
                        + preguntaActual
                        + "_"
                        + uid;

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("respuestas")
                .document(idRespuesta)
                .get()
                .addOnSuccessListener(
                        documento -> {

                            Long opcion =
                                    documento.getLong(
                                            "opcionIndex"
                                    );

                            if (opcion != null) {

                                indiceRespuestaUsuario =
                                        opcion.intValue();

                                pintarResultadoOpciones();
                            }
                        }
                );
    }

    private void cargarResultadoDelJugador() {

        if (
                auth.getCurrentUser() == null
        ) {
            return;
        }

        String uid =
                auth
                        .getCurrentUser()
                        .getUid();

        firestore
                .collection("salas")
                .document(codigoSala)
                .collection("participantes")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        jugador -> {

                            Boolean correcta =
                                    jugador.getBoolean(
                                            "ultimaCorrecta"
                                    );

                            Long puntos =
                                    jugador.getLong(
                                            "ultimoPuntaje"
                                    );

                            long puntosRonda =
                                    puntos != null
                                            ? puntos
                                            : 0L;

                            if (
                                    Boolean.TRUE.equals(
                                            correcta
                                    )
                            ) {

                                tvEstadoRespuesta.setText(
                                        "¡Correcto!  +"
                                                + puntosRonda
                                                + " puntos"
                                );

                            } else {

                                tvEstadoRespuesta.setText(
                                        "Incorrecto · +0 puntos"
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error ->
                                tvEstadoRespuesta.setText(
                                        "Resultado de la ronda"
                                )
                );
    }

    private void habilitarOpciones(
            boolean habilitar
    ) {

        for (
                MaterialButton boton :
                botonesOpciones
        ) {

            boton.setEnabled(
                    habilitar
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> opcionesDe(
            Map<String, Object> pregunta
    ) {

        Object opciones =
                pregunta.get(
                        "opciones"
                );

        if (
                opciones instanceof
                        List
        ) {
            return (List<String>)
                    opciones;
        }

        return new ArrayList<>();
    }

    private String textoMapa(
            Map<String, Object> mapa,
            String clave
    ) {

        Object valor =
                mapa.get(
                        clave
                );

        return valor != null
                ? String.valueOf(
                valor
        )
                : "";
    }

    private int enteroMapa(
            Map<String, Object> mapa,
            String clave
    ) {

        Object valor =
                mapa.get(
                        clave
                );

        if (
                valor instanceof
                        Number
        ) {

            return (
                    (Number) valor
            ).intValue();
        }

        return -1;
    }

    private void cancelarTimerPregunta() {

        if (
                timerPregunta != null
        ) {

            timerPregunta.cancel();
            timerPregunta = null;
        }
    }

    private void cancelarTimerResultado() {

        if (
                timerResultado != null
        ) {

            timerResultado.cancel();
            timerResultado = null;
        }
    }

    @Override
    protected void onDestroy() {

        cancelarTimerPregunta();
        cancelarTimerResultado();

        if (listenerSala != null) {
            listenerSala.remove();
            listenerSala = null;
        }

        if (
                listenerParticipantes !=
                        null
        ) {
            listenerParticipantes.remove();
            listenerParticipantes = null;
        }

        if (
                listenerRespuestas !=
                        null
        ) {
            listenerRespuestas.remove();
            listenerRespuestas = null;
        }

        if (
                listenerLecturasResultado !=
                        null
        ) {
            listenerLecturasResultado.remove();
            listenerLecturasResultado = null;
        }

        if (
                listenerRevancha !=
                        null
        ) {
            listenerRevancha.remove();
            listenerRevancha = null;
        }

        super.onDestroy();
    }
}

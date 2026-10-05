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

    private String codigoSala;
    private String partidaId = "";
    private String anfitrionId = "";

    private boolean soyAnfitrion = false;
    private boolean respuestaEnviada = false;
    private boolean finalizandoPregunta = false;

    private int cantidadParticipantes = 0;
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

    private LinearLayout contenedorOpciones;
    private LinearLayout panelResultado;

    private MaterialButton btnOpcionA;
    private MaterialButton btnOpcionB;
    private MaterialButton btnOpcionC;
    private MaterialButton btnOpcionD;
    private MaterialButton btnVolverJugar;
    private MaterialButton btnSalirPartida;

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

        btnVolverJugar.setOnClickListener(
                v -> volverAJugar()
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

                                    List<DocumentSnapshot> jugadores =
                                            new ArrayList<>(
                                                    snapshot
                                                            .getDocuments()
                                            );

                                    jugadores.sort(
                                            (a, b) ->
                                                    Long.compare(
                                                            puntosDe(b),
                                                            puntosDe(a)
                                                    )
                                    );

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
                                            !soyAnfitrion ||
                                            finalizandoPregunta ||
                                            preguntaActual < 0 ||
                                            cantidadParticipantes < 2 ||
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

                                    if (
                                            respuestasActuales >=
                                                    cantidadParticipantes
                                    ) {
                                        finalizarPregunta();
                                    }
                                }
                        );
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

        restaurarColoresOpciones();

        contenedorAccionesFinales.setVisibility(
                View.GONE
        );

        panelResultado.setVisibility(
                View.GONE
        );

        contenedorOpciones.setVisibility(
                View.VISIBLE
        );

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
                                                "Respuesta enviada · esperando a los demás"
                                        );

                            } else {

                                tvEstadoRespuesta
                                        .setText(
                                                "Tu respuesta ya estaba registrada"
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

        if (soyAnfitrion) {
            iniciarTimerResultado();
        }
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

        panelResultado.setVisibility(
                View.VISIBLE
        );

        tvResultadoTitulo.setText(
                "Clasificación final"
        );

        tvRetroalimentacion.setText(
                "El ranking de abajo muestra el puntaje total de la partida."
        );

        tvEstadoRespuesta.setText(
                soyAnfitrion
                        ? "Puedes iniciar otra partida con los mismos jugadores."
                        : "El anfitrión puede iniciar una nueva partida."
        );

        contenedorAccionesFinales.setVisibility(
                View.VISIBLE
        );

        /*
         * Solo el anfitrión reinicia la sala para que todos
         * entren juntos a la misma nueva partida.
         */
        btnVolverJugar.setVisibility(
                soyAnfitrion
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void volverAJugar() {

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

        super.onDestroy();
    }
}

package com.utm.semiologia.ui.estudio;

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

public class QuizMultijugadorActivity
        extends AppCompatActivity {

    public static final String EXTRA_CODIGO_SALA =
            "extra_codigo_sala";

    private static final long TIEMPO_PREGUNTA_MS =
            15_000L;

    private static final long TIEMPO_RESULTADO_MS =
            5_000L;

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

                                respuestaEnviada =
                                        true;

                                habilitarOpciones(
                                        false
                                );

                                tvEstadoRespuesta
                                        .setText(
                                                "Respuesta enviada · esperando a los demás"
                                        );
                            }
                        }
                )
                .addOnFailureListener(
                        error ->
                                Toast.makeText(
                                        this,
                                        "No se pudo enviar la respuesta.",
                                        Toast.LENGTH_SHORT
                                ).show()
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

        contenedorOpciones.setVisibility(
                View.GONE
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

        tvEstadoRespuesta.setText(
                "Ranking actualizado"
        );

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
                "Gracias por jugar"
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

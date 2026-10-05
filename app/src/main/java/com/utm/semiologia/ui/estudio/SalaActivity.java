package com.utm.semiologia.ui.estudio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.utm.semiologia.R;
import com.utm.semiologia.ui.common.BaseActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SalaActivity extends BaseActivity {

    public static final String EXTRA_CODIGO_SALA =
            "extra_codigo_sala";

    private static final int MIN_PARTICIPANTES = 2;
    private static final int MAX_PARTICIPANTES = 5;
    private static final int PREGUNTAS_POR_PARTIDA = 10;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private ListenerRegistration listenerSala;
    private ListenerRegistration listenerParticipantes;

    private String codigoSala;
    private String anfitrionUid;

    private int cantidadParticipantesActual = 0;
    private boolean abriendoPartida = false;
    private boolean iniciandoPartida = false;

    private TextView tvNombreSala;
    private TextView tvCodigoSala;
    private TextView tvCantidadParticipantes;
    private TextView tvEstadoSala;

    private LinearLayout contenedorParticipantes;

    private MaterialButton btnCopiarCodigo;
    private MaterialButton btnIniciarSesion;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_sala
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
                    "No se recibió el código de la sala.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        enlazarVistas();
        configurarListeners();

        tvCodigoSala.setText(
                codigoSala
        );

        escucharSala();
        escucharParticipantes();
    }

    private void enlazarVistas() {

        tvNombreSala =
                findViewById(
                        R.id.tvNombreSala
                );

        tvCodigoSala =
                findViewById(
                        R.id.tvCodigoSala
                );

        tvCantidadParticipantes =
                findViewById(
                        R.id.tvCantidadParticipantes
                );

        tvEstadoSala =
                findViewById(
                        R.id.tvEstadoSala
                );

        contenedorParticipantes =
                findViewById(
                        R.id.contenedorParticipantes
                );

        btnCopiarCodigo =
                findViewById(
                        R.id.btnCopiarCodigoSala
                );

        btnIniciarSesion =
                findViewById(
                        R.id.btnIniciarSesion
                );
    }

    private void configurarListeners() {

        findViewById(
                R.id.btnVolver
        ).setOnClickListener(
                v -> finish()
        );

        btnCopiarCodigo.setOnClickListener(
                v -> copiarCodigo()
        );

        btnIniciarSesion.setOnClickListener(
                v -> iniciarPartida()
        );
    }

    private void escucharSala() {

        tvEstadoSala.setText(
                "Conectando..."
        );

        listenerSala =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        tvEstadoSala.setText(
                                                "Error al conectar con la sala"
                                        );

                                        Toast.makeText(
                                                this,
                                                error.getMessage() != null
                                                        ? error.getMessage()
                                                        : "No se pudo conectar con la sala.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {

                                        tvEstadoSala.setText(
                                                "Conectando..."
                                        );

                                        return;
                                    }

                                    if (!snapshot.exists()) {

                                        if (
                                                snapshot
                                                        .getMetadata()
                                                        .isFromCache()
                                        ) {

                                            tvEstadoSala.setText(
                                                    "Conectando con Firebase..."
                                            );

                                            return;
                                        }

                                        Toast.makeText(
                                                this,
                                                "La sala ya no existe.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        finish();
                                        return;
                                    }

                                    actualizarDatosSala(
                                            snapshot
                                    );
                                }
                        );
    }

    private void actualizarDatosSala(
            DocumentSnapshot snapshot
    ) {

        String nombre =
                snapshot.getString(
                        "nombre"
                );

        anfitrionUid =
                snapshot.getString(
                        "anfitrionId"
                );

        if (
                nombre == null ||
                        nombre.trim().isEmpty()
        ) {
            nombre =
                    "Sala de estudio";
        }

        tvNombreSala.setText(
                nombre
        );

        String estadoPartida =
                snapshot.getString(
                        "estadoPartida"
                );

        if (
                estadoPartida == null ||
                        estadoPartida.trim().isEmpty()
        ) {
            estadoPartida =
                    "esperando";
        }

        if (
                "jugando".equalsIgnoreCase(
                        estadoPartida
                )
        ) {

            tvEstadoSala.setText(
                    "Partida iniciada"
            );

            abrirPartida();
            return;
        }

        if (
                "finalizada".equalsIgnoreCase(
                        estadoPartida
                )
        ) {
            tvEstadoSala.setText(
                    "Partida finalizada"
            );
        } else {
            tvEstadoSala.setText(
                    "Esperando jugadores"
            );
        }

        actualizarBotonAnfitrion();
    }

    private void escucharParticipantes() {

        listenerParticipantes =
                firestore
                        .collection("salas")
                        .document(codigoSala)
                        .collection("participantes")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        Toast.makeText(
                                                this,
                                                "No se pudieron cargar los participantes.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    contenedorParticipantes
                                            .removeAllViews();

                                    cantidadParticipantesActual =
                                            snapshot.size();

                                    tvCantidadParticipantes
                                            .setText(
                                                    cantidadParticipantesActual
                                                            + "/"
                                                            + MAX_PARTICIPANTES
                                                            + (
                                                            cantidadParticipantesActual == 1
                                                                    ? " participante"
                                                                    : " participantes"
                                                    )
                                            );

                                    for (
                                            DocumentSnapshot documento :
                                            snapshot.getDocuments()
                                    ) {
                                        agregarParticipante(
                                                documento
                                        );
                                    }

                                    actualizarBotonAnfitrion();
                                }
                        );
    }

    private void agregarParticipante(
            DocumentSnapshot documento
    ) {

        String nombre =
                documento.getString(
                        "nombre"
                );

        Boolean anfitrion =
                documento.getBoolean(
                        "anfitrion"
                );

        if (
                nombre == null ||
                        nombre.trim().isEmpty()
        ) {
            nombre =
                    "Participante";
        }

        boolean esAnfitrion =
                Boolean.TRUE.equals(
                        anfitrion
                );

        LinearLayout fila =
                new LinearLayout(
                        this
                );

        fila.setOrientation(
                LinearLayout.HORIZONTAL
        );

        fila.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );

        int padding =
                dp(14);

        fila.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        LinearLayout.LayoutParams paramsFila =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        paramsFila.setMargins(
                0,
                0,
                0,
                dp(8)
        );

        fila.setLayoutParams(
                paramsFila
        );

        fila.setBackgroundResource(
                R.drawable.bg_etiqueta_suave
        );

        TextView icono =
                new TextView(
                        this
                );

        icono.setText(
                esAnfitrion
                        ? "★"
                        : "●"
        );

        icono.setTextSize(
                18
        );

        icono.setTextColor(
                android.graphics.Color.parseColor(
                        "#6C5CE7"
                )
        );

        TextView nombreView =
                new TextView(
                        this
                );

        LinearLayout.LayoutParams paramsNombre =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        nombreView.setLayoutParams(
                paramsNombre
        );

        nombreView.setPadding(
                dp(12),
                0,
                0,
                0
        );

        nombreView.setText(
                nombre
        );

        nombreView.setTextColor(
                android.graphics.Color.parseColor(
                        "#29263A"
                )
        );

        nombreView.setTextSize(
                15
        );

        TextView estado =
                new TextView(
                        this
                );

        estado.setText(
                esAnfitrion
                        ? "Anfitrión"
                        : "Conectado"
        );

        estado.setTextColor(
                android.graphics.Color.parseColor(
                        esAnfitrion
                                ? "#6C5CE7"
                                : "#777386"
                )
        );

        estado.setTextSize(
                12
        );

        fila.addView(
                icono
        );

        fila.addView(
                nombreView
        );

        fila.addView(
                estado
        );

        contenedorParticipantes
                .addView(
                        fila
                );
    }

    private void actualizarBotonAnfitrion() {

        if (
                auth.getCurrentUser() == null ||
                        anfitrionUid == null
        ) {

            btnIniciarSesion.setVisibility(
                    View.GONE
            );

            return;
        }

        boolean soyAnfitrion =
                anfitrionUid.equals(
                        auth
                                .getCurrentUser()
                                .getUid()
                );

        if (!soyAnfitrion) {

            btnIniciarSesion.setVisibility(
                    View.GONE
            );

            if (
                    cantidadParticipantesActual <
                            MIN_PARTICIPANTES
            ) {

                tvEstadoSala.setText(
                        "Esperando al menos 2 jugadores"
                );

            } else {

                tvEstadoSala.setText(
                        "Esperando que el anfitrión inicie"
                );
            }

            return;
        }

        btnIniciarSesion.setVisibility(
                View.VISIBLE
        );

        boolean cantidadValida =
                cantidadParticipantesActual >=
                        MIN_PARTICIPANTES
                        &&
                cantidadParticipantesActual <=
                        MAX_PARTICIPANTES;

        btnIniciarSesion.setEnabled(
                cantidadValida &&
                        !iniciandoPartida
        );

        if (iniciandoPartida) {

            btnIniciarSesion.setText(
                    "Preparando partida..."
            );

            return;
        }

        if (
                cantidadParticipantesActual <
                        MIN_PARTICIPANTES
        ) {

            btnIniciarSesion.setText(
                    "Falta 1 jugador para iniciar"
            );

            tvEstadoSala.setText(
                    "Se necesitan mínimo 2 jugadores"
            );

        } else {

            btnIniciarSesion.setText(
                    "Iniciar Desafío Clínico · "
                            + cantidadParticipantesActual
                            + "/"
                            + MAX_PARTICIPANTES
            );

            tvEstadoSala.setText(
                    "Listos para jugar"
            );
        }
    }

    private void iniciarPartida() {

        if (iniciandoPartida) {
            return;
        }

        if (
                auth.getCurrentUser() == null
        ) {

            Toast.makeText(
                    this,
                    "No se encontró la sesión de Firebase.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                anfitrionUid == null ||
                        !anfitrionUid.equals(
                                auth
                                        .getCurrentUser()
                                        .getUid()
                        )
        ) {

            Toast.makeText(
                    this,
                    "Solo el anfitrión puede iniciar.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                cantidadParticipantesActual <
                        MIN_PARTICIPANTES
        ) {

            Toast.makeText(
                    this,
                    "Se necesitan mínimo 2 jugadores.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                cantidadParticipantesActual >
                        MAX_PARTICIPANTES
        ) {

            Toast.makeText(
                    this,
                    "La sala admite máximo 5 jugadores.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        iniciandoPartida = true;

        actualizarBotonAnfitrion();

        final List<Map<String, Object>> preguntas;

        try {

            preguntas =
                    BancoPreguntasMultijugador
                            .seleccionarPreguntas(
                                    this,
                                    PREGUNTAS_POR_PARTIDA
                            );

        } catch (Exception error) {

            iniciandoPartida = false;

            actualizarBotonAnfitrion();

            Toast.makeText(
                    this,
                    "No se pudo cargar el banco de preguntas: "
                            + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String partidaId =
                UUID
                        .randomUUID()
                        .toString();

        DocumentReference salaRef =
                firestore
                        .collection("salas")
                        .document(codigoSala);

        salaRef
                .collection("participantes")
                .get()
                .addOnSuccessListener(
                        participantes -> {

                            int cantidadReal =
                                    participantes.size();

                            if (
                                    cantidadReal <
                                            MIN_PARTICIPANTES
                            ) {

                                iniciandoPartida =
                                        false;

                                actualizarBotonAnfitrion();

                                Toast.makeText(
                                        this,
                                        "Se necesitan mínimo 2 jugadores.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (
                                    cantidadReal >
                                            MAX_PARTICIPANTES
                            ) {

                                iniciandoPartida =
                                        false;

                                actualizarBotonAnfitrion();

                                Toast.makeText(
                                        this,
                                        "La sala admite máximo 5 jugadores.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            WriteBatch batch =
                                    firestore.batch();

                            for (
                                    DocumentSnapshot participante :
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
                                        participante.getReference(),
                                        reinicio
                                );
                            }

                            batch.commit()
                                    .addOnSuccessListener(
                                            unused ->
                                                    guardarPartidaEnSala(
                                                            salaRef,
                                                            partidaId,
                                                            preguntas
                                                    )
                                    )
                                    .addOnFailureListener(
                                            error ->
                                                    errorInicio(
                                                            error
                                                    )
                                    );
                        }
                )
                .addOnFailureListener(
                        this::errorInicio
                );
    }

    private void guardarPartidaEnSala(
            DocumentReference salaRef,
            String partidaId,
            List<Map<String, Object>> preguntas
    ) {

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
                preguntas.size()
        );

        datos.put(
                "partidaId",
                partidaId
        );

        datos.put(
                "preguntasPartida",
                preguntas
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

        salaRef
                .update(datos)
                .addOnFailureListener(
                        this::errorInicio
                );
    }

    private void errorInicio(
            Exception error
    ) {

        iniciandoPartida =
                false;

        actualizarBotonAnfitrion();

        Toast.makeText(
                this,
                error.getMessage() != null
                        ? error.getMessage()
                        : "No se pudo iniciar la partida.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void abrirPartida() {

        if (abriendoPartida) {
            return;
        }

        abriendoPartida =
                true;

        Intent intent =
                new Intent(
                        this,
                        QuizMultijugadorActivity.class
                );

        intent.putExtra(
                QuizMultijugadorActivity
                        .EXTRA_CODIGO_SALA,
                codigoSala
        );

        startActivity(
                intent
        );
    }

    private void copiarCodigo() {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        ClipData clip =
                ClipData.newPlainText(
                        "Código de sala",
                        codigoSala
                );

        clipboard.setPrimaryClip(
                clip
        );

        Toast.makeText(
                this,
                "Código copiado",
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dp(
            int valor
    ) {

        return Math.round(
                valor
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

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
    }
}

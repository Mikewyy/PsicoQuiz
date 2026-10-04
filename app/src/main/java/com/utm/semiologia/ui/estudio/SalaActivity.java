package com.utm.semiologia.ui.estudio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.utm.semiologia.R;
import com.utm.semiologia.ui.common.BaseActivity;

import java.util.Locale;

public class SalaActivity extends BaseActivity {

    // =========================================================
    // EXTRA
    // =========================================================

    public static final String EXTRA_CODIGO_SALA =
            "extra_codigo_sala";


    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseFirestore firestore;

    private FirebaseAuth auth;

    private ListenerRegistration listenerSala;

    private ListenerRegistration listenerParticipantes;


    // =========================================================
    // DATOS
    // =========================================================

    private String codigoSala;

    private String creadorUid;


    // =========================================================
    // VISTAS
    // =========================================================

    private TextView tvNombreSala;

    private TextView tvCodigoSala;

    private TextView tvCantidadParticipantes;

    private TextView tvEstadoSala;

    private LinearLayout contenedorParticipantes;

    private MaterialButton btnCopiarCodigo;

    private MaterialButton btnIniciarSesion;


    // =========================================================
    // CREACIÓN
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


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


    // =========================================================
    // ENLAZAR VISTAS
    // =========================================================

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


    // =========================================================
    // LISTENERS DE BOTONES
    // =========================================================

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
                v -> Toast.makeText(
                        this,
                        "La sesión sincronizada será el siguiente paso.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }


    // =========================================================
    // ESCUCHAR SALA EN TIEMPO REAL
    // =========================================================

    private void escucharSala() {

        tvEstadoSala.setText(
                "Conectando..."
        );


        listenerSala =
                firestore
                        .collection(
                                "salas"
                        )
                        .document(
                                codigoSala
                        )
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


                                    /*
                                     * Firestore puede entregar primero
                                     * un resultado desde la caché local.
                                     *
                                     * Si todavía no aparece el documento
                                     * pero viene de caché, NO cerramos
                                     * la pantalla porque todavía estamos
                                     * esperando al servidor.
                                     */
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


                                        /*
                                         * Aquí ya respondió el servidor
                                         * y confirmó que realmente no existe.
                                         */
                                        Toast.makeText(
                                                this,
                                                "La sala ya no existe.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        finish();

                                        return;
                                    }


                                    // Documento encontrado correctamente.
                                    actualizarDatosSala(
                                            snapshot
                                    );
                                }
                        );
    }
    // =========================================================
    // ACTUALIZAR INFORMACIÓN DE SALA
    // =========================================================

    private void actualizarDatosSala(
            DocumentSnapshot snapshot
    ) {

        String nombre =
                snapshot.getString(
                        "nombre"
                );


        creadorUid =
                snapshot.getString(
                        "creadorUid"
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


        tvEstadoSala.setText(
                "Sala activa"
        );


        actualizarBotonAnfitrion();
    }


    // =========================================================
    // ESCUCHAR PARTICIPANTES
    // =========================================================

    private void escucharParticipantes() {

        listenerParticipantes =
                firestore
                        .collection(
                                "salas"
                        )
                        .document(
                                codigoSala
                        )
                        .collection(
                                "participantes"
                        )
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


                                    int cantidad =
                                            snapshot.size();


                                    tvCantidadParticipantes.setText(
                                            cantidad
                                                    + (
                                                    cantidad == 1
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
                                }
                        );
    }


    // =========================================================
    // AGREGAR PARTICIPANTE VISUAL
    // =========================================================

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
                dp(
                        14
                );


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
                dp(
                        8
                )
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
                dp(
                        12
                ),
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


        contenedorParticipantes.addView(
                fila
        );
    }


    // =========================================================
    // ANFITRIÓN
    // =========================================================

    private void actualizarBotonAnfitrion() {

        if (
                auth.getCurrentUser() == null ||
                        creadorUid == null
        ) {

            btnIniciarSesion.setVisibility(
                    View.GONE
            );

            return;
        }


        boolean soyAnfitrion =
                creadorUid.equals(
                        auth
                                .getCurrentUser()
                                .getUid()
                );


        if (soyAnfitrion) {

            btnIniciarSesion.setVisibility(
                    View.VISIBLE
            );


            btnIniciarSesion.setText(
                    "Iniciar sesión de estudio"
            );

        } else {

            btnIniciarSesion.setVisibility(
                    View.GONE
            );
        }
    }


    // =========================================================
    // COPIAR CÓDIGO
    // =========================================================

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


    // =========================================================
    // DP
    // =========================================================

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


    // =========================================================
    // DESTRUIR LISTENERS
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();


        if (listenerSala != null) {

            listenerSala.remove();

            listenerSala = null;
        }


        if (listenerParticipantes != null) {

            listenerParticipantes.remove();

            listenerParticipantes = null;
        }
    }
}
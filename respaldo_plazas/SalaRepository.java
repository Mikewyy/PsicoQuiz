package com.utm.semiologia.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.utm.semiologia.firebase.model.ParticipanteSala;
import com.utm.semiologia.firebase.model.SalaEstudio;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class SalaRepository {

    private static final String COLECCION_SALAS = "salas";
    private static final String SUBCOLECCION_PARTICIPANTES = "participantes";
    private static final String SUBCOLECCION_PLAZAS = "plazas";
    private static final int CAPACIDAD_PLAZAS = 10;


    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_CODIGO = 6;

    private final FirebaseFirestore db;
    private final FirebaseAuthManager authManager;
    private final SecureRandom random;

    public SalaRepository() {
        db = FirebaseFirestore.getInstance();
        authManager = new FirebaseAuthManager();
        random = new SecureRandom();
    }

    // ============================================================
    // CREAR SALA
    // ============================================================

    public void crearSala(
            String nombreSala,
            String nombreUsuario,
            SalaCallback callback
    ) {
        crearSala(nombreSala, nombreUsuario, "avatar_01", callback);
    }

    public void crearSala(
            String nombreSala,
            String nombreUsuario,
            String avatarId,
            SalaCallback callback
    ) {

        final String avatarNormalizado = normalizarAvatarId(avatarId);

        authManager.iniciarSesionAnonima(
                new FirebaseAuthManager.AuthCallback() {

                    @Override
                    public void onSuccess(
                            com.google.firebase.auth.FirebaseUser usuario
                    ) {
                        intentarCrearSala(
                                nombreSala,
                                nombreUsuario,
                                avatarNormalizado,
                                usuario.getUid(),
                                callback,
                                0
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        callback.onError(
                                mensajeError(error)
                        );
                    }
                }
        );
    }

    private void intentarCrearSala(
            String nombreSala,
            String nombreUsuario,
            String avatarId,
            String uid,
            SalaCallback callback,
            int intento
    ) {

        if (intento >= 5) {
            callback.onError(
                    "No se pudo generar un código de sala. Inténtalo nuevamente."
            );
            return;
        }

        String codigo = generarCodigo();

        DocumentReference salaRef =
                db.collection(COLECCION_SALAS)
                        .document(codigo);

        salaRef.get()
                .addOnSuccessListener(documento -> {

                    if (documento.exists()) {

                        intentarCrearSala(
                                nombreSala,
                                nombreUsuario,
                                avatarId,
                                uid,
                                callback,
                                intento + 1
                        );

                        return;
                    }

                    SalaEstudio sala = new SalaEstudio(
                            codigo,
                            nombreSala,
                            uid
                    );

                    ParticipanteSala participante =
                            new ParticipanteSala(uid, nombreUsuario, true, avatarId);
                    sala.setMaxParticipantes(CAPACIDAD_PLAZAS);

                    db.runTransaction(transaction -> {

                                DocumentSnapshot comprobacion =
                                        transaction.get(salaRef);

                                if (comprobacion.exists()) {
                                    throw new IllegalStateException(
                                            "El código ya está siendo utilizado."
                                    );
                                }

                                transaction.set(
                                        salaRef,
                                        sala
                                );

                                DocumentReference participanteRef =
                                        salaRef
                                                .collection(
                                                        SUBCOLECCION_PARTICIPANTES
                                                )
                                                .document(uid);

                                Map<String, Object> datosHost = new HashMap<>();
                                datosHost.put("uid", uid);
                                datosHost.put("numero", "0");
                                Map<String, Object> fichaHost = new HashMap<>();
                                fichaHost.put("uid", uid);
                                fichaHost.put("nombre", nombreUsuario);
                                fichaHost.put("anfitrion", true);
                                fichaHost.put("avatarId", avatarId);
                                fichaHost.put("unidoEn", participante.getUnidoEn());
                                fichaHost.put("plaza", "0");
                                fichaHost.put("puntosPartida", 0);
                                fichaHost.put("estadoConexion", "activo");
                                transaction.set(participanteRef, fichaHost);
                                transaction.set(salaRef.collection(SUBCOLECCION_PLAZAS).document("0"), datosHost);

                                return null;

                            })
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(sala)
                            )
                            .addOnFailureListener(error -> {

                                // Es extremadamente improbable,
                                // pero otro usuario podría haber creado
                                // el mismo código entre ambas comprobaciones.
                                if (error instanceof IllegalStateException) {

                                    intentarCrearSala(
                                            nombreSala,
                                            nombreUsuario,
                                            avatarId,
                                            uid,
                                            callback,
                                            intento + 1
                                    );

                                } else {

                                    callback.onError(
                                            mensajeError(error)
                                    );
                                }
                            });
                })
                .addOnFailureListener(error ->
                        callback.onError(
                                mensajeError(error)
                        )
                );
    }

    // ============================================================
    // UNIRSE A UNA SALA
    // ============================================================

    public void unirseSala(
            String codigoIngresado,
            String nombreUsuario,
            SalaCallback callback
    ) {
        unirseSala(codigoIngresado, nombreUsuario, "avatar_01", callback);
    }

    public void unirseSala(
            String codigoIngresado,
            String nombreUsuario,
            String avatarId,
            SalaCallback callback
    ) {

        String codigo = normalizarCodigo(codigoIngresado);
        final String avatarNormalizado = normalizarAvatarId(avatarId);

        if (codigo.length() != LONGITUD_CODIGO) {
            callback.onError(
                    "El código debe tener 6 caracteres."
            );
            return;
        }

        authManager.iniciarSesionAnonima(
                new FirebaseAuthManager.AuthCallback() {

                    @Override
                    public void onSuccess(
                            com.google.firebase.auth.FirebaseUser usuario
                    ) {

                        ejecutarUnionSala(
                                codigo,
                                nombreUsuario,
                                avatarNormalizado,
                                usuario.getUid(),
                                callback
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        callback.onError(
                                mensajeError(error)
                        );
                    }
                }
        );
    }

    private void ejecutarUnionSala(String codigo, String nombreUsuario,
                                  String avatarId, String uid, SalaCallback callback) {
        // Las nuevas salas utilizan plazas. No se modifican las salas legadas.
        int inicio = random.nextInt(CAPACIDAD_PLAZAS);
        intentarReservarPlaza(codigo, nombreUsuario, avatarId, uid,
                callback, inicio, 0);
    }

    private void intentarReservarPlaza(String codigo, String nombreUsuario,
                                     String avatarId, String uid,
                                     SalaCallback callback, int inicio, int intento) {
        if (intento >= CAPACIDAD_PLAZAS) {
            callback.onError("La sala está llena o no se pudo reservar una plaza.");
            return;
        }
        String numero = String.valueOf((inicio + intento) % CAPACIDAD_PLAZAS);
        DocumentReference salaRef = db.collection(COLECCION_SALAS).document(codigo);
        DocumentReference participanteRef = salaRef.collection(SUBCOLECCION_PARTICIPANTES).document(uid);
        DocumentReference plazaRef = salaRef.collection(SUBCOLECCION_PLAZAS).document(numero);

        db.runTransaction((Transaction.Function<String>) transaction -> {
            // Todas las lecturas ocurren antes de las escrituras.
            DocumentSnapshot salaDoc = transaction.get(salaRef);
            DocumentSnapshot fichaDoc = transaction.get(participanteRef);
            DocumentSnapshot plazaDoc = transaction.get(plazaRef);
            if (!salaDoc.exists()) throw new IllegalStateException("La sala no existe.");
            SalaEstudio sala = salaDoc.toObject(SalaEstudio.class);
            if (sala == null) throw new IllegalStateException("No se pudo leer la sala.");
            if (sala.getMaxParticipantes() != CAPACIDAD_PLAZAS) {
                throw new IllegalStateException("Esta sala utiliza un formato anterior; crea una sala nueva.");
            }
            if (fichaDoc.exists()) {
                return "already"; // Evita duplicar la plaza del mismo UID.
            }
            if (!"esperando".equals(sala.getEstadoPartida()))
                throw new IllegalStateException("La partida ya comenzó.");
            if (plazaDoc.exists()) return "occupied";

            Map<String, Object> plaza = new HashMap<>();
            plaza.put("uid", uid);
            plaza.put("numero", numero);
            Map<String, Object> ficha = new HashMap<>();
            ficha.put("uid", uid);
            ficha.put("nombre", nombreUsuario);
            ficha.put("anfitrion", false);
            ficha.put("avatarId", avatarId);
            ficha.put("unidoEn", System.currentTimeMillis());
            ficha.put("plaza", numero);
            ficha.put("puntosPartida", 0);
            ficha.put("estadoConexion", "activo");
            transaction.set(plazaRef, plaza);
            transaction.set(participanteRef, ficha);
            // NO se actualiza cantidadParticipantes: evita el documento caliente.
            return "joined";
        }).addOnSuccessListener(resultado -> {
            if ("occupied".equals(resultado)) {
                intentarReservarPlaza(codigo, nombreUsuario, avatarId,
                        uid, callback, inicio, intento + 1);
            } else {
                obtenerSala(codigo, callback);
            }
        }).addOnFailureListener(error -> callback.onError(mensajeError(error)));
    }

    // ============================================================
    // OBTENER SALA
    // ============================================================

    public void obtenerSala(
            String codigoIngresado,
            SalaCallback callback
    ) {

        String codigo =
                normalizarCodigo(codigoIngresado);

        db.collection(COLECCION_SALAS)
                .document(codigo)
                .get()
                .addOnSuccessListener(documento -> {

                    if (!documento.exists()) {
                        callback.onError(
                                "La sala no existe."
                        );
                        return;
                    }

                    SalaEstudio sala =
                            documento.toObject(
                                    SalaEstudio.class
                            );

                    if (sala == null) {
                        callback.onError(
                                "No se pudo leer la sala."
                        );
                        return;
                    }

                    callback.onSuccess(sala);
                })
                .addOnFailureListener(error ->
                        callback.onError(
                                mensajeError(error)
                        )
                );
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private String generarCodigo() {

        StringBuilder codigo =
                new StringBuilder();

        for (int i = 0;
             i < LONGITUD_CODIGO;
             i++) {

            int posicion =
                    random.nextInt(
                            CARACTERES.length()
                    );

            codigo.append(
                    CARACTERES.charAt(posicion)
            );
        }

        return codigo.toString();
    }

    private String normalizarAvatarId(String avatarId) {
        if (avatarId == null) return "avatar_01";
        String value = avatarId.trim();
        if (!value.matches("avatar_0[1-6]")) return "avatar_01";
        return value;
    }

    private String normalizarCodigo(
            String codigo
    ) {

        if (codigo == null) {
            return "";
        }

        return codigo
                .trim()
                .toUpperCase();
    }

    @NonNull
    private String mensajeError(
            Exception error
    ) {

        if (error == null) {
            return "Ocurrió un error desconocido.";
        }

        if (error.getMessage() == null ||
                error.getMessage().trim().isEmpty()) {

            return "Ocurrió un error al comunicarse con Firebase.";
        }

        return error.getMessage();
    }

    // ============================================================
    // CALLBACK
    // ============================================================

    public interface SalaCallback {

        void onSuccess(
                SalaEstudio sala
        );

        void onError(
                String mensaje
        );
    }
}
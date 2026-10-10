package com.utm.semiologia.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.utm.semiologia.firebase.model.ParticipanteSala;
import com.utm.semiologia.firebase.model.SalaEstudio;

import java.security.SecureRandom;

public class SalaRepository {

    private static final String COLECCION_SALAS = "salas";
    private static final String SUBCOLECCION_PARTICIPANTES = "participantes";

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
                            new ParticipanteSala(
                                    uid,
                                    nombreUsuario,
                                    true,
                                    avatarId
                            );

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

                                transaction.set(
                                        participanteRef,
                                        participante
                                );

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

    private void ejecutarUnionSala(
            String codigo,
            String nombreUsuario,
            String avatarId,
            String uid,
            SalaCallback callback
    ) {

        DocumentReference salaRef =
                db.collection(COLECCION_SALAS)
                        .document(codigo);

        DocumentReference participanteRef =
                salaRef
                        .collection(SUBCOLECCION_PARTICIPANTES)
                        .document(uid);

        db.runTransaction(
                        (Transaction.Function<SalaEstudio>) transaction -> {

                            DocumentSnapshot salaSnapshot =
                                    transaction.get(salaRef);

                            if (!salaSnapshot.exists()) {
                                throw new IllegalStateException(
                                        "La sala no existe."
                                );
                            }

                            SalaEstudio sala =
                                    salaSnapshot.toObject(
                                            SalaEstudio.class
                                    );

                            if (sala == null) {
                                throw new IllegalStateException(
                                        "No se pudo leer la sala."
                                );
                            }

                            DocumentSnapshot participanteSnapshot =
                                    transaction.get(participanteRef);

                            // Ya pertenece a la sala.
                            // No volvemos a incrementar el contador.
                            if (participanteSnapshot.exists()) {
                                transaction.update(
                                        participanteRef,
                                        "nombre", nombreUsuario,
                                        "avatarId", avatarId
                                );
                                return sala;
                            }

                            int cantidad =
                                    sala.getCantidadParticipantes();

                            int maximo =
                                    sala.getMaxParticipantes();

                            if (cantidad >= maximo) {
                                throw new IllegalStateException(
                                        "La sala está llena."
                                );
                            }

                            ParticipanteSala participante =
                                    new ParticipanteSala(
                                            uid,
                                            nombreUsuario,
                                            false,
                                            avatarId
                                    );

                            transaction.set(
                                    participanteRef,
                                    participante
                            );

                            transaction.update(
                                    salaRef,
                                    "cantidadParticipantes",
                                    cantidad + 1
                            );

                            sala.setCantidadParticipantes(
                                    cantidad + 1
                            );

                            return sala;

                        })
                .addOnSuccessListener(
                        callback::onSuccess
                )
                .addOnFailureListener(error ->
                        callback.onError(
                                mensajeError(error)
                        )
                );
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
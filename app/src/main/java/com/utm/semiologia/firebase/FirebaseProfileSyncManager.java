package com.utm.semiologia.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.Mascota;
import com.utm.semiologia.data.model.Usuario;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Sincronización del perfil base de PsicoQuiz con Cloud Firestore.
 *
 * Fase 1C:
 * - Sube perfil + mascota en una sola escritura.
 * - Descarga perfil + mascota para reconstruir SQLite en otro dispositivo.
 * - Usa Firebase UID como identidad portable.
 * - Nunca sube password_hash, password_salt ni el ID SQLite local.
 *
 * El progreso de estudio, inventarios, niveles, notas y Pomodoro se añadirá
 * en las fases siguientes.
 */
public class FirebaseProfileSyncManager {

    private static final String TAG = "PsicoQuizSync";
    private static final String COLECCION_USUARIOS = "usuarios";
    private static final int SCHEMA_VERSION = 4;
    private static final long DEBOUNCE_MS = 1800L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<Long, Runnable> PENDIENTES = new ConcurrentHashMap<>();

    private final FirebaseFirestore db;

    public FirebaseProfileSyncManager() {
        db = FirebaseFirestore.getInstance();
    }

    /** Fase 4: sincroniza perfil + mascota leyendo el estado SQLite actual. */
    public static void programarSubidaDesdeSQLite(@NonNull Context context, long usuarioId) {
        if (usuarioId <= 0 || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        Runnable anterior = PENDIENTES.remove(usuarioId);
        if (anterior != null) HANDLER.removeCallbacks(anterior);
        Context app = context.getApplicationContext();
        Runnable nuevo = () -> {
            PENDIENTES.remove(usuarioId);
            subirDesdeSQLiteAhora(app, usuarioId);
        };
        PENDIENTES.put(usuarioId, nuevo);
        HANDLER.postDelayed(nuevo, DEBOUNCE_MS);
    }

    public static void forzarSubidaDesdeSQLite(@NonNull Context context, long usuarioId) {
        Runnable anterior = PENDIENTES.remove(usuarioId);
        if (anterior != null) HANDLER.removeCallbacks(anterior);
        subirDesdeSQLiteAhora(context.getApplicationContext(), usuarioId);
    }

    private static void subirDesdeSQLiteAhora(@NonNull Context context, long usuarioId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || usuarioId <= 0) return;
        Repositorio repo = Repositorio.get(context);
        Usuario usuario = repo.usuarios().buscarPorId(usuarioId);
        if (usuario == null) return;
        Mascota mascota = repo.mascotas().obtener(usuarioId);
        new FirebaseProfileSyncManager().subirPerfilLocal(user, usuario, mascota, null);
    }

    /**
     * Crea o actualiza usuarios/{uid} usando merge.
     * Perfil y mascota viajan juntos para consumir una sola escritura.
     */
    public void subirPerfilLocal(
            @NonNull FirebaseUser usuarioFirebase,
            @NonNull Usuario usuarioLocal,
            @Nullable Mascota mascota,
            @Nullable SyncCallback callback
    ) {
        String uid = usuarioFirebase.getUid();

        if (uid.trim().isEmpty()) {
            enviarError(callback, new IllegalArgumentException("Firebase no devolvió un UID válido."));
            return;
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("schemaVersion", SCHEMA_VERSION);
        datos.put("uid", uid);
        datos.put("email", usuarioFirebase.getEmail() != null
                ? usuarioFirebase.getEmail()
                : usuarioLocal.getEmail());
        datos.put("nombre", usuarioLocal.getNombre());
        datos.put("nombreMostrado", usuarioLocal.getNombreMostrado());
        datos.put("avatar", usuarioLocal.getAvatar());
        datos.put("puntos", usuarioLocal.getPuntos());
        datos.put("nivel", usuarioLocal.getNivel());
        datos.put("experiencia", usuarioLocal.getExperiencia());
        datos.put("rachaActual", usuarioLocal.getRachaActual());
        datos.put("rachaMaxima", usuarioLocal.getRachaMaxima());
        datos.put("ultimaActividadFecha", usuarioLocal.getUltimaActividadFecha());
        datos.put("creadoEn", usuarioLocal.getCreadoEn());
        datos.put("actualizadoEn", FieldValue.serverTimestamp());

        if (mascota != null) {
            Map<String, Object> datosMascota = new HashMap<>();
            datosMascota.put("nombre", mascota.getNombre());
            datosMascota.put("especie", mascota.getEspecie());
            datosMascota.put("skinId", mascota.getSkinId());
            datosMascota.put("hambre", mascota.getHambre());
            datosMascota.put("felicidad", mascota.getFelicidad());
            datosMascota.put("energia", mascota.getEnergia());
            datosMascota.put("estado", mascota.getEstado());
            datosMascota.put("accesorioEquipadoId", mascota.getAccesorioEquipadoId());
            datosMascota.put("hambreActualizadaEn", mascota.getHambreActualizadaEn());
            datosMascota.put("creadoEn", mascota.getCreadoEn());
            datos.put("mascota", datosMascota);
        }

        db.collection(COLECCION_USUARIOS)
                .document(uid)
                .set(datos, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Perfil sincronizado en Firestore. UID: " + uid);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "No se pudo sincronizar el perfil en Firestore. UID: " + uid, error);
                    enviarError(callback, error);
                });
    }

    /**
     * Descarga usuarios/{uid} y lo convierte en modelos locales sin IDs SQLite.
     * El llamador asigna el nuevo usuario_id de la mascota al insertar en SQLite.
     */
    public void descargarPerfil(
            @NonNull FirebaseUser usuarioFirebase,
            @NonNull DownloadCallback callback
    ) {
        String uid = usuarioFirebase.getUid();

        if (uid.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Firebase no devolvió un UID válido."));
            return;
        }

        db.collection(COLECCION_USUARIOS)
                .document(uid)
                .get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()) {
                        Log.w(TAG, "No existe perfil Firestore para UID: " + uid);
                        callback.onNotFound();
                        return;
                    }

                    try {
                        Usuario usuario = mapearUsuario(documento, usuarioFirebase);
                        Mascota mascota = mapearMascota(documento);
                        Log.d(TAG, "Perfil descargado desde Firestore. UID: " + uid);
                        callback.onSuccess(usuario, mascota);
                    } catch (Exception error) {
                        Log.e(TAG, "El perfil Firestore no pudo convertirse a SQLite. UID: " + uid, error);
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "No se pudo descargar el perfil Firestore. UID: " + uid, error);
                    callback.onError(error);
                });
    }

    private Usuario mapearUsuario(
            @NonNull DocumentSnapshot documento,
            @NonNull FirebaseUser usuarioFirebase
    ) {
        Usuario u = new Usuario();

        String email = usuarioFirebase.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = texto(documento.get("email"), "");
        }

        String nombre = texto(documento.get("nombre"), "Usuario");
        String nombreMostrado = textoNullable(documento.get("nombreMostrado"));
        String avatar = texto(documento.get("avatar"), "avatar_01");

        u.setNombre(nombre);
        u.setNombreMostrado(nombreMostrado);
        u.setEmail(email != null ? email.trim().toLowerCase() : "");
        u.setAvatar(avatar);
        u.setPuntos(entero(documento.get("puntos"), 0, 0, Integer.MAX_VALUE));
        u.setNivel(entero(documento.get("nivel"), 1, 1, 999));
        u.setExperiencia(entero(documento.get("experiencia"), 0, 0, Integer.MAX_VALUE));
        u.setRachaActual(entero(documento.get("rachaActual"), 0, 0, Integer.MAX_VALUE));
        u.setRachaMaxima(entero(documento.get("rachaMaxima"), 0, 0, Integer.MAX_VALUE));
        u.setUltimaActividadFecha(textoNullable(documento.get("ultimaActividadFecha")));
        u.setGrupoId(null); // Los grupos locales no se restauran en esta fase.
        u.setCreadoEn(largo(documento.get("creadoEn"), System.currentTimeMillis()));

        return u;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Mascota mapearMascota(@NonNull DocumentSnapshot documento) {
        Object bruto = documento.get("mascota");
        if (!(bruto instanceof Map)) {
            return null;
        }

        Map<String, Object> mapa = (Map<String, Object>) bruto;
        Mascota m = new Mascota();

        m.setNombre(texto(mapa.get("nombre"), "Mateo"));
        m.setEspecie(texto(mapa.get("especie"), "gato"));
        m.setSkinId(entero(mapa.get("skinId"), 0, 0, 5));
        m.setHambre(entero(mapa.get("hambre"), 100, 0, 100));
        m.setFelicidad(entero(mapa.get("felicidad"), 80, 0, 100));
        m.setEnergia(entero(mapa.get("energia"), 100, 0, 100));
        m.setEstado(texto(mapa.get("estado"), Mascota.ESTADO_FELIZ));

        Object accesorio = mapa.get("accesorioEquipadoId");
        if (accesorio instanceof Number) {
            m.setAccesorioEquipadoId(((Number) accesorio).longValue());
        } else {
            m.setAccesorioEquipadoId(null);
        }

        long ahora = System.currentTimeMillis();
        m.setHambreActualizadaEn(largo(mapa.get("hambreActualizadaEn"), ahora));
        m.setCreadoEn(largo(mapa.get("creadoEn"), ahora));

        return m;
    }

    @NonNull
    private String texto(@Nullable Object valor, @NonNull String defecto) {
        if (valor == null) return defecto;
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? defecto : texto;
    }

    @Nullable
    private String textoNullable(@Nullable Object valor) {
        if (valor == null) return null;
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? null : texto;
    }

    private int entero(@Nullable Object valor, int defecto, int minimo, int maximo) {
        long numero = largo(valor, defecto);
        if (numero < minimo) return minimo;
        if (numero > maximo) return maximo;
        return (int) numero;
    }

    private long largo(@Nullable Object valor, long defecto) {
        return valor instanceof Number ? ((Number) valor).longValue() : defecto;
    }

    private void enviarError(@Nullable SyncCallback callback, @Nullable Exception error) {
        if (callback == null) return;
        callback.onError(error != null
                ? error
                : new IllegalStateException("Error desconocido durante la sincronización."));
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public interface DownloadCallback {
        void onSuccess(@NonNull Usuario usuario, @Nullable Mascota mascota);
        void onNotFound();
        void onError(Exception error);
    }
}

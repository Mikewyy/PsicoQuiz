package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.util.FechaUtil;
import com.utm.semiologia.util.Gamificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD + lógica de rachas sobre la tabla usuarios.
 */
public class UsuarioDao {

    private final DatabaseHelper helper;

    public UsuarioDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    // ------------------------------------------------------------------
    // LECTURAS
    // ------------------------------------------------------------------

    @Nullable
    public Usuario buscarPorId(long id) {

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor c = db.query(
                DatabaseHelper.T_USUARIOS,
                null,
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null,
                "1"
        )) {

            return c.moveToFirst()
                    ? mapear(c)
                    : null;
        }
    }

    @Nullable
    public Usuario buscarPorEmail(String email) {

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor c = db.query(
                DatabaseHelper.T_USUARIOS,
                null,
                "email = ?",
                new String[]{email},
                null,
                null,
                null,
                "1"
        )) {

            return c.moveToFirst()
                    ? mapear(c)
                    : null;
        }
    }

    public List<Usuario> rankingGlobal(int limite) {

        SQLiteDatabase db = helper.getReadableDatabase();

        List<Usuario> lista = new ArrayList<>();

        try (Cursor c = db.query(
                DatabaseHelper.T_USUARIOS,
                null,
                null,
                null,
                null,
                null,
                "puntos DESC",
                String.valueOf(limite)
        )) {

            while (c.moveToNext()) {
                lista.add(mapear(c));
            }
        }

        return lista;
    }

    /**
     * Top de un grupo por puntos semanales.
     */
    public List<Usuario> rankingGrupo(long grupoId, int limite) {

        SQLiteDatabase db = helper.getReadableDatabase();

        List<Usuario> lista = new ArrayList<>();

        String sql =
                "SELECT u.* FROM " + DatabaseHelper.T_USUARIOS + " u " +
                        "JOIN " + DatabaseHelper.T_GRUPO_MIEMBROS + " m " +
                        "ON m.usuario_id = u.id " +
                        "WHERE m.grupo_id = ? " +
                        "ORDER BY m.puntos_semanales DESC " +
                        "LIMIT ?";

        try (Cursor c = db.rawQuery(
                sql,
                new String[]{
                        String.valueOf(grupoId),
                        String.valueOf(limite)
                }
        )) {

            while (c.moveToNext()) {
                lista.add(mapear(c));
            }
        }

        return lista;
    }

    // ------------------------------------------------------------------
    // ESCRITURAS
    // ------------------------------------------------------------------

    /**
     * Inserta un nuevo usuario.
     *
     * @return id del nuevo usuario o -1 si el email ya existe.
     */
    public long insertar(Usuario u) {

        if (buscarPorEmail(u.getEmail()) != null) {
            return -1L;
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        return db.insertOrThrow(
                DatabaseHelper.T_USUARIOS,
                null,
                toValues(u)
        );
    }

    /**
     * Actualiza todos los datos del usuario.
     */
    public void actualizar(Usuario u) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.update(
                DatabaseHelper.T_USUARIOS,
                toValues(u),
                "id = ?",
                new String[]{String.valueOf(u.getId())}
        );
    }

    /**
     * Actualiza puntos, nivel y racha.
     */
    public void actualizarPuntosYRacha(
            long usuarioId,
            int puntos,
            int nivel,
            int racha
    ) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put("puntos", puntos);
        cv.put("nivel", nivel);
        cv.put("racha_actual", racha);

        db.update(
                DatabaseHelper.T_USUARIOS,
                cv,
                "id = ?",
                new String[]{String.valueOf(usuarioId)}
        );
    }

    /**
     * Cambia el avatar del usuario.
     *
     * Ejemplo:
     * avatar_01
     * avatar_02
     * avatar_03
     * ...
     */
    public void actualizarAvatar(long usuarioId, String avatar) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put("avatar", avatar);

        db.update(
                DatabaseHelper.T_USUARIOS,
                cv,
                "id = ?",
                new String[]{String.valueOf(usuarioId)}
        );
    }

    /**
     * Guarda el alias que el usuario quiere ver en la app. Si es null o
     * viene vacío, la app vuelve a mostrar el nombre real del registro.
     */
    public void actualizarNombreMostrado(long usuarioId, String nombreMostrado) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        if (nombreMostrado == null || nombreMostrado.trim().isEmpty()) {
            cv.putNull("nombre_mostrado");
        } else {
            cv.put("nombre_mostrado", nombreMostrado.trim());
        }

        db.update(
                DatabaseHelper.T_USUARIOS,
                cv,
                "id = ?",
                new String[]{String.valueOf(usuarioId)}
        );
    }

    /**
     * Suma puntos semanales dentro de un grupo.
     */
    public void sumarPuntosSemanales(
            long grupoId,
            long usuarioId,
            int puntos
    ) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL(
                "UPDATE " + DatabaseHelper.T_GRUPO_MIEMBROS +
                        " SET puntos_semanales = puntos_semanales + ? " +
                        "WHERE grupo_id = ? AND usuario_id = ?",
                new Object[]{
                        puntos,
                        grupoId,
                        usuarioId
                }
        );
    }

    // ------------------------------------------------------------------
    // ACTIVIDAD Y RACHA
    // ------------------------------------------------------------------

    /**
     * Registra actividad de estudio de HOY en una sola transacción:
     *
     * 1. Actualiza actividad_diaria.
     * 2. Recalcula la racha.
     * 3. Actualiza la racha máxima.
     * 4. Puede entregar comida como recompensa.
     *
     * @return nueva racha actual.
     */
    public int registrarActividad(
            long usuarioId,
            int seccionesLeidas,
            int minutosPomodoro,
            int puntos,
            MascotaDao mascotaDao
    ) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {

            String hoy = FechaUtil.hoy();

            // ----------------------------------------------------------
            // 1. REGISTRAR ACTIVIDAD DIARIA
            // ----------------------------------------------------------

            db.execSQL(
                    "INSERT INTO " + DatabaseHelper.T_ACTIVIDAD +
                            " (usuario_id, fecha, secciones_leidas, " +
                            "minutos_pomodoro, puntos) " +
                            "VALUES (?,?,?,?,?) " +

                            "ON CONFLICT(usuario_id, fecha) DO UPDATE SET " +

                            "secciones_leidas = " +
                            "secciones_leidas + excluded.secciones_leidas, " +

                            "minutos_pomodoro = " +
                            "minutos_pomodoro + excluded.minutos_pomodoro, " +

                            "puntos = puntos + excluded.puntos",

                    new Object[]{
                            usuarioId,
                            hoy,
                            seccionesLeidas,
                            minutosPomodoro,
                            puntos
                    }
            );

            // ----------------------------------------------------------
            // 2. OBTENER USUARIO ACTUAL
            // ----------------------------------------------------------

            Usuario u = buscarPorId(usuarioId);

            int rachaAnterior =
                    u != null
                            ? u.getRachaActual()
                            : 0;

            String ultimaFecha =
                    u != null
                            ? u.getUltimaActividadFecha()
                            : null;

            // ----------------------------------------------------------
            // 3. CALCULAR NUEVA RACHA
            // ----------------------------------------------------------

            int nuevaRacha =
                    Gamificacion.calcularRacha(
                            rachaAnterior,
                            ultimaFecha
                    );

            // ----------------------------------------------------------
            // 4. RECOMPENSA POR AUMENTO REAL DE RACHA
            // ----------------------------------------------------------

            if (nuevaRacha > rachaAnterior && mascotaDao != null) {

                int comidaPremio =
                        Gamificacion.calcularComidaRacha(
                                nuevaRacha
                        );

                mascotaDao.otorgarAlimento(
                        usuarioId,
                        Gamificacion.ALIMENTO_GALLETA_ID,
                        comidaPremio
                );
            }

            // ----------------------------------------------------------
            // 5. ACTUALIZAR RACHA DEL USUARIO
            // ----------------------------------------------------------

            ContentValues cv = new ContentValues();

            cv.put(
                    "racha_actual",
                    nuevaRacha
            );

            cv.put(
                    "ultima_actividad_fecha",
                    hoy
            );

            if (u != null &&
                    nuevaRacha > u.getRachaMaxima()) {

                cv.put(
                        "racha_maxima",
                        nuevaRacha
                );
            }

            db.update(
                    DatabaseHelper.T_USUARIOS,
                    cv,
                    "id = ?",
                    new String[]{
                            String.valueOf(usuarioId)
                    }
            );

            db.setTransactionSuccessful();

            return nuevaRacha;

        } finally {

            db.endTransaction();
        }
    }

    /**
     * Devuelve la cantidad de días de la racha actual.
     */
    public int diasDeRachaViva(long usuarioId) {

        Usuario u = buscarPorId(usuarioId);

        if (u == null ||
                u.getUltimaActividadFecha() == null) {

            return 0;
        }

        return u.getRachaActual();
    }

    // ------------------------------------------------------------------
    // MAPEO SQLITE -> USUARIO
    // ------------------------------------------------------------------

    /**
     * Convierte una fila de SQLite en un objeto Usuario.
     */
    private Usuario mapear(Cursor c) {

        Usuario u = new Usuario();

        // ID
        u.setId(
                c.getLong(
                        c.getColumnIndexOrThrow("id")
                )
        );

        // Datos personales
        u.setNombre(
                c.getString(
                        c.getColumnIndexOrThrow("nombre")
                )
        );

        u.setEmail(
                c.getString(
                        c.getColumnIndexOrThrow("email")
                )
        );

        /*
         * Alias visible: se lee con getColumnIndex (y no getColumnIndexOrThrow)
         * para no romper si alguna consulta futura trae un subconjunto de
         * columnas. Si no está, el usuario ve su nombre real.
         */
        int idxMostrado =
                c.getColumnIndex("nombre_mostrado");

        if (idxMostrado >= 0) {
            u.setNombreMostrado(
                    c.getString(idxMostrado)
            );
        }

        // Seguridad
        u.setPasswordHash(
                c.getString(
                        c.getColumnIndexOrThrow("password_hash")
                )
        );

        u.setPasswordSalt(
                c.getString(
                        c.getColumnIndexOrThrow("password_salt")
                )
        );

        // Gamificación
        u.setPuntos(
                c.getInt(
                        c.getColumnIndexOrThrow("puntos")
                )
        );

        u.setNivel(
                c.getInt(
                        c.getColumnIndexOrThrow("nivel")
                )
        );

        u.setExperiencia(
                c.getInt(
                        c.getColumnIndexOrThrow("experiencia")
                )
        );

        // Racha
        u.setRachaActual(
                c.getInt(
                        c.getColumnIndexOrThrow("racha_actual")
                )
        );

        u.setRachaMaxima(
                c.getInt(
                        c.getColumnIndexOrThrow("racha_maxima")
                )
        );

        u.setUltimaActividadFecha(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "ultima_actividad_fecha"
                        )
                )
        );

        // Grupo
        int grupoIndex =
                c.getColumnIndexOrThrow("grupo_id");

        u.setGrupoId(
                c.isNull(grupoIndex)
                        ? null
                        : c.getLong(grupoIndex)
        );

        // --------------------------------------------------------------
        // AVATAR
        // --------------------------------------------------------------

        int avatarIndex = c.getColumnIndex("avatar");

        if (avatarIndex >= 0 && !c.isNull(avatarIndex)) {

            String avatar =
                    c.getString(avatarIndex);

            if (avatar == null ||
                    avatar.trim().isEmpty()) {

                avatar = "avatar_01";
            }

            u.setAvatar(avatar);

        } else {

            // Avatar predeterminado
            u.setAvatar("avatar_01");
        }

        // Fecha de creación
        u.setCreadoEn(
                c.getLong(
                        c.getColumnIndexOrThrow("creado_en")
                )
        );

        return u;
    }

    // ------------------------------------------------------------------
    // USUARIO -> CONTENT VALUES
    // ------------------------------------------------------------------

    /**
     * Convierte Usuario en ContentValues para SQLite.
     */
    private ContentValues toValues(Usuario u) {

        ContentValues cv = new ContentValues();

        // Datos personales
        cv.put(
                "nombre",
                u.getNombre()
        );

        cv.put(
                "nombre_mostrado",
                u.getNombreMostrado()
        );

        cv.put(
                "email",
                u.getEmail()
        );

        // Seguridad
        cv.put(
                "password_hash",
                u.getPasswordHash()
        );

        cv.put(
                "password_salt",
                u.getPasswordSalt()
        );

        // Gamificación
        cv.put(
                "puntos",
                u.getPuntos()
        );

        cv.put(
                "nivel",
                u.getNivel()
        );

        cv.put(
                "experiencia",
                u.getExperiencia()
        );

        // Rachas
        cv.put(
                "racha_actual",
                u.getRachaActual()
        );

        cv.put(
                "racha_maxima",
                u.getRachaMaxima()
        );

        cv.put(
                "ultima_actividad_fecha",
                u.getUltimaActividadFecha()
        );

        // Grupo
        if (u.getGrupoId() != null) {

            cv.put(
                    "grupo_id",
                    u.getGrupoId()
            );

        } else {

            cv.putNull("grupo_id");
        }

        // --------------------------------------------------------------
        // AVATAR
        // --------------------------------------------------------------

        String avatar = u.getAvatar();

        if (avatar == null ||
                avatar.trim().isEmpty()) {

            avatar = "avatar_01";
        }

        cv.put(
                "avatar",
                avatar
        );

        // Creación
        cv.put(
                "creado_en",
                u.getCreadoEn()
        );

        return cv;
    }
}
package com.utm.semiologia.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Nota;
import com.utm.semiologia.data.model.ProgresoSeccion;
import com.utm.semiologia.util.Gamificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a:
 *
 * - Notas de estudio.
 * - Progreso de lectura.
 * - Secciones de la guía.
 * - Caminos de Síntomas y Síndromes.
 * - Estadísticas de estudio.
 */
public class EstudioDao {

    public static final String CAMINO_SINTOMAS = "sintomas";
    public static final String CAMINO_SINDROMES = "sindromes";

    private final DatabaseHelper helper;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EstudioDao(DatabaseHelper helper) {
        this.helper = helper;
    }


    // =========================================================
    // NOTAS
    // =========================================================

    public long insertarNota(Nota n) {

        SQLiteDatabase db =
                helper.getWritableDatabase();

        ContentValues cv =
                new ContentValues();

        cv.put(
                "usuario_id",
                n.getUsuarioId()
        );

        if (n.getSeccionId() != null) {

            cv.put(
                    "seccion_id",
                    n.getSeccionId()
            );
        }

        cv.put(
                "texto_seleccionado",
                n.getTextoSeleccionado()
        );

        cv.put(
                "comentario",
                n.getComentario()
        );

        cv.put(
                "color",
                n.getColor()
        );

        cv.put(
                "creada_en",
                n.getCreadaEn()
        );

        cv.put(
                "sincronizada",
                0
        );

        return db.insertOrThrow(
                DatabaseHelper.T_NOTAS,
                null,
                cv
        );
    }


    public void actualizarNota(
            long notaId,
            String comentario,
            String color
    ) {

        SQLiteDatabase db =
                helper.getWritableDatabase();

        ContentValues cv =
                new ContentValues();

        cv.put(
                "comentario",
                comentario
        );

        cv.put(
                "color",
                color
        );

        cv.put(
                "sincronizada",
                0
        );

        db.update(
                DatabaseHelper.T_NOTAS,
                cv,
                "id = ?",
                new String[]{
                        String.valueOf(notaId)
                }
        );
    }


    public void eliminarNota(
            long notaId,
            long usuarioId
    ) {

        SQLiteDatabase db =
                helper.getWritableDatabase();

        db.delete(
                DatabaseHelper.T_NOTAS,
                "id = ? AND usuario_id = ?",
                new String[]{
                        String.valueOf(notaId),
                        String.valueOf(usuarioId)
                }
        );
    }


    public List<Nota> listarNotas(
            long usuarioId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        List<Nota> lista =
                new ArrayList<>();

        try (
                Cursor c = db.query(
                        DatabaseHelper.T_NOTAS,
                        null,
                        "usuario_id = ?",
                        new String[]{
                                String.valueOf(usuarioId)
                        },
                        null,
                        null,
                        "creada_en DESC"
                )
        ) {

            while (c.moveToNext()) {

                lista.add(
                        mapearNota(c)
                );
            }
        }

        return lista;
    }


    /**
     * Notas pertenecientes a una sección concreta.
     */
    public List<Nota> listarNotasDeSeccion(
            long usuarioId,
            long seccionId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        List<Nota> lista =
                new ArrayList<>();

        try (
                Cursor c = db.query(
                        DatabaseHelper.T_NOTAS,
                        null,
                        "usuario_id = ? AND seccion_id = ?",
                        new String[]{
                                String.valueOf(usuarioId),
                                String.valueOf(seccionId)
                        },
                        null,
                        null,
                        "creada_en DESC"
                )
        ) {

            while (c.moveToNext()) {

                lista.add(
                        mapearNota(c)
                );
            }
        }

        return lista;
    }


    /**
     * Marca varias notas como sincronizadas.
     */
    public void marcarNotasSincronizadas(
            List<Long> ids
    ) {

        if (ids == null || ids.isEmpty()) {
            return;
        }

        StringBuilder sb =
                new StringBuilder();

        for (int i = 0; i < ids.size(); i++) {

            if (i > 0) {
                sb.append(",");
            }

            sb.append(
                    ids.get(i)
            );
        }

        SQLiteDatabase db =
                helper.getWritableDatabase();

        db.execSQL(
                "UPDATE " +
                        DatabaseHelper.T_NOTAS +
                        " SET sincronizada = 1 " +
                        "WHERE id IN (" +
                        sb +
                        ")"
        );
    }


    // =========================================================
    // PROGRESO DE SECCIONES
    // =========================================================

    /**
     * Obtiene el progreso de una sección.
     *
     * Si todavía no existe, crea automáticamente
     * una fila de progreso para el usuario.
     */
    public ProgresoSeccion obtenerProgreso(
            long usuarioId,
            long seccionId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        try (
                Cursor c = db.query(
                        DatabaseHelper.T_PROGRESO,
                        null,
                        "usuario_id = ? AND seccion_id = ?",
                        new String[]{
                                String.valueOf(usuarioId),
                                String.valueOf(seccionId)
                        },
                        null,
                        null,
                        null,
                        "1"
                )
        ) {

            if (c.moveToFirst()) {

                return mapearProgreso(c);
            }
        }


        // No existe progreso todavía.
        ProgresoSeccion nuevo =
                ProgresoSeccion.iniciar(
                        usuarioId,
                        seccionId
                );

        insertarProgreso(
                nuevo
        );

        return nuevo;
    }


    /**
     * Inserta progreso de una sección.
     */
    public long insertarProgreso(
            ProgresoSeccion p
    ) {

        SQLiteDatabase db =
                helper.getWritableDatabase();

        ContentValues cv =
                new ContentValues();

        cv.put(
                "usuario_id",
                p.getUsuarioId()
        );

        cv.put(
                "seccion_id",
                p.getSeccionId()
        );

        cv.put(
                "lectura",
                p.getLectura()
        );

        cv.put(
                "completada",
                p.isCompletada()
                        ? 1
                        : 0
        );

        cv.put(
                "puntos_otorgados",
                p.getPuntosOtorgados()
        );

        cv.put(
                "comida_otorgada",
                p.getComidaOtorgada()
        );

        cv.put(
                "iniciada_en",
                p.getIniciadaEn()
        );

        if (p.getCompletadaEn() != null) {

            cv.put(
                    "completada_en",
                    p.getCompletadaEn()
            );
        }

        return db.insertOrThrow(
                DatabaseHelper.T_PROGRESO,
                null,
                cv
        );
    }


    /**
     * Actualiza el progreso.
     *
     * IMPORTANTE:
     *
     * La comida solamente se entrega cuando la sección
     * pasa de NO completada a COMPLETADA.
     *
     * De esta manera volver a abrir una sección terminada
     * no permite conseguir comida infinitamente.
     */
    public void actualizarProgreso(
            ProgresoSeccion p,
            MascotaDao mascotaDao,
            long usuarioId
    ) {

        SQLiteDatabase db =
                helper.getWritableDatabase();

        if (p.isCompletada()) {

            ProgresoSeccion actual =
                    obtenerProgreso(
                            usuarioId,
                            p.getSeccionId()
                    );

            if (
                    actual != null &&
                            !actual.isCompletada()
            ) {

                mascotaDao.otorgarAlimento(
                        usuarioId,
                        Gamificacion.ALIMENTO_GALLETA_ID,
                        Gamificacion.COMIDA_POR_SECCION_LEIDA
                );

                p.setComidaOtorgada(
                        Gamificacion.COMIDA_POR_SECCION_LEIDA
                );
            }
        }

        ContentValues cv =
                new ContentValues();

        cv.put(
                "lectura",
                p.getLectura()
        );

        cv.put(
                "completada",
                p.isCompletada()
                        ? 1
                        : 0
        );

        cv.put(
                "puntos_otorgados",
                p.getPuntosOtorgados()
        );

        cv.put(
                "comida_otorgada",
                p.getComidaOtorgada()
        );

        if (p.getCompletadaEn() != null) {

            cv.put(
                    "completada_en",
                    p.getCompletadaEn()
            );
        }

        db.update(
                DatabaseHelper.T_PROGRESO,
                cv,
                "usuario_id = ? AND seccion_id = ?",
                new String[]{
                        String.valueOf(
                                p.getUsuarioId()
                        ),
                        String.valueOf(
                                p.getSeccionId()
                        )
                }
        );
    }


    // =========================================================
    // GUÍA DE ESTUDIO
    // =========================================================

    /**
     * Método de compatibilidad.
     *
     * Devuelve TODAS las secciones de ambos caminos.
     *
     * Lo conservamos porque otras partes de la aplicación,
     * como el Dashboard, todavía pueden utilizarlo.
     */
    public List<ProgresoSeccion> listarSeccionesConProgreso(
            long usuarioId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        List<ProgresoSeccion> lista =
                new ArrayList<>();

        String sql =
                "SELECT " +

                        "s.id AS seccion_id, " +
                        "s.camino, " +
                        "s.titulo, " +
                        "s.tema, " +
                        "s.contenido, " +
                        "s.duracion_estimada_min, " +
                        "s.puntos_recompensa, " +

                        "COALESCE(p.lectura, 0) AS lectura, " +
                        "COALESCE(p.completada, 0) AS completada, " +
                        "COALESCE(p.puntos_otorgados, 0) AS puntos_otorgados, " +
                        "COALESCE(p.comida_otorgada, 0) AS comida_otorgada, " +

                        "p.id AS progreso_id, " +
                        "p.iniciada_en, " +
                        "p.completada_en " +

                        "FROM " +
                        DatabaseHelper.T_SECCIONES +
                        " s " +

                        "LEFT JOIN " +
                        DatabaseHelper.T_PROGRESO +
                        " p " +

                        "ON p.seccion_id = s.id " +
                        "AND p.usuario_id = ? " +

                        "ORDER BY " +
                        "CASE s.camino " +
                        "WHEN 'sintomas' THEN 1 " +
                        "WHEN 'sindromes' THEN 2 " +
                        "ELSE 3 END, " +
                        "s.orden ASC";

        try (
                Cursor c = db.rawQuery(
                        sql,
                        new String[]{
                                String.valueOf(
                                        usuarioId
                                )
                        }
                )
        ) {

            while (c.moveToNext()) {

                lista.add(
                        mapearSeccionConProgreso(
                                c,
                                usuarioId
                        )
                );
            }
        }

        return lista;
    }


    /**
     * NUEVO MÉTODO.
     *
     * Devuelve únicamente las secciones pertenecientes
     * al camino solicitado.
     *
     * Ejemplos:
     *
     * listarSeccionesConProgreso(id, "sintomas")
     *
     * listarSeccionesConProgreso(id, "sindromes")
     */
    public List<ProgresoSeccion> listarSeccionesConProgreso(
            long usuarioId,
            String camino
    ) {

        List<ProgresoSeccion> lista =
                new ArrayList<>();

        if (!esCaminoValido(camino)) {
            return lista;
        }

        SQLiteDatabase db =
                helper.getReadableDatabase();

        String sql =
                "SELECT " +

                        "s.id AS seccion_id, " +
                        "s.camino, " +
                        "s.titulo, " +
                        "s.tema, " +
                        "s.contenido, " +
                        "s.duracion_estimada_min, " +
                        "s.puntos_recompensa, " +

                        "COALESCE(p.lectura, 0) AS lectura, " +
                        "COALESCE(p.completada, 0) AS completada, " +
                        "COALESCE(p.puntos_otorgados, 0) AS puntos_otorgados, " +
                        "COALESCE(p.comida_otorgada, 0) AS comida_otorgada, " +

                        "p.id AS progreso_id, " +
                        "p.iniciada_en, " +
                        "p.completada_en " +

                        "FROM " +
                        DatabaseHelper.T_SECCIONES +
                        " s " +

                        "LEFT JOIN " +
                        DatabaseHelper.T_PROGRESO +
                        " p " +

                        "ON p.seccion_id = s.id " +
                        "AND p.usuario_id = ? " +

                        "WHERE s.camino = ? " +

                        "ORDER BY s.orden ASC";

        try (
                Cursor c = db.rawQuery(
                        sql,
                        new String[]{
                                String.valueOf(
                                        usuarioId
                                ),
                                camino
                        }
                )
        ) {

            while (c.moveToNext()) {

                lista.add(
                        mapearSeccionConProgreso(
                                c,
                                usuarioId
                        )
                );
            }
        }

        return lista;
    }


    /**
     * Acceso directo al camino de Síntomas.
     */
    public List<ProgresoSeccion> listarSintomasConProgreso(
            long usuarioId
    ) {

        return listarSeccionesConProgreso(
                usuarioId,
                CAMINO_SINTOMAS
        );
    }


    /**
     * Acceso directo al camino de Síndromes.
     */
    public List<ProgresoSeccion> listarSindromesConProgreso(
            long usuarioId
    ) {

        return listarSeccionesConProgreso(
                usuarioId,
                CAMINO_SINDROMES
        );
    }


    /**
     * Obtiene una sección concreta con su contenido
     * y progreso.
     *
     * Utilizado por LectorActivity.
     */
    @Nullable
    public ProgresoSeccion obtenerSeccionConProgreso(
            long usuarioId,
            long seccionId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        String sql =
                "SELECT " +

                        "s.id AS seccion_id, " +
                        "s.camino, " +
                        "s.titulo, " +
                        "s.tema, " +
                        "s.contenido, " +
                        "s.duracion_estimada_min, " +
                        "s.puntos_recompensa, " +

                        "COALESCE(p.lectura, 0) AS lectura, " +
                        "COALESCE(p.completada, 0) AS completada, " +
                        "COALESCE(p.puntos_otorgados, 0) AS puntos_otorgados, " +
                        "COALESCE(p.comida_otorgada, 0) AS comida_otorgada, " +

                        "p.id AS progreso_id, " +
                        "p.iniciada_en, " +
                        "p.completada_en " +

                        "FROM " +
                        DatabaseHelper.T_SECCIONES +
                        " s " +

                        "LEFT JOIN " +
                        DatabaseHelper.T_PROGRESO +
                        " p " +

                        "ON p.seccion_id = s.id " +
                        "AND p.usuario_id = ? " +

                        "WHERE s.id = ? " +

                        "LIMIT 1";

        try (
                Cursor c = db.rawQuery(
                        sql,
                        new String[]{
                                String.valueOf(
                                        usuarioId
                                ),
                                String.valueOf(
                                        seccionId
                                )
                        }
                )
        ) {

            if (!c.moveToFirst()) {
                return null;
            }

            ProgresoSeccion p =
                    mapearSeccionConProgreso(
                            c,
                            usuarioId
                    );

            /*
             * Comprobamos si todavía no existe una fila
             * de progreso para esta sección.
             */
            int progresoIdIndex =
                    c.getColumnIndex(
                            "progreso_id"
                    );

            boolean sinProgreso =
                    progresoIdIndex < 0 ||
                            c.isNull(
                                    progresoIdIndex
                            );

            if (sinProgreso) {

                ProgresoSeccion nuevo =
                        ProgresoSeccion.iniciar(
                                usuarioId,
                                seccionId
                        );

                insertarProgreso(
                        nuevo
                );

                p.setIniciadaEn(
                        nuevo.getIniciadaEn()
                );
            }

            return p;
        }
    }


    /**
     * Actualiza únicamente el porcentaje leído.
     *
     * No entrega puntos ni comida.
     */
    public void actualizarLectura(
            long usuarioId,
            long seccionId,
            int porcentaje
    ) {

        porcentaje =
                Math.max(
                        0,
                        Math.min(
                                100,
                                porcentaje
                        )
                );

        /*
         * Garantizamos que exista primero una fila
         * de progreso.
         */
        obtenerProgreso(
                usuarioId,
                seccionId
        );

        SQLiteDatabase db =
                helper.getWritableDatabase();

        ContentValues cv =
                new ContentValues();

        cv.put(
                "lectura",
                porcentaje
        );

        db.update(
                DatabaseHelper.T_PROGRESO,
                cv,
                "usuario_id = ? AND seccion_id = ?",
                new String[]{
                        String.valueOf(usuarioId),
                        String.valueOf(seccionId)
                }
        );
    }


    // =========================================================
    // ESTADÍSTICAS GENERALES
    // =========================================================

    /**
     * Secciones completadas entre ambos caminos.
     */
    public int seccionesCompletadas(
            long usuarioId
    ) {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        try (
                Cursor c = db.rawQuery(

                        "SELECT COUNT(*) " +
                                "FROM " +
                                DatabaseHelper.T_PROGRESO +
                                " WHERE usuario_id = ? " +
                                "AND completada = 1",

                        new String[]{
                                String.valueOf(
                                        usuarioId
                                )
                        }
                )
        ) {

            return c.moveToFirst()
                    ? c.getInt(0)
                    : 0;
        }
    }


    /**
     * Total de secciones entre ambos caminos.
     */
    public int seccionesTotales() {

        SQLiteDatabase db =
                helper.getReadableDatabase();

        try (
                Cursor c = db.rawQuery(

                        "SELECT COUNT(*) " +
                                "FROM " +
                                DatabaseHelper.T_SECCIONES,

                        null
                )
        ) {

            return c.moveToFirst()
                    ? c.getInt(0)
                    : 0;
        }
    }


    // =========================================================
    // ESTADÍSTICAS POR CAMINO
    // =========================================================

    /**
     * Número total de secciones de un camino.
     */
    public int seccionesTotales(
            String camino
    ) {

        if (!esCaminoValido(camino)) {
            return 0;
        }

        SQLiteDatabase db =
                helper.getReadableDatabase();

        try (
                Cursor c = db.rawQuery(

                        "SELECT COUNT(*) " +
                                "FROM " +
                                DatabaseHelper.T_SECCIONES +
                                " WHERE camino = ?",

                        new String[]{
                                camino
                        }
                )
        ) {

            return c.moveToFirst()
                    ? c.getInt(0)
                    : 0;
        }
    }


    /**
     * Número de secciones completadas de un camino.
     */
    public int seccionesCompletadas(
            long usuarioId,
            String camino
    ) {

        if (!esCaminoValido(camino)) {
            return 0;
        }

        SQLiteDatabase db =
                helper.getReadableDatabase();

        String sql =
                "SELECT COUNT(*) " +

                        "FROM " +
                        DatabaseHelper.T_PROGRESO +
                        " p " +

                        "INNER JOIN " +
                        DatabaseHelper.T_SECCIONES +
                        " s " +

                        "ON s.id = p.seccion_id " +

                        "WHERE p.usuario_id = ? " +
                        "AND p.completada = 1 " +
                        "AND s.camino = ?";

        try (
                Cursor c = db.rawQuery(
                        sql,
                        new String[]{
                                String.valueOf(
                                        usuarioId
                                ),
                                camino
                        }
                )
        ) {

            return c.moveToFirst()
                    ? c.getInt(0)
                    : 0;
        }
    }


    /**
     * Calcula el porcentaje promedio de lectura
     * de un camino.
     *
     * Ejemplo:
     *
     * 4 secciones:
     * 100, 100, 50, 0
     *
     * resultado = 62%
     */
    public int progresoCamino(
            long usuarioId,
            String camino
    ) {

        if (!esCaminoValido(camino)) {
            return 0;
        }

        SQLiteDatabase db =
                helper.getReadableDatabase();

        String sql =
                "SELECT " +

                        "COALESCE(" +
                        "CAST(AVG(" +
                        "COALESCE(p.lectura, 0)" +
                        ") AS INTEGER), 0" +
                        ") " +

                        "FROM " +
                        DatabaseHelper.T_SECCIONES +
                        " s " +

                        "LEFT JOIN " +
                        DatabaseHelper.T_PROGRESO +
                        " p " +

                        "ON p.seccion_id = s.id " +
                        "AND p.usuario_id = ? " +

                        "WHERE s.camino = ?";

        try (
                Cursor c = db.rawQuery(
                        sql,
                        new String[]{
                                String.valueOf(
                                        usuarioId
                                ),
                                camino
                        }
                )
        ) {

            return c.moveToFirst()
                    ? c.getInt(0)
                    : 0;
        }
    }


    // =========================================================
    // VALIDACIÓN DE CAMINO
    // =========================================================

    private boolean esCaminoValido(
            String camino
    ) {

        if (camino == null) {
            return false;
        }

        return CAMINO_SINTOMAS.equals(camino)
                ||
                CAMINO_SINDROMES.equals(camino);
    }


    // =========================================================
    // MAPEO DE NOTAS
    // =========================================================

    private Nota mapearNota(
            Cursor c
    ) {

        Nota n =
                new Nota();

        n.setId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "id"
                        )
                )
        );

        n.setUsuarioId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "usuario_id"
                        )
                )
        );

        int seccionIndex =
                c.getColumnIndexOrThrow(
                        "seccion_id"
                );

        n.setSeccionId(
                c.isNull(seccionIndex)
                        ? null
                        : c.getLong(
                        seccionIndex
                )
        );

        n.setTextoSeleccionado(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "texto_seleccionado"
                        )
                )
        );

        n.setComentario(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "comentario"
                        )
                )
        );

        n.setColor(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "color"
                        )
                )
        );

        n.setCreadaEn(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "creada_en"
                        )
                )
        );

        n.setSincronizada(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "sincronizada"
                        )
                ) == 1
        );

        return n;
    }


    // =========================================================
    // MAPEO DE PROGRESO
    // =========================================================

    private ProgresoSeccion mapearProgreso(
            Cursor c
    ) {

        ProgresoSeccion p =
                new ProgresoSeccion();

        p.setId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "id"
                        )
                )
        );

        p.setUsuarioId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "usuario_id"
                        )
                )
        );

        p.setSeccionId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "seccion_id"
                        )
                )
        );

        p.setLectura(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "lectura"
                        )
                )
        );

        p.setCompletada(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "completada"
                        )
                ) == 1
        );

        p.setPuntosOtorgados(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "puntos_otorgados"
                        )
                )
        );

        p.setComidaOtorgada(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "comida_otorgada"
                        )
                )
        );

        p.setIniciadaEn(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "iniciada_en"
                        )
                )
        );

        int completadaIndex =
                c.getColumnIndexOrThrow(
                        "completada_en"
                );

        p.setCompletadaEn(
                c.isNull(
                        completadaIndex
                )
                        ? null
                        : c.getLong(
                        completadaIndex
                )
        );

        return p;
    }


    // =========================================================
    // MAPEO DE SECCIÓN + PROGRESO
    // =========================================================

    private ProgresoSeccion mapearSeccionConProgreso(
            Cursor c,
            long usuarioId
    ) {

        ProgresoSeccion p =
                new ProgresoSeccion();

        p.setUsuarioId(
                usuarioId
        );

        p.setSeccionId(
                c.getLong(
                        c.getColumnIndexOrThrow(
                                "seccion_id"
                        )
                )
        );

        p.setTitulo(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "titulo"
                        )
                )
        );

        p.setTema(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "tema"
                        )
                )
        );

        p.setContenido(
                c.getString(
                        c.getColumnIndexOrThrow(
                                "contenido"
                        )
                )
        );

        p.setDuracionEstimadaMin(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "duracion_estimada_min"
                        )
                )
        );

        p.setPuntosRecompensa(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "puntos_recompensa"
                        )
                )
        );

        p.setLectura(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "lectura"
                        )
                )
        );

        p.setCompletada(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "completada"
                        )
                ) == 1
        );

        p.setPuntosOtorgados(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "puntos_otorgados"
                        )
                )
        );

        p.setComidaOtorgada(
                c.getInt(
                        c.getColumnIndexOrThrow(
                                "comida_otorgada"
                        )
                )
        );


        // -----------------------------------------------------
        // ID DEL PROGRESO
        // -----------------------------------------------------

        int progresoIdIndex =
                c.getColumnIndex(
                        "progreso_id"
                );

        if (
                progresoIdIndex >= 0 &&
                        !c.isNull(progresoIdIndex)
        ) {

            p.setId(
                    c.getLong(
                            progresoIdIndex
                    )
            );
        }


        // -----------------------------------------------------
        // FECHA DE INICIO
        // -----------------------------------------------------

        int iniciadaIndex =
                c.getColumnIndex(
                        "iniciada_en"
                );

        if (
                iniciadaIndex >= 0 &&
                        !c.isNull(iniciadaIndex)
        ) {

            p.setIniciadaEn(
                    c.getLong(
                            iniciadaIndex
                    )
            );
        }


        // -----------------------------------------------------
        // FECHA DE FINALIZACIÓN
        // -----------------------------------------------------

        int completadaIndex =
                c.getColumnIndex(
                        "completada_en"
                );

        if (
                completadaIndex >= 0 &&
                        !c.isNull(completadaIndex)
        ) {

            p.setCompletadaEn(
                    c.getLong(
                            completadaIndex
                    )
            );
        }

        return p;
    }
}
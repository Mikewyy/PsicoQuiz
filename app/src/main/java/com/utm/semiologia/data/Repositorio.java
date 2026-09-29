package com.utm.semiologia.data;

import android.content.Context;

import com.utm.semiologia.data.dao.EstudioDao;
import com.utm.semiologia.data.dao.MascotaDao;
import com.utm.semiologia.data.dao.NivelesDao;
import com.utm.semiologia.data.dao.PomodoroDao;
import com.utm.semiologia.data.dao.UsuarioDao;
import com.utm.semiologia.data.db.DatabaseHelper;

/**
 * Fachada única de la capa de datos.
 *
 * Las Activities/ViewModels no hablan con la base de datos directamente:
 * dependen de esta clase, lo que permite sustituir la implementación local
 * por una remota (o un repositorio híbrido con sincronización) sin tocar la UI.
 */
public class Repositorio {

    private static volatile Repositorio instance;

    private final UsuarioDao  usuarioDao;
    private final MascotaDao  mascotaDao;
    private final EstudioDao  estudioDao;
    private final PomodoroDao pomodoroDao;
    private final NivelesDao  nivelesDao;

    private Repositorio(Context context) {
        DatabaseHelper helper = DatabaseHelper.get(context);
        this.usuarioDao  = new UsuarioDao(helper);
        this.mascotaDao  = new MascotaDao(helper);
        this.estudioDao  = new EstudioDao(helper);
        this.pomodoroDao = new PomodoroDao(helper);
        this.nivelesDao  = new NivelesDao(helper);
    }

    public static Repositorio get(Context context) {
        if (instance == null) {
            synchronized (Repositorio.class) {
                if (instance == null) {
                    instance = new Repositorio(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public UsuarioDao  usuarios()  { return usuarioDao; }
    public MascotaDao  mascotas()  { return mascotaDao; }
    public EstudioDao  estudio()   { return estudioDao; }
    public PomodoroDao pomodoro()  { return pomodoroDao; }
    public NivelesDao  niveles()   { return nivelesDao; }
}

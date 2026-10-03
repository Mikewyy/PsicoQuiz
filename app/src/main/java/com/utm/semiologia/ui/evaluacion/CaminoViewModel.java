package com.utm.semiologia.ui.evaluacion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.util.SesionManager;

/** Carga el progreso de los tramos para el usuario en sesión. */
public class CaminoViewModel extends AndroidViewModel {

    private final Repositorio repo;
    private final SesionManager sesion;
    private final MutableLiveData<EstadoCamino> estado = new MutableLiveData<>();

    public CaminoViewModel(@NonNull Application app) {
        super(app);
        this.repo = Repositorio.get(app);
        this.sesion = new SesionManager(app);
    }

    public LiveData<EstadoCamino> getEstado() {
        return estado;
    }

    public void cargar(String categoria) {
        long usuarioId = sesion.getUsuarioId();
        if (usuarioId <= 0) return;
        Usuario u = repo.usuarios().buscarPorId(usuarioId);
        if (u == null) return;
        estado.setValue(new EstadoCamino(
                u.getPuntos(),
                repo.niveles().listarProgreso(usuarioId, categoria)));
    }
}

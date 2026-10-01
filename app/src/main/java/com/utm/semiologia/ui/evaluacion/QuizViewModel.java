package com.utm.semiologia.ui.evaluacion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utm.semiologia.data.Repositorio;
import com.utm.semiologia.data.db.DatabaseHelper;
import com.utm.semiologia.data.model.Intento;
import com.utm.semiologia.data.model.Nivel;
import com.utm.semiologia.data.model.Opcion;
import com.utm.semiologia.data.model.Pregunta;
import com.utm.semiologia.data.model.ProgresoNivel;
import com.utm.semiologia.data.model.Usuario;
import com.utm.semiologia.util.Gamificacion;
import com.utm.semiologia.util.Normalizador;
import com.utm.semiologia.util.SesionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lógica de un intento: presenta las preguntas, corrige (con Normalizador para
 * las escritas) y, al terminar, persiste el intento y reparte recompensas.
 *
 * Sólo el modo examen cuenta para aprobar el tramo; el bonus de puntos/comida
 * se entrega una única vez, en la primera aprobación.
 */
public class QuizViewModel extends AndroidViewModel {

    private static class Respuesta {
        long preguntaId;
        Long opcionId;
        String texto;
        boolean correcta;
        int puntos;
    }

    private final Repositorio repo;
    private final SesionManager sesion;
    private final MutableLiveData<EstadoQuiz> estado = new MutableLiveData<>();
    private final List<Pregunta> preguntas = new ArrayList<>();
    private final List<Respuesta> respuestas = new ArrayList<>();
    private final Random rnd = new Random();

    private boolean iniciado;
    private long nivelId;
    private int nivelNumero;
    private String nivelNombre;
    private String modo;

    private int indice;
    private int aciertos;
    private int puntos;
    private boolean respondida;
    private boolean correcta;
    private List<Opcion> opcionesActuales = Collections.emptyList();

    public QuizViewModel(@NonNull Application app) {
        super(app);
        this.repo = Repositorio.get(app);
        this.sesion = new SesionManager(app);
    }

    public LiveData<EstadoQuiz> getEstado() {
        return estado;
    }

    /**
     * Idempotente: en una rotación no reinicia el intento ya empezado.
     * Cobra {@link Gamificacion#ENERGIA_POR_INTENTO} al empezar; devuelve false
     * si la mascota no tiene energía suficiente.
     */
    public boolean iniciar(long nivelId, int nivelNumero, String nivelNombre, String modo) {
        if (iniciado) {
            emitir();
            return true;
        }
        long usuarioId = sesion.getUsuarioId();
        if (!repo.mascotas().consumirEnergia(usuarioId, Gamificacion.ENERGIA_POR_INTENTO)) {
            return false;
        }
        this.nivelId = nivelId;
        this.nivelNumero = nivelNumero;
        this.nivelNombre = nivelNombre;
        this.modo = modo;

        List<Pregunta> cargadas = repo.niveles().listarPreguntas(nivelId);
        Collections.shuffle(cargadas, rnd);
        preguntas.addAll(cargadas);
        iniciado = true;
        cargarActual();
        return true;
    }

    private boolean esExamen() {
        return Intento.MODO_EXAMEN.equals(modo);
    }

    private Pregunta actual() {
        return (indice >= 0 && indice < preguntas.size()) ? preguntas.get(indice) : null;
    }

    private void cargarActual() {
        respondida = false;
        correcta = false;
        Pregunta p = actual();
        opcionesActuales = (p != null && p.esMcq())
                ? p.opcionesBarajadas(rnd)
                : Collections.emptyList();
        emitir();
    }

    private void emitir() {
        Pregunta p = actual();
        if (p == null) {
            estado.setValue(EstadoQuiz.vacio(esExamen()));
            return;
        }
        estado.setValue(new EstadoQuiz(p, opcionesActuales, indice, preguntas.size(),
                aciertos, puntos, respondida, correcta, p.esEscrita(), esExamen(),
                p.getJustificacion()));
    }

    public void comprobarOpcion(long opcionId) {
        Pregunta p = actual();
        if (p == null || respondida) return;
        Opcion elegida = null;
        for (Opcion o : opcionesActuales) {
            if (o.getId() == opcionId) {
                elegida = o;
                break;
            }
        }
        if (elegida == null) return;
        puntuar(p, elegida.isCorrecta(), elegida.getId(), null);
    }

    public void comprobarTexto(String texto) {
        Pregunta p = actual();
        if (p == null || respondida) return;
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return;
        boolean ok = Normalizador.coincide(limpio, p.getRespuestasValidas());
        puntuar(p, ok, null, limpio);
    }

    private void puntuar(Pregunta p, boolean ok, Long opcionId, String texto) {
        Respuesta r = new Respuesta();
        r.preguntaId = p.getId();
        r.opcionId = opcionId;
        r.texto = texto;
        r.correcta = ok;
        r.puntos = ok ? p.getPuntos() : 0;
        respuestas.add(r);

        respondida = true;
        correcta = ok;
        if (ok) {
            aciertos++;
            puntos += p.getPuntos();
        }
        emitir();
    }

    /** Pasa a la siguiente pregunta. Devuelve false si era la última. */
    public boolean avanzar() {
        if (indice >= preguntas.size() - 1) return false;
        indice++;
        cargarActual();
        return true;
    }

    public boolean esUltima() {
        return !preguntas.isEmpty() && indice >= preguntas.size() - 1;
    }

    /** Persiste el intento, actualiza progreso y reparte recompensas. */
    public ResultadoQuiz terminar() {
        long usuarioId = sesion.getUsuarioId();

        ResultadoQuiz out = new ResultadoQuiz();
        out.nivelId = nivelId;
        out.nivelNumero = nivelNumero;
        out.nivelNombre = nivelNombre;
        out.total = preguntas.size();
        out.aciertos = aciertos;
        out.puntos = puntos;
        out.esExamen = esExamen();

        Intento intento = Intento.iniciar(usuarioId, nivelId, modo, out.total);
        intento.finalizar(aciertos, puntos, DatabaseHelper.UMBRAL_APROBACION);
        out.aprobado = intento.isAprobado();
        out.porcentaje = Math.round(intento.porcentaje());

        long intentoId = repo.niveles().registrarIntento(intento);
        for (Respuesta r : respuestas) {
            repo.niveles().registrarRespuesta(intentoId, r.preguntaId, r.texto,
                    r.opcionId, r.correcta, r.puntos);
        }

        if (esExamen()) {
            ProgresoNivel antes = repo.niveles().obtenerProgreso(usuarioId, nivelId);
            out.primeraAprobacion = out.aprobado
                    && (antes == null || antes.getCompletadoEn() == null);

            repo.niveles().actualizarProgresoTrasExamen(
                    usuarioId, nivelId, out.porcentaje, puntos, out.aprobado);

            out.bonusPuntos = out.primeraAprobacion ? Gamificacion.PUNTOS_BONUS_TRAMO_APROBADO : 0;
            out.comida = out.primeraAprobacion ? Gamificacion.COMIDA_POR_TRAMO_APROBADO : 0;

            int ganados = puntos + out.bonusPuntos;
            Usuario u = repo.usuarios().buscarPorId(usuarioId);
            if (u != null) {
                u.sumarPuntos(ganados);
                repo.usuarios().actualizar(u);
            }
            repo.usuarios().registrarActividad(usuarioId, 0, 0, ganados);

            if (out.comida > 0) {
                repo.mascotas().otorgarAlimento(
                        usuarioId, Gamificacion.ALIMENTO_GALLETA_ID, out.comida);
            }
            if (out.aprobado) {
                out.siguienteNumero = desbloqueadoSiguiente(usuarioId, nivelNumero);
            }
        } else {
            // La práctica no da puntos, pero sí cuenta como actividad del día.
            repo.usuarios().registrarActividad(usuarioId, 0, 0, 0);
        }
        return out;
    }

    private int desbloqueadoSiguiente(long usuarioId, int numeroActual) {
        Nivel siguiente = repo.niveles().obtenerNivelPorNumero(numeroActual + 1);
        if (siguiente == null) return 0;
        return repo.niveles().estaDesbloqueado(usuarioId, siguiente.getId())
                ? numeroActual + 1 : 0;
    }
}

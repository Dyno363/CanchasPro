/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.GeneradorCodigo;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador de las canchas (alta, edición y cambio de estado).
 *
 * Permisos: consultar es libre; crear, modificar y cambiar el estado
 * es solo del ADMINISTRADOR (forma parte del menú Config).
 *
 * Reglas que se aplican aquí:
 
 *   La capacidad no puede superar los jugadores máximos del tipo de cancha
 *   No se puede cambiar el horario si deja reservas activas fuera de él
 *   Las canchas no se eliminan (tienen historial); se ponen INACTIVAS
 *   Al pasar a mantenimiento o inactiva, se devuelven las reservas activas
 *       afectadas para que el operador las reprograme o cancele
 *
 */

public class CanchaController {
 
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;
 
    public CanchaController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }
 
    // =====================================================================
    //  CONSULTAS (cualquier usuario)
    // =====================================================================
 
    /** @return todas las canchas en orden de código (para tablas y para el mapa) */
    public List<Cancha> listar() {
        return new ArrayList<>(complejo.getCanchas());
    }
 
    /** @return solo las canchas que aceptan reservas (para el combo de Nueva Reserva) */
    public List<Cancha> listarOperativas() {
        List<Cancha> resultado = new ArrayList<>();
        for (Cancha cancha : complejo.getCanchas()) {
            if (cancha.estaOperativa()) {
                resultado.add(cancha);
            }
        }
        return resultado;
    }
 
    public Cancha obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarCancha(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la cancha " + codigo + "."));
    }
 
    /**
     * Reservas que todavía no terminan (pendientes, confirmadas o en curso).
     * Son las que se ven afectadas por un cambio de horario o de estado.
     */
    public List<Reserva> getReservasActivas(String codigoCancha) throws EntidadNoEncontradaException {
        List<Reserva> activas = new ArrayList<>();
        for (Reserva reserva : complejo.getReservasDeCancha(obtener(codigoCancha))) {
            if (!reserva.getEstado().esTerminal()) {
                activas.add(reserva);
            }
        }
        return activas;
    }
 
    public TipoCancha[] getTipos() {
        return TipoCancha.values();
    }
 
    public EstadoCancha[] getEstados() {
        return EstadoCancha.values();
    }
 
    public boolean puedeAdministrar() {
        return sesion.esAdministrador();
    }
 
    // =====================================================================
    //  OPERACIONES (solo administrador)
    // =====================================================================
 
    /**
     * Registra una cancha nueva; queda DISPONIBLE.
     */
    public Cancha crear(String nombre, TipoCancha tipo, int capacidad, LocalTime apertura, LocalTime cierre)
            throws CanchaProException {
        sesion.exigirAdministrador("registrar canchas");
        validarCapacidad(tipo, capacidad);
        Cancha cancha = new Cancha(complejo.generarCodigo(GeneradorCodigo.Tipo.CANCHA), nombre, tipo,
                capacidad, EstadoCancha.DISPONIBLE, apertura, cierre);
        complejo.agregarCancha(cancha);
        return cancha;
    }
 
    /**
     * Modifica nombre, tipo, capacidad y horario. El estado se cambia aparte
     * con {@link #cambiarEstado}.
     *
     * Técnica "objeto de prueba": se arma una cancha temporal con los datos
     * nuevos. Si el constructor no protesta, TODOS los datos son válidos; recién
     * entonces se copian a la cancha real.
     *
     * @throws CanchaProException si un dato es inválido o el nuevo horario deja
     *                            reservas activas fuera
     */
    public Cancha actualizar(String codigo, String nombre, TipoCancha tipo, int capacidad,
                             LocalTime apertura, LocalTime cierre) throws CanchaProException {
        sesion.exigirAdministrador("modificar canchas");
        Cancha cancha = obtener(codigo);
 
        // 1) Validar todo con un objeto de prueba
        validarCapacidad(tipo, capacidad);
        Cancha prueba = new Cancha(codigo, nombre, tipo, capacidad, cancha.getEstado(), apertura, cierre);
 
        StringJoiner fueraDeHorario = new StringJoiner(", ");
        for (Reserva reserva : getReservasActivas(codigo)) {
            if (!prueba.cubreHorario(reserva.getInicio(), reserva.getFin())) {
                fueraDeHorario.add(reserva.getCodigo());
            }
        }
        if (fueraDeHorario.length() > 0) {
            throw new CanchaProException("El nuevo horario (" + prueba.getHorarioTexto()
                    + ") deja fuera reservas activas: " + fueraDeHorario
                    + ". Reprográmelas o cancélelas primero.");
        }
 
        // 2) Aplicar
        cancha.setNombre(nombre);
        cancha.setTipo(tipo);
        cancha.setCapacidad(capacidad);
        cancha.setHorario(apertura, cierre);
        return cancha;
    }
 
    /**
     * Cambia el estado (Disponible / Mantenimiento / Inactiva).
     *
     * No se bloquea el cambio si hay reservas: una cancha puede dañarse en
     * cualquier momento. En cambio, se devuelven las reservas activas afectadas
     * para que la vista avise al operador.
     *
     * @return reservas activas que quedan en una cancha que ya no está disponible
     *         (lista vacía si el nuevo estado es DISPONIBLE o no hay reservas)
     */
    public List<Reserva> cambiarEstado(String codigo, EstadoCancha nuevoEstado) throws CanchaProException {
        sesion.exigirAdministrador("cambiar el estado de las canchas");
        Cancha cancha = obtener(codigo);
        cancha.setEstado(nuevoEstado);
        if (nuevoEstado.permiteReservas()) {
            return new ArrayList<>();
        }
        return getReservasActivas(codigo);
    }
 
    /** Cambia solo el horario de atención de una cancha (conserva los demás datos). */
    public Cancha actualizarHorario(String codigo, LocalTime apertura, LocalTime cierre) throws CanchaProException {
        Cancha c = obtener(codigo);
        return actualizar(codigo, c.getNombre(), c.getTipo(), c.getCapacidad(), apertura, cierre);
    }
 
    /**
     * Aplica el mismo horario de atención a TODAS las canchas.
     *
     * "Todo o nada": primero revisa todas las canchas; si alguna tiene reservas
     * activas fuera del nuevo horario, no cambia ninguna.
     *
     * @return cantidad de canchas actualizadas
     */
    public int cambiarHorarioGeneral(LocalTime apertura, LocalTime cierre) throws CanchaProException {
        sesion.exigirAdministrador("cambiar el horario de atención");
        StringJoiner problemas = new StringJoiner("; ");
        for (Cancha c : complejo.getCanchas()) {
            Cancha prueba = new Cancha(c.getCodigo(), c.getNombre(), c.getTipo(), c.getCapacidad(),
                    c.getEstado(), apertura, cierre);
            StringJoiner fuera = new StringJoiner(", ");
            for (Reserva r : getReservasActivas(c.getCodigo())) {
                if (!prueba.cubreHorario(r.getInicio(), r.getFin())) {
                    fuera.add(r.getCodigo());
                }
            }
            if (fuera.length() > 0) {
                problemas.add(c.getNombre() + ": " + fuera);
            }
        }
        if (problemas.length() > 0) {
            throw new CanchaProException("No se cambió ningún horario. Hay reservas activas fuera del nuevo horario ("
                    + problemas + ").");
        }
        for (Cancha c : complejo.getCanchas()) {
            c.setHorario(apertura, cierre);
        }
        return complejo.getCanchas().size();
    }
 
    /** Elimina una cancha SIN reservas (por ejemplo, registrada por error). */
    public void eliminar(String codigo) throws CanchaProException {
        sesion.exigirAdministrador("eliminar canchas");
        complejo.eliminarCancha(codigo);
    }
 
    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================
 
    private static void validarCapacidad(TipoCancha tipo, int capacidad) throws DatoInvalidoException {
        if (tipo == null) {
            throw new DatoInvalidoException("El tipo de cancha es obligatorio.");
        }
        if (capacidad > tipo.getJugadoresMaximos()) {
            throw new DatoInvalidoException("Una cancha de " + tipo.getDescripcion() + " admite como máximo "
                    + tipo.getJugadoresMaximos() + " jugadores.");
        }
    }
}
 
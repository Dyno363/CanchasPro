/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.Validador;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador de RESERVAS: el corazón del sistema.
 *
 * Orquesta a los objetos del modelo sin repetir sus reglas:
 *  
 *    {@code MotorDisponibilidad}: cancha operativa, horario y choques.  
 *    {@code CalculadoraTarifa}: precio de la cancha por tramos.  
 *    {@code Reserva}: servicios, pagos y máquina de estados.  
 *    {@code ComplejoDeportivo}: guarda en memoria (última defensa contra choques).  
 *   
 *
 * Reglas propias del controlador:
 *  
 *    No se reserva en el pasado (con {@value #MINUTOS_GRACIA} min de gracia para
 *       el cliente que llega sin reserva y quiere jugar "ya").  
 *    La reserva se arma COMPLETA (con sus servicios) antes de guardarla: si un
 *       servicio falla, no queda una reserva a medias en memoria.  
 *    El responsable de crear o cancelar siempre es el usuario de la sesión.  
 *   
 */
public class ReservaController {
     /** Minutos hacia atrás que se aceptan como inicio (cliente que llega sin reservar). */
    public static final int MINUTOS_GRACIA = 30;

    /** Minutos antes del inicio en que ya se puede marcar "En curso" (llegaron temprano). */
    public static final int MINUTOS_ANTICIPACION_INICIO = 15;

    /** Duración máxima ofrecida en el formulario (4 horas). */
    public static final int DURACION_MAXIMA_MINUTOS = 240;

    private final ComplejoDeportivo complejo;
    private final Sesion sesion;

    public ReservaController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }

    // =====================================================================
    //  CONSULTAS
    // =====================================================================

    /** @return todas las reservas (incluidas canceladas), ordenadas por inicio */
    public List<Reserva> listar() {
        List<Reserva> lista = new ArrayList<>(complejo.getReservas());
        lista.sort(Comparator.comparing(Reserva::getInicio));
        return lista;
    }

    /**
     * Filtro de la tabla de reservas. Cualquier parámetro en null (o vacío) no filtra.
     *
     * @param fecha    solo reservas que inician ese día
     * @param estado   solo reservas en ese estado
     * @param criterio texto a buscar en código de reserva, cliente o cancha
     */
    public List<Reserva> filtrar(LocalDate fecha, EstadoReserva estado, String criterio) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : listar()) {
            boolean coincideFecha = fecha == null || r.getInicio().toLocalDate().equals(fecha);
            boolean coincideEstado = estado == null || r.getEstado() == estado;
            boolean coincideTexto = Validador.esVacio(criterio)
                    || Validador.contiene(r.getCodigo(), criterio)
                    || r.getCliente().coincideCon(criterio)
                    || Validador.contiene(r.getCancha().getNombre(), criterio);
            if (coincideFecha && coincideEstado && coincideTexto) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    public List<Reserva> getReservasDelDia(LocalDate fecha) {
        return complejo.getReservasDelDia(fecha);
    }

    public Reserva obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarReserva(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la reserva " + codigo + "."));
    }

    public EstadoReserva[] getEstados() {
        return EstadoReserva.values();
    }

    /** @return duraciones válidas en minutos para el combo: 30, 60, 90 ... 240 */
    public List<Integer> getDuracionesPermitidas() {
        List<Integer> duraciones = new ArrayList<>();
        for (int m = Reserva.DURACION_MINIMA_MINUTOS; m <= DURACION_MAXIMA_MINUTOS; m += Reserva.BLOQUE_MINUTOS) {
            duraciones.add(m);
        }
        return duraciones;
    }

    /**
     * BÚSQUEDA AUTOMÁTICA (pedida por el PDF): el operador indica fecha, hora,
     * duración y tipo; el sistema responde qué canchas están libres.
     *
     * @param tipo null = cualquier tipo
     */
    public List<Cancha> buscarCanchasDisponibles(LocalDate fecha, LocalTime hora, int duracionMinutos,
                                                 TipoCancha tipo) throws CanchaProException {
        LocalDateTime inicio = combinar(fecha, hora);
        validarDuracion(duracionMinutos);
        validarNoPasado(inicio);
        return complejo.getMotorDisponibilidad().buscarCanchasDisponibles(inicio,
                Duration.ofMinutes(duracionMinutos), tipo, complejo.getCanchas(), complejo.getReservas());
    }

    /** Precio de la cancha (sin servicios) para mostrar antes de reservar. */
    public double cotizarCancha(LocalDateTime inicio, int duracionMinutos) throws CanchaProException {
        validarDuracion(duracionMinutos);
        return complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, inicio.plusMinutes(duracionMinutos));
    }

    // =====================================================================
    //  CREAR
    // =====================================================================

    /**
     * Crea una reserva PENDIENTE con sus servicios adicionales.
     *
     * Pasos (cada uno puede detener el proceso con un mensaje claro):
     *  
     *    Buscar cliente y cancha.  
     *    Validar duración y que no sea en el pasado.  
     *    MotorDisponibilidad: cancha operativa, dentro del horario, sin choques
     *       (incluye minutos de limpieza).  
     *    CalculadoraTarifa: precio de la cancha ("foto" que no cambia después).  
     *    Construir la reserva y agregarle los servicios.  
     *    Recién entonces guardarla en memoria.  
     *   
     *
     * @param servicios código de servicio → cantidad (puede ser null o vacío)
     * @return la reserva creada
     */
    public Reserva crear(String codigoCliente, String codigoCancha, LocalDateTime inicio,
                         int duracionMinutos, Map<String, Integer> servicios) throws CanchaProException {
        // 1) Buscar
        Cliente cliente = complejo.buscarCliente(codigoCliente)
                .orElseThrow(() -> new EntidadNoEncontradaException("Seleccione un cliente válido."));
        Cancha cancha = complejo.buscarCancha(codigoCancha)
                .orElseThrow(() -> new EntidadNoEncontradaException("Seleccione una cancha válida."));

        // 2) Validar duración y fecha
        validarDuracion(duracionMinutos);
        if (inicio == null) {
            throw new DatoInvalidoException("La fecha y la hora de inicio son obligatorias.");
        }
        validarNoPasado(inicio);
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);

        // 3) Disponibilidad (pruebas #3, #4 y #5 del PDF)
        complejo.getMotorDisponibilidad().validarDisponibilidad(cancha, inicio, fin, complejo.getReservas());

        // 4) Precio
        double precio = complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, fin);

        // 5) Armar la reserva completa
        Reserva reserva = new Reserva(complejo.generarCodigo(GeneradorCodigo.Tipo.RESERVA), cliente, cancha,
                inicio, Duration.ofMinutes(duracionMinutos), precio, sesion.getUsuario(), FechaUtil.ahora());
        if (servicios != null) {
            for (Map.Entry<String, Integer> e : servicios.entrySet()) {
                reserva.agregarServicio(obtenerServicioActivo(e.getKey()), e.getValue());
            }
        }

        // 6) Guardar en memoria
        complejo.agregarReserva(reserva);
        return reserva;
    }

    // =====================================================================
    //  SERVICIOS DE UNA RESERVA EXISTENTE
    // =====================================================================

    public Reserva agregarServicio(String codigoReserva, String codigoServicio, int cantidad)
            throws CanchaProException {
        Reserva reserva = obtener(codigoReserva);
        reserva.agregarServicio(obtenerServicioActivo(codigoServicio), cantidad);
        return reserva;
    }

    public Reserva quitarServicio(String codigoReserva, String codigoServicio) throws CanchaProException {
        Reserva reserva = obtener(codigoReserva);
        reserva.quitarServicio(codigoServicio);
        return reserva;
    }

    // =====================================================================
    //  CAMBIOS DE ESTADO (la Reserva decide si la transición es válida)
    // =====================================================================

    /** PENDIENTE → CONFIRMADA, exigiendo el anticipo mínimo de config.properties. */
    public Reserva confirmar(String codigo) throws CanchaProException {
        Reserva reserva = obtener(codigo);
        reserva.confirmar(complejo.getConfiguracion().getPorcentajeAnticipoMinimo());
        return reserva;
    }

    /**
     * CONFIRMADA → EN_CURSO. Solo desde {@value #MINUTOS_ANTICIPACION_INICIO} min
     * antes del inicio y mientras no haya terminado el horario.
     */
    public Reserva iniciar(String codigo) throws CanchaProException {
        Reserva reserva = obtener(codigo);
        LocalDateTime ahora = FechaUtil.ahora();
        if (ahora.isBefore(reserva.getInicio().minusMinutes(MINUTOS_ANTICIPACION_INICIO))) {
            throw new CanchaProException("Aún no es hora: la reserva " + codigo + " inicia el "
                    + FechaUtil.formatearFechaHora(reserva.getInicio()) + ".");
        }
        if (!ahora.isBefore(reserva.getFin())) {
            throw new CanchaProException("El horario de la reserva " + codigo
                    + " ya terminó. Cancélela si el cliente no se presentó.");
        }
        reserva.iniciar();
        return reserva;
    }

    /** EN_CURSO → FINALIZADA (se permite terminar antes de la hora). */
    public Reserva finalizar(String codigo) throws CanchaProException {
        Reserva reserva = obtener(codigo);
        reserva.finalizar();
        return reserva;
    }

    /**
     * Cancela SIN eliminar: guarda motivo, fecha y responsable (el usuario de la
     * sesión). El horario queda libre. Los pagos recibidos se conservan en el
     * historial (prueba #9 del PDF).
     */
    public Reserva cancelar(String codigo, String motivo) throws CanchaProException {
        Reserva reserva = obtener(codigo);
        reserva.cancelar(motivo, sesion.getUsuario(), FechaUtil.ahora());
        return reserva;
    }

    /**
     * Actualiza los estados según el reloj. Lo llama el mapa (con un Timer) y el
     * arranque del sistema:
     * 
     *   CONFIRMADA cuya hora ya empezó → EN_CURSO. 
     *   EN_CURSO cuya hora ya terminó → FINALIZADA. 
     *  
     * Las PENDIENTES no se tocan: sin confirmar, el operador decide.
     *
     * @return cuántas reservas cambiaron de estado
     */
    public int actualizarEstadosPorHora() {
        LocalDateTime ahora = FechaUtil.ahora();
        int cambios = 0;
        for (Reserva r : complejo.getReservas()) {
            try {
                if (r.getEstado() == EstadoReserva.CONFIRMADA && r.estaEnCurso(ahora)) {
                    r.iniciar();
                    cambios++;
                } else if (r.getEstado() == EstadoReserva.EN_CURSO && !ahora.isBefore(r.getFin())) {
                    r.finalizar();
                    cambios++;
                }
            } catch (CanchaProException e) {
                // No ocurre: se comprobó el estado antes de cada transición.
            }
        }
        return cambios;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private ServicioAdicional obtenerServicioActivo(String codigo) throws CanchaProException {
        ServicioAdicional servicio = complejo.buscarServicio(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el servicio " + codigo + "."));
        if (!servicio.isActivo()) {
            throw new DatoInvalidoException("El servicio " + servicio.getNombre() + " ya no se ofrece.");
        }
        return servicio;
    }

    private static LocalDateTime combinar(LocalDate fecha, LocalTime hora) throws DatoInvalidoException {
        if (fecha == null || hora == null) {
            throw new DatoInvalidoException("La fecha y la hora de inicio son obligatorias.");
        }
        return LocalDateTime.of(fecha, hora);
    }

    private static void validarDuracion(int minutos) throws DatoInvalidoException {
        if (minutos < Reserva.DURACION_MINIMA_MINUTOS || minutos % Reserva.BLOQUE_MINUTOS != 0) {
            throw new DatoInvalidoException("La duración debe ser de al menos " + Reserva.DURACION_MINIMA_MINUTOS
                    + " minutos y en bloques de " + Reserva.BLOQUE_MINUTOS + " minutos.");
        }
        if (minutos > DURACION_MAXIMA_MINUTOS) {
            throw new DatoInvalidoException("La duración máxima de una reserva es de "
                    + (DURACION_MAXIMA_MINUTOS / 60) + " horas.");
        }
    }

    private static void validarNoPasado(LocalDateTime inicio) throws DatoInvalidoException {
        LocalDateTime limite = FechaUtil.ahora().minusMinutes(MINUTOS_GRACIA);
        if (inicio.isBefore(limite)) {
            throw new DatoInvalidoException("No se puede reservar en el pasado ("
                    + FechaUtil.formatearFechaHora(inicio) + ").");
        }
    }
}

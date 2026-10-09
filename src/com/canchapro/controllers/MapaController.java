/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
/**
 * Controlador del MAPA DINÁMICO de canchas (pestaña 2 del dashboard).
 *
 * La vista dibuja un botón por cancha, pero NO decide nada: le pide a este
 * controlador una lista de {@link InfoCancha} ya calculada (estado visual,
 * partido en curso, próxima reserva) y solo pinta.
 *
 * Flujo cada {@value #INTERVALO_REFRESCO_MS} ms (Timer de Swing en el panel):
 * 
 *   {@link #refrescar()}: ajusta estados por la hora (confirmada → en curso → finalizada).
 *   {@link #getEstadoCanchas()}: el panel repinta los colores.
 * 
 *
 * Clic en una cancha "En curso" → {@link #getReservaParaPos(String)} devuelve la
 * reserva a la que se cargarán los productos de la tienda.
 */
public class MapaController {
    /** Cada cuánto se refresca el mapa (1 minuto). */
    public static final int INTERVALO_REFRESCO_MS = 60_000;

    private final ComplejoDeportivo complejo;
    private final ReservaController reservaController;

    /**
     * @param complejo          sistema en memoria
     * @param reservaController se reutiliza para actualizar estados por hora
     *                          (no se duplica esa lógica aquí)
     */
    public MapaController(ComplejoDeportivo complejo, ReservaController reservaController) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.reservaController = Objects.requireNonNull(reservaController, "El controlador de reservas es obligatorio.");
    }

    // =====================================================================
    //  CONSULTAS PARA PINTAR EL MAPA
    // =====================================================================

    /** @return una ficha por cancha, calculada con la hora actual */
    public List<InfoCancha> getEstadoCanchas() {
        return getEstadoCanchas(FechaUtil.ahora());
    }

    /**
     * Versión con hora explícita: permite probar el mapa "como si fueran" las 16:00
     * de un día con partidos, sin esperar al reloj.
     */
    public List<InfoCancha> getEstadoCanchas(LocalDateTime momento) {
        List<InfoCancha> fichas = new ArrayList<>();
        for (Cancha cancha : complejo.getCanchas()) {
            fichas.add(crearFicha(cancha, momento));
        }
        return fichas;
    }

    public InfoCancha getInfo(String codigoCancha) throws EntidadNoEncontradaException {
        return crearFicha(obtenerCancha(codigoCancha), FechaUtil.ahora());
    }

    /** Cuántas canchas hay en cada estado (para la leyenda y los indicadores). */
    public Map<EstadoVisualCancha, Integer> contarPorEstado() {
        Map<EstadoVisualCancha, Integer> conteo = new EnumMap<>(EstadoVisualCancha.class);
        for (EstadoVisualCancha estado : EstadoVisualCancha.values()) {
            conteo.put(estado, 0);
        }
        for (InfoCancha ficha : getEstadoCanchas()) {
            conteo.merge(ficha.getEstado(), 1, Integer::sum);
        }
        return conteo;
    }

    /** El mapa muestra u oculta el acceso al POS según config.properties. */
    public boolean isTiendaActiva() {
        return complejo.getConfiguracion().isModuloTiendaActivo();
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    /**
     * Pone las reservas al día según la hora (lo llama el Timer del mapa).
     *
     * @return cuántas reservas cambiaron de estado
     */
    public int refrescar() {
        return reservaController.actualizarEstadosPorHora();
    }

    /**
     * Regla del enunciado: el POS solo se abre desde una cancha EN CURSO.
     *
     * @return la reserva que se está jugando en esa cancha
     * @throws CanchaProException si la tienda está desactivada o la cancha no tiene partido ahora
     */
    public Reserva getReservaParaPos(String codigoCancha) throws CanchaProException {
        if (!isTiendaActiva()) {
            throw new CanchaProException("El módulo de Tienda/Cafetería está desactivado en la configuración.");
        }
        InfoCancha ficha = getInfo(codigoCancha);
        if (!ficha.getEstado().permiteAbrirPos()) {
            throw new CanchaProException("La cancha " + ficha.getCancha().getNombre() + " está "
                    + ficha.getEstado().getDescripcion().toLowerCase(Locale.ROOT)
                    + ". El POS solo se abre desde una cancha En curso.");
        }
        return ficha.getReservaEnCurso();
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private Cancha obtenerCancha(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarCancha(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la cancha " + codigo + "."));
    }

    private InfoCancha crearFicha(Cancha cancha, LocalDateTime momento) {
        Reserva enCurso = complejo.getReservaEnCurso(cancha, momento).orElse(null);
        EstadoVisualCancha estado = EstadoVisualCancha.calcular(cancha, enCurso != null);
        return new InfoCancha(cancha, estado, enCurso, buscarProxima(cancha, momento), momento);
    }

    /** Siguiente reserva activa del MISMO día que todavía no empieza. */
    private Reserva buscarProxima(Cancha cancha, LocalDateTime momento) {
        for (Reserva r : complejo.getReservasDeCancha(cancha)) { // ya vienen ordenadas por inicio
            if (r.ocupaHorario() && !r.getEstado().esTerminal()
                    && r.getInicio().isAfter(momento)
                    && r.getInicio().toLocalDate().equals(momento.toLocalDate())) {
                return r;
            }
        }
        return null;
    }

    // =====================================================================
    //  CLASE ANIDADA: ficha de una cancha para el mapa
    // =====================================================================

    /**
     * Todo lo que el mapa necesita saber de UNA cancha en un momento dado.
     * Inmutable: cada refresco crea fichas nuevas.
     */
    public static final class InfoCancha {

        private final Cancha cancha;
        private final EstadoVisualCancha estado;
        private final Reserva reservaEnCurso;
        private final Reserva proximaReserva;
        private final LocalDateTime momento;

        private InfoCancha(Cancha cancha, EstadoVisualCancha estado, Reserva reservaEnCurso,
                           Reserva proximaReserva, LocalDateTime momento) {
            this.cancha = cancha;
            this.estado = estado;
            this.reservaEnCurso = reservaEnCurso;
            this.proximaReserva = proximaReserva;
            this.momento = momento;
        }

        public Cancha getCancha() {
            return cancha;
        }

        public EstadoVisualCancha getEstado() {
            return estado;
        }

        /** @return la reserva que se juega ahora, o null si no hay partido */
        public Reserva getReservaEnCurso() {
            return reservaEnCurso;
        }

        /** @return la siguiente reserva de hoy, o null si no hay más */
        public Reserva getProximaReserva() {
            return proximaReserva;
        }

        public boolean tienePartido() {
            return reservaEnCurso != null;
        }

        /** Minutos que le quedan al partido en curso (0 si no hay partido). */
        public long getMinutosRestantes() {
            return reservaEnCurso == null ? 0
                    : Math.max(0, Duration.between(momento, reservaEnCurso.getFin()).toMinutes());
        }

        /** Segunda línea del botón. Ej.: "Carlos Pérez · quedan 45 min" o "Libre · próxima 20:00". */
        public String getSubtitulo() {
            if (reservaEnCurso != null) {
                return reservaEnCurso.getCliente().getNombre() + " · quedan " + getMinutosRestantes() + " min";
            }
            if (estado != EstadoVisualCancha.DISPONIBLE) {
                return estado.getDescripcion();
            }
            return proximaReserva == null ? "Libre por hoy"
                    : "Libre · próxima " + FechaUtil.formatearHora(proximaReserva.getInicio().toLocalTime());
        }

        /** Texto del tooltip (varias líneas) al pasar el mouse sobre la cancha. */
        public String getDetalle() {
            StringBuilder sb = new StringBuilder();
            sb.append(cancha).append('\n')
              .append("Horario: ").append(cancha.getHorarioTexto()).append('\n')
              .append("Estado: ").append(estado.getDescripcion());
            if (reservaEnCurso != null) {
                sb.append('\n').append("Partido: ").append(reservaEnCurso.getCodigo()).append(" - ")
                  .append(reservaEnCurso.getCliente().getNombre()).append(" (")
                  .append(FechaUtil.formatearRango(reservaEnCurso.getInicio(), reservaEnCurso.getFin()))
                  .append(')').append('\n')
                  .append("Saldo pendiente: ").append(MonedaUtil.formatear(reservaEnCurso.calcularSaldo()));
            }
            if (proximaReserva != null) {
                sb.append('\n').append("Próxima: ").append(proximaReserva.getCodigo()).append(" a las ")
                  .append(FechaUtil.formatearHora(proximaReserva.getInicio().toLocalTime()));
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return cancha.getCodigo() + " [" + estado + "] " + getSubtitulo();
        }
    }
}

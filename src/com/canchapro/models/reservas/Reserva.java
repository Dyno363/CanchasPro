/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.PagoInvalidoException;
import com.canchapro.models.exceptions.TransicionEstadoInvalidaException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Reserva de una cancha: el corazón de CanchaPro.
 *
 * Responsabilidades (cada regla vive aquí porque aquí están los datos):
 * 
 *   Horario: detectar si se traslapa con otro rango de horas.
 *   Cobro: total = precio de cancha + servicios; saldo = total - pagos.
 *   Pagos: rechazar pagos mayores al saldo o a reservas canceladas.
 *   Estados: permitir solo transiciones válidas y cancelar sin borrar.
 * 
 *
 *    Relaciones:
 * 
 *    Asociación con Cliente, Cancha y Usuario (los conoce, pero no son suyos).
 *   Composición con ServicioReserva, Pago y Cancelacion (son parte de la reserva).
 * 
 */
public class Reserva {

    /** Duración mínima de una reserva, en minutos. */
    public static final int DURACION_MINIMA_MINUTOS = 30;

    /** La duración debe ser múltiplo de este bloque (ej. 30 → 1 h, 1.5 h, 2 h...). */
    public static final int BLOQUE_MINUTOS = 30;

    private final String codigo;
    private final Cliente cliente;
    private final Cancha cancha;
    private final LocalDateTime inicio;
    private final LocalDateTime fin;
    private final double precioCancha;
    private EstadoReserva estado;
    private final LocalDateTime fechaCreacion;
    private final Usuario creadaPor;
    private final List<ServicioReserva> servicios;
    private final List<Pago> pagos;
    private Cancelacion cancelacion;

    // =====================================================================
    //  CONSTRUCTORES
    // =====================================================================

    /**
     * Crea una reserva NUEVA en estado PENDIENTE.
     *
     * El fin se calcula automáticamente: fin = inicio + duración.
     * El precio de la cancha se recibe ya calculado por CalculadoraTarifa
     * (nunca se escribe a mano).
     *
     * @param codigo        código único, ej. "R0006"
     * @param cliente       cliente que reserva
     * @param cancha        cancha reservada
     * @param inicio        fecha y hora de inicio
     * @param duracion      duración (mínimo 30 min, en bloques de 30 min)
     * @param precioCancha  precio calculado por CalculadoraTarifa
     * @param creadaPor     operador que registra la reserva
     * @param fechaCreacion momento en que se registra (normalmente FechaUtil.ahora())
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public Reserva(String codigo, Cliente cliente, Cancha cancha, LocalDateTime inicio,
                   Duration duracion, double precioCancha, Usuario creadaPor,
                   LocalDateTime fechaCreacion) throws DatoInvalidoException {
        this(codigo, cliente, cancha, inicio, calcularFin(inicio, duracion), precioCancha,
                EstadoReserva.PENDIENTE, fechaCreacion, creadaPor, null);
    }

    /**
     * Reconstruye una reserva YA GUARDADA (desde reservas.csv), en el estado
     * que tenía. Sus servicios y pagos se agregan después con
     * {@link #restaurarServicio} y {@link #restaurarPago}.
     *
     * @param cancelacion datos de cancelación; obligatoria si y solo si el estado es CANCELADA
     * @throws DatoInvalidoException si los datos son inválidos o inconsistentes
     */
    public Reserva(String codigo, Cliente cliente, Cancha cancha, LocalDateTime inicio,
                   LocalDateTime fin, double precioCancha, EstadoReserva estado,
                   LocalDateTime fechaCreacion, Usuario creadaPor, Cancelacion cancelacion)
            throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código de reserva");
        this.cliente = exigir(cliente, "El cliente es obligatorio.");
        this.cancha = exigir(cancha, "La cancha es obligatoria.");
        this.creadaPor = exigir(creadaPor, "El usuario que registra la reserva es obligatorio.");
        this.fechaCreacion = exigir(fechaCreacion, "La fecha de creación es obligatoria.");
        this.estado = exigir(estado, "El estado de la reserva es obligatorio.");
        exigir(inicio, "La hora de inicio es obligatoria.");
        exigir(fin, "La hora de fin es obligatoria.");
        if (!inicio.isBefore(fin)) {
            throw new DatoInvalidoException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        this.inicio = inicio;
        this.fin = fin;
        this.precioCancha = MonedaUtil.redondear(Validador.validarPositivo(precioCancha, "precio de la cancha"));

        // Consistencia: solo una reserva CANCELADA tiene datos de cancelación.
        if ((estado == EstadoReserva.CANCELADA) != (cancelacion != null)) {
            throw new DatoInvalidoException("La reserva " + this.codigo
                    + ": el estado CANCELADA y los datos de cancelación deben ir juntos.");
        }
        this.cancelacion = cancelacion;
        this.servicios = new ArrayList<>();
        this.pagos = new ArrayList<>();
    }

    // =====================================================================
    //  GETTERS
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public Duration getDuracion() {
        return Duration.between(inicio, fin);
    }

    /** Duración en horas con decimales (ej. 1.5). */
    public double getHoras() {
        return FechaUtil.horasEntre(inicio, fin);
    }

    public double getPrecioCancha() {
        return precioCancha;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Usuario getCreadaPor() {
        return creadaPor;
    }

    /** @return lista de solo lectura; para agregar use {@link #agregarServicio} */
    public List<ServicioReserva> getServicios() {
        return Collections.unmodifiableList(servicios);
    }

    /** @return lista de solo lectura; para agregar use {@link #registrarPago} */
    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    /** @return datos de cancelación, o null si no está cancelada */
    public Cancelacion getCancelacion() {
        return cancelacion;
    }

    // =====================================================================
    //  REGLAS DE HORARIO
    // =====================================================================

    /**
     * REGLA DE TRASLAPE DEL PDF: existe conflicto si
     * inicioNuevo &lt; finExistente  Y  finNuevo &gt; inicioExistente.
     *
     * Las comparaciones son estrictas: una reserva que empieza justo
     * cuando esta termina (17:00 = 17:00) NO choca.
     *
     * @param inicioNuevo inicio del rango a comparar
     * @param finNuevo    fin del rango a comparar
     * @return true si los rangos se superponen, aunque sea un minuto
     */
    public boolean seTraslapaCon(LocalDateTime inicioNuevo, LocalDateTime finNuevo) {
        return inicioNuevo.isBefore(this.fin) && finNuevo.isAfter(this.inicio);
    }

    /**
     * Una reserva cancelada libera su horario; cualquier otra lo ocupa.
     *
     * @return true si esta reserva debe tomarse en cuenta al buscar conflictos
     */
    public boolean ocupaHorario() {
        return estado != EstadoReserva.CANCELADA;
    }

    /**
     * Indica si la reserva se está jugando en este momento: el instante cae
     * en [inicio, fin) y la reserva sigue activa (no cancelada ni finalizada).
     *
     * El mapa de canchas usa este método para pintar una cancha "En curso".
     */
    public boolean estaEnCurso(LocalDateTime ahora) {
        return !estado.esTerminal()
                && !ahora.isBefore(inicio)
                && ahora.isBefore(fin);
    }

    // =====================================================================
    //  SERVICIOS ADICIONALES
    // =====================================================================

    /**
     * Agrega un servicio. Si ya estaba en la reserva, suma la cantidad en
     * lugar de crear una línea repetida. Si se cobra por hora, usa la
     * duración de la reserva.
     *
     * @throws DatoInvalidoException si la reserva ya no admite cambios o el servicio es inválido
     */
    public void agregarServicio(ServicioAdicional servicio, int cantidad) throws DatoInvalidoException {
        exigirModificable("agregar servicios");
        if (servicio == null) {
            throw new DatoInvalidoException("El servicio es obligatorio.");
        }
        for (ServicioReserva linea : servicios) {
            if (linea.getServicio().equals(servicio)) {
                linea.aumentarCantidad(cantidad);
                return;
            }
        }
        servicios.add(new ServicioReserva(servicio, cantidad, getHoras()));
    }

    /**
     * Quita por completo un servicio de la reserva.
     *
     * @throws DatoInvalidoException si no está en la reserva, la reserva no admite
     *                               cambios o el nuevo total quedaría por debajo de lo ya pagado
     */
    public void quitarServicio(String codigoServicio) throws DatoInvalidoException {
        exigirModificable("quitar servicios");
        ServicioReserva encontrada = null;
        for (ServicioReserva linea : servicios) {
            if (linea.getServicio().getCodigo().equals(codigoServicio)) {
                encontrada = linea;
                break;
            }
        }
        if (encontrada == null) {
            throw new DatoInvalidoException("El servicio " + codigoServicio + " no está en la reserva.");
        }
        double nuevoTotal = calcularTotal() - encontrada.getSubtotal();
        if (MonedaUtil.esMayor(calcularTotalPagado(), nuevoTotal)) {
            throw new DatoInvalidoException("No se puede quitar el servicio: lo pagado ("
                    + MonedaUtil.formatear(calcularTotalPagado()) + ") superaría el nuevo total ("
                    + MonedaUtil.formatear(nuevoTotal) + ").");
        }
        servicios.remove(encontrada);
    }

    /**
     * Solo para la carga desde CSV: agrega un servicio guardado sin
     * validar el estado (las reservas finalizadas también tienen servicios).
     */
    public void restaurarServicio(ServicioReserva linea) throws DatoInvalidoException {
        if (linea == null) {
            throw new DatoInvalidoException("El servicio a restaurar es obligatorio.");
        }
        servicios.add(linea);
    }

    // =====================================================================
    //  REGLAS DE COBRO
    // =====================================================================

    public double calcularTotalServicios() {
        return Cobrable.sumarSubtotales(servicios);
    }

    /** Total = precio de la cancha + servicios adicionales. */
    public double calcularTotal() {
        return MonedaUtil.redondear(precioCancha + calcularTotalServicios());
    }

    public double calcularTotalPagado() {
        double pagado = 0;
        for (Pago pago : pagos) {
            pagado += pago.getMonto();
        }
        return MonedaUtil.redondear(pagado);
    }

    /** Saldo = total - pagado. Nunca es negativo. */
    public double calcularSaldo() {
        double saldo = calcularTotal() - calcularTotalPagado();
        return MonedaUtil.esCero(saldo) || saldo < 0 ? 0.0 : MonedaUtil.redondear(saldo);
    }

    /** Prueba #7 del PDF: después del pago total, el saldo es Q0. */
    public boolean estaPagada() {
        return MonedaUtil.esCero(calcularSaldo());
    }

    /** Porcentaje pagado del total, de 0 a 100. Útil para exigir un anticipo mínimo. */
    public double calcularPorcentajePagado() {
        double total = calcularTotal();
        return total <= 0 ? 0 : MonedaUtil.redondear(calcularTotalPagado() * 100.0 / total);
    }

    // =====================================================================
    //  PAGOS
    // =====================================================================

    /**
     * Registra un pago o abono.
     *
     * Reglas del PDF:
     * 
     *   No se aceptan pagos negativos (lo valida el constructor de Pago).
     *   No se permite un pago mayor al saldo pendiente (prueba #8).
     *   No se aceptan pagos a una reserva cancelada.
     * 
     *
     * @throws PagoInvalidoException si el pago rompe alguna regla
     */
    public void registrarPago(Pago pago) throws PagoInvalidoException {
        if (estado == EstadoReserva.CANCELADA) {
            throw new PagoInvalidoException("No se pueden registrar pagos a una reserva cancelada.");
        }
        agregarPagoValidado(pago);
    }

    /**
     * Solo para la carga desde CSV: agrega un pago guardado. No valida el
     * estado (una reserva pudo recibir un anticipo y cancelarse después),
     * pero sí que no se exceda el total.
     */
    public void restaurarPago(Pago pago) throws PagoInvalidoException {
        agregarPagoValidado(pago);
    }

    private void agregarPagoValidado(Pago pago) throws PagoInvalidoException {
        if (pago == null) {
            throw new PagoInvalidoException("El pago es obligatorio.");
        }
        for (Pago existente : pagos) {
            if (existente.getCodigo().equals(pago.getCodigo())) {
                throw new PagoInvalidoException("El pago " + pago.getCodigo() + " ya está registrado.");
            }
        }
        double saldo = calcularSaldo();
        if (MonedaUtil.esMayor(pago.getMonto(), saldo)) {
            throw new PagoInvalidoException("El pago de " + MonedaUtil.formatear(pago.getMonto())
                    + " supera el saldo pendiente de " + MonedaUtil.formatear(saldo) + ".");
        }
        pagos.add(pago);
    }

    // =====================================================================
    //  REGLAS DE ESTADO
    // =====================================================================

    /**
     * PENDIENTE → CONFIRMADA.
     *
     * @param porcentajeAnticipoMinimo porcentaje del total que debe estar pagado
     *        para confirmar (0 = sin requisito; 25 = anticipo mínimo del 25%)
     * @throws TransicionEstadoInvalidaException si no está PENDIENTE
     * @throws PagoInvalidoException            si no cubre el anticipo mínimo
     */
    public void confirmar(double porcentajeAnticipoMinimo)
            throws TransicionEstadoInvalidaException, PagoInvalidoException {
        validarTransicion(EstadoReserva.CONFIRMADA);
        if (calcularPorcentajePagado() + MonedaUtil.TOLERANCIA < porcentajeAnticipoMinimo) {
            double requerido = MonedaUtil.redondear(calcularTotal() * porcentajeAnticipoMinimo / 100.0);
            throw new PagoInvalidoException("Para confirmar se requiere un anticipo mínimo de "
                    + MonedaUtil.formatear(requerido)
                    + " (" + formatearPorcentaje(porcentajeAnticipoMinimo) + "%). "
                    + "Pagado: " + MonedaUtil.formatear(calcularTotalPagado()) + ".");
        }
        estado = EstadoReserva.CONFIRMADA;
    }

    /** CONFIRMADA → EN_CURSO (los jugadores llegaron a la cancha). */
    public void iniciar() throws TransicionEstadoInvalidaException {
        validarTransicion(EstadoReserva.EN_CURSO);
        estado = EstadoReserva.EN_CURSO;
    }

    /** EN_CURSO → FINALIZADA. Se permite aunque haya saldo: el reporte de pagos pendientes lo mostrará. */
    public void finalizar() throws TransicionEstadoInvalidaException {
        validarTransicion(EstadoReserva.FINALIZADA);
        estado = EstadoReserva.FINALIZADA;
    }

    /**
     * Cancela la reserva SIN eliminarla: cambia el estado a CANCELADA y guarda
     * fecha, motivo y responsable. Su horario queda libre para otras reservas.
     *
     * @throws TransicionEstadoInvalidaException si ya está finalizada, en curso o cancelada
     * @throws DatoInvalidoException            si falta el motivo, la fecha o el responsable
     */
    public void cancelar(String motivo, Usuario responsable, LocalDateTime fecha)
            throws TransicionEstadoInvalidaException, DatoInvalidoException {
        validarTransicion(EstadoReserva.CANCELADA);
        Cancelacion nueva = new Cancelacion(fecha, motivo, responsable);
        this.cancelacion = nueva;
        this.estado = EstadoReserva.CANCELADA;
    }

    private void validarTransicion(EstadoReserva destino) throws TransicionEstadoInvalidaException {
        if (!estado.puedeTransicionarA(destino)) {
            throw new TransicionEstadoInvalidaException("La reserva " + codigo + " está "
                    + estado.getDescripcion().toUpperCase(Locale.ROOT) + " y no puede pasar a "
                    + destino.getDescripcion().toUpperCase(Locale.ROOT) + ".");
        }
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private void exigirModificable(String accion) throws DatoInvalidoException {
        if (estado.esTerminal()) {
            throw new DatoInvalidoException("No se pueden " + accion + " en una reserva "
                    + estado.getDescripcion().toLowerCase(Locale.ROOT) + ".");
        }
    }

    private static LocalDateTime calcularFin(LocalDateTime inicio, Duration duracion)
            throws DatoInvalidoException {
        exigir(inicio, "La hora de inicio es obligatoria.");
        exigir(duracion, "La duración es obligatoria.");
        long minutos = duracion.toMinutes();
        if (minutos < DURACION_MINIMA_MINUTOS) {
            throw new DatoInvalidoException("La duración mínima es de " + DURACION_MINIMA_MINUTOS + " minutos.");
        }
        if (minutos % BLOQUE_MINUTOS != 0) {
            throw new DatoInvalidoException("La duración debe ser en bloques de " + BLOQUE_MINUTOS + " minutos.");
        }
        return inicio.plus(duracion);
    }

    /** 25.0 -> "25"; 12.5 -> "12.5" */
    private static String formatearPorcentaje(double porcentaje) {
        if (porcentaje == Math.floor(porcentaje)) {
            return String.valueOf((long) porcentaje);
        }
        return String.valueOf(porcentaje);
    }

    private static <T> T exigir(T valor, String mensaje) throws DatoInvalidoException {
        if (valor == null) {
            throw new DatoInvalidoException(mensaje);
        }
        return valor;
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Reserva)) {
            return false;
        }
        return codigo.equals(((Reserva) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Ej.: "R0003 - Luis Hernández - La Bombonera 06/10/2026 15:00 - 17:00 (En curso)". */
    @Override
    public String toString() {
        return codigo + " - " + cliente.getNombre() + " - " + cancha.getNombre() + " "
                + FechaUtil.formatearRango(inicio, fin) + " (" + estado + ")";
    }
}

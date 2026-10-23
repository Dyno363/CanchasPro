/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.tienda;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.StockInsuficienteException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.Cobrable;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
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
 * Venta del punto de venta (POS) de la Tienda / Cafetería.
 *
 * Funciona como un carrito en dos pasos:
 *
 *  Armar {@link #agregarProducto} / {@link #quitarProducto}. El inventario
 *       todavía NO cambia; solo se verifica que alcance.
 *  Confirmar {@link #confirmar}. Recién aquí se descuenta el inventario
 *       de todos los productos (prueba #13 del PDF) y la venta queda cerrada.
 *
 *
 * Módulo opcional: la Venta conoce a la Reserva (0..1), pero la Reserva
 * NO conoce a la Venta. Así el núcleo de reservas funciona igual con
 * {@code modulo_tienda=false}.
 */
public class Venta {
    private final String numero;
    private final LocalDateTime fechaHora;
    private final Reserva reserva;
    private final Usuario vendedor;
    private final List<DetalleVenta> detalles;
    private MetodoPago metodoPago;
    private LocalDateTime fechaPago;
    private boolean confirmada;

    /**
     * Abre una venta NUEVA (carrito vacío, sin confirmar).
     *
     * @param numero    número único, ej. "V0003"
     * @param fechaHora momento de la venta
     * @param reserva   reserva asociada (consumo en cancha) o null si es venta independiente
     * @param vendedor  operador que atiende
     * @throws DatoInvalidoException si faltan datos o la reserva está cancelada o finalizada
     */
   public Venta(String numero, LocalDateTime fechaHora, Reserva reserva, Usuario vendedor)
            throws DatoInvalidoException {
        this(numero, fechaHora, validarReservaActiva(reserva), vendedor, null, null, false);
    }

    /**
     * Reconstruye una venta YA CONFIRMADA y PAGADA en el momento (formato anterior de ventas.csv).
     */
    public Venta(String numero, LocalDateTime fechaHora, Reserva reserva, Usuario vendedor,
                 MetodoPago metodoPago) throws DatoInvalidoException {
        this(numero, fechaHora, reserva, vendedor, exigirMetodo(metodoPago), fechaHora, true);
    }

    /**
     * Reconstruye una venta YA CONFIRMADA (desde ventas.csv).
     *
     * @param metodoPago null = venta a cuenta sin pagar (solo válido si tiene reserva)
     * @param fechaPago  cuándo se cobró (null si sigue pendiente; si viene null en una
     *                   venta pagada se asume la fecha de la venta)
     */
    public Venta(String numero, LocalDateTime fechaHora, Reserva reserva, Usuario vendedor,
                 MetodoPago metodoPago, LocalDateTime fechaPago) throws DatoInvalidoException {
        this(numero, fechaHora, reserva, vendedor, metodoPago,
                metodoPago == null ? null : (fechaPago == null ? fechaHora : fechaPago), true);
        if (metodoPago == null && reserva == null) {
            throw new DatoInvalidoException("La venta " + numero
                    + " está pendiente de pago pero no tiene reserva a la cual cargarse.");
        }
    }

    private Venta(String numero, LocalDateTime fechaHora, Reserva reserva, Usuario vendedor,
                  MetodoPago metodoPago, LocalDateTime fechaPago, boolean confirmada) throws DatoInvalidoException {
        this.numero = Validador.validarRequerido(numero, "número de venta");
        if (fechaHora == null) {
            throw new DatoInvalidoException("La fecha de la venta es obligatoria.");
        }
        if (vendedor == null) {
            throw new DatoInvalidoException("El vendedor es obligatorio.");
        }
        this.fechaHora = fechaHora;
        this.reserva = reserva;
        this.vendedor = vendedor;
        this.metodoPago = metodoPago;
        this.fechaPago = fechaPago;
        this.confirmada = confirmada;
        this.detalles = new ArrayList<>();
    }

    // =====================================================================
    //  GETTERS
    // =====================================================================

    public String getNumero() {
        return numero;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /** @return la reserva asociada, o null si es una venta independiente */
    public Reserva getReserva() {
        return reserva;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    /** @return lista de solo lectura; para modificar use agregarProducto / quitarProducto */
    public List<DetalleVenta> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /** @return método de pago, o null mientras la venta no esté confirmada */
    public MetodoPago getMetodoPago() {
        return metodoPago;
    }
    /** @return cuándo entró el dinero, o null si todavía no se ha cobrado */
    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    /** @return true si se cerró "a cuenta" de la reserva y todavía no se cobra */
    public boolean isPendienteDePago() {
        return confirmada && metodoPago == null;
    }

    /** @return true si ya está confirmada y cobrada (cuenta como dinero en caja) */
    public boolean isPagada() {
        return confirmada && metodoPago != null;
    }
    public boolean isConfirmada() {
        return confirmada;
    }

    public boolean estaAsociadaAReserva() {
        return reserva != null;
    }

    // =====================================================================
    //  CARRITO
    // =====================================================================

    /**
     * Agrega un producto al carrito. Si ya estaba, suma la cantidad.
     * Verifica que la existencia alcance para TODO lo que lleva el carrito
     * de ese producto, pero todavía no descuenta inventario.
     *
     * @throws StockInsuficienteException si no hay unidades suficientes (prueba #14)
     * @throws DatoInvalidoException      si la venta ya se confirmó, el producto está
     *                                    inactivo o la cantidad no es positiva
     */
    public void agregarProducto(Producto producto, int cantidad)
            throws StockInsuficienteException, DatoInvalidoException {
        exigirAbierta();
        Validador.validarEnteroPositivo(cantidad, "cantidad");
        if (producto == null) {
            throw new DatoInvalidoException("El producto es obligatorio.");
        }
        DetalleVenta existente = buscarDetalle(producto.getCodigo());
        int totalPedido = cantidad + (existente == null ? 0 : existente.getCantidad());
        if (!producto.hayStock(totalPedido)) {
            throw new StockInsuficienteException("Stock insuficiente de " + producto.getNombre()
                    + ": se pidieron " + totalPedido + " y hay " + producto.getExistencia() + ".");
        }
        if (existente != null) {
            existente.agregarCantidad(cantidad);
        } else {
            detalles.add(new DetalleVenta(producto, cantidad));
        }
    }

    /** Quita del carrito todas las unidades de un producto. */
    public void quitarProducto(String codigoProducto) throws DatoInvalidoException {
        exigirAbierta();
        DetalleVenta existente = buscarDetalle(codigoProducto);
        if (existente == null) {
            throw new DatoInvalidoException("El producto " + codigoProducto + " no está en la venta.");
        }
        detalles.remove(existente);
    }

    /** Total = suma de los subtotales de cada línea. */
    public double calcularTotal() {
        return Cobrable.sumarSubtotales(detalles);
    }

    /** Total de unidades vendidas (útil para reportes). */
    public int contarUnidades() {
        int unidades = 0;
        for (DetalleVenta detalle : detalles) {
            unidades += detalle.getCantidad();
        }
        return unidades;
    }

    // =====================================================================
    //  CONFIRMACIÓN
    // =====================================================================

     /**
     * Cierra la venta COBRÁNDOLA en el momento y descuenta el inventario de todos sus productos.
     * Primero verifica TODAS las líneas y solo después descuenta ("todo o nada").
     */
    public void confirmar(MetodoPago metodo) throws StockInsuficienteException, DatoInvalidoException {
        exigirMetodo(metodo);
        cerrarYDescontar();
        this.metodoPago = metodo;
        this.fechaPago = fechaHora;
    }

    /**
     * Cierra la venta SIN cobrarla: el consumo queda como saldo pendiente de la
     * reserva. El inventario se descuenta igual, porque el producto ya se entregó.
     */
    public void confirmarACuenta() throws StockInsuficienteException, DatoInvalidoException {
        if (reserva == null) {
            throw new DatoInvalidoException("Solo una venta cargada a una reserva puede quedar a cuenta. "
                    + "En mostrador se cobra al momento.");
        }
        cerrarYDescontar();
        this.metodoPago = null;
        this.fechaPago = null;
    }

    /**
     * Cobra una venta que quedó a cuenta.
     *
     * @param fecha momento del cobro (cuenta como ingreso de ESE día)
     */
    public void liquidar(MetodoPago metodo, LocalDateTime fecha) throws DatoInvalidoException {
        if (!isPendienteDePago()) {
            throw new DatoInvalidoException("La venta " + numero + " no tiene un saldo pendiente.");
        }
        exigirMetodo(metodo);
        if (fecha == null) {
            throw new DatoInvalidoException("La fecha del cobro es obligatoria.");
        }
        this.metodoPago = metodo;
        this.fechaPago = fecha;
    }

    /** Parte común de confirmar y confirmarACuenta: "todo o nada" con el inventario. */
    private void cerrarYDescontar() throws StockInsuficienteException, DatoInvalidoException {
        exigirAbierta();
        if (detalles.isEmpty()) {
            throw new DatoInvalidoException("La venta no tiene productos.");
        }
        // 1) Verificar todo
        for (DetalleVenta detalle : detalles) {
            if (!detalle.getProducto().hayStock(detalle.getCantidad())) {
                throw new StockInsuficienteException("Stock insuficiente de "
                        + detalle.getProducto().getNombre() + ": se pidieron " + detalle.getCantidad()
                        + " y hay " + detalle.getProducto().getExistencia() + ".");
            }
        }
        // 2) Descontar todo (ya sabemos que alcanza)
        for (DetalleVenta detalle : detalles) {
            detalle.getProducto().descontarStock(detalle.getCantidad());
        }
        this.confirmada = true;
    }

    /**
     * Solo para la carga desde CSV: agrega una línea guardada. No toca el
     * inventario, porque productos.csv ya guarda la existencia actualizada.
     */
    public void restaurarDetalle(DetalleVenta detalle) throws DatoInvalidoException {
        if (detalle == null) {
            throw new DatoInvalidoException("El detalle a restaurar es obligatorio.");
        }
        detalles.add(detalle);
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private DetalleVenta buscarDetalle(String codigoProducto) {
        for (DetalleVenta detalle : detalles) {
            if (detalle.getProducto().getCodigo().equals(codigoProducto)) {
                return detalle;
            }
        }
        return null;
    }

    private void exigirAbierta() throws DatoInvalidoException {
        if (confirmada) {
            throw new DatoInvalidoException("La venta " + numero + " ya fue confirmada y no se puede modificar.");
        }
    }

    private static MetodoPago exigirMetodo(MetodoPago metodo) throws DatoInvalidoException {
        if (metodo == null) {
            throw new DatoInvalidoException("El método de pago es obligatorio.");
        }
        return metodo;
    }

    private static Reserva validarReservaActiva(Reserva reserva) throws DatoInvalidoException {
        if (reserva != null && reserva.getEstado().esTerminal()) {
            throw new DatoInvalidoException("No se puede cargar consumo a la reserva " + reserva.getCodigo()
                    + " porque está " + reserva.getEstado().getDescripcion().toLowerCase(Locale.ROOT) + ".");
        }
        return reserva;
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Venta)) {
            return false;
        }
        return numero.equals(((Venta) otro).numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    /** Ej.: "V0001 - 05/10/2026 20:10 - Q40.00 (Reserva R0002)". */
    @Override
    public String toString() {
        return numero + " - " + FechaUtil.formatearFechaHora(fechaHora) + " - "
                + MonedaUtil.formatear(calcularTotal())
                + (reserva != null ? " (Reserva " + reserva.getCodigo() + ")" : "");
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del PUNTO DE VENTA (POS) de la tienda/cafetería.
 *
 *  Se abre de dos formas:
 *   Desde el MAPA, al hacer clic en una cancha "En curso": la venta queda
 *       ligada a esa reserva ({@link #iniciarVentaParaReserva}). 
 *   Desde el módulo Tienda, como venta de mostrador sin reserva
 *       ({@link #iniciarVentaMostrador}). 
 *  
 *
 * El carrito vive aquí, en memoria ({@code ventaActual}). Mientras no se
 * confirme, el inventario NO se toca y la venta NO existe en el complejo: si el
 * operador cierra el POS, la venta simplemente se descarta.
 *
 * Al confirmar: {@code Venta.confirmar} verifica TODO el stock y luego descuenta
 * ("todo o nada", prueba #13) y recién entonces se guarda en el complejo.
 * Pedir más de lo que hay se rechaza desde que se agrega al carrito (prueba #14).
 */
public class PosController {
     private final ComplejoDeportivo complejo;
    private final Sesion sesion;

    /** Venta abierta (carrito). null = no hay venta en curso. */
    private Venta ventaActual;

    public PosController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }

    // =====================================================================
    //  ABRIR VENTA
    // =====================================================================

    /**
     * Abre un carrito ligado a una reserva (consumo en cancha).
     *
     * @throws CanchaProException si la tienda está apagada, ya hay una venta abierta,
     *                            o la reserva está cancelada/finalizada
     */
    public Venta iniciarVentaParaReserva(String codigoReserva) throws CanchaProException {
        Reserva reserva = complejo.buscarReserva(codigoReserva)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la reserva " + codigoReserva + "."));
        return abrir(reserva);
    }

    /** Abre un carrito de mostrador (sin reserva). */
    public Venta iniciarVentaMostrador() throws CanchaProException {
        return abrir(null);
    }

    // =====================================================================
    //  CARRITO
    // =====================================================================

    /**
     * Agrega unidades de un producto. Si ya estaba, suma la cantidad.
     *
     * @throws CanchaProException si no hay venta abierta, el producto no está a la venta
     *                            o no hay existencia suficiente (prueba #14)
     */
    public Venta agregarProducto(String codigoProducto, int cantidad) throws CanchaProException {
        Venta venta = exigirVentaAbierta();
        Producto producto = complejo.buscarProducto(codigoProducto)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el producto " + codigoProducto + "."));
        venta.agregarProducto(producto, cantidad);
        return venta;
    }

    public Venta quitarProducto(String codigoProducto) throws CanchaProException {
        Venta venta = exigirVentaAbierta();
        venta.quitarProducto(codigoProducto);
        return venta;
    }

    /** @return líneas del carrito para llenar la tabla del POS (vacía si no hay venta) */
    public List<DetalleVenta> getLineas() {
        return ventaActual == null ? new ArrayList<>() : ventaActual.getDetalles();
    }

    public double getTotal() {
        return ventaActual == null ? 0 : ventaActual.calcularTotal();
    }

    public boolean hayVentaAbierta() {
        return ventaActual != null;
    }

    public Venta getVentaActual() {
        return ventaActual;
    }

    /** Título del diálogo. Ej.: "POS - R0006 Equipo Los Halcones (El Maracaná)" o "POS - Mostrador". */
    public String getTitulo() {
        if (ventaActual == null || !ventaActual.estaAsociadaAReserva()) {
            return "POS - Mostrador";
        }
        Reserva r = ventaActual.getReserva();
        return "POS - " + r.getCodigo() + " " + r.getCliente().getNombre() + " (" + r.getCancha().getNombre() + ")";
    }

    // =====================================================================
    //  CERRAR VENTA
    // =====================================================================

    /**
     * Cobra la venta: descuenta inventario (prueba #13) y la guarda en memoria.
     *
     * @return la venta confirmada (para mostrar el comprobante)
     */
    public Venta confirmar(MetodoPago metodo) throws CanchaProException {
        Venta venta = exigirVentaAbierta();
        venta.confirmar(metodo);       // 1) verifica todo el stock, 2) descuenta todo
        complejo.agregarVenta(venta);  // 3) ahora sí existe en el sistema
        ventaActual = null;
        return venta;
    }
    /**
     * Cierra la venta SIN cobrarla: el consumo queda como saldo pendiente de la
     * reserva y se cobra después desde la ventana de Pagos. El inventario sí baja.
     */
    public Venta confirmarACuenta() throws CanchaProException {
        Venta venta = exigirVentaAbierta();
        venta.confirmarACuenta();
        complejo.agregarVenta(venta);
        ventaActual = null;
        return venta;
    }

    /** La vista lo usa para mostrar u ocultar el botón "Dejar a cuenta". */
    public boolean puedeDejarACuenta() {
        return ventaActual != null && ventaActual.estaAsociadaAReserva();
    }
    /** Descarta el carrito. No hay nada que deshacer: el inventario nunca se tocó. */
    public void cancelarVenta() {
        ventaActual = null;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private Venta abrir(Reserva reserva) throws CanchaProException {
        if (!complejo.getConfiguracion().isModuloTiendaActivo()) {
            throw new CanchaProException("El módulo de Tienda/Cafetería está desactivado en la configuración.");
        }
        if (ventaActual != null) {
            throw new CanchaProException("Ya hay una venta abierta (" + ventaActual.getNumero()
                    + "). Confírmela o cancélela primero.");
        }
        // El constructor de Venta rechaza reservas canceladas o finalizadas.
        ventaActual = new Venta(complejo.generarCodigo(GeneradorCodigo.Tipo.VENTA), FechaUtil.ahora(),
                reserva, sesion.getUsuario());
        return ventaActual;
    }

    private Venta exigirVentaAbierta() throws DatoInvalidoException {
        if (ventaActual == null) {
            throw new DatoInvalidoException("No hay una venta abierta. Inicie una venta primero.");
        }
        return ventaActual;
    }
}

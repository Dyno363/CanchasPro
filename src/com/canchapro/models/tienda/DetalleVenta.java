/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.tienda;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.reservas.Cobrable;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
/**
 *
 * @author SANTANA
 */
/**
 * Una línea de una venta: qué producto, cuántas unidades y a qué precio.
 *
 * Guarda una "foto" del precio del producto al momento de la venta, igual
 * que ServicioReserva. Si mañana el agua sube de precio, las ventas
 * anteriores no cambian.
 *
 * Implementa {@link Cobrable}: un comprobante puede mezclar estas líneas
 * con los servicios de una reserva y tratarlas igual.
 */
public class DetalleVenta implements Cobrable {
    private final Producto producto;
    private int cantidad;
    private final double precioUnitario;

    /**
     * Línea NUEVA en el carrito del punto de venta, con el precio actual del producto.
     *
     * @throws DatoInvalidoException si el producto está inactivo o la cantidad no es positiva
     */
    public DetalleVenta(Producto producto, int cantidad) throws DatoInvalidoException {
        this(validarVendible(producto), cantidad, producto.getPrecio());
    }

    /**
     * Reconstruye una línea YA GUARDADA (desde detalle_ventas.csv) con su precio
     * original. No exige que el producto siga activo: el historial debe cargarse completo.
     *
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public DetalleVenta(Producto producto, int cantidad, double precioUnitario) throws DatoInvalidoException {
        if (producto == null) {
            throw new DatoInvalidoException("El producto es obligatorio.");
        }
        this.producto = producto;
        this.cantidad = Validador.validarEnteroPositivo(cantidad, "cantidad");
        this.precioUnitario = MonedaUtil.redondear(Validador.validarPositivo(precioUnitario, "precio unitario"));
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /** Suma unidades si el mismo producto se agrega otra vez al carrito. */
    public void agregarCantidad(int extra) throws DatoInvalidoException {
        this.cantidad += Validador.validarEnteroPositivo(extra, "cantidad adicional");
    }

    /** Subtotal = precio unitario × cantidad. */
    @Override
    public double getSubtotal() {
        return MonedaUtil.redondear(precioUnitario * cantidad);
    }

    /** Ej.: "Agua pura 600 ml x4". */
    @Override
    public String getConcepto() {
        return producto.getNombre() + " x" + cantidad;
    }

    @Override
    public String toString() {
        return getConcepto() + " = " + MonedaUtil.formatear(getSubtotal());
    }

    private static Producto validarVendible(Producto producto) throws DatoInvalidoException {
        if (producto == null) {
            throw new DatoInvalidoException("El producto es obligatorio.");
        }
        if (!producto.estaDisponibleParaVenta()) {
            throw new DatoInvalidoException("El producto '" + producto.getNombre()
                    + "' no está disponible para la venta.");
        }
        return producto;
    }
}

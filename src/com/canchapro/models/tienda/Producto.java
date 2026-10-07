/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.tienda;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.EstadoProducto;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.StockInsuficienteException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */

/**
 * Producto de la Tienda / Cafetería (bebidas, snacks, golosinas, comida).
 *
 * Controla su propio inventario: la existencia solo cambia por
 * {@link #descontarStock} (al confirmar una venta) o {@link #reponerStock}
 * (al recibir mercadería). No hay un {@code setExistencia()} público, para
 * que nadie pueda dejar el inventario en un número arbitrario.
 */
public class Producto {
    private final String codigo;
    private String nombre;
    private CategoriaProducto categoria;
    private double precio;
    private int existencia;
    private int stockMinimo;
    private EstadoProducto estado;

    /**
     * @param codigo      código único, ej. "P001"
     * @param nombre      ej. "Agua pura 600 ml"
     * @param categoria   bebida, snack, golosina o comida preparada
     * @param precio      precio de venta (mayor que cero)
     * @param existencia  unidades en inventario (0 o más)
     * @param stockMinimo cantidad a partir de la cual se debe reabastecer (0 o más)
     * @param estado      ACTIVO o INACTIVO
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public Producto(String codigo, String nombre, CategoriaProducto categoria, double precio,
                    int existencia, int stockMinimo, EstadoProducto estado) throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código");
        setNombre(nombre);
        setCategoria(categoria);
        setPrecio(precio);
        this.existencia = Validador.validarNoNegativo(existencia, "existencia");
        setStockMinimo(stockMinimo);
        setEstado(estado);
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) throws DatoInvalidoException {
        this.nombre = Validador.validarRequerido(nombre, "nombre del producto");
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public final void setCategoria(CategoriaProducto categoria) throws DatoInvalidoException {
        if (categoria == null) {
            throw new DatoInvalidoException("La categoría del producto es obligatoria.");
        }
        this.categoria = categoria;
    }

    public double getPrecio() {
        return precio;
    }

    /** Cambiar el precio no altera ventas anteriores: cada DetalleVenta guardó su precio. */
    public final void setPrecio(double precio) throws DatoInvalidoException {
        this.precio = MonedaUtil.redondear(Validador.validarPositivo(precio, "precio"));
    }

    public int getExistencia() {
        return existencia;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public final void setStockMinimo(int stockMinimo) throws DatoInvalidoException {
        this.stockMinimo = Validador.validarNoNegativo(stockMinimo, "stock mínimo");
    }

    public EstadoProducto getEstado() {
        return estado;
    }

    public final void setEstado(EstadoProducto estado) throws DatoInvalidoException {
        if (estado == null) {
            throw new DatoInvalidoException("El estado del producto es obligatorio.");
        }
        this.estado = estado;
    }

    // =====================================================================
    //  INVENTARIO
    // =====================================================================

    /** @return true si hay al menos {@code cantidad} unidades en existencia */
    public boolean hayStock(int cantidad) {
        return cantidad > 0 && cantidad <= existencia;
    }

    /**
     * Resta unidades del inventario. Lo llama Venta al confirmarse.
     *
     * @throws StockInsuficienteException si se pide más de lo que hay (prueba #14 del PDF)
     * @throws DatoInvalidoException      si la cantidad no es positiva
     */
    public void descontarStock(int cantidad) throws StockInsuficienteException, DatoInvalidoException {
        Validador.validarEnteroPositivo(cantidad, "cantidad");
        if (!hayStock(cantidad)) {
            throw new StockInsuficienteException("Stock insuficiente de " + nombre + ": se pidieron "
                    + cantidad + " y hay " + existencia + ".");
        }
        existencia -= cantidad;
    }

    /** Suma unidades al inventario (compra o reabastecimiento). */
    public void reponerStock(int cantidad) throws DatoInvalidoException {
        existencia += Validador.validarEnteroPositivo(cantidad, "cantidad a reponer");
    }

    /** @return true si la existencia llegó al stock mínimo o está por debajo (hay que reabastecer) */
    public boolean estaBajoMinimo() {
        return existencia <= stockMinimo;
    }

    /** Un producto se puede vender si está activo y tiene al menos una unidad. */
    public boolean estaDisponibleParaVenta() {
        return estado == EstadoProducto.ACTIVO && existencia > 0;
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Producto)) {
            return false;
        }
        return codigo.equals(((Producto) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Ej.: "P001 - Agua pura 600 ml (Q5.00)". */
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + MonedaUtil.formatear(precio) + ")";
    }
}


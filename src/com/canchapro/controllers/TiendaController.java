/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.EstadoProducto;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.utils.FechaUtil;
import java.time.LocalDateTime;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author SANTANA
 */
/**
 * Controlador del módulo TIENDA/CAFETERÍA: catálogo de productos, inventario
 * y consulta de ventas. (El carrito y el cobro están en {@link PosController}.)
 *
 * Módulo opcional: si {@code modulo_tienda=false} en config.properties,
 * todas las operaciones se rechazan y la vista oculta el menú. Las reservas
 * siguen funcionando normal.
 *
 * Permisos: crear, editar y activar/desactivar productos es del
 * ADMINISTRADOR. Reponer inventario (recibir mercadería) lo puede hacer
 * cualquier usuario.
 *
 * El inventario NUNCA se edita a mano: solo baja al confirmar una venta y
 * solo sube con {@link #reponerStock}. Así cada unidad tiene una explicación.
 */
public class TiendaController {
 
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;
 
    public TiendaController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }
 
    // =====================================================================
    //  ESTADO DEL MÓDULO
    // =====================================================================
 
    /** La vista lo usa para mostrar u ocultar "Tienda" en el menú lateral. */
    public boolean isModuloActivo() {
        return complejo.getConfiguracion().isModuloTiendaActivo();
    }
 
    public boolean puedeAdministrar() {
        return sesion.esAdministrador();
    }
 
    // =====================================================================
    //  CONSULTAS DE PRODUCTOS
    // =====================================================================
 
    /** @return todos los productos ordenados por nombre */
    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>(complejo.getProductos());
        lista.sort(Comparator.comparing(p -> Validador.normalizarTexto(p.getNombre())));
        return lista;
    }
 
    /** @return solo los que se pueden vender ahora (activos y con existencia) */
    public List<Producto> listarDisponibles() {
        List<Producto> disponibles = new ArrayList<>();
        for (Producto p : listarProductos()) {
            if (p.estaDisponibleParaVenta()) {
                disponibles.add(p);
            }
        }
        return disponibles;
    }
 
    /** Búsqueda por código o nombre (sin tildes ni mayúsculas); categoría null = todas. */
    public List<Producto> buscar(String criterio, CategoriaProducto categoria) {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : listarProductos()) {
            boolean coincideTexto = Validador.esVacio(criterio)
                    || Validador.contiene(p.getCodigo(), criterio)
                    || Validador.contiene(p.getNombre(), criterio);
            boolean coincideCategoria = categoria == null || p.getCategoria() == categoria;
            if (coincideTexto && coincideCategoria) {
                resultado.add(p);
            }
        }
        return resultado;
    }
 
    public Producto obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarProducto(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el producto " + codigo + "."));
    }
 
    /** Alerta de reabastecimiento (indicador del dashboard y reporte de inventario). */
    public List<Producto> getProductosBajoMinimo() {
        return complejo.getProductosBajoMinimo();
    }
 
    public CategoriaProducto[] getCategorias() {
        return CategoriaProducto.values();
    }
 
    public EstadoProducto[] getEstados() {
        return EstadoProducto.values();
    }
 
    // =====================================================================
    //  OPERACIONES SOBRE PRODUCTOS
    // =====================================================================
 
    /** Registra un producto nuevo (queda ACTIVO). Solo administrador. */
    public Producto crearProducto(String nombre, CategoriaProducto categoria, double precio,
                                  int existenciaInicial, int stockMinimo) throws CanchaProException {
        exigirModuloActivo();
        sesion.exigirAdministrador("registrar productos");
        exigirNombreLibre(nombre, null);
        Producto producto = new Producto(complejo.generarCodigo(GeneradorCodigo.Tipo.PRODUCTO), nombre,
                categoria, precio, existenciaInicial, stockMinimo, EstadoProducto.ACTIVO);
        complejo.agregarProducto(producto);
        return producto;
    }
 
    /**
     * Modifica datos del catálogo. La EXISTENCIA no se edita aquí (ver {@link #reponerStock}).
     * Cambiar el precio no afecta ventas pasadas: cada detalle guardó su precio.
     */
    public Producto actualizarProducto(String codigo, String nombre, CategoriaProducto categoria,
                                       double precio, int stockMinimo) throws CanchaProException {
        exigirModuloActivo();
        sesion.exigirAdministrador("modificar productos");
        Producto producto = obtener(codigo);
 
        // 1) Validar todo con un objeto de prueba
        new Producto(codigo, nombre, categoria, precio, producto.getExistencia(), stockMinimo, producto.getEstado());
        exigirNombreLibre(nombre, producto);
 
        // 2) Aplicar
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setPrecio(precio);
        producto.setStockMinimo(stockMinimo);
        return producto;
    }
 
    /** Un producto INACTIVO deja de ofrecerse en el POS, pero sigue en el historial. */
    public void cambiarEstado(String codigo, EstadoProducto estado) throws CanchaProException {
        exigirModuloActivo();
        sesion.exigirAdministrador("activar o desactivar productos");
        obtener(codigo).setEstado(estado);
    }
 
    /** Elimina un producto que nunca se vendió (si ya se vendió, desactívelo). */
    public void eliminarProducto(String codigo) throws CanchaProException {
        exigirModuloActivo();
        sesion.exigirAdministrador("eliminar productos");
        complejo.eliminarProducto(codigo);
    }
 
    /** Entrada de mercadería: suma unidades al inventario. */
    public Producto reponerStock(String codigo, int cantidad) throws CanchaProException {
        exigirModuloActivo();
        Producto producto = obtener(codigo);
        producto.reponerStock(cantidad);
        return producto;
    }
 
    // =====================================================================
    //  CONSULTAS DE VENTAS
    // =====================================================================
 
    /** @param fecha null = todas las ventas */
    public List<Venta> listarVentas(LocalDate fecha) {
        List<Venta> resultado = new ArrayList<>();
        for (Venta v : complejo.getVentas()) {
            if (fecha == null || v.getFechaHora().toLocalDate().equals(fecha)) {
                resultado.add(v);
            }
        }
        return resultado;
    }
 
    /** Total vendido en la tienda en una fecha (indicador del dashboard). */
    public double getTotalVendido(LocalDate fecha) {
        double total = 0;
        for (Venta v : listarVentas(fecha)) {
            total += v.calcularTotal();
        }
        return MonedaUtil.redondear(total);
    }
 
    /** Ventas cargadas a una reserva (detalle de la reserva). */
    public List<Venta> getVentasDeReserva(String codigoReserva) throws EntidadNoEncontradaException {
        return complejo.getVentasDeReserva(obtenerReserva(codigoReserva));
    }
 
    /** Total consumido en la tienda por una reserva. */
    public double getConsumoDeReserva(String codigoReserva) throws EntidadNoEncontradaException {
        return complejo.calcularConsumoTienda(obtenerReserva(codigoReserva));
    }
     /** Consumo de tienda que la reserva dejó "a cuenta" y todavía debe. */
    public double getConsumoPendiente(String codigoReserva) throws EntidadNoEncontradaException {
        return complejo.calcularConsumoPendiente(obtenerReserva(codigoReserva));
    }

    /**
     * Cobra TODO el consumo pendiente de una reserva con un mismo método de pago.
     * El ingreso cuenta con la fecha de HOY.
     */
    public double cobrarConsumoPendiente(String codigoReserva, MetodoPago metodo) throws CanchaProException {
        if (metodo == null) {
            throw new DatoInvalidoException("Seleccione el método de pago.");
        }
        Reserva reserva = obtenerReserva(codigoReserva);
        List<Venta> pendientes = new ArrayList<>();
        for (Venta v : complejo.getVentasDeReserva(reserva)) {
            if (v.isPendienteDePago()) {
                pendientes.add(v);
            }
        }
        if (pendientes.isEmpty()) {
            throw new DatoInvalidoException("La reserva " + codigoReserva + " no tiene consumo de tienda pendiente.");
        }
        LocalDateTime ahora = FechaUtil.ahora();
        double total = 0;
        for (Venta v : pendientes) {
            v.liquidar(metodo, ahora);
            total += v.calcularTotal();
        }
        return MonedaUtil.redondear(total);
    }
 
    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================
 
    private void exigirModuloActivo() throws CanchaProException {
        if (!isModuloActivo()) {
            throw new CanchaProException("El módulo de Tienda/Cafetería está desactivado en la configuración.");
        }
    }
 
    private Reserva obtenerReserva(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarReserva(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la reserva " + codigo + "."));
    }
 
    private void exigirNombreLibre(String nombre, Producto actual) throws DatoInvalidoException {
        if (Validador.esVacio(nombre)) {
            return; // el constructor dará el mensaje de campo obligatorio
        }
        String buscado = Validador.normalizarTexto(nombre);
        for (Producto otro : complejo.getProductos()) {
            if (!otro.equals(actual) && Validador.normalizarTexto(otro.getNombre()).equals(buscado)) {
                throw new DatoInvalidoException("Ya existe el producto " + otro + ".");
            }
        }
    }
}
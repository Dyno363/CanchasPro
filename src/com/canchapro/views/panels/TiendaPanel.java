/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TiendaController;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.EstadoProducto;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import com.canchapro.views.components.RenderizadorPildora;
import com.canchapro.views.dialogs.ProductoDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;
/**
 *
 * @author SANTANA
 */
public class TiendaPanel extends PanelListado<Producto>{
    private static final long serialVersionUID = 1L;

    /** Nivel del inventario de un producto (también sirve para ordenar: agotado primero). */
    private enum NivelStock {
        AGOTADO("Agotado"), BAJO("Bajo mínimo"), SUFICIENTE("Suficiente");

        private final String texto;

        NivelStock(String texto) {
            this.texto = texto;
        }

        static NivelStock de(Producto p) {
            if (p.getExistencia() == 0) {
                return AGOTADO;
            }
            return p.estaBajoMinimo() ? BAJO : SUFICIENTE;
        }
    }

    private final transient TiendaController tienda;
    /** Se llama al pulsar "Venta de mostrador" (lo conecta el Dashboard; en la 8.3 abre el POS). */
    private final transient Runnable alVenderMostrador;

    private final JButton btnNuevo;
    private final JButton btnEditar;
    private final JButton btnReponer;
    private final JButton btnEstado;
    private final JButton btnEliminar;

    // --- Pie: alerta + ventas
    private final JLabel lblAlerta = new JLabel(" ");
    private final JLabel lblVentas = new JLabel(" ");
    private final JCheckBox chkTodas = new JCheckBox("Ver todas las ventas");
    private final JButton btnMostrador = new JButton("Venta de mostrador");
    private final ModeloTablaLista<Venta> modeloVentas = new ModeloTablaLista<>(
            new String[]{"Venta", "Fecha y hora", "Para", "Vendedor", "Unidades", "Método", "Total"},
            new Class<?>[]{String.class, LocalDateTime.class, String.class, String.class, Integer.class,
                String.class, Double.class},
            v -> new Object[]{v.getNumero(), v.getFechaHora(),
                v.estaAsociadaAReserva() ? "Reserva " + v.getReserva().getCodigo() + " · "
                        + v.getReserva().getCliente().getNombre() : "Mostrador (sin reserva)",
                v.getVendedor().getNombre(), v.contarUnidades(),
                v.getMetodoPago() == null ? "" : v.getMetodoPago().getDescripcion(), v.calcularTotal()});
    private final JTable tblVentas = new JTable(modeloVentas);
    private final JScrollPane scrVentas = new JScrollPane(tblVentas);

    public TiendaPanel(SistemaController sistema, Runnable alVenderMostrador) {
        super("Tienda / Cafetería", "Inventario de bebidas y snacks, alertas de stock y ventas.",
                crearModelo(), new int[]{60, 200, 130, 80, 80, 70, 120, 90});
        this.tienda = sistema.getTiendaController();
        this.alVenderMostrador = alVenderMostrador == null ? () -> { } : alVenderMostrador;

        btnNuevo = agregarBoton("+ Nuevo producto", this::nuevo);
        btnEditar = agregarBoton("Editar", this::editar);
        btnReponer = agregarBoton("Reponer stock", this::reponer);
        btnEstado = agregarBoton("Desactivar", this::cambiarEstado);
        btnEliminar = agregarBoton("Eliminar", this::eliminar);

        tabla.getColumnModel().getColumn(3).setCellRenderer(renderizadorMoneda());
        tabla.getColumnModel().getColumn(6).setCellRenderer(new RenderizadorPildora(
                v -> ((NivelStock) v).texto, v -> colorDe((NivelStock) v)));
        tabla.getColumnModel().getColumn(7).setCellRenderer(new RenderizadorPildora(
                v -> ((EstadoProducto) v).getDescripcion(),
                v -> Tema.color(v == EstadoProducto.ACTIVO ? ColorTema.EXITO : ColorTema.TEXTO_SECUNDARIO)));

        setPie(crearPie());
        aplicarTema();
        recargar();
    }

    private static ModeloTablaLista<Producto> crearModelo() {
        return new ModeloTablaLista<>(
                new String[]{"Código", "Producto", "Categoría", "Precio", "Existencia", "Mínimo", "Stock", "Estado"},
                new Class<?>[]{String.class, String.class, CategoriaProducto.class, Double.class, Integer.class,
                    Integer.class, NivelStock.class, EstadoProducto.class},
                p -> new Object[]{p.getCodigo(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getExistencia(),
                    p.getStockMinimo(), NivelStock.de(p), p.getEstado()});
    }

    private static Color colorDe(NivelStock nivel) {
        switch (nivel) {
            case AGOTADO: return Tema.color(ColorTema.ERROR);
            case BAJO:    return Tema.color(ColorTema.ADVERTENCIA);
            default:      return Tema.color(ColorTema.EXITO);
        }
    }

    /** Debajo del inventario: alerta de reposición + ventas del día. */
    private JPanel crearPie() {
        tblVentas.setRowSorter(new TableRowSorter<>(modeloVentas));
        tblVentas.getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                setText(valor instanceof LocalDateTime ? FechaUtil.formatearFechaHora((LocalDateTime) valor) : "");
            }
        });
        tblVentas.getColumnModel().getColumn(6).setCellRenderer(renderizadorMoneda());
        javax.swing.table.DefaultTableCellRenderer centrado = new javax.swing.table.DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tblVentas.getColumnModel().getColumn(4).setCellRenderer(centrado); // unidades
        int[] anchos = {70, 130, 300, 160, 80, 110, 90};
        for (int i = 0; i < anchos.length; i++) {
            tblVentas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        scrVentas.setPreferredSize(new Dimension(400, 170));
        chkTodas.setOpaque(false);
        chkTodas.addActionListener(e -> cargarVentas());
        btnMostrador.addActionListener(e -> alVenderMostrador.run());

        JPanel controles = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, Tema.ESPACIO, 0));
        controles.setOpaque(false);
        controles.add(chkTodas);
        controles.add(btnMostrador);
        JPanel barraVentas = new JPanel(new BorderLayout());
        barraVentas.setOpaque(false);
        barraVentas.add(lblVentas, BorderLayout.WEST);
        barraVentas.add(controles, BorderLayout.EAST);

        JPanel ventas = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        ventas.setOpaque(false);
        ventas.add(barraVentas, BorderLayout.NORTH);
        ventas.add(scrVentas, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, 0, 0, 0));
        pie.add(lblAlerta, BorderLayout.NORTH);
        pie.add(ventas, BorderLayout.CENTER);
        return pie;
    }

    // =====================================================================
    //  PLANTILLA (PanelListado)
    // =====================================================================

    /** El administrador maneja el catálogo; el operador solo repone y vende. */
    @Override
    protected List<Producto> cargar() {
        boolean admin = tienda.puedeAdministrar();
        btnNuevo.setVisible(admin);
        btnEditar.setVisible(admin);
        btnEstado.setVisible(admin);
        btnEliminar.setVisible(admin);
        actualizarAlerta();
        cargarVentas();
        return tienda.listarProductos();
    }

    @Override
    protected String textoConteo(List<Producto> lista) {
        return lista.size() + " productos  ·  " + tienda.listarDisponibles().size() + " a la venta  ·  "
                + tienda.getProductosBajoMinimo().size() + " por reponer";
    }

    /** "Desactivar" para uno activo, "Activar" para uno inactivo. */
    @Override
    protected void alCambiarSeleccion(Producto p) {
        btnEditar.setEnabled(p != null);
        btnReponer.setEnabled(p != null);
        btnEstado.setEnabled(p != null);
        btnEliminar.setEnabled(p != null);
        btnEstado.setText(p != null && p.getEstado() == EstadoProducto.INACTIVO ? "Activar" : "Desactivar");
    }

    @Override
    protected void alDobleClic(Producto p) {
        if (tienda.puedeAdministrar()) {
            editar();
        } else {
            reponer();
        }
    }

    @Override
    public void aplicarTema() {
        super.aplicarTema();
        if (btnEliminar == null) {
            return; // todavía se está construyendo
        }
        Tema.botonPeligro(btnEliminar);
        Tema.botonPrimario(btnMostrador);
        Tema.tabla(tblVentas);
        scrVentas.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        lblVentas.setFont(Tema.FUENTE_NEGRITA);
        lblVentas.setForeground(Tema.color(ColorTema.TEXTO));
        lblAlerta.setFont(Tema.FUENTE_NEGRITA);
        actualizarAlerta();
    }

    // =====================================================================
    //  PIE: ALERTA Y VENTAS (datos del controlador)
    // =====================================================================

    /** Lista los productos que llegaron al mínimo (los calcula el modelo). */
    private void actualizarAlerta() {
        List<Producto> bajos = tienda.getProductosBajoMinimo();
        if (bajos.isEmpty()) {
            lblAlerta.setText(" Todos los productos tienen existencia suficiente.");
            lblAlerta.setForeground(Tema.color(ColorTema.EXITO));
            return;
        }
        StringJoiner lista = new StringJoiner(",  ");
        for (Producto p : bajos) {
            lista.add(p.getNombre() + " (" + p.getExistencia() + " de mínimo " + p.getStockMinimo() + ")");
        }
        lblAlerta.setText(" Reponer pronto: " + lista);
        lblAlerta.setForeground(Tema.oscurecer(Tema.color(ColorTema.ADVERTENCIA), 0.3));
    }

    private void cargarVentas() {
        LocalDate fecha = chkTodas.isSelected() ? null : FechaUtil.ahora().toLocalDate();
        List<Venta> ventas = tienda.listarVentas(fecha);
        modeloVentas.setElementos(ventas);
        double total = fecha == null ? sumar(ventas) : tienda.getTotalVendido(fecha);
        lblVentas.setText((fecha == null ? "Todas las ventas: " : "Ventas de hoy: ") + ventas.size()
                + "  ·  " + MonedaUtil.formatear(total));
    }

    /** Total de "todas las ventas" (para hoy se usa getTotalVendido del controlador). */
    private static double sumar(List<Venta> ventas) {
        double total = 0;
        for (Venta v : ventas) {
            total += v.calcularTotal();
        }
        return MonedaUtil.redondear(total);
    }

    // =====================================================================
    //  ACCIONES (formularios de la 8.1 + una llamada al controlador)
    // =====================================================================

    private void nuevo() {
        Producto nuevo = ProductoDialog.mostrar(this, tienda, null);
        if (nuevo != null) {
            recargar();
            seleccionar(nuevo::equals);
            informar("Producto " + nuevo.getCodigo() + " registrado. Ya se puede vender en el POS.");
        }
    }

    private void editar() {
        Producto p = seleccionado();
        if (p != null && ProductoDialog.mostrar(this, tienda, p) != null) {
            recargar();
        }
    }

    private void reponer() {
        Producto p = seleccionado();
        if (p != null && ProductoDialog.reponer(this, tienda, p) != null) {
            recargar();
        }
    }

    private void cambiarEstado() {
        Producto p = seleccionado();
        if (p == null) {
            return;
        }
        boolean activar = p.getEstado() == EstadoProducto.INACTIVO;
        if (!confirmar(activar ? "¿Volver a vender \"" + p.getNombre() + "\"?"
                : "¿Dejar de vender \"" + p.getNombre() + "\"? Ya no aparecerá en el POS; las ventas pasadas no cambian.")) {
            return;
        }
        try {
            tienda.cambiarEstado(p.getCodigo(), activar ? EstadoProducto.ACTIVO : EstadoProducto.INACTIVO);
            recargar();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void eliminar() {
        Producto p = seleccionado();
        if (p == null || !confirmar("¿Eliminar \"" + p.getNombre() + "\"?\nSolo se permite si nunca se vendió; "
                + "si ya tiene ventas, desactívelo.")) {
            return;
        }
        try {
            tienda.eliminarProducto(p.getCodigo());
            recargar();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.PosController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TiendaController;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
/**
 *
 * @author SANTANA
 */
public class PosDialog extends JDialog{
    private static final long serialVersionUID = 1L;
    private static final String TODAS = "Todas";
     /** Ancho aproximado de cada tarjeta de producto (para calcular cuántas caben por fila). */
    private static final int ANCHO_TARJETA = 160;
    /** Ancho del texto del nombre: si no cabe, baja a otra línea. */
    private static final int ANCHO_TEXTO = 120;
    private final transient PosController pos;
    private final transient TiendaController tienda;
    private transient Venta realizada;

    // --- Catálogo
    private final JTextField txtBuscar = new JTextField(12);
    private final JComboBox<Object> cmbCategoria = new JComboBox<>();
    private final JSpinner spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JPanel pnlProductos = new JPanel(new GridLayout(0, 3, Tema.ESPACIO, Tema.ESPACIO));

    // --- Carrito
    private final ModeloTablaLista<DetalleVenta> modeloCarrito = new ModeloTablaLista<>(
            new String[]{"Producto", "Cant.", "Precio", "Subtotal"},
            new Class<?>[]{String.class, Integer.class, Double.class, Double.class},
            d -> new Object[]{d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()});
    private final JTable tblCarrito = new JTable(modeloCarrito);
    private final JLabel lblTotal = new JLabel("Q0.00", SwingConstants.RIGHT);
    private final JComboBox<MetodoPago> cmbMetodo = new JComboBox<>(MetodoPago.values());
    private final JButton btnQuitar = new JButton("Quitar");
    private final JButton btnVaciar = new JButton("Vaciar");
    private final JButton btnCancelar = new JButton("Cancelar venta");
     private final JButton btnACuenta = new JButton("Dejar a cuenta");
    private final JButton btnCobrar = new JButton("Cobrar");
    private final JLabel lblMensaje = new JLabel(" ");

    /**
     * @param codigoReserva reserva EN CURSO a la que se carga el consumo; {@code null} = venta de mostrador
     * @throws CanchaProException si la tienda está apagada o la reserva ya no admite consumos
     */
    public PosDialog(Component origen, SistemaController sistema, String codigoReserva) throws CanchaProException {
        super(ventanaDe(origen), "POS", ModalityType.APPLICATION_MODAL);
        this.pos = sistema.getPosController();
        this.tienda = sistema.getTiendaController();

        pos.cancelarVenta(); // por si quedó un carrito viejo abierto
        if (codigoReserva == null) {
            pos.iniciarVentaMostrador();
        } else {
            pos.iniciarVentaParaReserva(codigoReserva);
        }
        setTitle(pos.getTitulo());

        construir(codigoReserva == null ? null : pos.getVentaActual().getReserva());
        cargarProductos();
        actualizarCarrito();
        setSize(new Dimension(1000, 620));
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(getOwner());
    }

    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    /** @return la venta cobrada, o {@code null} si se cerró sin cobrar */
    public Venta getVentaRealizada() {
        return realizada;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir(Reserva reserva) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelar();
            }
        });

        JPanel raiz = new JPanel(new BorderLayout(Tema.MARGEN, Tema.MARGEN));
        raiz.setBorder(Tema.margen());
        raiz.setBackground(Tema.color(ColorTema.FONDO));

        // ---- Encabezado
        JLabel lblTitulo = new JLabel(pos.getTitulo());
        Tema.titulo(lblTitulo);
        JLabel lblDetalle = new JLabel(reserva == null
                ? "Venta de mostrador: no se carga a ninguna reserva."
                : "Partido " + FechaUtil.formatearRango(reserva.getInicio(), reserva.getFin())
                + "   ·   ya consumió " + MonedaUtil.formatear(consumoPrevio(reserva)) + " en la tienda");
        Tema.textoSecundario(lblDetalle);
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 2));
        encabezado.setOpaque(false);
        encabezado.add(lblTitulo);
        encabezado.add(lblDetalle);
        raiz.add(encabezado, BorderLayout.NORTH);

        // ---- Centro: catálogo | carrito
        JPanel centro = new JPanel(new GridLayout(1, 2, Tema.MARGEN, 0));
        centro.setOpaque(false);
        centro.add(crearCatalogo());
        centro.add(crearCarrito());
        raiz.add(centro, BorderLayout.CENTER);

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        raiz.add(lblMensaje, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(btnCobrar);
        getRootPane().registerKeyboardAction(e -> cancelar(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private double consumoPrevio(Reserva reserva) {
        try {
            return tienda.getConsumoDeReserva(reserva.getCodigo());
        } catch (CanchaProException ex) {
            return 0;
        }
    }

    /** Izquierda: filtros + un botón por producto disponible (se generan por código). */
    private JComponent crearCatalogo() {
        cmbCategoria.addItem(TODAS);
        for (CategoriaProducto c : tienda.getCategorias()) {
            cmbCategoria.addItem(c);
        }
        Tema.campo(txtBuscar);
        txtBuscar.setToolTipText("Código o nombre");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cargarProductos();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cargarProductos();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cargarProductos();
            }
        });
        cmbCategoria.addActionListener(e -> cargarProductos());
        spnCantidad.setPreferredSize(new Dimension(60, 30));

        JPanel filaBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        filaBuscar.setOpaque(false);
        filaBuscar.add(new JLabel("Buscar"));
        filaBuscar.add(txtBuscar);
        filaBuscar.add(cmbCategoria);
        JPanel filaCantidad = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        filaCantidad.setOpaque(false);
        filaCantidad.add(new JLabel("Unidades por clic"));
        filaCantidad.add(spnCantidad);
        JPanel filtros = new JPanel(new GridLayout(2, 1, 0, Tema.ESPACIO));
        filtros.setOpaque(false);
        filtros.add(filaBuscar);
        filtros.add(filaCantidad);

        pnlProductos.setOpaque(false);
        JPanel contenedor = new PanelAjustadoAlAncho(); // nunca sale barra horizontal
        contenedor.setLayout(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(pnlProductos, BorderLayout.NORTH); // los botones no se estiran a lo alto
        JScrollPane scr = new JScrollPane(contenedor);
        scr.setBorder(BorderFactory.createEmptyBorder());
        scr.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        scr.getVerticalScrollBar().setUnitIncrement(16);
        // Cuántos productos por fila según el ancho visible (igual que el mapa de canchas)
        scr.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int columnas = Math.max(2, scr.getViewport().getWidth() / ANCHO_TARJETA);
                GridLayout grid = (GridLayout) pnlProductos.getLayout();
                if (grid.getColumns() != columnas) {
                    grid.setColumns(columnas);
                    pnlProductos.revalidate();
                }
            }
        });

        JLabel titulo = new JLabel("Productos (clic para agregar)");
        titulo.setFont(Tema.FUENTE_SUBTITULO);
        JPanel arriba = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        arriba.setOpaque(false);
        arriba.add(titulo, BorderLayout.NORTH);
        arriba.add(filtros, BorderLayout.SOUTH);

        JPanel tarjeta = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        tarjeta.setBackground(Tema.color(ColorTema.SUPERFICIE));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(Tema.bordeTarjeta(), Tema.margen()));
        tarjeta.add(arriba, BorderLayout.NORTH);
        tarjeta.add(scr, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Derecha: tabla del carrito, total, método de pago y botones. */
    private JComponent crearCarrito() {
        Tema.tabla(tblCarrito);
        tblCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer moneda = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(valor == null ? "" : MonedaUtil.formatear((Double) valor));
            }
        };
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        tblCarrito.getColumnModel().getColumn(1).setCellRenderer(centrado);
        tblCarrito.getColumnModel().getColumn(2).setCellRenderer(moneda);
        tblCarrito.getColumnModel().getColumn(3).setCellRenderer(moneda);
        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(200);
        tblCarrito.getSelectionModel().addListSelectionListener(e -> btnQuitar.setEnabled(tblCarrito.getSelectedRow() >= 0));
        JScrollPane scr = new JScrollPane(tblCarrito);
        scr.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));

        Tema.botonSecundario(btnQuitar);
        Tema.botonSecundario(btnVaciar);
        btnQuitar.addActionListener(e -> quitar());
        btnVaciar.addActionListener(e -> vaciar());
        JPanel filaQuitar = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        filaQuitar.setOpaque(false);
        filaQuitar.add(btnQuitar);
        filaQuitar.add(btnVaciar);

        JLabel lblTextoTotal = new JLabel("TOTAL");
        lblTextoTotal.setFont(Tema.FUENTE_SUBTITULO);
        lblTotal.setFont(Tema.FUENTE_INDICADOR);
        lblTotal.setForeground(Tema.color(ColorTema.PRIMARIO));
        JPanel filaTotal = new JPanel(new BorderLayout());
        filaTotal.setOpaque(false);
        filaTotal.add(lblTextoTotal, BorderLayout.WEST);
        filaTotal.add(lblTotal, BorderLayout.EAST);

        JPanel filaPago = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        filaPago.setOpaque(false);
        JLabel lblPago = new JLabel("Forma de pago");
        lblPago.setFont(Tema.FUENTE_NEGRITA);
        filaPago.add(lblPago);
        filaPago.add(cmbMetodo);

        Tema.botonSecundario(btnCancelar);
        Tema.botonPrimario(btnCobrar);
        btnCancelar.addActionListener(e -> cancelar());
       btnCobrar.addActionListener(e -> cobrar());
        Tema.botonSecundario(btnACuenta);
        btnACuenta.setToolTipText("No se cobra ahora: el consumo queda como saldo pendiente de la reserva.");
        btnACuenta.addActionListener(e -> dejarACuenta());
        btnACuenta.setVisible(pos.puedeDejarACuenta()); // solo existe para ventas de una reserva
        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        filaBotones.setOpaque(false);
        filaBotones.add(btnCancelar);
        filaBotones.add(btnACuenta);
        filaBotones.add(btnCobrar);

        JPanel abajo = new JPanel(new GridLayout(4, 1, 0, Tema.ESPACIO));
        abajo.setOpaque(false);
        abajo.add(filaQuitar);
        abajo.add(filaTotal);
        abajo.add(filaPago);
        abajo.add(filaBotones);

        JLabel titulo = new JLabel("Carrito");
        titulo.setFont(Tema.FUENTE_SUBTITULO);

        JPanel tarjeta = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        tarjeta.setBackground(Tema.color(ColorTema.SUPERFICIE));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(Tema.bordeTarjeta(), Tema.margen()));
        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(scr, BorderLayout.CENTER);
        tarjeta.add(abajo, BorderLayout.SOUTH);
        return tarjeta;
    }

    // =====================================================================
    //  DATOS
    // =====================================================================

    /** Un botón por producto disponible (activo y con existencia) que cumpla los filtros. */
    private void cargarProductos() {
        Object seleccion = cmbCategoria.getSelectedItem();
        CategoriaProducto categoria = seleccion instanceof CategoriaProducto ? (CategoriaProducto) seleccion : null;
        pnlProductos.removeAll();
        for (Producto p : tienda.buscar(txtBuscar.getText(), categoria)) {
            if (p.estaDisponibleParaVenta()) {
                pnlProductos.add(botonDe(p));
            }
        }
        if (pnlProductos.getComponentCount() == 0) {
            JLabel vacio = new JLabel("No hay productos disponibles con ese filtro.");
            Tema.textoSecundario(vacio);
            pnlProductos.add(vacio);
        }
        pnlProductos.revalidate();
        pnlProductos.repaint();
    }

        /**
     * Tarjeta de un producto: el NOMBRE (hasta 2 líneas, se parte solo), el PRECIO
     * y la EXISTENCIA, cada uno en su propia etiqueta. El alto lo decide el texto,
     * así nada se corta aunque Windows use letras más grandes.
     */
    private JButton botonDe(Producto p) {
        JButton b = new JButton();
        Tema.botonSecundario(b);
        b.setLayout(new javax.swing.BoxLayout(b, javax.swing.BoxLayout.Y_AXIS));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)));

        // El ancho fijo en el HTML hace que el nombre largo baje a una segunda línea
        JLabel nombre = new JLabel("<html><div style='text-align:center; width:" + ANCHO_TEXTO + "px'>"
                + p.getNombre() + "</div></html>", SwingConstants.CENTER);
        nombre.setFont(Tema.FUENTE_NEGRITA);
        nombre.setForeground(Tema.color(ColorTema.TEXTO));

        JLabel precio = new JLabel(MonedaUtil.formatear(p.getPrecio()), SwingConstants.CENTER);
        precio.setFont(Tema.FUENTE_SUBTITULO);
        precio.setForeground(Tema.color(ColorTema.PRIMARIO));

        JLabel existencia = new JLabel(p.getExistencia() + " en inventario", SwingConstants.CENTER);
        existencia.setFont(Tema.FUENTE_PEQUENA);
        existencia.setForeground(p.estaBajoMinimo()
                ? Tema.oscurecer(Tema.color(ColorTema.ADVERTENCIA), 0.3) : Tema.color(ColorTema.TEXTO_SECUNDARIO));

        for (JComponent c : new JComponent[]{nombre, precio, existencia}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        b.add(nombre);
        b.add(javax.swing.Box.createVerticalStrut(4));
        b.add(precio);
        b.add(javax.swing.Box.createVerticalStrut(2));
        b.add(existencia);

        b.setToolTipText(p.getCodigo() + " · " + p.getNombre() + " · " + p.getCategoria().getDescripcion());
        b.addActionListener(e -> agregar(p));
        return b;
    }

    /** Repinta la tabla y el total con lo que tiene el carrito del controlador. */
    private void actualizarCarrito() {
        List<DetalleVenta> lineas = pos.getLineas();
        modeloCarrito.setElementos(lineas);
        lblTotal.setText(MonedaUtil.formatear(pos.getTotal()));
        btnCobrar.setEnabled(!lineas.isEmpty());
        btnACuenta.setEnabled(!lineas.isEmpty());
        btnVaciar.setEnabled(!lineas.isEmpty());
        btnQuitar.setEnabled(tblCarrito.getSelectedRow() >= 0);
    }

    // =====================================================================
    //  ACCIONES (cada una = una llamada al PosController)
    // =====================================================================

        /** Dentro de un JScrollPane toma SIEMPRE el ancho visible: así no aparece la barra horizontal. */
    private static final class PanelAjustadoAlAncho extends JPanel implements javax.swing.Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
    /** Prueba 14: si se pide más de lo que hay, el modelo lanza StockInsuficienteException. */
    private void agregar(Producto p) {
        int cantidad = (Integer) spnCantidad.getValue();
        try {
            pos.agregarProducto(p.getCodigo(), cantidad);
            mensaje(" - " + cantidad + " × " + p.getNombre() + " agregado.", ColorTema.EXITO);
            spnCantidad.setValue(1);
        } catch (CanchaProException ex) {
            mensaje(ex.getMessage(), ColorTema.ERROR);
        }
        actualizarCarrito();
    }

    private void quitar() {
        int fila = tblCarrito.getSelectedRow();
        if (fila < 0) {
            return;
        }
        DetalleVenta linea = modeloCarrito.getElementoEn(tblCarrito.convertRowIndexToModel(fila));
        try {
            pos.quitarProducto(linea.getProducto().getCodigo());
            mensaje("Se quitó " + linea.getProducto().getNombre() + ".", ColorTema.TEXTO_SECUNDARIO);
        } catch (CanchaProException ex) {
            mensaje(ex.getMessage(), ColorTema.ERROR);
        }
        actualizarCarrito();
    }

    /** Se recorre una COPIA: getLineas() es una vista de la lista real y borrar mientras se recorre fallaría. */
    private void vaciar() {
        for (DetalleVenta linea : new ArrayList<>(pos.getLineas())) {
            try {
                pos.quitarProducto(linea.getProducto().getCodigo());
            } catch (CanchaProException ex) {
                mensaje(ex.getMessage(), ColorTema.ERROR);
            }
        }
        actualizarCarrito();
    }

    /** Prueba 13: el controlador verifica TODO el stock y descuenta todo junto (o nada). */
    private void cobrar() {
        MetodoPago metodo = (MetodoPago) cmbMetodo.getSelectedItem();
        try {
            realizada = pos.confirmar(metodo);
            mostrarComprobante(realizada);
            dispose();
        } catch (CanchaProException ex) {
            mensaje(ex.getMessage(), ColorTema.ERROR);
            cargarProductos(); // las existencias pudieron cambiar
            actualizarCarrito();
        }
    }

    /** Cerrar sin cobrar: el carrito se descarta y el inventario no cambió. */
    private void cancelar() {
        if (!pos.getLineas().isEmpty() && JOptionPane.showConfirmDialog(this,
                "¿Cancelar esta venta? El carrito se descarta y el inventario no cambia.",
                "Cancelar venta", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        pos.cancelarVenta();
        dispose();
    }
    /** Cierra la venta sin cobrar: queda como saldo pendiente de la reserva. */
    private void dejarACuenta() {
        try {
            realizada = pos.confirmarACuenta();
            mostrarComprobante(realizada);
            dispose();
        } catch (CanchaProException ex) {
            mensaje(ex.getMessage(), ColorTema.ERROR);
            cargarProductos(); // las existencias pudieron cambiar
            actualizarCarrito();
        }
    }
    
/** Comprobante de la venta: productos, total y cómo se pagó (o si quedó a cuenta). */
    private void mostrarComprobante(Venta v) {
        StringBuilder sb = new StringBuilder("<html><b>Venta " + v.getNumero() + "</b> · "
                + FechaUtil.formatearFechaHora(v.getFechaHora()) + "<br>"
                + (v.estaAsociadaAReserva() ? "Cargada a la reserva " + v.getReserva().getCodigo() + " · "
                        + v.getReserva().getCliente().getNombre() : "Venta de mostrador") + "<br><br>");
        for (DetalleVenta d : v.getDetalles()) {
            sb.append(d.getCantidad()).append(" × ").append(d.getProducto().getNombre()).append(" — ")
              .append(MonedaUtil.formatear(d.getSubtotal())).append("<br>");
        }
        sb.append("<br><b>Total: ").append(MonedaUtil.formatear(v.calcularTotal())).append("</b> · ")
          .append(v.isPendienteDePago()
                  ? "<font color='#B45309'>A CUENTA: queda como saldo pendiente de la reserva</font>"
                  : v.getMetodoPago().getDescripcion())
          .append("</html>");
        JOptionPane.showMessageDialog(this, sb.toString(),
                v.isPendienteDePago() ? "Venta cargada a cuenta" : "Venta registrada",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mensaje(String texto, ColorTema color) {
        lblMensaje.setText(texto);
        lblMensaje.setForeground(Tema.color(color));
    }
}

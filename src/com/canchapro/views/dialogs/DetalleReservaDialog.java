/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TiendaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Cancelacion;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioReserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
/**
 ** Ficha COMPLETA de una reserva (solo consulta, construida por código).
 *
 * 
 *  ┌ Reserva R0009 ─────────────────────────────── [Confirmada] ┐
 *  │ Cliente  Equipo Los Halcones · 55123456 · correo            │
 *  │ Cancha   La Bombonera (Fútbol 5)                            │
 *  │ Horario  16/10/2026 19:00 - 21:00 (2 h)                     │
 *  │ Creada   13/10/2026 12:00 por Administrador General         │
 *  │ [Cancha Q450] [Servicios Q180] [Total Q630] [Pagado] [Saldo]│
 *  │ ┌ Servicios │ Pagos │ Tienda ┐                              │
 *  │ │ tabla                       │                             │
 *  │ └─────────────────────────────┘                             │
 *  │ (si está cancelada: motivo, fecha y responsable)            │
 *  │                                               [Cerrar]      │
 *  └─────────────────────────────────────────────────────────────┘
 * 
 *
 * No modifica nada: solo LEE la reserva que entrega el ReservaController y
 * los consumos de tienda que entrega el TiendaController. Los totales los
 * calcula la propia Reserva (modelo).
 * @author SANTANA
 */
public class DetalleReservaDialog extends JDialog{
    private static final long serialVersionUID = 1L;

    /**
     * @throws CanchaProException si la reserva no existe
     */
    public DetalleReservaDialog(Component origen, SistemaController sistema, String codigoReserva)
            throws CanchaProException {
        super(ventanaDe(origen), "Reserva " + codigoReserva, ModalityType.APPLICATION_MODAL);
        Reserva r = sistema.getReservaController().obtener(codigoReserva);
        construir(r, sistema.getTiendaController());
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir(Reserva r, TiendaController tienda) throws CanchaProException {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel raiz = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        raiz.setBorder(Tema.margen());
        raiz.setBackground(Tema.color(ColorTema.SUPERFICIE));

        JPanel arriba = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        arriba.setOpaque(false);
        arriba.add(crearEncabezado(r), BorderLayout.NORTH);
        arriba.add(crearDatos(r), BorderLayout.CENTER);
        arriba.add(crearCifras(r), BorderLayout.SOUTH);
        raiz.add(arriba, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Tema.FUENTE_NEGRITA);
        tabs.addTab("Servicios (" + r.getServicios().size() + ")", crearTablaServicios(r));
        tabs.addTab("Pagos (" + r.getPagos().size() + ")", crearTablaPagos(r));
        if (tienda.isModuloActivo()) {
            tabs.addTab("Tienda (" + MonedaUtil.formatear(tienda.getConsumoDeReserva(r.getCodigo())) + ")",
                    crearTablaTienda(tienda, r));
        }
        tabs.setPreferredSize(new Dimension(680, 200));
        raiz.add(tabs, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        abajo.setOpaque(false);
        if (r.getEstado() == EstadoReserva.CANCELADA && r.getCancelacion() != null) {
            abajo.add(crearCancelacion(r.getCancelacion()), BorderLayout.NORTH);
        }
        JButton btnCerrar = new JButton("Cerrar");
        Tema.botonPrimario(btnCerrar);
        btnCerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botones.add(btnCerrar);
        abajo.add(botones, BorderLayout.SOUTH);
        raiz.add(abajo, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(btnCerrar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** "Reserva R0009" a la izquierda y la píldora del estado a la derecha. */
    private JComponent crearEncabezado(Reserva r) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel titulo = new JLabel("Reserva " + r.getCodigo());
        Tema.titulo(titulo);
        p.add(titulo, BorderLayout.WEST);

        Color color = Tema.colorEstadoReserva(r.getEstado());
        JLabel estado = new JLabel(r.getEstado().getDescripcion().toUpperCase(java.util.Locale.ROOT));
        estado.setOpaque(true);
        estado.setBackground(color);
        estado.setForeground(Tema.textoSobre(color));
        estado.setFont(Tema.FUENTE_NEGRITA);
        estado.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        p.add(estado, BorderLayout.EAST);
        return p;
    }

    private JComponent crearDatos(Reserva r) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 0, 2, 12);
        g.anchor = GridBagConstraints.WEST;

        String cliente = r.getCliente().getNombre() + "   ·   " + r.getCliente().getTelefono()
                + (r.getCliente().tieneCorreo() ? "   ·   " + r.getCliente().getCorreo() : "");
        String cancha = r.getCancha().getNombre() + "  (" + r.getCancha().getTipo().getDescripcion() + ")";
        String horario = FechaUtil.formatearRango(r.getInicio(), r.getFin())
                + "   (" + FechaUtil.formatearDuracion(r.getDuracion()) + ")";
        String creada = FechaUtil.formatearFechaHora(r.getFechaCreacion())
                + (r.getCreadaPor() == null ? "" : "  por " + r.getCreadaPor().getNombre());

        fila(p, g, 0, "Cliente", cliente);
        fila(p, g, 1, "Cancha", cancha);
        fila(p, g, 2, "Horario", horario);
        fila(p, g, 3, "Creada", creada);
        return p;
    }

    private static void fila(JPanel p, GridBagConstraints g, int y, String etiqueta, String valor) {
        JLabel e = new JLabel(etiqueta);
        e.setFont(Tema.FUENTE_NEGRITA);
        e.setForeground(Tema.color(ColorTema.TEXTO_SECUNDARIO));
        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        p.add(e, g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(new JLabel(valor), g);
    }

    /** Las cifras salen de la Reserva (modelo): cancha, servicios, total, pagado y saldo. */
    private JComponent crearCifras(Reserva r) {
        JPanel p = new JPanel(new GridLayout(1, 5, Tema.ESPACIO, 0));
        p.setOpaque(false);
        boolean cancelada = r.getEstado() == EstadoReserva.CANCELADA;
        p.add(cifra("CANCHA", MonedaUtil.formatear(r.getPrecioCancha()), null));
        p.add(cifra("SERVICIOS", MonedaUtil.formatear(r.calcularTotalServicios()), null));
        p.add(cifra("TOTAL", MonedaUtil.formatear(r.calcularTotal()), Tema.color(ColorTema.PRIMARIO)));
        p.add(cifra("PAGADO", MonedaUtil.formatear(r.calcularTotalPagado()), null));
        p.add(cifra("SALDO", cancelada ? "—" : MonedaUtil.formatear(r.calcularSaldo()),
                cancelada || r.estaPagada() ? Tema.color(ColorTema.EXITO) : Tema.color(ColorTema.ERROR)));
        return p;
    }

    private static JPanel cifra(String titulo, String valor, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.color(ColorTema.FONDO));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel t = new JLabel(titulo);
        Tema.textoSecundario(t);
        JLabel v = new JLabel(valor);
        v.setFont(Tema.FUENTE_SUBTITULO);
        if (color != null) {
            v.setForeground(color);
        }
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    // =====================================================================
    //  TABLAS (solo lectura)
    // =====================================================================

    private JComponent crearTablaServicios(Reserva r) {
        DefaultTableModel m = modelo("Servicio", "Cantidad", "Precio unit.", "Horas", "Subtotal");
        for (ServicioReserva s : r.getServicios()) {
            m.addRow(new Object[]{s.getServicio().getNombre(), s.getCantidad(),
                MonedaUtil.formatear(s.getPrecioUnitario()),
                s.getServicio().isCobroPorHora() ? s.getHoras() : "-", MonedaUtil.formatear(s.getSubtotal())});
        }
        return tabla(m, "Sin servicios adicionales.");
    }

    private JComponent crearTablaPagos(Reserva r) {
        DefaultTableModel m = modelo("Código", "Fecha", "Método", "Monto", "Observación");
        for (Pago p : r.getPagos()) {
            m.addRow(new Object[]{p.getCodigo(), FechaUtil.formatearFechaHora(p.getFechaHora()),
                p.getMetodo().getDescripcion(), MonedaUtil.formatear(p.getMonto()),
                p.tieneObservacion() ? p.getObservacion() : ""});
        }
        return tabla(m, "Todavía no hay pagos registrados.");
    }

    private JComponent crearTablaTienda(TiendaController tienda, Reserva r) throws CanchaProException {
        DefaultTableModel m = modelo("Venta", "Fecha", "Producto", "Cantidad", "Subtotal");
        for (Venta v : tienda.getVentasDeReserva(r.getCodigo())) {
            for (DetalleVenta d : v.getDetalles()) {
                m.addRow(new Object[]{v.getNumero(), FechaUtil.formatearFechaHora(v.getFechaHora()),
                    d.getProducto().getNombre(), d.getCantidad(), MonedaUtil.formatear(d.getSubtotal())});
            }
        }
        return tabla(m, "Sin consumos de tienda/cafetería.");
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /** Tabla con estilo del Tema; si no hay filas, muestra un texto en su lugar. */
    private static JComponent tabla(DefaultTableModel m, String textoVacio) {
        if (m.getRowCount() == 0) {
            JLabel vacio = new JLabel(textoVacio, JLabel.CENTER);
            Tema.textoSecundario(vacio);
            return vacio;
        }
        JTable t = new JTable(m);
        Tema.tabla(t);
        return new JScrollPane(t);
    }

    // =====================================================================
    //  CANCELACIÓN
    // =====================================================================

    private JComponent crearCancelacion(Cancelacion c) {
        JLabel l = new JLabel("<html><b>Cancelada</b> el " + FechaUtil.formatearFechaHora(c.getFechaHora())
                + " por <b>" + c.getResponsable().getNombre() + "</b><br>Motivo: " + c.getMotivo() + "</html>");
        l.setForeground(Tema.color(ColorTema.ERROR));
        l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.color(ColorTema.ERROR)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return l;
    }
}

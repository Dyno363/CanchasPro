/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.PagoController;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
/**
 ** Ventana de COBRO de una reserva (modal, construida por código).
 *
 * 
 *  ┌ Pagos · R0006 ──────────────────────────────────────────────┐
 *  │ Luis Hernández · La Bombonera · 16/10/2026 08:00 - 10:00     │
 *  │ [ Total Q340 ] [ Pagado Q150 ] [ Saldo Q190 ]  ████░░ 44%    │
 *  │ Historial de pagos (tabla)                                   │
 *  │ Monto [     ]  Método [Efectivo ▼]  Referencia [          ]  │
 *  │ (mensaje)    [Cerrar] [Pagar saldo Q190.00] [Registrar abono]│
 *  └──────────────────────────────────────────────────────────────┘
 * 
 *
 * MVC: el monto se envía como TEXTO al {@link PagoController}, que lo convierte
 * y valida (mayor que cero, no mayor que el saldo, referencia obligatoria en
 * transferencia/depósito). La vista solo muestra el resumen que él calcula.
 * @author SANTANA
 */
public class PagoDialog extends JDialog{
    private static final long serialVersionUID = 1L;

    private final transient PagoController pagos;
    private final transient ReservaController reservas;
    private final String codigoReserva;
    private boolean huboCambios;

    private final JLabel lblReserva = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblPagado = new JLabel();
    private final JLabel lblSaldo = new JLabel();
    private final JProgressBar barPagado = new JProgressBar(0, 100);
    private final JLabel lblAnticipo = new JLabel(" ");
    private final DefaultTableModel modeloPagos = new DefaultTableModel(
            new Object[]{"Código", "Fecha", "Método", "Monto", "Observación"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; // solo consulta
        }
    };
    private final JTable tblPagos = new JTable(modeloPagos);
    private final JTextField txtMonto = new JTextField(10);
    private final JComboBox<MetodoPago> cmbMetodo = new JComboBox<>();
    private final JTextField txtObservacion = new JTextField(18);
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnAbonar = new JButton("Registrar abono");
    private final JButton btnPagarSaldo = new JButton("Pagar saldo");
    private final JButton btnCerrar = new JButton("Cerrar");

    /**
     * @param origen        componente desde el que se abre
     * @param sistema       acceso a los controladores de la sesión
     * @param codigoReserva reserva a cobrar
     * @throws CanchaProException si la reserva no existe
     */
    public PagoDialog(Component origen, SistemaController sistema, String codigoReserva)
            throws CanchaProException {
        super(ventanaDe(origen), "Pagos · " + codigoReserva, ModalityType.APPLICATION_MODAL);
        this.pagos = sistema.getPagoController();
        this.reservas = sistema.getReservaController();
        this.codigoReserva = codigoReserva;
        Reserva r = reservas.obtener(codigoReserva); // valida que exista
        lblReserva.setText(r.getCliente().getNombre() + "   ·   " + r.getCancha().getNombre() + "   ·   "
                + FechaUtil.formatearRango(r.getInicio(), r.getFin()) + "   ·   " + r.getEstado());
        construir();
        refrescar();
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

    /** @return true si se registró al menos un pago o se confirmó la reserva */
    public boolean huboCambios() {
        return huboCambios;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel raiz = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        raiz.setBorder(Tema.margen());
        raiz.setBackground(Tema.color(ColorTema.SUPERFICIE));

        // ---- Arriba: datos de la reserva + resumen de cobro
        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.setOpaque(false);
        lblReserva.setFont(Tema.FUENTE_NEGRITA);
        lblReserva.setAlignmentX(LEFT_ALIGNMENT);
        arriba.add(lblReserva);
        arriba.add(javax.swing.Box.createVerticalStrut(Tema.ESPACIO));

        JPanel cifras = new JPanel(new GridLayout(1, 3, Tema.ESPACIO, 0));
        cifras.setOpaque(false);
        cifras.setAlignmentX(LEFT_ALIGNMENT);
        cifras.add(cifra("TOTAL", lblTotal));
        cifras.add(cifra("PAGADO", lblPagado));
        cifras.add(cifra("SALDO", lblSaldo));
        arriba.add(cifras);
        arriba.add(javax.swing.Box.createVerticalStrut(Tema.ESPACIO));

        barPagado.setStringPainted(true);
        barPagado.setForeground(Tema.color(ColorTema.EXITO));
        barPagado.setAlignmentX(LEFT_ALIGNMENT);
        arriba.add(barPagado);
        Tema.textoSecundario(lblAnticipo);
        lblAnticipo.setAlignmentX(LEFT_ALIGNMENT);
        arriba.add(lblAnticipo);
        raiz.add(arriba, BorderLayout.NORTH);

        // ---- Centro: historial de pagos
        Tema.tabla(tblPagos);
        JScrollPane scr = new JScrollPane(tblPagos);
        scr.setPreferredSize(new Dimension(620, 150));
        scr.setBorder(BorderFactory.createTitledBorder("Historial de pagos"));
        raiz.add(scr, BorderLayout.CENTER);

        // ---- Abajo: formulario de pago + botones
        JPanel abajo = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        abajo.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        for (MetodoPago m : pagos.getMetodos()) {
            cmbMetodo.addItem(m);
        }
        Tema.campo(txtMonto);
        Tema.campo(txtObservacion);
        txtMonto.setToolTipText("Ej. 100 o Q100.00");
        g.gridy = 0;
        g.gridx = 0;
        form.add(negrita("Monto"), g);
        g.gridx = 1;
        form.add(negrita("Método"), g);
        g.gridx = 2;
        form.add(negrita("Observación / No. de referencia"), g);
        g.gridy = 1;
        g.gridx = 0;
        form.add(txtMonto, g);
        g.gridx = 1;
        form.add(cmbMetodo, g);
        g.gridx = 2;
        form.add(txtObservacion, g);
        JLabel ayuda = new JLabel("La referencia es obligatoria para Transferencia y Depósito.");
        Tema.textoSecundario(ayuda);
        g.gridy = 2;
        g.gridx = 0;
        g.gridwidth = 3;
        form.add(ayuda, g);
        abajo.add(form, BorderLayout.NORTH);

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        Tema.botonPrimario(btnAbonar);
        Tema.botonSecundario(btnPagarSaldo);
        Tema.botonSecundario(btnCerrar);
        btnAbonar.addActionListener(e -> registrarAbono());
        btnPagarSaldo.addActionListener(e -> pagarSaldo());
        btnCerrar.addActionListener(e -> dispose());
        txtMonto.addActionListener(e -> registrarAbono()); // Enter en el monto
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(btnCerrar);
        botones.add(btnPagarSaldo);
        botones.add(btnAbonar);
        JPanel pie = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        pie.setOpaque(false);
        pie.add(lblMensaje, BorderLayout.NORTH);   // el error ocupa todo el ancho
        pie.add(botones, BorderLayout.SOUTH);
        abajo.add(pie, BorderLayout.SOUTH);
        raiz.add(abajo, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // =====================================================================
    //  MOSTRAR (los números los calcula el PagoController)
    // =====================================================================

    private void refrescar() {
        try {
            PagoController.ResumenCobro r = pagos.getResumen(codigoReserva);
            lblTotal.setText(MonedaUtil.formatear(r.getTotal()));
            lblPagado.setText(MonedaUtil.formatear(r.getPagado()));
            lblSaldo.setText(MonedaUtil.formatear(r.getSaldo()));
            lblSaldo.setForeground(r.estaPagada() ? Tema.color(ColorTema.EXITO) : Tema.color(ColorTema.ERROR));
            barPagado.setValue((int) Math.round(r.getPorcentajePagado()));
            barPagado.setString(r.estaPagada() ? "Pagada" : Math.round(r.getPorcentajePagado()) + "% pagado");
            lblAnticipo.setText(r.getAnticipoMinimo() > 0
                    ? "Anticipo mínimo para confirmar: " + MonedaUtil.formatear(r.getAnticipoMinimo())
                      + (r.getFaltaParaAnticipo() > 0
                         ? "  (faltan " + MonedaUtil.formatear(r.getFaltaParaAnticipo()) + ")" : "  ✓ cubierto")
                    : "No se exige anticipo mínimo para confirmar.");

            modeloPagos.setRowCount(0);
            for (Pago p : pagos.getPagos(codigoReserva)) {
                modeloPagos.addRow(new Object[]{p.getCodigo(), FechaUtil.formatearFechaHora(p.getFechaHora()),
                    p.getMetodo().getDescripcion(), MonedaUtil.formatear(p.getMonto()),
                    p.tieneObservacion() ? p.getObservacion() : ""});
            }

            // Si ya está pagada no hay nada más que cobrar
            boolean pagada = r.estaPagada();
            btnPagarSaldo.setText(pagada ? "Pagar saldo" : "Pagar saldo " + MonedaUtil.formatear(r.getSaldo()));
            for (JComponent c : new JComponent[]{txtMonto, cmbMetodo, txtObservacion, btnAbonar, btnPagarSaldo}) {
                c.setEnabled(!pagada);
            }
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }

    // =====================================================================
    //  ACCIONES (cada una: 1 llamada al controlador)
    // =====================================================================

    private void registrarAbono() {
        try {
            Pago p = pagos.registrarPago(codigoReserva, txtMonto.getText(),
                    (MetodoPago) cmbMetodo.getSelectedItem(), txtObservacion.getText());
            despuesDePagar(p);
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }

    private void pagarSaldo() {
        try {
            Pago p = pagos.pagarSaldo(codigoReserva, (MetodoPago) cmbMetodo.getSelectedItem(),
                    txtObservacion.getText());
            despuesDePagar(p);
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }

    /** Refresca, limpia el formulario y ofrece confirmar si ya se cubrió el anticipo. */
    private void despuesDePagar(Pago pago) {
        huboCambios = true;
        txtMonto.setText("");
        txtObservacion.setText("");
        lblMensaje.setText(" ");
        refrescar();
        try {
            if (pagos.puedeConfirmarse(codigoReserva)) {
                int r = JOptionPane.showConfirmDialog(this, "Pago " + pago.getCodigo() + " registrado ("
                        + MonedaUtil.formatear(pago.getMonto()) + ").\n¿Confirmar ahora la reserva "
                        + codigoReserva + "?", "Confirmar reserva", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    reservas.confirmar(codigoReserva);
                }
            }
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }

    // =====================================================================
    //  APOYO VISUAL
    // =====================================================================

    private static JPanel cifra(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        Tema.tarjeta(p);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel t = new JLabel(titulo);
        Tema.textoSecundario(t);
        valor.setFont(Tema.FUENTE_SUBTITULO);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private static JLabel negrita(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.FUENTE_NEGRITA);
        return l;
    }
}

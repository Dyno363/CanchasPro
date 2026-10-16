/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
/**
 ** Ventana para CANCELAR una reserva con motivo obligatorio (prueba #9 del PDF).
 *
 * 
 *  ┌ Cancelar reserva R0007 ─────────────────────────────────┐
 *  │ María López · La Bombonera · 16/10/2026 11:00 - 12:00    │
 *  │  Esta reserva tiene Q50.00 pagados. Los pagos quedan    │
 *  │   en el historial; la devolución se gestiona aparte.     │
 *  │ Motivo rápido [Lluvia o mal clima ▼]                     │
 *  │ Motivo *  ┌──────────────────────────────────────┐       │
 *  │           │                                      │       │
 *  │           └──────────────────────────────────────┘       │
 *  │ Cancelada por: Administrador General (ahora)             │
 *  │ (mensaje)             [Volver] [Cancelar reserva]        │
 *  └──────────────────────────────────────────────────────────┘
 * 
 *
 * La reserva NO se elimina: el modelo cambia su estado a CANCELADA y guarda
 * fecha, motivo y responsable (el usuario de la sesión). El horario queda libre.
 * Si el motivo está vacío, el modelo ({@code Cancelacion}) lo rechaza.
 * @author SANTANA
 */
public class CancelacionDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    /** Motivos frecuentes: solo rellenan el texto; el operador puede editarlo. */
    private static final String[] MOTIVOS_RAPIDOS = {
        "(escribir motivo)",
        "Lluvia o mal clima",
        "El cliente no se presentó",
        "El cliente reprogramó",
        "Cancha en mantenimiento",
        "Error al registrar la reserva"
    };

    private final transient ReservaController reservas;
    private final String codigoReserva;
    private boolean cancelada;

    private final JComboBox<String> cmbMotivoRapido = new JComboBox<>(MOTIVOS_RAPIDOS);
    private final JTextArea txaMotivo = new JTextArea(4, 34);
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnCancelarReserva = new JButton("Cancelar reserva");
    private final JButton btnVolver = new JButton("Volver");

    /**
     * @throws CanchaProException si la reserva no existe
     */
    public CancelacionDialog(Component origen, SistemaController sistema, String codigoReserva)
            throws CanchaProException {
        super(ventanaDe(origen), "Cancelar reserva " + codigoReserva, ModalityType.APPLICATION_MODAL);
        this.reservas = sistema.getReservaController();
        this.codigoReserva = codigoReserva;
        Reserva r = reservas.obtener(codigoReserva);
        construir(r, sistema.getSesion().getUsuario().getNombre());
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    /** @return true si la reserva se canceló */
    public boolean seCancelo() {
        return cancelada;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir(Reserva r, String responsable) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel raiz = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        raiz.setBorder(Tema.margen());
        raiz.setBackground(Tema.color(ColorTema.SUPERFICIE));

        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setOpaque(false);

        JLabel lblReserva = new JLabel(r.getCliente().getNombre() + "   ·   " + r.getCancha().getNombre()
                + "   ·   " + FechaUtil.formatearRango(r.getInicio(), r.getFin()));
        lblReserva.setFont(Tema.FUENTE_NEGRITA);
        agregar(cuerpo, lblReserva);
        JLabel lblEstado = new JLabel("Estado actual: " + r.getEstado() + "   ·   Total "
                + MonedaUtil.formatear(r.calcularTotal()));
        Tema.textoSecundario(lblEstado);
        agregar(cuerpo, lblEstado);
        cuerpo.add(Box.createVerticalStrut(Tema.ESPACIO));

        // Aviso si ya hubo pagos: NO se borran (quedan en el historial)
        double pagado = r.calcularTotalPagado();
        if (!MonedaUtil.esCero(pagado)) {
            JLabel aviso = new JLabel("<html>⚠ Esta reserva tiene <b>" + MonedaUtil.formatear(pagado)
                    + "</b> pagados. Los pagos se conservan en el historial;<br>"
                    + "la devolución al cliente se gestiona fuera del sistema.</html>");
            aviso.setForeground(Tema.oscurecer(Tema.color(ColorTema.ADVERTENCIA), 0.35));
            aviso.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.color(ColorTema.ADVERTENCIA)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            agregar(cuerpo, aviso);
            cuerpo.add(Box.createVerticalStrut(Tema.ESPACIO));
        }

        JLabel lblRapido = new JLabel("Motivo rápido");
        lblRapido.setFont(Tema.FUENTE_NEGRITA);
        agregar(cuerpo, lblRapido);
        cmbMotivoRapido.setMaximumSize(new Dimension(300, 30));
        cmbMotivoRapido.addActionListener(e -> usarMotivoRapido());
        agregar(cuerpo, cmbMotivoRapido);
        cuerpo.add(Box.createVerticalStrut(Tema.ESPACIO));

        JLabel lblMotivo = new JLabel("Motivo *");
        lblMotivo.setFont(Tema.FUENTE_NEGRITA);
        agregar(cuerpo, lblMotivo);
        txaMotivo.setLineWrap(true);
        txaMotivo.setWrapStyleWord(true);
        txaMotivo.setFont(Tema.FUENTE_NORMAL);
        JScrollPane scr = new JScrollPane(txaMotivo);
        scr.setBorder(BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)));
        agregar(cuerpo, scr);
        cuerpo.add(Box.createVerticalStrut(Tema.ESPACIO));

        JLabel lblResponsable = new JLabel("Cancelada por: " + responsable + "  (se registra la fecha y hora actual)");
        Tema.textoSecundario(lblResponsable);
        agregar(cuerpo, lblResponsable);
        raiz.add(cuerpo, BorderLayout.CENTER);

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        Tema.botonPeligro(btnCancelarReserva);
        Tema.botonSecundario(btnVolver);
        btnCancelarReserva.addActionListener(e -> cancelar());
        btnVolver.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(btnVolver);
        botones.add(btnCancelarReserva);
        JPanel pie = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        pie.setOpaque(false);
        pie.add(lblMensaje, BorderLayout.NORTH);
        pie.add(botones, BorderLayout.SOUTH);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void agregar(JPanel panel, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(c);
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    /** El motivo rápido solo copia el texto al área; el operador puede completarlo. */
    private void usarMotivoRapido() {
        if (cmbMotivoRapido.getSelectedIndex() > 0) {
            txaMotivo.setText((String) cmbMotivoRapido.getSelectedItem());
            txaMotivo.requestFocusInWindow();
        }
    }

    /** UNA llamada al controlador: él registra responsable y fecha; el modelo valida el motivo. */
    private void cancelar() {
        try {
            reservas.cancelar(codigoReserva, txaMotivo.getText());
            cancelada = true;
            dispose();
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaReservas;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.views.dialogs.NuevaReservaDialog;
import com.canchapro.views.dialogs.PagoDialog;
import com.canchapro.views.dialogs.CancelacionDialog;
import com.canchapro.views.dialogs.DetalleReservaDialog;
        
/**
 *
 * @author SANTANA
 */
public class ReservasPanel extends javax.swing.JPanel {

    /**
     * Creates new form Cambiar
     */
        private static final String TODOS = "Todos";

    /** Lo usarán los diálogos de las entregas 6.4 a 6.7 (clientes, canchas, pagos). */
    private final transient SistemaController sistema;
    private final transient ReservaController reservas;
    private final transient Runnable alCambiarDatos;
    private final ModeloTablaReservas modelo = new ModeloTablaReservas();

    /**
     * Pestaña 1 del Dashboard: lista de reservas con filtros, búsqueda y acciones.
     *
     * @param sistema        acceso a los controladores de la sesión
     * @param alCambiarDatos qué hacer después de crear o modificar una reserva
     *                       (el Dashboard pasa "actualizar las tarjetas")
     */
    public ReservasPanel(SistemaController sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.reservas = sistema.getReservaController();
        this.alCambiarDatos = alCambiarDatos;
        initComponents();
        prepararComponentes();
        aplicarTema();
        irAHoy();
    }

    // =====================================================================
    //  PREPARACIÓN
    // =====================================================================

    private void prepararComponentes() {
        modelo.configurar(tblReservas);
        modelo.setCalculoSaldo(sistema.getPagoController()::getSaldoTotal); // saldo + tienda a cuenta
        // Cada vez que cambia la fila seleccionada se habilitan/deshabilitan botones
        tblReservas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotones();
            }
        });

        cmbEstado.removeAllItems();
        cmbEstado.addItem(TODOS);
        for (EstadoReserva estado : reservas.getEstados()) {
            cmbEstado.addItem(estado.getDescripcion());
        }
        txtBuscar.setToolTipText("Código de reserva, cliente (nombre o teléfono) o cancha");
        txtFecha.setToolTipText("dd/MM/yyyy y Enter");
    }

    /** Colores y fuentes del Tema (se puede volver a llamar si el desarrollador los cambia). */
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        pnlFiltros.setBackground(Tema.color(ColorTema.FONDO));
        pnlAcciones.setBackground(Tema.color(ColorTema.FONDO));
        chkTodasFechas.setOpaque(false);
        Tema.campo(txtFecha);
        Tema.campo(txtBuscar);
        for (JLabel lbl : new JLabel[]{lblFecha, lblEstado, lblBuscar}) {
            lbl.setFont(Tema.FUENTE_NEGRITA);
        }
        Tema.textoSecundario(lblConteo);
        for (JButton b : new JButton[]{btnDiaAnterior, btnDiaSiguiente, btnHoy, btnLimpiar, btnDetalle,
            btnPagar, btnConfirmar, btnIniciar, btnFinalizar}) {
            Tema.botonSecundario(b);
        }
        Tema.botonPrimario(btnNueva);
        Tema.botonPeligro(btnCancelar);
        Tema.tabla(tblReservas);
    }

    // =====================================================================
    //  FILTROS
    // =====================================================================

    /** Vuelve a pedir la lista al controlador conservando la fila seleccionada. */
    public void recargar() {
        Reserva antes = seleccionada();
        aplicarFiltros();
        if (antes != null) {
            seleccionar(antes.getCodigo());
        }
    }

    private void aplicarFiltros() {
        LocalDate fecha = null;
        txtFecha.setEnabled(!chkTodasFechas.isSelected());
        if (!chkTodasFechas.isSelected()) {
            try {
                fecha = FechaUtil.leerFecha(txtFecha.getText());
                txtFecha.setForeground(Tema.color(ColorTema.TEXTO));
            } catch (CanchaProException ex) {
                txtFecha.setForeground(Tema.color(ColorTema.ERROR)); // fecha mal escrita: no filtra aún
                return;
            }
        }
        int indice = cmbEstado.getSelectedIndex();
        EstadoReserva estado = indice <= 0 ? null : reservas.getEstados()[indice - 1];

        modelo.setReservas(reservas.filtrar(fecha, estado, txtBuscar.getText()));
        int n = modelo.getRowCount();
        lblConteo.setText(n + (n == 1 ? " reserva" : " reservas"));
        actualizarBotones();
    }

    private void moverDia(int dias) {
        LocalDate actual;
        try {
            actual = FechaUtil.leerFecha(txtFecha.getText());
        } catch (CanchaProException ex) {
            actual = LocalDate.now();
        }
        chkTodasFechas.setSelected(false);
        txtFecha.setText(FechaUtil.formatearFecha(actual.plusDays(dias)));
        aplicarFiltros();
    }

    private void irAHoy() {
        chkTodasFechas.setSelected(false);
        txtFecha.setText(FechaUtil.formatearFecha(LocalDate.now()));
        aplicarFiltros();
    }

    private void limpiarFiltros() {
        txtBuscar.setText("");
        cmbEstado.setSelectedIndex(0);
        chkTodasFechas.setSelected(true);
        aplicarFiltros();
    }

    // =====================================================================
    //  SELECCIÓN Y BOTONES
    // =====================================================================

    /** @return la reserva de la fila seleccionada (convirtiendo el índice si está ordenada) */
    private Reserva seleccionada() {
        int filaVista = tblReservas.getSelectedRow();
        return filaVista < 0 ? null : modelo.getReservaEn(tblReservas.convertRowIndexToModel(filaVista));
    }

    private void seleccionar(String codigo) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getReservaEn(i).getCodigo().equals(codigo)) {
                int filaVista = tblReservas.convertRowIndexToView(i);
                tblReservas.setRowSelectionInterval(filaVista, filaVista);
                tblReservas.scrollRectToVisible(tblReservas.getCellRect(filaVista, 0, true));
                return;
            }
        }
    }

    /**
     * Habilita solo las acciones que la reserva permite. La regla la decide el
     * MODELO ({@code EstadoReserva.puedeTransicionarA}); la vista solo pregunta.
     */
     private void actualizarBotones() {
        Reserva r = seleccionada();
        EstadoReserva e = r == null ? null : r.getEstado();
        btnDetalle.setEnabled(r != null);
        btnPagar.setEnabled(r != null && sistema.getPagoController().tieneAlgoPorCobrar(r));
        btnConfirmar.setEnabled(e != null && e.puedeTransicionarA(EstadoReserva.CONFIRMADA));
        btnIniciar.setEnabled(e != null && e.puedeTransicionarA(EstadoReserva.EN_CURSO));
        btnFinalizar.setEnabled(e != null && e.puedeTransicionarA(EstadoReserva.FINALIZADA));
        btnCancelar.setEnabled(e != null && e.puedeTransicionarA(EstadoReserva.CANCELADA));
    }

    // =====================================================================
    //  ACCIONES (cada una: 1 llamada al controlador + mensaje)
    // =====================================================================

    private void confirmar() {
        Reserva r = seleccionada();
        if (r == null) {
            return;
        }
        try {
            reservas.confirmar(r.getCodigo());
            despuesDeCambiar("Reserva " + r.getCodigo() + " confirmada.");
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void iniciar() {
        Reserva r = seleccionada();
        if (r == null) {
            return;
        }
        try {
            reservas.iniciar(r.getCodigo());
            despuesDeCambiar("Partido de la reserva " + r.getCodigo() + " iniciado.");
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void finalizar() {
        Reserva r = seleccionada();
        if (r == null || !confirmar("¿Finalizar el partido de la reserva " + r.getCodigo() + "?")) {
            return;
        }
        try {
            reservas.finalizar(r.getCodigo());
            despuesDeCambiar("Reserva " + r.getCodigo() + " finalizada.");
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** Abre la ventana de Nueva reserva; si se creó, la muestra seleccionada en la tabla. */
    private void nuevaReserva() {
        NuevaReservaDialog dlg = new NuevaReservaDialog(this, sistema);
        dlg.setVisible(true); // modal: espera hasta que se cierre
        Reserva nueva = dlg.getReservaCreada();
        if (nueva == null) {
            return; // canceló
        }
        // Ir a la fecha de la nueva reserva para que se vea en la tabla
        chkTodasFechas.setSelected(false);
        txtFecha.setText(FechaUtil.formatearFecha(nueva.getInicio().toLocalDate()));
        cmbEstado.setSelectedIndex(0);
        txtBuscar.setText("");
        aplicarFiltros();
        seleccionar(nueva.getCodigo());
        alCambiarDatos.run();
        JOptionPane.showMessageDialog(this, "Reserva " + nueva.getCodigo() + " creada para "
                + nueva.getCliente().getNombre() + ".\nEstado: Pendiente. Total: "
                + MonedaUtil.formatear(nueva.calcularTotal()), "Reservas", JOptionPane.INFORMATION_MESSAGE);
    }

   /** Abre la ventana de pagos de la reserva seleccionada. */
    private void registrarPago() {
        Reserva r = seleccionada();
        if (r == null) {
            return;
        }
        try {
            PagoDialog dlg = new PagoDialog(this, sistema, r.getCodigo());
            dlg.setVisible(true); // modal: espera hasta que se cierre
            if (dlg.huboCambios()) {
                recargar();
                alCambiarDatos.run(); // "Cobrado hoy" y "Saldo pendiente" cambian
            }
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** Abre la ventana de cancelación (motivo obligatorio) de la reserva seleccionada. */
    private void cancelar() {
        Reserva r = seleccionada();
        if (r == null) {
            return;
        }
        try {
            CancelacionDialog dlg = new CancelacionDialog(this, sistema, r.getCodigo());
            dlg.setVisible(true); // modal: espera hasta que se cierre
            if (dlg.seCancelo()) {
                despuesDeCambiar("Reserva " + r.getCodigo() + " cancelada. El horario quedó libre.");
            }
        } catch (CanchaProException ex) {
            error(ex);
        }
    }
    /** Abre la ficha completa (solo lectura) de la reserva seleccionada. */
    private void verDetalle() {
        Reserva r = seleccionada();
        if (r == null) {
            return;
        }
        try {
            new DetalleReservaDialog(this, sistema, r.getCodigo()).setVisible(true);
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    // =====================================================================
    //  APOYO
    // =====================================================================

    /** Después de un cambio: recarga la tabla y avisa al Dashboard (tarjetas). */
    private void despuesDeCambiar(String mensaje) {
        recargar();
        alCambiarDatos.run();
        JOptionPane.showMessageDialog(this, mensaje, "Reservas", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(this, pregunta, "Reservas", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    private void pendiente(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Reservas", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Reservas", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlFiltros = new javax.swing.JPanel();
        lblFecha = new javax.swing.JLabel();
        btnDiaAnterior = new javax.swing.JButton();
        txtFecha = new javax.swing.JTextField();
        btnDiaSiguiente = new javax.swing.JButton();
        btnHoy = new javax.swing.JButton();
        chkTodasFechas = new javax.swing.JCheckBox();
        lblEstado = new javax.swing.JLabel();
        cmbEstado = new javax.swing.JComboBox<>();
        lblBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        btnLimpiar = new javax.swing.JButton();
        scrReservas = new javax.swing.JScrollPane();
        tblReservas = new javax.swing.JTable();
        pnlAcciones = new javax.swing.JPanel();
        lblConteo = new javax.swing.JLabel();
        btnNueva = new javax.swing.JButton();
        btnDetalle = new javax.swing.JButton();
        btnPagar = new javax.swing.JButton();
        btnConfirmar = new javax.swing.JButton();
        btnIniciar = new javax.swing.JButton();
        btnFinalizar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        pnlFiltros.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 8));

        lblFecha.setText("Fecha:");
        pnlFiltros.add(lblFecha);

        btnDiaAnterior.setText("<");
        btnDiaAnterior.addActionListener(this::btnDiaAnteriorActionPerformed);
        pnlFiltros.add(btnDiaAnterior);

        txtFecha.setColumns(9);
        txtFecha.addActionListener(this::txtFechaActionPerformed);
        pnlFiltros.add(txtFecha);

        btnDiaSiguiente.setText(">");
        btnDiaSiguiente.addActionListener(this::btnDiaSiguienteActionPerformed);
        pnlFiltros.add(btnDiaSiguiente);

        btnHoy.setText("Hoy");
        btnHoy.addActionListener(this::btnHoyActionPerformed);
        pnlFiltros.add(btnHoy);

        chkTodasFechas.setText("Todas las fechas");
        chkTodasFechas.addActionListener(this::chkTodasFechasActionPerformed);
        pnlFiltros.add(chkTodasFechas);

        lblEstado.setText("Estado:");
        pnlFiltros.add(lblEstado);

        cmbEstado.addActionListener(this::cmbEstadoActionPerformed);
        pnlFiltros.add(cmbEstado);

        lblBuscar.setText("Buscar:");
        pnlFiltros.add(lblBuscar);

        txtBuscar.setColumns(16);
        txtBuscar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBuscarKeyReleased(evt);
            }
        });
        pnlFiltros.add(txtBuscar);

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        pnlFiltros.add(btnLimpiar);

        add(pnlFiltros, java.awt.BorderLayout.PAGE_START);

        tblReservas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblReservas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblReservasMouseClicked(evt);
            }
        });
        scrReservas.setViewportView(tblReservas);

        add(scrReservas, java.awt.BorderLayout.CENTER);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 8));

        lblConteo.setText("0 reservas");
        pnlAcciones.add(lblConteo);

        btnNueva.setText("Nueva Reserva");
        btnNueva.addActionListener(this::btnNuevaActionPerformed);
        pnlAcciones.add(btnNueva);

        btnDetalle.setText("Ver detalle");
        btnDetalle.addActionListener(this::btnDetalleActionPerformed);
        pnlAcciones.add(btnDetalle);

        btnPagar.setText("Registrar pago");
        btnPagar.addActionListener(this::btnPagarActionPerformed);
        pnlAcciones.add(btnPagar);

        btnConfirmar.setText("Confirmar");
        btnConfirmar.addActionListener(this::btnConfirmarActionPerformed);
        pnlAcciones.add(btnConfirmar);

        btnIniciar.setText("Iniciar");
        btnIniciar.addActionListener(this::btnIniciarActionPerformed);
        pnlAcciones.add(btnIniciar);

        btnFinalizar.setText("Finalizar");
        btnFinalizar.addActionListener(this::btnFinalizarActionPerformed);
        pnlAcciones.add(btnFinalizar);

        btnCancelar.setText("Cancelar reserva");
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);
        pnlAcciones.add(btnCancelar);

        add(pnlAcciones, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void btnDiaAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDiaAnteriorActionPerformed
        moverDia(-1);
    }//GEN-LAST:event_btnDiaAnteriorActionPerformed

    private void txtFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFechaActionPerformed
        aplicarFiltros();
    }//GEN-LAST:event_txtFechaActionPerformed

    private void btnDiaSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDiaSiguienteActionPerformed
        moverDia(1);
    }//GEN-LAST:event_btnDiaSiguienteActionPerformed

    private void btnHoyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHoyActionPerformed
        irAHoy();
    }//GEN-LAST:event_btnHoyActionPerformed

    private void chkTodasFechasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkTodasFechasActionPerformed
        aplicarFiltros();
    }//GEN-LAST:event_chkTodasFechasActionPerformed

    private void txtBuscarKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBuscarKeyReleased
        aplicarFiltros();
    }//GEN-LAST:event_txtBuscarKeyReleased

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltros();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void tblReservasMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblReservasMouseClicked
        if (evt.getClickCount() == 2) { verDetalle(); }
    }//GEN-LAST:event_tblReservasMouseClicked

    private void btnNuevaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaActionPerformed
        nuevaReserva();
    }//GEN-LAST:event_btnNuevaActionPerformed

    private void btnDetalleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetalleActionPerformed
        verDetalle();
    }//GEN-LAST:event_btnDetalleActionPerformed

    private void btnPagarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPagarActionPerformed
        registrarPago();
    }//GEN-LAST:event_btnPagarActionPerformed

    private void btnConfirmarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarActionPerformed
        confirmar();
    }//GEN-LAST:event_btnConfirmarActionPerformed

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        iniciar();
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void btnFinalizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarActionPerformed
        finalizar();
    }//GEN-LAST:event_btnFinalizarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        	cancelar();
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void cmbEstadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbEstadoActionPerformed
        // TODO add your handling code here
        aplicarFiltros();
    }//GEN-LAST:event_cmbEstadoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnConfirmar;
    private javax.swing.JButton btnDetalle;
    private javax.swing.JButton btnDiaAnterior;
    private javax.swing.JButton btnDiaSiguiente;
    private javax.swing.JButton btnFinalizar;
    private javax.swing.JButton btnHoy;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnNueva;
    private javax.swing.JButton btnPagar;
    private javax.swing.JCheckBox chkTodasFechas;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblConteo;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlFiltros;
    private javax.swing.JScrollPane scrReservas;
    private javax.swing.JTable tblReservas;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtFecha;
    // End of variables declaration//GEN-END:variables
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 ** Ventana de formulario REUTILIZABLE: filas "Etiqueta  [campo]", un texto de
 * ayuda, el mensaje de error en rojo y los botones Cancelar / Guardar.
 *
 * Quien la usa solo arma los campos y dice QUÉ HACER al guardar (una llamada
 * al controlador). Si el controlador lanza una excepción, el mensaje aparece en
 * rojo y la ventana sigue abierta para corregir
 *
 * 
 * DialogoFormulario f = new DialogoFormulario(this, "Nueva cancha");
 * f.fila("Nombre *", txtNombre);
 * f.fila("Tipo *", cmbTipo);
 * boolean guardo = f.mostrar(() -&gt; canchas.crear(txtNombre.getText(), ...));
 * 
 * @author SANTANA
 */
public class DialogoFormulario extends JDialog{
    private static final long serialVersionUID = 1L;

    /** Lo que se ejecuta al pulsar Guardar; si lanza excepción, el formulario no se cierra. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar() throws CanchaProException;
    }

    private final JPanel formulario = new JPanel(new GridBagLayout());
    private final GridBagConstraints g = new GridBagConstraints();
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnCancelar = new JButton("Cancelar");
    private int filas;
    private transient Accion accion;
    private boolean guardado;

    public DialogoFormulario(Component origen, String titulo) {
        super(ventanaDe(origen), titulo, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        formulario.setBorder(Tema.margen());
        formulario.setBackground(Tema.color(ColorTema.SUPERFICIE));
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        Tema.botonPrimario(btnGuardar);
        Tema.botonSecundario(btnCancelar);
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, Tema.ESPACIO));
        botones.setBackground(Tema.color(ColorTema.SUPERFICIE));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(formulario, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    /** Agrega una fila "etiqueta  campo". */
    public DialogoFormulario fila(String etiqueta, JComponent campo) {
        JLabel l = new JLabel(etiqueta);
        l.setFont(Tema.FUENTE_NEGRITA);
        g.gridx = 0;
        g.gridy = filas;
        g.gridwidth = 1;
        formulario.add(l, g);
        if (campo instanceof JTextField) {
            Tema.campo((JTextField) campo);
        }
        if (campo.getPreferredSize().width < 260) {
            campo.setPreferredSize(new Dimension(260, Math.max(30, campo.getPreferredSize().height)));
        }
        g.gridx = 1;
        formulario.add(campo, g);
        filas++;
        return this;
    }

    /** Agrega un texto gris de ayuda debajo de los campos. */
    public DialogoFormulario ayuda(String texto) {
        JLabel l = new JLabel(texto);
        Tema.textoSecundario(l);
        g.gridx = 1;
        g.gridy = filas++;
        g.gridwidth = 1;
        formulario.add(l, g);
        return this;
    }

    /**
     * Muestra la ventana (modal) y espera.
     *
     * @param alGuardar llamada al controlador
     * @return true si se guardó sin errores; false si se canceló
     */
    public boolean mostrar(Accion alGuardar) {
        this.accion = alGuardar;
        g.gridx = 0;
        g.gridy = filas;
        g.gridwidth = 2;
        formulario.add(lblMensaje, g);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setVisible(true); // aquí se queda esperando hasta que se cierre
        return guardado;
    }

    private void guardar() {
        try {
            accion.ejecutar();
            guardado = true;
            dispose();
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
            pack();
        }
    }
}

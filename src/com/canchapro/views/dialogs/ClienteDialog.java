/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.ClienteController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Cliente;
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
 *
 * Formulario para REGISTRAR o EDITAR un cliente (modal).
 *
 * Se usa desde dos lugares:
 * 
 *   Nueva reserva (6.4): botón "Nuevo cliente" para registrarlo sin salir.
 *   Módulo Clientes (7.1): botones Nuevo y Editar.
 * 
 *
 * 
 *   ┌ Nuevo cliente ─────────────────────┐
 *   │ Código      CL0006                 │
 *   │ Nombre *    [                    ] │
 *   │ Teléfono *  [                    ] │
 *   │ Correo      [                    ] │
 *   │ (mensaje de error en rojo)         │
 *   │            [Cancelar] [Guardar]    │
 *   └────────────────────────────────────┘
 * 
 *
 * MVC: la vista NO valida (teléfono de 8 dígitos, correo, duplicados); solo
 * llama a {@code registrar} o {@code actualizar} del {@link ClienteController}
 * y, si hay error, muestra el mensaje que trae la excepción.
 *
 * Uso:
 * 
 *   ClienteDialog dlg = new ClienteDialog(this, clienteController, null);  // null = nuevo
 *   dlg.setVisible(true);                       // espera aquí (modal)
 *   Cliente creado = dlg.getClienteGuardado();  // null si canceló
 * 
 * @author SANTANA
 */
public class ClienteDialog extends JDialog{
    private static final long serialVersionUID = 1L;

    private final transient ClienteController clientes;
    private final transient Cliente aEditar;      // null = registrar uno nuevo
    private transient Cliente guardado;           // resultado (null si canceló)

    private final JLabel lblCodigo = new JLabel();
    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtTelefono = new JTextField(24);
    private final JTextField txtCorreo = new JTextField(24);
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnCancelar = new JButton("Cancelar");

    /**
     * @param origen   componente desde el que se abre (un panel o una ventana)
     * @param clientes controlador de clientes de la sesión
     * @param aEditar  cliente a modificar, o {@code null} para registrar uno nuevo
     */
    public ClienteDialog(Component origen, ClienteController clientes, Cliente aEditar) {
        super(ventanaDe(origen),
                aEditar == null ? "Nuevo cliente" : "Editar cliente", ModalityType.APPLICATION_MODAL);
        this.clientes = clientes;
        this.aEditar = aEditar;
        construir();
        cargarDatos();
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    /** La ventana que "contiene" al componente (o él mismo si ya es una ventana). */
    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    /** @return el cliente registrado/actualizado, o {@code null} si se canceló */
    public Cliente getClienteGuardado() {
        return guardado;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(Tema.margen());
        formulario.setBackground(Tema.color(ColorTema.SUPERFICIE));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        agregarFila(formulario, g, 0, "Código", lblCodigo);
        agregarFila(formulario, g, 1, "Nombre *", txtNombre);
        agregarFila(formulario, g, 2, "Teléfono *", txtTelefono);
        agregarFila(formulario, g, 3, "Correo", txtCorreo);

        JLabel lblAyuda = new JLabel("* obligatorio · Teléfono de 8 dígitos · Correo opcional");
        Tema.textoSecundario(lblAyuda);
        g.gridx = 1;
        g.gridy = 4;
        formulario.add(lblAyuda, g);

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        g.gridx = 0;
        g.gridy = 5;
        g.gridwidth = 2;
        formulario.add(lblMensaje, g);

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

        // Enter = Guardar, Esc = Cancelar
        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void agregarFila(JPanel panel, GridBagConstraints g, int fila, String texto, JComponent campo) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Tema.FUENTE_NEGRITA);
        g.gridx = 0;
        g.gridy = fila;
        g.gridwidth = 1;
        panel.add(etiqueta, g);
        if (campo instanceof JTextField) {
            Tema.campo((JTextField) campo);
            campo.setPreferredSize(new Dimension(260, 30));
        }
        g.gridx = 1;
        panel.add(campo, g);
    }

    /** Nuevo: muestra el código que se asignará. Editar: llena los campos actuales. */
    private void cargarDatos() {
        if (aEditar == null) {
            lblCodigo.setText(clientes.getSiguienteCodigo());
        } else {
            lblCodigo.setText(aEditar.getCodigo());
            txtNombre.setText(aEditar.getNombre());
            txtTelefono.setText(aEditar.getTelefono());
            txtCorreo.setText(aEditar.getCorreo());
        }
        lblCodigo.setFont(Tema.FUENTE_NEGRITA);
        lblCodigo.setForeground(Tema.color(ColorTema.PRIMARIO));
    }

    // =====================================================================
    //  ACCIÓN
    // =====================================================================

    /** Una sola llamada al controlador; si falla, el mensaje se muestra en rojo. */
    private void guardar() {
        try {
            if (aEditar == null) {
                guardado = clientes.registrar(txtNombre.getText(), txtTelefono.getText(), txtCorreo.getText());
            } else {
                guardado = clientes.actualizar(aEditar.getCodigo(), txtNombre.getText(),
                        txtTelefono.getText(), txtCorreo.getText());
            }
            dispose();
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
            pack(); // por si el mensaje es largo
        }
    }
}

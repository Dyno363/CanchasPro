/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.UsuarioController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Usuario;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
/**
 *
 * Formulario para CREAR o EDITAR un usuario del sistema (construido por código).
 *
 * 
 *  ┌ Nuevo usuario ──────────────────────────────┐
 *  │ Nombre *        [____________________]│
 *  │ Teléfono *      [____________________]│
 *  │ Usuario *       [____________________]│  ← en Editar se ve pero no se cambia
 *  │ Rol *           [Operador         ▼]  │
 *  │ Contraseña *    [••••••••]               │  ← solo al crear
 *  │ Confirmar *     [••••••••]               │
 *  │ (mensaje de error)                     │
 *  │                   [Cancelar] [Guardar] │
 *  └─────────────────────────────────────────────┘
 * 
 *
 * Las contraseñas se entregan al controlador como {@code char[]} (nunca como
 * String) y el controlador las borra de memoria después de calcular el hash
 *
 * @author SANTANA
 */
public class UsuarioDialog extends JDialog{
    private static final long serialVersionUID = 1L;

    private final transient UsuarioController usuarios;
    private final transient Usuario aEditar;     // null = crear uno nuevo
    private transient Usuario guardado;          // resultado (null si canceló)

    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtTelefono = new JTextField(22);
    private final JTextField txtUsername = new JTextField(22);
    private final JComboBox<RolUsuario> cmbRol;
    private final JPasswordField pwdContrasena = new JPasswordField(22);
    private final JPasswordField pwdConfirmacion = new JPasswordField(22);
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnCancelar = new JButton("Cancelar");

    /**
     * @param origen   componente desde el que se abre
     * @param usuarios controlador de usuarios de la sesión
     * @param aEditar  usuario a modificar, o {@code null} para crear uno nuevo
     */
    public UsuarioDialog(Component origen, UsuarioController usuarios, Usuario aEditar) {
        super(ventanaDe(origen), aEditar == null ? "Nuevo usuario" : "Editar usuario " + aEditar.getCodigo(),
                ModalityType.APPLICATION_MODAL);
        this.usuarios = usuarios;
        this.aEditar = aEditar;
        this.cmbRol = new JComboBox<>(usuarios.getRoles());
        construir();
        cargarDatos();
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

    /** @return el usuario creado/actualizado, o {@code null} si se canceló */
    public Usuario getUsuarioGuardado() {
        return guardado;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        boolean nuevo = aEditar == null;

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(Tema.margen());
        formulario.setBackground(Tema.color(ColorTema.SUPERFICIE));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        int fila = 0;
        agregarFila(formulario, g, fila++, "Nombre *", txtNombre);
        agregarFila(formulario, g, fila++, "Teléfono *", txtTelefono);
        agregarFila(formulario, g, fila++, "Usuario *", txtUsername);
        agregarFila(formulario, g, fila++, "Rol *", cmbRol);
        if (nuevo) {
            agregarFila(formulario, g, fila++, "Contraseña *", pwdContrasena);
            agregarFila(formulario, g, fila++, "Confirmar *", pwdConfirmacion);
        }

        JLabel lblAyuda = new JLabel(nuevo
                ? "* obligatorio · Contraseña de " + UsuarioController.MIN_CONTRASENA + " caracteres o más, sin espacios"
                : "El nombre de usuario no se cambia (es con el que inicia sesión).");
        Tema.textoSecundario(lblAyuda);
        g.gridx = 1;
        g.gridy = fila++;
        formulario.add(lblAyuda, g);

        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        g.gridx = 0;
        g.gridy = fila;
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

        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void agregarFila(JPanel panel, GridBagConstraints g, int fila, String texto, JComponent campo) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Tema.FUENTE_NEGRITA);
        g.gridx = 0;
        g.gridy = fila;
        g.gridwidth = 1;
        panel.add(etiqueta, g);
        if (campo instanceof JTextField) {
            Tema.campo((JTextField) campo);
        }
        campo.setPreferredSize(new Dimension(260, 30));
        g.gridx = 1;
        panel.add(campo, g);
    }

    private void cargarDatos() {
        if (aEditar == null) {
            cmbRol.setSelectedItem(RolUsuario.OPERADOR); // lo más común es crear operadores
        } else {
            txtNombre.setText(aEditar.getNombre());
            txtTelefono.setText(aEditar.getTelefono());
            txtUsername.setText(aEditar.getUsername());
            txtUsername.setEditable(false);
            cmbRol.setSelectedItem(aEditar.getRol());
        }
    }

    // =====================================================================
    //  ACCIÓN
    // =====================================================================

    /** Una sola llamada al controlador; si falla, el mensaje se muestra en rojo. */
    private void guardar() {
        RolUsuario rol = (RolUsuario) cmbRol.getSelectedItem();
        try {
            if (aEditar == null) {
                guardado = usuarios.crear(txtNombre.getText(), txtTelefono.getText(), txtUsername.getText(),
                        rol, pwdContrasena.getPassword(), pwdConfirmacion.getPassword());
            } else {
                guardado = usuarios.actualizar(aEditar.getCodigo(), txtNombre.getText(),
                        txtTelefono.getText(), rol);
            }
            dispose();
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
            pack();
        }
    }
}

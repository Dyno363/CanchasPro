/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.canchapro.views;
import com.canchapro.controllers.LoginController;
import com.canchapro.controllers.Sesion;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.utils.Tema;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JOptionPane;
/**
 *
 * @author SANTANA
 */
public class LoginView extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginView.class.getName());
    private final SistemaController sistema;
    private final LoginController login;
    /** Carácter con que se ocultan las letras de la contraseña (para "Mostrar contraseña"). */
    private char ecoOriginal;
    /**
     * Creates new form LoginView
     */
    public LoginView(SistemaController sistema) {
        this.sistema = sistema;
        this.login = sistema.getLoginController();
        initComponents();
        aplicarEstilos();
        cargarDatos();
        getRootPane().setDefaultButton(btnIngresar); // Enter = Ingresar
        setLocationRelativeTo(null);                  // centrada en pantalla
    }
    // =====================================================================
    //  PREPARACIÓN DE LA VENTANA
    // =====================================================================

    /** Colores y fuentes desde el Tema (así respetan los colores del desarrollador). */
    private void aplicarEstilos() {
        Color marca = Tema.color(ColorTema.PRIMARIO);
        Color textoMarca = Tema.textoSobre(marca);
        Color superficie = Tema.color(ColorTema.SUPERFICIE);

        pnlMarca.setBackground(marca);
        lblLogo.setFont(new Font(Tema.FAMILIA, Font.BOLD, 40));
        lblLogo.setForeground(textoMarca);
        lblNombreComplejo.setFont(Tema.FUENTE_SUBTITULO);
        lblNombreComplejo.setForeground(textoMarca);
        lblEslogan.setFont(Tema.FUENTE_NORMAL);
        lblEslogan.setForeground(textoMarca);

        pnlFormulario.setBackground(superficie);
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        for (javax.swing.JLabel etiqueta : new javax.swing.JLabel[]{lblUsuario, lblContrasena}) {
            etiqueta.setFont(Tema.FUENTE_NEGRITA);
            etiqueta.setForeground(Tema.color(ColorTema.TEXTO));
        }
        Tema.campo(txtUsuario);
        Tema.campo(pwdContrasena);
        chkMostrar.setBackground(superficie);
        chkMostrar.setFont(Tema.FUENTE_PEQUENA);
        chkMostrar.setFocusPainted(false);
        Tema.botonPrimario(btnIngresar);
        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        Tema.textoSecundario(lblIntentos);
        Tema.textoSecundario(lblVersion);
    }

    /** Textos que vienen del controlador (nombre del complejo en config.properties). */
    private void cargarDatos() {
        setTitle("CanchaPro - Iniciar sesión");
        lblNombreComplejo.setText(login.getNombreComplejo());
        ecoOriginal = pwdContrasena.getEchoChar();
        limpiarMensaje();
    }

    // =====================================================================
    //  ACCIONES (los eventos solo llaman a estos métodos)
    // =====================================================================

    private void ingresar() {
        String usuario = txtUsuario.getText();
        char[] contrasena = pwdContrasena.getPassword(); // el controlador la borra
        try {
            Sesion sesion = sistema.iniciarSesion(usuario, contrasena);
            abrirDashboard(sesion);
        } catch (CanchaProException ex) {
            pwdContrasena.setText("");
            mostrarError(ex.getMessage());
            if (login.isBloqueado()) {
                JOptionPane.showMessageDialog(this, ex.getMessage() + "\nEl programa se cerrará.",
                        "Acceso bloqueado", JOptionPane.ERROR_MESSAGE);
                salir();
            } else {
                pwdContrasena.requestFocusInWindow();
            }
        }
    }

      /** Abre la ventana principal y cierra el login (la sesión ya está iniciada). */
    private void abrirDashboard(Sesion sesion) {
        new DashboardView(sistema).setVisible(true);
        dispose();
    }

    /** Cerrar la ventana = cerrar el programa: aquí se guardan los CSV. */
    private void salir() {
        try {
            System.out.println(sistema.cerrar());
        } catch (CanchaProException ex) {
            int respuesta = JOptionPane.showConfirmDialog(this, ex.getMessage()
                    + "\n¿Desea salir de todas formas?", "Error al guardar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return; // se queda abierto para no perder datos
            }
        }
        dispose();
        System.exit(0);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblIntentos.setText("Intentos restantes: " + login.getIntentosRestantes());
    }

    private void limpiarMensaje() {
        lblMensaje.setText(" ");
        lblIntentos.setText("Intentos restantes: " + login.getIntentosRestantes());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlMarca = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        lblNombreComplejo = new javax.swing.JLabel();
        lblEslogan = new javax.swing.JLabel();
        pnlFormulario = new javax.swing.JPanel();
        txtUsuario = new javax.swing.JTextField();
        lblUsuario = new javax.swing.JLabel();
        lblTitulo = new javax.swing.JLabel();
        lblSubtitulo = new javax.swing.JLabel();
        lblIntentos = new javax.swing.JLabel();
        pwdContrasena = new javax.swing.JPasswordField();
        chkMostrar = new javax.swing.JCheckBox();
        btnIngresar = new javax.swing.JButton();
        lblContrasena = new javax.swing.JLabel();
        lblMensaje = new javax.swing.JLabel();
        lblVersion = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(760, 480));
        setResizable(false);
        setSize(new java.awt.Dimension(760, 480));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridLayout(1, 2));

        lblLogo.setText("CanchaPro");

        lblNombreComplejo.setText("Nombre del Complejo");

        lblEslogan.setText("Reservas, mapa de canchas y cafeteria");

        javax.swing.GroupLayout pnlMarcaLayout = new javax.swing.GroupLayout(pnlMarca);
        pnlMarca.setLayout(pnlMarcaLayout);
        pnlMarcaLayout.setHorizontalGroup(
            pnlMarcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarcaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMarcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblNombreComplejo, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                    .addComponent(lblLogo, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(pnlMarcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pnlMarcaLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblEslogan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        pnlMarcaLayout.setVerticalGroup(
            pnlMarcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMarcaLayout.createSequentialGroup()
                .addContainerGap(155, Short.MAX_VALUE)
                .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblNombreComplejo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(177, 177, 177))
            .addGroup(pnlMarcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMarcaLayout.createSequentialGroup()
                    .addContainerGap(273, Short.MAX_VALUE)
                    .addComponent(lblEslogan, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(123, 123, 123)))
        );

        getContentPane().add(pnlMarca);

        txtUsuario.addActionListener(this::txtUsuarioActionPerformed);

        lblUsuario.setText("Usuario");

        lblTitulo.setText("Inicio de sesion");

        lblSubtitulo.setText("Ingrese sus credenciales de operador");

        lblIntentos.setText(" ");

        chkMostrar.setText("Mostrar Contraseña");
        chkMostrar.addActionListener(this::chkMostrarActionPerformed);

        btnIngresar.setText("Ingresar");
        btnIngresar.addActionListener(this::btnIngresarActionPerformed);

        lblContrasena.setText("Contraseña");

        lblMensaje.setText(" ");

        lblVersion.setText("Sistema Canchas Pro");

        javax.swing.GroupLayout pnlFormularioLayout = new javax.swing.GroupLayout(pnlFormulario);
        pnlFormulario.setLayout(pnlFormularioLayout);
        pnlFormularioLayout.setHorizontalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlFormularioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnIngresar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblUsuario, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pwdContrasena, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSubtitulo, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlFormularioLayout.createSequentialGroup()
                        .addComponent(chkMostrar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(lblContrasena, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblMensaje, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblIntentos, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblVersion, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlFormularioLayout.setVerticalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormularioLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSubtitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pwdContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkMostrar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnIngresar, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblIntentos, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(pnlFormulario);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtUsuarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUsuarioActionPerformed
        // TODO add your handling code here:
        pwdContrasena.requestFocusInWindow(); // Enter en Usuario pasa a Contraseña
    }//GEN-LAST:event_txtUsuarioActionPerformed

    private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIngresarActionPerformed
        // TODO add your handling code here:
         ingresar();
         pwdContrasena.setText("");
         txtUsuario.setText("");
         
    }//GEN-LAST:event_btnIngresarActionPerformed

    private void chkMostrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMostrarActionPerformed
        // TODO add your handling code here:
        pwdContrasena.setEchoChar(chkMostrar.isSelected() ? (char) 0 : ecoOriginal);
    }//GEN-LAST:event_chkMostrarActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        salir();
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnIngresar;
    private javax.swing.JCheckBox chkMostrar;
    private javax.swing.JLabel lblContrasena;
    private javax.swing.JLabel lblEslogan;
    private javax.swing.JLabel lblIntentos;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblNombreComplejo;
    private javax.swing.JLabel lblSubtitulo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JLabel lblVersion;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JPanel pnlMarca;
    private javax.swing.JPasswordField pwdContrasena;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}

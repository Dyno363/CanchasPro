/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.ConfiguracionController;
import com.canchapro.controllers.Sesion;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.UsuarioController;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.dialogs.DialogoFormulario;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
/**
 * * Módulo CONFIGURACIÓN del menú lateral (construido por código).
 *
 * 
 *  ┌ Configuración ──────────────────────────────────────────┐
 *  │ ┌ Parámetros del complejo ────────────────────────────┐ │
 *  │ │ Nombre    [Complejo La 10        ]                  │ │
 *  │ │ Limpieza  [ 0 ] minutos   Anticipo [ 0 ] %          │ │
 *  │ │ (mensaje)                        [Aplicar cambios]  │ │
 *  │ └─────────────────────────────────────────────────────┘ │
 *  │ ┌ Datos y archivos ───────────────────────────────────┐ │
 *  │ │ Carpeta · Último guardado · [Guardar sistema ahora] │ │
 *  │ │ Resultado de la carga + lista de avisos · Respaldos │ │
 *  │ └─────────────────────────────────────────────────────┘ │
 *  │ ┌ Mi cuenta ──────────────────────────────────────────┐ │
 *  │ │ Administrador General (admin) [Cambiar contraseña]  │ │
 *  │ └─────────────────────────────────────────────────────┘ │
 *  └─────────────────────────────────────────────────────────┘
 * 
 *
 * Los parámetros cambian la MEMORIA al instante; los archivos solo se escriben
 * con "Guardar sistema ahora" (o al cerrar el programa). El interruptor de la
 * Tienda NO está aquí: es del panel oculto del desarrollador.
 * @author SANTANA
 */
public class ConfiguracionPanel extends JPanel implements PanelModulo{
    private static final long serialVersionUID = 1L;

    private final transient ConfiguracionController configuracion;
    private final transient UsuarioController usuarios;
    private final transient Sesion sesion;
    /** Avisa al Dashboard (por ejemplo, el nombre del complejo cambió). */
    private final transient Runnable alCambiarDatos;

    private final JLabel lblTitulo = new JLabel("Configuración");
    private final JLabel lblSubtitulo = new JLabel("Parámetros del complejo, guardado de datos y tu cuenta.");

    // --- Parámetros
    private final JPanel crdParametros = new JPanel(new BorderLayout(0, Tema.ESPACIO));
    private final JTextField txtNombre = new JTextField(24);
    private final JSpinner spnLimpieza = new JSpinner(new SpinnerNumberModel(0, 0, 120, 5));
    private final JSpinner spnAnticipo = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 5.0));
    private final JButton btnAplicar = new JButton("Aplicar cambios");
    private final JLabel lblMensajeParametros = new JLabel(" ");

    // --- Datos y archivos
    private final JPanel crdArchivos = new JPanel(new BorderLayout(0, Tema.ESPACIO));
    private final JLabel lblCarpeta = new JLabel(" ");
    private final JLabel lblUltimoGuardado = new JLabel("Último guardado en esta sesión: todavía no");
    private final JButton btnGuardar = new JButton("Guardar sistema ahora");
    private final JLabel lblResumenCarga = new JLabel(" ");
    private final DefaultListModel<String> modeloAvisos = new DefaultListModel<>();
    private final JList<String> lstAvisos = new JList<>(modeloAvisos);
    private final JLabel lblRespaldos = new JLabel(" ");

    // --- Mi cuenta
    private final JPanel crdCuenta = new JPanel(new BorderLayout(0, Tema.ESPACIO));
    private final JLabel lblUsuario = new JLabel(" ");
    private final JButton btnContrasena = new JButton("Cambiar mi contraseña");

    private final List<JLabel> titulosTarjetas = new ArrayList<>();

    public ConfiguracionPanel(SistemaController sistema, Runnable alCambiarDatos) {
        this.configuracion = sistema.getConfiguracionController();
        this.usuarios = sistema.getUsuarioController();
        this.sesion = sistema.getSesion();
        this.alCambiarDatos = alCambiarDatos == null ? () -> { } : alCambiarDatos;
        construir();
        btnAplicar.addActionListener(e -> aplicarParametros());
        btnGuardar.addActionListener(e -> guardarSistema());
        btnContrasena.addActionListener(e -> cambiarMiContrasena());
        aplicarTema();
        recargar();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setLayout(new BorderLayout(0, Tema.MARGEN));
        setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);
        add(titulos, BorderLayout.NORTH);

        // ---- Tarjeta: parámetros
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 0, 4, 10);
        g.anchor = GridBagConstraints.WEST;
        fila(formulario, g, 0, "Nombre del complejo", txtNombre, "Aparece en el login y en el menú.");
        fila(formulario, g, 1, "Limpieza entre reservas", conSufijo(spnLimpieza, "minutos"),
                "Tiempo libre obligatorio entre dos reservas de la misma cancha (0 a 120).");
        fila(formulario, g, 2, "Anticipo mínimo", conSufijo(spnAnticipo, "% del total"),
                "Lo que debe estar pagado para CONFIRMAR una reserva (0 = no se exige).");
        JPanel pieParametros = new JPanel(new BorderLayout(Tema.ESPACIO, 0));
        pieParametros.setOpaque(false);
        pieParametros.add(lblMensajeParametros, BorderLayout.CENTER);
        pieParametros.add(btnAplicar, BorderLayout.EAST);
        JPanel formularioIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // pegado a la izquierda
        formularioIzq.setOpaque(false);
        formularioIzq.add(formulario);
        armarTarjeta(crdParametros, "Parámetros del complejo", formularioIzq, pieParametros);

        // ---- Tarjeta: datos y archivos
        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.setOpaque(false);
        lstAvisos.setVisibleRowCount(6);
        JScrollPane scrAvisos = new JScrollPane(lstAvisos);
        scrAvisos.setPreferredSize(new Dimension(380, 130));
        JPanel filaGuardar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaGuardar.setOpaque(false);
        filaGuardar.add(btnGuardar);
        for (JComponent c : new JComponent[]{lblCarpeta, lblUltimoGuardado, filaGuardar, lblResumenCarga,
            scrAvisos, lblRespaldos}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0),
                    c.getBorder()));
            datos.add(c);
        }
        JLabel nota = new JLabel("<html>Los cambios del día viven en memoria. Se escriben en los archivos .csv "
                + "solo al pulsar <b>Guardar</b> o al cerrar el programa (antes se hace un respaldo).</html>");
        Tema.textoSecundario(nota);
        armarTarjeta(crdArchivos, "Datos y archivos", datos, nota);

        // ---- Tarjeta: mi cuenta
        JPanel cuenta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cuenta.setOpaque(false);
        cuenta.add(btnContrasena);
        armarTarjeta(crdCuenta, "Mi cuenta", lblUsuario, cuenta);

        // ---- Las 3 tarjetas una debajo de otra, siempre del ancho de la ventana
        JPanel tarjetas = new PanelAjustadoAlAncho();
        tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.Y_AXIS));
        tarjetas.setOpaque(false);
        for (JPanel t : new JPanel[]{crdParametros, crdArchivos, crdCuenta}) {
            t.setAlignmentX(LEFT_ALIGNMENT);
            tarjetas.add(t);
            tarjetas.add(Box.createVerticalStrut(Tema.MARGEN));
        }

        JScrollPane scr = new JScrollPane(tarjetas);
        scr.setBorder(BorderFactory.createEmptyBorder());
        scr.setOpaque(false);
        scr.getViewport().setOpaque(false);
        scr.getVerticalScrollBar().setUnitIncrement(16);
        add(scr, BorderLayout.CENTER);
    }

    /**
     * Panel que dentro de un JScrollPane toma SIEMPRE el ancho visible: así nunca
     * aparece la barra horizontal; si no cabe a lo alto, sale la barra vertical.
     */
    private static final class PanelAjustadoAlAncho extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
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

    /** Tarjeta blanca con título, contenido y pie. */
    private void armarTarjeta(JPanel tarjeta, String titulo, JComponent contenido, JComponent pie) {
        JLabel lbl = new JLabel(titulo);
        titulosTarjetas.add(lbl);
        tarjeta.add(lbl, BorderLayout.NORTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        tarjeta.add(pie, BorderLayout.SOUTH);
    }

    private static void fila(JPanel p, GridBagConstraints g, int y, String etiqueta, JComponent campo, String ayuda) {
        JLabel e = new JLabel(etiqueta);
        e.setFont(Tema.FUENTE_NEGRITA);
        g.gridy = y * 2;
        g.gridx = 0;
        p.add(e, g);
        g.gridx = 1;
        p.add(campo, g);
        JLabel a = new JLabel(ayuda);
        Tema.textoSecundario(a);
        g.gridy = y * 2 + 1;
        g.insets = new Insets(0, 0, 8, 10);
        p.add(a, g);
        g.insets = new Insets(4, 0, 4, 10);
    }

    private static JPanel conSufijo(JSpinner spinner, String sufijo) {
        spinner.setPreferredSize(new Dimension(80, 30));
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        p.add(spinner);
        p.add(new JLabel(sufijo));
        return p;
    }

    // =====================================================================
    //  PanelModulo
    // =====================================================================

    @Override
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        for (JPanel t : new JPanel[]{crdParametros, crdArchivos, crdCuenta}) {
            Tema.tarjeta(t);
            t.setBorder(BorderFactory.createCompoundBorder(Tema.bordeTarjeta(),
                    BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN)));
        }
        for (JLabel l : titulosTarjetas) {
            l.setFont(Tema.FUENTE_SUBTITULO);
            l.setForeground(Tema.color(ColorTema.TEXTO));
        }
        Tema.campo(txtNombre);
        Tema.botonPrimario(btnAplicar);
        Tema.botonPrimario(btnGuardar);
        Tema.botonSecundario(btnContrasena);
        Tema.textoSecundario(lblCarpeta);
        Tema.textoSecundario(lblUltimoGuardado);
        lblResumenCarga.setFont(Tema.FUENTE_NEGRITA);
        Tema.textoSecundario(lblRespaldos);
        lblMensajeParametros.setFont(Tema.FUENTE_NEGRITA);
        lstAvisos.setFont(Tema.FUENTE_PEQUENA);
        repaint();
    }

    /** Llena todo con lo que hay en memoria (se llama cada vez que se abre el módulo). */
    @Override
    public void recargar() {
        ConfiguracionSistema c = configuracion.getConfiguracion();
        txtNombre.setText(c.getNombreComplejo());
        spnLimpieza.setValue(c.getMinutosLimpieza());
        spnAnticipo.setValue(c.getPorcentajeAnticipoMinimo());
        lblMensajeParametros.setText(" ");

        // El operador ve los parámetros pero no los cambia (el controlador también lo exige)
        boolean admin = configuracion.puedeAdministrar();
        txtNombre.setEditable(admin);
        spnLimpieza.setEnabled(admin);
        spnAnticipo.setEnabled(admin);
        btnAplicar.setVisible(admin);
        if (!admin) {
            mensaje("Solo el administrador puede cambiar estos parámetros.", ColorTema.TEXTO_SECUNDARIO);
        }

        lblCarpeta.setText("Carpeta de datos: " + configuracion.getCarpetaDatos());
        lblCarpeta.setToolTipText(configuracion.getCarpetaDatos());
        lblResumenCarga.setText("Al iniciar: " + configuracion.getResumenCarga());
        modeloAvisos.clear();
        for (String e : configuracion.getErroresCarga()) {
            modeloAvisos.addElement(" " + e);
        }
        for (String a : configuracion.getAdvertenciasCarga()) {
            modeloAvisos.addElement(" " + a);
        }
        if (modeloAvisos.isEmpty()) {
            modeloAvisos.addElement(" Todos los archivos se leyeron sin errores ni advertencias.");
        }
        try {
            List<String> respaldos = configuracion.getRespaldos();
            lblRespaldos.setText("Respaldos guardados: " + respaldos.size()
                    + (respaldos.isEmpty() ? "" : "  ·  el más reciente: " + respaldos.get(respaldos.size() - 1)));
        } catch (CanchaProException ex) {
            lblRespaldos.setText(ex.getMessage());
        }

        lblUsuario.setText("<html><b>" + sesion.getUsuario().getNombre() + "</b>  ("
                + sesion.getUsuario().getUsername() + ")<br>Rol: " + sesion.getRol().getDescripcion() + "</html>");
    }

    // =====================================================================
    //  ACCIONES (una llamada al controlador cada una)
    // =====================================================================

    /** Cambia la configuración en memoria; el controlador valida los rangos. */
    private void aplicarParametros() {
        try {
            configuracion.actualizar(txtNombre.getText(),
                    configuracion.getConfiguracion().isModuloTiendaActivo(), // la Tienda la decide el desarrollador
                    (Integer) spnLimpieza.getValue(),
                    (Double) spnAnticipo.getValue());
            mensaje(" Cambios aplicados. Se guardarán en el archivo al pulsar Guardar o al salir.", ColorTema.EXITO);
            alCambiarDatos.run(); // el nombre nuevo se ve en el menú y en el título
        } catch (CanchaProException ex) {
            mensaje(ex.getMessage(), ColorTema.ERROR);
        }
    }

    /** La ÚNICA escritura de archivos durante la ejecución: es una acción explícita del usuario. */
    private void guardarSistema() {
        try {
            String resultado = configuracion.guardarSistema();
            lblUltimoGuardado.setText("Último guardado en esta sesión: "
                    + FechaUtil.formatearHora(FechaUtil.ahora().toLocalTime()));
            recargar(); // hay un respaldo nuevo
            JOptionPane.showMessageDialog(this, resultado, "Guardar sistema", JOptionPane.INFORMATION_MESSAGE);
        } catch (CanchaProException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Guardar sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Cualquier usuario puede cambiar SU contraseña; se exige la actual para confirmar que es él. */
    private void cambiarMiContrasena() {
        JPasswordField pwdActual = new JPasswordField();
        JPasswordField pwdNueva = new JPasswordField();
        JPasswordField pwdConfirmacion = new JPasswordField();
        DialogoFormulario f = new DialogoFormulario(this, "Cambiar mi contraseña");
        f.fila("Contraseña actual *", pwdActual)
         .fila("Nueva *", pwdNueva)
         .fila("Confirmar nueva *", pwdConfirmacion)
         .ayuda("Mínimo " + UsuarioController.MIN_CONTRASENA + " caracteres, sin espacios.");
        if (f.mostrar(() -> usuarios.cambiarMiContrasena(pwdActual.getPassword(), pwdNueva.getPassword(),
                pwdConfirmacion.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Tu contraseña se cambió. Úsala la próxima vez que inicies sesión.",
                    "Mi cuenta", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void mensaje(String texto, ColorTema color) {
        lblMensajeParametros.setText(texto);
        lblMensajeParametros.setForeground(Tema.color(color));
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.DesarrolladorController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
/**
 * Panel OCULTO del desarrollador (se abre con Ctrl + Shift + D desde el Dashboard).
 *
 * Construido 100 % por código. Tiene dos "cartas" (CardLayout):
 * 
 *   ACCES: pide la clave del desarrollador.
 *   PANEL pestañas Módulos · Datos y respaldos · Colores · Clave.
 * 
 *
 * Regla MVC: cada botón solo lee lo que escribió el usuario, llama a UN método
 * del {@link DesarrolladorController} y muestra el resultado o el error. La
 * clave, los respaldos y los colores los maneja el controlador.
 *
 * Colores: el controlador guarda el texto "#RRGGBB" en la configuración y esta
 * vista los aplica en pantalla con {@code Tema.cargarColores(...)}.
 */
public class DesarrolladorDialog extends JDialog{
    private static final long serialVersionUID = 1L;
    private static final String CARTA_ACCESO = "ACCESO";
    private static final String CARTA_PANEL = "PANEL";

    private final transient DesarrolladorController dev;
    private final CardLayout cartas = new CardLayout();
    private final JPanel pnlCartas = new JPanel(cartas);

    // Carta ACCESO
    private final JPasswordField pwdClave = new JPasswordField(18);
    private final JLabel lblMensajeAcceso = new JLabel(" ");

    // Pestaña Módulos
    private final JCheckBox chkTienda = new JCheckBox("Módulo Tienda / Cafetería activo");

    // Pestaña Datos y respaldos
    private final DefaultListModel<String> modeloRespaldos = new DefaultListModel<>();
    private final JList<String> lstRespaldos = new JList<>(modeloRespaldos);

    // Pestaña Colores: un botón-muestra por color
    private final Map<ColorTema, JButton> muestras = new EnumMap<>(ColorTema.class);
    private final Map<ColorTema, JLabel> codigos = new EnumMap<>(ColorTema.class);

    // Pestaña Clave
    private final JPasswordField pwdActual = new JPasswordField(18);
    private final JPasswordField pwdNueva = new JPasswordField(18);
    private final JPasswordField pwdConfirmar = new JPasswordField(18);

    // Aviso de clave de fábrica (arriba del panel)
    private final JLabel lblAvisoFabrica = new JLabel();

    /**
     * @param dueno ventana que lo abre (el Dashboard); el diálogo es MODAL
     * @param dev   controlador del desarrollador (lo entrega el SistemaController)
     */
    public DesarrolladorDialog(Frame dueno, DesarrolladorController dev) {
        super(dueno, "Panel del desarrollador", true);
        this.dev = dev;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                dev.cerrarAcceso(); // la próxima vez vuelve a pedir la clave
            }
        });

        pnlCartas.add(crearCartaAcceso(), CARTA_ACCESO);
        pnlCartas.add(crearCartaPanel(), CARTA_PANEL);
        setContentPane(pnlCartas);

        setSize(new Dimension(640, 540));
        setLocationRelativeTo(dueno);
        cartas.show(pnlCartas, CARTA_ACCESO);
    }

    // =====================================================================
    //  CARTA 1: ACCESO
    // =====================================================================

    private JPanel crearCartaAcceso() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Tema.color(ColorTema.MENU_FONDO));

        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        Tema.tarjeta(caja);

        JLabel lblTitulo = new JLabel("Acceso de desarrollador");
        Tema.titulo(lblTitulo);
        JLabel lblAyuda = new JLabel("Ingrese la clave del desarrollador para continuar.");
        Tema.textoSecundario(lblAyuda);
        Tema.campo(pwdClave);
        pwdClave.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_BOTON));
        lblMensajeAcceso.setFont(Tema.FUENTE_NEGRITA);
        lblMensajeAcceso.setForeground(Tema.color(ColorTema.ERROR));

        JButton btnEntrar = new JButton("Entrar");
        Tema.botonPrimario(btnEntrar);
        btnEntrar.addActionListener(e -> entrar());
        pwdClave.addActionListener(e -> entrar()); // Enter en el campo = Entrar

        JButton btnCancelar = new JButton("Cancelar");
        Tema.botonSecundario(btnCancelar);
        btnCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnEntrar);

        for (JComponent c : new JComponent[]{lblTitulo, lblAyuda, pwdClave, lblMensajeAcceso, botones}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        caja.add(lblTitulo);
        caja.add(Box.createVerticalStrut(4));
        caja.add(lblAyuda);
        caja.add(Box.createVerticalStrut(Tema.MARGEN));
        caja.add(pwdClave);
        caja.add(Box.createVerticalStrut(Tema.ESPACIO));
        caja.add(lblMensajeAcceso);
        caja.add(Box.createVerticalStrut(Tema.ESPACIO));
        caja.add(botones);
        caja.setPreferredSize(new Dimension(380, 210));

        panel.add(caja);
        return panel;
    }

    private void entrar() {
        try {
            dev.autenticar(pwdClave.getPassword()); // el controlador borra el char[]
            pwdClave.setText("");
            cargarDatosDelPanel();
            cartas.show(pnlCartas, CARTA_PANEL);
        } catch (CanchaProException ex) {
            pwdClave.setText("");
            lblMensajeAcceso.setText(ex.getMessage());
            if (dev.isBloqueado()) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Acceso bloqueado",
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        }
    }

    // =====================================================================
    //  CARTA 2: PANEL CON PESTAÑAS
    // =====================================================================

    private JPanel crearCartaPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        panel.setBorder(Tema.margen());

        lblAvisoFabrica.setFont(Tema.FUENTE_NEGRITA);
        lblAvisoFabrica.setForeground(Tema.color(ColorTema.ADVERTENCIA));
        panel.add(lblAvisoFabrica, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Tema.FUENTE_NEGRITA);
        tabs.addTab("Módulos", crearPestanaModulos());
        tabs.addTab("Datos y respaldos", crearPestanaDatos());
        tabs.addTab("Colores", crearPestanaColores());
        tabs.addTab("Clave", crearPestanaClave());
        panel.add(tabs, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar panel");
        Tema.botonSecundario(btnCerrar);
        btnCerrar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.add(btnCerrar);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    /** Lee del controlador lo que se muestra en las pestañas (después de autenticar). */
    private void cargarDatosDelPanel() {
        chkTienda.setSelected(dev.isTiendaActiva());
        lblAvisoFabrica.setText(dev.usaClaveDeFabrica()
                ? " Está usando la clave de fábrica. Cámbiela en la pestaña Clave." : " ");
        cargarRespaldos();
        pintarMuestras();
    }

    // ----- Pestaña: Módulos ------------------------------------------------

    private JPanel crearPestanaModulos() {
        JPanel panel = pestanaVacia();
        chkTienda.setOpaque(false);
        chkTienda.setFont(Tema.FUENTE_NEGRITA);
        chkTienda.addActionListener(e -> cambiarTienda());
        panel.add(chkTienda);
        panel.add(Box.createVerticalStrut(4));
        panel.add(ayuda("Si se desactiva, el menú oculta Tienda y el mapa no abre el POS."));
        panel.add(ayuda("Las reservas siguen funcionando igual. Se guarda al cerrar el programa."));
        return panel;
    }

    private void cambiarTienda() {
        try {
            dev.setTiendaActiva(chkTienda.isSelected());
        } catch (CanchaProException ex) {
            chkTienda.setSelected(dev.isTiendaActiva()); // vuelve al valor real
            error(ex);
        }
    }

    // ----- Pestaña: Datos y respaldos ---------------------------------------

    private JPanel crearPestanaDatos() {
        JPanel panel = pestanaVacia();

        panel.add(subtitulo("Respaldos (data/backup)"));
        panel.add(ayuda("Se crea uno automáticamente cada vez que el sistema guarda. Del más reciente al más antiguo:"));
        lstRespaldos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstRespaldos.setVisibleRowCount(5);
        JScrollPane scrRespaldos = new JScrollPane(lstRespaldos);
        scrRespaldos.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrRespaldos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(scrRespaldos);
        panel.add(Box.createVerticalStrut(Tema.ESPACIO));

        JButton btnRestaurar = new JButton("Restaurar respaldo seleccionado");
        Tema.botonSecundario(btnRestaurar);
        btnRestaurar.addActionListener(e -> restaurarRespaldo());
        JButton btnDatosPrueba = new JButton("Restaurar datos de prueba");
        Tema.botonSecundario(btnDatosPrueba);
        btnDatosPrueba.addActionListener(e -> restaurarDatosPrueba());
        panel.add(fila(btnRestaurar, btnDatosPrueba));

        panel.add(Box.createVerticalStrut(Tema.MARGEN * 2));
        panel.add(subtitulo("Zona de peligro"));
        panel.add(ayuda("Borra clientes, canchas, tarifas, servicios, productos, reservas y ventas."));
        panel.add(ayuda("Conserva usuarios y configuración. Antes se crea un respaldo."));
        panel.add(Box.createVerticalStrut(Tema.ESPACIO));
        JButton btnEliminar = new JButton("Eliminar todos los datos");
        Tema.botonPeligro(btnEliminar);
        btnEliminar.addActionListener(e -> eliminarTodo());
        panel.add(fila(btnEliminar));
        return panel;
    }

    private void cargarRespaldos() {
        modeloRespaldos.clear();
        try {
            List<String> nombres = dev.listarRespaldos();
            for (String nombre : nombres) {
                modeloRespaldos.addElement(nombre);
            }
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void restaurarRespaldo() {
        String nombre = lstRespaldos.getSelectedValue();
        if (nombre == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un respaldo de la lista.", "Respaldos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (confirmar("¿Restaurar el respaldo " + nombre + "?\n"
                + "Los cambios hechos desde que abrió el programa NO se guardarán.")) {
            try {
                informar(dev.restaurarRespaldo(nombre));
                cargarRespaldos();
            } catch (CanchaProException ex) {
                error(ex);
            }
        }
    }

    private void restaurarDatosPrueba() {
        if (confirmar("¿Reemplazar los datos actuales por los datos de prueba?\n"
                + "Se hará un respaldo antes. Los cambios de esta sesión NO se guardarán.")) {
            try {
                informar(dev.restaurarDatosPrueba());
                cargarRespaldos();
            } catch (CanchaProException ex) {
                error(ex);
            }
        }
    }

    private void eliminarTodo() {
        String texto = JOptionPane.showInputDialog(this,
                "Esta acción borra todos los datos (menos usuarios y configuración).\n"
                + "Para confirmar escriba " + DesarrolladorController.PALABRA_CONFIRMACION + ":",
                "Eliminar todos los datos", JOptionPane.WARNING_MESSAGE);
        if (texto == null) {
            return; // canceló
        }
        try {
            informar(dev.eliminarTodosLosDatos(texto));
            cargarRespaldos();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    // ----- Pestaña: Colores -------------------------------------------------

    /** Una fila por cada constante de ColorTema, agrupadas: se genera recorriendo el enum. */
    private JComponent crearPestanaColores() {
        JPanel lista = new JPanel(new GridBagLayout());
        lista.setBorder(Tema.margen());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        int fila = 0;
        String grupoActual = null;
        for (ColorTema color : ColorTema.values()) {
            if (!color.getGrupo().equals(grupoActual)) {
                grupoActual = color.getGrupo();
                g.gridx = 0;
                g.gridy = fila++;
                g.gridwidth = 3;
                g.insets = new Insets(fila == 1 ? 0 : 12, 4, 4, 4);
                lista.add(subtitulo(grupoActual), g);
                g.gridwidth = 1;
                g.insets = new Insets(3, 4, 3, 4);
            }
            JButton muestra = new JButton();
            muestra.setPreferredSize(new Dimension(44, 22));
            muestra.setFocusPainted(false);
            muestra.setContentAreaFilled(false);
            muestra.setOpaque(true);
            muestra.setToolTipText("Cambiar color");
            muestra.addActionListener(e -> elegirColor(color));
            muestras.put(color, muestra);

            JLabel codigo = new JLabel();
            codigo.setFont(Tema.FUENTE_PEQUENA);
            codigos.put(color, codigo);

            g.gridy = fila++;
            g.gridx = 0;
            lista.add(muestra, g);
            g.gridx = 1;
            g.weightx = 1;
            lista.add(new JLabel(color.getDescripcion()), g);
            g.weightx = 0;
            g.gridx = 2;
            lista.add(codigo, g);
        }

        JButton btnRestablecer = new JButton("Restablecer colores de fábrica");
        Tema.botonSecundario(btnRestablecer);
        btnRestablecer.addActionListener(e -> restablecerColores());
        g.gridx = 0;
        g.gridy = fila;
        g.gridwidth = 3;
        g.insets = new Insets(Tema.MARGEN, 4, 4, 4);
        lista.add(btnRestablecer, g);

        JScrollPane scr = new JScrollPane(lista);
        scr.setBorder(BorderFactory.createEmptyBorder());
        scr.getVerticalScrollBar().setUnitIncrement(16);
        return scr;
    }

    private void elegirColor(ColorTema color) {
        Color nuevo = JColorChooser.showDialog(this, color.getDescripcion(), Tema.color(color));
        if (nuevo == null) {
            return; // canceló
        }
        try {
            dev.cambiarColor(color, Tema.aHex(nuevo));   // el controlador guarda el texto
            Tema.cargarColores(dev.getColores());          // la vista lo aplica en pantalla
            pintarMuestras();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void restablecerColores() {
        if (!confirmar("¿Volver a los colores de fábrica?")) {
            return;
        }
        try {
            dev.restablecerColores();
            Tema.cargarColores(dev.getColores());
            pintarMuestras();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** Pinta cada muestra con el color actual del Tema. */
    private void pintarMuestras() {
        for (Map.Entry<ColorTema, JButton> e : muestras.entrySet()) {
            Color actual = Tema.color(e.getKey());
            e.getValue().setBackground(actual);
            e.getValue().setBorder(BorderFactory.createLineBorder(Tema.oscurecer(actual, 0.3)));
            codigos.get(e.getKey()).setText(Tema.aHex(actual));
        }
    }

    // ----- Pestaña: Clave ---------------------------------------------------

    private JPanel crearPestanaClave() {
        JPanel panel = pestanaVacia();
        panel.add(subtitulo("Cambiar la clave del desarrollador"));
        panel.add(ayuda("Mínimo " + DesarrolladorController.MIN_CLAVE + " caracteres. Se guarda al cerrar el programa."));
        panel.add(Box.createVerticalStrut(Tema.ESPACIO));
        panel.add(campoConEtiqueta("Clave actual", pwdActual));
        panel.add(campoConEtiqueta("Clave nueva", pwdNueva));
        panel.add(campoConEtiqueta("Confirmar clave nueva", pwdConfirmar));
        panel.add(Box.createVerticalStrut(Tema.ESPACIO));
        JButton btnCambiar = new JButton("Cambiar clave");
        Tema.botonPrimario(btnCambiar);
        btnCambiar.addActionListener(e -> cambiarClave());
        panel.add(fila(btnCambiar));
        return panel;
    }

    private void cambiarClave() {
        try {
            dev.cambiarClave(pwdActual.getPassword(), pwdNueva.getPassword(), pwdConfirmar.getPassword());
            informar("Clave del desarrollador cambiada.");
            lblAvisoFabrica.setText(" ");
        } catch (CanchaProException ex) {
            error(ex);
        } finally {
            pwdActual.setText("");
            pwdNueva.setText("");
            pwdConfirmar.setText("");
        }
    }

    // =====================================================================
    //  APOYO VISUAL
    // =====================================================================

    private JPanel pestanaVacia() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(Tema.margen());
        return panel;
    }

    private JLabel subtitulo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(Tema.FUENTE_SUBTITULO);
        lbl.setForeground(Tema.color(ColorTema.TEXTO));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel ayuda(String texto) {
        JLabel lbl = new JLabel(texto);
        Tema.textoSecundario(lbl);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel fila(JComponent... componentes) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < componentes.length; i++) {
            if (i > 0) {
                fila.add(Box.createHorizontalStrut(Tema.ESPACIO));
            }
            fila.add(componentes[i]);
        }
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
        return fila;
    }

    private JPanel campoConEtiqueta(String etiqueta, JPasswordField campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, Tema.ESPACIO, 0));
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(Tema.FUENTE_NEGRITA);
        Tema.campo(campo);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(320, panel.getPreferredSize().height));
        return panel;
    }

    private boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Panel del desarrollador", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Panel del desarrollador", JOptionPane.ERROR_MESSAGE);
    }
}

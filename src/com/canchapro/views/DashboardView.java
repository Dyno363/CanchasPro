/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.canchapro.views;
import com.canchapro.views.panels.ReservasPanel;
import com.canchapro.controllers.DesarrolladorController;
import com.canchapro.controllers.IndicadoresController;
import com.canchapro.controllers.MapaController;
import com.canchapro.controllers.Sesion;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.MenuLateral;
import com.canchapro.views.components.ModuloMenu;
import com.canchapro.views.components.TarjetaIndicador;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import com.canchapro.views.dialogs.DesarrolladorDialog;
import com.canchapro.views.panels.MapaCanchasPanel;
/**
 * Ventana principal de CanchaPro (VISTA).
 *
 * Izquierda: {@link MenuLateral}. Derecha: {@code pnlContenido} con CardLayout:
 * la carta "INICIO" tiene las 2 pestañas (Reservas | Mapa de canchas) y hay una
 * carta por cada módulo del menú.
 *
 * @author SANTANA
 */
public class DashboardView extends javax.swing.JFrame {
/** Nombre de la "carta" del CardLayout que contiene las 2 pestañas. */
    private static final String CARTA_INICIO = "INICIO";

    private final SistemaController sistema;
    private final Sesion sesion;
    private final IndicadoresController indicadores;
    private MenuLateral menu;
    private final Map<ModuloMenu, JPanel> modulos = new EnumMap<>(ModuloMenu.class);
    private Timer refresco;
    private ReservasPanel reservasPanel;
        private MapaCanchasPanel mapaPanel;
    // Tarjetas de la pestaña Reservas
    private TarjetaIndicador tarReservasHoy;
    private TarjetaIndicador tarEnCurso;
    private TarjetaIndicador tarCobrado;
    private TarjetaIndicador tarSaldo;
    // Tarjetas de la pestaña Mapa
    private TarjetaIndicador tarDisponibles;
    private TarjetaIndicador tarCanchasEnCurso;
    private TarjetaIndicador tarMantenimiento;

    /**
     * Ventana principal (VISTA). Se abre después del login con la sesión ya iniciada.
     * Solo muestra datos que le dan los controladores y avisa las acciones del usuario.
     */
    public DashboardView(SistemaController sistema) {
        this.sistema = sistema;
        this.sesion = sistema.getSesion();
        this.indicadores = sistema.getIndicadoresController();
        initComponents();
        construirMenu();
        construirPestanas();
        construirModulos();
        registrarAtajoDesarrollador();
        aplicarTema();
        actualizarVisibilidadModulos();
        mostrarInicio();
        iniciarRefresco();
        setTitle("CanchaPro - " + sistema.getNombreComplejo());
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH); // abre ocupando toda la pantalla
    }

    // =====================================================================
    //  CONSTRUCCIÓN (por código: menú, tarjetas y módulos)
    // =====================================================================

    private void construirMenu() {
        menu = new MenuLateral(sistema.getNombreComplejo(), sesion.getUsuario().getNombre(),
                sesion.getRol().getDescripcion());
        menu.alSeleccionar(this::alSeleccionarModulo); // el menú solo avisa; aquí se decide
        getContentPane().add(menu, BorderLayout.WEST);
    }

    /** Pone las tarjetas arriba de cada pestaña; debajo, la lista de reservas y el mapa. */
    private void construirPestanas() {
        // Por si en el diseñador quedaron en "Free Design": las pestañas necesitan BorderLayout
        pnlTabReservas.setLayout(new BorderLayout());
        pnlTabMapa.setLayout(new BorderLayout());

        // ---- Pestaña 1: Reservas
        tarReservasHoy = new TarjetaIndicador("Reservas hoy", ColorTema.INFO);
        tarEnCurso = new TarjetaIndicador("En curso", ColorTema.CANCHA_EN_CURSO);
        tarCobrado = new TarjetaIndicador("Cobrado hoy", ColorTema.EXITO);
        tarSaldo = new TarjetaIndicador("Saldo pendiente", ColorTema.ERROR);
        pnlTabReservas.add(filaDeTarjetas(tarReservasHoy, tarEnCurso, tarCobrado, tarSaldo), BorderLayout.NORTH);
        reservasPanel = new ReservasPanel(sistema, this::alCambiarReservas);
        pnlTabReservas.add(reservasPanel, BorderLayout.CENTER);

        // ---- Pestaña 2: Mapa de canchas
        tarDisponibles = new TarjetaIndicador("Disponibles", ColorTema.CANCHA_DISPONIBLE);
        tarCanchasEnCurso = new TarjetaIndicador("En curso", ColorTema.CANCHA_EN_CURSO);
        tarMantenimiento = new TarjetaIndicador("Mantenimiento", ColorTema.CANCHA_MANTENIMIENTO);
        pnlTabMapa.add(filaDeTarjetas(tarDisponibles, tarCanchasEnCurso, tarMantenimiento), BorderLayout.NORTH);
        mapaPanel = new MapaCanchasPanel(sistema, this::alCambiarDesdeMapa);
        pnlTabMapa.add(mapaPanel, BorderLayout.CENTER);
    }

    /** Una reserva cambió en la pestaña 1: el mapa y los números se ponen al día. */
    private void alCambiarReservas() {
        actualizarIndicadores();
        mapaPanel.recargar();
    }

    /** Algo cambió desde el mapa (refresco o POS): la lista y los números se ponen al día. */
    private void alCambiarDesdeMapa() {
        actualizarIndicadores();
        reservasPanel.recargar();
    }
        /** Crea una "carta" provisional por cada módulo del menú (se reemplazan en la Fase 7). */
    private void construirModulos() {
        for (ModuloMenu modulo : ModuloMenu.values()) {
            if (modulo.getGrupo() != null && modulo != ModuloMenu.GUARDAR) {
                JPanel panel = provisional(modulo.getTexto(), modulo.getAyuda() + ". Se implementa después.");
                modulos.put(modulo, panel);
                pnlContenido.add(panel, modulo.name());
            }
        }
    }
    private JPanel filaDeTarjetas(TarjetaIndicador... tarjetas) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.MARGEN, Tema.MARGEN));
        fila.setOpaque(false);
        for (TarjetaIndicador t : tarjetas) {
            fila.add(t);
        }
        return fila;
    }

    /** Panel con título y texto centrado, mientras llega el panel definitivo. */
    private JPanel provisional(String titulo, String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        Tema.titulo(lblTitulo);
        JLabel lblTexto = new JLabel(texto, SwingConstants.CENTER);
        Tema.textoSecundario(lblTexto);
        lblTitulo.setAlignmentX(CENTER_ALIGNMENT);
        lblTexto.setAlignmentX(CENTER_ALIGNMENT);
        centro.add(lblTitulo);
        centro.add(javax.swing.Box.createVerticalStrut(Tema.ESPACIO));
        centro.add(lblTexto);
        panel.add(centro, BorderLayout.CENTER);
        panel.setBorder(Tema.margen());
        return panel;
    }

    // =====================================================================
    //  NAVEGACIÓN (lo que pasa al pulsar el menú)
    // =====================================================================

    private void alSeleccionarModulo(ModuloMenu modulo) {
        switch (modulo) {
            case INICIO:
                mostrarInicio();
                break;
            case GUARDAR:
                guardarSistema();
                break;
            case CERRAR_SESION:
                cerrarSesion();
                break;
            default:
                mostrarModulo(modulo);
                break;
        }
    }

    private void mostrarInicio() {
        ((CardLayout) pnlContenido.getLayout()).show(pnlContenido, CARTA_INICIO);
        menu.setSeleccionado(ModuloMenu.INICIO);
        actualizarIndicadores();
    }

    private void mostrarModulo(ModuloMenu modulo) {
        ((CardLayout) pnlContenido.getLayout()).show(pnlContenido, modulo.name());
        menu.setSeleccionado(modulo);
    }

    /** Oculta Tienda si el desarrollador la desactivó y Usuarios si entra un Operador. */
    private void actualizarVisibilidadModulos() {
        menu.setModuloVisible(ModuloMenu.TIENDA, sistema.getTiendaController().isModuloActivo());
        menu.setModuloVisible(ModuloMenu.USUARIOS, sistema.getUsuarioController().puedeGestionarUsuarios());
        ModuloMenu abierto = menu.getSeleccionado();
        if (abierto != null && !menu.isModuloVisible(abierto)) {
            mostrarInicio(); // el módulo que estaba abierto ya no está permitido
        }
    }

    // =====================================================================
    //  INDICADORES (los números los calcula el controlador)
    // =====================================================================

    private void actualizarIndicadores() {
        IndicadoresController.ResumenReservas r = indicadores.getResumenReservas(LocalDate.now());
        tarReservasHoy.setValor(String.valueOf(r.getReservasDelDia()));
        tarReservasHoy.setDetalle(r.getPendientes() + " pendientes de confirmar");
        tarEnCurso.setValor(String.valueOf(r.getEnCurso()));
        tarEnCurso.setDetalle("partidos jugándose ahora");
        tarCobrado.setValor(MonedaUtil.formatear(r.getCobrado()));
        tarCobrado.setDetalle("pagos recibidos hoy");
        tarSaldo.setValor(MonedaUtil.formatear(r.getSaldoPendiente()));
        tarSaldo.setDetalle(r.getReservasConSaldo() + " reservas con saldo");
        tarSaldo.setAcento(r.haySaldoPendiente() ? ColorTema.ERROR : ColorTema.EXITO);

        IndicadoresController.ResumenCanchas c = indicadores.getResumenCanchas();
        tarDisponibles.setValor(String.valueOf(c.getDisponibles()));
        tarDisponibles.setDetalle("de " + c.getTotal() + " canchas");
        tarCanchasEnCurso.setValor(String.valueOf(c.getEnCurso()));
        tarCanchasEnCurso.setDetalle(c.isTiendaActiva() ? "clic para abrir el POS" : "con partido ahora");
        tarMantenimiento.setValor(String.valueOf(c.getMantenimiento()));
        tarMantenimiento.setDetalle(c.getInactivas() + " inactivas");
    }

     /** Cada minuto: pone las reservas al día según la hora y repinta los números y el mapa. */
    private void iniciarRefresco() {
        refresco = new Timer(MapaController.INTERVALO_REFRESCO_MS, e -> {
            sistema.getMapaController().refrescar();
            actualizarIndicadores();
            reservasPanel.recargar(); // los estados pudieron cambiar con la hora
            mapaPanel.recargar();     // colores y "quedan X min"
        });
        refresco.start();
    }
    // =====================================================================
    //  ACCIONES DEL MENÚ
    // =====================================================================

    /** "Guardar Sistema": la única escritura de archivos fuera del cierre (acción explícita). */
    private void guardarSistema() {
        try {
            String mensaje = sistema.getConfiguracionController().guardarSistema();
            JOptionPane.showMessageDialog(this, mensaje, "Guardar Sistema", JOptionPane.INFORMATION_MESSAGE);
        } catch (CanchaProException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Guardar Sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Vuelve al login SIN guardar ni perder datos: todo sigue en memoria. */
    private void cerrarSesion() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión de "
                + sesion.getUsuario().getNombre() + "?", "Cerrar sesión", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        refresco.stop();
        sistema.cerrarSesion();
        new LoginView(sistema).setVisible(true);
        dispose();
    }

    /** Cerrar la ventana = cerrar el programa: aquí se guardan los CSV (regla de oro). */
    private void salir() {
        int r = JOptionPane.showConfirmDialog(this, "¿Desea salir de CanchaPro?\nLos datos se guardarán al salir.",
                "Salir", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
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
        refresco.stop();
        dispose();
        System.exit(0);
    }

    // =====================================================================
    //  PANEL OCULTO DEL DESARROLLADOR (Ctrl + Shift + D)
    // =====================================================================

    /**
     * Registra el atajo en el InputMap de la ventana: funciona sin importar qué
     * componente tenga el foco, y no hay ningún botón visible que lo abra.
     */
    private void registrarAtajoDesarrollador() {
        KeyStroke atajo = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        JComponent raiz = getRootPane();
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(atajo, "panelDesarrollador");
        raiz.getActionMap().put("panelDesarrollador", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirPanelDesarrollador();
            }
        });
    }

    
        /**
     * Abre el panel del desarrollador. Es MODAL: esta línea espera hasta que se
     * cierre, y después se aplican los cambios (colores, módulo Tienda, datos).
     */
    private void abrirPanelDesarrollador() {
        DesarrolladorController dev = sistema.getDesarrolladorController();
        if (dev.isBloqueado()) {
            JOptionPane.showMessageDialog(this, "Acceso de desarrollador bloqueado. Reinicie el programa.",
                    "Panel del desarrollador", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new DesarrolladorDialog(this, dev).setVisible(true);
        aplicarCambiosDelDesarrollador();
    }

    /** Después de cerrar el panel del desarrollador: aplica colores y módulos nuevos. */
    private void aplicarCambiosDelDesarrollador() {
        DesarrolladorController dev = sistema.getDesarrolladorController();
        Tema.cargarColores(dev.getColores());
        Tema.aplicar();
        SwingUtilities.updateComponentTreeUI(this);
        aplicarTema();
        actualizarVisibilidadModulos();
        actualizarIndicadores();
    }

    /** Colores del Tema en los elementos de esta ventana (se repite si cambian). */
    private void aplicarTema() {
        getContentPane().setBackground(Tema.color(ColorTema.FONDO));
        pnlContenido.setBackground(Tema.color(ColorTema.FONDO));
        pnlContenido.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, Tema.ESPACIO, Tema.ESPACIO, Tema.ESPACIO));
        tabPrincipal.setFont(Tema.FUENTE_NEGRITA);
        pnlTabReservas.setBackground(Tema.color(ColorTema.FONDO));
        pnlTabMapa.setBackground(Tema.color(ColorTema.FONDO));
        menu.aplicarTema();
         reservasPanel.aplicarTema();
        for (TarjetaIndicador t : new TarjetaIndicador[]{tarReservasHoy, tarEnCurso, tarCobrado, tarSaldo,
            tarDisponibles, tarCanchasEnCurso, tarMantenimiento}) {
            t.aplicarTema();
        }
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlContenido = new javax.swing.JPanel();
        tabPrincipal = new javax.swing.JTabbedPane();
        pnlTabReservas = new javax.swing.JPanel();
        pnlTabMapa = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1100, 700));
        setSize(new java.awt.Dimension(1100, 700));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnlContenido.setLayout(new java.awt.CardLayout());

        javax.swing.GroupLayout pnlTabReservasLayout = new javax.swing.GroupLayout(pnlTabReservas);
        pnlTabReservas.setLayout(pnlTabReservasLayout);
        pnlTabReservasLayout.setHorizontalGroup(
            pnlTabReservasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 700, Short.MAX_VALUE)
        );
        pnlTabReservasLayout.setVerticalGroup(
            pnlTabReservasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 365, Short.MAX_VALUE)
        );

        tabPrincipal.addTab("Reservas", pnlTabReservas);

        javax.swing.GroupLayout pnlTabMapaLayout = new javax.swing.GroupLayout(pnlTabMapa);
        pnlTabMapa.setLayout(pnlTabMapaLayout);
        pnlTabMapaLayout.setHorizontalGroup(
            pnlTabMapaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 700, Short.MAX_VALUE)
        );
        pnlTabMapaLayout.setVerticalGroup(
            pnlTabMapaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 365, Short.MAX_VALUE)
        );

        tabPrincipal.addTab("Mapa de canchas", pnlTabMapa);

        pnlContenido.add(tabPrincipal, "INICIO");

        getContentPane().add(pnlContenido, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        salir();
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnlContenido;
    private javax.swing.JPanel pnlTabMapa;
    private javax.swing.JPanel pnlTabReservas;
    private javax.swing.JTabbedPane tabPrincipal;
    // End of variables declaration//GEN-END:variables
}

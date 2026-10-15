/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
/**
 *
 * @author SANTANA
 */
/**
 * Panel lateral izquierdo del Dashboard, construido 100 % por código.
 *
 * 
 *  ┌──────────────────────┐
 *  │ Complejo Deportivo…  │  nombre del negocio
 *  │ Bienvenido,          │
 *  │ Ana López            │  usuario de la sesión
 *  │ Administrador        │  rol
 *  │ [      Inicio      ] │  vuelve a Panel 1 / Panel 2
 *  │ ┌ PERSONAS ────────┐ │
 *  │ │ [Clientes]       │ │  una tarjeta por grupo de ModuloMenu
 *  │ │ [Usuarios]       │ │
 *  │ └──────────────────┘ │
 *  │ ┌ NEGOCIO ─────────┐ │
 *  │ │ [Canchas] ...    │ │
 *  │ └──────────────────┘ │
 *  │ ┌ SISTEMA ─────────┐ │
 *  │ └──────────────────┘ │
 *  │ [  Cerrar sesión   ] │
 *  └──────────────────────┘
 *
 *
 * No contiene lógica: cuando se pulsa un botón solo avisa
 * "se eligió el módulo X" mediante {@link #alSeleccionar(Consumer)}. Qué hacer
 * con eso lo decide el DashboardView (que a su vez llama al controlador).
 */
public class MenuLateral extends JPanel{
     private static final long serialVersionUID = 1L;
    private static final int RADIO = 14;

    private final JLabel lblNegocio = new JLabel();
    private final JLabel lblBienvenida = new JLabel("Bienvenido,");
    private final JLabel lblUsuario = new JLabel();
    private final JLabel lblRol = new JLabel();
    private final Map<ModuloMenu, JButton> botones = new EnumMap<>(ModuloMenu.class);
    private final Map<String, TarjetaGrupo> tarjetas = new LinkedHashMap<>();

    private transient Consumer<ModuloMenu> alSeleccionar = modulo -> { };
    private ModuloMenu seleccionado;

    /**
     * @param nombreNegocio nombre del complejo (config.properties)
     * @param nombreUsuario nombre del usuario de la sesión
     * @param rol           texto del rol (Administrador / Operador)
     */
    public MenuLateral(String nombreNegocio, String nombreUsuario, String rol) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Tema.margen());
        setPreferredSize(new Dimension(Tema.ANCHO_MENU, 600));

        construirEncabezado(nombreNegocio, nombreUsuario, rol);
        add(crearBoton(ModuloMenu.INICIO, true));
        add(Box.createVerticalStrut(Tema.ESPACIO + 2));
        construirGrupos();
        add(Box.createVerticalGlue());
        add(crearBoton(ModuloMenu.CERRAR_SESION, true));

        aplicarTema();
    }

    // =====================================================================
    //  API PÚBLICA (la usa el DashboardView)
    // =====================================================================

    /** Registra qué hacer cuando se pulsa un módulo (el Dashboard pasa una lambda). */
    public void alSeleccionar(Consumer<ModuloMenu> accion) {
        this.alSeleccionar = Objects.requireNonNull(accion);
    }

    /** Resalta el módulo abierto (o ninguno con null). */
    public void setSeleccionado(ModuloMenu modulo) {
        this.seleccionado = modulo;
        pintarBotones();
    }

    public ModuloMenu getSeleccionado() {
        return seleccionado;
    }

    /**
     * Muestra u oculta un módulo (ej. TIENDA si el módulo está desactivado,
     * USUARIOS si el usuario es Operador). Si un grupo queda vacío, se oculta la tarjeta.
     */
    public void setModuloVisible(ModuloMenu modulo, boolean visible) {
        JButton boton = botones.get(modulo);
        if (boton == null) {
            return;
        }
        boton.setVisible(visible);
        if (modulo.getGrupo() != null) {
            tarjetas.get(modulo.getGrupo()).actualizarVisibilidad();
        }
        revalidate();
        repaint();
    }

    public boolean isModuloVisible(ModuloMenu modulo) {
        JButton boton = botones.get(modulo);
        return boton != null && boton.isVisible();
    }

    /** Cambia el nombre del negocio (si el administrador lo edita en Configuración). */
    public void setNombreNegocio(String nombre) {
        lblNegocio.setText(nombre);
        lblNegocio.setToolTipText(nombre);
    }

    /** Vuelve a leer los colores del Tema (después de que el desarrollador los cambie). */
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.MENU_FONDO));
        lblNegocio.setForeground(Tema.color(ColorTema.TEXTO_INVERSO));
        lblBienvenida.setForeground(Tema.color(ColorTema.MENU_TEXTO));
        lblUsuario.setForeground(Tema.color(ColorTema.TEXTO_INVERSO));
        lblRol.setForeground(Tema.color(ColorTema.MENU_TEXTO));
        for (TarjetaGrupo tarjeta : tarjetas.values()) {
            tarjeta.aplicarTema();
        }
        pintarBotones();
        repaint();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construirEncabezado(String negocio, String usuario, String rol) {
        setNombreNegocio(negocio);
        lblNegocio.setFont(Tema.FUENTE_SUBTITULO);
        lblBienvenida.setFont(Tema.FUENTE_NORMAL);
        lblUsuario.setText(usuario);
        lblUsuario.setToolTipText(usuario); // si el nombre es largo y se corta, se ve completo aquí
        lblUsuario.setFont(Tema.FUENTE_NEGRITA);
        lblRol.setText(rol);
        lblRol.setFont(Tema.FUENTE_PEQUENA);

        lblNegocio.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(lblNegocio);
        add(Box.createVerticalStrut(Tema.ESPACIO));
        for (JLabel etiqueta : new JLabel[]{lblBienvenida, lblUsuario, lblRol}) {
            etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT); // en BoxLayout todos deben alinearse igual
            add(etiqueta);
            add(Box.createVerticalStrut(2));
        }
        add(Box.createVerticalStrut(Tema.MARGEN));
    }

    /** Agrupa los módulos por su grupo (en el orden del enum) y crea una tarjeta por grupo. */
    private void construirGrupos() {
        Map<String, List<ModuloMenu>> porGrupo = new LinkedHashMap<>();
        for (ModuloMenu modulo : ModuloMenu.values()) {
            if (modulo.getGrupo() != null) {
                porGrupo.computeIfAbsent(modulo.getGrupo(), g -> new ArrayList<>()).add(modulo);
            }
        }
        for (Map.Entry<String, List<ModuloMenu>> grupo : porGrupo.entrySet()) {
            TarjetaGrupo tarjeta = new TarjetaGrupo(grupo.getKey());
            for (ModuloMenu modulo : grupo.getValue()) {
                tarjeta.agregar(crearBoton(modulo, false));
            }
            tarjetas.put(grupo.getKey(), tarjeta);
            add(tarjeta);
            add(Box.createVerticalStrut(Tema.ESPACIO + 2));
        }
    }

    private JButton crearBoton(ModuloMenu modulo, boolean anchoCompleto) {
        JButton boton = new JButton(modulo.getTexto());
        Tema.botonMenu(boton);
        boton.setToolTipText(modulo.getAyuda());
        boton.setHorizontalAlignment(JButton.LEFT);
        if (anchoCompleto) {
            boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_BOTON));
            boton.setHorizontalAlignment(JButton.CENTER);
        }
        // La vista solo AVISA qué módulo se eligió; no decide nada.
        boton.addActionListener(e -> alSeleccionar.accept(modulo));
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (modulo != seleccionado) {
                    boton.setBackground(Tema.color(ColorTema.PRIMARIO_OSCURO));
                    boton.setForeground(Tema.color(ColorTema.TEXTO_INVERSO));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pintarBoton(modulo, boton);
            }
        });
        botones.put(modulo, boton);
        return boton;
    }

    // =====================================================================
    //  COLORES DE LOS BOTONES
    // =====================================================================

    private void pintarBotones() {
        for (Map.Entry<ModuloMenu, JButton> e : botones.entrySet()) {
            pintarBoton(e.getKey(), e.getValue());
        }
    }

    private void pintarBoton(ModuloMenu modulo, JButton boton) {
        Color fondo;
        if (modulo == seleccionado) {
            fondo = Tema.color(ColorTema.PRIMARIO);
        } else if (modulo == ModuloMenu.CERRAR_SESION) {
            fondo = Tema.color(ColorTema.MENU_TARJETA);
        } else {
            fondo = Tema.color(ColorTema.MENU_FONDO);
        }
        boton.setBackground(fondo);
        boton.setForeground(modulo == seleccionado ? Tema.textoSobre(fondo) : Tema.color(ColorTema.MENU_TEXTO));
    }

    // =====================================================================
    //  TARJETA DE GRUPO (clase interna: solo la usa el menú)
    // =====================================================================

    /** Tarjeta con esquinas redondeadas: título del grupo + botones en una columna. */
    private static final class TarjetaGrupo extends JPanel {

        private static final long serialVersionUID = 1L;
        private final JLabel lblTitulo;
        private final JPanel pnlBotones = new JPanel(new GridLayout(0, 1, 0, 4));
        private final List<JButton> todos = new ArrayList<>();

        TarjetaGrupo(String titulo) {
            super(new BorderLayout(0, Tema.ESPACIO));
            setOpaque(false); // el fondo redondeado lo pinta paintComponent
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            lblTitulo = new JLabel(titulo.toUpperCase(java.util.Locale.ROOT));
            lblTitulo.setFont(Tema.FUENTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
            pnlBotones.setOpaque(false);
            add(lblTitulo, BorderLayout.NORTH);
            add(pnlBotones, BorderLayout.CENTER);
        }

        void agregar(JButton boton) {
            todos.add(boton);
            pnlBotones.add(boton);
        }

        /**
         * Vuelve a colocar solo los botones visibles (GridLayout reserva espacio
         * incluso para los ocultos) y oculta la tarjeta si no queda ninguno.
         */
        void actualizarVisibilidad() {
            pnlBotones.removeAll();
            for (JButton boton : todos) {
                if (boton.isVisible()) {
                    pnlBotones.add(boton);
                }
            }
            setVisible(pnlBotones.getComponentCount() > 0);
        }

        void aplicarTema() {
            lblTitulo.setForeground(Tema.color(ColorTema.MENU_TEXTO));
        }

        /** Que la tarjeta no crezca a lo alto dentro del BoxLayout. */
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.color(ColorTema.MENU_TARJETA));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIO, RADIO);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
}

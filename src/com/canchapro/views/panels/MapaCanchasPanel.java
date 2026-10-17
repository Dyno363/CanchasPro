/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.EstadoVisualCancha;
import com.canchapro.controllers.MapaController;
import com.canchapro.controllers.MapaController.InfoCancha;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.TarjetaCancha;
import com.canchapro.views.dialogs.DetalleReservaDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import com.canchapro.views.dialogs.PosDialog;
/**
 *
 * @author SANTANA
 */
public class MapaCanchasPanel extends JPanel{
    private static final long serialVersionUID = 1L;

    private final transient SistemaController sistema;
    private final transient MapaController mapa;
    /** Avisa al Dashboard que cambió algo (indicadores y lista de reservas). */
    private final transient Runnable alCambiarDatos;

    private final List<TarjetaCancha> tarjetas = new ArrayList<>();
    private final JPanel pnlCuadricula = new JPanel(new GridLayout(0, 1, Tema.MARGEN, Tema.MARGEN));
    private final JScrollPane scrMapa = new JScrollPane();
    private final JLabel lblTitulo = new JLabel("Mapa del complejo");
    private final JLabel lblActualizado = new JLabel(" ");
    private final JLabel lblVacio = new JLabel("No hay canchas registradas. Agrégalas en el módulo Canchas.",
            SwingConstants.CENTER);
    private final JPanel pnlLeyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.MARGEN, 0));
    private final JButton btnActualizar = new JButton("Actualizar");

    public MapaCanchasPanel(SistemaController sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.mapa = sistema.getMapaController();
        this.alCambiarDatos = alCambiarDatos == null ? () -> { } : alCambiarDatos;
        construir();
        aplicarTema();
        recargar();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setLayout(new BorderLayout(0, Tema.ESPACIO));
        setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        // --- Barra superior: título + hora del último refresco | leyenda + botón
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblActualizado);

        pnlLeyenda.setOpaque(false);
        for (EstadoVisualCancha estado : EstadoVisualCancha.values()) {
            pnlLeyenda.add(new JLabel(estado.getDescripcion(), new Punto(estado), SwingConstants.LEFT));
        }
        btnActualizar.addActionListener(e -> actualizarAhora());
        pnlLeyenda.add(btnActualizar);

        JPanel barra = new JPanel(new BorderLayout(Tema.MARGEN, 0));
        barra.setOpaque(false);
        barra.add(textos, BorderLayout.WEST);
        barra.add(pnlLeyenda, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

        // --- Cuadrícula de tarjetas dentro de un scroll.
        // Va en el NORTE de un contenedor para que las tarjetas no se estiren a lo alto.
        pnlCuadricula.setOpaque(false);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(pnlCuadricula, BorderLayout.NORTH);
        scrMapa.setViewportView(contenedor);
        scrMapa.setBorder(BorderFactory.createEmptyBorder());
        scrMapa.getVerticalScrollBar().setUnitIncrement(16);
        scrMapa.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarColumnas();
            }
        });
        add(scrMapa, BorderLayout.CENTER);
    }

    /** Colores del Tema (se vuelve a llamar si el desarrollador los cambia). */
    public final void aplicarTema() {
        Color fondo = Tema.color(ColorTema.FONDO);
        setBackground(fondo);
        scrMapa.getViewport().setBackground(fondo);
        Tema.titulo(lblTitulo);
        lblTitulo.setFont(Tema.FUENTE_SUBTITULO);
        Tema.textoSecundario(lblActualizado);
        Tema.textoSecundario(lblVacio);
        Tema.botonSecundario(btnActualizar);
        for (java.awt.Component c : pnlLeyenda.getComponents()) {
            if (c instanceof JLabel) {
                c.setFont(Tema.FUENTE_PEQUENA);
                c.setForeground(Tema.color(ColorTema.TEXTO));
            }
        }
        repaint(); // las tarjetas toman los colores nuevos al repintarse
    }

    // =====================================================================
    //  DATOS (los da el controlador; aquí solo se pintan)
    // =====================================================================

    /**
     * Vuelve a pedir el estado de las canchas y repinta.
     * Si son las mismas canchas, solo actualiza cada tarjeta (sin parpadeo);
     * si se agregó o quitó alguna, reconstruye la cuadrícula.
     */
    public final void recargar() {
        List<InfoCancha> fichas = mapa.getEstadoCanchas();
        if (mismasCanchas(fichas)) {
            for (int i = 0; i < fichas.size(); i++) {
                tarjetas.get(i).setInfo(fichas.get(i));
            }
        } else {
            reconstruir(fichas);
        }
        String ayuda = mapa.isTiendaActiva()
                ? "clic en una cancha En curso para abrir el POS"
                : "clic en una cancha para ver su información";
        lblActualizado.setText("Actualizado " + FechaUtil.formatearHora(FechaUtil.ahora().toLocalTime())
                + "  ·  " + ayuda);
    }

    private boolean mismasCanchas(List<InfoCancha> fichas) {
        if (fichas.size() != tarjetas.size() || fichas.isEmpty()) {
            return false;
        }
        for (int i = 0; i < fichas.size(); i++) {
            if (!fichas.get(i).getCancha().getCodigo().equals(tarjetas.get(i).getCodigoCancha())) {
                return false;
            }
        }
        return true;
    }

    /** Crea una TarjetaCancha por cada ficha (aquí nace el mapa "por código"). */
    private void reconstruir(List<InfoCancha> fichas) {
        tarjetas.clear();
        pnlCuadricula.removeAll();
        if (fichas.isEmpty()) {
            pnlCuadricula.setLayout(new BorderLayout());
            pnlCuadricula.add(lblVacio, BorderLayout.CENTER);
        } else {
            pnlCuadricula.setLayout(new GridLayout(0, 1, Tema.MARGEN, Tema.MARGEN));
            for (InfoCancha ficha : fichas) {
                TarjetaCancha tarjeta = new TarjetaCancha(ficha);
                tarjeta.alHacerClic(this::alHacerClicEnCancha);
                tarjetas.add(tarjeta);
                pnlCuadricula.add(tarjeta);
            }
            ajustarColumnas();
        }
        pnlCuadricula.revalidate();
        pnlCuadricula.repaint();
    }

    /** Cuántas tarjetas caben por fila según el ancho visible (mínimo 1). */
    private void ajustarColumnas() {
        if (!(pnlCuadricula.getLayout() instanceof GridLayout)) {
            return;
        }
        int ancho = scrMapa.getViewport().getWidth();
        if (ancho <= 0) {
            ancho = 3 * (Tema.ANCHO_TARJETA_CANCHA + Tema.MARGEN);
        }
        int columnas = Math.max(1, (ancho + Tema.MARGEN) / (Tema.ANCHO_TARJETA_CANCHA + Tema.MARGEN));
        GridLayout grid = (GridLayout) pnlCuadricula.getLayout();
        if (grid.getColumns() != columnas) {
            grid.setColumns(columnas);
            pnlCuadricula.revalidate();
        }
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    /** Botón "Actualizar": no espera al Timer; pone las reservas al día con la hora. */
    private void actualizarAhora() {
        int cambios = mapa.refrescar();
        recargar();
        if (cambios > 0) {
            alCambiarDatos.run();
        }
    }
    /** El panel decide QUÉ abrir según el estado calculado por el controlador. */
    private void alHacerClicEnCancha(InfoCancha info) {
        if (info.getEstado().permiteAbrirPos()) {
            if (mapa.isTiendaActiva()) {
                abrirPos(info);
            } else {
                verDetalle(info.getReservaEnCurso());
            }
        } else {
            mostrarFicha(info);
        }
    }
    /**
     * Abre el POS para la reserva que se está jugando. El controlador valida la regla
     * "el POS solo se abre desde una cancha EN CURSO" y devuelve esa reserva.
     */
    private void abrirPos(InfoCancha info) {
        try {
            Reserva reserva = mapa.getReservaParaPos(info.getCancha().getCodigo());
            PosDialog dialogo = new PosDialog(this, sistema, reserva.getCodigo());
            dialogo.setVisible(true); // modal: espera hasta que se cierre
            if (dialogo.getVentaRealizada() != null) {
                recargar();
                alCambiarDatos.run(); // el consumo de la reserva cambió
            }
        } catch (CanchaProException ex) {
            error(ex); // p. ej. el partido terminó justo antes del clic
            recargar();
        }
    }

    /** Cancha sin partido: muestra su información y, si hay, ofrece ver la próxima reserva. */
    private void mostrarFicha(InfoCancha info) {
        Reserva proxima = info.getProximaReserva();
        Object[] opciones = proxima == null ? new Object[]{"Cerrar"}
                : new Object[]{"Ver próxima reserva (" + proxima.getCodigo() + ")", "Cerrar"};
        int r = JOptionPane.showOptionDialog(this, info.getDetalle(), info.getCancha().getNombre(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                new Punto(info.getEstado(), 18), opciones, opciones[0]);
        if (proxima != null && r == 0) {
            verDetalle(proxima);
        }
    }

    private void verDetalle(Reserva reserva) {
        try {
            new DetalleReservaDialog(this, sistema, reserva.getCodigo()).setVisible(true);
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Mapa de canchas", JOptionPane.WARNING_MESSAGE);
    }

    // =====================================================================
    //  ÍCONO DE LA LEYENDA: un círculo del color del estado (dibujado por código)
    // =====================================================================

    private static final class Punto implements Icon {

        private final EstadoVisualCancha estado;
        private final int tamano;

        Punto(EstadoVisualCancha estado) {
            this(estado, 12);
        }

        Punto(EstadoVisualCancha estado, int tamano) {
            this.estado = estado;
            this.tamano = tamano;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TarjetaCancha.colorDe(estado)); // se lee al pintar: respeta cambios de color
            g2.fillOval(x, y, tamano, tamano);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(Tema.ANCHO_TARJETA_CANCHA + 2 * Tema.MARGEN, Tema.ALTO_TARJETA_CANCHA);
    }
}

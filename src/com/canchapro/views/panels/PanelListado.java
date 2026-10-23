/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
/**
 *
 *  Plantilla común de las pantallas "lista + botones" del menú (Canchas, Tarifas,
 * Servicios…). Patrón TEMPLATE METHOD: esta clase arma todo lo repetido
 * (título, barra de botones, tabla, conteo, colores, mensajes) y cada pantalla
 * hija solo completa lo propio:
 *
 * 
 *   {@link #cargar()}: pedir la lista a SU controlador.
 *   {@link #textoConteo(List)}: el texto "3 canchas · 1 en mantenimiento".
 *   {@link #alCambiarSeleccion(Object)}: habilitar sus botones.
 * 
 *
 * 
 *  ┌ Título ───────────────────────────────────────────────┐
 *  │ Subtítulo                                    │
 *  │ conteo            [botón] [botón] [botón]    │
 *  │ ┌ tabla ──────────────────────────────────────────┐   │
 *  │ └───────────────────────────────────────────-──────┘   │
 *  │ (pie opcional: avisos, simulador…)           │
 *  └───────────────────────────────────────────────────--────┘
 
 *
 * @param <T> tipo de objeto que lista (Cancha, Tarifa, ServicioAdicional)
 */
public abstract class PanelListado<T> extends JPanel implements PanelModulo {

    private static final long serialVersionUID = 1L;

    private final String tituloModulo;
    private final JLabel lblTitulo;
    private final JLabel lblSubtitulo;
    private final JLabel lblConteo = new JLabel(" ");
    private final JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
    private final List<JButton> botones = new ArrayList<>();
    private final JPanel pnlPie = new JPanel(new BorderLayout());

    protected final ModeloTablaLista<T> modelo;
    protected final JTable tabla = new JTable();
    private final JScrollPane scrTabla = new JScrollPane(tabla);

    protected PanelListado(String titulo, String subtitulo, ModeloTablaLista<T> modelo, int[] anchos) {
        this.tituloModulo = titulo;
        this.lblTitulo = new JLabel(titulo);
        this.lblSubtitulo = new JLabel(subtitulo);
        this.modelo = modelo;

        setLayout(new BorderLayout(0, Tema.ESPACIO));
        setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        pnlBotones.setOpaque(false);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(lblConteo, BorderLayout.WEST);
        barra.add(pnlBotones, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, Tema.MARGEN));
        norte.setOpaque(false);
        norte.add(titulos, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        tabla.setModel(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSorter(new TableRowSorter<>(modelo));
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        // Los números enteros (capacidad, prioridad, cantidad…) se ven centrados
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.setDefaultRenderer(Integer.class, centrado);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alCambiarSeleccion(seleccionado());
            }
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && seleccionado() != null) {
                    alDobleClic(seleccionado());
                }
            }
        });
        add(scrTabla, BorderLayout.CENTER);

        pnlPie.setOpaque(false);
        add(pnlPie, BorderLayout.SOUTH);
    }

    // =====================================================================
    //  LO QUE CADA PANTALLA HIJA DEBE COMPLETAR
    // =====================================================================

    /** Pide la lista a su controlador. */
    protected abstract List<T> cargar() throws CanchaProException;

    /** Texto de la izquierda de la barra, por ejemplo "3 canchas · 1 en mantenimiento". */
    protected abstract String textoConteo(List<T> lista);

    /** Se llama al seleccionar otra fila (o ninguna: null) para habilitar botones. */
    protected abstract void alCambiarSeleccion(T elemento);

    /** Doble clic sobre una fila (por defecto no hace nada; las hijas suelen "Editar"). */
    protected void alDobleClic(T elemento) {
        // opcional
    }

    // =====================================================================
    //  HERRAMIENTAS PARA LAS HIJAS
    // =====================================================================

    /** Crea un botón en la barra superior. El primero que se agrega se pinta como principal. */
    protected JButton agregarBoton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        botones.add(b);
        pnlBotones.add(b);
        return b;
    }

    /** Pone un componente debajo de la tabla (avisos, simulador…). */
    protected void setPie(JComponent pie) {
        pnlPie.removeAll();
        pnlPie.add(pie, BorderLayout.CENTER);
        pnlPie.revalidate();
    }

    /** Muestra u oculta TODA la barra de botones (p. ej. el operador solo consulta). */
    protected void setBotonesVisibles(boolean visibles) {
        pnlBotones.setVisible(visibles);
    }

    /** Dibuja un Double como dinero ("Q150.00"), alineado a la derecha con un pequeño margen. */
    protected static DefaultTableCellRenderer renderizadorMoneda() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
                setText(valor == null ? "" : MonedaUtil.formatear((Double) valor));
            }
        };
    }

    protected T seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.getElementoEn(tabla.convertRowIndexToModel(fila));
    }

    /** Selecciona la primera fila que cumpla la condición (p. ej. el código recién creado). */
    protected void seleccionar(Predicate<T> condicion) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (condicion.test(modelo.getElementoEn(i))) {
                int fila = tabla.convertRowIndexToView(i);
                if (fila >= 0) {
                    tabla.setRowSelectionInterval(fila, fila);
                    tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
                }
                return;
            }
        }
    }

    protected boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(this, pregunta, tituloModulo, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    protected void informar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, tituloModulo, JOptionPane.INFORMATION_MESSAGE);
    }

    protected void advertir(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, tituloModulo, JOptionPane.WARNING_MESSAGE);
    }

    protected void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), tituloModulo, JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  PanelModulo
    // =====================================================================

    /** Vuelve a pedir la lista y conserva la fila que estaba seleccionada. */
    @Override
    public void recargar() {
        T antes = seleccionado();
        try {
            List<T> lista = cargar();
            modelo.setElementos(lista);
            lblConteo.setText(textoConteo(lista));
        } catch (CanchaProException ex) {
            modelo.setElementos(new ArrayList<>());
            lblConteo.setText(ex.getMessage());
        }
        if (antes != null) {
            seleccionar(antes::equals);
        }
        alCambiarSeleccion(seleccionado());
    }

    @Override
    public void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        Tema.textoSecundario(lblConteo);
        for (int i = 0; i < botones.size(); i++) {
            if (i == 0) {
                Tema.botonPrimario(botones.get(i));
            } else {
                Tema.botonSecundario(botones.get(i));
            }
        }
        Tema.tabla(tabla);
        scrTabla.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        repaint();
    }
}

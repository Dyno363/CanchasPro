/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.ReporteController;
import com.canchapro.controllers.ReporteController.Reporte;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TiendaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 ** Módulo REPORTES: los 8 reportes obligatorios del PDF + reporte mensual
 * (prueba 12) + ventas de tienda. Construido por código.
 *
 * 
 *  ┌ Reportes ──────────────────────────────────────────────────────────────┐
 *  │ ┌ Reportes ────────┐ Desde [01/10/2026] Hasta [31/10/2026] [Este mes]   │
 *  │ │ Reservas x fecha │ Estado [Todos ▼]  Top [5]  Mes [Octubre 2026 ▼]    │
 *  │ │ Reservas x cancha│                                     [Generar]      │
 *  │ │ Ingresos         │ ── Reservas por fecha ──                           │
 *  │ │ Pagos pendientes │ Del 01/10/2026 al 31/10/2026                       │
 *  │ │ Canceladas       │ ┌ tabla (cualquier reporte) ─────────────────────┐ │
 *  │ │ Cancha + usada   │ └───────────────────────────────────────────────┘ │
 *  │ │ Horarios demanda │ ┌ Resumen ──────────────────────────────────────┐ │
 *  │ │ Clientes top     │ │ Total de reservas: 13 · Monto total: Q4,275   │ │
 *  │ │ Reporte mensual  │ └───────────────────────────────────────────────┘ │
 *  │ │ Ventas tienda    │                       [Copiar al portapapeles]     │
 *  │ └──────────────────┘                                                    │
 *  └────────────────────────────────────────────────────────────────────────┘
 * 
 *
 * a vista NO agrupa, suma ni ordena: eso lo hace el {@link ReporteController}
 * y devuelve un {@link Reporte} genérico (columnas + filas + resumen). Por eso
 * UNA sola tabla sirve para mostrar cualquiera de los 10 reportes.
 * @author SANTANA
 */
public class ReportesPanel extends JPanel implements PanelModulo{
    private static final long serialVersionUID = 1L;
    private static final Locale ES = Locale.forLanguageTag("es");

    /** Cada reporte disponible y QUÉ filtros necesita (los demás se ocultan). */
    private enum TipoReporte {
        RESERVAS_POR_FECHA("Reservas por fecha", true, true, false, false),
        RESERVAS_POR_CANCHA("Reservas por cancha", true, false, false, false),
        INGRESOS_POR_PERIODO("Ingresos por período", true, false, false, false),
        PAGOS_PENDIENTES("Pagos pendientes", false, false, false, false),
        RESERVAS_CANCELADAS("Reservas canceladas", true, false, false, false),
        CANCHA_MAS_UTILIZADA("Cancha más utilizada", true, false, false, false),
        HORARIOS_DEMANDA("Horarios de mayor demanda", true, false, false, false),
        CLIENTES_FRECUENTES("Clientes con más reservas", true, false, true, false),
        REPORTE_MENSUAL("Reporte mensual", false, false, false, true),
        VENTAS_TIENDA("Ventas por producto (tienda)", true, false, false, false);

        private final String texto;
        private final boolean usaRango;
        private final boolean usaEstado;
        private final boolean usaLimite;
        private final boolean usaMes;

        TipoReporte(String texto, boolean usaRango, boolean usaEstado, boolean usaLimite, boolean usaMes) {
            this.texto = texto;
            this.usaRango = usaRango;
            this.usaEstado = usaEstado;
            this.usaLimite = usaLimite;
            this.usaMes = usaMes;
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    private final transient ReporteController reportes;
    private final transient TiendaController tienda;
    private transient Reporte actual;

    private final JLabel lblTitulo = new JLabel("Reportes");
    private final JLabel lblSubtitulo = new JLabel("Información procesada a partir de las reservas, pagos y ventas en memoria.");

    // --- Lista de reportes (izquierda)
    private final DefaultListModel<TipoReporte> modeloLista = new DefaultListModel<>();
    private final JList<TipoReporte> lstReportes = new JList<>(modeloLista);

    // --- Filtros
    private final JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 4));
    private final JLabel lblDesde = new JLabel("Desde");
    private final JTextField txtDesde = new JTextField(8);
    private final JLabel lblHasta = new JLabel("Hasta");
    private final JTextField txtHasta = new JTextField(8);
    private final JButton btnEsteMes = new JButton("Este mes");
    private final JButton btnTodo = new JButton("Todo");
    private final JLabel lblEstado = new JLabel("Estado");
    private final JComboBox<Object> cmbEstado = new JComboBox<>();
    private final JLabel lblLimite = new JLabel("Top");
    private final JSpinner spnLimite = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
    private final JLabel lblMes = new JLabel("Mes");
    private final JComboBox<YearMonth> cmbMes = new JComboBox<>();
    private final JButton btnGenerar = new JButton("Generar");

    // --- Resultado
    private final JLabel lblReporte = new JLabel(" ");
    private final JLabel lblDetalle = new JLabel(" ");
    private final DefaultTableModel modeloTabla = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        /**
         * Si TODA la columna es del mismo tipo (p. ej. Integer), se usa ese tipo y se
         * ordena como número. Si mezcla tipos (el reporte mensual tiene 13 y "Q4,275.00"
         * en la misma columna), se declara Object: así Swing compara su texto (toString)
         * y ordenar no falla.
         */
        @Override
        public Class<?> getColumnClass(int columna) {
            Class<?> tipo = null;
            for (int fila = 0; fila < getRowCount(); fila++) {
                Object valor = getValueAt(fila, columna);
                if (valor == null) {
                    continue;
                }
                if (tipo == null) {
                    tipo = valor.getClass();
                } else if (tipo != valor.getClass()) {
                    return Object.class; // tipos mezclados
                }
            }
            return tipo == null ? String.class : tipo;
        }
    };
    private final JTable tblReporte = new JTable(modeloTabla);
    private final JScrollPane scrReporte = new JScrollPane(tblReporte);
    private final JPanel pnlResumen = new JPanel(new BorderLayout());
    private final JLabel lblResumen = new JLabel(" ");
    private final JButton btnCopiar = new JButton("Copiar al portapapeles");

    public ReportesPanel(SistemaController sistema) {
        this.reportes = sistema.getReporteController();
        this.tienda = sistema.getTiendaController();
        construir();
        registrarEventos();
        irAEsteMes();
        aplicarTema();
        recargar();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setLayout(new BorderLayout(Tema.MARGEN, Tema.MARGEN));
        setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);
        add(titulos, BorderLayout.NORTH);

        // ---- Izquierda: lista de reportes
        lstReportes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstReportes.setFixedCellHeight(34);
        JScrollPane scrLista = new JScrollPane(lstReportes);
        scrLista.setPreferredSize(new Dimension(230, 200));
        add(scrLista, BorderLayout.WEST);

        // ---- Centro: filtros + resultado
        cmbEstado.addItem("Todos");
        for (EstadoReserva e : EstadoReserva.values()) {
            cmbEstado.addItem(e);
        }
        llenarMeses();
        spnLimite.setPreferredSize(new Dimension(60, 30));
        pnlFiltros.setOpaque(false);
        for (JComponent c : new JComponent[]{lblDesde, txtDesde, lblHasta, txtHasta, btnEsteMes, btnTodo,
            lblEstado, cmbEstado, lblLimite, spnLimite, lblMes, cmbMes}) {
            pnlFiltros.add(c);
        }
        JPanel barraFiltros = new JPanel(new BorderLayout(Tema.ESPACIO, 0));
        barraFiltros.setOpaque(false);
        barraFiltros.add(pnlFiltros, BorderLayout.CENTER);
        barraFiltros.add(btnGenerar, BorderLayout.EAST); // siempre visible, aunque haya muchos filtros

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 2));
        encabezado.setOpaque(false);
        encabezado.add(lblReporte);
        encabezado.add(lblDetalle);

        JPanel arriba = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        arriba.setOpaque(false);
        arriba.add(barraFiltros, BorderLayout.NORTH);
        arriba.add(encabezado, BorderLayout.SOUTH);

        pnlResumen.add(lblResumen, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botones.add(btnCopiar);
        JPanel abajo = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        abajo.setOpaque(false);
        abajo.add(pnlResumen, BorderLayout.CENTER);
        abajo.add(botones, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        centro.setOpaque(false);
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(scrReporte, BorderLayout.CENTER);
        centro.add(abajo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        // Alineación: enteros al centro; dinero, horas y porcentajes a la derecha
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        tblReporte.setDefaultRenderer(Integer.class, centrado);
        tblReporte.setDefaultRenderer(String.class, new RenderizadorTexto());
        tblReporte.setDefaultRenderer(Object.class, new RenderizadorTexto()); // columnas con tipos mezclados
        tblReporte.setAutoCreateRowSorter(true); // clic en el encabezado = ordenar
    }

    /** Los últimos 12 meses (el actual primero) para el reporte mensual. */
    private void llenarMeses() {
        DefaultComboBoxModel<YearMonth> meses = new DefaultComboBoxModel<>();
        YearMonth mes = YearMonth.from(FechaUtil.ahora());
        for (int i = 0; i < 12; i++) {
            meses.addElement(mes.minusMonths(i));
        }
        cmbMes.setModel(meses);
        cmbMes.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor instanceof YearMonth) {
                    setText(nombreMes((YearMonth) valor));
                }
                return this;
            }
        });
    }

    private void registrarEventos() {
        lstReportes.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarFiltrosDe(lstReportes.getSelectedValue());
                generar(); // al elegir un reporte se genera de una vez
            }
        });
        btnGenerar.addActionListener(e -> generar());
        btnEsteMes.addActionListener(e -> {
            irAEsteMes();
            generar();
        });
        btnTodo.addActionListener(e -> {
            txtDesde.setText("");
            txtHasta.setText("");
            generar();
        });
        btnCopiar.addActionListener(e -> copiar());
    }

    /** Muestra solo los filtros que usa el reporte elegido. */
    private void mostrarFiltrosDe(TipoReporte tipo) {
        boolean rango = tipo != null && tipo.usaRango;
        for (JComponent c : new JComponent[]{lblDesde, txtDesde, lblHasta, txtHasta, btnEsteMes, btnTodo}) {
            c.setVisible(rango);
        }
        lblEstado.setVisible(tipo != null && tipo.usaEstado);
        cmbEstado.setVisible(tipo != null && tipo.usaEstado);
        lblLimite.setVisible(tipo != null && tipo.usaLimite);
        spnLimite.setVisible(tipo != null && tipo.usaLimite);
        lblMes.setVisible(tipo != null && tipo.usaMes);
        cmbMes.setVisible(tipo != null && tipo.usaMes);
        pnlFiltros.revalidate();
        pnlFiltros.repaint();
    }

    private void irAEsteMes() {
        YearMonth mes = YearMonth.from(FechaUtil.ahora());
        txtDesde.setText(FechaUtil.formatearFecha(mes.atDay(1)));
        txtHasta.setText(FechaUtil.formatearFecha(mes.atEndOfMonth()));
    }

    // =====================================================================
    //  PanelModulo
    // =====================================================================

    /** La lista se arma cada vez: "Ventas de tienda" solo aparece si el módulo Tienda está activo. */
    @Override
    public void recargar() {
        TipoReporte antes = lstReportes.getSelectedValue();
        modeloLista.clear();
        for (TipoReporte t : TipoReporte.values()) {
            if (t != TipoReporte.VENTAS_TIENDA || tienda.isModuloActivo()) {
                modeloLista.addElement(t);
            }
        }
        if (antes != null && modeloLista.contains(antes)) {
            lstReportes.setSelectedValue(antes, true); // vuelve a generar con los datos de ahora
        } else {
            lstReportes.setSelectedIndex(0);
        }
    }

    @Override
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        lstReportes.setFont(Tema.FUENTE_NEGRITA);
        lstReportes.setSelectionBackground(Tema.color(ColorTema.PRIMARIO));
        lstReportes.setSelectionForeground(Tema.textoSobre(Tema.color(ColorTema.PRIMARIO)));
        lstReportes.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        Tema.campo(txtDesde);
        Tema.campo(txtHasta);
        for (JLabel l : new JLabel[]{lblDesde, lblHasta, lblEstado, lblLimite, lblMes}) {
            l.setFont(Tema.FUENTE_NEGRITA);
        }
        Tema.botonSecundario(btnEsteMes);
        Tema.botonSecundario(btnTodo);
        Tema.botonPrimario(btnGenerar);
        Tema.botonSecundario(btnCopiar);
        lblReporte.setFont(Tema.FUENTE_SUBTITULO);
        lblReporte.setForeground(Tema.color(ColorTema.TEXTO));
        Tema.textoSecundario(lblDetalle);
        Tema.tabla(tblReporte);
        scrReporte.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        Tema.tarjeta(pnlResumen);
        pnlResumen.setBorder(BorderFactory.createCompoundBorder(Tema.bordeTarjeta(),
                BorderFactory.createEmptyBorder(Tema.ESPACIO, Tema.MARGEN, Tema.ESPACIO, Tema.MARGEN)));
        lblResumen.setFont(Tema.FUENTE_NORMAL);
        lblResumen.setForeground(Tema.color(ColorTema.TEXTO));
        repaint();
    }

    // =====================================================================
    //  GENERAR (una llamada al controlador según el reporte elegido)
    // =====================================================================

    private void generar() {
        TipoReporte tipo = lstReportes.getSelectedValue();
        if (tipo == null) {
            return;
        }
        try {
            LocalDate desde = fechaOpcional(txtDesde.getText());
            LocalDate hasta = fechaOpcional(txtHasta.getText());
            switch (tipo) {
                case RESERVAS_POR_FECHA:
                    Object estado = cmbEstado.getSelectedItem();
                    mostrar(reportes.reservasPorFecha(desde, hasta,
                            estado instanceof EstadoReserva ? (EstadoReserva) estado : null));
                    break;
                case RESERVAS_POR_CANCHA:
                    mostrar(reportes.reservasPorCancha(desde, hasta));
                    break;
                case INGRESOS_POR_PERIODO:
                    mostrar(reportes.ingresosPorPeriodo(desde, hasta));
                    break;
                case PAGOS_PENDIENTES:
                    mostrar(reportes.pagosPendientes());
                    break;
                case RESERVAS_CANCELADAS:
                    mostrar(reportes.reservasCanceladas(desde, hasta));
                    break;
                case CANCHA_MAS_UTILIZADA:
                    mostrar(reportes.canchaMasUtilizada(desde, hasta));
                    break;
                case HORARIOS_DEMANDA:
                    mostrar(reportes.horariosMayorDemanda(desde, hasta));
                    break;
                case CLIENTES_FRECUENTES:
                    mostrar(reportes.clientesFrecuentes(desde, hasta, (Integer) spnLimite.getValue()));
                    break;
                case REPORTE_MENSUAL:
                    mostrar(reportes.reporteMensual((YearMonth) cmbMes.getSelectedItem()));
                    break;
                default:
                    mostrar(reportes.ventasPorProducto(desde, hasta));
                    break;
            }
        } catch (CanchaProException ex) {
            actual = null;
            modeloTabla.setDataVector(new Object[0][0], new Object[0]);
            lblReporte.setText(tipo.toString());
            lblDetalle.setText(" ");
            lblResumen.setForeground(Tema.color(ColorTema.ERROR));
            lblResumen.setText(ex.getMessage()); // p. ej. "Fecha inválida" o "desde es posterior a hasta"
        }
    }

    /** Campo vacío = sin límite (el controlador interpreta null así). */
    private static LocalDate fechaOpcional(String texto) throws CanchaProException {
        return texto == null || texto.trim().isEmpty() ? null : FechaUtil.leerFecha(texto);
    }

    /** Pone CUALQUIER reporte en la misma tabla: columnas, filas y resumen vienen del controlador. */
    private void mostrar(Reporte r) {
        actual = r;
        lblReporte.setText(r.getTitulo());
        lblDetalle.setText(r.getSubtitulo() + "   ·   " + r.getFilas().size() + " fila(s)");
        modeloTabla.setDataVector(r.getFilas().toArray(new Object[0][]), r.getColumnas());
        Tema.tabla(tblReporte);
        ajustarAnchos();

        StringBuilder sb = new StringBuilder("<html>");
        if (r.estaVacio()) {
            sb.append("<i>No hay datos para los filtros elegidos.</i><br>");
        }
        for (String linea : r.getResumen()) {
            boolean importante = linea.equals(linea.toUpperCase(ES)) || linea.startsWith("Total")
                    || linea.startsWith("INGRESO");
            sb.append(importante ? "<b>" : "").append("• ").append(linea.trim()).append(importante ? "</b>" : "")
              .append("<br>");
        }
        lblResumen.setForeground(Tema.color(ColorTema.TEXTO));
        lblResumen.setText(sb.append("</html>").toString());
    }

    /** Cada columna toma el ancho de su texto más largo (encabezado o celda), entre 60 y 320 px. */
    private void ajustarAnchos() {
        for (int col = 0; col < tblReporte.getColumnCount(); col++) {
            javax.swing.table.TableColumn columna = tblReporte.getColumnModel().getColumn(col);
            int ancho = tblReporte.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tblReporte, columna.getHeaderValue(), false, false, -1, col)
                    .getPreferredSize().width;
            for (int fila = 0; fila < tblReporte.getRowCount(); fila++) {
                Component celda = tblReporte.prepareRenderer(tblReporte.getCellRenderer(fila, col), fila, col);
                ancho = Math.max(ancho, celda.getPreferredSize().width);
            }
            columna.setPreferredWidth(Math.min(320, Math.max(60, ancho + 16)));
        }
    }

    /** Copia el reporte como texto (para pegarlo en Word o Excel). No escribe ningún archivo. */
    private void copiar() {
        if (actual == null) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(actual.aTexto()), null);
        lblDetalle.setText(actual.getSubtitulo() + "   ·   ✔ Copiado al portapapeles");
    }

    private static String nombreMes(YearMonth mes) {
        String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
    }

    // =====================================================================
    //  DIBUJO DE CELDAS
    // =====================================================================

    /** Texto normal a la izquierda; dinero ("Q…"), horas ("… h") y porcentajes ("…%") a la derecha. */
    private static final class RenderizadorTexto extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        protected void setValue(Object valor) {
            String texto = valor == null ? "" : valor.toString();
            boolean numero = texto.matches("-?Q[\\d,.]+") || texto.matches("[\\d.,]+ h") || texto.matches("[\\d.,]+%");
            setHorizontalAlignment(numero ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
            setText(texto);
        }
    }
    
}

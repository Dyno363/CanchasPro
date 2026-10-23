/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
/**
 * Modelo de la tabla de reservas (pestaña 1 del Dashboard).
 *
 * Es un adaptador: la JTable no sabe qué es una {@link Reserva}; solo
 * pregunta "¿cuántas filas hay?" y "¿qué va en la fila 3, columna 2?". Esta
 * clase responde esas preguntas a partir de una {@code List<Reserva>}.
 *
 * 
 *  Código | Inicio           | Horario       | Cancha   | Cliente     | Estado   | Total     | Saldo
 *  R0004  | 15/10/2026 18:00 | 18:00 - 19:30 | Cancha 1 | Ana López   | Confirm. | Q350.00   | Q0.00
 * 
 *
 * No calcula nada por su cuenta: total y saldo los da la propia Reserva
 * (modelo) y la lista ya filtrada la entrega el ReservaController.
 */
public class ModeloTablaReservas extends AbstractTableModel{
    private static final long serialVersionUID = 1L;

    // Índices de columna (constantes para no usar "números mágicos")
    public static final int COL_CODIGO = 0;
    public static final int COL_INICIO = 1;
    public static final int COL_HORARIO = 2;
    public static final int COL_CANCHA = 3;
    public static final int COL_CLIENTE = 4;
    public static final int COL_ESTADO = 5;
    public static final int COL_TOTAL = 6;
    public static final int COL_SALDO = 7;

    private static final String[] COLUMNAS = {
        "Código", "Inicio", "Horario", "Cancha", "Cliente", "Estado", "Total", "Saldo"
    };

    /**
     * Cómo se calcula la columna Saldo. Por defecto, solo el saldo de la reserva;
     * el panel le pasa {@code PagoController::getSaldoTotal} para incluir la
     * tienda a cuenta. Así el modelo de tabla no conoce a ningún controlador.
     */
    private transient java.util.function.Function<Reserva, Double> calculoSaldo =
            r -> r.getEstado() == EstadoReserva.CANCELADA ? 0.0 : r.calcularSaldo();

    /** @param calculo función que da el saldo de una reserva (null = la de por defecto) */
    public void setCalculoSaldo(java.util.function.Function<Reserva, Double> calculo) {
        if (calculo != null) {
            this.calculoSaldo = calculo;
            fireTableDataChanged();
        }
    }
    /** Tipo de dato de cada columna: permite ordenar bien (fechas y montos, no texto). */
    private static final Class<?>[] TIPOS = {
        String.class, LocalDateTime.class, String.class, String.class, String.class,
        EstadoReserva.class, Double.class, Double.class
    };

    private static final int[] ANCHOS = {70, 130, 110, 120, 180, 110, 90, 90};

    private transient List<Reserva> reservas = new ArrayList<>();

    // =====================================================================
    //  DATOS
    // =====================================================================

    /** Reemplaza las filas (la lista viene ya filtrada del controlador). */
    public void setReservas(List<Reserva> nuevas) {
        reservas = nuevas == null ? new ArrayList<>() : new ArrayList<>(nuevas);
        fireTableDataChanged(); // avisa a la JTable que se vuelva a dibujar
    }

    /** @return copia de solo lectura de las reservas mostradas */
    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    /**
     * Reserva de una fila DEL MODELO. Si la tabla está ordenada, primero convierta
     * el índice: {@code tabla.convertRowIndexToModel(tabla.getSelectedRow())}.
     *
     * @return la reserva, o null si la fila no existe
     */
    public Reserva getReservaEn(int filaModelo) {
        return filaModelo >= 0 && filaModelo < reservas.size() ? reservas.get(filaModelo) : null;
    }

    // =====================================================================
    //  LO QUE LA JTable LE PREGUNTA AL MODELO
    // =====================================================================

    @Override
    public int getRowCount() {
        return reservas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        return TIPOS[columna];
    }

    /** La tabla es solo de consulta: se edita con los botones/diálogos. */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Reserva r = reservas.get(fila);
        switch (columna) {
            case COL_CODIGO:
                return r.getCodigo();
            case COL_INICIO:
                return r.getInicio();
            case COL_HORARIO:
                return FechaUtil.formatearHora(r.getInicio().toLocalTime()) + " - "
                        + FechaUtil.formatearHora(r.getFin().toLocalTime());
            case COL_CANCHA:
                return r.getCancha().getNombre();
            case COL_CLIENTE:
                return r.getCliente().getNombre();
            case COL_ESTADO:
                return r.getEstado();
            case COL_TOTAL:
                return r.calcularTotal();
            case COL_SALDO:
                // Una cancelada sin deuda de tienda se muestra vacía (no se cobra).
                double saldo = calculoSaldo.apply(r);
                return r.getEstado() == EstadoReserva.CANCELADA && MonedaUtil.esCero(saldo) ? null : saldo;
            default:
                return null;
        }
    }

    // =====================================================================
    //  CONFIGURAR LA JTable (estilo, anchos, ordenamiento y dibujantes)
    // =====================================================================

    /**
     * Conecta este modelo a una tabla y le aplica el estilo del sistema.
     * El panel de reservas solo llama: {@code modelo.configurar(tblReservas);}
     */
    public void configurar(JTable tabla) {
        tabla.setModel(this);
        Tema.tabla(tabla);
        tabla.setAutoCreateRowSorter(false);
        tabla.setRowSorter(new TableRowSorter<>(this)); // clic en el encabezado = ordenar

        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < ANCHOS.length; i++) {
            columnas.getColumn(i).setPreferredWidth(ANCHOS[i]);
        }
        columnas.getColumn(COL_INICIO).setCellRenderer(new RenderizadorFecha());
        columnas.getColumn(COL_ESTADO).setCellRenderer(new RenderizadorEstado());
        columnas.getColumn(COL_TOTAL).setCellRenderer(new RenderizadorMonto(false));
        columnas.getColumn(COL_SALDO).setCellRenderer(new RenderizadorMonto(true));
    }

    // =====================================================================
    //  DIBUJANTES DE CELDAS (cómo se VE cada valor; el valor real no cambia)
    // =====================================================================

    /** LocalDateTime → "15/10/2026 18:00" (se ordena como fecha, se ve como texto). */
    private static final class RenderizadorFecha extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        protected void setValue(Object valor) {
            setText(valor instanceof LocalDateTime ? FechaUtil.formatearFechaHora((LocalDateTime) valor) : "");
        }
    }

    /** Double → "Q1,250.00" alineado a la derecha; el saldo > 0 en rojo; null → "—". */
    private static final class RenderizadorMonto extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final boolean resaltarDeuda;

        RenderizadorMonto(boolean resaltarDeuda) {
            this.resaltarDeuda = resaltarDeuda;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            if (!(valor instanceof Double)) {
                setText("—");
                setForeground(Tema.color(ColorTema.TEXTO_SECUNDARIO));
                setFont(Tema.FUENTE_NORMAL);
                return this;
            }
            double monto = (Double) valor;
            setText(MonedaUtil.formatear(monto));
            boolean debe = resaltarDeuda && !MonedaUtil.esCero(monto);
            setForeground(debe ? Tema.color(ColorTema.ERROR) : Tema.color(ColorTema.TEXTO));
            setFont(debe ? Tema.FUENTE_NEGRITA : Tema.FUENTE_NORMAL);
            return this;
        }
    }

    /** EstadoReserva → "píldora" de color (Pendiente amarillo, Confirmada azul, etc.). */
    private static final class RenderizadorEstado extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private Color colorPildora = Color.GRAY;

        RenderizadorEstado() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Tema.FUENTE_NEGRITA);
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionada, foco,
                    fila, columna);
            EstadoReserva estado = valor instanceof EstadoReserva ? (EstadoReserva) valor : null;
            colorPildora = Tema.colorEstadoReserva(estado);
            celda.setText(estado == null ? "" : estado.getDescripcion());
            celda.setForeground(Tema.textoSobre(colorPildora));
            celda.setFont(Tema.FUENTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
            return celda;
        }

        /** Primero el fondo normal de la celda, luego la píldora redondeada, luego el texto. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colorPildora);
            int alto = getHeight() - 10;
            g2.fillRoundRect(6, 5, getWidth() - 12, alto, alto, alto);
            g2.dispose();
            setOpaque(false); // que super no tape la píldora con el fondo
            super.paintComponent(g);
            setOpaque(true);
        }
    }
    
}

package com.canchapro.views.panels;

import com.canchapro.controllers.ClienteController;
import com.canchapro.controllers.ClienteController.ResumenCliente;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaReservas;
import com.canchapro.views.dialogs.ClienteDialog;
import com.canchapro.views.dialogs.DetalleReservaDialog;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

/**
 * Módulo CLIENTES del menú lateral (construido por código).
 *
 * 
 *  ┌ Clientes ──────────────────────────────────────────────────────────────┐
 *  │ Registra, busca y edita a los clientes del complejo.                    │
 *  │ Buscar [__________] [Limpiar]   5 clientes   [+ Nuevo] [Editar] [Eliminar] │
 *  ├──────────────────────────────┬──────────────────────────────────────────┤
 *  │ Código │ Nombre │ Teléfono … │  Carlos Pérez                            │
 *  │ CL0001 │ Carlos │ 4411-2233  │  44112233 · carlos@mail.com              │
 *  │ CL0002 │ María  │ …          │  Cliente desde … · última reserva …      │
 *  │                              │  [Reservas][Activas][Pagado][Saldo]       │
 *  │                              │  Historial de reservas                    │
 *  │                              │  ┌ tabla ┐               [Ver reserva]    │
 *  └──────────────────────────────┴──────────────────────────────────────────┘
 * 
 *
 * Izquierda la LISTA, derecha la FICHA del cliente seleccionado (patrón
 * maestro-detalle). Todas las operaciones van al {@link ClienteController};
 * los números de la ficha llegan calculados en un {@link ResumenCliente}.
 */
public class ClientesPanel extends JPanel implements PanelModulo {

    private static final long serialVersionUID = 1L;
    private static final String FICHA_VACIA = "VACIA";
    private static final String FICHA_DATOS = "DATOS";

    private final transient SistemaController sistema;
    private final transient ClienteController clientes;
    /** Avisa al Dashboard: si cambia un nombre, la lista de reservas debe repintarse. */
    private final transient Runnable alCambiarDatos;

    // --- Encabezado y barra
    private final JLabel lblTitulo = new JLabel("Clientes");
    private final JLabel lblSubtitulo = new JLabel("Registra, busca y edita a los clientes del complejo.");
    private final JLabel lblBuscar = new JLabel("Buscar");
    private final JTextField txtBuscar = new JTextField();
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JLabel lblConteo = new JLabel(" ");
    private final JButton btnNuevo = new JButton("+ Nuevo cliente");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    // --- Lista (izquierda)
    private final ModeloClientes modeloClientes = new ModeloClientes();
    private final JTable tblClientes = new JTable();
    private final JScrollPane scrClientes = new JScrollPane(tblClientes);

    // --- Ficha (derecha)
    private final JPanel pnlFicha = new JPanel(new CardLayout());
    private final JPanel pnlDatos = new JPanel(new BorderLayout(0, Tema.ESPACIO));
    private final JLabel lblVacia = new JLabel("Selecciona un cliente para ver su ficha.", SwingConstants.CENTER);
    private final JLabel lblNombre = new JLabel(" ");
    private final JLabel lblContacto = new JLabel(" ");
    private final JLabel lblRegistro = new JLabel(" ");
    private final JLabel lblReservas = new JLabel("0");
    private final JLabel lblActivas = new JLabel("0");
    private final JLabel lblPagado = new JLabel("Q0.00");
    private final JLabel lblSaldo = new JLabel("Q0.00");
    private final JPanel pnlCifras = new JPanel(new GridLayout(1, 4, Tema.ESPACIO, 0));
    private final JLabel lblHistorial = new JLabel("Historial de reservas");
    private final ModeloTablaReservas modeloHistorial = new ModeloTablaReservas();
    private final JTable tblHistorial = new JTable();
    private final JScrollPane scrHistorial = new JScrollPane(tblHistorial);
    private final JButton btnVerReserva = new JButton("Ver reserva");

    public ClientesPanel(SistemaController sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.clientes = sistema.getClienteController();
        this.alCambiarDatos = alCambiarDatos == null ? () -> { } : alCambiarDatos;
        construir();
        registrarEventos();
        aplicarTema();
        recargar();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setLayout(new BorderLayout(0, Tema.ESPACIO));
        setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        // ---- Norte: título + barra de búsqueda y acciones
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        txtBuscar.setPreferredSize(new Dimension(240, 30));
        txtBuscar.setToolTipText("Código, nombre o teléfono");
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        busqueda.setOpaque(false);
        busqueda.add(lblBuscar);
        busqueda.add(txtBuscar);
        busqueda.add(btnLimpiar);
        busqueda.add(lblConteo);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        acciones.setOpaque(false);
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnEliminar);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(busqueda, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, Tema.MARGEN));
        norte.setOpaque(false);
        norte.add(titulos, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        // ---- Centro: lista | ficha
        modeloClientes.configurar(tblClientes);
        construirFicha();
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrClientes, pnlFicha);
        division.setResizeWeight(0.45);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setContinuousLayout(true);
        add(division, BorderLayout.CENTER);
    }

    private void construirFicha() {
        JPanel cabecera = new JPanel(new GridLayout(3, 1, 0, 2));
        cabecera.setOpaque(false);
        cabecera.add(lblNombre);
        cabecera.add(lblContacto);
        cabecera.add(lblRegistro);

        pnlCifras.setOpaque(false);
        pnlCifras.add(cifra("RESERVAS", lblReservas));
        pnlCifras.add(cifra("ACTIVAS", lblActivas));
        pnlCifras.add(cifra("PAGADO", lblPagado));
        pnlCifras.add(cifra("SALDO", lblSaldo));

        JPanel arriba = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        arriba.setOpaque(false);
        arriba.add(cabecera, BorderLayout.NORTH);
        arriba.add(pnlCifras, BorderLayout.CENTER);
        arriba.add(lblHistorial, BorderLayout.SOUTH);

        // Se reutiliza el modelo de la pestaña Reservas. Aquí sobran "Cliente" (ya sabemos
        // quién es) y "Horario" (Inicio ya trae la hora). Solo se ocultan de la VISTA.
        modeloHistorial.configurar(tblHistorial);
        TableColumnModel cols = tblHistorial.getColumnModel();
        TableColumn colCliente = cols.getColumn(ModeloTablaReservas.COL_CLIENTE);
        TableColumn colHorario = cols.getColumn(ModeloTablaReservas.COL_HORARIO);
        tblHistorial.removeColumn(colCliente);
        tblHistorial.removeColumn(colHorario);

        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        abajo.setOpaque(false);
        abajo.add(btnVerReserva);

        pnlDatos.setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
        pnlDatos.add(arriba, BorderLayout.NORTH);
        pnlDatos.add(scrHistorial, BorderLayout.CENTER);
        pnlDatos.add(abajo, BorderLayout.SOUTH);

        pnlFicha.add(lblVacia, FICHA_VACIA);
        pnlFicha.add(pnlDatos, FICHA_DATOS);
    }

    /** Recuadro "TÍTULO / valor" de la ficha (el valor es un label que se actualiza). */
    private static JPanel cifra(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setName("cifra");
        JLabel t = new JLabel(titulo);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private void registrarEventos() {
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                buscar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                buscar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                buscar();
            }
        });
        btnLimpiar.addActionListener(e -> txtBuscar.setText(""));
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnVerReserva.addActionListener(e -> verReserva());

        tblClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarFicha();
            }
        });
        tblClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && seleccionado() != null) {
                    editar();
                }
            }
        });
        tblHistorial.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tblHistorial.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verReserva();
                }
            }
        });
    }

    // =====================================================================
    //  TEMA
    // =====================================================================

    @Override
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        lblBuscar.setFont(Tema.FUENTE_NEGRITA);
        Tema.campo(txtBuscar);
        Tema.textoSecundario(lblConteo);
        Tema.botonSecundario(btnLimpiar);
        Tema.botonPrimario(btnNuevo);
        Tema.botonSecundario(btnEditar);
        Tema.botonPeligro(btnEliminar);
        Tema.botonSecundario(btnVerReserva);
        Tema.tabla(tblClientes);
        Tema.tabla(tblHistorial);
        scrClientes.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        scrHistorial.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));

        Tema.tarjeta(pnlFicha);
        pnlDatos.setOpaque(false);
        Tema.textoSecundario(lblVacia);
        lblNombre.setFont(Tema.FUENTE_TITULO);
        lblNombre.setForeground(Tema.color(ColorTema.TEXTO));
        Tema.textoSecundario(lblContacto);
        Tema.textoSecundario(lblRegistro);
        lblHistorial.setFont(Tema.FUENTE_NEGRITA);
        lblHistorial.setForeground(Tema.color(ColorTema.TEXTO));
        for (java.awt.Component c : pnlCifras.getComponents()) {
            JPanel p = (JPanel) c;
            p.setBackground(Tema.color(ColorTema.FONDO));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            Tema.textoSecundario((JLabel) p.getComponent(0));
            ((JLabel) p.getComponent(1)).setFont(Tema.FUENTE_SUBTITULO);
        }
        repaint();
    }

    // =====================================================================
    //  DATOS
    // =====================================================================

    /** Vuelve a pedir la lista (respetando la búsqueda) y conserva el cliente seleccionado. */
    @Override
    public void recargar() {
        Cliente antes = seleccionado();
        buscar();
        if (antes != null) {
            seleccionar(antes.getCodigo());
        }
        btnEliminar.setVisible(clientes.puedeEliminar()); // el operador no ve Eliminar
    }

    /** La búsqueda la hace el controlador (sin tildes ni mayúsculas, por código, nombre o teléfono). */
    private void buscar() {
        List<Cliente> lista = clientes.buscar(txtBuscar.getText());
        modeloClientes.setClientes(lista);
        lblConteo.setText(lista.size() == 1 ? "1 cliente" : lista.size() + " clientes");
        mostrarFicha();
    }

    private Cliente seleccionado() {
        int fila = tblClientes.getSelectedRow();
        return fila < 0 ? null : modeloClientes.getClienteEn(tblClientes.convertRowIndexToModel(fila));
    }

    private void seleccionar(String codigo) {
        for (int i = 0; i < modeloClientes.getRowCount(); i++) {
            if (modeloClientes.getClienteEn(i).getCodigo().equals(codigo)) {
                int fila = tblClientes.convertRowIndexToView(i);
                if (fila >= 0) {
                    tblClientes.setRowSelectionInterval(fila, fila);
                    tblClientes.scrollRectToVisible(tblClientes.getCellRect(fila, 0, true));
                }
                return;
            }
        }
    }

    /** Llena la ficha derecha con el cliente seleccionado (o muestra el aviso si no hay). */
    private void mostrarFicha() {
        Cliente c = seleccionado();
        CardLayout cartas = (CardLayout) pnlFicha.getLayout();
        if (c == null) {
            modeloHistorial.setReservas(new ArrayList<>());
            cartas.show(pnlFicha, FICHA_VACIA);
            actualizarBotones();
            return;
        }
        try {
            ResumenCliente r = clientes.getResumen(c.getCodigo());
            lblNombre.setText(c.getNombre());
            lblContacto.setText(c.getCodigo() + "   ·   " + c.getTelefono()
                    + (c.tieneCorreo() ? "   ·   " + c.getCorreo() : "   ·   sin correo"));
            lblRegistro.setText("Cliente desde " + FechaUtil.formatearFecha(c.getFechaRegistro())
                    + (r.getUltimaReserva() == null ? "   ·   aún no ha reservado"
                    : "   ·   última reserva " + FechaUtil.formatearFecha(r.getUltimaReserva().toLocalDate())));
            lblReservas.setText(String.valueOf(r.getReservas()));
            lblActivas.setText(String.valueOf(r.getActivas()));
            lblPagado.setText(MonedaUtil.formatear(r.getTotalPagado()));
            lblSaldo.setText(MonedaUtil.formatear(r.getSaldoPendiente()));
            lblSaldo.setForeground(Tema.color(r.tieneSaldo() ? ColorTema.ERROR : ColorTema.EXITO));
            lblHistorial.setText("Historial de reservas (" + r.getReservas() + ")"
                    + (r.getCanceladas() > 0 ? "  ·  " + r.getCanceladas() + " canceladas" : ""));
            modeloHistorial.setReservas(clientes.getHistorial(c.getCodigo()));
            cartas.show(pnlFicha, FICHA_DATOS);
        } catch (CanchaProException ex) {
            error(ex);
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hayCliente = seleccionado() != null;
        btnEditar.setEnabled(hayCliente);
        btnEliminar.setEnabled(hayCliente);
        btnVerReserva.setEnabled(tblHistorial.getSelectedRow() >= 0);
    }

    // =====================================================================
    //  ACCIONES (cada una = una llamada al controlador o a un diálogo)
    // =====================================================================

    private void nuevo() {
        ClienteDialog d = new ClienteDialog(this, clientes, null);
        d.setVisible(true);
        Cliente nuevo = d.getClienteGuardado();
        if (nuevo != null) {
            txtBuscar.setText(""); // para que el nuevo aparezca en la lista
            seleccionar(nuevo.getCodigo());
            JOptionPane.showMessageDialog(this, "Cliente " + nuevo.getCodigo() + " registrado.",
                    "Clientes", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editar() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        ClienteDialog d = new ClienteDialog(this, clientes, c);
        d.setVisible(true);
        if (d.getClienteGuardado() != null) {
            recargar();
            alCambiarDatos.run(); // el nombre nuevo debe verse en la lista de reservas
        }
    }

    private void eliminar() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar al cliente " + c.getCodigo() + " - " + c.getNombre()
                + "?\nSolo se permite si nunca ha hecho una reserva.", "Eliminar cliente",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            clientes.eliminar(c.getCodigo());
            recargar();
        } catch (CanchaProException ex) {
            error(ex); // p. ej. "tiene reservas: conserve el registro"
        }
    }

    private void verReserva() {
        int fila = tblHistorial.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Reserva r = modeloHistorial.getReservaEn(tblHistorial.convertRowIndexToModel(fila));
        try {
            new DetalleReservaDialog(this, sistema, r.getCodigo()).setVisible(true);
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Clientes", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  MODELO DE LA TABLA DE CLIENTES
    // =====================================================================

    /** Columnas: Código, Nombre, Teléfono, Correo, Registrado. Solo lectura. */
    private static final class ModeloClientes extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Código", "Nombre", "Teléfono", "Correo", "Registrado"};
        private static final int[] ANCHOS = {70, 190, 90, 190, 90};

        private transient List<Cliente> lista = new ArrayList<>();

        void setClientes(List<Cliente> nuevos) {
            lista = new ArrayList<>(nuevos);
            fireTableDataChanged();
        }

        Cliente getClienteEn(int filaModelo) {
            return lista.get(filaModelo);
        }

        void configurar(JTable tabla) {
            tabla.setModel(this);
            Tema.tabla(tabla);
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.setRowSorter(new TableRowSorter<>(this));
            for (int i = 0; i < ANCHOS.length; i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(ANCHOS[i]);
            }
            // La fecha se ORDENA como fecha, pero se VE como 05/10/2026
            tabla.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void setValue(Object valor) {
                    setText(valor instanceof LocalDate ? FechaUtil.formatearFecha((LocalDate) valor) : "");
                }
            });
        }

        @Override
        public int getRowCount() {
            return lista.size();
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
            return columna == 4 ? LocalDate.class : String.class;
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Cliente c = lista.get(fila);
            switch (columna) {
                case 0:  return c.getCodigo();
                case 1:  return c.getNombre();
                case 2:  return c.getTelefono();
                case 3:  return c.tieneCorreo() ? c.getCorreo() : "";
                default: return c.getFechaRegistro();
            }
        }
    }
}

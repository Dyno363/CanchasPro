/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.ClienteController;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.ServicioController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 ** Ventana para CREAR una reserva (modal, construida por código).
 *
 * 
 *  ┌ Nueva reserva ──────────────────────────────────────────────────────┐
 *  │ 1. Cliente   [buscar...] [Carlos Pérez ▼] [+ Nuevo cliente]          │
 *  │ 2. Horario   Fecha [16/10/2026] Hora [15:00▼] Duración [1 h▼] Tipo  │
 *  │ 3. Cancha    ┌ canchas LIBRES en ese horario ┐   SERVICIOS          │
 *  │              │ La Bombonera · Fútbol 5       │   ☐ Balón  [1]       │
 *  │              └───────────────────────────────┘   ☐ Árbitro [1]      │
 *  │                                       Cancha Q150 · Servicios Q0     │
 *  │ (mensaje)                                         TOTAL   Q150.00    │
 *  │                                      [Cancelar] [Crear reserva]      │
 *  └──────────────────────────────────────────────────────────────────────┘
 * 
 *
 * Cada cambio (fecha, hora, duración, tipo o servicios) vuelve a preguntar
 * al {@link ReservaController}: qué canchas están libres
 * ({@code buscarCanchasDisponibles}) y cuánto costaría ({@code cotizar}). La
 * regla de choque de horarios, las tarifas y las validaciones viven en el
 * controlador y el modelo; esta ventana solo muestra resultados.
 * @author SANTANA
 */
public class NuevaReservaDialog extends JDialog{
    private static final long serialVersionUID = 1L;
    private static final String CUALQUIERA = "Cualquiera";

    private final transient ReservaController reservas;
    private final transient ClienteController clientes;
    private final transient ServicioController servicios;
    private transient Reserva creada;

    // 1. Cliente
    private final JTextField txtBuscarCliente = new JTextField(12);
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private final JButton btnNuevoCliente = new JButton("+ Nuevo cliente");
    // 2. Horario
    private final JTextField txtFecha = new JTextField(9);
    private final JComboBox<LocalTime> cmbHora = new JComboBox<>();
    private final JComboBox<Integer> cmbDuracion = new JComboBox<>();
    private final JComboBox<String> cmbTipo = new JComboBox<>();
    // 3. Cancha
    private final DefaultListModel<Cancha> modeloCanchas = new DefaultListModel<>();
    private final JList<Cancha> lstCanchas = new JList<>(modeloCanchas);
    private final JLabel lblDisponibles = new JLabel(" ");
    // Servicios: una casilla + cantidad por servicio activo
    private final Map<String, JCheckBox> chkServicios = new LinkedHashMap<>();
    private final Map<String, JSpinner> spnServicios = new LinkedHashMap<>();
    // Resumen
    private final JLabel lblPrecioCancha = new JLabel("-");
    private final JLabel lblPrecioServicios = new JLabel("-");
    private final JLabel lblTotal = new JLabel("-");
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnCrear = new JButton("Crear reserva");
    private final JButton btnCancelar = new JButton("Cancelar");

    /**
     * @param origen  componente desde el que se abre (normalmente el ReservasPanel)
     * @param sistema acceso a los controladores de la sesión
     */
    public NuevaReservaDialog(Component origen, SistemaController sistema) {
        super(ventanaDe(origen), "Nueva reserva", ModalityType.APPLICATION_MODAL);
        this.reservas = sistema.getReservaController();
        this.clientes = sistema.getClienteController();
        this.servicios = sistema.getServicioController();
        construir();
        cargarListas();
        conectarEventos();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
        actualizarTodo();
    }

    /** La ventana que "contiene" al componente (o él mismo si ya es una ventana). */
    private static Window ventanaDe(Component origen) {
        if (origen == null || origen instanceof Window) {
            return (Window) origen;
        }
        return SwingUtilities.getWindowAncestor(origen);
    }

    /** @return la reserva creada, o {@code null} si se canceló */
    public Reserva getReservaCreada() {
        return creada;
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel raiz = new JPanel(new BorderLayout(Tema.MARGEN, Tema.ESPACIO));
        raiz.setBorder(Tema.margen());
        raiz.setBackground(Tema.color(ColorTema.SUPERFICIE));

        // ---- Arriba: cliente y horario
        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0;
        g.gridx = 0;
        datos.add(titulo("1. Cliente"), g);
        g.gridx = 1;
        datos.add(fila(new JLabel("Buscar:"), txtBuscarCliente, cmbCliente, btnNuevoCliente), g);

        g.gridy = 1;
        g.gridx = 0;
        datos.add(titulo("2. Horario"), g);
        g.gridx = 1;
        datos.add(fila(new JLabel("Fecha:"), txtFecha, new JLabel("Hora:"), cmbHora,
                new JLabel("Duración:"), cmbDuracion, new JLabel("Tipo:"), cmbTipo), g);
        raiz.add(datos, BorderLayout.NORTH);

        // ---- Centro: canchas libres (izq.) y servicios + resumen (der.)
        JPanel centro = new JPanel(new GridLayout(1, 2, Tema.MARGEN, 0));
        centro.setOpaque(false);

        JPanel pnlCanchas = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        pnlCanchas.setOpaque(false);
        pnlCanchas.add(titulo("3. Cancha disponible"), BorderLayout.NORTH);
        lstCanchas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCanchas.setFixedCellHeight(Tema.ALTO_FILA_TABLA + 4);
        JScrollPane scrCanchas = new JScrollPane(lstCanchas);
        scrCanchas.setPreferredSize(new Dimension(300, 200));
        pnlCanchas.add(scrCanchas, BorderLayout.CENTER);
        Tema.textoSecundario(lblDisponibles);
        pnlCanchas.add(lblDisponibles, BorderLayout.SOUTH);
        centro.add(pnlCanchas);

        JPanel derecha = new JPanel(new BorderLayout(0, Tema.ESPACIO));
        derecha.setOpaque(false);
        derecha.add(titulo("4. Servicios adicionales"), BorderLayout.NORTH);
        derecha.add(crearListaServicios(), BorderLayout.CENTER);
        derecha.add(crearResumen(), BorderLayout.SOUTH);
        centro.add(derecha);
        raiz.add(centro, BorderLayout.CENTER);

        // ---- Abajo: mensaje y botones
        lblMensaje.setFont(Tema.FUENTE_NEGRITA);
        lblMensaje.setForeground(Tema.color(ColorTema.ERROR));
        Tema.botonPrimario(btnCrear);
        Tema.botonSecundario(btnCancelar);
        Tema.botonSecundario(btnNuevoCliente);
        Tema.campo(txtBuscarCliente);
        Tema.campo(txtFecha);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnCrear);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(lblMensaje, BorderLayout.CENTER);
        pie.add(botones, BorderLayout.EAST);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Una fila por servicio activo: [☐ Balón profesional  Q25.00] [cantidad]. */
    private JComponent crearListaServicios() {
        JPanel lista = new JPanel(new GridBagLayout());
        lista.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 0, 2, 6);
        g.anchor = GridBagConstraints.WEST;
        int fila = 0;
        for (ServicioAdicional s : servicios.listarActivos()) {
            String precio = MonedaUtil.formatear(s.getPrecio()) + (s.isCobroPorHora() ? "/hora" : "");
            JCheckBox chk = new JCheckBox(s.getNombre() + "  " + precio);
            chk.setOpaque(false);
            JSpinner spn = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
            spn.setPreferredSize(new Dimension(60, 26));
            spn.setEnabled(false);
            chkServicios.put(s.getCodigo(), chk);
            spnServicios.put(s.getCodigo(), spn);
            g.gridy = fila++;
            g.gridx = 0;
            g.weightx = 1;
            lista.add(chk, g);
            g.gridx = 1;
            g.weightx = 0;
            lista.add(spn, g);
        }
        JScrollPane scr = new JScrollPane(lista);
        scr.setBorder(BorderFactory.createLineBorder(Tema.color(ColorTema.BORDE)));
        scr.setPreferredSize(new Dimension(300, 170));
        return scr;
    }

    private JComponent crearResumen() {
        JPanel resumen = new JPanel(new GridLayout(3, 2, 4, 2));
        Tema.tarjeta(resumen);
        resumen.add(new JLabel("Cancha:"));
        resumen.add(lblPrecioCancha);
        resumen.add(new JLabel("Servicios:"));
        resumen.add(lblPrecioServicios);
        JLabel lblTextoTotal = new JLabel("TOTAL:");
        lblTextoTotal.setFont(Tema.FUENTE_SUBTITULO);
        lblTotal.setFont(Tema.FUENTE_SUBTITULO);
        lblTotal.setForeground(Tema.color(ColorTema.PRIMARIO));
        resumen.add(lblTextoTotal);
        resumen.add(lblTotal);
        for (JLabel l : new JLabel[]{lblPrecioCancha, lblPrecioServicios, lblTotal}) {
            l.setHorizontalAlignment(JLabel.RIGHT);
        }
        return resumen;
    }

    /** Llena combos: clientes, horas cada 30 min, duraciones y tipos de cancha. */
    private void cargarListas() {
        filtrarClientes();
        cmbCliente.setPreferredSize(new Dimension(220, 30));
        cmbCliente.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Cliente) {
                    setText(((Cliente) v).getNombre() + "  ·  " + ((Cliente) v).getTelefono());
                }
                return this;
            }
        });

        for (LocalTime h = LocalTime.of(6, 0); ; h = h.plusMinutes(30)) {
            cmbHora.addItem(h);
            if (h.equals(LocalTime.of(23, 30))) {
                break;
            }
        }
        cmbHora.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                setText(v instanceof LocalTime ? FechaUtil.formatearHora((LocalTime) v) : "");
                return this;
            }
        });

        for (Integer minutos : reservas.getDuracionesPermitidas()) {
            cmbDuracion.addItem(minutos);
        }
        cmbDuracion.setSelectedItem(60);
        cmbDuracion.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                setText(v instanceof Integer ? FechaUtil.formatearDuracion(Duration.ofMinutes((Integer) v)) : "");
                return this;
            }
        });

        cmbTipo.addItem(CUALQUIERA);
        for (TipoCancha tipo : TipoCancha.values()) {
            cmbTipo.addItem(tipo.getDescripcion());
        }

        lstCanchas.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Cancha) {
                    Cancha c = (Cancha) v;
                    setText(c.getNombre() + "   ·   " + c.getTipo().getDescripcion()
                            + "   ·   " + c.getCapacidad() + " jugadores");
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Valores iniciales: hoy y la próxima media hora
        txtFecha.setText(FechaUtil.formatearFecha(LocalDate.now()));
        LocalTime ahora = LocalTime.now();
        LocalTime siguiente = ahora.withSecond(0).withNano(0).plusMinutes(30 - ahora.getMinute() % 30);
        if (ahora.getHour() < 6 || ahora.getHour() >= 23) {
            siguiente = LocalTime.of(8, 0);
        }
        cmbHora.setSelectedItem(siguiente);
    }

    private void conectarEventos() {
        txtBuscarCliente.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarClientes();
            }
        });
        btnNuevoCliente.addActionListener(e -> nuevoCliente());
        txtFecha.addActionListener(e -> actualizarTodo());       // Enter en la fecha
        cmbHora.addActionListener(e -> actualizarTodo());
        cmbDuracion.addActionListener(e -> actualizarTodo());
        cmbTipo.addActionListener(e -> actualizarTodo());
        for (String codigo : chkServicios.keySet()) {
            chkServicios.get(codigo).addActionListener(e -> {
                spnServicios.get(codigo).setEnabled(chkServicios.get(codigo).isSelected());
                actualizarPrecio();
            });
            spnServicios.get(codigo).addChangeListener(e -> actualizarPrecio());
        }
        btnCrear.addActionListener(e -> crear());
        btnCancelar.addActionListener(e -> dispose());
    }

    // =====================================================================
    //  LECTURA DE LOS CAMPOS (solo convierte texto → datos; no decide nada)
    // =====================================================================

    private LocalDateTime leerInicio() throws CanchaProException {
        LocalDate fecha = FechaUtil.leerFecha(txtFecha.getText());
        return LocalDateTime.of(fecha, (LocalTime) cmbHora.getSelectedItem());
    }

    private int leerDuracion() {
        return (Integer) cmbDuracion.getSelectedItem();
    }

    private TipoCancha leerTipo() {
        int i = cmbTipo.getSelectedIndex();
        return i <= 0 ? null : TipoCancha.values()[i - 1];
    }

    /** @return código de servicio → cantidad, solo de las casillas marcadas */
    private Map<String, Integer> leerServicios() {
        Map<String, Integer> elegidos = new LinkedHashMap<>();
        for (Map.Entry<String, JCheckBox> e : chkServicios.entrySet()) {
            if (e.getValue().isSelected()) {
                elegidos.put(e.getKey(), (Integer) spnServicios.get(e.getKey()).getValue());
            }
        }
        return elegidos;
    }

    // =====================================================================
    //  ACTUALIZACIONES EN VIVO (cada una pregunta al controlador)
    // =====================================================================

    private void filtrarClientes() {
        Object antes = cmbCliente.getSelectedItem();
        cmbCliente.removeAllItems();
        for (Cliente c : clientes.buscar(txtBuscarCliente.getText())) {
            cmbCliente.addItem(c);
        }
        if (antes != null) {
            cmbCliente.setSelectedItem(antes);
        }
    }

    /** Fecha, hora, duración o tipo cambiaron: buscar canchas libres y recotizar. */
    private void actualizarTodo() {
        buscarCanchas();
        actualizarPrecio();
    }

    private void buscarCanchas() {
        Cancha antes = lstCanchas.getSelectedValue();
        modeloCanchas.clear();
        try {
            List<Cancha> libres = reservas.buscarCanchasDisponibles(leerInicio().toLocalDate(),
                    leerInicio().toLocalTime(), leerDuracion(), leerTipo());
            for (Cancha c : libres) {
                modeloCanchas.addElement(c);
            }
            lblDisponibles.setText(libres.isEmpty()
                    ? "No hay canchas libres en ese horario. Pruebe otra hora."
                    : libres.size() + (libres.size() == 1 ? " cancha libre" : " canchas libres"));
            lblMensaje.setText(" ");
            if (antes != null && modeloCanchas.contains(antes)) {
                lstCanchas.setSelectedValue(antes, true);
            } else if (!libres.isEmpty()) {
                lstCanchas.setSelectedIndex(0);
            }
        } catch (CanchaProException ex) {
            lblDisponibles.setText(" ");
            lblMensaje.setText(ex.getMessage()); // ej. fecha inválida o en el pasado
        }
    }

    private void actualizarPrecio() {
        try {
            ReservaController.Cotizacion c = reservas.cotizar(leerInicio(), leerDuracion(), leerServicios());
            lblPrecioCancha.setText(MonedaUtil.formatear(c.getCancha()));
            lblPrecioServicios.setText(MonedaUtil.formatear(c.getServicios()));
            lblTotal.setText(MonedaUtil.formatear(c.getTotal()));
        } catch (CanchaProException ex) {
            lblPrecioCancha.setText("-");
            lblPrecioServicios.setText("-");
            lblTotal.setText("-");
        }
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    /** Abre el ClienteDialog (6.3) y deja seleccionado el cliente creado. */
    private void nuevoCliente() {
        ClienteDialog dlg = new ClienteDialog(this, clientes, null);
        dlg.setVisible(true);
        Cliente nuevo = dlg.getClienteGuardado();
        if (nuevo != null) {
            txtBuscarCliente.setText("");
            filtrarClientes();
            cmbCliente.setSelectedItem(nuevo);
        }
    }

    /** UNA llamada al controlador; él valida todo (cliente, cancha, choque, horario, pasado). */
    private void crear() {
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        Cancha cancha = lstCanchas.getSelectedValue();
        try {
            creada = reservas.crear(cliente == null ? "" : cliente.getCodigo(),
                    cancha == null ? "" : cancha.getCodigo(),
                    leerInicio(), leerDuracion(), leerServicios());
            dispose();
        } catch (CanchaProException ex) {
            lblMensaje.setText(ex.getMessage());
        }
    }

    // =====================================================================
    //  APOYO VISUAL
    // =====================================================================

    private static JLabel titulo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(Tema.FUENTE_NEGRITA);
        lbl.setForeground(Tema.color(ColorTema.PRIMARIO));
        return lbl;
    }

    private static JPanel fila(JComponent... componentes) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila.setOpaque(false);
        for (JComponent c : componentes) {
            fila.add(c);
        }
        return fila;
    }
}

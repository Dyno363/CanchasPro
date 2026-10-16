/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TarifaController;
import com.canchapro.models.canchas.CalculadoraTarifa;
import com.canchapro.models.canchas.Tarifa;
import com.canchapro.models.canchas.TarifaEspecial;
import com.canchapro.models.canchas.TarifaFranja;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import com.canchapro.views.components.RenderizadorPildora;
import com.canchapro.views.dialogs.DialogoFormulario;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 *
 * Módulo TARIFAS: precios por franja (normales) y por fecha (especiales),
 * con un SIMULADOR que muestra cómo se calcularía el precio de un horario.
 *
 * 
 *  ┌ Tarifas ───────────────────────────────────────────────────────────────┐
 *  │ 4 tarifas · 3 normales · 1 especial  [+ Normal] [+ Especial] [Editar] … │
 *  │ Código │ Tipo     │ Nombre                  │ Q/hora │ Prio │ Aplica    │
 *  │ T01    │ NORMAL   │ Lunes a viernes diurno  │ 150.00 │ 1    │ Lun… 8-17 │
 *  │ T04    │ ESPECIAL │ Día de Todos los Santos │ 275.00 │ 10   │ 01/11/2026│
 *  ├────────────────────────────────────────────────────────────────────────┤
 *  │ ✔ Todo el horario de atención tiene precio.                            │
 *  │ Simulador: [16/10/2026] [16:00▼] [120 min▼] [Calcular]                  │
 *  │   16:00-17:00 · diurno · 1 h × Q150 = Q150 / 17:00-18:00 · nocturno …   │
 *  └────────────────────────────────────────────────────────────────────────┘
 * 
 *
 * El cálculo NO se hace aquí: el simulador llama a {@code TarifaController.desglosar}
 * y {@code cotizar}, que usan la misma CalculadoraTarifa que las reservas reales.
 *
 * @author SANTANA
 */
public class TarifasPanel extends PanelListado<Tarifa>{
    
    private static final long serialVersionUID = 1L;
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final transient TarifaController tarifas;

    private final JButton btnEditar;
    private final JButton btnEliminar;

    // --- Pie: cobertura + simulador
    private final JLabel lblCobertura = new JLabel(" ");
    private final JTextField txtFechaSim = new JTextField(FechaUtil.formatearFecha(LocalDate.now()), 9);
    private final JComboBox<LocalTime> cmbHoraSim = CanchasPanel.combosDeHoras();
    private final JComboBox<Integer> cmbDuracionSim;
    private final JButton btnCalcular = new JButton("Calcular");
    private final JLabel lblResultado = new JLabel(" ");

    public TarifasPanel(SistemaController sistema) {
        super("Tarifas", "Precio por hora según el día y la hora (normales) o por fecha especial (feriados, eventos).",
                crearModelo(), new int[]{60, 90, 220, 90, 70, 330});
        this.tarifas = sistema.getTarifaController();
        this.cmbDuracionSim = new JComboBox<>(sistema.getReservaController().getDuracionesPermitidas()
                .toArray(new Integer[0]));

        agregarBoton("+ Tarifa normal", () -> mostrarFormularioFranja(null));
        agregarBoton("+ Tarifa especial", () -> mostrarFormularioEspecial(null));
        btnEditar = agregarBoton("Editar", this::editar);
        btnEliminar = agregarBoton("Eliminar", this::eliminar);

        tabla.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorPildora(
                v -> TarifaEspecial.TIPO.equals(v) ? "Especial" : "Normal",
                v -> Tema.color(TarifaEspecial.TIPO.equals(v) ? ColorTema.ADVERTENCIA : ColorTema.INFO)));
        tabla.getColumnModel().getColumn(3).setCellRenderer(renderizadorMoneda());
        setPie(crearPie());
        aplicarTema();
        recargar();
    }

    private static ModeloTablaLista<Tarifa> crearModelo() {
        return new ModeloTablaLista<>(
                new String[]{"Código", "Tipo", "Nombre", "Precio/hora", "Prioridad", "Cuándo aplica"},
                new Class<?>[]{String.class, String.class, String.class, Double.class, Integer.class, String.class},
                t -> new Object[]{t.getCodigo(), t.getTipoTarifa(), t.getNombre(), t.getPrecioHora(),
                    t.getPrioridad(), t.getDescripcionAplicacion()});
    }

    /** Debajo de la tabla: aviso de cobertura y el simulador de precios. */
    private JPanel crearPie() {
        cmbHoraSim.setSelectedItem(LocalTime.of(17, 0));
        cmbDuracionSim.setSelectedItem(120);
        btnCalcular.addActionListener(e -> simular());

        JPanel simulador = new JPanel(new FlowLayout(FlowLayout.LEFT, Tema.ESPACIO, 0));
        simulador.setOpaque(false);
        JLabel lblSim = new JLabel("Simulador de precio:");
        lblSim.setFont(Tema.FUENTE_NEGRITA);
        simulador.add(lblSim);
        simulador.add(new JLabel("Fecha"));
        simulador.add(txtFechaSim);
        simulador.add(new JLabel("Inicio"));
        simulador.add(cmbHoraSim);
        simulador.add(new JLabel("Minutos"));
        simulador.add(cmbDuracionSim);
        simulador.add(btnCalcular);

        JPanel pie = new JPanel();
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS)); // una fila debajo de otra, cada una de su alto
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, 0, 0, 0));
        for (JComponent c : new JComponent[]{lblCobertura, simulador, lblResultado}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            pie.add(c);
        }
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(pie, BorderLayout.CENTER);
        return contenedor;
    }

    // =====================================================================
    //  PLANTILLA (PanelListado)
    // =====================================================================

    @Override
    protected List<Tarifa> cargar() {
        setBotonesVisibles(tarifas.puedeAdministrar()); // el operador consulta y simula
        actualizarCobertura();
        return tarifas.listar();
    }

    @Override
    protected String textoConteo(List<Tarifa> lista) {
        int especiales = 0;
        for (Tarifa t : lista) {
            if (t instanceof TarifaEspecial) {
                especiales++;
            }
        }
        return lista.size() + " tarifas  ·  " + (lista.size() - especiales) + " normales  ·  "
                + especiales + " especiales";
    }

    @Override
    protected void alCambiarSeleccion(Tarifa t) {
        btnEditar.setEnabled(t != null);
        btnEliminar.setEnabled(t != null);
    }

    @Override
    protected void alDobleClic(Tarifa t) {
        if (tarifas.puedeAdministrar()) {
            editar();
        }
    }

    @Override
    public void aplicarTema() {
        super.aplicarTema();
        if (btnEliminar == null) {
            return; // todavía se está construyendo
        }
        Tema.botonPeligro(btnEliminar);
        Tema.botonSecundario(btnCalcular);
        Tema.campo(txtFechaSim);
        lblResultado.setFont(Tema.FUENTE_NEGRITA);
        lblResultado.setForeground(Tema.color(ColorTema.TEXTO));
        actualizarCobertura();
    }

    /** Pregunta al controlador si quedó alguna hora del horario de atención sin precio. */
    private void actualizarCobertura() {
        List<String> huecos = tarifas.getHuecosDeCobertura();
        lblCobertura.setFont(Tema.FUENTE_NEGRITA);
        if (huecos.isEmpty()) {
            lblCobertura.setText(" Todo el horario de atención tiene una tarifa normal.");
            lblCobertura.setForeground(Tema.color(ColorTema.EXITO));
        } else {
            lblCobertura.setText(" Horarios SIN tarifa (no se podrán reservar): " + String.join(" · ", huecos));
            lblCobertura.setForeground(Tema.color(ColorTema.ERROR));
        }
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    /** Polimorfismo: según la CLASE de la tarifa seleccionada se abre un formulario u otro. */
    private void editar() {
        Tarifa t = seleccionado();
        if (t instanceof TarifaFranja) {
            mostrarFormularioFranja((TarifaFranja) t);
        } else if (t instanceof TarifaEspecial) {
            mostrarFormularioEspecial((TarifaEspecial) t);
        }
    }

    private void mostrarFormularioFranja(TarifaFranja t) {
        JTextField txtNombre = new JTextField();
        JSpinner spnPrecio = spinnerPrecio(t == null ? 150 : t.getPrecioHora());
        JSpinner spnPrioridad = new JSpinner(new SpinnerNumberModel(t == null ? 1 : t.getPrioridad(), 1, 99, 1));
        JCheckBox[] chkDias = new JCheckBox[7];
        JPanel pnlDias = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pnlDias.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            chkDias[i] = new JCheckBox(DIAS[i]);
            chkDias[i].setOpaque(false);
            chkDias[i].setSelected(t != null && t.getDias().contains(DayOfWeek.of(i + 1)));
            pnlDias.add(chkDias[i]);
        }
        JComboBox<LocalTime> cmbInicio = CanchasPanel.combosDeHoras();
        JComboBox<LocalTime> cmbFin = CanchasPanel.combosDeHoras();
        if (t == null) {
            cmbInicio.setSelectedItem(LocalTime.of(8, 0));
            cmbFin.setSelectedItem(LocalTime.of(17, 0));
        } else {
            txtNombre.setText(t.getNombre());
            cmbInicio.setSelectedItem(t.getHoraInicio());
            cmbFin.setSelectedItem(t.getHoraFin());
        }

        DialogoFormulario f = new DialogoFormulario(this, t == null ? "Nueva tarifa normal" : "Editar " + t.getCodigo());
        f.fila("Nombre *", txtNombre)
         .fila("Precio por hora *", spnPrecio)
         .fila("Prioridad *", spnPrioridad)
         .fila("Días *", pnlDias)
         .fila("Desde *", cmbInicio)
         .fila("Hasta *", cmbFin)
         .ayuda("Si dos tarifas aplican a la misma hora, gana la de MAYOR prioridad.");

        boolean guardo = f.mostrar(() -> {
            Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
            for (int i = 0; i < 7; i++) {
                if (chkDias[i].isSelected()) {
                    dias.add(DayOfWeek.of(i + 1));
                }
            }
            double precio = (Double) spnPrecio.getValue();
            int prioridad = (Integer) spnPrioridad.getValue();
            LocalTime desde = (LocalTime) cmbInicio.getSelectedItem();
            LocalTime hasta = (LocalTime) cmbFin.getSelectedItem();
            if (t == null) {
                tarifas.crearFranja(txtNombre.getText(), precio, prioridad, dias, desde, hasta);
            } else {
                tarifas.actualizarFranja(t.getCodigo(), txtNombre.getText(), precio, prioridad, dias, desde, hasta);
            }
        });
        if (guardo) {
            recargar();
        }
    }

    private void mostrarFormularioEspecial(TarifaEspecial t) {
        JTextField txtNombre = new JTextField();
        JSpinner spnPrecio = spinnerPrecio(t == null ? 275 : t.getPrecioHora());
        JSpinner spnPrioridad = new JSpinner(new SpinnerNumberModel(t == null ? 10 : t.getPrioridad(), 1, 99, 1));
        JTextField txtFecha = new JTextField();
        JTextField txtMotivo = new JTextField();
        if (t != null) {
            txtNombre.setText(t.getNombre());
            txtFecha.setText(FechaUtil.formatearFecha(t.getFecha()));
            txtMotivo.setText(t.getMotivo());
        }

        DialogoFormulario f = new DialogoFormulario(this, t == null ? "Nueva tarifa especial" : "Editar " + t.getCodigo());
        f.fila("Nombre *", txtNombre)
         .fila("Precio por hora *", spnPrecio)
         .fila("Prioridad *", spnPrioridad)
         .fila("Fecha * (dd/MM/aaaa)", txtFecha)
         .fila("Motivo *", txtMotivo)
         .ayuda("Aplica todo ese día. Use una prioridad MAYOR que las normales para que gane.");

        boolean guardo = f.mostrar(() -> {
            LocalDate fecha = FechaUtil.leerFecha(txtFecha.getText()); // si el formato es malo, lanza el error
            double precio = (Double) spnPrecio.getValue();
            int prioridad = (Integer) spnPrioridad.getValue();
            if (t == null) {
                tarifas.crearEspecial(txtNombre.getText(), precio, prioridad, fecha, txtMotivo.getText());
            } else {
                tarifas.actualizarEspecial(t.getCodigo(), txtNombre.getText(), precio, prioridad, fecha,
                        txtMotivo.getText());
            }
        });
        if (guardo) {
            recargar();
        }
    }

    /** Elimina y avisa si quedaron horarios sin precio. Las reservas ya hechas conservan su precio. */
    private void eliminar() {
        Tarifa t = seleccionado();
        if (t == null || !confirmar("¿Eliminar la tarifa " + t.getCodigo() + " - " + t.getNombre() + "?\n"
                + "Las reservas ya creadas conservan el precio con el que se hicieron.")) {
            return;
        }
        try {
            List<String> huecos = tarifas.eliminar(t.getCodigo());
            recargar();
            if (!huecos.isEmpty()) {
                advertir("Quedaron horarios sin tarifa:\n" + String.join("\n", huecos));
            }
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** Simulador: el controlador desglosa el horario en tramos con su tarifa y calcula el total. */
    private void simular() {
        try {
            LocalDate fecha = FechaUtil.leerFecha(txtFechaSim.getText());
            LocalDateTime inicio = LocalDateTime.of(fecha, (LocalTime) cmbHoraSim.getSelectedItem());
            LocalDateTime fin = inicio.plusMinutes((Integer) cmbDuracionSim.getSelectedItem());
            StringBuilder sb = new StringBuilder("<html>");
            for (CalculadoraTarifa.Tramo tramo : tarifas.desglosar(inicio, fin)) {
                sb.append(FechaUtil.formatearRango(tramo.getInicio(), tramo.getFin())).append("  ·  ")
                  .append(tramo.getTarifa().getNombre()).append("  ·  ")
                  .append(tramo.getHoras()).append(" h × ")
                  .append(MonedaUtil.formatear(tramo.getTarifa().getPrecioHora())).append(" = ")
                  .append(MonedaUtil.formatear(tramo.getSubtotal())).append("<br>");
            }
            sb.append("<b>Total de la cancha: ").append(MonedaUtil.formatear(tarifas.cotizar(inicio, fin)))
              .append("</b></html>");
            lblResultado.setForeground(Tema.color(ColorTema.TEXTO));
            lblResultado.setText(sb.toString());
        } catch (CanchaProException ex) {
            lblResultado.setForeground(Tema.color(ColorTema.ERROR));
            lblResultado.setText(ex.getMessage());
        }
    }

    private static JSpinner spinnerPrecio(double inicial) {
        return new JSpinner(new SpinnerNumberModel(inicial, 1.0, 10_000.0, 5.0));
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.CanchaController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import com.canchapro.views.components.RenderizadorPildora;
import com.canchapro.views.dialogs.DialogoFormulario;
import java.time.LocalTime;
import java.util.List;
import java.util.StringJoiner;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
/**
 * Módulo CANCHAS: registrar, editar, cambiar estado y horario (solo administrador;
 * el operador solo consulta).
 *
 * 
 *  ┌ Canchas ───────────────────────────────────────────────────────────────┐
 *  │ 3 canchas · 1 en mantenimiento [+ Nueva] [Editar] [Cambiar estado] …    │
 *  │ Código │ Nombre       │ Tipo     │ Cap. │ Horario       │ Estado │ Res. │
 *  │ C01    │ La Bombonera │ Fútbol 5 │ 10   │ 08:00 - 23:00 │ DISP.  │ 3    │
 *  └────────────────────────────────────────────────────────────────────────┘
 * 
 *
 * Al cambiar el estado se avisa al Dashboard: el MAPA se repinta con el
 * color nuevo y la cancha deja (o vuelve a) aparecer en "Nueva reserva".
 */
public class CanchasPanel extends PanelListado<Cancha>{
    private static final long serialVersionUID = 1L;

    private final transient CanchaController canchas;
    private final transient Runnable alCambiarDatos;

    private final JButton btnEditar;
    private final JButton btnEstado;
    private final JButton btnEliminar;

    public CanchasPanel(SistemaController sistema, Runnable alCambiarDatos) {
        super("Canchas", "Las canchas del complejo, su capacidad, horario de atención y estado.",
                crearModelo(sistema.getCanchaController()), new int[]{70, 200, 100, 80, 130, 150, 120});
        this.canchas = sistema.getCanchaController();
        this.alCambiarDatos = alCambiarDatos == null ? () -> { } : alCambiarDatos;

        agregarBoton("+ Nueva cancha", this::nueva);
        btnEditar = agregarBoton("Editar", this::editar);
        btnEstado = agregarBoton("Cambiar estado", this::cambiarEstado);
        agregarBoton("Horario general", this::horarioGeneral);
        btnEliminar = agregarBoton("Eliminar", this::eliminar);

        tabla.getColumnModel().getColumn(5).setCellRenderer(new RenderizadorPildora(
                v -> ((EstadoCancha) v).getDescripcion(), v -> Tema.colorEstadoCancha((EstadoCancha) v)));
        aplicarTema();
        recargar();
    }

    private static ModeloTablaLista<Cancha> crearModelo(CanchaController canchas) {
        return new ModeloTablaLista<>(
                new String[]{"Código", "Nombre", "Tipo", "Capacidad", "Horario", "Estado", "Reservas activas"},
                new Class<?>[]{String.class, String.class, TipoCancha.class, Integer.class, String.class,
                    EstadoCancha.class, Integer.class},
                c -> new Object[]{c.getCodigo(), c.getNombre(), c.getTipo(), c.getCapacidad(),
                    c.getHorarioTexto(), c.getEstado(), reservasActivas(canchas, c)});
    }

    private static int reservasActivas(CanchaController canchas, Cancha c) {
        try {
            return canchas.getReservasActivas(c.getCodigo()).size();
        } catch (CanchaProException ex) {
            return 0;
        }
    }

    // =====================================================================
    //  PLANTILLA (PanelListado)
    // =====================================================================

    @Override
    protected List<Cancha> cargar() {
        setBotonesVisibles(canchas.puedeAdministrar()); // el operador solo consulta
        return canchas.listar();
    }

    @Override
    protected String textoConteo(List<Cancha> lista) {
        int mantenimiento = 0;
        for (Cancha c : lista) {
            if (c.getEstado() == EstadoCancha.MANTENIMIENTO) {
                mantenimiento++;
            }
        }
        return lista.size() + " canchas  ·  " + mantenimiento + " en mantenimiento"
                + (canchas.puedeAdministrar() ? "" : "  ·  (solo consulta)");
    }

    @Override
    protected void alCambiarSeleccion(Cancha c) {
        btnEditar.setEnabled(c != null);
        btnEstado.setEnabled(c != null);
        btnEliminar.setEnabled(c != null);
    }

    @Override
    protected void alDobleClic(Cancha c) {
        if (canchas.puedeAdministrar()) {
            editar();
        }
    }

    @Override
    public void aplicarTema() {
        super.aplicarTema();
        if (btnEliminar != null) {
            Tema.botonPeligro(btnEliminar);
        }
    }

    // =====================================================================
    //  ACCIONES (cada una = un formulario + una llamada al controlador)
    // =====================================================================

    private void nueva() {
        mostrarFormulario(null);
    }

    private void editar() {
        Cancha c = seleccionado();
        if (c != null) {
            mostrarFormulario(c);
        }
    }

    /** Un mismo formulario para crear (c == null) o editar. */
    private void mostrarFormulario(Cancha c) {
        JTextField txtNombre = new JTextField();
        JComboBox<TipoCancha> cmbTipo = new JComboBox<>(canchas.getTipos());
        JSpinner spnCapacidad = new JSpinner(new SpinnerNumberModel(10, 1, 30, 1));
        JComboBox<LocalTime> cmbApertura = combosDeHoras();
        JComboBox<LocalTime> cmbCierre = combosDeHoras();
        if (c == null) {
            cmbApertura.setSelectedItem(LocalTime.of(8, 0));
            cmbCierre.setSelectedItem(LocalTime.of(23, 0));
        } else {
            txtNombre.setText(c.getNombre());
            cmbTipo.setSelectedItem(c.getTipo());
            spnCapacidad.setValue(c.getCapacidad());
            cmbApertura.setSelectedItem(c.getHoraApertura());
            cmbCierre.setSelectedItem(c.getHoraCierre());
        }
        // Al cambiar el tipo se sugiere su capacidad máxima (Fútbol 5 → 10, Fútbol 7 → 14)
        cmbTipo.addActionListener(e ->
                spnCapacidad.setValue(((TipoCancha) cmbTipo.getSelectedItem()).getJugadoresMaximos()));

        DialogoFormulario f = new DialogoFormulario(this, c == null ? "Nueva cancha" : "Editar " + c.getCodigo());
        f.fila("Nombre *", txtNombre)
         .fila("Tipo *", cmbTipo)
         .fila("Capacidad *", spnCapacidad)
         .fila("Abre a las *", cmbApertura)
         .fila("Cierra a las *", cmbCierre)
         .ayuda(c == null ? "La cancha nueva queda DISPONIBLE." : "Si el nuevo horario deja reservas fuera, no se permite.");

        Cancha[] resultado = new Cancha[1];
        boolean guardo = f.mostrar(() -> {
            TipoCancha tipo = (TipoCancha) cmbTipo.getSelectedItem();
            int capacidad = (Integer) spnCapacidad.getValue();
            LocalTime abre = (LocalTime) cmbApertura.getSelectedItem();
            LocalTime cierra = (LocalTime) cmbCierre.getSelectedItem();
            resultado[0] = c == null
                    ? canchas.crear(txtNombre.getText(), tipo, capacidad, abre, cierra)
                    : canchas.actualizar(c.getCodigo(), txtNombre.getText(), tipo, capacidad, abre, cierra);
        });
        if (guardo) {
            recargar();
            seleccionar(resultado[0]::equals);
            alCambiarDatos.run(); // el mapa dibuja la cancha nueva o el nombre nuevo
        }
    }

    /**
     * Cambia el estado. Nunca se bloquea (una cancha puede dañarse en cualquier
     * momento), pero si hay reservas activas el controlador las devuelve para avisar.
     */
    private void cambiarEstado() {
        Cancha c = seleccionado();
        if (c == null) {
            return;
        }
        JComboBox<EstadoCancha> cmbEstado = new JComboBox<>(canchas.getEstados());
        cmbEstado.setSelectedItem(c.getEstado());
        int r = JOptionPane.showConfirmDialog(this, cmbEstado, "Estado de " + c.getNombre(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.OK_OPTION || cmbEstado.getSelectedItem() == c.getEstado()) {
            return;
        }
        try {
            List<Reserva> afectadas = canchas.cambiarEstado(c.getCodigo(), (EstadoCancha) cmbEstado.getSelectedItem());
            recargar();
            alCambiarDatos.run(); // el mapa cambia de color
            if (!afectadas.isEmpty()) {
                StringJoiner codigos = new StringJoiner(", ");
                for (Reserva a : afectadas) {
                    codigos.add(a.getCodigo());
                }
                advertir("La cancha quedó " + c.getEstado().getDescripcion().toLowerCase(java.util.Locale.ROOT)
                        + ", pero tiene " + afectadas.size() + " reserva(s) activa(s): " + codigos
                        + ".\nAvise a los clientes y reprograme o cancele esas reservas.");
            }
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** El mismo horario para TODAS las canchas ("todo o nada" en el controlador). */
    private void horarioGeneral() {
        JComboBox<LocalTime> cmbApertura = combosDeHoras();
        JComboBox<LocalTime> cmbCierre = combosDeHoras();
        cmbApertura.setSelectedItem(LocalTime.of(8, 0));
        cmbCierre.setSelectedItem(LocalTime.of(23, 0));
        DialogoFormulario f = new DialogoFormulario(this, "Horario de atención general");
        f.fila("Abre a las *", cmbApertura)
         .fila("Cierra a las *", cmbCierre)
         .ayuda("Se aplica a todas las canchas. Si alguna reserva queda fuera, no se cambia ninguna.");
        int[] cambiadas = new int[1];
        if (f.mostrar(() -> cambiadas[0] = canchas.cambiarHorarioGeneral(
                (LocalTime) cmbApertura.getSelectedItem(), (LocalTime) cmbCierre.getSelectedItem()))) {
            recargar();
            alCambiarDatos.run();
            informar("Horario actualizado en " + cambiadas[0] + " canchas.");
        }
    }

    private void eliminar() {
        Cancha c = seleccionado();
        if (c == null || !confirmar("¿Eliminar la cancha " + c + "?\nSolo se permite si nunca tuvo reservas;"
                + " si tiene historial, póngala INACTIVA.")) {
            return;
        }
        try {
            canchas.eliminar(c.getCodigo());
            recargar();
            alCambiarDatos.run();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    /** Horas cada 30 minutos de 05:00 a 23:30 (se elige de una lista: no hay que escribir ni validar formato). */
    static JComboBox<LocalTime> combosDeHoras() {
        JComboBox<LocalTime> combo = new JComboBox<>();
        // Se cuenta por pasos (no "hasta 23:30") porque 23:30 + 30 min da 00:00 y el ciclo nunca terminaría
        for (int paso = 0; paso <= 37; paso++) {
            combo.addItem(LocalTime.of(5, 0).plusMinutes(30L * paso));
        }
        return combo;
    }

}

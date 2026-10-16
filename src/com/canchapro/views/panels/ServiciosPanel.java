/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.ServicioController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.utils.Tema;
import com.canchapro.views.components.ModeloTablaLista;
import com.canchapro.views.components.RenderizadorPildora;
import com.canchapro.views.dialogs.DialogoFormulario;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
/**
 * Módulo SERVICIOS ADICIONALES: balones, chalecos, árbitro, iluminación…
 *
 * 
 *  ┌ Servicios adicionales ──────────────────────────────────────────────┐
 *  │ 5 servicios · 5 activos   [+ Nuevo] [Editar] [Desactivar] [Eliminar] │
 *  │ Código │ Nombre            │ Precio │ Se cobra    │ Estado            │
 *  │ S01    │ Balón profesional │ Q25.00 │ Por reserva │ ACTIVO            │
 *  │ S03    │ Árbitro           │ Q75.00 │ Por hora    │ ACTIVO            │
 *  │ ⓘ Cambiar un precio no afecta las reservas ya hechas.                 │
 *  └──────────────────────────────────────────────────────────────────────┘
 * 
 *
 * Un servicio INACTIVO deja de ofrecerse en "Nueva reserva", pero sigue
 * apareciendo en las reservas viejas que lo usaron.
 */
public class ServiciosPanel extends PanelListado<ServicioAdicional>{
    private static final long serialVersionUID = 1L;

    private final transient ServicioController servicios;

    private final JButton btnEditar;
    private final JButton btnEstado;
    private final JButton btnEliminar;
    private final JLabel lblNota = new JLabel("ⓘ Cambiar un precio NO cambia las reservas ya hechas: cada "
            + "reserva guardó el precio del día en que se agregó el servicio.");

    public ServiciosPanel(SistemaController sistema) {
        super("Servicios adicionales", "Lo que el cliente puede agregar a su reserva: balones, chalecos, árbitro…",
                crearModelo(), new int[]{70, 240, 110, 130, 120});
        this.servicios = sistema.getServicioController();

        agregarBoton("+ Nuevo servicio", () -> mostrarFormulario(null));
        btnEditar = agregarBoton("Editar", this::editar);
        btnEstado = agregarBoton("Desactivar", this::cambiarEstado);
        btnEliminar = agregarBoton("Eliminar", this::eliminar);

        tabla.getColumnModel().getColumn(2).setCellRenderer(renderizadorMoneda());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new RenderizadorPildora(
                v -> Boolean.TRUE.equals(v) ? "Activo" : "Inactivo",
                v -> Tema.color(Boolean.TRUE.equals(v) ? ColorTema.EXITO : ColorTema.TEXTO_SECUNDARIO)));
        setPie(lblNota);
        aplicarTema();
        recargar();
    }

    private static ModeloTablaLista<ServicioAdicional> crearModelo() {
        return new ModeloTablaLista<>(
                new String[]{"Código", "Nombre", "Precio", "Se cobra", "Estado"},
                new Class<?>[]{String.class, String.class, Double.class, String.class, Boolean.class},
                s -> new Object[]{s.getCodigo(), s.getNombre(), s.getPrecio(), s.getModalidadTexto(), s.isActivo()});
    }

    // =====================================================================
    //  PLANTILLA (PanelListado)
    // =====================================================================

    @Override
    protected List<ServicioAdicional> cargar() {
        setBotonesVisibles(servicios.puedeAdministrar());
        return servicios.listar();
    }

    @Override
    protected String textoConteo(List<ServicioAdicional> lista) {
        return lista.size() + " servicios  ·  " + servicios.listarActivos().size() + " activos";
    }

    /** "Desactivar" para uno activo, "Activar" para uno inactivo. */
    @Override
    protected void alCambiarSeleccion(ServicioAdicional s) {
        btnEditar.setEnabled(s != null);
        btnEstado.setEnabled(s != null);
        btnEliminar.setEnabled(s != null);
        btnEstado.setText(s != null && !s.isActivo() ? "Activar" : "Desactivar");
    }

    @Override
    protected void alDobleClic(ServicioAdicional s) {
        if (servicios.puedeAdministrar()) {
            editar();
        }
    }

    @Override
    public void aplicarTema() {
        super.aplicarTema();
        if (btnEliminar != null) {
            Tema.botonPeligro(btnEliminar);
            Tema.textoSecundario(lblNota);
        }
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    private void editar() {
        ServicioAdicional s = seleccionado();
        if (s != null) {
            mostrarFormulario(s);
        }
    }

    private void mostrarFormulario(ServicioAdicional s) {
        JTextField txtNombre = new JTextField(s == null ? "" : s.getNombre());
        JSpinner spnPrecio = new JSpinner(new SpinnerNumberModel(s == null ? 25.0 : s.getPrecio(), 0.0, 5_000.0, 5.0));
        JCheckBox chkPorHora = new JCheckBox("Se multiplica por las horas de la reserva (ej. árbitro)");
        chkPorHora.setOpaque(false);
        chkPorHora.setSelected(s != null && s.isCobroPorHora());

        DialogoFormulario f = new DialogoFormulario(this, s == null ? "Nuevo servicio" : "Editar " + s.getCodigo());
        f.fila("Nombre *", txtNombre)
         .fila("Precio *", spnPrecio)
         .fila("Cobro por hora", chkPorHora)
         .ayuda("Sin marcar = precio fijo por reserva (ej. balón).");

        ServicioAdicional[] resultado = new ServicioAdicional[1];
        boolean guardo = f.mostrar(() -> {
            double precio = (Double) spnPrecio.getValue();
            resultado[0] = s == null
                    ? servicios.crear(txtNombre.getText(), precio, chkPorHora.isSelected())
                    : servicios.actualizar(s.getCodigo(), txtNombre.getText(), precio, chkPorHora.isSelected());
        });
        if (guardo) {
            recargar();
            seleccionar(resultado[0]::equals);
        }
    }

    private void cambiarEstado() {
        ServicioAdicional s = seleccionado();
        if (s == null) {
            return;
        }
        boolean activar = !s.isActivo();
        if (!confirmar(activar ? "¿Volver a ofrecer \"" + s.getNombre() + "\" en las reservas?"
                : "¿Dejar de ofrecer \"" + s.getNombre() + "\"? Las reservas que ya lo tienen no cambian.")) {
            return;
        }
        try {
            servicios.cambiarEstado(s.getCodigo(), activar);
            recargar();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }

    private void eliminar() {
        ServicioAdicional s = seleccionado();
        if (s == null || !confirmar("¿Eliminar \"" + s.getNombre() + "\"?\nSolo se permite si nunca se usó en "
                + "una reserva; si ya se usó, desactívelo.")) {
            return;
        }
        try {
            servicios.eliminar(s.getCodigo());
            recargar();
        } catch (CanchaProException ex) {
            error(ex);
        }
    }
    
}

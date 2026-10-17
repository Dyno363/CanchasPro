/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.dialogs;
import com.canchapro.controllers.TiendaController;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.tienda.Producto;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Tema;
import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 ** Formularios de PRODUCTOS de la Tienda/Cafetería (construidos por código).
 *
 * 
 *  ┌ Nuevo producto ─────────────────────────┐   ┌ Reponer: Agua pura 600 ml ──┐
 *  │ Código          P007                    │   │ Existencia actual   44      │
 *  │ Nombre *        [___________________]   │   │ Unidades que entran [ 24 ]  │
 *  │ Categoría *     [Bebida          ▼]     │   │ Quedarán            68 ✔    │
 *  │ Precio *        [  5.00 ]               │   │        [Cancelar] [Guardar] │
 *  │ Existencia *    [   24  ]  ← solo nuevo │   └─────────────────────────────┘
 *  │ Stock mínimo *  [   10  ]               │
 *  │ ✔ Stock suficiente                      │   (vista previa en vivo)
 *  │                  [Cancelar] [Guardar]   │
 *  └─────────────────────────────────────────┘
 * 
 *
 * Usa el {@link DialogoFormulario} de la 7.3: aquí solo se arman los campos
 * y se dice QUÉ método del {@link TiendaController} llamar. Las reglas (nombre
 * único, precio &gt; 0, existencia no negativa) las valida el modelo.
 *
 * La EXISTENCIA de un producto ya registrado no se edita a mano: solo cambia
 * con "Reponer" (entra mercadería) o con una venta (sale). Así el inventario
 * siempre se puede explicar.
 * @author SANTANA
 */
public class ProductoDialog {
    private ProductoDialog() {
        // solo métodos estáticos: no se crean objetos de esta clase
    }

    /**
     * Crear (aEditar == null) o editar un producto.
     *
     * @return el producto guardado, o {@code null} si se canceló
     */
    public static Producto mostrar(Component origen, TiendaController tienda, Producto aEditar) {
        boolean nuevo = aEditar == null;

        JLabel lblCodigo = new JLabel(nuevo ? "(se asigna al guardar)" : aEditar.getCodigo());
        lblCodigo.setFont(Tema.FUENTE_NEGRITA);
        lblCodigo.setForeground(Tema.color(ColorTema.PRIMARIO));
        JTextField txtNombre = new JTextField(nuevo ? "" : aEditar.getNombre());
        JComboBox<CategoriaProducto> cmbCategoria = new JComboBox<>(tienda.getCategorias());
        JSpinner spnPrecio = new JSpinner(new SpinnerNumberModel(nuevo ? 5.0 : aEditar.getPrecio(), 0.5, 5_000.0, 0.5));
        JSpinner spnExistencia = new JSpinner(new SpinnerNumberModel(nuevo ? 12 : aEditar.getExistencia(), 0, 10_000, 1));
        JSpinner spnMinimo = new JSpinner(new SpinnerNumberModel(nuevo ? 5 : aEditar.getStockMinimo(), 0, 1_000, 1));
        JLabel lblAviso = new JLabel(" ");
        if (!nuevo) {
            cmbCategoria.setSelectedItem(aEditar.getCategoria());
            spnExistencia.setEnabled(false); // se cambia con "Reponer" o con ventas, no a mano
        }

        // Vista previa en vivo: ¿cómo queda el inventario frente al mínimo?
        Runnable actualizarAviso = () -> avisoStock(lblAviso,
                (Integer) spnExistencia.getValue(), (Integer) spnMinimo.getValue());
        spnExistencia.addChangeListener(e -> actualizarAviso.run());
        spnMinimo.addChangeListener(e -> actualizarAviso.run());
        actualizarAviso.run();

        DialogoFormulario f = new DialogoFormulario(origen, nuevo ? "Nuevo producto" : "Editar " + aEditar.getCodigo());
        f.fila("Código", lblCodigo)
         .fila("Nombre *", txtNombre)
         .fila("Categoría *", cmbCategoria)
         .fila("Precio (Q) *", spnPrecio)
         .fila(nuevo ? "Existencia inicial *" : "Existencia actual", spnExistencia)
         .fila("Stock mínimo *", spnMinimo)
         .fila("", lblAviso)
         .ayuda(nuevo ? "El producto nuevo queda ACTIVO y se puede vender de inmediato."
                 : "Para sumar unidades use \"Reponer\". Cambiar el precio no altera ventas pasadas.");

        Producto[] resultado = new Producto[1];
        boolean guardo = f.mostrar(() -> {
            CategoriaProducto categoria = (CategoriaProducto) cmbCategoria.getSelectedItem();
            double precio = (Double) spnPrecio.getValue();
            int minimo = (Integer) spnMinimo.getValue();
            resultado[0] = nuevo
                    ? tienda.crearProducto(txtNombre.getText(), categoria, precio,
                            (Integer) spnExistencia.getValue(), minimo)
                    : tienda.actualizarProducto(aEditar.getCodigo(), txtNombre.getText(), categoria, precio, minimo);
        });
        return guardo ? resultado[0] : null;
    }

    /**
     * Entrada de mercadería: suma unidades a la existencia.
     *
     * @return el producto con su existencia nueva, o {@code null} si se canceló
     */
    public static Producto reponer(Component origen, TiendaController tienda, Producto producto) {
        JLabel lblActual = new JLabel(String.valueOf(producto.getExistencia()) + " unidades");
        lblActual.setFont(Tema.FUENTE_NEGRITA);
        JSpinner spnCantidad = new JSpinner(new SpinnerNumberModel(12, 1, 10_000, 1));
        JLabel lblQuedaran = new JLabel(" ");
        Runnable vistaPrevia = () -> avisoStock(lblQuedaran,
                producto.getExistencia() + (Integer) spnCantidad.getValue(), producto.getStockMinimo());
        spnCantidad.addChangeListener(e -> vistaPrevia.run());
        vistaPrevia.run();

        DialogoFormulario f = new DialogoFormulario(origen, "Reponer: " + producto.getNombre());
        f.fila("Existencia actual", lblActual)
         .fila("Unidades que entran *", spnCantidad)
         .fila("Quedarán", lblQuedaran)
         .ayuda("Stock mínimo de este producto: " + producto.getStockMinimo() + " · precio "
                 + MonedaUtil.formatear(producto.getPrecio()));

        Producto[] resultado = new Producto[1];
        boolean guardo = f.mostrar(() ->
                resultado[0] = tienda.reponerStock(producto.getCodigo(), (Integer) spnCantidad.getValue()));
        return guardo ? resultado[0] : null;
    }

    /** Texto y color del aviso: agotado (rojo), bajo el mínimo (amarillo) o suficiente (verde). */
    private static void avisoStock(JLabel etiqueta, int existencia, int minimo) {
        etiqueta.setFont(Tema.FUENTE_NEGRITA);
        if (existencia == 0) {
            etiqueta.setText(" " + existencia + " unidades: AGOTADO, no aparecerá en el POS");
            etiqueta.setForeground(Tema.color(ColorTema.ERROR));
        } else if (existencia <= minimo) {
            etiqueta.setText(" " + existencia + " unidades: en o bajo el mínimo (" + minimo + "), conviene reponer");
            etiqueta.setForeground(Tema.oscurecer(Tema.color(ColorTema.ADVERTENCIA), 0.3));
        } else {
            etiqueta.setText(" " + existencia + " unidades: stock suficiente");
            etiqueta.setForeground(Tema.color(ColorTema.EXITO));
        }
    }
}

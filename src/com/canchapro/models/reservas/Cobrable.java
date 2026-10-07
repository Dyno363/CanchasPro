/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.utils.MonedaUtil;
import java.util.Collection;
/**
 *
 * @author SANTANA
 */
/**
 * Contrato de todo lo que suma dinero a una cuenta.
 *
 * Lo implementan clases que no tienen nada más en común:
 * 
 *   {@link ServicioReserva}: un balón o un árbitro agregado a una reserva.
 *   DetalleVenta: un producto vendido en la tienda.
 *
 *
 * Gracias a esta interfaz, un comprobante puede recorrer una sola
 * {@code List<Cobrable>} con servicios y productos mezclados, y mostrar
 * cada línea sin saber de qué tipo es (polimorfismo por interfaz).
 */
public interface Cobrable {

    /**
     * Texto que describe la línea en un comprobante.
     * Ej.: "Árbitro x1 (2 h)" o "Agua pura 600 ml x4".
     */
    String getConcepto();

    /**
     * Monto total de la línea en quetzales, ya redondeado a centavos.
     */
    double getSubtotal();

    /**
     * Suma los subtotales de cualquier colección de objetos cobrables.
     *
     * Es un método {@code static} de la interfaz: se llama como
     * {@code Cobrable.sumarSubtotales(lista)}. El comodín
     * {@code ? extends Cobrable} permite pasar una {@code List<ServicioReserva>}
     * o una {@code List<DetalleVenta>} sin convertirlas.
     *
     * @param items líneas a sumar (puede ser vacía)
     * @return total redondeado a centavos
     */
    static double sumarSubtotales(Collection<? extends Cobrable> items) {
        double total = 0;
        for (Cobrable item : items) {
            total += item.getSubtotal();
        }
        return MonedaUtil.redondear(total);
    }
}
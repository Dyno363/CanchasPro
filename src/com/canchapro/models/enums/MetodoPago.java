/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.canchapro.models.enums;

/**
 *
 * @author SANTANA
 */
/**
 * Formas de pago aceptadas en el complejo.
 *
 * Se usa tanto en los pagos de reservas como en las ventas de la tienda,
 * por eso vive en un paquete común y no dentro de reservas o tienda.
 */
public enum MetodoPago {

    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    DEPOSITO("Depósito");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

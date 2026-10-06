/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.exceptions;

/**
 *
 * @author SANTANA
 */
/**
 * Se lanza cuando se intenta vender una cantidad mayor a la existencia
 * disponible de un producto.
 *
 * Prueba del PDF: #14 (venta mayor a existencia).
 */
public class StockInsuficienteException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}

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
 * Se lanza cuando un pago no cumple las reglas: monto negativo o cero,
 * monto mayor al saldo pendiente, o pago a una reserva cancelada.
 *
 * Prueba del PDF: #8 (pago mayor al saldo).
 */
public class PagoInvalidoException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
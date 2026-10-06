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
 * Se lanza cuando se intenta un cambio de estado de reserva que no tiene
 * sentido, por ejemplo pasar de FINALIZADA a PENDIENTE.
 *
 * Las transiciones válidas están definidas en
 * {@code EstadoReserva.puedeTransicionarA()}.
 */
public class TransicionEstadoInvalidaException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}

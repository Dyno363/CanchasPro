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
 * Se lanza cuando se busca un objeto por su código y no existe en memoria:
 * cliente, cancha, reserva, servicio, producto o usuario.
 *
 * Ejemplo: al cargar reservas.csv, una reserva apunta al cliente "CL0099"
 * que no existe en clientes.csv.
 */
public class EntidadNoEncontradaException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}

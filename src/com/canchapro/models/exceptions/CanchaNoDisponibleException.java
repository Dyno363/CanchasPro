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
 * Se lanza cuando se intenta reservar una cancha que no puede usarse:
 * en mantenimiento, inactiva o fuera de su horario de apertura/cierre.
 *
 * Prueba del PDF: #5 (reserva en cancha en mantenimiento).
 */
public class CanchaNoDisponibleException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public CanchaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

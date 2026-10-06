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
 * Se lanza cuando una reserva nueva se traslapa con otra existente
 * en la misma cancha.
 *
 * Regla: existe conflicto si inicioNuevo &lt; finExistente
 * Y finNuevo &gt; inicioExistente.
 *
 * Pruebas del PDF: #3 (traslape parcial) y #4 (reserva contenida en otra).
 */
public class ConflictoHorarioException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public ConflictoHorarioException(String mensaje) {
        super(mensaje);
    }
}
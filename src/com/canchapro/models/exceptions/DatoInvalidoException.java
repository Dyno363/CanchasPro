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
 * Se lanza cuando un dato tiene formato incorrecto: fecha u hora inválida,
 * monto no numérico, campo obligatorio vacío o línea CSV corrupta.
 *
 * Tiene un segundo constructor que conserva la excepción original
 * (por ejemplo {@code DateTimeParseException}) para no perder el detalle técnico.
 *
 * Prueba del PDF: #11 (archivo con registro incorrecto → error controlado).
 */
public class DatoInvalidoException extends CanchaProException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public DatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

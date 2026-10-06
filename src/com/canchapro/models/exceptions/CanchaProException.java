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
 * Excepción base de todas las reglas de negocio de CanchaPro.
 *
 * Es una excepción verificada (extiende {@link Exception}), por lo que
 * el compilador obliga a quien llame un método que la lanza a manejarla con
 * try/catch o a declararla con throws. Así ningún error de negocio se pierde.
 *
 * Las vistas atrapan esta clase general y muestran {@code getMessage()} al
 * operador, sin necesidad de conocer cada excepción específica.
 */
public class CanchaProException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje descripción del error, pensada para mostrarse al usuario
     */
    public CanchaProException(String mensaje) {
        super(mensaje);
    }

    /**
     * @param mensaje descripción del error
     * @param causa   excepción original que provocó este error (ej. NumberFormatException)
     */
    public CanchaProException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

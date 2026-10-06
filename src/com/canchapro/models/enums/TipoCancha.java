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
 * Tipos de cancha que maneja el complejo.
 *
 * Cada tipo conoce su cantidad máxima de jugadores. Así, si mañana el
 * complejo agrega Fútbol 11, solo se añade una constante aquí y el resto
 * del sistema (filtros de disponibilidad, combos) la reconoce sola.
 */
public enum TipoCancha {

    FUTBOL_5("Fútbol 5", 10),
    FUTBOL_7("Fútbol 7", 14);

    private final String descripcion;
    private final int jugadoresMaximos;

    TipoCancha(String descripcion, int jugadoresMaximos) {
        this.descripcion = descripcion;
        this.jugadoresMaximos = jugadoresMaximos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getJugadoresMaximos() {
        return jugadoresMaximos;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

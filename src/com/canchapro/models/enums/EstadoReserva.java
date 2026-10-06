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
 * Ciclo de vida de una reserva (máquina de estados).
 *
 *
 *   PENDIENTE  ──► CONFIRMADA ──► EN_CURSO ──► FINALIZADA
 *               │
 *               └-- ► CANCELADA
 * 
 *
 * FINALIZADA y CANCELADA son estados terminales: desde ahí no se puede
 * pasar a ningún otro. Esto cumple la regla del PDF: "una reserva
 * finalizada no debe volver a PENDIENTE".
 */
public enum EstadoReserva {

    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String descripcion;

    EstadoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Indica si es válido pasar del estado actual ({@code this}) al estado destino.
     *
     * @param destino estado al que se quiere cambiar
     * @return true si la transición está permitida
     */
    public boolean puedeTransicionarA(EstadoReserva destino) {
        if (destino == null) {
            return false;
        }
        switch (this) {
            case PENDIENTE:
                return destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA:
                return destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO:
                return destino == FINALIZADA;
            default:
                // FINALIZADA y CANCELADA: no permiten ningún cambio.
                return false;
        }
    }

    /**
     * @return true si desde este estado ya no se permite ningún cambio
     */
    public boolean esTerminal() {
        return this == FINALIZADA || this == CANCELADA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

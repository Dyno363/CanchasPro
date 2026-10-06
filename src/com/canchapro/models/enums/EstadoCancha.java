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
 * Estados posibles de una cancha física.
 *
 * Importante: "EN CURSO" NO es un estado de la cancha. Ese dato se calcula
 * a partir de las reservas (ver MapaController). Aquí solo se guarda lo que
 * el administrador decide: si la cancha se puede usar o no.
 *
 * En los CSV se guarda con {@code name()} (ej. "MANTENIMIENTO");
 * en la interfaz se muestra con {@code toString()} (ej. "En mantenimiento").
 */
public enum EstadoCancha {

    DISPONIBLE("Disponible"),
    MANTENIMIENTO("En mantenimiento"),
    INACTIVA("Inactiva");

    /** Texto amigable para mostrar en tablas y combos. */
    private final String descripcion;

    EstadoCancha(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Regla del PDF: una cancha inactiva o en mantenimiento nunca debe
     * aparecer como disponible para reservar.
     *
     * @return true solo si el estado es DISPONIBLE
     */
    public boolean permiteReservas() {
        return this == DISPONIBLE;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

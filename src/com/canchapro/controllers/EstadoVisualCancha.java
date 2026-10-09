/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.canchas.Cancha;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Estado que se PINTA en el mapa dinámico de canchas.
 *
 * No es lo mismo que {@link com.canchapro.models.enums.EstadoCancha}:
 * 
 *   {@code EstadoCancha} es un dato que el administrador fija y se guarda en
 *       canchas.csv (Disponible, Mantenimiento, Inactiva).
 *   {@code EstadoVisualCancha} se CALCULA en el momento, combinando el estado
 *       de la cancha con las reservas: si alguien está jugando ahora, la cancha
 *       está "En curso". Nunca se guarda en archivos.
 *
 *
 * Por eso vive en {@code controllers} y no en {@code models}: es información
 * preparada para la vista. Los COLORES no están aquí, sino en {@code utils.Tema},
 * porque el color es asunto de la interfaz.
 */

public enum EstadoVisualCancha {
     DISPONIBLE("Disponible"),
    EN_CURSO("En curso"),
    MANTENIMIENTO("En mantenimiento"),
    INACTIVA("Inactiva");

    private final String descripcion;

    EstadoVisualCancha(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Decide el estado visual de una cancha.
     *
     * Prioridad: si hay una reserva jugándose AHORA, se muestra "En curso"
     * aunque la cancha se haya marcado en mantenimiento a mitad del partido:
     * hay gente en la cancha y el POS debe seguir disponible para cobrarles.
     * Si no hay partido, manda el estado administrativo de la cancha.
     *
     * @param cancha            cancha a evaluar
     * @param hayReservaEnCurso true si una reserva activa cubre el momento actual
     * @return estado a pintar en el mapa
     */
    public static EstadoVisualCancha calcular(Cancha cancha, boolean hayReservaEnCurso) {
        Objects.requireNonNull(cancha, "La cancha es obligatoria.");
        if (hayReservaEnCurso) {
            return EN_CURSO;
        }
        switch (cancha.getEstado()) {
            case MANTENIMIENTO:
                return MANTENIMIENTO;
            case INACTIVA:
                return INACTIVA;
            default:
                return DISPONIBLE;
        }
    }

    /** Regla del enunciado: el POS solo se abre desde una cancha "En curso". */
    public boolean permiteAbrirPos() {
        return this == EN_CURSO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

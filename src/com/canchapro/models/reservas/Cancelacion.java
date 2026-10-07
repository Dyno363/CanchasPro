/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDateTime;

/**
 *
 * @author SANTANA
 */
/**
 * Registro de por qué, cuándo y quién canceló una reserva.
 *
 * Regla del PDF: una reserva cancelada NO se elimina. Cambia su estado a
 * CANCELADA y guarda este objeto con la fecha, el motivo y el responsable,
 * para conservar el historial.
 *
 * Vive dentro de su reserva (composición, multiplicidad 0..1): solo las
 * reservas canceladas tienen una Cancelacion. Es inmutable.
 */
public final class Cancelacion {

    private final LocalDateTime fechaHora;
    private final String motivo;
    private final Usuario responsable;

    /**
     * @param fechaHora   momento en que se canceló
     * @param motivo      razón de la cancelación (obligatoria)
     * @param responsable operador que la canceló
     * @throws DatoInvalidoException si algún dato falta
     */
    public Cancelacion(LocalDateTime fechaHora, String motivo, Usuario responsable)
            throws DatoInvalidoException {
        if (fechaHora == null) {
            throw new DatoInvalidoException("La fecha de cancelación es obligatoria.");
        }
        if (responsable == null) {
            throw new DatoInvalidoException("El responsable de la cancelación es obligatorio.");
        }
        this.fechaHora = fechaHora;
        this.motivo = Validador.validarRequerido(motivo, "motivo de cancelación");
        this.responsable = responsable;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    /** Ej.: "Cancelada el 01/10/2026 18:40 por operador: Lluvia fuerte anunciada". */
    @Override
    public String toString() {
        return "Cancelada el " + FechaUtil.formatearFechaHora(fechaHora)
                + " por " + responsable.getUsername() + ": " + motivo;
    }
}

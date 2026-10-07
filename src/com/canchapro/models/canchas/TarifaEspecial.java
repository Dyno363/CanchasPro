/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.canchas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 *
 * @author SANTANA
 */



/**
 * Tarifa especial: aplica durante todo un día concreto del calendario.
 *
 * Ejemplos: feriados (1 de noviembre), Navidad, días de torneo.
 * Se le asigna una prioridad mayor que a las franjas normales, para que
 * ese día "gane" sobre la tarifa de lunes a viernes o de fin de semana.
 */
public class TarifaEspecial extends Tarifa {

    public static final String TIPO = "ESPECIAL";

    private LocalDate fecha;
    private String motivo;

    /**
     * @param codigo     código único, ej. "T04"
     * @param nombre     ej. "Día de Todos los Santos"
     * @param precioHora precio por hora (mayor que cero)
     * @param prioridad  0 o más (se recomienda mayor que la de las franjas, ej. 10)
     * @param fecha      día en que aplica
     * @param motivo     razón de la tarifa, ej. "Feriado nacional"
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public TarifaEspecial(String codigo, String nombre, double precioHora, int prioridad,
                          LocalDate fecha, String motivo) throws DatoInvalidoException {
        super(codigo, nombre, precioHora, prioridad);
        setFecha(fecha);
        setMotivo(motivo);
    }

    // =====================================================================
    //  POLIMORFISMO
    // =====================================================================

    /** Aplica a cualquier hora de su fecha. */
    @Override
    public boolean aplicaA(LocalDateTime momento) {
        return momento != null && momento.toLocalDate().equals(fecha);
    }

    @Override
    public String getTipoTarifa() {
        return TIPO;
    }

    /** Ejemplo: "01/11/2026 (Feriado nacional)". */
    @Override
    public String getDescripcionAplicacion() {
        return FechaUtil.formatearFecha(fecha) + " (" + motivo + ")";
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public LocalDate getFecha() {
        return fecha;
    }

    public final void setFecha(LocalDate fecha) throws DatoInvalidoException {
        if (fecha == null) {
            throw new DatoInvalidoException("La fecha de la tarifa especial es obligatoria.");
        }
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public final void setMotivo(String motivo) throws DatoInvalidoException {
        this.motivo = Validador.validarRequerido(motivo, "motivo");
    }
}
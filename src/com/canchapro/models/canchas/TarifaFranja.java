/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.canchas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.FechaUtil;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
/**
 *
 * @author SANTANA
 */



/**
 * Tarifa normal: aplica ciertos días de la semana dentro de un rango de horas.
 *
 * Ejemplos del PDF:
 * 
 *   Lunes a viernes, 08:00 a 17:00 → Q150/h
 *   Lunes a viernes, 17:00 a 23:00 → Q225/h
 *   Sábado y domingo, 08:00 a 23:00 → Q250/h
 * 
 *
 * El rango es semiabierto: [horaInicio, horaFin). Es decir, incluye la hora
 * de inicio pero NO la de fin. Así, a las 17:00 en punto aplica la tarifa
 * nocturna y no la diurna: las franjas nunca se pisan en el borde.
 */
public class TarifaFranja extends Tarifa {

    public static final String TIPO = "FRANJA";

    /** Abreviaturas en español, en el orden de DayOfWeek (lunes = índice 0). */
    private static final String[] NOMBRES_DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private Set<DayOfWeek> dias;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    /**
     * @param codigo     código único, ej. "T01"
     * @param nombre     ej. "Lunes a viernes diurno"
     * @param precioHora precio por hora
     * @param prioridad  0 o más 
     * @param dias       días en que aplica 
     * @param horaInicio inicio de la franja (incluido)
     * @param horaFin    fin de la franja (excluido); debe ser posterior al inicio
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public TarifaFranja(String codigo, String nombre, double precioHora, int prioridad,
                        Set<DayOfWeek> dias, LocalTime horaInicio, LocalTime horaFin)
            throws DatoInvalidoException {
        super(codigo, nombre, precioHora, prioridad);
        setFranja(dias, horaInicio, horaFin);
    }

    // =====================================================================
    //  POLIMORFISMO
    // =====================================================================

    /**
     * Aplica si el día de la semana está en la lista y la hora cae en
     * [horaInicio, horaFin).
     */
    @Override
    public boolean aplicaA(LocalDateTime momento) {
        if (momento == null || !dias.contains(momento.getDayOfWeek())) {
            return false;
        }
        LocalTime hora = momento.toLocalTime();
        return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
    }

    @Override
    public String getTipoTarifa() {
        return TIPO;
    }

    /** Ejemplo: "Lun, Mar, Mié, Jue, Vie de 08:00 a 17:00". */
    @Override
    public String getDescripcionAplicacion() {
        StringJoiner nombres = new StringJoiner(", ");
        for (DayOfWeek dia : dias) {
            nombres.add(NOMBRES_DIAS[dia.getValue() - 1]);
        }
        return nombres + " de " + FechaUtil.formatearHora(horaInicio)
                + " a " + FechaUtil.formatearHora(horaFin);
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    /** @return copia de solo lectura (nadie puede modificar los días desde fuera) */
    public Set<DayOfWeek> getDias() {
        return Collections.unmodifiableSet(dias);
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    /**
     * Días y horas se cambian juntos porque se validan entre sí.
     */
    public final void setFranja(Set<DayOfWeek> dias, LocalTime horaInicio, LocalTime horaFin)
            throws DatoInvalidoException {
        if (dias == null || dias.isEmpty()) {
            throw new DatoInvalidoException("La tarifa debe aplicar al menos a un día.");
        }
        if (horaInicio == null || horaFin == null) {
            throw new DatoInvalidoException("La hora de inicio y de fin de la franja son obligatorias.");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new DatoInvalidoException("La hora de inicio de la franja debe ser anterior a la hora de fin.");
        }
        this.dias = EnumSet.copyOf(dias);
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }
}
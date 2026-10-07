/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.canchas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
/**
 *
 * @author SANTANA
 */



/**
 * Calcula el precio de alquiler de una cancha a partir de las tarifas configuradas.
 *
 * Nivel avanzado del PDF: si una reserva cruza dos franjas tarifarias,
 * el período se divide en tramos y cada tramo se cobra con su propia tarifa
 *
 * Ejemplo: martes de 16:00 a 18:00
 * 
 *   16:00 - 17:00  Lunes a viernes diurno    Q150/h × 1 h = Q150.00
 *   17:00 - 18:00  Lunes a viernes nocturno  Q225/h × 1 h = Q225.00
 *                                            Total        = Q375.00
 * 
 *
 * Esta clase NO guarda una copia de las tarifas: trabaja sobre la lista viva
 * del ComplejoDeportivo. Si el administrador agrega o cambia una tarifa, el
 * siguiente cálculo ya la toma en cuenta.
 */
public class CalculadoraTarifa {

    /** Lista de tarifas del sistema (referencia compartida, no copia). */
    private final List<Tarifa> tarifas;

    /**
     * @param tarifas lista de tarifas configuradas (normalmente la del ComplejoDeportivo)
     */
    public CalculadoraTarifa(List<Tarifa> tarifas) {
        if (tarifas == null) {
            throw new IllegalArgumentException("La lista de tarifas no puede ser null.");
        }
        this.tarifas = tarifas;
    }

    // =====================================================================
    //  CONSULTAS
    // =====================================================================

    /**
     * Busca la tarifa que aplica en un momento dado.
     *
     * Si varias aplican, gana la de mayor prioridad. Si dos tienen la misma
     * prioridad, gana la que aparece primero en la lista.
     *
     * @param momento fecha y hora a evaluar
     * @return la tarifa ganadora
     * @throws EntidadNoEncontradaException si ninguna tarifa cubre ese momento
     */
    public Tarifa buscarTarifa(LocalDateTime momento) throws EntidadNoEncontradaException {
        Tarifa ganadora = null;
        for (Tarifa tarifa : tarifas) {
            if (tarifa.aplicaA(momento)
                    && (ganadora == null || tarifa.getPrioridad() > ganadora.getPrioridad())) {
                ganadora = tarifa;
            }
        }
        if (ganadora == null) {
            throw new EntidadNoEncontradaException("No hay tarifa configurada para el "
                    + FechaUtil.formatearFechaHora(momento) + ".");
        }
        return ganadora;
    }

    /**
     * Divide el período [inicio, fin) en tramos, uno por cada cambio de tarifa.
     *
     * Recorre el período minuto a minuto. Cada vez que la tarifa ganadora
     * cambia, cierra el tramo actual y abre uno nuevo. Así funciona aunque la
     * reserva empiece a una hora "rara" (ej. 16:45).
     *
     * @return lista de tramos en orden cronológico (solo lectura)
     * @throws DatoInvalidoException        si el fin no es posterior al inicio
     * @throws EntidadNoEncontradaException si algún minuto no tiene tarifa
     */
    public List<Tramo> desglosar(LocalDateTime inicio, LocalDateTime fin)
            throws DatoInvalidoException, EntidadNoEncontradaException {
        validarRango(inicio, fin);

        List<Tramo> tramos = new ArrayList<>();
        LocalDateTime inicioTramo = inicio;
        Tarifa tarifaActual = buscarTarifa(inicio);

        LocalDateTime minuto = inicio.plusMinutes(1);
        while (minuto.isBefore(fin)) {
            Tarifa tarifaMinuto = buscarTarifa(minuto);
            if (!tarifaMinuto.equals(tarifaActual)) {
                tramos.add(new Tramo(inicioTramo, minuto, tarifaActual));
                inicioTramo = minuto;
                tarifaActual = tarifaMinuto;
            }
            minuto = minuto.plusMinutes(1);
        }
        tramos.add(new Tramo(inicioTramo, fin, tarifaActual));
        return Collections.unmodifiableList(tramos);
    }

    /**
     * Precio total de la cancha para el período [inicio, fin).
     *
     * @return suma de los subtotales de cada tramo, redondeada a centavos
     * @throws DatoInvalidoException        si el fin no es posterior al inicio
     * @throws EntidadNoEncontradaException si algún minuto no tiene tarifa
     */
    public double calcularPrecioCancha(LocalDateTime inicio, LocalDateTime fin)
            throws DatoInvalidoException, EntidadNoEncontradaException {
        double total = 0;
        for (Tramo tramo : desglosar(inicio, fin)) {
            total += tramo.getSubtotal();
        }
        return MonedaUtil.redondear(total);
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static void validarRango(LocalDateTime inicio, LocalDateTime fin) throws DatoInvalidoException {
        if (inicio == null || fin == null) {
            throw new DatoInvalidoException("El inicio y el fin de la reserva son obligatorios.");
        }
        if (!inicio.isBefore(fin)) {
            throw new DatoInvalidoException("La hora de fin debe ser posterior a la hora de inicio.");
        }
    }

    // =====================================================================
    //  CLASE ANIDADA: un tramo del desglose
    // =====================================================================

    /**
     * Un pedazo del período cobrado con una sola tarifa.
     *
     * Es inmutable (todos sus campos son final) porque representa un
     * resultado de cálculo, no algo que se edite.
     */
    public static final class Tramo {

        private final LocalDateTime inicio;
        private final LocalDateTime fin;
        private final Tarifa tarifa;
        private final double subtotal;

        private Tramo(LocalDateTime inicio, LocalDateTime fin, Tarifa tarifa) {
            this.inicio = inicio;
            this.fin = fin;
            this.tarifa = tarifa;
            this.subtotal = MonedaUtil.redondear(tarifa.getPrecioHora() * getHoras());
        }

        public LocalDateTime getInicio() {
            return inicio;
        }

        public LocalDateTime getFin() {
            return fin;
        }

        public Tarifa getTarifa() {
            return tarifa;
        }

        /** Horas del tramo con decimales (ej. 30 minutos = 0.5). */
        public double getHoras() {
            return Duration.between(inicio, fin).toMinutes() / 60.0;
        }

        public double getSubtotal() {
            return subtotal;
        }

        /** Ejemplo: "16:00 - 17:00  Lunes a viernes diurno  Q150.00/h x 1.00 h = Q150.00" */
        @Override
        public String toString() {
            return FechaUtil.formatearHora(inicio.toLocalTime()) + " - "
                    + FechaUtil.formatearHora(fin.toLocalTime()) + "  "
                    + tarifa.getNombre() + "  "
                    + MonedaUtil.formatear(tarifa.getPrecioHora()) + "/h x "
                    + String.format(Locale.US, "%.2f", getHoras()) + " h = "
                    + MonedaUtil.formatear(subtotal);
        }
    }
}

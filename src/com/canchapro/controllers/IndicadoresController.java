/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.MonedaUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calcula los números de las tarjetas del Dashboard.
 *
 * No guarda datos ni repite lógica: reutiliza los controladores que ya
 * existen (reservas, pagos y mapa) y solo junta sus resultados en dos objetos
 * de resumen. Así la vista NO cuenta ni suma nada: solo muestra.
 *
 * 
 *   {@link ResumenReservas}: tarjetas de la pestaña 1 (Reservas).
 *   @link ResumenCanchas}: tarjetas de la pestaña 2 (Mapa).
 * 
 */

public class IndicadoresController {
    private final ReservaController reservaController;
    private final PagoController pagoController;
    private final MapaController mapaController;

    public IndicadoresController(ReservaController reservaController, PagoController pagoController,
                                 MapaController mapaController) {
        this.reservaController = Objects.requireNonNull(reservaController);
        this.pagoController = Objects.requireNonNull(pagoController);
        this.mapaController = Objects.requireNonNull(mapaController);
    }

    /** Indicadores de reservas y dinero de una fecha (normalmente hoy). */
    public ResumenReservas getResumenReservas(LocalDate fecha) {
        int activas = 0;
        int pendientes = 0;
        int enCurso = 0;
        for (Reserva r : reservaController.getReservasDelDia(fecha)) {
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                continue;
            }
            activas++;
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                pendientes++;
            } else if (r.getEstado() == EstadoReserva.EN_CURSO) {
                enCurso++;
            }
        }

        List<Reserva> conSaldo = pagoController.getReservasConSaldo();
        double saldo = 0;
        for (Reserva r : conSaldo) {
            saldo += r.calcularSaldo();
        }
        return new ResumenReservas(activas, pendientes, enCurso, pagoController.getTotalCobrado(fecha),
                MonedaUtil.redondear(saldo), conSaldo.size());
    }

    /** Cuántas canchas hay en cada estado visual (leyenda del mapa). */
    public ResumenCanchas getResumenCanchas() {
        Map<EstadoVisualCancha, Integer> conteo = mapaController.contarPorEstado();
        return new ResumenCanchas(conteo.get(EstadoVisualCancha.DISPONIBLE),
                conteo.get(EstadoVisualCancha.EN_CURSO),
                conteo.get(EstadoVisualCancha.MANTENIMIENTO),
                conteo.get(EstadoVisualCancha.INACTIVA),
                mapaController.isTiendaActiva());
    }

    // =====================================================================
    //  OBJETOS DE RESUMEN (inmutables: solo getters)
    // =====================================================================

    /** Números de la pestaña Reservas. */
    public static final class ResumenReservas {
        private final int reservasDelDia;
        private final int pendientes;
        private final int enCurso;
        private final double cobrado;
        private final double saldoPendiente;
        private final int reservasConSaldo;

        private ResumenReservas(int reservasDelDia, int pendientes, int enCurso, double cobrado,
                                double saldoPendiente, int reservasConSaldo) {
            this.reservasDelDia = reservasDelDia;
            this.pendientes = pendientes;
            this.enCurso = enCurso;
            this.cobrado = cobrado;
            this.saldoPendiente = saldoPendiente;
            this.reservasConSaldo = reservasConSaldo;
        }

        /** Reservas del día que no están canceladas. */
        public int getReservasDelDia() {
            return reservasDelDia;
        }

        /** Reservas del día que aún esperan confirmación. */
        public int getPendientes() {
            return pendientes;
        }

        public int getEnCurso() {
            return enCurso;
        }

        /** Dinero recibido ese día. */
        public double getCobrado() {
            return cobrado;
        }

        /** Lo que todavía deben TODAS las reservas no canceladas (no solo las del día). */
        public double getSaldoPendiente() {
            return saldoPendiente;
        }

        public int getReservasConSaldo() {
            return reservasConSaldo;
        }

        public boolean haySaldoPendiente() {
            return !MonedaUtil.esCero(saldoPendiente);
        }
    }

    /** Números de la pestaña Mapa. */
    public static final class ResumenCanchas {
        private final int disponibles;
        private final int enCurso;
        private final int mantenimiento;
        private final int inactivas;
        private final boolean tiendaActiva;

        private ResumenCanchas(int disponibles, int enCurso, int mantenimiento, int inactivas,
                               boolean tiendaActiva) {
            this.disponibles = disponibles;
            this.enCurso = enCurso;
            this.mantenimiento = mantenimiento;
            this.inactivas = inactivas;
            this.tiendaActiva = tiendaActiva;
        }

        public int getDisponibles() {
            return disponibles;
        }

        public int getEnCurso() {
            return enCurso;
        }

        public int getMantenimiento() {
            return mantenimiento;
        }

        public int getInactivas() {
            return inactivas;
        }

        public int getTotal() {
            return disponibles + enCurso + mantenimiento + inactivas;
        }

        /** Para el texto de ayuda "clic en una cancha en curso para abrir el POS". */
        public boolean isTiendaActiva() {
            return tiendaActiva;
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.exceptions.PagoInvalidoException;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador de PAGOS y abonos de las reservas.
 *
 * Las reglas de dinero viven en el modelo y aquí solo se usan:
 *   
 *     {@code Pago}: monto mayor que cero, método obligatorio (inmutable).   
 *     {@code Reserva.registrarPago}: no pagar más que el saldo (prueba #8) ni
 *       pagar una reserva cancelada.   
 *     {@code Reserva.calcularSaldo}: total - pagado, nunca negativo (pruebas #6 y #7).   
 *    
 *
 * Reglas propias del controlador:
 *   
 *     El monto llega como TEXTO desde la vista ("Q1,250.50") y se convierte aquí:
 *       la vista no hace Double.parseDouble.   
 *     Transferencias y depósitos exigen el número de referencia en la observación.   
 *     El código del pago (PG0005...) lo genera el sistema.   
 *    
 */
public class PagoController {
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;

    public PagoController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }

    // =====================================================================
    //  CONSULTAS
    // =====================================================================

    /** Usuario que está cobrando (para mensajes o futuros reportes por cajero). */
    public String getCajero() {
        return sesion.getUsuario().getNombre();
    }

    public MetodoPago[] getMetodos() {
        return MetodoPago.values();
    }

    /** @return pagos de la reserva en el orden en que se registraron */
    public List<Pago> getPagos(String codigoReserva) throws EntidadNoEncontradaException {
        return obtenerReserva(codigoReserva).getPagos();
    }

    /** Datos que muestra el diálogo de pago: total, pagado, saldo y anticipo. */
    public ResumenCobro getResumen(String codigoReserva) throws EntidadNoEncontradaException {
        Reserva r = obtenerReserva(codigoReserva);
        double porcentajeMinimo = complejo.getConfiguracion().getPorcentajeAnticipoMinimo();
        double anticipo = MonedaUtil.redondear(r.calcularTotal() * porcentajeMinimo / 100.0);
        double faltaAnticipo = Math.max(0, MonedaUtil.redondear(anticipo - r.calcularTotalPagado()));
        return new ResumenCobro(r.calcularTotal(), r.calcularTotalPagado(), r.calcularSaldo(),
                r.calcularPorcentajePagado(), anticipo, faltaAnticipo);
    }

    /**
     * Después de un pago, la vista pregunta esto para ofrecer "¿Confirmar la reserva?".
     *
     * @return true si está PENDIENTE y ya cubre el anticipo mínimo
     */
    public boolean puedeConfirmarse(String codigoReserva) throws EntidadNoEncontradaException {
        Reserva r = obtenerReserva(codigoReserva);
        double minimo = complejo.getConfiguracion().getPorcentajeAnticipoMinimo();
        return r.getEstado() == EstadoReserva.PENDIENTE
                && r.calcularPorcentajePagado() + MonedaUtil.TOLERANCIA >= minimo;
    }

    /** Reservas no canceladas que aún deben dinero (reporte "pagos pendientes"). */
    public List<Reserva> getReservasConSaldo() {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            if (r.getEstado() != EstadoReserva.CANCELADA && !r.estaPagada()) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /** Dinero recibido por reservas en una fecha (indicador del dashboard). */
    public double getTotalCobrado(LocalDate fecha) {
        double total = 0;
        for (Reserva r : complejo.getReservas()) {
            for (Pago p : r.getPagos()) {
                if (p.getFechaHora().toLocalDate().equals(fecha)) {
                    total += p.getMonto();
                }
            }
        }
        return MonedaUtil.redondear(total);
    }

    // =====================================================================
    //  OPERACIONES
    // =====================================================================

    /**
     * Registra un abono (prueba #6) leyendo el monto tal como lo escribió el operador.
     *
     * @param montoTexto ej. "150", "Q150.00", "Q1,250.50"
     */
    public Pago registrarPago(String codigoReserva, String montoTexto, MetodoPago metodo, String observacion)
            throws CanchaProException {
        return registrarPago(codigoReserva, MonedaUtil.leer(montoTexto), metodo, observacion);
    }

    /**
     * Registra un abono.
     *
     * @throws PagoInvalidoException si el monto no es positivo, supera el saldo
     *                               (prueba #8) o la reserva está cancelada
     */
    public Pago registrarPago(String codigoReserva, double monto, MetodoPago metodo, String observacion)
            throws CanchaProException {
        Reserva reserva = obtenerReserva(codigoReserva);
        exigirReferenciaSiCorresponde(metodo, observacion);
        Pago pago = new Pago(complejo.generarCodigo(GeneradorCodigo.Tipo.PAGO), FechaUtil.ahora(),
                monto, metodo, observacion);
        reserva.registrarPago(pago);
        return pago;
    }

    /**
     * Paga exactamente el saldo pendiente (prueba #7: al terminar, saldo = Q0.00).
     */
    public Pago pagarSaldo(String codigoReserva, MetodoPago metodo, String observacion)
            throws CanchaProException {
        Reserva reserva = obtenerReserva(codigoReserva);
        if (reserva.estaPagada()) {
            throw new PagoInvalidoException("La reserva " + codigoReserva + " ya está pagada por completo.");
        }
        return registrarPago(codigoReserva, reserva.calcularSaldo(), metodo, observacion);
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private Reserva obtenerReserva(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarReserva(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la reserva " + codigo + "."));
    }

    private static void exigirReferenciaSiCorresponde(MetodoPago metodo, String observacion)
            throws PagoInvalidoException {
        boolean requiere = metodo == MetodoPago.TRANSFERENCIA || metodo == MetodoPago.DEPOSITO;
        if (requiere && Validador.esVacio(observacion)) {
            throw new PagoInvalidoException("Para pagos por " + metodo.getDescripcion().toLowerCase(Locale.ROOT)
                    + " escriba el número de referencia o boleta en la observación.");
        }
    }

    // =====================================================================
    //  CLASE ANIDADA: resumen de cobro para la vista
    // =====================================================================

    /**
     * Foto de los números de cobro de una reserva. Es inmutable: se pide de
     * nuevo después de cada pago para refrescar el diálogo.
     */
    public static final class ResumenCobro {

        private final double total;
        private final double pagado;
        private final double saldo;
        private final double porcentajePagado;
        private final double anticipoMinimo;
        private final double faltaParaAnticipo;

        private ResumenCobro(double total, double pagado, double saldo, double porcentajePagado,
                             double anticipoMinimo, double faltaParaAnticipo) {
            this.total = total;
            this.pagado = pagado;
            this.saldo = saldo;
            this.porcentajePagado = porcentajePagado;
            this.anticipoMinimo = anticipoMinimo;
            this.faltaParaAnticipo = faltaParaAnticipo;
        }

        public double getTotal() {
            return total;
        }

        public double getPagado() {
            return pagado;
        }

        public double getSaldo() {
            return saldo;
        }

        public double getPorcentajePagado() {
            return porcentajePagado;
        }

        public double getAnticipoMinimo() {
            return anticipoMinimo;
        }

        public double getFaltaParaAnticipo() {
            return faltaParaAnticipo;
        }

        public boolean estaPagada() {
            return MonedaUtil.esCero(saldo);
        }

        /** Ej.: "Total Q650.00 | Pagado Q100.00 (15.38%) | Saldo Q550.00" */
        @Override
        public String toString() {
            return "Total " + MonedaUtil.formatear(total) + " | Pagado " + MonedaUtil.formatear(pagado)
                    + " (" + porcentajePagado + "%) | Saldo " + MonedaUtil.formatear(saldo);
        }
    }
}

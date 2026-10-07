/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.exceptions.PagoInvalidoException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import java.time.LocalDateTime;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Un pago o abono realizado a una reserva.
 *
 * Una reserva puede tener varios pagos (anticipo, abono, saldo final).
 * El pago vive DENTRO de su reserva (composición): no existe un pago sin reserva.
 *
 * Es inmutable: todos sus campos son final y no tiene setters.
 * Un pago registrado no se edita; si hubo un error, se corrige con otro
 * movimiento. Así se protege el historial de dinero.
 *
 * Esta clase solo valida lo que puede saber por sí misma (monto positivo,
 * método obligatorio). La regla "no pagar más que el saldo" depende de la
 * reserva, por eso se valida en {@code Reserva.registrarPago()}.
 */
public final class Pago {

    private final String codigo;
    private final LocalDateTime fechaHora;
    private final double monto;
    private final MetodoPago metodo;
    private final String observacion;

    /**
     * @param codigo      código único, ej. "PG0001"
     * @param fechaHora   momento del pago
     * @param monto       monto en quetzales (mayor que cero)
     * @param metodo      efectivo, tarjeta, transferencia o depósito
     * @param observacion nota opcional (ej. "Anticipo"); null se guarda como ""
     * @throws PagoInvalidoException si algún dato es inválido
     */
    public Pago(String codigo, LocalDateTime fechaHora, double monto,
                MetodoPago metodo, String observacion) throws PagoInvalidoException {
        if (codigo == null || codigo.isBlank()) {
            throw new PagoInvalidoException("El código del pago es obligatorio.");
        }
        if (fechaHora == null) {
            throw new PagoInvalidoException("La fecha del pago es obligatoria.");
        }
        if (Double.isNaN(monto) || monto <= 0 || MonedaUtil.esCero(monto)) {
            throw new PagoInvalidoException("El monto del pago debe ser mayor que cero.");
        }
        if (metodo == null) {
            throw new PagoInvalidoException("El método de pago es obligatorio.");
        }
        this.codigo = codigo.trim();
        this.fechaHora = fechaHora;
        this.monto = MonedaUtil.redondear(monto);
        this.metodo = metodo;
        this.observacion = observacion == null ? "" : observacion.trim();
    }

    // =====================================================================
    //  GETTERS (no hay setters: el pago es inmutable)
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public double getMonto() {
        return monto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public String getObservacion() {
        return observacion;
    }

    public boolean tieneObservacion() {
        return !observacion.isEmpty();
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Pago)) {
            return false;
        }
        return codigo.equals(((Pago) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Ej.: "PG0001 - 02/10/2026 11:05 - Q300.00 (Transferencia)". */
    @Override
    public String toString() {
        return codigo + " - " + FechaUtil.formatearFechaHora(fechaHora) + " - "
                + MonedaUtil.formatear(monto) + " (" + metodo + ")";
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */

/**
 * Servicio del catálogo que se puede agregar a una reserva:
 * balón, chalecos, árbitro, iluminación, agua para jugadores, etc.
 *
 * Hay dos formas de cobro:
 *   Por reserva (cobroPorHora = false): precio fijo. Ej. balón Q25.
 *   Por hora (cobroPorHora = true): precio × horas. Ej. árbitro Q75/h.
 * 
 *
 * Esta clase es el CATÁLOGO (lo que se ofrece). Lo que se agregó a una
 * reserva concreta es un {@link ServicioReserva}.
 */
public class ServicioAdicional {

    private final String codigo;
    private String nombre;
    private double precio;
    private boolean cobroPorHora;
    private boolean activo;

    /**
     * Crea un servicio activo.
     *
     * @param codigo       código único, ej. "S03"
     * @param nombre       ej. "Árbitro"
     * @param precio       precio unitario o por hora
     * @param cobroPorHora true si se cobra por cada hora de la reserva
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public ServicioAdicional(String codigo, String nombre, double precio, boolean cobroPorHora)
            throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código");
        setNombre(nombre);
        setPrecio(precio);
        this.cobroPorHora = cobroPorHora;
        this.activo = true;
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) throws DatoInvalidoException {
        this.nombre = Validador.validarRequerido(nombre, "nombre del servicio");
    }

    public double getPrecio() {
        return precio;
    }

    /**
     * Cambiar el precio NO afecta a las reservas ya hechas: cada
     * ServicioReserva guardó el precio del momento en que se agregó.
     */
    public final void setPrecio(double precio) throws DatoInvalidoException {
        this.precio = MonedaUtil.redondear(Validador.validarPositivo(precio, "precio del servicio"));
    }

    public boolean isCobroPorHora() {
        return cobroPorHora;
    }

    public void setCobroPorHora(boolean cobroPorHora) {
        this.cobroPorHora = cobroPorHora;
    }

    /** Un servicio inactivo ya no se ofrece, pero se conserva en el historial. */
    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // =====================================================================
    //  LÓGICA
    // =====================================================================

    /**
     * Calcula cuánto costaría este servicio con el precio actual.
     *
     * @param cantidad unidades (ej. 2 balones)
     * @param horas    duración de la reserva; solo se usa si cobra por hora
     * @return costo redondeado a centavos
     */
    public double calcularCosto(int cantidad, double horas) {
        double costo = precio * cantidad;
        if (cobroPorHora) {
            costo *= horas;
        }
        return MonedaUtil.redondear(costo);
    }

    /** "Por hora" o "Por reserva", para tablas y combos. */
    public String getModalidadTexto() {
        return cobroPorHora ? "Por hora" : "Por reserva";
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ServicioAdicional)) {
            return false;
        }
        return codigo.equals(((ServicioAdicional) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Ej.: "S01 - Balón profesional (Q25.00)" o "S03 - Árbitro (Q75.00/h)". */
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + MonedaUtil.formatear(precio)
                + (cobroPorHora ? "/h" : "") + ")";
    }
}

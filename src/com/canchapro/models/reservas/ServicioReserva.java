/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.util.Locale;
/**
 *
 * @author SANTANA
 */
/**
 * Un servicio adicional ya agregado a una reserva concreta
 * (ej. "1 árbitro durante 2 horas a Q75/h").
 *
 * Guarda una "foto" del precio en el momento de agregarlo
 * ({@code precioUnitario}). Si mañana el árbitro sube a Q90/h, las reservas
 * anteriores conservan su precio original.
 *
 * Las horas también quedan fijas: si el servicio es por reserva, se
 * guarda {@code horas = 0} y el subtotal es simplemente precio × cantidad.
 */
public class ServicioReserva implements Cobrable {

    private final ServicioAdicional servicio;
    private int cantidad;
    private final double precioUnitario;
    private final double horas;

    /**
     * Agrega un servicio NUEVO a una reserva, usando el precio actual del catálogo.
     *
     * @param servicio servicio del catálogo (debe estar activo)
     * @param cantidad unidades (mayor que cero)
     * @param horas    duración de la reserva; se ignora si el servicio es por reserva
     * @throws DatoInvalidoException si el servicio está inactivo o algún dato es inválido
     */
    public ServicioReserva(ServicioAdicional servicio, int cantidad, double horas)
            throws DatoInvalidoException {
        this(validarActivo(servicio), cantidad, servicio.getPrecio(), horas);
    }

    /**
     * Reconstruye un servicio YA GUARDADO (desde servicios_reserva.csv),
     * con el precio que tenía en ese momento. No exige que el servicio
     * siga activo, porque el historial debe poder cargarse completo.
     *
     * @param servicio       servicio del catálogo
     * @param cantidad       unidades (mayor que cero)
     * @param precioUnitario precio guardado (mayor que cero)
     * @param horas          horas guardadas (0 si es por reserva)
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public ServicioReserva(ServicioAdicional servicio, int cantidad, double precioUnitario, double horas)
            throws DatoInvalidoException {
        if (servicio == null) {
            throw new DatoInvalidoException("El servicio es obligatorio.");
        }
        this.servicio = servicio;
        this.cantidad = Validador.validarEnteroPositivo(cantidad, "cantidad");
        this.precioUnitario = MonedaUtil.redondear(Validador.validarPositivo(precioUnitario, "precio unitario"));
        if (servicio.isCobroPorHora()) {
            this.horas = Validador.validarPositivo(horas, "horas");
        } else {
            this.horas = 0;
        }
    }

    // =====================================================================
    //  GETTERS
    // =====================================================================

    public ServicioAdicional getServicio() {
        return servicio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /** @return horas cobradas, o 0 si el servicio es por reserva */
    public double getHoras() {
        return horas;
    }

    // =====================================================================
    //  LÓGICA
    // =====================================================================

    /**
     * Suma unidades cuando se agrega otra vez el mismo servicio
     * (ej. pedir un segundo balón), en lugar de crear una línea repetida.
     */
    public void aumentarCantidad(int extra) throws DatoInvalidoException {
        this.cantidad += Validador.validarEnteroPositivo(extra, "cantidad adicional");
    }

    /**
     * Subtotal = precio × cantidad, y además × horas si se cobró por hora.
     *
     * Usa las horas guardadas (y no el {@code cobroPorHora} actual del catálogo),
     * para que el historial no cambie si el administrador modifica el servicio.
     */
    @Override
    public double getSubtotal() {
        double subtotal = precioUnitario * cantidad;
        if (horas > 0) {
            subtotal *= horas;
        }
        return MonedaUtil.redondear(subtotal);
    }

    /** Ej.: "Árbitro x1 (2 h)" o "Balón profesional x2". */
    @Override
    public String getConcepto() {
        String texto = servicio.getNombre() + " x" + cantidad;
        if (horas > 0) {
            texto += " (" + formatearHoras(horas) + " h)";
        }
        return texto;
    }

    @Override
    public String toString() {
        return getConcepto() + " = " + MonedaUtil.formatear(getSubtotal());
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    /** 2.0 -> "2", 1.5 -> "1.5" */
    private static String formatearHoras(double horas) {
        if (horas == Math.floor(horas)) {
            return String.valueOf((long) horas);
        }
        return String.format(Locale.US, "%.2f", horas).replaceAll("0+$", "");
    }

    private static ServicioAdicional validarActivo(ServicioAdicional servicio) throws DatoInvalidoException {
        if (servicio == null) {
            throw new DatoInvalidoException("El servicio es obligatorio.");
        }
        if (!servicio.isActivo()) {
            throw new DatoInvalidoException("El servicio '" + servicio.getNombre() + "' no está disponible.");
        }
        return servicio;
    }
}

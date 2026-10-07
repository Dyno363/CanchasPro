/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.canchas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDateTime;
import java.util.Objects; 
/**
 *
 * @author SANTANA
 */



/**
 * Tarifa abstracta: un precio por hora que aplica en ciertos momentos.
 *
 * Cada subclase decide CUÁNDO aplica, implementando {@link #aplicaA}:
 * 
 *   {@link TarifaFranja}: por días de la semana y rango de horas (tarifa normal).
 *   {@link TarifaEspecial}: en una fecha concreta, como un feriado (tarifa especial).
 * 
 *
 * La CalculadoraTarifa trabaja con una {@code List<Tarifa>} sin saber de qué
 * tipo es cada una: solo pregunta {@code aplicaA(momento)}. Eso es polimorfismo.
 *
 * Si varias tarifas aplican al mismo momento, gana la de mayor {@code prioridad}.
 * Por eso una TarifaEspecial (prioridad alta) le gana a una TarifaFranja (prioridad baja).
 */
public abstract class Tarifa {

    private final String codigo;
    private String nombre;
    private double precioHora;
    private int prioridad;

    /**
     * @param codigo     código único, ej. "T01"
     * @param nombre     nombre descriptivo, ej. "Lunes a viernes diurno"
     * @param precioHora precio por hora en quetzales (mayor que cero)
     * @param prioridad  0 o más; a mayor número, mayor prioridad
     * @throws DatoInvalidoException si algún dato es inválido
     */
    protected Tarifa(String codigo, String nombre, double precioHora, int prioridad)
            throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código");
        setNombre(nombre);
        setPrecioHora(precioHora);
        setPrioridad(prioridad);
    }

    // =====================================================================
    //  MÉTODOS ABSTRACTOS: cada tipo de tarifa los resuelve a su manera
    // =====================================================================

    /**
     * Indica si esta tarifa aplica en un momento dado.
     *
     * @param momento fecha y hora a evaluar
     * @return true si la tarifa es válida para ese momento
     */
    public abstract boolean aplicaA(LocalDateTime momento);

    /**
     * Identificador del tipo para guardar en tarifas.csv ("FRANJA" o "ESPECIAL").
     */
    public abstract String getTipoTarifa();

    /**
     * Texto legible de cuándo aplica, para tablas.
     * Ej.: "Lun, Mar, Mié, Jue, Vie de 08:00 a 17:00" o "01/11/2026 (Feriado nacional)".
     */
    public abstract String getDescripcionAplicacion();

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
        this.nombre = Validador.validarRequerido(nombre, "nombre de la tarifa");
    }

    public double getPrecioHora() {
        return precioHora;
    }

    public final void setPrecioHora(double precioHora) throws DatoInvalidoException {
        this.precioHora = MonedaUtil.redondear(Validador.validarPositivo(precioHora, "precio por hora"));
    }

    public int getPrioridad() {
        return prioridad;
    }

    public final void setPrioridad(int prioridad) throws DatoInvalidoException {
        this.prioridad = Validador.validarNoNegativo(prioridad, "prioridad");
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Tarifa)) {
            return false;
        }
        return codigo.equals(((Tarifa) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Ejemplo: "T01 - Lunes a viernes diurno (Q150.00/h)". */
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + MonedaUtil.formatear(precioHora) + "/h)";
    }
}

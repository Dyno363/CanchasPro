/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
/**
 *
 * @author SANTANA
 */
/**
 * Parámetros del sistema que se pueden cambiar SIN recompilar.
 *
 * Se leen de {@code data/config.properties} al iniciar y se guardan al cerrar:
 * 
 *   nombre_complejo=Complejo Deportivo La 10
 *   modulo_tienda=true
 *   minutos_limpieza=0
 *   porcentaje_anticipo_minimo=0
 * 
 *
 * Estos valores permiten resolver varios "cambios en vivo" de la defensa
 * sin tocar el código: activar/desactivar la tienda, agregar tiempo de
 * limpieza entre reservas o exigir un anticipo mínimo.
 */
public class ConfiguracionSistema {
     public static final String NOMBRE_POR_DEFECTO = "Complejo Deportivo";

    private String nombreComplejo;
    private boolean moduloTiendaActivo;
    private int minutosLimpieza;
    private double porcentajeAnticipoMinimo;

    /**
     * Configuración por defecto: tienda activa, sin limpieza y sin anticipo mínimo.
     * Se usa si config.properties no existe.
     */
    public ConfiguracionSistema() {
        this.nombreComplejo = NOMBRE_POR_DEFECTO;
        this.moduloTiendaActivo = true;
        this.minutosLimpieza = 0;
        this.porcentajeAnticipoMinimo = 0;
    }

    public String getNombreComplejo() {
        return nombreComplejo;
    }

    public void setNombreComplejo(String nombreComplejo) throws DatoInvalidoException {
        this.nombreComplejo = Validador.validarRequerido(nombreComplejo, "nombre del complejo");
    }

    /** modulo_tienda=true → mostrar funciones de tienda; false → ocultarlas. */
    public boolean isModuloTiendaActivo() {
        return moduloTiendaActivo;
    }

    public void setModuloTiendaActivo(boolean activo) {
        this.moduloTiendaActivo = activo;
    }

    public int getMinutosLimpieza() {
        return minutosLimpieza;
    }

    /** Minutos libres obligatorios entre reservas de una misma cancha (0 a 120). */
    public void setMinutosLimpieza(int minutos) throws DatoInvalidoException {
        if (minutos < 0 || minutos > 120) {
            throw new DatoInvalidoException("Los minutos de limpieza deben estar entre 0 y 120.");
        }
        this.minutosLimpieza = minutos;
    }

    public double getPorcentajeAnticipoMinimo() {
        return porcentajeAnticipoMinimo;
    }

    /** Porcentaje del total que debe estar pagado para confirmar una reserva (0 a 100). */
    public void setPorcentajeAnticipoMinimo(double porcentaje) throws DatoInvalidoException {
        if (Double.isNaN(porcentaje) || porcentaje < 0 || porcentaje > 100) {
            throw new DatoInvalidoException("El porcentaje de anticipo debe estar entre 0 y 100.");
        }
        this.porcentajeAnticipoMinimo = porcentaje;
    }

    @Override
    public String toString() {
        return "Configuración [" + nombreComplejo + ", tienda=" + moduloTiendaActivo
                + ", limpieza=" + minutosLimpieza + " min, anticipo=" + porcentajeAnticipoMinimo + "%]";
    }
}

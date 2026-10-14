/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
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
 *   clave_desarrollador=7691a81c...   
 *   color.PRIMARIO=#1E7A46           
 * 
 *
 * Estos valores permiten resolver varios "cambios en vivo" de la defensa
 * sin tocar el código: activar/desactivar la tienda, agregar tiempo de
 * limpieza entre reservas o exigir un anticipo mínimo.
 * 
 * Los colores se guardan como TEXTO ("#1E7A46"): el modelo no conoce
 * {@code java.awt.Color}, porque el color es asunto de la vista.
 */
public class ConfiguracionSistema {
    public static final String NOMBRE_POR_DEFECTO = "Complejo Deportivo";

    /** Hash SHA-256 de la clave de fábrica del panel del desarrollador: "dev2026". */
    public static final String CLAVE_DESARROLLADOR_POR_DEFECTO =
            "7691a81c8ebd9e37886813fae57afd0bea83e1d218fb89b1ea8cc90326772322";

    private static final Pattern HASH_SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern CLAVE_COLOR = Pattern.compile("^color\\.[A-Z_]+$");
    private static final Pattern VALOR_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private String nombreComplejo;
    private boolean moduloTiendaActivo;
    private int minutosLimpieza;
    private double porcentajeAnticipoMinimo;
    private String claveDesarrolladorHash;
    private final Map<String, String> colores;

    /**
     * Configuración por defecto: tienda activa, sin limpieza, sin anticipo mínimo,
     * clave de desarrollador de fábrica y colores de fábrica (mapa vacío).
     * Se usa si config.properties no existe.
     */
    public ConfiguracionSistema() {
        this.nombreComplejo = NOMBRE_POR_DEFECTO;
        this.moduloTiendaActivo = true;
        this.minutosLimpieza = 0;
        this.porcentajeAnticipoMinimo = 0;
        this.claveDesarrolladorHash = CLAVE_DESARROLLADOR_POR_DEFECTO;
        this.colores = new LinkedHashMap<>();
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

    // =====================================================================
    //  CLAVE DEL DESARROLLADOR (panel oculto Ctrl+Shift+D)
    // =====================================================================

    /** Solo para persistencia (guardar en config.properties). */
    public String getClaveDesarrolladorHash() {
        return claveDesarrolladorHash;
    }

    /** Recibe el hash SHA-256 ya calculado, nunca la clave en texto plano. */
    public void setClaveDesarrolladorHash(String hash) throws DatoInvalidoException {
        String limpio = Validador.validarRequerido(hash, "clave del desarrollador").toLowerCase(Locale.ROOT);
        if (!HASH_SHA256.matcher(limpio).matches()) {
            throw new DatoInvalidoException("La clave del desarrollador no es un hash SHA-256 válido.");
        }
        this.claveDesarrolladorHash = limpio;
    }

    /** @return true si el hash coincide con la clave guardada */
    public boolean verificarClaveDesarrollador(String hash) {
        return hash != null && claveDesarrolladorHash.equalsIgnoreCase(hash.trim());
    }

    /** @return true si todavía se usa la clave de fábrica (conviene cambiarla) */
    public boolean usaClaveDeFabrica() {
        return CLAVE_DESARROLLADOR_POR_DEFECTO.equals(claveDesarrolladorHash);
    }

    // =====================================================================
    //  COLORES DE LA INTERFAZ (texto "#RRGGBB")
    // =====================================================================

    /** @return copia de solo lectura: "color.PRIMARIO" → "#1E7A46" */
    public Map<String, String> getColores() {
        return Collections.unmodifiableMap(colores);
    }

    /**
     * Guarda un color. La clave debe tener la forma "color.NOMBRE" y el valor "#RRGGBB".
     */
    public void setColor(String clave, String valorHex) throws DatoInvalidoException {
        if (clave == null || !CLAVE_COLOR.matcher(clave.trim()).matches()) {
            throw new DatoInvalidoException("Clave de color inválida: '" + clave + "'.");
        }
        if (valorHex == null || !VALOR_COLOR.matcher(valorHex.trim()).matches()) {
            throw new DatoInvalidoException("Color inválido en '" + clave + "': '" + valorHex
                    + "'. Use el formato #RRGGBB.");
        }
        colores.put(clave.trim(), valorHex.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Reemplaza TODOS los colores ("todo o nada": si uno es inválido, no cambia ninguno).
     */
    public void setColores(Map<String, String> nuevos) throws DatoInvalidoException {
        ConfiguracionSistema prueba = new ConfiguracionSistema();
        if (nuevos != null) {
            for (Map.Entry<String, String> e : nuevos.entrySet()) {
                prueba.setColor(e.getKey(), e.getValue());
            }
        }
        colores.clear();
        colores.putAll(prueba.colores);
    }

    /** Vuelve a los colores de fábrica (sin colores guardados). */
    public void restablecerColores() {
        colores.clear();
    }

    @Override
    public String toString() {
        return "Configuración [" + nombreComplejo + ", tienda=" + moduloTiendaActivo
                + ", limpieza=" + minutosLimpieza + " min, anticipo=" + porcentajeAnticipoMinimo
                + "%, colores personalizados=" + colores.size() + "]";
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import com.canchapro.models.exceptions.DatoInvalidoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
/**
 *
 * @author SANTANA
 */

/**
 * Utilidades para montos en quetzales.
 *
 * El sistema usa {@code double} para el dinero por simplicidad. Como
 * {@code double} tiene pequeños errores de precisión (0.1 + 0.2 = 0.30000000000000004),
 * esta clase centraliza el redondeo y las comparaciones con tolerancia.
 */
public final class MonedaUtil {

    /** Diferencia máxima para considerar dos montos iguales (medio centavo). */
    public static final double TOLERANCIA = 0.005;

    private MonedaUtil() {
        // No se instancia.
    }

    /**
     * Redondea a 2 decimales (centavos) con redondeo comercial.
     * Ejemplo: 10.005 -> 10.01
     */
    public static double redondear(double monto) {
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** @return true si el monto es prácticamente cero (ej. saldo pagado). */
    public static boolean esCero(double monto) {
        return Math.abs(monto) < TOLERANCIA;
    }

    /** @return true si a es mayor que b por más de medio centavo. */
    public static boolean esMayor(double a, double b) {
        return a - b > TOLERANCIA;
    }

    /** @return true si a y b son el mismo monto (diferencia menor a medio centavo). */
    public static boolean sonIguales(double a, double b) {
        return Math.abs(a - b) < TOLERANCIA;
    }

    /**
     * Formato para pantalla. Ejemplo: 1250.5 -> "Q1,250.50"
     */
    public static String formatear(double monto) {
        String texto = String.format(Locale.US, "Q%,.2f", Math.abs(monto));
        return monto < 0 && !esCero(monto) ? "-" + texto : texto;
    }

    /**
     * Formato para CSV: siempre con punto decimal y sin separador de miles.
     * Ejemplo: 1250.5 -> "1250.50"
     *
     * Se fuerza {@code Locale.US} porque en una computadora configurada en
     * español el decimal podría salir con coma ("1250,50") y el archivo se
     * leería mal en otra máquina.
     */
    public static String aCsv(double monto) {
        return String.format(Locale.US, "%.2f", monto);
    }

    /**
     * Convierte texto a monto. Acepta "150", "150.00", "Q150", "Q1,250.50".
     *
     * @throws DatoInvalidoException si el texto está vacío o no es numérico
     */
    public static double leer(String texto) throws DatoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new DatoInvalidoException("El monto es obligatorio.");
        }
        String limpio = texto.trim()
                .replace("Q", "")
                .replace("q", "")
                .replace(",", "")
                .replace(" ", "");
        try {
            double valor = Double.parseDouble(limpio);
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new NumberFormatException();
            }
            return redondear(valor);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("Monto no numérico: '" + texto.trim() + "'.", e);
        }
    }
}
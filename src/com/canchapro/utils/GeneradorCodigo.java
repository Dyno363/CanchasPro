/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import java.util.Collection;

/**
 *
 * @author SANTANA
 */


/**
 * Genera el siguiente código correlativo para cada tipo de entidad.
 *
 * No guarda ningún contador: calcula el siguiente número a partir de los
 * códigos que ya existen en memoria. Así, al reiniciar el programa y cargar
 * los CSV, la numeración continúa correctamente sin archivos extra.
 *
 * Ejemplo: existentes [R0001, R0002, R0005] -> siguiente = R0006
 */
public final class GeneradorCodigo {

    /**
     * Cada tipo define su prefijo y la cantidad de dígitos del número.
     * Debe coincidir con los códigos de los CSV de prueba.
     */
    public enum Tipo {
        USUARIO("U", 3),
        CLIENTE("CL", 4),
        CANCHA("C", 2),
        TARIFA("T", 2),
        SERVICIO("S", 2),
        RESERVA("R", 4),
        PAGO("PG", 4),
        PRODUCTO("P", 3),
        VENTA("V", 4);

        private final String prefijo;
        private final int digitos;

        Tipo(String prefijo, int digitos) {
            this.prefijo = prefijo;
            this.digitos = digitos;
        }

        public String getPrefijo() {
            return prefijo;
        }

        public int getDigitos() {
            return digitos;
        }
    }

    private GeneradorCodigo() {
        // No se instancia.
    }

    /**
     * Calcula el siguiente código disponible.
     *
     * @param tipo        tipo de entidad (define prefijo y dígitos)
     * @param existentes  códigos que ya existen en memoria (puede estar vacío)
     * @return el siguiente código, ej. "CL0005"
     */
    public static String siguiente(Tipo tipo, Collection<String> existentes) {
        int mayor = 0;
        if (existentes != null) {
            for (String codigo : existentes) {
                int numero = extraerNumero(tipo.getPrefijo(), codigo);
                if (numero > mayor) {
                    mayor = numero;
                }
            }
        }
        return formatear(tipo, mayor + 1);
    }

    /** Arma el código con ceros a la izquierda: (CLIENTE, 7) -> "CL0007". */
    public static String formatear(Tipo tipo, int numero) {
        return tipo.getPrefijo() + String.format("%0" + tipo.getDigitos() + "d", numero);
    }

    /**
     * Obtiene la parte numérica de un código. Si no corresponde al prefijo
     * o la parte final no es numérica, devuelve 0 (se ignora).
     *
     * Ejemplo: con prefijo "C", el código "CL0001" se ignora porque
     * "L0001" no es un número. Así no se confunden canchas con clientes.
     */
    private static int extraerNumero(String prefijo, String codigo) {
        if (codigo == null || !codigo.startsWith(prefijo)) {
            return 0;
        }
        String parteNumerica = codigo.substring(prefijo.length());
        if (parteNumerica.isEmpty() || !parteNumerica.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        try {
            return Integer.parseInt(parteNumerica);
        } catch (NumberFormatException e) {
            return 0; // número demasiado grande
        }
    }
}

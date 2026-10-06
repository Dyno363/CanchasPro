/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import com.canchapro.models.exceptions.DatoInvalidoException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
/**
 *
 * @author SANTANA
 */

/**
 * Validaciones de datos de entrada (formato), reutilizables por el modelo
 * y los controladores.
 *
 * Ojo con la diferencia: esta clase valida formato ("¿el teléfono
 * tiene 8 dígitos?"). Las reglas de negocio("¿el pago supera el
 * saldo?") viven en las clases del modelo.
 *
 * Convención de nombres:
 * 
 *   {@code esXxx(...)}: pregunta, devuelve boolean y nunca lanza excepción.
 *   {@code validarXxx(...)}: exige, devuelve el valor limpio o lanza DatoInvalidoException.
 * 
 */
public final class Validador {

    /** Teléfono de Guatemala: 8 dígitos (después de quitar espacios y guiones). */
    private static final Pattern TELEFONO = Pattern.compile("^\\d{8}$");

    /** Correo simple: algo@dominio.ext */
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validador() {
        // No se instancia.
    }

    // =====================================================================
    //  PREGUNTAS (no lanzan excepción)
    // =====================================================================

    public static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean esTelefonoValido(String telefono) {
        return !esVacio(telefono) && TELEFONO.matcher(normalizarTelefono(telefono)).matches();
    }

    public static boolean esCorreoValido(String correo) {
        return !esVacio(correo) && CORREO.matcher(correo.trim()).matches();
    }

    // =====================================================================
    //  VALIDACIONES (lanzan DatoInvalidoException)
    // =====================================================================

    /**
     * Exige que un texto tenga contenido.
     *
     * @param valor  texto a validar
     * @param campo  nombre del campo para el mensaje (ej. "nombre")
     * @return el texto sin espacios al inicio ni al final
     */
    public static String validarRequerido(String valor, String campo) throws DatoInvalidoException {
        if (esVacio(valor)) {
            throw new DatoInvalidoException("El campo '" + campo + "' es obligatorio.");
        }
        return valor.trim();
    }

    /**
     * @return el teléfono normalizado (solo dígitos), ej. "4411-2233" -> "44112233"
     */
    public static String validarTelefono(String telefono) throws DatoInvalidoException {
        if (!esTelefonoValido(telefono)) {
            throw new DatoInvalidoException("Teléfono inválido: debe tener 8 dígitos.");
        }
        return normalizarTelefono(telefono);
    }

    /**
     * El correo es opcional: vacío es válido y devuelve "".
     * Si se escribe algo, debe tener formato correcto.
     */
    public static String validarCorreoOpcional(String correo) throws DatoInvalidoException {
        if (esVacio(correo)) {
            return "";
        }
        if (!esCorreoValido(correo)) {
            throw new DatoInvalidoException("Correo electrónico inválido: '" + correo.trim() + "'.");
        }
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    /** Exige un número mayor que cero (precios, montos de pago). */
    public static double validarPositivo(double valor, String campo) throws DatoInvalidoException {
        if (valor <= 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return valor;
    }

    /** Exige un número mayor o igual que cero (existencias, stock mínimo). */
    public static int validarNoNegativo(int valor, String campo) throws DatoInvalidoException {
        if (valor < 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' no puede ser negativo.");
        }
        return valor;
    }

    /** Exige un entero mayor que cero (cantidades, capacidad). */
    public static int validarEnteroPositivo(int valor, String campo) throws DatoInvalidoException {
        if (valor <= 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return valor;
    }

    /** Convierte texto a entero. */
    public static int leerEntero(String texto, String campo) throws DatoInvalidoException {
        String valor = validarRequerido(texto, campo);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo '" + campo + "' debe ser un número entero: '" + valor + "'.", e);
        }
    }

    /** Convierte "true"/"false" (sin importar mayúsculas). Cualquier otro texto es error. */
    public static boolean leerBooleano(String texto, String campo) throws DatoInvalidoException {
        String valor = validarRequerido(texto, campo).toLowerCase(Locale.ROOT);
        if (valor.equals("true")) {
            return true;
        }
        if (valor.equals("false")) {
            return false;
        }
        throw new DatoInvalidoException("El campo '" + campo + "' debe ser true o false: '" + valor + "'.");
    }

    // =====================================================================
    //  BÚSQUEDA DE TEXTO
    // =====================================================================

    /**
     * Búsqueda flexible: ignora mayúsculas y tildes.
     * Ejemplo: contiene("Carlos Pérez", "perez") -> true
     *
     * Se usa para buscar clientes por nombre y evitar duplicados.
     */
    public static boolean contiene(String texto, String criterio) {
        if (esVacio(texto) || esVacio(criterio)) {
            return false;
        }
        return normalizarTexto(texto).contains(normalizarTexto(criterio));
    }

    /** "Pérez  " -> "perez" (minúsculas, sin tildes, sin espacios extremos). */
    public static String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    /** Deja solo los dígitos: "4411-2233" -> "44112233". */
    public static String normalizarTelefono(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\D", "");
    }
}

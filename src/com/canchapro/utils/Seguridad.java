/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
/**
 *
 * @author SANTANA
 */



/**
 * Manejo seguro de contraseñas mediante hash SHA-256.
 *
 * Las contraseñas NUNCA se guardan en texto plano en usuarios.csv.
 * Se guarda su "huella" (hash). Para iniciar sesión, se calcula el hash de lo
 * que el usuario escribió y se compara con el hash guardado.
 *
 * Ejemplo: "admin123" -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
 *
 * Limitación académica: no se usa "sal" (salt). En un sistema real se usaría
 * un algoritmo con sal como BCrypt para impedir ataques con tablas precalculadas.
 */
public final class Seguridad {

    private Seguridad() {
        // No se instancia.
    }

    /**
     * Calcula el hash SHA-256 de un texto y lo devuelve en hexadecimal (64 caracteres).
     */
    public static String hashSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return aHexadecimal(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 viene incluido en toda máquina virtual de Java; esto no debería ocurrir.
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", e);
        }
    }

    /**
     * Versión para el campo de contraseña de Swing: {@code JPasswordField.getPassword()}
     * devuelve un char[]. Después de calcular el hash, el arreglo se borra de memoria.
     */
    public static String hashContrasena(char[] contrasena) {
        try {
            return hashSha256(new String(contrasena));
        } finally {
            Arrays.fill(contrasena, '\0');
        }
    }

    /**
     * Compara una contraseña escrita con el hash guardado.
     *
     * Se usa {@code MessageDigest.isEqual} en vez de {@code equals} porque compara
     * en tiempo constante (no revela por cuánto tiempo coincidieron los caracteres).
     */
    public static boolean verificar(String contrasenaPlana, String hashGuardado) {
        if (contrasenaPlana == null || hashGuardado == null) {
            return false;
        }
        byte[] calculado = hashSha256(contrasenaPlana).getBytes(StandardCharsets.UTF_8);
        byte[] guardado = hashGuardado.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculado, guardado);
    }

    private static String aHexadecimal(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

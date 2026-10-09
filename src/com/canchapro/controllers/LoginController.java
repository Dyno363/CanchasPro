/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Seguridad;
import com.canchapro.utils.Validador;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del inicio de sesión.
 *
 * Flujo MVC: LoginView lee los campos → llama a {@link #iniciarSesion} →
 * si todo está bien recibe una {@link Sesion} y abre el Dashboard; si no,
 * muestra el mensaje de la excepción. La vista NO compara contraseñas ni
 * busca usuarios: eso pasa aquí, contra los usuarios EN MEMORIA.
 *
 * Reglas de seguridad:
 * 
 *   La contraseña nunca se guarda ni se compara en texto plano: se convierte
 *       a hash SHA-256 y el arreglo char[] se borra de inmediato.
 *   El mensaje de error es el mismo si falla el usuario o la contraseña,
 *       para no revelar qué usuarios existen.
 *   Máximo {@value #MAX_INTENTOS} intentos fallidos; después el login queda
 *       bloqueado y la vista cierra el programa.
 * 
 */
public class LoginController {
    /** Intentos fallidos permitidos antes de bloquear el inicio de sesión. */
    public static final int MAX_INTENTOS = 3;

    private final ComplejoDeportivo complejo;
    private int intentosFallidos;

    /**
     * @param complejo sistema ya cargado en memoria desde los CSV
     */
    public LoginController(ComplejoDeportivo complejo) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.intentosFallidos = 0;
    }

    /**
     * Valida las credenciales y abre la sesión.
     *
     * @param username   lo escrito en el campo usuario
     * @param contrasena {@code JPasswordField.getPassword()}; se borra al terminar
     * @return la sesión del usuario autenticado
     * @throws CanchaProException con un mensaje listo para mostrar al operador
     */
    public Sesion iniciarSesion(String username, char[] contrasena) throws CanchaProException {
        try {
            if (isBloqueado()) {
                throw new CanchaProException("Se superó el máximo de " + MAX_INTENTOS
                        + " intentos. Reinicie el programa.");
            }
            // Campos vacíos: es un descuido, no un intento; no se cuenta.
            if (Validador.esVacio(username) || contrasena == null || contrasena.length == 0) {
                throw new CanchaProException("Ingrese el usuario y la contraseña.");
            }

            String hash = Seguridad.hashContrasena(contrasena); // también borra el char[]
            Optional<Usuario> encontrado = complejo.buscarUsuarioPorUsername(username);

            if (encontrado.isPresent()) {
                Usuario usuario = encontrado.get();
                if (usuario.validarCredenciales(username, hash)) {
                    intentosFallidos = 0;
                    return new Sesion(usuario, FechaUtil.ahora());
                }
                // Contraseña correcta pero cuenta desactivada: se informa, sin contar intento.
                if (!usuario.isActivo() && usuario.getPasswordHash().equalsIgnoreCase(hash)) {
                    throw new CanchaProException("El usuario '" + usuario.getUsername()
                            + "' está desactivado. Consulte al administrador.");
                }
            }

            intentosFallidos++;
            throw new CanchaProException(mensajeDeFallo());
        } finally {
            if (contrasena != null) {
                Arrays.fill(contrasena, '\0'); // por si salió antes de calcular el hash
            }
        }
    }

    /** @return intentos fallidos acumulados */
    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    /** @return intentos que quedan antes del bloqueo (nunca negativo) */
    public int getIntentosRestantes() {
        return Math.max(0, MAX_INTENTOS - intentosFallidos);
    }

    /** La vista lo consulta después de un error: si es true, cierra el programa. */
    public boolean isBloqueado() {
        return intentosFallidos >= MAX_INTENTOS;
    }

    /** Nombre del complejo para el título de la ventana de login (sale de config.properties). */
    public String getNombreComplejo() {
        return complejo.getNombre();
    }

    private String mensajeDeFallo() {
        int restantes = getIntentosRestantes();
        if (restantes == 0) {
            return "Usuario o contraseña incorrectos. Se agotaron los " + MAX_INTENTOS
                    + " intentos; el sistema se cerrará.";
        }
        return "Usuario o contraseña incorrectos. Le quedan " + restantes
                + (restantes == 1 ? " intento." : " intentos.");
    }
}

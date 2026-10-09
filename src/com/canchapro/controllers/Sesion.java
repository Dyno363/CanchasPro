/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.FechaUtil;
import java.time.LocalDateTime;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Sesión de trabajo del operador que inició sesión.
 *
 * No es una variable global. El {@link LoginController} la crea cuando
 * las credenciales son correctas y luego se entrega por constructor a cada
 * controlador que la necesite (por ejemplo, para saber QUIÉN registró una
 * reserva o QUIÉN canceló).
 *
 * El constructor es de paquete: fuera de {@code controllers} nadie puede
 * "fabricar" una sesión sin pasar por el login.
 */
public class Sesion {
    private final Usuario usuario;
    private final LocalDateTime inicio;

    /**
     * Solo la crea el LoginController.
     *
     * @param usuario usuario autenticado
     * @param inicio  momento en que inició la sesión
     */
    Sesion(Usuario usuario, LocalDateTime inicio) {
        this.usuario = Objects.requireNonNull(usuario, "La sesión necesita un usuario.");
        this.inicio = Objects.requireNonNull(inicio, "La sesión necesita la hora de inicio.");
    }

    /** @return el usuario autenticado (se usa como "responsable" en reservas, pagos y ventas) */
    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public RolUsuario getRol() {
        return usuario.getRol();
    }

    /** @return true si el usuario de la sesión es administrador */
    public boolean esAdministrador() {
        return usuario.esAdministrador();
    }

    /**
     * Control de permisos para acciones delicadas (usuarios, tarifas, configuración).
     * Los controladores lo llaman al inicio del método protegido.
     *
     * @param accion lo que se intenta hacer, en infinitivo (ej. "crear usuarios")
     * @throws CanchaProException si el usuario no es administrador
     */
    public void exigirAdministrador(String accion) throws CanchaProException {
        if (!esAdministrador()) {
            throw new CanchaProException("Solo un administrador puede " + accion + ".");
        }
    }

    /**
     * Texto para la barra superior del dashboard.
     * Ejemplo: "Administrador General (Administrador) - desde 08/10/2026 17:05"
     */
    public String getResumen() {
        return usuario.getNombre() + " (" + usuario.getRol().getDescripcion() + ") - desde "
                + FechaUtil.formatearFechaHora(inicio);
    }

    @Override
    public String toString() {
        return getResumen();
    }
}

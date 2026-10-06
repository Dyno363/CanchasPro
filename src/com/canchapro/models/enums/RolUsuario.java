/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.canchapro.models.enums;

/**
 *
 * @author SANTANA
 */
/**
 * Roles de los operadores que inician sesión.
 *
 * ADMINISTRADOR: acceso total (usuarios, configuración, canchas, tarifas).
 * OPERADOR: trabajo diario (reservas, pagos, clientes, tienda).
 */
public enum RolUsuario {

    ADMINISTRADOR("Administrador"),
    OPERADOR("Operador");

    private final String descripcion;

    RolUsuario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

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
 * Estado de un producto de la tienda.
 *
 * Un producto INACTIVO no se elimina (se conserva en el historial de
 * ventas), simplemente deja de aparecer en el punto de venta.
 */
public enum EstadoProducto {

    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String descripcion;

    EstadoProducto(String descripcion) {
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

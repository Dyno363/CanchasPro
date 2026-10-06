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
 * Categorías de productos del módulo Tienda / Cafetería.
 *
 * Permite el reporte avanzado "ingresos por categoría".
 */
public enum CategoriaProducto {

    BEBIDA("Bebida"),
    SNACK("Snack"),
    GOLOSINA("Golosina"),
    COMIDA_PREPARADA("Comida preparada");

    private final String descripcion;

    CategoriaProducto(String descripcion) {
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

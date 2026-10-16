/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.canchapro.views.panels;

/**
 *
 * Lo que el Dashboard necesita de CUALQUIER pantalla del menú lateral
 * (Clientes, Usuarios, Canchas, Tarifas, Servicios, Tienda, Reportes, Configuración).
 *
 * Gracias a esta interfaz el Dashboard no pregunta "¿eres Clientes? ¿eres
 * Tarifas?": a todas les dice lo mismo. Es polimorfismo: cada panel decide
 * CÓMO recargarse y CÓMO pintarse.
 *
 * @author SANTANA
 */
public interface PanelModulo {
    /** Vuelve a pedir los datos a su controlador (se llama cada vez que se abre el módulo). */
    void recargar();

    /** Vuelve a aplicar los colores del Tema (cuando el desarrollador los cambia). */
    void aplicarTema();
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.canchapro.views.components;

/**
 *
 * @author SANTANA
 */
/**
 * Módulos que aparecen en el {@link MenuLateral}
 *
 * El menú se arma recorriendo {@code ModuloMenu.values()}: para agregar un
 * módulo nuevo basta con agregar una constante aquí; el menú lo dibuja solo.
 *
 * Es un enum de la VISTA (no del modelo): solo describe botones del menú.
 */
public enum ModuloMenu {
    // ----- Fuera de los grupos (botón superior)
    INICIO(null, "Inicio", "Reservas y mapa de canchas"),

    // ----- Personas
    CLIENTES("Personas", "Clientes", "Registrar y buscar clientes"),
    USUARIOS("Personas", "Usuarios", "Operadores y administradores"),

    // ----- Negocio
    CANCHAS("Negocio", "Canchas", "Canchas y horarios de atención"),
    TARIFAS("Negocio", "Tarifas", "Franjas horarias y fechas especiales"),
    SERVICIOS("Negocio", "Servicios", "Balones, chalecos, árbitros"),
    TIENDA("Negocio", "Tienda", "Productos y ventas de cafetería"),
    REPORTES("Negocio", "Reportes", "Ocupación, ingresos y más"),

    // ----- Sistema
    CONFIGURACION("Sistema", "Configuración", "Parámetros del complejo"),
    GUARDAR("Sistema", "Guardar", "Guardar Sistema en los CSV"),

    // ----- Fuera de los grupos (botón inferior)
    CERRAR_SESION(null, "Cerrar sesión", "Volver a la pantalla de inicio");

    private final String grupo;
    private final String texto;
    private final String ayuda;

    ModuloMenu(String grupo, String texto, String ayuda) {
        this.grupo = grupo;
        this.texto = texto;
        this.ayuda = ayuda;
    }

    /** @return nombre del grupo (tarjeta) o null si va fuera de los grupos */
    public String getGrupo() {
        return grupo;
    }

    public String getTexto() {
        return texto;
    }

    /** Texto del tooltip al pasar el mouse. */
    public String getAyuda() {
        return ayuda;
    }

    @Override
    public String toString() {
        return texto;
    }
}

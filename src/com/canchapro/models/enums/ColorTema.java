/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */

package com.canchapro.models.enums;

/**
 * Lista de TODOS los colores configurables de la interfaz.
 *
 * Cada constante trae su color por defecto, el grupo al que pertenece y una
 * descripción. El panel del desarrollador recorre {@code ColorTema.values()}
 * para mostrar el editor de colores: si mañana se agrega un color nuevo aquí,
 * aparece solo en el editor, sin tocar la pantalla.
 *
 * En config.properties se guardan como {@code color.PRIMARIO=#1E7A46}.
 */
public enum ColorTema {
    // ----- General
    PRIMARIO("General", "Color de marca (botones principales, encabezados)", 0x1E7A46),
    PRIMARIO_OSCURO("General", "Color de marca oscuro (botón presionado)", 0x155C34),
    PRIMARIO_CLARO("General", "Fila seleccionada en tablas", 0xE3F2E9),
    FONDO("General", "Fondo de las ventanas", 0xF3F5F7),
    SUPERFICIE("General", "Fondo de tarjetas y paneles", 0xFFFFFF),
    BORDE("General", "Bordes y líneas", 0xD5DAE1),
    TEXTO("General", "Texto principal", 0x1F2933),
    TEXTO_SECUNDARIO("General", "Texto de ayuda", 0x616E7C),
    TEXTO_INVERSO("General", "Texto sobre fondos oscuros", 0xFFFFFF),

    // ----- Panel lateral
    MENU_FONDO("Panel lateral", "Fondo del panel lateral", 0x1F2A37),
    MENU_TARJETA("Panel lateral", "Fondo de cada grupo de módulos", 0x2C3A4B),
    MENU_TEXTO("Panel lateral", "Texto del panel lateral", 0xD9E2EC),

    // ----- Mensajes
    EXITO("Mensajes", "Éxito / pagado / finalizado", 0x2E9E5B),
    ADVERTENCIA("Mensajes", "Advertencia / pendiente", 0xE0A100),
    ERROR("Mensajes", "Error / cancelado / eliminar", 0xC62828),
    INFO("Mensajes", "Información / confirmado", 0x2F6FDE),

    // ----- Mapa dinámico
    CANCHA_DISPONIBLE("Mapa de canchas", "Cancha disponible", 0x2E9E5B),
    CANCHA_EN_CURSO("Mapa de canchas", "Cancha con partido en curso", 0xE4572E),
    CANCHA_MANTENIMIENTO("Mapa de canchas", "Cancha en mantenimiento", 0xE0A100),
    CANCHA_INACTIVA("Mapa de canchas", "Cancha inactiva", 0x7B8794);

    private final String grupo;
    private final String descripcion;
    private final int rgbPorDefecto;

    ColorTema(String grupo, String descripcion, int rgbPorDefecto) {
        this.grupo = grupo;
        this.descripcion = descripcion;
        this.rgbPorDefecto = rgbPorDefecto;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** @return el color de fábrica en formato 0xRRGGBB */
    public int getRgbPorDefecto() {
        return rgbPorDefecto;
    }

    /** Clave en config.properties, ej. "color.PRIMARIO". */
    public String getClave() {
        return "color." + name();
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.exceptions.DatoInvalidoException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
/**
 * Tema visual de CanchaPro: colores, fuentes, medidas y estilos de componentes.
 *
 * Un solo lugar para el aspecto de toda la aplicación. Ninguna ventana
 * escribe {@code new Color(...)} ni {@code new Font(...)}: todas piden el color
 * con {@code Tema.color(ColorTema.PRIMARIO)}.
 *
 * Colores editables: la paleta vive en memoria en un {@link EnumMap}.
 * Al iniciar se carga desde config.properties ({@link #cargarColores}); el panel
 * del desarrollador la modifica ({@link #cambiarColor}); al cerrar se exporta
 * de nuevo a config.properties ({@link #exportarColores}). Es la misma regla de
 * oro que el resto del sistema: archivo → memoria → archivo.
 *
 * Solo contiene ASPECTO, nunca reglas de negocio.
 */
public final class Tema {
    /** Paleta actual (en memoria). Empieza con los colores de fábrica. */
    private static final Map<ColorTema, Color> PALETA = new EnumMap<>(ColorTema.class);

    static {
        restablecerColores();
    }

    private Tema() {
        // Solo métodos estáticos.
    }

    // =====================================================================
    //  PALETA DE COLORES (editable)
    // =====================================================================

    /** Color actual de un elemento de la interfaz. */
    public static Color color(ColorTema clave) {
        return PALETA.get(clave);
    }

    /** Cambia un color en memoria (lo usa el panel del desarrollador). */
    public static void cambiarColor(ColorTema clave, Color nuevo) {
        if (clave != null && nuevo != null) {
            PALETA.put(clave, nuevo);
        }
    }

    /** Vuelve todos los colores a los de fábrica. */
    public static void restablecerColores() {
        for (ColorTema c : ColorTema.values()) {
            PALETA.put(c, new Color(c.getRgbPorDefecto()));
        }
    }

    /**
     * Carga los colores guardados (ej. {"color.PRIMARIO" → "#1E7A46"}).
     * Las claves desconocidas o con formato inválido se ignoran y ese color
     * queda con su valor de fábrica: un archivo mal editado nunca impide arrancar.
     *
     * @return cuántos colores se aplicaron
     */
    public static int cargarColores(Map<String, String> guardados) {
        restablecerColores();
        int aplicados = 0;
        if (guardados == null) {
            return 0;
        }
        for (ColorTema c : ColorTema.values()) {
            String hex = guardados.get(c.getClave());
            if (hex != null) {
                try {
                    PALETA.put(c, deHex(hex));
                    aplicados++;
                } catch (DatoInvalidoException e) {
                    // Se conserva el color de fábrica.
                }
            }
        }
        return aplicados;
    }

    /** @return la paleta actual como texto, lista para guardarse en config.properties */
    public static Map<String, String> exportarColores() {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (ColorTema c : ColorTema.values()) {
            resultado.put(c.getClave(), aHex(PALETA.get(c)));
        }
        return resultado;
    }

    /** Color → "#1E7A46". */
    public static String aHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** "#1E7A46" o "1E7A46" → Color. */
    public static Color deHex(String texto) throws DatoInvalidoException {
        String limpio = texto == null ? "" : texto.trim().replace("#", "");
        if (!limpio.matches("[0-9A-Fa-f]{6}")) {
            throw new DatoInvalidoException("Color inválido: '" + texto + "'. Use el formato #RRGGBB.");
        }
        return new Color(Integer.parseInt(limpio, 16));
    }

    // =====================================================================
    //  FUENTES Y MEDIDAS (fijas)
    // =====================================================================

    /** "Segoe UI" en Windows; si no existe, la fuente sans-serif de Java. */
    public static final String FAMILIA = elegirFamilia("Segoe UI", "Helvetica Neue", "Arial");

    public static final Font FUENTE_TITULO = new Font(FAMILIA, Font.BOLD, 22);
    public static final Font FUENTE_SUBTITULO = new Font(FAMILIA, Font.BOLD, 16);
    public static final Font FUENTE_NORMAL = new Font(FAMILIA, Font.PLAIN, 13);
    public static final Font FUENTE_NEGRITA = new Font(FAMILIA, Font.BOLD, 13);
    public static final Font FUENTE_PEQUENA = new Font(FAMILIA, Font.PLAIN, 11);
    /** Números grandes de las tarjetas indicadoras del dashboard. */
    public static final Font FUENTE_INDICADOR = new Font(FAMILIA, Font.BOLD, 26);

    public static final int MARGEN = 16;
    public static final int ESPACIO = 8;
    public static final int ANCHO_MENU = 240;
    public static final int ALTO_BOTON = 36;
    public static final int ALTO_FILA_TABLA = 28;
    public static final int ANCHO_TARJETA_CANCHA = 200;
    public static final int ALTO_TARJETA_CANCHA = 130;

    // =====================================================================
    //  CONFIGURACIÓN GLOBAL (Main la llama antes de abrir ventanas)
    // =====================================================================

    /**
     * Look and Feel multiplataforma (Metal), para que los colores de los botones
     * se vean igual en cualquier sistema operativo, y valores por defecto de Swing.
     * Se vuelve a llamar después de cambiar colores.
     */
    public static void aplicar() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Si falla, Swing usa su aspecto por defecto; no es un error grave.
        }
        String[] componentes = {"Label", "Button", "TextField", "PasswordField", "ComboBox", "Spinner",
            "Table", "TableHeader", "TabbedPane", "CheckBox", "RadioButton", "List", "TextArea",
            "OptionPane", "Menu", "MenuItem", "ToolTip"};
        for (String c : componentes) {
            UIManager.put(c + ".font", FUENTE_NORMAL);
        }
        UIManager.put("Panel.background", color(ColorTema.FONDO));
        UIManager.put("OptionPane.background", color(ColorTema.FONDO));
        UIManager.put("Table.selectionBackground", color(ColorTema.PRIMARIO_CLARO));
        UIManager.put("Table.selectionForeground", color(ColorTema.TEXTO));
        UIManager.put("TextField.selectionBackground", color(ColorTema.PRIMARIO_CLARO));
        UIManager.put("TabbedPane.selected", color(ColorTema.SUPERFICIE));
        UIManager.put("ToolTip.background", color(ColorTema.SUPERFICIE));
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(color(ColorTema.BORDE)));
    }

    // =====================================================================
    //  COLORES SEGÚN ESTADO
    // =====================================================================

    /** Color de la "etiqueta" de estado en la tabla de reservas. */
    public static Color colorEstadoReserva(EstadoReserva estado) {
        if (estado == null) {
            return color(ColorTema.TEXTO_SECUNDARIO);
        }
        switch (estado) {
            case PENDIENTE:  return color(ColorTema.ADVERTENCIA);
            case CONFIRMADA: return color(ColorTema.INFO);
            case EN_CURSO:   return color(ColorTema.CANCHA_EN_CURSO);
            case FINALIZADA: return color(ColorTema.EXITO);
            case CANCELADA:  return color(ColorTema.ERROR);
            default:         return color(ColorTema.TEXTO_SECUNDARIO);
        }
    }

    /** Color del estado administrativo de una cancha (tabla de canchas). */
    public static Color colorEstadoCancha(EstadoCancha estado) {
        if (estado == null) {
            return color(ColorTema.TEXTO_SECUNDARIO);
        }
        switch (estado) {
            case DISPONIBLE:    return color(ColorTema.CANCHA_DISPONIBLE);
            case MANTENIMIENTO: return color(ColorTema.CANCHA_MANTENIMIENTO);
            default:            return color(ColorTema.CANCHA_INACTIVA);
        }
    }

    /**
     * Texto oscuro o blanco según qué se lee mejor sobre ese fondo.
     * Usa la luminancia percibida (0.299 R + 0.587 G + 0.114 B).
     */
    public static Color textoSobre(Color fondo) {
        double luminancia = (0.299 * fondo.getRed() + 0.587 * fondo.getGreen() + 0.114 * fondo.getBlue()) / 255;
        return luminancia > 0.6 ? color(ColorTema.TEXTO) : Color.WHITE;
    }

    /** Versión más oscura de un color (bordes y efecto al pasar el mouse). */
    public static Color oscurecer(Color color, double factor) {
        return new Color((int) (color.getRed() * (1 - factor)), (int) (color.getGreen() * (1 - factor)),
                (int) (color.getBlue() * (1 - factor)));
    }

    // =====================================================================
    //  ESTILOS DE COMPONENTES (las vistas los llaman en su constructor)
    // =====================================================================

    /** Botón de la acción principal (Ingresar, Guardar, Reservar). */
    public static void botonPrimario(JButton boton) {
        botonPlano(boton, color(ColorTema.PRIMARIO), textoSobre(color(ColorTema.PRIMARIO)));
    }

    /** Botón de acción secundaria (Cancelar, Limpiar, Volver). */
    public static void botonSecundario(JButton boton) {
        botonPlano(boton, color(ColorTema.SUPERFICIE), color(ColorTema.TEXTO));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color(ColorTema.BORDE)), BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        boton.setBorderPainted(true);
    }

    /** Botón de acción destructiva (Cancelar reserva, Eliminar). */
    public static void botonPeligro(JButton boton) {
        botonPlano(boton, color(ColorTema.ERROR), textoSobre(color(ColorTema.ERROR)));
    }

    /** Botón pequeño y redondeado de los grupos del panel lateral. */
    public static void botonMenu(JButton boton) {
        botonPlano(boton, color(ColorTema.SUPERFICIE), color(ColorTema.TEXTO));
        boton.setFont(FUENTE_PEQUENA);
        boton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    private static void botonPlano(JButton boton, Color fondo, Color texto) {
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFont(FUENTE_NEGRITA);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Campo de texto con borde suave y espacio interno. */
    public static void campo(JTextField campo) {
        campo.setFont(FUENTE_NORMAL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color(ColorTema.BORDE)), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }

    /** Título de pantalla (ej. "Gestión de reservas"). */
    public static void titulo(JLabel etiqueta) {
        etiqueta.setFont(FUENTE_TITULO);
        etiqueta.setForeground(color(ColorTema.TEXTO));
    }

    /** Texto de ayuda o secundario. */
    public static void textoSecundario(JLabel etiqueta) {
        etiqueta.setFont(FUENTE_PEQUENA);
        etiqueta.setForeground(color(ColorTema.TEXTO_SECUNDARIO));
    }

    /** Tabla con filas altas, encabezado de color y sin líneas verticales. */
    public static void tabla(JTable tabla) {
        tabla.setRowHeight(ALTO_FILA_TABLA);
        tabla.setFont(FUENTE_NORMAL);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(color(ColorTema.BORDE));
        tabla.setSelectionBackground(color(ColorTema.PRIMARIO_CLARO));
        tabla.setSelectionForeground(color(ColorTema.TEXTO));
        tabla.setFillsViewportHeight(true);
        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setFont(FUENTE_NEGRITA);
        encabezado.setBackground(color(ColorTema.PRIMARIO));
        encabezado.setForeground(textoSobre(color(ColorTema.PRIMARIO)));
        encabezado.setReorderingAllowed(false);
    }

    /** Panel tipo "tarjeta": fondo blanco, borde fino y margen interno. */
    public static void tarjeta(JComponent panel) {
        panel.setOpaque(true);
        panel.setBackground(color(ColorTema.SUPERFICIE));
        panel.setBorder(bordeTarjeta());
    }

    public static Border bordeTarjeta() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color(ColorTema.BORDE)),
                BorderFactory.createEmptyBorder(MARGEN, MARGEN, MARGEN, MARGEN));
    }

    /** Margen vacío estándar alrededor de un panel. */
    public static Border margen() {
        return BorderFactory.createEmptyBorder(MARGEN, MARGEN, MARGEN, MARGEN);
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    /** Primera familia de fuente instalada, o "SansSerif" (siempre existe en Java). */
    private static String elegirFamilia(String... preferidas) {
        try {
            String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String familia : preferidas) {
                if (Arrays.asList(instaladas).contains(familia)) {
                    return familia;
                }
            }
        } catch (RuntimeException e) {
            // Entorno sin fuentes del sistema: se usa la genérica.
        }
        return Font.SANS_SERIF;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import com.canchapro.utils.Tema;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Locale;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
/**
 * * Dibuja el valor de una celda como una "píldora" de color (ACTIVO, EN MANTENIMIENTO…).
 * Solo cambia cómo SE VE la celda: el valor real (enum, boolean) no se toca,
 * así la columna se sigue ordenando bien.
 * @author SANTANA
 */
public class RenderizadorPildora extends DefaultTableCellRenderer{
    private static final long serialVersionUID = 1L;

    private final transient Function<Object, String> texto;
    private final transient Function<Object, Color> color;

    /**
     * @param texto qué palabra mostrar para cada valor
     * @param color de qué color pintar la píldora para cada valor
     */
    public RenderizadorPildora(Function<Object, String> texto, Function<Object, Color> color) {
        this.texto = texto;
        this.color = color;
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean foco, int fila, int columna) {
        JLabel l = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
        Color fondo = color.apply(valor);
        l.setText(texto.apply(valor).toUpperCase(Locale.ROOT));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(Tema.FUENTE_PEQUENA.deriveFont(Font.BOLD));
        l.setBackground(fondo);
        l.setForeground(Tema.textoSobre(fondo));
        l.setBorder(BorderFactory.createMatteBorder(5, 8, 5, 8, tabla.getBackground()));
        l.setOpaque(true);
        return l;
    }
}

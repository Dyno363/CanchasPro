/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.utils.Tema;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
/**
 * Tarjeta de indicador (KPI) del Dashboard, construida por código.
 *
 * 
 *  ▌ RESERVAS HOY
 *  ▌ 12
 *  ▌ 3 pendientes de pago
 * 
 *
 * La barra de la izquierda usa un {@link ColorTema} (no un Color fijo), así
 * respeta los colores que elija el desarrollador.
 *
 * Solo MUESTRA valores ya calculados: el número lo calcula el controlador
 * y el Dashboard se lo pasa con {@link #setValor} y {@link #setDetalle}.
 */
public class TarjetaIndicador extends JPanel{
    private static final long serialVersionUID = 1L;
    private static final int ANCHO_BARRA = 5;
    private static final int RADIO = 12;

    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblValor = new JLabel("-");
    private final JLabel lblDetalle = new JLabel(" ");
    private ColorTema acento;

    /**
     * @param titulo ej. "Reservas hoy"
     * @param acento color de la barra lateral (ej. ColorTema.INFO)
     */
    public TarjetaIndicador(String titulo, ColorTema acento) {
        this.acento = Objects.requireNonNull(acento, "El color de acento es obligatorio.");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false); // el fondo redondeado lo pinta paintComponent
        setBorder(BorderFactory.createEmptyBorder(12, 12 + ANCHO_BARRA, 12, 12));
        setPreferredSize(new Dimension(190, 96));

        lblTitulo.setText(titulo.toUpperCase(java.util.Locale.ROOT));
        lblTitulo.setFont(Tema.FUENTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
        lblValor.setFont(Tema.FUENTE_INDICADOR);
        lblDetalle.setFont(Tema.FUENTE_PEQUENA);

        add(lblTitulo);
        add(lblValor);
        add(lblDetalle);
        aplicarTema();
    }

    /** Valor principal ya formateado (ej. "12" o "Q 1,250.00"). */
    public void setValor(String valor) {
        lblValor.setText(valor == null || valor.isEmpty() ? "-" : valor);
    }

    /** Texto pequeño debajo del valor (ej. "3 pendientes de pago"). */
    public void setDetalle(String detalle) {
        lblDetalle.setText(detalle == null || detalle.isEmpty() ? " " : detalle);
    }

    /** Cambia el color de la barra (ej. a ERROR si hay saldo pendiente). */
    public void setAcento(ColorTema acento) {
        this.acento = Objects.requireNonNull(acento);
        repaint();
    }

    /** Vuelve a leer los colores del Tema (después de que el desarrollador los cambie). */
    public final void aplicarTema() {
        lblTitulo.setForeground(Tema.color(ColorTema.TEXTO_SECUNDARIO));
        lblValor.setForeground(Tema.color(ColorTema.TEXTO));
        lblDetalle.setForeground(Tema.color(ColorTema.TEXTO_SECUNDARIO));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        // Fondo y borde
        g2.setColor(Tema.color(ColorTema.SUPERFICIE));
        g2.fillRoundRect(0, 0, w, h, RADIO, RADIO);
        g2.setColor(Tema.color(ColorTema.BORDE));
        g2.drawRoundRect(0, 0, w, h, RADIO, RADIO);
        // Barra de color a la izquierda
        g2.setColor(Tema.color(acento));
        g2.fillRoundRect(0, 0, ANCHO_BARRA * 2, h + 1, RADIO, RADIO);
        g2.fillRect(ANCHO_BARRA, 0, ANCHO_BARRA, h + 1);
        g2.dispose();
        super.paintComponent(g);
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import com.canchapro.controllers.EstadoVisualCancha;
import com.canchapro.controllers.MapaController.InfoCancha;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.Tema;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 *
 * Una cancha del MAPA DINÁMICO, dibujada 100% por código con Graphics2D.
 *
 * 
 *  ╭──────────────────────────────╮
 *  │ C01 ┌────────┬────────┐       │  ← franja del color del estado con
 *  │     │   (   )│        │       │    las líneas de una cancha dibujadas
 *  │     └────────┴──[EN CURSO]    │    y la píldora del estado
 *  │ La Bombonera                  │  ← nombre
 *  │ Fútbol 5 · 10 jugadores       │  ← tipo y capacidad
 *  │ Carlos Pérez · quedan 45 min  │  ← subtítulo que arma el controlador
 *  │ ████████████░░░░░░░░          │  ← avance del partido (solo EN CURSO)
 *  ╰──────────────────────────────╯
 * 
 *
 * La tarjeta NO decide nada: recibe una {@link InfoCancha} ya calculada por
 * el {@code MapaController} y solo la pinta. Al hacer clic avisa a quien la
 * contenga (el MapaCanchasPanel) mediante un {@link Consumer}; es el panel
 * quien decide si abre el POS (solo EN CURSO) u otra cosa.
 *
 * @author SANTANA
 */
public class TarjetaCancha extends JComponent{
    private static final long serialVersionUID = 1L;

    private static final int RADIO = 16;
    private static final int ALTO_FRANJA = 54;
    private static final int PAD = 12;

    private transient InfoCancha info;
    private transient Consumer<InfoCancha> alHacerClic = i -> { };
    private boolean encima;

    public TarjetaCancha(InfoCancha info) {
        setInfo(info);
        setPreferredSize(new Dimension(Tema.ANCHO_TARJETA_CANCHA, Tema.ALTO_TARJETA_CANCHA));
        setMinimumSize(getPreferredSize());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        setOpaque(false);
        registrarEventos();
    }

    // =====================================================================
    //  API PÚBLICA
    // =====================================================================

    /** Cambia los datos (cada refresco del mapa) y se vuelve a pintar. */
    public final void setInfo(InfoCancha nueva) {
        this.info = Objects.requireNonNull(nueva, "La información de la cancha es obligatoria.");
        setToolTipText("<html>" + nueva.getDetalle().replace("\n", "<br>") + "</html>");
        repaint();
    }

    public InfoCancha getInfo() {
        return info;
    }

    /** Código de la cancha que representa (para buscarla al refrescar). */
    public String getCodigoCancha() {
        return info.getCancha().getCodigo();
    }

    /** Qué hacer cuando el operador hace clic (o Enter/Espacio con el teclado). */
    public void alHacerClic(Consumer<InfoCancha> accion) {
        this.alHacerClic = accion == null ? i -> { } : accion;
    }

    /** Color asociado a cada estado VISUAL (los define el desarrollador en ColorTema). */
    public static Color colorDe(EstadoVisualCancha estado) {
        switch (estado) {
            case EN_CURSO:      return Tema.color(ColorTema.CANCHA_EN_CURSO);
            case MANTENIMIENTO: return Tema.color(ColorTema.CANCHA_MANTENIMIENTO);
            case INACTIVA:      return Tema.color(ColorTema.CANCHA_INACTIVA);
            default:            return Tema.color(ColorTema.CANCHA_DISPONIBLE);
        }
    }

    // =====================================================================
    //  EVENTOS (solo avisan; no hay lógica de negocio aquí)
    // =====================================================================

    private void registrarEventos() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                alHacerClic.accept(info);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    alHacerClic.accept(info);
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    // =====================================================================
    //  DIBUJO
    // =====================================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;
        Color estado = colorDe(info.getEstado());
        Color franja = encima ? Tema.oscurecer(estado, 0.12) : estado;

        // 1) Fondo blanco redondeado
        g2.setColor(Tema.color(ColorTema.SUPERFICIE));
        g2.fillRoundRect(0, 0, w, h, RADIO, RADIO);

        // 2) Franja superior del color del estado (redondeada solo arriba)
        g2.setColor(franja);
        g2.fillRoundRect(0, 0, w, ALTO_FRANJA, RADIO, RADIO);
        g2.fillRect(0, ALTO_FRANJA - RADIO, w, RADIO);
        dibujarLineasDeCancha(g2, w);

        // 3) Código de la cancha (arriba-izq.) y píldora del estado (abajo-der.) sobre la franja
        g2.setFont(Tema.FUENTE_SUBTITULO);
        g2.setColor(Tema.textoSobre(franja));
        g2.drawString(info.getCancha().getCodigo(), PAD, 24);
        String est = info.getEstado().getDescripcion().toUpperCase(java.util.Locale.ROOT);
        g2.setFont(Tema.FUENTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        int anchoPildora = fm.stringWidth(est) + 14;
        int xPildora = w - PAD + 4 - anchoPildora;
        int yPildora = ALTO_FRANJA - 24;
        Color fondoPildora = Tema.oscurecer(franja, 0.25);
        g2.setColor(fondoPildora);
        g2.fillRoundRect(xPildora, yPildora, anchoPildora, 18, 18, 18);
        g2.setColor(Tema.textoSobre(fondoPildora));
        g2.drawString(est, xPildora + 7, yPildora + 13);

        // 4) Textos de la parte blanca
        Cancha c = info.getCancha();
        int ancho = w - 2 * PAD;
        int y = ALTO_FRANJA + 18;
        g2.setFont(Tema.FUENTE_NEGRITA);
        g2.setColor(Tema.color(ColorTema.TEXTO));
        g2.drawString(recortar(c.getNombre(), g2.getFontMetrics(), ancho), PAD, y);

        g2.setFont(Tema.FUENTE_PEQUENA);
        g2.setColor(Tema.color(ColorTema.TEXTO_SECUNDARIO));
        y += 16;
        g2.drawString(recortar(c.getTipo().getDescripcion() + " · " + c.getCapacidad() + " jugadores",
                g2.getFontMetrics(), ancho), PAD, y);

        y += 16;
        g2.setColor(info.tienePartido() ? Tema.oscurecer(estado, 0.25) : Tema.color(ColorTema.TEXTO_SECUNDARIO));
        g2.drawString(recortar(info.getSubtitulo(), g2.getFontMetrics(), ancho), PAD, y);

        // 5) Barra de avance del partido (solo EN CURSO)
        if (info.tienePartido()) {
            dibujarAvance(g2, PAD, h - 12, ancho, estado);
        }

        // 6) Borde: del color del estado; más grueso con el mouse encima o con foco
        boolean resaltar = encima || isFocusOwner();
        g2.setStroke(new BasicStroke(resaltar ? 2.5f : 1f));
        g2.setColor(resaltar ? Tema.oscurecer(estado, 0.2) : Tema.color(ColorTema.BORDE));
        g2.drawRoundRect(0, 0, w, h, RADIO, RADIO);
        g2.dispose();
    }

    /**
     * Líneas blancas semitransparentes de una cancha de fútbol (contorno, media,
     * círculo central y áreas). Ocupan toda la franja y se adaptan al ancho.
     */
    private static void dibujarLineasDeCancha(Graphics2D g2, int w) {
        int x = 6;
        int y = 6;
        int cw = w - 12;
        int ch = ALTO_FRANJA - 12;
        int area = ch / 2;
        g2.setColor(new Color(255, 255, 255, 70));
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawRoundRect(x, y, cw, ch, 8, 8);                                   // contorno
        g2.drawLine(x + cw / 2, y, x + cw / 2, y + ch);                         // línea media
        g2.drawOval(x + cw / 2 - area / 2, y + ch / 2 - area / 2, area, area);  // círculo central
        g2.drawRect(x, y + ch / 2 - area / 2, area / 2, area);                  // área izquierda
        g2.drawRect(x + cw - area / 2, y + ch / 2 - area / 2, area / 2, area);  // área derecha
    }

    /** Porcentaje jugado = 1 - (minutos restantes / duración total). */
    private void dibujarAvance(Graphics2D g2, int x, int y, int ancho, Color color) {
        Reserva r = info.getReservaEnCurso();
        long total = Math.max(1, r.getDuracion().toMinutes());
        double avance = 1.0 - Math.min(total, info.getMinutosRestantes()) / (double) total;
        g2.setColor(Tema.color(ColorTema.FONDO));
        g2.fillRoundRect(x, y, ancho, 6, 6, 6);
        g2.setColor(color);
        g2.fillRoundRect(x, y, (int) Math.round(ancho * avance), 6, 6, 6);
    }

    /** Corta el texto con "…" si no cabe en el ancho disponible. */
    private static String recortar(String texto, FontMetrics fm, int ancho) {
        if (fm.stringWidth(texto) <= ancho) {
            return texto;
        }
        String puntos = "…";
        int fin = texto.length();
        while (fin > 0 && fm.stringWidth(texto.substring(0, fin) + puntos) > ancho) {
            fin--;
        }
        return texto.substring(0, fin) + puntos;
    }
}

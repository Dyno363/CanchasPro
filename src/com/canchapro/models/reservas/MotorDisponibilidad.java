/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.reservas;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaNoDisponibleException;
import com.canchapro.models.exceptions.ConflictoHorarioException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.FechaUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
/**
 *
 * @author SANTANA
 */
/**
 * Motor de disponibilidad: responde automáticamente si una cancha puede
 * reservarse en un horario y qué canchas están libres.
 *
 * Combina tres preguntas, cada una respondida por quien tiene los datos:
 * 
 *   ¿La cancha puede usarse?           → {@code Cancha.estaOperativa()}
 *   ¿El horario cabe en su apertura?    → {@code Cancha.cubreHorario()}
 *   ¿Choca con otra reserva?            → {@code Reserva.seTraslapaCon()}
 * 
 *
 * No guarda reservas ni canchas: las recibe como parámetro (las colecciones
 * en memoria del ComplejoDeportivo). Así nunca trabaja con datos desactualizados.
 *
 * Tiempo de limpieza: si se configuran L minutos, cada reserva existente
 * "ocupa" L minutos antes y después. Con L = 15, después de una reserva que
 * termina a las 21:00 la siguiente puede empezar a las 21:15.</p>
 */
public class MotorDisponibilidad {
    private int minutosLimpieza;

    /**
     * @param minutosLimpieza minutos libres obligatorios entre reservas (0 = sin limpieza)
     * @throws DatoInvalidoException si es negativo
     */
    public MotorDisponibilidad(int minutosLimpieza) throws DatoInvalidoException {
        setMinutosLimpieza(minutosLimpieza);
    }

    public int getMinutosLimpieza() {
        return minutosLimpieza;
    }

    public final void setMinutosLimpieza(int minutosLimpieza) throws DatoInvalidoException {
        if (minutosLimpieza < 0) {
            throw new DatoInvalidoException("Los minutos de limpieza no pueden ser negativos.");
        }
        this.minutosLimpieza = minutosLimpieza;
    }

    // =====================================================================
    //  CONFLICTOS DE HORARIO
    // =====================================================================

    /**
     * Busca la primera reserva que choca con el horario solicitado en la
     * misma cancha. Ignora las reservas canceladas.
     *
     * @param cancha   cancha a revisar
     * @param inicio   inicio solicitado
     * @param fin      fin solicitado
     * @param reservas todas las reservas en memoria
     * @return la reserva en conflicto, o vacío si el horario está libre
     */
    public Optional<Reserva> buscarConflicto(Cancha cancha, LocalDateTime inicio,
                                             LocalDateTime fin, Collection<Reserva> reservas) {
        // Con limpieza, el rango solicitado se "ensancha" L minutos a cada lado.
        LocalDateTime inicioConLimpieza = inicio.minusMinutes(minutosLimpieza);
        LocalDateTime finConLimpieza = fin.plusMinutes(minutosLimpieza);

        for (Reserva existente : reservas) {
            if (existente.getCancha().equals(cancha)
                    && existente.ocupaHorario()
                    && existente.seTraslapaCon(inicioConLimpieza, finConLimpieza)) {
                return Optional.of(existente);
            }
        }
        return Optional.empty();
    }

    /** @return true si el horario choca con alguna reserva de la cancha */
    public boolean hayConflicto(Cancha cancha, LocalDateTime inicio,
                                LocalDateTime fin, Collection<Reserva> reservas) {
        return buscarConflicto(cancha, inicio, fin, reservas).isPresent();
    }

    // =====================================================================
    //  VALIDACIÓN COMPLETA (se usa justo antes de crear la reserva)
    // =====================================================================

    /**
     * Verifica que una cancha pueda reservarse en el horario solicitado.
     * Si algo falla, lanza la excepción con un mensaje claro para el operador.
     *
     * @throws DatoInvalidoException        si faltan datos o el fin no es posterior al inicio
     * @throws CanchaNoDisponibleException  si la cancha no está operativa o el horario está fuera
     *                                      de su apertura/cierre (prueba #5 del PDF)
     * @throws ConflictoHorarioException    si choca con otra reserva (pruebas #3 y #4 del PDF)
     */
    public void validarDisponibilidad(Cancha cancha, LocalDateTime inicio, LocalDateTime fin,
                                      Collection<Reserva> reservas)
            throws DatoInvalidoException, CanchaNoDisponibleException, ConflictoHorarioException {
        if (cancha == null) {
            throw new DatoInvalidoException("La cancha es obligatoria.");
        }
        validarRango(inicio, fin, reservas);

        if (!cancha.estaOperativa()) {
            throw new CanchaNoDisponibleException("La cancha " + cancha.getNombre()
                    + " está " + cancha.getEstado().getDescripcion().toLowerCase(Locale.ROOT)
                    + " y no acepta reservas.");
        }
        if (!cancha.cubreHorario(inicio, fin)) {
            throw new CanchaNoDisponibleException("El horario "
                    + FechaUtil.formatearRango(inicio, fin) + " está fuera del horario de "
                    + cancha.getNombre() + " (" + cancha.getHorarioTexto() + ").");
        }

        Optional<Reserva> conflicto = buscarConflicto(cancha, inicio, fin, reservas);
        if (conflicto.isPresent()) {
            Reserva r = conflicto.get();
            String detalleLimpieza = minutosLimpieza > 0
                    ? " (incluye " + minutosLimpieza + " min de limpieza entre reservas)"
                    : "";
            throw new ConflictoHorarioException("La cancha " + cancha.getNombre()
                    + " ya está reservada de " + FechaUtil.formatearRango(r.getInicio(), r.getFin())
                    + " (reserva " + r.getCodigo() + ")" + detalleLimpieza + ".");
        }
    }

    /**
     * Igual que {@link #validarDisponibilidad}, pero responde con un boolean
     * en lugar de lanzar excepciones. Útil para filtrar listas.
     */
    public boolean estaDisponible(Cancha cancha, LocalDateTime inicio, LocalDateTime fin,
                                  Collection<Reserva> reservas) {
        return cancha != null && inicio != null && fin != null && inicio.isBefore(fin)
                && cancha.estaOperativa()
                && cancha.cubreHorario(inicio, fin)
                && !hayConflicto(cancha, inicio, fin, reservas);
    }

    // =====================================================================
    //  BÚSQUEDA AUTOMÁTICA (el "corazón del proyecto" según el PDF)
    // =====================================================================

    /**
     * El operador indica fecha, hora de inicio, duración y tipo de cancha;
     * el sistema responde cuáles canchas pueden reservarse.
     *
     * @param inicio   fecha y hora de inicio
     * @param duracion duración solicitada
     * @param tipo     tipo de cancha deseado, o null para cualquier tipo
     * @param canchas  todas las canchas en memoria
     * @param reservas todas las reservas en memoria
     * @return canchas disponibles, ordenadas por código
     * @throws DatoInvalidoException si faltan datos o la duración no es positiva
     */
    public List<Cancha> buscarCanchasDisponibles(LocalDateTime inicio, Duration duracion, TipoCancha tipo,
                                                 Collection<Cancha> canchas, Collection<Reserva> reservas)
            throws DatoInvalidoException {
        if (duracion == null || duracion.isZero() || duracion.isNegative()) {
            throw new DatoInvalidoException("La duración debe ser mayor que cero.");
        }
        if (canchas == null) {
            throw new DatoInvalidoException("La lista de canchas es obligatoria.");
        }
        LocalDateTime fin = inicio == null ? null : inicio.plus(duracion);
        validarRango(inicio, fin, reservas);

        List<Cancha> disponibles = new ArrayList<>();
        for (Cancha cancha : canchas) {
            boolean tipoCoincide = tipo == null || cancha.getTipo() == tipo;
            if (tipoCoincide && estaDisponible(cancha, inicio, fin, reservas)) {
                disponibles.add(cancha);
            }
        }
        disponibles.sort(Comparator.comparing(Cancha::getCodigo));
        return disponibles;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static void validarRango(LocalDateTime inicio, LocalDateTime fin,
                                     Collection<Reserva> reservas) throws DatoInvalidoException {
        if (inicio == null || fin == null) {
            throw new DatoInvalidoException("La fecha, la hora de inicio y la duración son obligatorias.");
        }
        if (!inicio.isBefore(fin)) {
            throw new DatoInvalidoException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (reservas == null) {
            throw new DatoInvalidoException("La lista de reservas es obligatoria.");
        }
    }
}

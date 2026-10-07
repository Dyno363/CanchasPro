/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.canchas;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 *
 * @author SANTANA
 */


/**
 * Cancha física del complejo (ej. C01 La Bombonera).
 *
 * Responsabilidades:
 * 
 *   Conocer sus datos: tipo, capacidad, estado y horario.
 *   Responder si puede usarse ({@link #estaOperativa()}).
 *   Responder si un rango de horas cabe en su horario ({@link #cubreHorario}).
 * 
 *
 * La cancha NO conoce sus reservas (la relación va de Reserva hacia Cancha).
 * Decidir si hay choques de horario es tarea de MotorDisponibilidad.
 *
 * Limitación de diseño: el horario debe abrir y cerrar el mismo día
 * (ej. 08:00 a 23:00). No se manejan horarios que crucen la medianoche.
 */
public class Cancha {

    private final String codigo;
    private String nombre;
    private TipoCancha tipo;
    private int capacidad;
    private EstadoCancha estado;
    private LocalTime horaApertura;
    private LocalTime horaCierre;

    /**
     * @param codigo       código único, ej. "C01"
     * @param nombre       nombre comercial, ej. "La Bombonera"
     * @param tipo         FUTBOL_5 o FUTBOL_7
     * @param capacidad    número de jugadores (mayor que cero)
     * @param estado       DISPONIBLE, MANTENIMIENTO o INACTIVA
     * @param horaApertura hora en que abre (ej. 08:00)
     * @param horaCierre   hora en que cierra (ej. 23:00); debe ser posterior a la apertura
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public Cancha(String codigo, String nombre, TipoCancha tipo, int capacidad,
                  EstadoCancha estado, LocalTime horaApertura, LocalTime horaCierre)
            throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código");
        setNombre(nombre);
        setTipo(tipo);
        setCapacidad(capacidad);
        setEstado(estado);
        setHorario(horaApertura, horaCierre);
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) throws DatoInvalidoException {
        this.nombre = Validador.validarRequerido(nombre, "nombre de la cancha");
    }

    public TipoCancha getTipo() {
        return tipo;
    }

    public final void setTipo(TipoCancha tipo) throws DatoInvalidoException {
        if (tipo == null) {
            throw new DatoInvalidoException("El tipo de cancha es obligatorio.");
        }
        this.tipo = tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public final void setCapacidad(int capacidad) throws DatoInvalidoException {
        this.capacidad = Validador.validarEnteroPositivo(capacidad, "capacidad");
    }

    public EstadoCancha getEstado() {
        return estado;
    }

    /**
     * Cambia el estado. Ej.: poner la cancha en MANTENIMIENTO la saca
     * inmediatamente de las búsquedas de disponibilidad.
     */
    public final void setEstado(EstadoCancha estado) throws DatoInvalidoException {
        if (estado == null) {
            throw new DatoInvalidoException("El estado de la cancha es obligatorio.");
        }
        this.estado = estado;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    /**
     * Apertura y cierre se cambian juntos porque se validan entre sí:
     * la apertura debe ser anterior al cierre.
     */
    public final void setHorario(LocalTime apertura, LocalTime cierre) throws DatoInvalidoException {
        if (apertura == null || cierre == null) {
            throw new DatoInvalidoException("La hora de apertura y la de cierre son obligatorias.");
        }
        if (!apertura.isBefore(cierre)) {
            throw new DatoInvalidoException("La hora de apertura (" + FechaUtil.formatearHora(apertura)
                    + ") debe ser anterior a la de cierre (" + FechaUtil.formatearHora(cierre) + ").");
        }
        this.horaApertura = apertura;
        this.horaCierre = cierre;
    }

    // =====================================================================
    //  LÓGICA DE NEGOCIO
    // =====================================================================

    /**
     * Regla del PDF: una cancha inactiva o en mantenimiento nunca debe
     * aparecer como disponible. La decisión la toma el propio estado.
     *
     * @return true si la cancha acepta reservas
     */
    public boolean estaOperativa() {
        return estado.permiteReservas();
    }

    /**
     * Indica si el rango [inicio, fin] cabe completo dentro del horario
     * de la cancha y en un mismo día.
     *
     * Ejemplos con horario 08:00-23:00:
     * 
     *   21:00 a 23:00 → true (termina justo al cierre)
     *   07:00 a 09:00 → false (empieza antes de abrir)
     *   22:00 a 00:00 → false (pasa al día siguiente)
     * 
     *
     * @param inicio momento de inicio de la reserva
     * @param fin    momento de fin de la reserva
     * @return true si el rango está dentro del horario
     */
    public boolean cubreHorario(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            return false;
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            return false;
        }
        return !inicio.toLocalTime().isBefore(horaApertura)
                && !fin.toLocalTime().isAfter(horaCierre);
    }

    /** Ejemplo: "08:00 - 23:00". Útil para tablas y mensajes. */
    public String getHorarioTexto() {
        return FechaUtil.formatearHora(horaApertura) + " - " + FechaUtil.formatearHora(horaCierre);
    }

    // =====================================================================
    //  IDENTIDAD
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Cancha)) {
            return false;
        }
        return codigo.equals(((Cancha) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Texto para combos: "C01 - La Bombonera (Fútbol 5)". */
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + tipo + ")";
    }
}
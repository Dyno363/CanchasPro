/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.utils;
import com.canchapro.models.exceptions.DatoInvalidoException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
/**
 *
 * @author SANTANA
 */

/**
 * Utilidades de fecha y hora basadas en java.time.
 *
 * Maneja dos "idiomas" de fechas:
 * 
 *   CSV (formato ISO, estable): 2026-10-06, 15:00, 2026-10-06T15:00
 *   Pantalla (formato local, amigable): 06/10/2026, 15:00, 06/10/2026 15:00
 * 
 *
 * Clase utilitaria: es {@code final} y su constructor es privado porque
 * solo tiene métodos estáticos y no tiene sentido crear objetos de ella.
 */
public final class FechaUtil {

    /*
     * Todos los formatos usan ResolverStyle.STRICT. Sin STRICT, Java corrige
     * fechas imposibles en silencio: "31/02/2026" se convertiría en 28/02/2026.
     * En modo STRICT el año se escribe "uuuu" en lugar de "yyyy".
     */

    /** Formato de escritura en CSV: año-mes-díaThora:minuto (sin segundos). */
    public static final DateTimeFormatter CSV_FECHA_HORA = estricto("uuuu-MM-dd'T'HH:mm");
    public static final DateTimeFormatter CSV_FECHA = estricto("uuuu-MM-dd");
    public static final DateTimeFormatter CSV_HORA = estricto("HH:mm");

    /** Formatos para mostrar y capturar en la interfaz. */
    public static final DateTimeFormatter PANTALLA_FECHA = estricto("dd/MM/uuuu");
    public static final DateTimeFormatter PANTALLA_HORA = estricto("HH:mm");
    public static final DateTimeFormatter PANTALLA_FECHA_HORA = estricto("dd/MM/uuuu HH:mm");

    private FechaUtil() {
        // No se instancia.
    }

    // =====================================================================
    //  LECTURA (texto -> objeto). Aceptan formato CSV o formato de pantalla.
    // =====================================================================

    /**
     * Convierte texto a fecha. Acepta "2026-10-06" o "06/10/2026".
     *
     * @throws DatoInvalidoException si el texto está vacío o no es una fecha válida
     */
    public static LocalDate leerFecha(String texto) throws DatoInvalidoException {
        String valor = exigirTexto(texto, "fecha");
        try {
            if (valor.contains("/")) {
                return LocalDate.parse(valor, PANTALLA_FECHA);
            }
            return LocalDate.parse(valor, CSV_FECHA);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("Fecha inválida: '" + valor + "'. Use dd/MM/yyyy.", e);
        }
    }

    /**
     * Convierte texto a hora. Acepta "08:00" o "8:00".
     *
     * @throws DatoInvalidoException si el texto está vacío o no es una hora válida
     */
    public static LocalTime leerHora(String texto) throws DatoInvalidoException {
        String valor = exigirTexto(texto, "hora");
        if (valor.length() == 4 && valor.charAt(1) == ':') {
            valor = "0" + valor; // "8:00" -> "08:00"
        }
        try {
            return LocalTime.parse(valor, CSV_HORA);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("Hora inválida: '" + valor + "'. Use HH:mm.", e);
        }
    }

    /**
     * Convierte texto a fecha y hora. Acepta "2026-10-06T15:00" (ISO, con o sin
     * segundos) o "06/10/2026 15:00".
     *
     * @throws DatoInvalidoException si el texto está vacío o no es válido
     */
    public static LocalDateTime leerFechaHora(String texto) throws DatoInvalidoException {
        String valor = exigirTexto(texto, "fecha y hora");
        try {
            if (valor.contains("/")) {
                return LocalDateTime.parse(valor, PANTALLA_FECHA_HORA);
            }
            return LocalDateTime.parse(valor); // ISO: 2026-10-06T15:00[:ss]
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("Fecha y hora inválida: '" + valor + "'.", e);
        }
    }

    /**
     * Igual que {@link #leerFechaHora(String)}, pero un texto vacío devuelve null.
     * Útil para columnas opcionales del CSV, como la fecha de cancelación.
     */
    public static LocalDateTime leerFechaHoraOpcional(String texto) throws DatoInvalidoException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return leerFechaHora(texto);
    }

    // =====================================================================
    //  ESCRITURA EN CSV (objeto -> texto). Un valor null se guarda como "".
    // =====================================================================

    public static String aCsv(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(CSV_FECHA);
    }

    public static String aCsv(LocalTime hora) {
        return hora == null ? "" : hora.format(CSV_HORA);
    }

    public static String aCsv(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(CSV_FECHA_HORA);
    }

    // =====================================================================
    //  FORMATO PARA PANTALLA
    // =====================================================================

    public static String formatearFecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(PANTALLA_FECHA);
    }

    public static String formatearHora(LocalTime hora) {
        return hora == null ? "" : hora.format(PANTALLA_HORA);
    }

    public static String formatearFechaHora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(PANTALLA_FECHA_HORA);
    }

    /**
     * Ejemplo: "06/10/2026 15:00 - 17:00". Si el fin es otro día, lo muestra completo.
     */
    public static String formatearRango(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            return "";
        }
        String finTexto = inicio.toLocalDate().equals(fin.toLocalDate())
                ? formatearHora(fin.toLocalTime())
                : formatearFechaHora(fin);
        return formatearFechaHora(inicio) + " - " + finTexto;
    }

    /**
     * Ejemplo: 90 minutos -> "1 h 30 min"; 120 minutos -> "2 h".
     */
    public static String formatearDuracion(Duration duracion) {
        if (duracion == null) {
            return "";
        }
        long horas = duracion.toHours();
        long minutos = duracion.toMinutes() % 60;
        if (minutos == 0) {
            return horas + " h";
        }
        return horas == 0 ? minutos + " min" : horas + " h " + minutos + " min";
    }

    // =====================================================================
    //  CÁLCULOS
    // =====================================================================

    /**
     * Horas entre dos momentos, con decimales. Ejemplo: 15:00 a 16:30 = 1.5.
     * Se usa para cobrar servicios por hora (árbitro, iluminación).
     */
    public static double horasEntre(LocalDateTime inicio, LocalDateTime fin) {
        return Duration.between(inicio, fin).toMinutes() / 60.0;
    }

    /**
     * Momento actual sin segundos ni nanosegundos (ej. 2026-10-06T16:45).
     * Todas las fechas del sistema se manejan a nivel de minuto.
     */
    public static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static DateTimeFormatter estricto(String patron) {
        return DateTimeFormatter.ofPattern(patron).withResolverStyle(ResolverStyle.STRICT);
    }

    private static String exigirTexto(String texto, String nombreCampo) throws DatoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new DatoInvalidoException("La " + nombreCampo + " es obligatoria.");
        }
        return texto.trim();
    }
}

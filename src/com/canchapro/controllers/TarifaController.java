/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.CalculadoraTarifa;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.canchas.Tarifa;
import com.canchapro.models.canchas.TarifaEspecial;
import com.canchapro.models.canchas.TarifaFranja;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador de tarifas: normales (por franja horaria) y especiales (por fecha).
 *
 * Permisos: cotizar precios es libre (lo usa Nueva Reserva); crear,
 * modificar y eliminar tarifas es solo del ADMINISTRADOR.
 *
 * Reglas que se aplican aquí:
 * 
 *   Dos tarifas con la MISMA prioridad no pueden cubrir el mismo momento,
 *       porque el sistema no sabría cuál cobrar.
 *   Una tarifa especial no puede crearse para una fecha que ya pasó.
 *   Se avisa de los "huecos": horarios en que las canchas abren pero
 *       ninguna tarifa normal los cubre (no se podrían reservar).
 * 
 */
public class TarifaController {
    private static final Locale ES = Locale.forLanguageTag("es");

    private final ComplejoDeportivo complejo;
    private final Sesion sesion;

    public TarifaController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }

    // =====================================================================
    //  CONSULTAS (cualquier usuario)
    // =====================================================================

    /** @return tarifas ordenadas: primero las de mayor prioridad, luego por código */
    public List<Tarifa> listar() {
        List<Tarifa> lista = new ArrayList<>(complejo.getTarifas());
        lista.sort(Comparator.comparingInt(Tarifa::getPrioridad).reversed()
                .thenComparing(Tarifa::getCodigo));
        return lista;
    }

    public Tarifa obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarTarifa(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la tarifa " + codigo + "."));
    }

    /**
     * Desglose del precio de cancha por tramos (para mostrarlo antes de reservar).
     * Ej.: 16:00-17:00 Q150.00 + 17:00-18:00 Q225.00.
     */
    public List<CalculadoraTarifa.Tramo> desglosar(LocalDateTime inicio, LocalDateTime fin)
            throws CanchaProException {
        return complejo.getCalculadoraTarifa().desglosar(inicio, fin);
    }

    /** @return precio total de la cancha para ese período */
    public double cotizar(LocalDateTime inicio, LocalDateTime fin) throws CanchaProException {
        return complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, fin);
    }

    public boolean puedeAdministrar() {
        return sesion.esAdministrador();
    }

    // =====================================================================
    //  OPERACIONES (solo administrador)
    // =====================================================================

    public TarifaFranja crearFranja(String nombre, double precioHora, int prioridad,
                                    Set<DayOfWeek> dias, LocalTime horaInicio, LocalTime horaFin)
            throws CanchaProException {
        sesion.exigirAdministrador("crear tarifas");
        TarifaFranja tarifa = new TarifaFranja(siguienteCodigo(), nombre, precioHora, prioridad,
                dias, horaInicio, horaFin);
        exigirSinChoqueDePrioridad(tarifa, null);
        complejo.agregarTarifa(tarifa);
        return tarifa;
    }

    public TarifaEspecial crearEspecial(String nombre, double precioHora, int prioridad,
                                        LocalDate fecha, String motivo) throws CanchaProException {
        sesion.exigirAdministrador("crear tarifas");
        exigirFechaNoPasada(fecha);
        TarifaEspecial tarifa = new TarifaEspecial(siguienteCodigo(), nombre, precioHora, prioridad,
                fecha, motivo);
        exigirSinChoqueDePrioridad(tarifa, null);
        complejo.agregarTarifa(tarifa);
        return tarifa;
    }

    /**
     * Modifica una tarifa por franja. Usa un "objeto de prueba" con los datos
     * nuevos: si se construye sin error y no choca con otra, se aplica.
     */
    public TarifaFranja actualizarFranja(String codigo, String nombre, double precioHora, int prioridad,
                                         Set<DayOfWeek> dias, LocalTime horaInicio, LocalTime horaFin)
            throws CanchaProException {
        sesion.exigirAdministrador("modificar tarifas");
        TarifaFranja tarifa = obtenerDeTipo(codigo, TarifaFranja.class);

        TarifaFranja prueba = new TarifaFranja(codigo, nombre, precioHora, prioridad, dias, horaInicio, horaFin);
        exigirSinChoqueDePrioridad(prueba, tarifa);

        tarifa.setNombre(nombre);
        tarifa.setPrecioHora(precioHora);
        tarifa.setPrioridad(prioridad);
        tarifa.setFranja(dias, horaInicio, horaFin);
        return tarifa;
    }

    public TarifaEspecial actualizarEspecial(String codigo, String nombre, double precioHora, int prioridad,
                                             LocalDate fecha, String motivo) throws CanchaProException {
        sesion.exigirAdministrador("modificar tarifas");
        TarifaEspecial tarifa = obtenerDeTipo(codigo, TarifaEspecial.class);
        exigirFechaNoPasada(fecha);

        TarifaEspecial prueba = new TarifaEspecial(codigo, nombre, precioHora, prioridad, fecha, motivo);
        exigirSinChoqueDePrioridad(prueba, tarifa);

        tarifa.setNombre(nombre);
        tarifa.setPrecioHora(precioHora);
        tarifa.setPrioridad(prioridad);
        tarifa.setFecha(fecha);
        tarifa.setMotivo(motivo);
        return tarifa;
    }

    /**
     * Elimina una tarifa. Las reservas ya hechas NO cambian de precio, porque
     * cada una guardó su "foto" del precio al crearse.
     *
     * @return huecos de cobertura que quedan después de eliminar (para avisar)
     */
    public List<String> eliminar(String codigo) throws CanchaProException {
        sesion.exigirAdministrador("eliminar tarifas");
        obtener(codigo);
        complejo.eliminarTarifa(codigo);
        return getHuecosDeCobertura();
    }

    // =====================================================================
    //  VERIFICACIÓN DE COBERTURA
    // =====================================================================

    /**
     * Revisa, para cada día de la semana y en bloques de 30 minutos, que
     * alguna tarifa NORMAL cubra el horario de las canchas que no están inactivas.
     *
     * @return textos como "Domingo 08:00 - 10:00"; lista vacía si todo está cubierto
     */
    public List<String> getHuecosDeCobertura() {
        List<String> huecos = new ArrayList<>();
        LocalTime apertura = null;
        LocalTime cierre = null;
        for (Cancha c : complejo.getCanchas()) {
            if (c.getEstado() != EstadoCancha.INACTIVA) {
                apertura = (apertura == null || c.getHoraApertura().isBefore(apertura)) ? c.getHoraApertura() : apertura;
                cierre = (cierre == null || c.getHoraCierre().isAfter(cierre)) ? c.getHoraCierre() : cierre;
            }
        }
        if (apertura == null) {
            return huecos; // no hay canchas en uso
        }

        for (DayOfWeek dia : DayOfWeek.values()) {
            LocalDate fecha = LocalDate.now().with(TemporalAdjusters.nextOrSame(dia));
            LocalTime inicioHueco = null;
            for (LocalTime hora = apertura; hora.isBefore(cierre); hora = hora.plusMinutes(30)) {
                boolean cubierta = hayFranjaPara(LocalDateTime.of(fecha, hora));
                if (!cubierta && inicioHueco == null) {
                    inicioHueco = hora;
                } else if (cubierta && inicioHueco != null) {
                    huecos.add(textoHueco(dia, inicioHueco, hora));
                    inicioHueco = null;
                }
                if (hora.plusMinutes(30).isBefore(hora)) {
                    break; // protección: pasó la medianoche
                }
            }
            if (inicioHueco != null) {
                huecos.add(textoHueco(dia, inicioHueco, cierre));
            }
        }
        return huecos;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private String siguienteCodigo() {
        return complejo.generarCodigo(GeneradorCodigo.Tipo.TARIFA);
    }

    /** Obtiene la tarifa y exige que sea del tipo esperado (no se cambia franja ↔ especial). */
    private <T extends Tarifa> T obtenerDeTipo(String codigo, Class<T> tipo) throws CanchaProException {
        Tarifa tarifa = obtener(codigo);
        if (!tipo.isInstance(tarifa)) {
            throw new CanchaProException("La tarifa " + codigo + " es de otro tipo. Para cambiar el tipo, "
                    + "elimínela y cree una nueva.");
        }
        return tipo.cast(tarifa);
    }

    private static void exigirFechaNoPasada(LocalDate fecha) throws DatoInvalidoException {
        if (fecha != null && fecha.isBefore(FechaUtil.ahora().toLocalDate())) {
            throw new DatoInvalidoException("La fecha " + FechaUtil.formatearFecha(fecha)
                    + " ya pasó; no tiene sentido crear una tarifa especial para ella.");
        }
    }

    /**
     * @param candidata tarifa nueva o versión de prueba con los cambios
     * @param original  la tarifa que se está editando (se ignora en la comparación); null si es nueva
     */
    private void exigirSinChoqueDePrioridad(Tarifa candidata, Tarifa original) throws CanchaProException {
        for (Tarifa otra : complejo.getTarifas()) {
            if (otra.equals(original) || otra.getPrioridad() != candidata.getPrioridad()) {
                continue;
            }
            if (seCruzan(candidata, otra)) {
                throw new CanchaProException("La tarifa choca con " + otra + " (" + otra.getDescripcionAplicacion()
                        + ") y ambas tienen prioridad " + otra.getPrioridad()
                        + ". Cambie el horario o use una prioridad distinta.");
            }
        }
    }

    /** ¿Existe algún momento en que ambas tarifas apliquen? */
    private static boolean seCruzan(Tarifa a, Tarifa b) {
        if (a instanceof TarifaEspecial && b instanceof TarifaEspecial) {
            return ((TarifaEspecial) a).getFecha().equals(((TarifaEspecial) b).getFecha());
        }
        if (a instanceof TarifaEspecial && b instanceof TarifaFranja) {
            return ((TarifaFranja) b).getDias().contains(((TarifaEspecial) a).getFecha().getDayOfWeek());
        }
        if (a instanceof TarifaFranja && b instanceof TarifaEspecial) {
            return seCruzan(b, a);
        }
        TarifaFranja fa = (TarifaFranja) a;
        TarifaFranja fb = (TarifaFranja) b;
        Set<DayOfWeek> comunes = EnumSet.copyOf(fa.getDias());
        comunes.retainAll(fb.getDias());
        // Misma regla que los choques de reservas: inicioA < finB Y finA > inicioB
        return !comunes.isEmpty()
                && fa.getHoraInicio().isBefore(fb.getHoraFin())
                && fa.getHoraFin().isAfter(fb.getHoraInicio());
    }

    private boolean hayFranjaPara(LocalDateTime momento) {
        for (Tarifa tarifa : complejo.getTarifas()) {
            if (tarifa instanceof TarifaFranja && tarifa.aplicaA(momento)) {
                return true;
            }
        }
        return false;
    }

    private static String textoHueco(DayOfWeek dia, LocalTime desde, LocalTime hasta) {
        String nombre = dia.getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " "
                + FechaUtil.formatearHora(desde) + " - " + FechaUtil.formatearHora(hasta);
    }
}

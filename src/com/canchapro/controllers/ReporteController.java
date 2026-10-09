/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador de REPORTES. El PDF exige: "no es solo listar registros", sino
 * PROCESAR los objetos en memoria: agrupar, contar, sumar, comparar, ordenar
 * y filtrar.
 *
 * Los 8 reportes obligatorios (sección 13 del PDF) + reporte mensual (prueba #12)
 * + ventas de tienda:
 *  
 *    {@link #reservasPorFecha}       — agrupa por día (TreeMap) y cuenta/suma.  
 *    {@link #reservasPorCancha}      — agrupa por cancha y calcula % de ocupación.  
 *    {@link #ingresosPorPeriodo}     — suma pagos y ventas por día.  
 *    {@link #pagosPendientes}        — filtra saldo &gt; 0 y ordena por saldo.  
 *    {@link #reservasCanceladas}     — filtra canceladas con motivo y responsable.  
 *    {@link #canchaMasUtilizada}     — ranking por horas usadas.  
 *    {@link #horariosMayorDemanda}   — cuenta reservas por franja de 1 hora.  
 *    {@link #clientesFrecuentes}     — ranking de clientes por cantidad de reservas.  
 *   
 *
 * Todos devuelven un {@link Reporte} genérico (título, columnas, filas y
 * resumen), así UNA sola tabla en la vista muestra cualquier reporte.
 *
 * Filtro de fechas: {@code desde} y {@code hasta} son inclusivos; null = sin límite.
 * Una reserva pertenece al período por la fecha en que se JUEGA (inicio).
 */
public class ReporteController {
    private static final Locale ES = Locale.forLanguageTag("es");
    private final ComplejoDeportivo complejo;
    public ReporteController(ComplejoDeportivo complejo) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
    }

    // =====================================================================
    //  1. RESERVAS POR FECHA
    // =====================================================================

    /**
     * Una fila por día con reservas.
     *
     * @param estado filtro opcional (null = todos)
     */
    public Reporte reservasPorFecha(LocalDate desde, LocalDate hasta, EstadoReserva estado)
            throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<LocalDate, List<Reserva>> porDia = new TreeMap<>(); // TreeMap: ordena las fechas solo
        for (Reserva r : reservasEnRango(desde, hasta)) {
            if (estado == null || r.getEstado() == estado) {
                porDia.computeIfAbsent(r.getInicio().toLocalDate(), k -> new ArrayList<>()).add(r);
            }
        }

        Reporte rep = new Reporte("Reservas por fecha", textoPeriodo(desde, hasta)
                + (estado == null ? "" : " · Estado: " + estado),
                "Fecha", "Día", "Reservas", "Activas", "Canceladas", "Horas", "Monto (sin canceladas)");
        int totalReservas = 0;
        double totalMonto = 0;
        for (Map.Entry<LocalDate, List<Reserva>> e : porDia.entrySet()) {
            int canceladas = 0;
            double horas = 0;
            double monto = 0;
            for (Reserva r : e.getValue()) {
                if (r.getEstado() == EstadoReserva.CANCELADA) {
                    canceladas++;
                } else {
                    horas += r.getHoras();
                    monto += r.calcularTotal();
                }
            }
            rep.agregarFila(FechaUtil.formatearFecha(e.getKey()), nombreDia(e.getKey().getDayOfWeek()),
                    e.getValue().size(), e.getValue().size() - canceladas, canceladas,
                    horas(horas), MonedaUtil.formatear(monto));
            totalReservas += e.getValue().size();
            totalMonto += monto;
        }
        rep.agregarResumen("Días con reservas: " + porDia.size());
        rep.agregarResumen("Total de reservas: " + totalReservas);
        rep.agregarResumen("Monto total: " + MonedaUtil.formatear(totalMonto));
        return rep;
    }

    // =====================================================================
    //  2. RESERVAS POR CANCHA
    // =====================================================================

    /** Una fila por cancha con su ocupación (horas reservadas / horas abiertas del período). */
    public Reporte reservasPorCancha(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        LocalDate[] rango = rangoEfectivo(desde, hasta);
        long dias = Duration.between(rango[0].atStartOfDay(), rango[1].plusDays(1).atStartOfDay()).toDays();

        Reporte rep = new Reporte("Reservas por cancha", textoPeriodo(desde, hasta) + " (" + dias + " días)",
                "Cancha", "Tipo", "Reservas", "Canceladas", "Horas usadas", "Ocupación", "Monto");
        for (Cancha cancha : complejo.getCanchas()) {
            int reservas = 0;
            int canceladas = 0;
            double horasUsadas = 0;
            double monto = 0;
            for (Reserva r : reservasEnRango(desde, hasta)) {
                if (!r.getCancha().equals(cancha)) {
                    continue;
                }
                reservas++;
                if (r.getEstado() == EstadoReserva.CANCELADA) {
                    canceladas++;
                } else {
                    horasUsadas += r.getHoras();
                    monto += r.calcularTotal();
                }
            }
            double horasAbiertas = dias * FechaUtil.horasEntre(
                    cancha.getHoraApertura().atDate(rango[0]), cancha.getHoraCierre().atDate(rango[0]));
            rep.agregarFila(cancha.getNombre(), cancha.getTipo().getDescripcion(), reservas, canceladas,
                    horas(horasUsadas), porcentaje(horasUsadas, horasAbiertas), MonedaUtil.formatear(monto));
        }
        rep.agregarResumen("Ocupación = horas reservadas (sin canceladas) / horas que la cancha estuvo abierta.");
        return rep;
    }

    // =====================================================================
    //  3. INGRESOS POR PERÍODO
    // =====================================================================

    /**
     * Dinero que REALMENTE entró, por día: pagos de reservas (según la fecha del
     * pago) + ventas de tienda. Criterio de caja: se cuenta cuando se cobra.
     */
    public Reporte ingresosPorPeriodo(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<LocalDate, double[]> porDia = new TreeMap<>(); // [0]=reservas, [1]=tienda
        Map<String, Double> porMetodo = new LinkedHashMap<>();

        for (Reserva r : complejo.getReservas()) {
            for (Pago p : r.getPagos()) {
                LocalDate fecha = p.getFechaHora().toLocalDate();
                if (enRango(fecha, desde, hasta)) {
                    porDia.computeIfAbsent(fecha, k -> new double[2])[0] += p.getMonto();
                    porMetodo.merge(p.getMetodo().getDescripcion(), p.getMonto(), Double::sum);
                }
            }
        }
        for (Venta v : complejo.getVentas()) {
            LocalDate fecha = v.getFechaHora().toLocalDate();
            if (enRango(fecha, desde, hasta)) {
                porDia.computeIfAbsent(fecha, k -> new double[2])[1] += v.calcularTotal();
                porMetodo.merge(v.getMetodoPago().getDescripcion(), v.calcularTotal(), Double::sum);
            }
        }

        Reporte rep = new Reporte("Ingresos por período", textoPeriodo(desde, hasta),
                "Fecha", "Pagos de reservas", "Ventas de tienda", "Total del día");
        double totalReservas = 0;
        double totalTienda = 0;
        for (Map.Entry<LocalDate, double[]> e : porDia.entrySet()) {
            double[] montos = e.getValue();
            rep.agregarFila(FechaUtil.formatearFecha(e.getKey()), MonedaUtil.formatear(montos[0]),
                    MonedaUtil.formatear(montos[1]), MonedaUtil.formatear(montos[0] + montos[1]));
            totalReservas += montos[0];
            totalTienda += montos[1];
        }
        rep.agregarResumen("Total pagos de reservas: " + MonedaUtil.formatear(totalReservas));
        rep.agregarResumen("Total ventas de tienda: " + MonedaUtil.formatear(totalTienda));
        rep.agregarResumen("INGRESO TOTAL: " + MonedaUtil.formatear(totalReservas + totalTienda));
        for (Map.Entry<String, Double> e : porMetodo.entrySet()) {
            rep.agregarResumen("  " + e.getKey() + ": " + MonedaUtil.formatear(e.getValue()));
        }
        return rep;
    }

    // =====================================================================
    //  4. PAGOS PENDIENTES
    // =====================================================================

    /** Reservas no canceladas con saldo, de la que más debe a la que menos. */
    public Reporte pagosPendientes() {
        List<Reserva> conSaldo = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            if (r.getEstado() != EstadoReserva.CANCELADA && !r.estaPagada()) {
                conSaldo.add(r);
            }
        }
        conSaldo.sort(Comparator.comparingDouble(Reserva::calcularSaldo).reversed());

        Reporte rep = new Reporte("Pagos pendientes", "Reservas no canceladas con saldo mayor a Q0.00",
                "Reserva", "Cliente", "Teléfono", "Fecha", "Estado", "Total", "Pagado", "Saldo");
        double totalSaldo = 0;
        for (Reserva r : conSaldo) {
            rep.agregarFila(r.getCodigo(), r.getCliente().getNombre(), r.getCliente().getTelefono(),
                    FechaUtil.formatearFecha(r.getInicio().toLocalDate()), r.getEstado().getDescripcion(),
                    MonedaUtil.formatear(r.calcularTotal()), MonedaUtil.formatear(r.calcularTotalPagado()),
                    MonedaUtil.formatear(r.calcularSaldo()));
            totalSaldo += r.calcularSaldo();
        }
        rep.agregarResumen("Reservas con saldo: " + conSaldo.size());
        rep.agregarResumen("Total por cobrar: " + MonedaUtil.formatear(totalSaldo));
        return rep;
    }

    // =====================================================================
    //  5. RESERVAS CANCELADAS
    // =====================================================================

    /** Canceladas del período, de la más reciente a la más antigua (con su historial). */
    public Reporte reservasCanceladas(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        List<Reserva> canceladas = new ArrayList<>();
        for (Reserva r : reservasEnRango(desde, hasta)) {
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                canceladas.add(r);
            }
        }
        canceladas.sort(Comparator.comparing((Reserva r) -> r.getCancelacion().getFechaHora()).reversed());

        Reporte rep = new Reporte("Reservas canceladas", textoPeriodo(desde, hasta),
                "Reserva", "Cliente", "Cancha", "Fecha reservada", "Cancelada el", "Motivo", "Responsable",
                "Anticipo recibido");
        double anticipos = 0;
        Map<String, Integer> porMotivo = new LinkedHashMap<>();
        for (Reserva r : canceladas) {
            rep.agregarFila(r.getCodigo(), r.getCliente().getNombre(), r.getCancha().getNombre(),
                    FechaUtil.formatearRango(r.getInicio(), r.getFin()),
                    FechaUtil.formatearFechaHora(r.getCancelacion().getFechaHora()),
                    r.getCancelacion().getMotivo(), r.getCancelacion().getResponsable().getNombre(),
                    MonedaUtil.formatear(r.calcularTotalPagado()));
            anticipos += r.calcularTotalPagado();
            porMotivo.merge(r.getCancelacion().getMotivo(), 1, Integer::sum);
        }
        int totalPeriodo = reservasEnRango(desde, hasta).size();
        rep.agregarResumen("Canceladas: " + canceladas.size() + " de " + totalPeriodo + " reservas ("
                + porcentaje(canceladas.size(), totalPeriodo) + ")");
        rep.agregarResumen("Anticipos recibidos de reservas canceladas: " + MonedaUtil.formatear(anticipos));
        String motivoFrecuente = null;
        for (Map.Entry<String, Integer> e : porMotivo.entrySet()) {
            if (motivoFrecuente == null || e.getValue() > porMotivo.get(motivoFrecuente)) {
                motivoFrecuente = e.getKey();
            }
        }
        if (motivoFrecuente != null) {
            rep.agregarResumen("Motivo más frecuente: " + motivoFrecuente + " (" + porMotivo.get(motivoFrecuente) + ")");
        }
        return rep;
    }

    // =====================================================================
    //  6. CANCHA MÁS UTILIZADA
    // =====================================================================

    /** Ranking de canchas por horas usadas (sin canceladas). */
    public Reporte canchaMasUtilizada(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<Cancha, double[]> uso = new LinkedHashMap<>(); // [0]=horas, [1]=reservas, [2]=monto
        for (Cancha c : complejo.getCanchas()) {
            uso.put(c, new double[3]);
        }
        double horasTotales = 0;
        for (Reserva r : reservasEnRango(desde, hasta)) {
            if (r.getEstado() != EstadoReserva.CANCELADA) {
                double[] datos = uso.get(r.getCancha());
                datos[0] += r.getHoras();
                datos[1]++;
                datos[2] += r.calcularTotal();
                horasTotales += r.getHoras();
            }
        }
        List<Map.Entry<Cancha, double[]>> ranking = new ArrayList<>(uso.entrySet());
        ranking.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0])); // mayor a menor

        Reporte rep = new Reporte("Cancha más utilizada", textoPeriodo(desde, hasta),
                "Puesto", "Cancha", "Horas usadas", "Reservas", "% del uso total", "Monto generado");
        int puesto = 1;
        for (Map.Entry<Cancha, double[]> e : ranking) {
            double[] d = e.getValue();
            rep.agregarFila(puesto++, e.getKey().getNombre(), horas(d[0]), (int) d[1],
                    porcentaje(d[0], horasTotales), MonedaUtil.formatear(d[2]));
        }
        if (!ranking.isEmpty() && ranking.get(0).getValue()[0] > 0) {
            rep.agregarResumen("Cancha más utilizada: " + ranking.get(0).getKey().getNombre()
                    + " con " + horas(ranking.get(0).getValue()[0]));
        } else {
            rep.agregarResumen("No hay reservas en el período.");
        }
        return rep;
    }

    // =====================================================================
    //  7. HORARIOS DE MAYOR DEMANDA
    // =====================================================================

    /**
     * Cuenta cuántas reservas (sin canceladas) ocupan cada franja de 1 hora.
     * Una reserva de 17:00 a 19:00 suma en "17:00-18:00" y en "18:00-19:00".
     */
    public Reporte horariosMayorDemanda(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<Integer, Integer> porHora = new TreeMap<>();
        Map<DayOfWeek, Integer> porDia = new EnumMap<>(DayOfWeek.class);
        int totalOcupaciones = 0;

        for (Reserva r : reservasEnRango(desde, hasta)) {
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                continue;
            }
            porDia.merge(r.getInicio().getDayOfWeek(), 1, Integer::sum);
            LocalDateTime franja = r.getInicio().withMinute(0);
            while (franja.isBefore(r.getFin())) {
                porHora.merge(franja.getHour(), 1, Integer::sum);
                totalOcupaciones++;
                franja = franja.plusHours(1);
            }
        }

        List<Map.Entry<Integer, Integer>> ranking = new ArrayList<>(porHora.entrySet());
        ranking.sort((a, b) -> b.getValue().equals(a.getValue())
                ? Integer.compare(a.getKey(), b.getKey())
                : Integer.compare(b.getValue(), a.getValue()));

        Reporte rep = new Reporte("Horarios de mayor demanda", textoPeriodo(desde, hasta),
                "Puesto", "Franja", "Reservas", "% de la demanda");
        int puesto = 1;
        for (Map.Entry<Integer, Integer> e : ranking) {
            rep.agregarFila(puesto++, String.format("%02d:00 - %02d:00", e.getKey(), e.getKey() + 1),
                    e.getValue(), porcentaje(e.getValue(), totalOcupaciones));
        }
        if (!ranking.isEmpty()) {
            int h = ranking.get(0).getKey();
            rep.agregarResumen(String.format("Hora pico: %02d:00 - %02d:00", h, h + 1));
        }
        DayOfWeek diaPico = null;
        for (Map.Entry<DayOfWeek, Integer> e : porDia.entrySet()) {
            if (diaPico == null || e.getValue() > porDia.get(diaPico)) {
                diaPico = e.getKey();
            }
        }
        if (diaPico != null) {
            rep.agregarResumen("Día con más reservas: " + nombreDia(diaPico) + " (" + porDia.get(diaPico) + ")");
        }
        return rep;
    }

    // =====================================================================
    //  8. CLIENTES CON MAYOR CANTIDAD DE RESERVAS
    // =====================================================================

    /**
     * @param limite cuántos clientes mostrar (0 = todos)
     */
    public Reporte clientesFrecuentes(LocalDate desde, LocalDate hasta, int limite) throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<Cliente, double[]> datos = new LinkedHashMap<>(); // [0]=reservas [1]=canceladas [2]=horas [3]=gastado
        for (Reserva r : reservasEnRango(desde, hasta)) {
            double[] d = datos.computeIfAbsent(r.getCliente(), k -> new double[4]);
            d[0]++;
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                d[1]++;
            } else {
                d[2] += r.getHoras();
                d[3] += r.calcularTotal() + complejo.calcularConsumoTienda(r);
            }
        }
        List<Map.Entry<Cliente, double[]>> ranking = new ArrayList<>(datos.entrySet());
        // Más reservas primero; si empatan, el que más gastó.
        ranking.sort((a, b) -> a.getValue()[0] != b.getValue()[0]
                ? Double.compare(b.getValue()[0], a.getValue()[0])
                : Double.compare(b.getValue()[3], a.getValue()[3]));
        if (limite > 0 && ranking.size() > limite) {
            ranking = ranking.subList(0, limite);
        }

        Reporte rep = new Reporte("Clientes con más reservas", textoPeriodo(desde, hasta)
                + (limite > 0 ? " · Top " + limite : ""),
                "Puesto", "Cliente", "Teléfono", "Reservas", "Canceladas", "Horas jugadas", "Total gastado");
        int puesto = 1;
        for (Map.Entry<Cliente, double[]> e : ranking) {
            double[] d = e.getValue();
            rep.agregarFila(puesto++, e.getKey().getNombre(), e.getKey().getTelefono(), (int) d[0], (int) d[1],
                    horas(d[2]), MonedaUtil.formatear(d[3]));
        }
        rep.agregarResumen("Total gastado incluye cancha, servicios y consumo de tienda (sin canceladas).");
        return rep;
    }

    // =====================================================================
    //  REPORTE MENSUAL (prueba #12: totales correctos)
    // =====================================================================

    /** Resumen de un mes: cantidades, horas, facturado, cobrado, saldo y tienda. */
    public Reporte reporteMensual(YearMonth mes) throws DatoInvalidoException {
        if (mes == null) {
            throw new DatoInvalidoException("Seleccione el mes del reporte.");
        }
        LocalDate desde = mes.atDay(1);
        LocalDate hasta = mes.atEndOfMonth();

        Map<EstadoReserva, Integer> porEstado = new EnumMap<>(EstadoReserva.class);
        for (EstadoReserva e : EstadoReserva.values()) {
            porEstado.put(e, 0);
        }
        int total = 0;
        double horasVendidas = 0;
        double facturado = 0;
        double saldoPendiente = 0;
        for (Reserva r : reservasEnRango(desde, hasta)) {
            total++;
            porEstado.merge(r.getEstado(), 1, Integer::sum);
            if (r.getEstado() != EstadoReserva.CANCELADA) {
                horasVendidas += r.getHoras();
                facturado += r.calcularTotal();
                saldoPendiente += r.calcularSaldo();
            }
        }
        double cobrado = 0;
        for (Reserva r : complejo.getReservas()) {
            for (Pago p : r.getPagos()) {
                if (enRango(p.getFechaHora().toLocalDate(), desde, hasta)) {
                    cobrado += p.getMonto();
                }
            }
        }
        double tienda = 0;
        int unidades = 0;
        for (Venta v : complejo.getVentas()) {
            if (enRango(v.getFechaHora().toLocalDate(), desde, hasta)) {
                tienda += v.calcularTotal();
                unidades += v.contarUnidades();
            }
        }

        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, ES);
        Reporte rep = new Reporte("Reporte mensual", capitalizar(nombreMes) + " " + mes.getYear(),
                "Concepto", "Valor");
        rep.agregarFila("Reservas registradas", total);
        for (Map.Entry<EstadoReserva, Integer> e : porEstado.entrySet()) {
            rep.agregarFila("   " + e.getKey().getDescripcion(), e.getValue());
        }
        rep.agregarFila("Horas vendidas (sin canceladas)", horas(horasVendidas));
        rep.agregarFila("Facturado en reservas (sin canceladas)", MonedaUtil.formatear(facturado));
        rep.agregarFila("Cobrado en el mes (pagos)", MonedaUtil.formatear(cobrado));
        rep.agregarFila("Saldo pendiente de reservas del mes", MonedaUtil.formatear(saldoPendiente));
        rep.agregarFila("Ventas de tienda (" + unidades + " unidades)", MonedaUtil.formatear(tienda));
        rep.agregarFila("INGRESO TOTAL DEL MES (cobrado + tienda)", MonedaUtil.formatear(cobrado + tienda));
        rep.agregarResumen("Facturado = suma de totales de reservas del mes (cancha + servicios).");
        rep.agregarResumen("Cobrado = pagos cuya fecha cae en el mes (criterio de caja).");
        return rep;
    }

    // =====================================================================
    //  EXTRA: VENTAS DE TIENDA POR PRODUCTO
    // =====================================================================

    public Reporte ventasPorProducto(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        validarRango(desde, hasta);
        Map<String, double[]> porProducto = new LinkedHashMap<>(); // [0]=unidades [1]=monto
        for (Venta v : complejo.getVentas()) {
            if (enRango(v.getFechaHora().toLocalDate(), desde, hasta)) {
                for (DetalleVenta d : v.getDetalles()) {
                    double[] datos = porProducto.computeIfAbsent(d.getProducto().getNombre(), k -> new double[2]);
                    datos[0] += d.getCantidad();
                    datos[1] += d.getSubtotal();
                }
            }
        }
        List<Map.Entry<String, double[]>> ranking = new ArrayList<>(porProducto.entrySet());
        ranking.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));

        Reporte rep = new Reporte("Ventas de tienda por producto", textoPeriodo(desde, hasta),
                "Producto", "Unidades", "Monto");
        double total = 0;
        for (Map.Entry<String, double[]> e : ranking) {
            rep.agregarFila(e.getKey(), (int) e.getValue()[0], MonedaUtil.formatear(e.getValue()[1]));
            total += e.getValue()[1];
        }
        rep.agregarResumen("Total vendido: " + MonedaUtil.formatear(total));
        return rep;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static void validarRango(LocalDate desde, LocalDate hasta) throws DatoInvalidoException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new DatoInvalidoException("La fecha inicial (" + FechaUtil.formatearFecha(desde)
                    + ") no puede ser posterior a la final (" + FechaUtil.formatearFecha(hasta) + ").");
        }
    }

    private static boolean enRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }

    /** FILTRAR: reservas cuya fecha de juego cae en el período. */
    private List<Reserva> reservasEnRango(LocalDate desde, LocalDate hasta) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            if (enRango(r.getInicio().toLocalDate(), desde, hasta)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /** Si el usuario no puso fechas, el período va de la primera a la última reserva. */
    private LocalDate[] rangoEfectivo(LocalDate desde, LocalDate hasta) {
        LocalDate min = desde;
        LocalDate max = hasta;
        for (Reserva r : complejo.getReservas()) {
            LocalDate f = r.getInicio().toLocalDate();
            if (desde == null && (min == null || f.isBefore(min))) {
                min = f;
            }
            if (hasta == null && (max == null || f.isAfter(max))) {
                max = f;
            }
        }
        LocalDate hoy = FechaUtil.ahora().toLocalDate();
        return new LocalDate[]{min == null ? hoy : min, max == null ? hoy : max};
    }

    private static String textoPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return "Todo el historial";
        }
        return "Del " + (desde == null ? "inicio" : FechaUtil.formatearFecha(desde))
                + " al " + (hasta == null ? "último registro" : FechaUtil.formatearFecha(hasta));
    }

    private static String horas(double h) {
        return String.format(Locale.US, "%.1f h", h);
    }

    private static String porcentaje(double parte, double total) {
        return total <= 0 ? "0.0%" : String.format(Locale.US, "%.1f%%", parte * 100.0 / total);
    }

    private static String nombreDia(DayOfWeek dia) {
        return capitalizar(dia.getDisplayName(TextStyle.FULL, ES));
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    // =====================================================================
    //  CLASE ANIDADA: resultado genérico de cualquier reporte
    // =====================================================================

    /**
     * Resultado de un reporte en forma de tabla: título, subtítulo (período),
     * nombres de columna, filas y líneas de resumen (totales, conclusiones).
     *
     * <p>Solo el ReporteController puede llenarlo (métodos privados); la vista
     * únicamente lo lee y lo pone en un JTable.</p>
     */
    public static final class Reporte {

        private final String titulo;
        private final String subtitulo;
        private final String[] columnas;
        private final List<Object[]> filas;
        private final List<String> resumen;

        private Reporte(String titulo, String subtitulo, String... columnas) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.columnas = columnas.clone();
            this.filas = new ArrayList<>();
            this.resumen = new ArrayList<>();
        }

        private void agregarFila(Object... valores) {
            filas.add(valores.clone());
        }

        private void agregarResumen(String linea) {
            resumen.add(linea);
        }

        public String getTitulo() {
            return titulo;
        }

        public String getSubtitulo() {
            return subtitulo;
        }

        public String[] getColumnas() {
            return columnas.clone();
        }

        /** @return filas de solo lectura; cada fila tiene un valor por columna */
        public List<Object[]> getFilas() {
            return Collections.unmodifiableList(filas);
        }

        public List<String> getResumen() {
            return Collections.unmodifiableList(resumen);
        }

        public boolean estaVacio() {
            return filas.isEmpty();
        }

        /** Versión en texto plano (consola, pruebas y vista previa). */
        public String aTexto() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(titulo).append(" ===\n").append(subtitulo).append('\n');
            sb.append(String.join(" | ", columnas)).append('\n');
            for (Object[] fila : filas) {
                StringBuilder linea = new StringBuilder();
                for (int i = 0; i < fila.length; i++) {
                    linea.append(i == 0 ? "" : " | ").append(fila[i]);
                }
                sb.append(linea).append('\n');
            }
            for (String r : resumen) {
                sb.append("> ").append(r).append('\n');
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return titulo + " (" + filas.size() + " filas)";
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.canchas.TarifaEspecial;
import com.canchapro.models.canchas.TarifaFranja;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.EstadoProducto;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaNoDisponibleException;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.ConflictoHorarioException;
import com.canchapro.models.exceptions.PagoInvalidoException;
import com.canchapro.models.exceptions.StockInsuficienteException;
import com.canchapro.models.exceptions.TransicionEstadoInvalidaException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Seguridad;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
/**
 *
 * @author SANTANA
 */
/**
 * Pruebas funcionales del MODELO (sin interfaz ni archivos).
 *
 * Ejecuta en consola las pruebas mínimas del PDF que dependen solo de la
 * lógica de negocio: 1 a 9, 13 y 14, más pruebas adicionales de tarifas,
 * limpieza y anticipo. Las pruebas 10 y 11 (archivos) están en
 * PruebasPersistencia (fase 3) y la 12 (reporte mensual) en la fase 7.
 *
 * Cómo ejecutar en NetBeans: clic derecho sobre este archivo → Run File (Shift+F6).
 *
 * Escenario: martes 13 de octubre de 2026, complejo con las mismas canchas,
 * tarifas y productos que los CSV de prueba.
 */
public class PruebasModelo {
    private static int aprobadas = 0;
    private static int fallidas = 0;

    /** Acción de una prueba: puede lanzar cualquier excepción. */
    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    // Datos del escenario
    private static ComplejoDeportivo complejo;
    private static Usuario operador;
    private static Cliente carlos;
    private static Cancha bombonera;
    private static Cancha maracana;
    private static Cancha monumental;
    private static ServicioAdicional arbitro;
    private static ServicioAdicional chalecos;
    private static Producto agua;
    private static Producto chocolate;

    private static final LocalDate DIA = LocalDate.of(2026, 10, 13); // martes

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================================");
        System.out.println("  CanchaPro - Pruebas funcionales del modelo");
        System.out.println("==============================================================");
        prepararEscenario();

        // ---------------------------------------------------------------
        System.out.println("\n--- Motor de reservas (existente: La Bombonera 15:00-17:00) ---");
        crearReservaBase();

        debeAceptar("#1", "Reserva en horario disponible (18:00-19:00)",
                () -> crearReserva(bombonera, 18, 0, 60));

        debeAceptar("#2", "Reserva exactamente despues de otra (17:00-18:00)",
                () -> crearReserva(bombonera, 17, 0, 60));

        debeRechazar("#3", "Reserva parcialmente traslapada (16:00-18:00)",
                ConflictoHorarioException.class,
                () -> crearReserva(bombonera, 16, 0, 120));

        debeRechazar("#4", "Reserva contenida dentro de otra (15:30-16:30)",
                ConflictoHorarioException.class,
                () -> crearReserva(bombonera, 15, 30, 60));

        debeRechazar("#5", "Reserva en cancha en mantenimiento (El Monumental)",
                CanchaNoDisponibleException.class,
                () -> crearReserva(monumental, 10, 0, 60));

        // ---------------------------------------------------------------
        System.out.println("\n--- Pagos (reserva de Q450 + arbitro 2 h Q150 + chalecos Q30 = Q630) ---");
        Reserva r = crearReserva(maracana, 19, 0, 120);
        r.agregarServicio(arbitro, 1);
        r.agregarServicio(chalecos, 1);
        verificar("--", "Total calculado automaticamente = Q630.00",
                MonedaUtil.sonIguales(r.calcularTotal(), 630));

        debeAceptar("#6", "Pago parcial de Q300 -> saldo Q330.00", () -> {
            r.registrarPago(nuevoPago(300));
            exigir(MonedaUtil.sonIguales(r.calcularSaldo(), 330), "saldo esperado Q330, fue " + r.calcularSaldo());
        });

        debeRechazar("#8", "Pago mayor al saldo (Q330.01 con saldo Q330)",
                PagoInvalidoException.class,
                () -> r.registrarPago(nuevoPago(330.01)));

        debeAceptar("#7", "Pago total del saldo -> saldo Q0.00", () -> {
            r.registrarPago(nuevoPago(330));
            exigir(r.estaPagada() && MonedaUtil.esCero(r.calcularSaldo()), "el saldo no quedo en Q0");
        });

        debeRechazar("--", "Pago negativo", PagoInvalidoException.class, () -> nuevoPago(-50));

        // ---------------------------------------------------------------
        System.out.println("\n--- Cancelacion y estados ---");
        Reserva aCancelar = crearReserva(maracana, 9, 0, 60);
        aCancelar.registrarPago(nuevoPago(50));

        debeAceptar("#9", "Cancelar reserva conserva historial", () -> {
            int antes = complejo.getReservas().size();
            aCancelar.cancelar("Lluvia fuerte anunciada", operador, LocalDateTime.of(DIA.minusDays(1), LocalTime.NOON));
            exigir(complejo.getReservas().size() == antes, "la reserva fue eliminada");
            exigir(aCancelar.getEstado() == EstadoReserva.CANCELADA, "el estado no es CANCELADA");
            exigir(aCancelar.getCancelacion().getMotivo().equals("Lluvia fuerte anunciada"), "no guardo el motivo");
            exigir(aCancelar.getCancelacion().getResponsable() == operador, "no guardo el responsable");
            exigir(aCancelar.getPagos().size() == 1, "se perdieron los pagos");
        });

        debeAceptar("--", "El horario de la reserva cancelada queda libre (09:00-10:00)",
                () -> crearReserva(maracana, 9, 0, 60));

        debeRechazar("--", "Reserva finalizada no vuelve a pendiente/confirmada",
                TransicionEstadoInvalidaException.class, () -> {
                    r.confirmar(0);
                    r.iniciar();
                    r.finalizar();
                    r.confirmar(0);
                });

        // ---------------------------------------------------------------
        System.out.println("\n--- Tarifas ---");
        verificar("--", "Martes 16:00-18:00 cruza franjas: Q150 + Q225 = Q375.00",
                MonedaUtil.sonIguales(precio(16, 0, 120), 375));
        verificar("--", "Martes 16:30-17:30: 30 min diurno + 30 min nocturno = Q187.50",
                MonedaUtil.sonIguales(precio(16, 30, 60), 187.5));
        verificar("--", "Domingo 01/11 (feriado) usa tarifa especial Q275, no fin de semana Q250",
                MonedaUtil.sonIguales(complejo.getCalculadoraTarifa().calcularPrecioCancha(
                        LocalDateTime.of(2026, 11, 1, 10, 0), LocalDateTime.of(2026, 11, 1, 11, 0)), 275));

        // ---------------------------------------------------------------
        System.out.println("\n--- Cambios en vivo por configuracion ---");
        complejo.getConfiguracion().setMinutosLimpieza(15);
        debeRechazar("--", "Con 15 min de limpieza: 19:00 despues de 18:00-19:00 se rechaza",
                ConflictoHorarioException.class, () -> crearReserva(bombonera, 19, 0, 60));
        debeAceptar("--", "Con 15 min de limpieza: 19:15 se acepta",
                () -> crearReserva(bombonera, 19, 15, 60));
        complejo.getConfiguracion().setMinutosLimpieza(0);

        Reserva conAnticipo = crearReserva(maracana, 10, 0, 60);
        debeRechazar("--", "Anticipo minimo 25%: confirmar sin pago se rechaza",
                PagoInvalidoException.class, () -> conAnticipo.confirmar(25));
        debeAceptar("--", "Anticipo minimo 25%: con 25% pagado se confirma", () -> {
            conAnticipo.registrarPago(nuevoPago(conAnticipo.calcularTotal() * 0.25));
            conAnticipo.confirmar(25);
        });

        // ---------------------------------------------------------------
        System.out.println("\n--- Tienda / Cafeteria ---");
        debeAceptar("#13", "Venta con stock disponible disminuye inventario (agua 44 -> 40)", () -> {
            Venta venta = new Venta(complejo.generarCodigo(GeneradorCodigo.Tipo.VENTA),
                    LocalDateTime.of(DIA, LocalTime.of(16, 0)), null, operador);
            venta.agregarProducto(agua, 4);
            venta.confirmar(MetodoPago.EFECTIVO);
            complejo.agregarVenta(venta);
            exigir(agua.getExistencia() == 40, "existencia esperada 40, fue " + agua.getExistencia());
        });

        debeRechazar("#14", "Venta mayor a existencia (7 chocolates con 6)",
                StockInsuficienteException.class, () -> {
                    Venta venta = new Venta(complejo.generarCodigo(GeneradorCodigo.Tipo.VENTA),
                            LocalDateTime.of(DIA, LocalTime.of(16, 5)), null, operador);
                    venta.agregarProducto(chocolate, 7);
                });
        verificar("--", "El inventario de chocolate no cambio (sigue en 6)", chocolate.getExistencia() == 6);

        // ---------------------------------------------------------------
        System.out.println("\n==============================================================");
        System.out.printf("  Resultado: %d aprobadas, %d fallidas%n", aprobadas, fallidas);
        System.out.println(fallidas == 0 ? "  TODAS LAS PRUEBAS DEL MODELO PASARON"
                                         : "  HAY PRUEBAS FALLIDAS: revise los mensajes de arriba");
        System.out.println("==============================================================");
    }

    // =====================================================================
    //  ESCENARIO
    // =====================================================================

    private static void prepararEscenario() throws CanchaProException {
        complejo = new ComplejoDeportivo(new ConfiguracionSistema());

        operador = new Usuario("U002", "Operador Turno Tarde", "55550002", "operador",
                Seguridad.hashSha256("operador123"), RolUsuario.OPERADOR);
        complejo.agregarUsuario(operador);

        carlos = new Cliente("CL0001", "Carlos Perez", "44112233", "", LocalDate.of(2026, 8, 15));
        complejo.agregarCliente(carlos);

        LocalTime abre = LocalTime.of(8, 0);
        LocalTime cierra = LocalTime.of(23, 0);
        bombonera = new Cancha("C01", "La Bombonera", TipoCancha.FUTBOL_5, 10, EstadoCancha.DISPONIBLE, abre, cierra);
        maracana = new Cancha("C02", "El Maracana", TipoCancha.FUTBOL_5, 10, EstadoCancha.DISPONIBLE, abre, cierra);
        monumental = new Cancha("C03", "El Monumental", TipoCancha.FUTBOL_7, 14, EstadoCancha.MANTENIMIENTO, abre, cierra);
        complejo.agregarCancha(bombonera);
        complejo.agregarCancha(maracana);
        complejo.agregarCancha(monumental);

        Set<DayOfWeek> lunesAViernes = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        Set<DayOfWeek> finDeSemana = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        complejo.agregarTarifa(new TarifaFranja("T01", "Lunes a viernes diurno", 150, 1,
                lunesAViernes, LocalTime.of(8, 0), LocalTime.of(17, 0)));
        complejo.agregarTarifa(new TarifaFranja("T02", "Lunes a viernes nocturno", 225, 1,
                lunesAViernes, LocalTime.of(17, 0), LocalTime.of(23, 0)));
        complejo.agregarTarifa(new TarifaFranja("T03", "Fin de semana", 250, 1,
                finDeSemana, LocalTime.of(8, 0), LocalTime.of(23, 0)));
        complejo.agregarTarifa(new TarifaEspecial("T04", "Dia de Todos los Santos", 275, 10,
                LocalDate.of(2026, 11, 1), "Feriado nacional"));

        arbitro = new ServicioAdicional("S03", "Arbitro", 75, true);
        chalecos = new ServicioAdicional("S02", "Juego de chalecos", 30, false);
        complejo.agregarServicio(arbitro);
        complejo.agregarServicio(chalecos);

        agua = new Producto("P001", "Agua pura 600 ml", CategoriaProducto.BEBIDA, 5, 44, 12, EstadoProducto.ACTIVO);
        chocolate = new Producto("P005", "Chocolate barra", CategoriaProducto.GOLOSINA, 4, 6, 8, EstadoProducto.ACTIVO);
        complejo.agregarProducto(agua);
        complejo.agregarProducto(chocolate);
    }

    private static void crearReservaBase() throws CanchaProException {
        crearReserva(bombonera, 15, 0, 120);
    }

    /**
     * Crea una reserva con el MISMO flujo que usará el ReservaController:
     * 1) validar disponibilidad con el motor, 2) calcular precio, 3) crear y agregar.
     */
    private static Reserva crearReserva(Cancha cancha, int hora, int minuto, int minutos)
            throws CanchaProException {
        LocalDateTime inicio = LocalDateTime.of(DIA, LocalTime.of(hora, minuto));
        LocalDateTime fin = inicio.plusMinutes(minutos);
        complejo.getMotorDisponibilidad().validarDisponibilidad(cancha, inicio, fin, complejo.getReservas());
        double precio = complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, fin);
        Reserva reserva = new Reserva(complejo.generarCodigo(GeneradorCodigo.Tipo.RESERVA), carlos, cancha,
                inicio, Duration.ofMinutes(minutos), precio, operador, LocalDateTime.of(DIA, LocalTime.of(7, 0)));
        complejo.agregarReserva(reserva);
        return reserva;
    }

    private static Pago nuevoPago(double monto) throws PagoInvalidoException {
        return new Pago(complejo.generarCodigo(GeneradorCodigo.Tipo.PAGO),
                LocalDateTime.of(DIA, LocalTime.of(8, 0)), monto, MetodoPago.EFECTIVO, "");
    }

    private static double precio(int hora, int minuto, int minutos) throws CanchaProException {
        LocalDateTime inicio = LocalDateTime.of(DIA, LocalTime.of(hora, minuto));
        return complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, inicio.plusMinutes(minutos));
    }

    // =====================================================================
    //  MINI-FRAMEWORK DE PRUEBAS
    // =====================================================================

    /** La acción debe terminar sin excepción. */
    private static void debeAceptar(String numero, String descripcion, Accion accion) {
        try {
            accion.ejecutar();
            registrar(true, numero, descripcion, "aceptada");
        } catch (Exception e) {
            registrar(false, numero, descripcion, "se esperaba aceptar, pero fallo: " + e.getMessage());
        }
    }

    /** La acción debe lanzar exactamente el tipo de excepción esperado. */
    private static void debeRechazar(String numero, String descripcion,
                                     Class<? extends Exception> esperada, Accion accion) {
        try {
            accion.ejecutar();
            registrar(false, numero, descripcion, "se esperaba " + esperada.getSimpleName() + " y fue aceptada");
        } catch (Exception e) {
            boolean ok = esperada.isInstance(e);
            registrar(ok, numero, descripcion, ok
                    ? "rechazada -> " + e.getMessage()
                    : "excepcion distinta: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private static void verificar(String numero, String descripcion, boolean condicion) {
        registrar(condicion, numero, descripcion, condicion ? "correcto" : "valor incorrecto");
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }

    private static void registrar(boolean ok, String numero, String descripcion, String detalle) {
        if (ok) {
            aprobadas++;
        } else {
            fallidas++;
        }
        System.out.printf("[%s] %-4s %s%n        %s%n", ok ? " OK  " : "FALLA", numero, descripcion, detalle);
    }
}

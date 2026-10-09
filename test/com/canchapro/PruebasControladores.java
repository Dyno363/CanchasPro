/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro;

import com.canchapro.controllers.EstadoVisualCancha;
import com.canchapro.controllers.MapaController;
import com.canchapro.controllers.PagoController;
import com.canchapro.controllers.PosController;
import com.canchapro.controllers.ReporteController;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.Sesion;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.ConflictoHorarioException;
import com.canchapro.models.exceptions.PagoInvalidoException;
import com.canchapro.models.exceptions.StockInsuficienteException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.persistence.ResultadoCarga;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Cierre de la FASE 4: flujo completo usando SOLO controladores, tal como lo
 * hará la interfaz gráfica:
 *
 * 
 *  arrancar → login → cliente → disponibilidad → reserva → choque → pagos
 *  → mapa / POS → cancelación → reporte → guardar → reiniciar → verificar
 */
public class PruebasControladores {

    private static int aprobadas = 0;
    private static int fallidas = 0;

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================================");
        System.out.println("  CanchaPro - Flujo completo por controladores (Fase 4)");
        System.out.println("==============================================================");

        if (!Files.isDirectory(GestorPersistencia.CARPETA_DATOS_PRUEBA)) {
            System.out.println("No se encontró datos_prueba/. Ejecute desde NetBeans con el proyecto abierto.");
            return;
        }
        Path carpeta = Files.createTempDirectory("canchapro_flujo_");
        try {
            copiarDatosPrueba(carpeta);
            ejecutarFlujo(carpeta);
        } finally {
            eliminarCarpeta(carpeta);
        }

        System.out.println("\n==============================================================");
        System.out.printf("  Resultado: %d aprobadas, %d fallidas%n", aprobadas, fallidas);
        System.out.println(fallidas == 0 ? "  FLUJO COMPLETO CORRECTO: FASE 4 TERMINADA"
                                         : "  HAY PRUEBAS FALLIDAS: revise los mensajes de arriba");
        System.out.println("==============================================================");
    }

    private static void ejecutarFlujo(Path carpeta) throws Exception {
        // Fecha de juego: pasado mañana a las 18:00 (siempre en el futuro).
        LocalDate dia = FechaUtil.ahora().toLocalDate().plusDays(2);
        LocalDateTime inicio = LocalDateTime.of(dia, LocalTime.of(18, 0));

        // ---------------------------------------------------------------- 1
        System.out.println("\n--- 1. Arranque y login ---");
        SistemaController sistema = new SistemaController(carpeta);
        ResultadoCarga carga = sistema.iniciar();
        System.out.println("        " + carga.getResumen());
        verificar("Carga inicial sin errores", !carga.hayErrores());

        debeRechazar("Login con contraseña incorrecta", CanchaProException.class,
                () -> sistema.iniciarSesion("operador", "mala".toCharArray()));
        Sesion sesion = sistema.iniciarSesion("operador", "operador123".toCharArray());
        verificar("Login del operador: " + sesion.getResumen(), sistema.haySesion());
        debeRechazar("Un operador no puede crear usuarios", CanchaProException.class,
                () -> sistema.getUsuarioController().crear("X", "44445555", "xx", RolUsuario.OPERADOR,
                        "123456".toCharArray(), "123456".toCharArray()));

        // ---------------------------------------------------------------- 2
        System.out.println("\n--- 2. Cliente y disponibilidad ---");
        debeRechazar("Cliente con teléfono repetido", CanchaProException.class,
                () -> sistema.getClienteController().registrar("Otro Carlos", "4411-2233", ""));
        Cliente cliente = sistema.getClienteController().registrar("Equipo Los Pumas", "5522-3344", "");
        verificar("Cliente nuevo " + cliente, cliente.getCodigo().equals("CL0005"));

        ReservaController reservas = sistema.getReservaController();
        verificar("Búsqueda automática: C01 y C02 libres (C03 en mantenimiento)",
                reservas.buscarCanchasDisponibles(dia, LocalTime.of(18, 0), 120, TipoCancha.FUTBOL_5).size() == 2);

        // ---------------------------------------------------------------- 3
        System.out.println("\n--- 3. Reserva, choque y contigua ---");
        Map<String, Integer> servicios = new LinkedHashMap<>();
        servicios.put("S03", 1); // árbitro por hora
        Reserva reserva = reservas.crear(cliente.getCodigo(), "C02", inicio, 120, servicios);
        verificar("#1 Reserva aceptada: " + reserva, reserva.getEstado() == EstadoReserva.PENDIENTE);
        double total = reserva.calcularTotal();
        System.out.println("        Total calculado: Q" + total + " (cancha + árbitro)");

        debeRechazar("#3 Reserva traslapada rechazada", ConflictoHorarioException.class,
                () -> reservas.crear("CL0001", "C02", inicio.plusHours(1), 120, null));
        debeRechazar("#4 Reserva contenida rechazada", ConflictoHorarioException.class,
                () -> reservas.crear("CL0001", "C02", inicio.plusMinutes(30), 60, null));
        debeRechazar("#5 Cancha en mantenimiento rechazada", CanchaProException.class,
                () -> reservas.crear("CL0001", "C03", inicio, 60, null));
        Reserva contigua = reservas.crear("CL0002", "C02", inicio.plusHours(2), 60, null);
        verificar("#2 Reserva justo después aceptada: " + contigua.getCodigo(), contigua != null);

        // ---------------------------------------------------------------- 4
        System.out.println("\n--- 4. Pagos ---");
        PagoController pagos = sistema.getPagoController();
        pagos.registrarPago(reserva.getCodigo(), "Q100", MetodoPago.EFECTIVO, "Anticipo");
        verificar("#6 Abono Q100: " + pagos.getResumen(reserva.getCodigo()),
                MonedaUtil.sonIguales(pagos.getResumen(reserva.getCodigo()).getSaldo(), total - 100));
        reservas.confirmar(reserva.getCodigo());
        debeRechazar("#8 Pago mayor al saldo rechazado", PagoInvalidoException.class,
                () -> pagos.registrarPago(reserva.getCodigo(), "99999", MetodoPago.EFECTIVO, ""));
        pagos.pagarSaldo(reserva.getCodigo(), MetodoPago.TARJETA, "");
        verificar("#7 Pago total: saldo Q0.00", pagos.getResumen(reserva.getCodigo()).estaPagada());

        // ---------------------------------------------------------------- 5
        System.out.println("\n--- 5. Mapa dinámico y POS ---");
        MapaController mapa = sistema.getMapaController();
        boolean c02EnCurso = mapa.getEstadoCanchas(inicio.plusMinutes(30)).stream()
                .anyMatch(f -> f.getCancha().getCodigo().equals("C02") && f.getEstado() == EstadoVisualCancha.EN_CURSO);
        verificar("Mapa a las 18:30 de ese día: El Maracaná EN CURSO", c02EnCurso);

        PosController pos = sistema.getPosController();
        int aguaAntes = sistema.getTiendaController().obtener("P001").getExistencia();
        pos.iniciarVentaParaReserva(reserva.getCodigo());
        pos.agregarProducto("P001", 3);
        debeRechazar("#14 Venta mayor a la existencia rechazada", StockInsuficienteException.class,
                () -> pos.agregarProducto("P005", 500));
        pos.confirmar(MetodoPago.EFECTIVO);
        int aguaDespues = sistema.getTiendaController().obtener("P001").getExistencia();
        verificar("#13 Inventario de agua " + aguaAntes + " → " + aguaDespues, aguaDespues == aguaAntes - 3);

        // ---------------------------------------------------------------- 6
        System.out.println("\n--- 6. Cancelación ---");
        reservas.cancelar(contigua.getCodigo(), "El equipo no completó jugadores");
        verificar("#9 Cancelada conserva motivo y responsable",
                contigua.getEstado() == EstadoReserva.CANCELADA
                && contigua.getCancelacion().getResponsable().getUsername().equals("operador"));
        verificar("#9 El horario quedó libre para otra reserva",
                reservas.buscarCanchasDisponibles(dia, LocalTime.of(20, 0), 60, TipoCancha.FUTBOL_5).size() == 2);

        // ---------------------------------------------------------------- 7
        System.out.println("\n--- 7. Reporte mensual ---");
        ReporteController.Reporte mensual = sistema.getReporteController().reporteMensual(YearMonth.from(dia));
        System.out.print(indentar(mensual.aTexto()));
        verificar("#12 El reporte mensual incluye la reserva y su cobro", !mensual.estaVacio());

        // ---------------------------------------------------------------- 8
        System.out.println("\n--- 8. Guardar al cerrar y reiniciar ---");
        String codigoReserva = reserva.getCodigo();
        String codigoCancelada = contigua.getCodigo();
        sistema.cerrarSesion();
        debeRechazar("Sin sesión no se entregan controladores", IllegalStateException.class,
                () -> sistema.getReservaController());
        System.out.println("        " + sistema.cerrar());

        SistemaController reinicio = new SistemaController(carpeta);
        ResultadoCarga recarga = reinicio.iniciar();
        System.out.println("        Al reiniciar: " + recarga.getResumen());
        reinicio.iniciarSesion("admin", "admin123".toCharArray());
        Reserva r = reinicio.getReservaController().obtener(codigoReserva);
        verificar("#10 La reserva permanece pagada y con su árbitro",
                r.estaPagada() && r.getServicios().size() == 1 && r.getPagos().size() == 2);
        verificar("#10 La cancelación permanece",
                reinicio.getReservaController().obtener(codigoCancelada).getEstado() == EstadoReserva.CANCELADA);
        verificar("#10 El inventario permanece descontado",
                reinicio.getTiendaController().obtener("P001").getExistencia() == aguaDespues);
        verificar("#10 El consumo de tienda sigue ligado a la reserva",
                MonedaUtil.sonIguales(reinicio.getTiendaController().getConsumoDeReserva(codigoReserva), 15.0));

        // ---------------------------------------------------------------- 9
        System.out.println("\n--- 9. Restaurar datos de prueba ---");
        System.out.println("        " + reinicio.restaurarDatosPrueba());
        System.out.println("        " + reinicio.cerrar());
        SistemaController limpio = new SistemaController(carpeta);
        limpio.iniciar();
        limpio.iniciarSesion("admin", "admin123".toCharArray());
        verificar("Tras restaurar vuelven los 4 clientes originales",
                limpio.getClienteController().listar().size() == 4);
    }

    // =====================================================================
    //  APOYO
    // =====================================================================

    private static void copiarDatosPrueba(Path destino) throws IOException {
        for (String nombre : GestorPersistencia.ARCHIVOS) {
            Path origen = GestorPersistencia.CARPETA_DATOS_PRUEBA.resolve(nombre);
            if (Files.isRegularFile(origen)) {
                Files.copy(origen, destino.resolve(nombre));
            }
        }
    }

    private static void eliminarCarpeta(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            List<Path> rutas = new ArrayList<>();
            recorrido.forEach(rutas::add);
            Collections.reverse(rutas);
            for (Path ruta : rutas) {
                Files.deleteIfExists(ruta);
            }
        }
    }

    private static String indentar(String texto) {
        return "        " + texto.replace("\n", "\n        ").stripTrailing() + "\n";
    }

    private static void debeRechazar(String descripcion, Class<? extends Exception> esperada, Accion accion) {
        try {
            accion.ejecutar();
            registrar(false, descripcion, "se aceptó, pero debía rechazarse");
        } catch (Exception e) {
            boolean ok = esperada.isInstance(e);
            registrar(ok, descripcion, (ok ? "rechazada: " : "excepción inesperada: ") + e.getMessage());
        }
    }

    private static void verificar(String descripcion, boolean condicion) {
        registrar(condicion, descripcion, condicion ? "correcto" : "valor incorrecto");
    }

    private static void registrar(boolean ok, String descripcion, String detalle) {
        if (ok) {
            aprobadas++;
        } else {
            fallidas++;
        }
        System.out.printf("[%s] %s%n        %s%n", ok ? " OK  " : "FALLA", descripcion, detalle);
    }
}
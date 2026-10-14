/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.canchapro;

import com.canchapro.controllers.CanchaController;
import com.canchapro.controllers.ClienteController;
import com.canchapro.controllers.ConfiguracionController;
import com.canchapro.controllers.LoginController;
import com.canchapro.controllers.MapaController;
import com.canchapro.controllers.PagoController;
import com.canchapro.controllers.PosController;
import com.canchapro.controllers.ReporteController;
import com.canchapro.controllers.ReservaController;
import com.canchapro.controllers.Sesion;
import com.canchapro.controllers.ServicioController;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.TarifaController;
import com.canchapro.controllers.TiendaController;
import com.canchapro.controllers.UsuarioController;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.canchas.CalculadoraTarifa;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.canchas.Tarifa;
import com.canchapro.models.canchas.TarifaEspecial;
import com.canchapro.models.canchas.TarifaFranja;
import com.canchapro.models.enums.CategoriaProducto;
import com.canchapro.models.enums.EstadoCancha;
import com.canchapro.models.enums.EstadoProducto;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.enums.TipoCancha;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.models.reservas.ServicioReserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.persistence.ResultadoCarga;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

/**
 * CanchaPro en CONSOLA: el sistema completo, sin interfaz gráfica.
 *
 * Esta clase es una VISTA de texto. Igual que lo harán las ventanas Swing,
 * solo hace tres cosas: pedir datos, llamar a un controlador y mostrar el
 * resultado o el mensaje de error. No calcula precios, no valida reglas y no
 * escribe archivos (eso lo hacen los controladores y el modelo).
 *
 * Al iniciar se elige:
 * 
 *   Datos reales (carpeta data/): al salir se guardan los cambios.
 *   Copia temporal de datos_prueba/: para practicar sin miedo; se borra al salir.
 * 
 *
 */
public class CanchaProConsola {

    private final Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8);
    private SistemaController sistema;
    private Path carpetaTemporal;

    /** Se lanza cuando la entrada se termina (por ejemplo, al cerrar la consola). */
    private static final class FinDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws CanchaProException;
    }

    public static void main(String[] args) {
        // Tildes y ñ correctas en la consola de NetBeans: se escribe y se lee en UTF-8.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        new CanchaProConsola().ejecutar();
    }

    // =====================================================================
    //  CICLO PRINCIPAL: arrancar → login → menú → salir
    // =====================================================================

    private void ejecutar() {
        titulo("CANCHAPRO - Sistema de reservas (versión consola)");
        try {
            prepararSistema();
            boolean salir = false;
            while (!salir) {
                if (!login()) {
                    break;
                }
                salir = menuPrincipal();
            }
        } catch (FinDeEntrada e) {
            System.out.println("\n(Fin de la entrada)");
        } catch (IOException e) {
            System.out.println("[ERROR] No se pudo preparar la carpeta de trabajo: " + e.getMessage());
        } finally {
            cerrarPrograma();
        }
    }

    private void prepararSistema() throws IOException {
        System.out.println("¿Con qué datos desea trabajar?");
        System.out.println("  1. Datos reales (carpeta data/; se guardan al salir)");
        System.out.println("  2. Copia temporal de datos_prueba/ (para practicar; no se guarda nada)");
        int modo = leerOpcion(1, 2);
        if (modo == 2) {
            carpetaTemporal = Files.createTempDirectory("canchapro_consola_");
            for (String nombre : GestorPersistencia.ARCHIVOS) {
                Path origen = GestorPersistencia.CARPETA_DATOS_PRUEBA.resolve(nombre);
                if (Files.isRegularFile(origen)) {
                    Files.copy(origen, carpetaTemporal.resolve(nombre));
                }
            }
            sistema = new SistemaController(carpetaTemporal);
        } else {
            sistema = new SistemaController();
        }

        ResultadoCarga carga = sistema.iniciar();
        System.out.println("\n" + carga.getResumen());
        for (ResultadoCarga.Incidencia e : carga.getErrores()) {
            System.out.println("  [ERROR] " + e);
        }
        for (ResultadoCarga.Incidencia a : carga.getAdvertencias()) {
            System.out.println("  [AVISO] " + a);
        }
    }

    /** @return true si inició sesión; false si el usuario quiso salir o se bloqueó */
    private boolean login() {
        LoginController login = sistema.getLoginController();
        titulo(sistema.getNombreComplejo() + " - Inicio de sesión");
        System.out.println("(Usuarios de prueba: admin/admin123 u operador/operador123; escriba 'salir' para terminar)");
        while (true) {
            String usuario = leer("Usuario");
            if (usuario.equalsIgnoreCase("salir")) {
                return false;
            }
            String clave = leer("Contraseña");
            try {
                Sesion sesion = sistema.iniciarSesion(usuario, clave.toCharArray());
                ok("Bienvenido, " + sesion.getResumen());
                return true;
            } catch (CanchaProException e) {
                error(e.getMessage());
                if (login.isBloqueado()) {
                    return false;
                }
            }
        }
    }

    /** @return true si se eligió salir del programa; false si solo se cerró la sesión */
    private boolean menuPrincipal() {
        while (true) {
            Sesion sesion = sistema.getSesion();
            boolean tienda = sistema.getTiendaController().isModuloActivo();
            titulo(sistema.getNombreComplejo() + " | " + sesion.getUsuario().getNombre()
                    + " (" + sesion.getRol() + ") | " + FechaUtil.formatearFechaHora(FechaUtil.ahora()));
            System.out.println("  1. Reservas");
            System.out.println("  2. Pagos");
            System.out.println("  3. Mapa de canchas y POS");
            System.out.println("  4. Clientes");
            System.out.println("  5. Tienda / Cafetería" + (tienda ? "" : "  (módulo desactivado)"));
            System.out.println("  6. Reportes");
            System.out.println("  --- Administración ---");
            System.out.println("  7. Canchas y horarios de atención");
            System.out.println("  8. Tarifas (franjas y especiales)");
            System.out.println("  9. Servicios adicionales");
            System.out.println(" 10. Usuarios" + admin());
            System.out.println(" 11. Configuración y Guardar Sistema");
            System.out.println("  --- Cuenta ---");
            System.out.println(" 12. Cambiar mi contraseña");
            System.out.println(" 13. Cerrar sesión (volver al login)");
            System.out.println("  0. Salir del programa");
            switch (leerOpcion(0, 13)) {
                case 1: menuReservas(); break;
                case 2: menuPagos(); break;
                case 3: menuMapa(); break;
                case 4: menuClientes(); break;
                case 5: menuTienda(); break;
                case 6: menuReportes(); break;
                case 7: menuCanchas(); break;
                case 8: menuTarifas(); break;
                case 9: menuServicios(); break;
                case 10: menuUsuarios(); break;
                case 11: menuConfiguracion(); break;
                case 12: accion(this::cambiarMiContrasena); break;
                case 13:
                    sistema.cerrarSesion();
                    ok("Sesión cerrada.");
                    return false;
                default:
                    return true;
            }
        }
    }

    private void cerrarPrograma() {
        if (sistema != null && sistema.estaIniciado()) {
            try {
                if (carpetaTemporal != null) {
                    System.out.println("Modo práctica: los cambios NO se guardan en data/.");
                } else {
                    ok(sistema.cerrar());
                }
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
        if (carpetaTemporal != null) {
            borrarCarpeta(carpetaTemporal);
        }
        System.out.println("Hasta pronto.");
    }

    // =====================================================================
    //  1. RESERVAS
    // =====================================================================

    private void menuReservas() {
        ReservaController rc = sistema.getReservaController();
        while (true) {
            subtitulo("RESERVAS");
            System.out.println("  1. Listar / filtrar reservas");
            System.out.println("  2. Buscar canchas disponibles");
            System.out.println("  3. Nueva reserva");
            System.out.println("  4. Ver detalle de una reserva");
            System.out.println("  5. Agregar servicio a una reserva");
            System.out.println("  6. Quitar servicio de una reserva");
            System.out.println("  7. Confirmar reserva");
            System.out.println("  8. Iniciar partido (En curso)");
            System.out.println("  9. Finalizar partido");
            System.out.println(" 10. Cancelar reserva");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 10)) {
                case 1: accion(this::listarReservas); break;
                case 2: accion(this::buscarDisponibles); break;
                case 3: accion(this::nuevaReserva); break;
                case 4: accion(() -> imprimirReserva(rc.obtener(leerCodigo("Código de reserva")))); break;
                case 5: accion(() -> {
                    String codigo = leerCodigo("Código de reserva");
                    listarServiciosActivos();
                    Reserva r = rc.agregarServicio(codigo, leerCodigo("Código de servicio"), leerEntero("Cantidad"));
                    ok("Servicio agregado. Nuevo total: " + MonedaUtil.formatear(r.calcularTotal()));
                }); break;
                case 6: accion(() -> {
                    Reserva r = rc.quitarServicio(leerCodigo("Código de reserva"), leerCodigo("Código de servicio"));
                    ok("Servicio quitado. Nuevo total: " + MonedaUtil.formatear(r.calcularTotal()));
                }); break;
                case 7: accion(() -> ok("Reserva " + rc.confirmar(leerCodigo("Código de reserva")).getEstado())); break;
                case 8: accion(() -> ok("Reserva " + rc.iniciar(leerCodigo("Código de reserva")).getEstado())); break;
                case 9: accion(() -> ok("Reserva " + rc.finalizar(leerCodigo("Código de reserva")).getEstado())); break;
                case 10: accion(() -> {
                    String codigo = leerCodigo("Código de reserva");
                    Reserva r = rc.cancelar(codigo, leer("Motivo de la cancelación"));
                    ok(r.getCancelacion().toString());
                }); break;
                default: return;
            }
        }
    }

    private void listarReservas() throws CanchaProException {
        ReservaController rc = sistema.getReservaController();
        LocalDate fecha = leerFechaOpcional("Fecha dd/MM/aaaa (Enter = todas, 'hoy' = hoy)");
        EstadoReserva estado = elegirEnum("Estado (Enter = todos)", rc.getEstados(), true);
        String texto = leerOpcional("Buscar cliente/cancha/código (Enter = todo)");
        List<Object[]> filas = new ArrayList<>();
        for (Reserva r : rc.filtrar(fecha, estado, texto)) {
            filas.add(new Object[]{r.getCodigo(), r.getCliente().getNombre(), r.getCancha().getNombre(),
                    FechaUtil.formatearRango(r.getInicio(), r.getFin()), r.getEstado(),
                    MonedaUtil.formatear(r.calcularTotal()), MonedaUtil.formatear(r.calcularSaldo())});
        }
        tabla(new String[]{"Código", "Cliente", "Cancha", "Horario", "Estado", "Total", "Saldo"}, filas);
    }

    private void buscarDisponibles() throws CanchaProException {
        ReservaController rc = sistema.getReservaController();
        LocalDate fecha = leerFecha("Fecha dd/MM/aaaa ('hoy' = hoy)");
        LocalTime hora = leerHora("Hora de inicio HH:mm");
        int minutos = leerDuracion();
        TipoCancha tipo = elegirEnum("Tipo de cancha (Enter = cualquiera)", TipoCancha.values(), true);
        List<Cancha> libres = rc.buscarCanchasDisponibles(fecha, hora, minutos, tipo);
        if (libres.isEmpty()) {
            error("No hay canchas disponibles en ese horario.");
            return;
        }
        double precio = rc.cotizarCancha(LocalDateTime.of(fecha, hora), minutos);
        ok("Canchas disponibles (precio de cancha " + MonedaUtil.formatear(precio) + "):");
        for (Cancha c : libres) {
            System.out.println("    " + c);
        }
    }

    private void nuevaReserva() throws CanchaProException {
        ReservaController rc = sistema.getReservaController();
        Cliente cliente = elegirCliente();
        System.out.println("Canchas que aceptan reservas:");
        for (Cancha c : sistema.getCanchaController().listarOperativas()) {
            System.out.println("    " + c + "  [" + c.getHorarioTexto() + "]");
        }
        String cancha = leerCodigo("Código de cancha");
        LocalDate fecha = leerFecha("Fecha dd/MM/aaaa ('hoy' = hoy)");
        LocalTime hora = leerHora("Hora de inicio HH:mm");
        int minutos = leerDuracion();
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);

        System.out.println("Desglose del precio de la cancha:");
        for (CalculadoraTarifa.Tramo t : sistema.getTarifaController().desglosar(inicio, inicio.plusMinutes(minutos))) {
            System.out.println("    " + t);
        }

        Map<String, Integer> servicios = new LinkedHashMap<>();
        if (leerSiNo("¿Agregar servicios adicionales?")) {
            listarServiciosActivos();
            while (true) {
                String codigo = leerOpcional("Código de servicio (Enter = terminar)");
                if (codigo.isEmpty()) {
                    break;
                }
                servicios.merge(codigo.toUpperCase(), leerEntero("Cantidad"), Integer::sum);
            }
        }
        Reserva r = rc.crear(cliente.getCodigo(), cancha, inicio, minutos, servicios);
        ok("Reserva creada:");
        imprimirReserva(r);
        if (leerSiNo("¿Registrar un pago ahora?")) {
            registrarPago(r.getCodigo());
        }
    }

    private Cliente elegirCliente() throws CanchaProException {
        ClienteController cc = sistema.getClienteController();
        while (true) {
            String criterio = leer("Cliente: escriba código, nombre o teléfono ('nuevo' = registrar)");
            if (criterio.equalsIgnoreCase("nuevo")) {
                return registrarCliente();
            }
            List<Cliente> encontrados = cc.buscar(criterio);
            if (encontrados.size() == 1) {
                ok("Cliente: " + encontrados.get(0));
                return encontrados.get(0);
            }
            if (encontrados.isEmpty()) {
                error("No se encontró. Escriba 'nuevo' para registrarlo.");
            } else {
                for (Cliente c : encontrados) {
                    System.out.println("    " + c + " - Tel. " + c.getTelefono());
                }
                System.out.println("  Hay varias coincidencias: escriba el código exacto.");
            }
        }
    }

    private void imprimirReserva(Reserva r) throws CanchaProException {
        System.out.println("  + " + r);
        System.out.println("  | Cliente: " + r.getCliente().getDescripcion());
        System.out.println("  | Registrada por " + r.getCreadaPor().getUsername() + " el "
                + FechaUtil.formatearFechaHora(r.getFechaCreacion()));
        System.out.println("  | Cancha (" + r.getHoras() + " h): " + MonedaUtil.formatear(r.getPrecioCancha()));
        for (ServicioReserva s : r.getServicios()) {
            System.out.println("  |   + " + s.getConcepto() + " = " + MonedaUtil.formatear(s.getSubtotal()));
        }
        System.out.println("  | TOTAL: " + MonedaUtil.formatear(r.calcularTotal()));
        for (Pago p : r.getPagos()) {
            System.out.println("  |   Pago " + p);
        }
        System.out.println("  | Pagado: " + MonedaUtil.formatear(r.calcularTotalPagado())
                + " | SALDO: " + MonedaUtil.formatear(r.calcularSaldo()));
        if (sistema.getTiendaController().isModuloActivo()) {
            double consumo = sistema.getTiendaController().getConsumoDeReserva(r.getCodigo());
            if (consumo > 0) {
                System.out.println("  | Consumo en tienda: " + MonedaUtil.formatear(consumo));
            }
        }
        if (r.getCancelacion() != null) {
            System.out.println("  | " + r.getCancelacion());
        }
        System.out.println("  +");
    }

    private void listarServiciosActivos() {
        System.out.println("Servicios disponibles:");
        for (ServicioAdicional s : sistema.getServicioController().listarActivos()) {
            System.out.println("    " + s + " - " + s.getModalidadTexto());
        }
    }

    // =====================================================================
    //  2. PAGOS
    // =====================================================================

    private void menuPagos() {
        PagoController pc = sistema.getPagoController();
        while (true) {
            subtitulo("PAGOS");
            System.out.println("  1. Ver resumen de cobro de una reserva");
            System.out.println("  2. Registrar abono");
            System.out.println("  3. Pagar saldo completo");
            System.out.println("  4. Reservas con saldo pendiente");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 4)) {
                case 1: accion(() -> {
                    String codigo = leerCodigo("Código de reserva");
                    PagoController.ResumenCobro rc = pc.getResumen(codigo);
                    System.out.println("  " + rc);
                    System.out.println("  Anticipo mínimo: " + MonedaUtil.formatear(rc.getAnticipoMinimo())
                            + " (falta " + MonedaUtil.formatear(rc.getFaltaParaAnticipo()) + ")");
                    for (Pago p : pc.getPagos(codigo)) {
                        System.out.println("    " + p);
                    }
                }); break;
                case 2: accion(() -> registrarPago(leerCodigo("Código de reserva"))); break;
                case 3: accion(() -> {
                    String codigo = leerCodigo("Código de reserva");
                    System.out.println("  Saldo a pagar: " + MonedaUtil.formatear(pc.getResumen(codigo).getSaldo()));
                    MetodoPago metodo = elegirEnum("Método de pago", pc.getMetodos(), false);
                    Pago p = pc.pagarSaldo(codigo, metodo, leerOpcional("Observación / referencia"));
                    ok("Pago " + p + " | " + pc.getResumen(codigo));
                }); break;
                case 4: imprimirReporte(sistema.getReporteController().pagosPendientes()); break;
                default: return;
            }
        }
    }

    private void registrarPago(String codigo) throws CanchaProException {
        PagoController pc = sistema.getPagoController();
        System.out.println("  " + pc.getResumen(codigo));
        String monto = leer("Monto (ej. 150 o Q150.00)");
        MetodoPago metodo = elegirEnum("Método de pago", pc.getMetodos(), false);
        String observacion = leerOpcional("Observación / referencia");
        Pago p = pc.registrarPago(codigo, monto, metodo, observacion);
        ok("Pago registrado: " + p);
        System.out.println("  " + pc.getResumen(codigo));
        if (pc.puedeConfirmarse(codigo) && leerSiNo("Ya cubre el anticipo. ¿Confirmar la reserva?")) {
            ok("Reserva " + sistema.getReservaController().confirmar(codigo).getEstado());
        }
    }

    // =====================================================================
    //  3. MAPA Y POS
    // =====================================================================

    private void menuMapa() {
        MapaController mc = sistema.getMapaController();
        while (true) {
            subtitulo("MAPA DE CANCHAS - " + FechaUtil.formatearFechaHora(FechaUtil.ahora()));
            imprimirMapa(mc.getEstadoCanchas());
            System.out.println("  1. Refrescar (actualiza estados por la hora)");
            System.out.println("  2. Ver detalle de una cancha");
            System.out.println("  3. Abrir POS desde una cancha EN CURSO");
            System.out.println("  4. Ver el mapa en otra fecha y hora (simulación)");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 4)) {
                case 1: ok(mc.refrescar() + " reservas cambiaron de estado."); break;
                case 2: accion(() -> System.out.println(sangria(mc.getInfo(leerCodigo("Código de cancha")).getDetalle()))); break;
                case 3: accion(() -> {
                    Reserva r = mc.getReservaParaPos(leerCodigo("Código de cancha"));
                    sistema.getPosController().iniciarVentaParaReserva(r.getCodigo());
                    pos();
                }); break;
                case 4: accion(() -> {
                    LocalDateTime momento = LocalDateTime.of(leerFecha("Fecha dd/MM/aaaa"), leerHora("Hora HH:mm"));
                    imprimirMapa(mc.getEstadoCanchas(momento));
                }); break;
                default: return;
            }
        }
    }

    private void imprimirMapa(List<MapaController.InfoCancha> fichas) {
        for (MapaController.InfoCancha f : fichas) {
            System.out.printf("  [%-17s] %s - %s%n        %s%n", f.getEstado().getDescripcion().toUpperCase(),
                    f.getCancha().getCodigo(), f.getCancha().getNombre(), f.getSubtitulo());
        }
    }

    /** Pantalla del POS: carrito abierto en el PosController. */
    private void pos() {
        PosController pos = sistema.getPosController();
        TiendaController tienda = sistema.getTiendaController();
        while (pos.hayVentaAbierta()) {
            subtitulo(pos.getTitulo() + " - Venta " + pos.getVentaActual().getNumero());
            List<Object[]> productos = new ArrayList<>();
            for (Producto p : tienda.listarDisponibles()) {
                productos.add(new Object[]{p.getCodigo(), p.getNombre(), MonedaUtil.formatear(p.getPrecio()),
                        p.getExistencia()});
            }
            tabla(new String[]{"Código", "Producto", "Precio", "Existencia"}, productos);
            System.out.println("  Carrito:");
            for (DetalleVenta d : pos.getLineas()) {
                System.out.println("    " + d);
            }
            System.out.println("  TOTAL: " + MonedaUtil.formatear(pos.getTotal()));
            System.out.println("  1. Agregar producto   2. Quitar producto   3. Cobrar   0. Cancelar venta");
            switch (leerOpcion(0, 3)) {
                case 1: accion(() -> pos.agregarProducto(leerCodigo("Código de producto"), leerEntero("Cantidad"))); break;
                case 2: accion(() -> pos.quitarProducto(leerCodigo("Código de producto"))); break;
                case 3: accion(() -> {
                    Venta v = pos.confirmar(elegirEnum("Método de pago", MetodoPago.values(), false));
                    ok("Venta cobrada: " + v);
                }); break;
                default:
                    pos.cancelarVenta();
                    ok("Venta cancelada (el inventario no se modificó).");
            }
        }
    }

    // =====================================================================
    //  4. CLIENTES
    // =====================================================================

    private void menuClientes() {
        ClienteController cc = sistema.getClienteController();
        while (true) {
            subtitulo("CLIENTES");
            System.out.println("  1. Listar / buscar");
            System.out.println("  2. Registrar cliente");
            System.out.println("  3. Editar cliente");
            System.out.println("  4. Historial de reservas de un cliente");
            System.out.println("  5. Eliminar cliente (solo sin reservas)" + admin());
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 5)) {
                case 1: {
                    List<Object[]> filas = new ArrayList<>();
                    for (Cliente c : cc.buscar(leerOpcional("Buscar (Enter = todos)"))) {
                        filas.add(new Object[]{c.getCodigo(), c.getNombre(), c.getTelefono(), c.getCorreo(),
                                FechaUtil.formatearFecha(c.getFechaRegistro())});
                    }
                    tabla(new String[]{"Código", "Nombre", "Teléfono", "Correo", "Registro"}, filas);
                    break;
                }
                case 2: accion(() -> registrarCliente()); break;
                case 3: accion(() -> {
                    Cliente c = cc.obtener(leerCodigo("Código de cliente"));
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", c.getNombre());
                    String tel = leerConDefecto("Teléfono", c.getTelefono());
                    String correo = leerConDefecto("Correo", c.getCorreo());
                    ok("Actualizado: " + cc.actualizar(c.getCodigo(), nombre, tel, correo));
                }); break;
                case 4: accion(() -> {
                    List<Object[]> filas = new ArrayList<>();
                    for (Reserva r : cc.getHistorial(leerCodigo("Código de cliente"))) {
                        filas.add(new Object[]{r.getCodigo(), r.getCancha().getNombre(),
                                FechaUtil.formatearRango(r.getInicio(), r.getFin()), r.getEstado(),
                                MonedaUtil.formatear(r.calcularTotal())});
                    }
                    tabla(new String[]{"Reserva", "Cancha", "Horario", "Estado", "Total"}, filas);
                }); break;
                case 5: accion(() -> {
                    Cliente c = cc.obtener(leerCodigo("Código de cliente"));
                    if (leerSiNo("¿Eliminar a " + c + "?")) {
                        cc.eliminar(c.getCodigo());
                        ok("Cliente eliminado.");
                    }
                }); break;
                default: return;
            }
        }
    }

    private Cliente registrarCliente() throws CanchaProException {
        ClienteController cc = sistema.getClienteController();
        System.out.println("  Código asignado: " + cc.getSiguienteCodigo());
        Cliente c = cc.registrar(leer("Nombre completo"), leer("Teléfono (8 dígitos)"), leerOpcional("Correo (opcional)"));
        ok("Cliente registrado: " + c);
        return c;
    }

    // =====================================================================
    //  5. TIENDA
    // =====================================================================

    private void menuTienda() {
        TiendaController tc = sistema.getTiendaController();
        if (!tc.isModuloActivo()) {
            error("El módulo de Tienda/Cafetería está desactivado (Configuración → modulo_tienda).");
            return;
        }
        while (true) {
            subtitulo("TIENDA / CAFETERÍA");
            System.out.println("  1. Productos e inventario");
            System.out.println("  2. Productos bajo stock mínimo");
            System.out.println("  3. Venta de mostrador (POS sin reserva)");
            System.out.println("  4. Reponer inventario");
            System.out.println("  5. Registrar producto" + admin());
            System.out.println("  6. Ventas de hoy");
            System.out.println("  7. Modificar producto" + admin());
            System.out.println("  8. Activar / desactivar producto" + admin());
            System.out.println("  9. Eliminar producto (solo si nunca se vendió)" + admin());
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 9)) {
                case 1: imprimirProductos(tc.listarProductos()); break;
                case 2: imprimirProductos(tc.getProductosBajoMinimo()); break;
                case 3: accion(() -> {
                    sistema.getPosController().iniciarVentaMostrador();
                    pos();
                }); break;
                case 4: accion(() -> {
                    Producto p = tc.reponerStock(leerCodigo("Código de producto"), leerEntero("Unidades que ingresan"));
                    ok(p.getNombre() + ": existencia " + p.getExistencia());
                }); break;
                case 5: accion(() -> {
                    CategoriaProducto cat = elegirEnum("Categoría", tc.getCategorias(), false);
                    Producto p = tc.crearProducto(leer("Nombre"), cat, MonedaUtil.leer(leer("Precio")),
                            leerEntero("Existencia inicial"), leerEntero("Stock mínimo"));
                    ok("Producto creado: " + p);
                }); break;
                case 6: {
                    List<Object[]> filas = new ArrayList<>();
                    for (Venta v : tc.listarVentas(FechaUtil.ahora().toLocalDate())) {
                        filas.add(new Object[]{v.getNumero(), FechaUtil.formatearHora(v.getFechaHora().toLocalTime()),
                                v.estaAsociadaAReserva() ? v.getReserva().getCodigo() : "Mostrador",
                                v.contarUnidades(), MonedaUtil.formatear(v.calcularTotal())});
                    }
                    tabla(new String[]{"Venta", "Hora", "Reserva", "Unidades", "Total"}, filas);
                    System.out.println("  Total vendido hoy: "
                            + MonedaUtil.formatear(tc.getTotalVendido(FechaUtil.ahora().toLocalDate())));
                    break;
                }
                case 7: accion(() -> {
                    Producto p = tc.obtener(leerCodigo("Código de producto"));
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", p.getNombre());
                    CategoriaProducto cat = elegirEnumConDefecto("Categoría", tc.getCategorias(), p.getCategoria());
                    double precio = leerMontoConDefecto("Precio", p.getPrecio());
                    int minimo = leerEnteroConDefecto("Stock mínimo", p.getStockMinimo());
                    ok("Actualizado: " + tc.actualizarProducto(p.getCodigo(), nombre, cat, precio, minimo));
                }); break;
                case 8: accion(() -> {
                    String codigo = leerCodigo("Código de producto");
                    EstadoProducto estado = elegirEnum("Nuevo estado", tc.getEstados(), false);
                    tc.cambiarEstado(codigo, estado);
                    ok("Producto " + codigo + ": " + estado);
                }); break;
                case 9: accion(() -> {
                    Producto p = tc.obtener(leerCodigo("Código de producto"));
                    if (leerSiNo("¿Eliminar " + p + "?")) {
                        tc.eliminarProducto(p.getCodigo());
                        ok("Producto eliminado.");
                    }
                }); break;
                default: return;
            }
        }
    }

    private void imprimirProductos(List<Producto> productos) {
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : productos) {
            filas.add(new Object[]{p.getCodigo(), p.getNombre(), p.getCategoria(), MonedaUtil.formatear(p.getPrecio()),
                    p.getExistencia(), p.getStockMinimo(), p.getEstado(), p.estaBajoMinimo() ? "REABASTECER" : ""});
        }
        tabla(new String[]{"Código", "Producto", "Categoría", "Precio", "Exist.", "Mín.", "Estado", "Alerta"}, filas);
    }

    // =====================================================================
    //  6. REPORTES
    // =====================================================================

    private void menuReportes() {
        ReporteController rp = sistema.getReporteController();
        while (true) {
            subtitulo("REPORTES");
            System.out.println("  1. Reservas por fecha");
            System.out.println("  2. Reservas por cancha");
            System.out.println("  3. Ingresos por período");
            System.out.println("  4. Pagos pendientes");
            System.out.println("  5. Reservas canceladas");
            System.out.println("  6. Cancha más utilizada");
            System.out.println("  7. Horarios de mayor demanda");
            System.out.println("  8. Clientes con más reservas");
            System.out.println("  9. Reporte mensual");
            System.out.println(" 10. Ventas de tienda por producto");
            System.out.println("  0. Volver");
            int op = leerOpcion(0, 10);
            if (op == 0) {
                return;
            }
            accion(() -> {
                ReporteController.Reporte rep;
                if (op == 4) {
                    rep = rp.pagosPendientes();
                } else if (op == 9) {
                    rep = rp.reporteMensual(leerMes());
                } else {
                    LocalDate desde = leerFechaOpcional("Desde dd/MM/aaaa (Enter = sin límite)");
                    LocalDate hasta = leerFechaOpcional("Hasta dd/MM/aaaa (Enter = sin límite)");
                    switch (op) {
                        case 1: rep = rp.reservasPorFecha(desde, hasta,
                                elegirEnum("Estado (Enter = todos)", EstadoReserva.values(), true)); break;
                        case 2: rep = rp.reservasPorCancha(desde, hasta); break;
                        case 3: rep = rp.ingresosPorPeriodo(desde, hasta); break;
                        case 5: rep = rp.reservasCanceladas(desde, hasta); break;
                        case 6: rep = rp.canchaMasUtilizada(desde, hasta); break;
                        case 7: rep = rp.horariosMayorDemanda(desde, hasta); break;
                        case 8: rep = rp.clientesFrecuentes(desde, hasta, 10); break;
                        default: rep = rp.ventasPorProducto(desde, hasta); break;
                    }
                }
                imprimirReporte(rep);
            });
        }
    }

    private void imprimirReporte(ReporteController.Reporte rep) {
        System.out.println("\n  === " + rep.getTitulo().toUpperCase() + " ===");
        System.out.println("  " + rep.getSubtitulo());
        tabla(rep.getColumnas(), rep.getFilas());
        for (String linea : rep.getResumen()) {
            System.out.println("  > " + linea);
        }
    }

    // =====================================================================
    //  7. CANCHAS Y HORARIOS DE ATENCIÓN
    // =====================================================================

    private void menuCanchas() {
        CanchaController cc = sistema.getCanchaController();
        while (true) {
            subtitulo("CANCHAS Y HORARIOS DE ATENCIÓN");
            System.out.println("  1. Listar canchas");
            System.out.println("  2. Registrar cancha" + admin());
            System.out.println("  3. Modificar cancha (nombre, tipo, capacidad, horario)" + admin());
            System.out.println("  4. Cambiar horario de atención de UNA cancha" + admin());
            System.out.println("  5. Cambiar horario de atención de TODAS las canchas" + admin());
            System.out.println("  6. Cambiar estado (Disponible / Mantenimiento / Inactiva)" + admin());
            System.out.println("  7. Eliminar cancha (solo sin reservas)" + admin());
            System.out.println("  8. Ver reservas activas de una cancha");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 8)) {
                case 1: imprimirCanchas(cc.listar()); break;
                case 2: accion(() -> {
                    String nombre = leer("Nombre de la cancha");
                    TipoCancha tipo = elegirEnum("Tipo", cc.getTipos(), false);
                    int capacidad = leerEntero("Capacidad (jugadores, máx. " + tipo.getJugadoresMaximos() + ")");
                    LocalTime abre = leerHora("Hora de apertura HH:mm");
                    LocalTime cierra = leerHora("Hora de cierre HH:mm");
                    ok("Cancha registrada: " + cc.crear(nombre, tipo, capacidad, abre, cierra));
                    avisarHuecosDeTarifa();
                }); break;
                case 3: accion(() -> {
                    Cancha c = cc.obtener(leerCodigo("Código de cancha"));
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", c.getNombre());
                    TipoCancha tipo = elegirEnumConDefecto("Tipo", cc.getTipos(), c.getTipo());
                    int capacidad = leerEnteroConDefecto("Capacidad", c.getCapacidad());
                    LocalTime abre = leerHoraConDefecto("Hora de apertura", c.getHoraApertura());
                    LocalTime cierra = leerHoraConDefecto("Hora de cierre", c.getHoraCierre());
                    ok("Actualizada: " + cc.actualizar(c.getCodigo(), nombre, tipo, capacidad, abre, cierra)
                            + " [" + c.getHorarioTexto() + "]");
                    avisarHuecosDeTarifa();
                }); break;
                case 4: accion(() -> {
                    Cancha c = cc.obtener(leerCodigo("Código de cancha"));
                    System.out.println("  Horario actual: " + c.getHorarioTexto());
                    LocalTime abre = leerHoraConDefecto("Nueva hora de apertura", c.getHoraApertura());
                    LocalTime cierra = leerHoraConDefecto("Nueva hora de cierre", c.getHoraCierre());
                    ok(cc.actualizarHorario(c.getCodigo(), abre, cierra) + " ahora atiende " + c.getHorarioTexto());
                    avisarHuecosDeTarifa();
                }); break;
                case 5: accion(() -> {
                    LocalTime abre = leerHora("Hora de apertura para TODAS las canchas HH:mm");
                    LocalTime cierra = leerHora("Hora de cierre para TODAS las canchas HH:mm");
                    ok(cc.cambiarHorarioGeneral(abre, cierra) + " canchas atienden ahora de "
                            + FechaUtil.formatearHora(abre) + " a " + FechaUtil.formatearHora(cierra) + ".");
                    avisarHuecosDeTarifa();
                }); break;
                case 6: accion(() -> {
                    String codigo = leerCodigo("Código de cancha");
                    EstadoCancha estado = elegirEnum("Nuevo estado", cc.getEstados(), false);
                    List<Reserva> afectadas = cc.cambiarEstado(codigo, estado);
                    ok("Estado actualizado a " + estado + ".");
                    for (Reserva r : afectadas) {
                        System.out.println("  [AVISO] Reserva activa afectada: " + r);
                    }
                }); break;
                case 7: accion(() -> {
                    Cancha c = cc.obtener(leerCodigo("Código de cancha"));
                    if (leerSiNo("¿Eliminar " + c + "?")) {
                        cc.eliminar(c.getCodigo());
                        ok("Cancha eliminada.");
                    }
                }); break;
                case 8: accion(() -> {
                    List<Reserva> activas = cc.getReservasActivas(leerCodigo("Código de cancha"));
                    if (activas.isEmpty()) {
                        System.out.println("  (sin reservas activas)");
                    }
                    for (Reserva r : activas) {
                        System.out.println("    " + r);
                    }
                }); break;
                default: return;
            }
        }
    }

    private void imprimirCanchas(List<Cancha> canchas) {
        List<Object[]> filas = new ArrayList<>();
        for (Cancha c : canchas) {
            filas.add(new Object[]{c.getCodigo(), c.getNombre(), c.getTipo(), c.getCapacidad(),
                    c.getEstado(), c.getHorarioTexto()});
        }
        tabla(new String[]{"Código", "Nombre", "Tipo", "Capacidad", "Estado", "Horario"}, filas);
    }

    /** Si el nuevo horario dejó horas sin tarifa, lo avisa (esas horas no se podrían reservar). */
    private void avisarHuecosDeTarifa() {
        for (String h : sistema.getTarifaController().getHuecosDeCobertura()) {
            System.out.println("  [AVISO] Horario sin tarifa (no se podrá reservar): " + h);
        }
    }

    // =====================================================================
    //  8. TARIFAS: CRUD DE FRANJAS Y ESPECIALES
    // =====================================================================

    private void menuTarifas() {
        TarifaController tc = sistema.getTarifaController();
        while (true) {
            subtitulo("TARIFAS (FRANJAS Y ESPECIALES)");
            System.out.println("  1. Listar tarifas");
            System.out.println("  2. Crear franja horaria" + admin());
            System.out.println("  3. Modificar franja horaria" + admin());
            System.out.println("  4. Crear tarifa especial (feriado / evento)" + admin());
            System.out.println("  5. Modificar tarifa especial" + admin());
            System.out.println("  6. Eliminar tarifa" + admin());
            System.out.println("  7. Cotizar precio de cancha");
            System.out.println("  8. Revisar horarios sin tarifa");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 8)) {
                case 1: imprimirTarifas(tc.listar()); break;
                case 2: accion(() -> {
                    String nombre = leer("Nombre (ej. Lunes a viernes nocturno)");
                    double precio = MonedaUtil.leer(leer("Precio por hora"));
                    int prioridad = leerEntero("Prioridad (las normales usan 1)");
                    Set<DayOfWeek> dias = leerDias(null);
                    LocalTime desde = leerHora("Hora de inicio HH:mm");
                    LocalTime hasta = leerHora("Hora de fin HH:mm (no incluida)");
                    ok("Franja creada: " + tc.crearFranja(nombre, precio, prioridad, dias, desde, hasta));
                    avisarHuecosDeTarifa();
                }); break;
                case 3: accion(() -> {
                    Tarifa t = tc.obtener(leerCodigo("Código de la franja"));
                    if (!(t instanceof TarifaFranja)) {
                        error(t.getCodigo() + " es una tarifa especial; use la opción 5.");
                        return;
                    }
                    TarifaFranja f = (TarifaFranja) t;
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", f.getNombre());
                    double precio = leerMontoConDefecto("Precio por hora", f.getPrecioHora());
                    int prioridad = leerEnteroConDefecto("Prioridad", f.getPrioridad());
                    Set<DayOfWeek> dias = leerDias(f.getDias());
                    LocalTime desde = leerHoraConDefecto("Hora de inicio", f.getHoraInicio());
                    LocalTime hasta = leerHoraConDefecto("Hora de fin", f.getHoraFin());
                    ok("Actualizada: " + tc.actualizarFranja(f.getCodigo(), nombre, precio, prioridad, dias, desde, hasta));
                    avisarHuecosDeTarifa();
                }); break;
                case 4: accion(() -> ok("Tarifa especial creada: " + tc.crearEspecial(leer("Nombre (ej. Navidad)"),
                        MonedaUtil.leer(leer("Precio por hora")), leerEntero("Prioridad (ej. 10, mayor que las franjas)"),
                        leerFecha("Fecha dd/MM/aaaa"), leer("Motivo")))); break;
                case 5: accion(() -> {
                    Tarifa t = tc.obtener(leerCodigo("Código de la tarifa especial"));
                    if (!(t instanceof TarifaEspecial)) {
                        error(t.getCodigo() + " es una franja horaria; use la opción 3.");
                        return;
                    }
                    TarifaEspecial te = (TarifaEspecial) t;
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", te.getNombre());
                    double precio = leerMontoConDefecto("Precio por hora", te.getPrecioHora());
                    int prioridad = leerEnteroConDefecto("Prioridad", te.getPrioridad());
                    LocalDate fecha = leerFechaConDefecto("Fecha", te.getFecha());
                    String motivo = leerConDefecto("Motivo", te.getMotivo());
                    ok("Actualizada: " + tc.actualizarEspecial(te.getCodigo(), nombre, precio, prioridad, fecha, motivo));
                }); break;
                case 6: accion(() -> {
                    Tarifa t = tc.obtener(leerCodigo("Código de la tarifa"));
                    if (leerSiNo("¿Eliminar " + t + "? (las reservas ya hechas conservan su precio)")) {
                        List<String> huecos = tc.eliminar(t.getCodigo());
                        ok("Tarifa eliminada.");
                        for (String h : huecos) {
                            System.out.println("  [AVISO] Horario sin tarifa (no se podrá reservar): " + h);
                        }
                    }
                }); break;
                case 7: accion(() -> {
                    LocalDateTime inicio = LocalDateTime.of(leerFecha("Fecha dd/MM/aaaa"), leerHora("Hora HH:mm"));
                    int minutos = leerDuracion();
                    for (CalculadoraTarifa.Tramo t : tc.desglosar(inicio, inicio.plusMinutes(minutos))) {
                        System.out.println("    " + t);
                    }
                    ok("Total cancha: " + MonedaUtil.formatear(tc.cotizar(inicio, inicio.plusMinutes(minutos))));
                }); break;
                case 8: {
                    List<String> huecos = tc.getHuecosDeCobertura();
                    if (huecos.isEmpty()) {
                        ok("Todos los horarios de atención tienen tarifa.");
                    }
                    for (String h : huecos) {
                        System.out.println("  [AVISO] Sin tarifa: " + h);
                    }
                    break;
                }
                default: return;
            }
        }
    }

    private void imprimirTarifas(List<Tarifa> tarifas) {
        List<Object[]> filas = new ArrayList<>();
        for (Tarifa t : tarifas) {
            filas.add(new Object[]{t.getCodigo(), t.getTipoTarifa(), t.getNombre(),
                    MonedaUtil.formatear(t.getPrecioHora()), t.getPrioridad(), t.getDescripcionAplicacion()});
        }
        tabla(new String[]{"Código", "Tipo", "Nombre", "Precio/h", "Prioridad", "Aplica"}, filas);
    }

    // =====================================================================
    //  9. SERVICIOS ADICIONALES
    // =====================================================================

    private void menuServicios() {
        ServicioController sc = sistema.getServicioController();
        while (true) {
            subtitulo("SERVICIOS ADICIONALES");
            System.out.println("  1. Listar servicios (activos e inactivos)");
            System.out.println("  2. Crear servicio" + admin());
            System.out.println("  3. Modificar servicio" + admin());
            System.out.println("  4. Activar / desactivar servicio" + admin());
            System.out.println("  5. Eliminar servicio (solo si nunca se usó)" + admin());
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 5)) {
                case 1: {
                    List<Object[]> filas = new ArrayList<>();
                    for (ServicioAdicional s : sc.listar()) {
                        filas.add(new Object[]{s.getCodigo(), s.getNombre(), MonedaUtil.formatear(s.getPrecio()),
                                s.getModalidadTexto(), s.isActivo() ? "Activo" : "Inactivo"});
                    }
                    tabla(new String[]{"Código", "Servicio", "Precio", "Cobro", "Estado"}, filas);
                    break;
                }
                case 2: accion(() -> ok("Servicio creado: " + sc.crear(leer("Nombre"),
                        MonedaUtil.leer(leer("Precio")), leerSiNo("¿Se cobra por hora?")))); break;
                case 3: accion(() -> {
                    ServicioAdicional s = sc.obtener(leerCodigo("Código de servicio"));
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", s.getNombre());
                    double precio = leerMontoConDefecto("Precio", s.getPrecio());
                    boolean porHora = leerSiNo("¿Se cobra por hora? (actual: " + s.getModalidadTexto() + ")");
                    ok("Actualizado: " + sc.actualizar(s.getCodigo(), nombre, precio, porHora));
                }); break;
                case 4: accion(() -> {
                    String codigo = leerCodigo("Código de servicio");
                    boolean activo = leerSiNo("¿Dejarlo ACTIVO? (n = desactivar)");
                    sc.cambiarEstado(codigo, activo);
                    ok("Servicio " + codigo + (activo ? " activado." : " desactivado."));
                }); break;
                case 5: accion(() -> {
                    ServicioAdicional s = sc.obtener(leerCodigo("Código de servicio"));
                    if (leerSiNo("¿Eliminar " + s + "?")) {
                        sc.eliminar(s.getCodigo());
                        ok("Servicio eliminado.");
                    }
                }); break;
                default: return;
            }
        }
    }

    // =====================================================================
    //  10. USUARIOS
    // =====================================================================

    private void menuUsuarios() {
        UsuarioController uc = sistema.getUsuarioController();
        if (!uc.puedeGestionarUsuarios()) {
            error("Solo un administrador puede gestionar usuarios.");
            return;
        }
        while (true) {
            subtitulo("USUARIOS");
            System.out.println("  1. Listar usuarios");
            System.out.println("  2. Crear usuario");
            System.out.println("  3. Activar / desactivar usuario");
            System.out.println("  4. Restablecer contraseña");
            System.out.println("  5. Modificar usuario (nombre, teléfono, rol)");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 5)) {
                case 1: accion(() -> {
                    List<Object[]> filas = new ArrayList<>();
                    for (Usuario u : uc.listar()) {
                        filas.add(new Object[]{u.getCodigo(), u.getNombre(), u.getUsername(), u.getRol(),
                                u.isActivo() ? "Activo" : "Inactivo"});
                    }
                    tabla(new String[]{"Código", "Nombre", "Usuario", "Rol", "Estado"}, filas);
                }); break;
                case 2: accion(() -> {
                    String nombre = leer("Nombre completo");
                    String tel = leer("Teléfono");
                    String username = leer("Nombre de usuario");
                    RolUsuario rol = elegirEnum("Rol", uc.getRoles(), false);
                    ok("Usuario creado: " + uc.crear(nombre, tel, username, rol,
                            leer("Contraseña").toCharArray(), leer("Confirme la contraseña").toCharArray()));
                }); break;
                case 3: accion(() -> {
                    String codigo = leerCodigo("Código de usuario");
                    boolean activo = leerSiNo("¿Dejarlo ACTIVO? (n = desactivar)");
                    uc.cambiarEstado(codigo, activo);
                    ok("Usuario " + codigo + (activo ? " activado." : " desactivado."));
                }); break;
                case 4: accion(() -> {
                    uc.restablecerContrasena(leerCodigo("Código de usuario"), leer("Nueva contraseña").toCharArray(),
                            leer("Confirme la contraseña").toCharArray());
                    ok("Contraseña restablecida.");
                }); break;
                case 5: accion(() -> {
                    Usuario u = uc.obtener(leerCodigo("Código de usuario"));
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre", u.getNombre());
                    String tel = leerConDefecto("Teléfono", u.getTelefono());
                    RolUsuario rol = elegirEnumConDefecto("Rol", uc.getRoles(), u.getRol());
                    ok("Actualizado: " + uc.actualizar(u.getCodigo(), nombre, tel, rol).getDescripcion());
                }); break;
                default: return;
            }
        }
    }

    private void cambiarMiContrasena() throws CanchaProException {
        sistema.getUsuarioController().cambiarMiContrasena(leer("Contraseña actual").toCharArray(),
                leer("Nueva contraseña").toCharArray(), leer("Confirme la nueva contraseña").toCharArray());
        ok("Su contraseña fue cambiada.");
    }

    // =====================================================================
    //  11. CONFIGURACIÓN
    // =====================================================================

    private void menuConfiguracion() {
        ConfiguracionController cc = sistema.getConfiguracionController();
        while (true) {
            subtitulo("CONFIGURACIÓN");
            System.out.println("  1. Ver configuración y resultado de la carga de archivos");
            System.out.println("  2. Cambiar parámetros (administrador)");
            System.out.println("  3. GUARDAR SISTEMA ahora");
            System.out.println("  4. Ver respaldos");
            System.out.println("  5. Restaurar datos de prueba (administrador)");
            System.out.println("  0. Volver");
            switch (leerOpcion(0, 5)) {
                case 1: {
                    ConfiguracionSistema c = cc.getConfiguracion();
                    System.out.println("  Complejo: " + c.getNombreComplejo());
                    System.out.println("  Tienda/Cafetería: " + (c.isModuloTiendaActivo() ? "ACTIVA" : "desactivada"));
                    System.out.println("  Limpieza entre reservas: " + c.getMinutosLimpieza() + " min");
                    System.out.println("  Anticipo mínimo para confirmar: " + c.getPorcentajeAnticipoMinimo() + "%");
                    System.out.println("  Carpeta de datos: " + cc.getCarpetaDatos());
                    System.out.println("  " + cc.getResumenCarga());
                    for (String e : cc.getErroresCarga()) {
                        System.out.println("    [ERROR] " + e);
                    }
                    for (String a : cc.getAdvertenciasCarga()) {
                        System.out.println("    [AVISO] " + a);
                    }
                    break;
                }
                case 2: accion(() -> {
                    ConfiguracionSistema c = cc.getConfiguracion();
                    System.out.println("  (Enter = conservar el valor actual)");
                    String nombre = leerConDefecto("Nombre del complejo", c.getNombreComplejo());
                    boolean tienda = leerSiNo("¿Tienda/Cafetería activa?");
                    int limpieza = Validador.leerEntero(leerConDefecto("Minutos de limpieza (0-120)",
                            String.valueOf(c.getMinutosLimpieza())), "minutos de limpieza");
                    double anticipo = MonedaUtil.leer(leerConDefecto("Anticipo mínimo % (0-100)",
                            String.valueOf(c.getPorcentajeAnticipoMinimo())));
                    cc.actualizar(nombre, tienda, limpieza, anticipo);
                    ok("Configuración aplicada en memoria (se guarda al salir o con 'Guardar Sistema').");
                }); break;
                case 3: accion(() -> ok(cc.guardarSistema())); break;
                case 4: accion(() -> {
                    List<String> respaldos = cc.getRespaldos();
                    if (respaldos.isEmpty()) {
                        System.out.println("  Aún no hay respaldos.");
                    }
                    for (String r : respaldos) {
                        System.out.println("    " + r);
                    }
                }); break;
                case 5: accion(() -> {
                    if (leerSiNo("Se reemplazarán los datos actuales por los de prueba. ¿Continuar?")) {
                        ok(sistema.restaurarDatosPrueba());
                    }
                }); break;
                default: return;
            }
        }
    }

    // =====================================================================
    //  LECTURA DE DATOS (la vista solo pide y repite si el formato es malo)
    // =====================================================================

    private String leerLinea(String prompt) {
        System.out.print("  " + prompt + ": ");
        if (!entrada.hasNextLine()) {
            throw new FinDeEntrada();
        }
        return entrada.nextLine().trim();
    }

    private String leer(String prompt) {
        while (true) {
            String texto = leerLinea(prompt);
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("  (Este dato es obligatorio)");
        }
    }

    private String leerOpcional(String prompt) {
        return leerLinea(prompt);
    }

    private String leerConDefecto(String prompt, String actual) {
        String texto = leerLinea(prompt + " [" + actual + "]");
        return texto.isEmpty() ? actual : texto;
    }

    private String leerCodigo(String prompt) {
        return leer(prompt).toUpperCase();
    }

    private int leerEntero(String prompt) {
        while (true) {
            try {
                return Validador.leerEntero(leer(prompt), prompt);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private int leerOpcion(int minimo, int maximo) {
        while (true) {
            int op = leerEntero("Opción");
            if (op >= minimo && op <= maximo) {
                return op;
            }
            error("Elija una opción entre " + minimo + " y " + maximo + ".");
        }
    }

    private boolean leerSiNo(String prompt) {
        while (true) {
            String r = leer(prompt + " (s/n)").toLowerCase();
            if (r.equals("s") || r.equals("si") || r.equals("sí")) {
                return true;
            }
            if (r.equals("n") || r.equals("no")) {
                return false;
            }
        }
    }

    private LocalDate leerFecha(String prompt) {
        while (true) {
            String texto = leer(prompt);
            if (texto.equalsIgnoreCase("hoy")) {
                return FechaUtil.ahora().toLocalDate();
            }
            if (texto.equalsIgnoreCase("mañana") || texto.equalsIgnoreCase("manana")) {
                return FechaUtil.ahora().toLocalDate().plusDays(1);
            }
            try {
                return FechaUtil.leerFecha(texto);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private LocalDate leerFechaOpcional(String prompt) {
        while (true) {
            String texto = leerOpcional(prompt);
            if (texto.isEmpty()) {
                return null;
            }
            if (texto.equalsIgnoreCase("hoy")) {
                return FechaUtil.ahora().toLocalDate();
            }
            try {
                return FechaUtil.leerFecha(texto);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private LocalTime leerHora(String prompt) {
        while (true) {
            try {
                return FechaUtil.leerHora(leer(prompt));
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private int leerDuracion() {
        return leerEntero("Duración en minutos " + sistema.getReservaController().getDuracionesPermitidas());
    }

    private YearMonth leerMes() {
        while (true) {
            String texto = leer("Mes MM/aaaa (ej. 10/2026)");
            try {
                String[] partes = texto.split("/");
                return YearMonth.of(Integer.parseInt(partes[1].trim()), Integer.parseInt(partes[0].trim()));
            } catch (RuntimeException e) {
                error("Formato inválido. Use MM/aaaa, por ejemplo 10/2026.");
            }
        }
    }

    /** Marca las opciones que solo puede usar el administrador (el controlador lo exige). */
    private String admin() {
        return sistema.getSesion().esAdministrador() ? "" : "  [solo administrador]";
    }

    private int leerEnteroConDefecto(String prompt, int actual) {
        while (true) {
            String texto = leerConDefecto(prompt, String.valueOf(actual));
            try {
                return Validador.leerEntero(texto, prompt);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private double leerMontoConDefecto(String prompt, double actual) {
        while (true) {
            String texto = leerConDefecto(prompt, MonedaUtil.aCsv(actual));
            try {
                return MonedaUtil.leer(texto);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private LocalTime leerHoraConDefecto(String prompt, LocalTime actual) {
        while (true) {
            String texto = leerConDefecto(prompt + " HH:mm", FechaUtil.formatearHora(actual));
            try {
                return FechaUtil.leerHora(texto);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    private LocalDate leerFechaConDefecto(String prompt, LocalDate actual) {
        while (true) {
            String texto = leerConDefecto(prompt + " dd/MM/aaaa", FechaUtil.formatearFecha(actual));
            try {
                return FechaUtil.leerFecha(texto);
            } catch (CanchaProException e) {
                error(e.getMessage());
            }
        }
    }

    /** Igual que elegirEnum, pero Enter conserva el valor actual. */
    private <T> T elegirEnumConDefecto(String prompt, T[] valores, T actual) {
        T elegido = elegirEnum(prompt + " (Enter = " + actual + ")", valores, true);
        return elegido == null ? actual : elegido;
    }

    /**
     * Lee los días de una franja. Acepta:
     * "lv" (lunes a viernes), "fds" (sábado y domingo), "todos",
     * letras "L,M,X,J,V,S,D" o números "1-5" / "1,3,5" (1 = lunes).
     *
     * @param actuales días actuales (Enter los conserva) o null si es obligatorio
     */
    private Set<DayOfWeek> leerDias(Set<DayOfWeek> actuales) {
        String ayuda = "Días (lv = lun-vie, fds = sáb-dom, todos, L,M,X,J,V,S,D o 1-5)";
        while (true) {
            String texto = actuales == null ? leer(ayuda) : leerConDefecto(ayuda, textoDias(actuales));
            Set<DayOfWeek> dias = interpretarDias(texto);
            if (dias != null && !dias.isEmpty()) {
                return dias;
            }
            error("No se reconocieron los días. Ejemplos: lv, fds, todos, L,M,X o 1-5.");
        }
    }

    private static Set<DayOfWeek> interpretarDias(String texto) {
        String t = texto.trim().toUpperCase().replace(" ", "");
        switch (t) {
            case "LV": return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            case "FDS": return EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            case "TODOS": return EnumSet.allOf(DayOfWeek.class);
            default: break;
        }
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        String letras = "LMXJVSD";
        for (String parte : t.split(",")) {
            if (parte.isEmpty()) {
                continue;
            }
            if (parte.matches("[1-7]-[1-7]")) {
                int a = parte.charAt(0) - '0';
                int b = parte.charAt(2) - '0';
                for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
                    dias.add(DayOfWeek.of(i));
                }
            } else if (parte.matches("[1-7]")) {
                dias.add(DayOfWeek.of(Integer.parseInt(parte)));
            } else if (parte.length() == 1 && letras.contains(parte)) {
                dias.add(DayOfWeek.of(letras.indexOf(parte) + 1));
            } else {
                return null;
            }
        }
        return dias;
    }

    private static String textoDias(Set<DayOfWeek> dias) {
        String letras = "LMXJVSD";
        StringBuilder sb = new StringBuilder();
        for (DayOfWeek d : dias) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(letras.charAt(d.getValue() - 1));
        }
        return sb.toString();
    }

    /** Muestra los valores de un enum numerados y devuelve el elegido (o null si se permite vacío). */
    private <T> T elegirEnum(String prompt, T[] valores, boolean permiteVacio) {
        for (int i = 0; i < valores.length; i++) {
            System.out.println("    " + (i + 1) + ". " + valores[i]);
        }
        while (true) {
            String texto = permiteVacio ? leerOpcional(prompt) : leer(prompt);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                int i = Integer.parseInt(texto);
                if (i >= 1 && i <= valores.length) {
                    return valores[i - 1];
                }
            } catch (NumberFormatException e) {
                // se repite la pregunta
            }
            error("Elija un número entre 1 y " + valores.length + ".");
        }
    }

    // =====================================================================
    //  SALIDA EN PANTALLA
    // =====================================================================

    /**
     * Ejecuta una acción y, si un controlador la rechaza, muestra su mensaje.
     * Es el equivalente al try/catch que tendrá cada botón de la interfaz Swing.
     */
    private void accion(Accion accion) {
        try {
            accion.ejecutar();
        } catch (CanchaProException e) {
            error(e.getMessage());
        }
    }

    /** Imprime una tabla con columnas alineadas (máximo 32 caracteres por celda). */
    private static void tabla(String[] columnas, List<Object[]> filas) {
        if (filas.isEmpty()) {
            System.out.println("  (sin registros)");
            return;
        }
        int[] anchos = new int[columnas.length];
        for (int i = 0; i < columnas.length; i++) {
            anchos[i] = columnas[i].length();
        }
        for (Object[] fila : filas) {
            for (int i = 0; i < columnas.length && i < fila.length; i++) {
                anchos[i] = Math.min(32, Math.max(anchos[i], celda(fila[i]).length()));
            }
        }
        StringBuilder separador = new StringBuilder("  ");
        StringBuilder encabezado = new StringBuilder("  ");
        for (int i = 0; i < columnas.length; i++) {
            encabezado.append(ajustar(columnas[i], anchos[i])).append(" | ");
            separador.append("-".repeat(anchos[i])).append("-+-");
        }
        System.out.println(encabezado);
        System.out.println(separador);
        for (Object[] fila : filas) {
            StringBuilder linea = new StringBuilder("  ");
            for (int i = 0; i < columnas.length; i++) {
                linea.append(ajustar(i < fila.length ? celda(fila[i]) : "", anchos[i])).append(" | ");
            }
            System.out.println(linea);
        }
        System.out.println("  (" + filas.size() + " registros)");
    }

    private static String celda(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private static String ajustar(String texto, int ancho) {
        if (texto.length() > ancho) {
            return texto.substring(0, ancho - 1) + "~";
        }
        return texto + " ".repeat(ancho - texto.length());
    }

    private static String sangria(String texto) {
        return "    " + texto.replace("\n", "\n    ");
    }

    private static void titulo(String texto) {
        String linea = "=".repeat(Math.max(60, texto.length() + 4));
        System.out.println("\n" + linea + "\n  " + texto + "\n" + linea);
    }

    private static void subtitulo(String texto) {
        System.out.println("\n--- " + texto + " ---");
    }

    private static void ok(String mensaje) {
        System.out.println("  [OK] " + mensaje);
    }

    private static void error(String mensaje) {
        System.out.println("  [ERROR] " + mensaje);
    }

    private static void borrarCarpeta(Path carpeta) {
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            List<Path> rutas = new ArrayList<>();
            recorrido.forEach(rutas::add);
            Collections.reverse(rutas);
            for (Path ruta : rutas) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            System.out.println("No se pudo borrar la carpeta temporal: " + carpeta);
        }
    }
}
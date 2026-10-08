/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.models.enums.MetodoPago;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.tienda.Venta;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.persistence.ResultadoCarga;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.MonedaUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
/**
 *
 * @author SANTANA
 */
/**
 * Pruebas funcionales de PERSISTENCIA (archivos CSV).
 *
 * Demuestra las pruebas del PDF que dependen de archivos:
 * 
 *   #10 Reiniciar programa → la información permanece.
 *   #11 Archivo con registro incorrecto → error controlado.
 * 
 *
 * No toca tus datos reales: copia datos_prueba/ a una carpeta temporal,
 * trabaja ahí y la borra al terminar.
 */
public class PruebasPersistencia {
     private static int aprobadas = 0;
    private static int fallidas = 0;

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================================");
        System.out.println("  CanchaPro - Pruebas de persistencia (archivos CSV)");
        System.out.println("==============================================================");

        Path origen = buscarDatosOrigen();
        if (origen == null) {
            System.out.println("No se encontró la carpeta datos_prueba/ ni data/.");
            System.out.println("Ejecute este archivo desde NetBeans con el proyecto CanchaPro abierto.");
            return;
        }
        System.out.println("Datos de origen: " + origen.toAbsolutePath());

        Path temporal = Files.createTempDirectory("canchapro_pruebas_");
        try {
            pruebaCargaInicial(origen, temporal.resolve("carga"));
            pruebaReinicio(origen, temporal.resolve("reinicio"));
            pruebaIdaYVuelta(origen, temporal.resolve("idavuelta"));
            pruebaRegistrosIncorrectos(origen, temporal.resolve("corruptos"));
            pruebaArchivosFaltantes(temporal.resolve("vacio"));
            pruebaRespaldosYRestauracion(origen, temporal.resolve("respaldos"));
        } finally {
            eliminarCarpeta(temporal);
        }

        System.out.println("\n==============================================================");
        System.out.printf("  Resultado: %d aprobadas, %d fallidas%n", aprobadas, fallidas);
        System.out.println(fallidas == 0 ? "  TODAS LAS PRUEBAS DE PERSISTENCIA PASARON"
                                         : "  HAY PRUEBAS FALLIDAS: revise los mensajes de arriba");
        System.out.println("==============================================================");
    }

    // =====================================================================
    //  1. CARGA INICIAL
    // =====================================================================

    private static void pruebaCargaInicial(Path origen, Path carpeta) throws IOException {
        System.out.println("\n--- Carga inicial de los datos de prueba ---");
        copiarCarpeta(origen, carpeta);
        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        ComplejoDeportivo complejo = gestor.cargarTodo();
        ResultadoCarga r = gestor.getUltimoResultado();
        System.out.println("        " + r.getResumen());

        verificar("--", "Se cargan los 43 registros sin errores ni advertencias",
                r.getTotalCargados() == 43 && !r.hayErrores() && !r.hayAdvertencias());

        Reserva r2 = complejo.buscarReserva("R0002").orElse(null);
        verificar("--", "R0002 reconstruida: total Q630.00 y saldo Q0.00",
                r2 != null && MonedaUtil.sonIguales(r2.calcularTotal(), 630) && r2.estaPagada());

        Reserva r1 = complejo.buscarReserva("R0001").orElse(null);
        verificar("--", "R0001 cancelada conserva motivo y responsable",
                r1 != null && r1.getEstado() == EstadoReserva.CANCELADA
                && r1.getCancelacion().getMotivo().equals("Lluvia fuerte anunciada"));

        verificar("--", "Las relaciones se reconstruyen (venta V0001 ligada a R0002)",
                complejo.buscarVenta("V0001").map(v -> v.getReserva() == r2).orElse(false));
    }

    // =====================================================================
    //  2. PRUEBA #10: REINICIAR PROGRAMA
    // =====================================================================

    private static void pruebaReinicio(Path origen, Path carpeta) throws Exception {
        System.out.println("\n--- Prueba #10: reiniciar el programa ---");
        copiarCarpeta(origen, carpeta);

        // "Primera ejecución": cargar, trabajar en memoria y guardar al cerrar.
        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        ComplejoDeportivo complejo = gestor.cargarTodo();
        Usuario operador = complejo.buscarUsuario("U002").orElseThrow();

        Cliente nuevo = new Cliente(complejo.generarCodigo(GeneradorCodigo.Tipo.CLIENTE),
                "Cliente de Prueba", "4455-6677", "prueba@mail.com", LocalDate.of(2026, 10, 1));
        complejo.agregarCliente(nuevo);

        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 18, 0);
        double precio = complejo.getCalculadoraTarifa().calcularPrecioCancha(inicio, inicio.plusMinutes(90));
        Reserva reserva = new Reserva(complejo.generarCodigo(GeneradorCodigo.Tipo.RESERVA), nuevo,
                complejo.buscarCancha("C02").orElseThrow(), inicio, Duration.ofMinutes(90), precio,
                operador, LocalDateTime.of(2026, 10, 8, 9, 0));
        complejo.agregarReserva(reserva);
        reserva.agregarServicio(complejo.buscarServicio("S03").orElseThrow(), 1);
        reserva.registrarPago(new Pago(complejo.generarCodigo(GeneradorCodigo.Tipo.PAGO),
                LocalDateTime.of(2026, 10, 8, 9, 5), 100, MetodoPago.TARJETA, "Anticipo"));

        complejo.buscarReserva("R0005").orElseThrow()
                .cancelar("Cliente enfermo", operador, LocalDateTime.of(2026, 10, 8, 9, 10));

        Venta venta = new Venta(complejo.generarCodigo(GeneradorCodigo.Tipo.VENTA),
                LocalDateTime.of(2026, 10, 8, 9, 15), null, operador);
        venta.agregarProducto(complejo.buscarProducto("P003").orElseThrow(), 3);
        venta.confirmar(MetodoPago.EFECTIVO);
        complejo.agregarVenta(venta);

        complejo.getConfiguracion().setMinutosLimpieza(15);
        double totalReserva = reserva.calcularTotal();
        gestor.guardarTodo(complejo);

        // "Segunda ejecución": un gestor y un complejo totalmente nuevos.
        GestorPersistencia gestor2 = new GestorPersistencia(carpeta);
        ComplejoDeportivo recargado = gestor2.cargarTodo();
        System.out.println("        Al reiniciar: " + gestor2.getUltimoResultado().getResumen());

        verificar("#10", "Recarga sin errores", !gestor2.getUltimoResultado().hayErrores());
        verificar("#10", "El cliente nuevo permanece",
                recargado.buscarCliente(nuevo.getCodigo()).isPresent());

        Reserva r = recargado.buscarReserva(reserva.getCodigo()).orElse(null);
        verificar("#10", "La reserva nueva permanece con servicio, pago y saldo correctos",
                r != null && MonedaUtil.sonIguales(r.calcularTotal(), totalReserva)
                && r.getServicios().size() == 1 && MonedaUtil.sonIguales(r.calcularTotalPagado(), 100));

        Reserva r5 = recargado.buscarReserva("R0005").orElseThrow();
        verificar("#10", "La cancelación de R0005 permanece (estado, motivo, responsable)",
                r5.getEstado() == EstadoReserva.CANCELADA
                && r5.getCancelacion().getMotivo().equals("Cliente enfermo")
                && r5.getCancelacion().getResponsable().getCodigo().equals("U002"));

        verificar("#10", "La venta permanece y el inventario quedó descontado (gaseosa 30 → 27)",
                recargado.buscarVenta(venta.getNumero()).isPresent()
                && recargado.buscarProducto("P003").orElseThrow().getExistencia() == 27);

        verificar("#10", "La configuración permanece (limpieza 15 min)",
                recargado.getConfiguracion().getMinutosLimpieza() == 15);

        verificar("#10", "La numeración continúa (siguiente reserva R0007)",
                recargado.generarCodigo(GeneradorCodigo.Tipo.RESERVA).equals("R0007"));
    }

    // =====================================================================
    //  3. IDA Y VUELTA: cargar y guardar sin cambios no altera los archivos
    // =====================================================================

    private static void pruebaIdaYVuelta(Path origen, Path carpeta) throws Exception {
        System.out.println("\n--- Ida y vuelta: cargar y guardar sin cambios ---");
        copiarCarpeta(origen, carpeta);
        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        gestor.guardarTodo(gestor.cargarTodo());

        List<String> distintos = new ArrayList<>();
        for (String nombre : GestorPersistencia.ARCHIVOS) {
            if (nombre.endsWith(".properties")) {
                continue; // el guardado agrega comentarios explicativos
            }
            List<String> antes = leerSinBlancos(origen.resolve(nombre));
            List<String> despues = leerSinBlancos(carpeta.resolve(nombre));
            if (!antes.equals(despues)) {
                distintos.add(nombre);
            }
        }
        verificar("--", "Los 11 CSV quedan idénticos a los originales"
                + (distintos.isEmpty() ? "" : " (distintos: " + distintos + ")"), distintos.isEmpty());
    }

    // =====================================================================
    //  4. PRUEBA #11: REGISTROS INCORRECTOS
    // =====================================================================

    private static void pruebaRegistrosIncorrectos(Path origen, Path carpeta) throws Exception {
        System.out.println("\n--- Prueba #11: archivos con registros incorrectos ---");
        copiarCarpeta(origen, carpeta);

        agregarLineas(carpeta.resolve("clientes.csv"),
                "CL0005;Teléfono Malo;123;;2026-09-01",            // teléfono inválido
                "CL0006;Fecha Mala;44443333;;2026-02-30");         // fecha inexistente
        agregarLineas(carpeta.resolve("reservas.csv"),
                "R0006;CL0099;C01;2026-10-12T10:00;2026-10-12T11:00;150.00;PENDIENTE;2026-10-06T08:30;U002;;;",  // cliente inexistente
                "R0007;CL0001;C01;2026-10-06T16:00;2026-10-06T18:00;375.00;PENDIENTE;2026-10-06T08:30;U002;;;",  // traslape con R0003
                "R0008;CL0001;C02;2026-13-40T10:00;2026-10-12T11:00;150.00;PENDIENTE;2026-10-06T08:30;U002;;;",  // fecha inválida
                "R0009;CL0001;C02;2026-10-12T10:00;2026-10-12T11:00;abc;PENDIENTE;2026-10-06T08:30;U002;;;",     // monto no numérico
                "R0010;CL0001;C02;2026-10-12T12:00");                                                           // línea cortada
        agregarLineas(carpeta.resolve("pagos.csv"),
                "PG0009;R0004;2026-10-05T10:10;9999.00;EFECTIVO;");                                              // pago mayor al saldo
        agregarLineas(carpeta.resolve("canchas.csv"),
                "C04;Cancha Nueva;FUTBOL_11;22;DISPONIBLE;08:00;23:00");                                         // enum inválido
        Files.writeString(carpeta.resolve("config.properties"),
                "modulo_tienda=quizas\nminutos_limpieza=10\n", StandardCharsets.UTF_8);                         // config inválida

        ComplejoDeportivo[] cargado = new ComplejoDeportivo[1];
        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        debeAceptar("#11", "El sistema arranca a pesar de los registros malos",
                () -> cargado[0] = gestor.cargarTodo());

        ResultadoCarga r = gestor.getUltimoResultado();
        System.out.println("        " + r.getResumen());
        for (ResultadoCarga.Incidencia e : r.getErrores()) {
            System.out.println("        [ERROR] " + e);
        }

        verificar("#11", "Cada línea mala queda reportada (10 errores)", r.getErrores().size() == 10);
        verificar("#11", "Los errores indican archivo y número de línea",
                r.getErrores().stream().anyMatch(e -> e.getArchivo().equals("reservas.csv") && e.getLinea() == 7));
        verificar("#11", "Los registros correctos sí se cargan (4 clientes, 5 reservas, 3 canchas)",
                cargado[0] != null && cargado[0].getClientes().size() == 4
                && cargado[0].getReservas().size() == 5 && cargado[0].getCanchas().size() == 3);
        verificar("#11", "El pago válido de R0004 se conserva y el inválido no",
                cargado[0] != null && cargado[0].buscarReserva("R0004")
                        .map(res -> res.getPagos().size() == 1).orElse(false));
        verificar("#11", "Config: valor malo usa el defecto, valor bueno se respeta",
                cargado[0] != null && cargado[0].getConfiguracion().isModuloTiendaActivo()
                && cargado[0].getConfiguracion().getMinutosLimpieza() == 10);
    }

    // =====================================================================
    //  5. ARCHIVOS INEXISTENTES O VACÍOS
    // =====================================================================

    private static void pruebaArchivosFaltantes(Path carpeta) throws Exception {
        System.out.println("\n--- Archivos inexistentes o vacíos ---");
        Files.createDirectories(carpeta);
        Files.writeString(carpeta.resolve("clientes.csv"), "", StandardCharsets.UTF_8); // vacío

        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        ComplejoDeportivo[] cargado = new ComplejoDeportivo[1];
        debeAceptar("--", "Arranca aunque no existan los archivos", () -> cargado[0] = gestor.cargarTodo());
        ResultadoCarga r = gestor.getUltimoResultado();
        verificar("--", "Solo advertencias, ningún error (" + r.getAdvertencias().size() + " avisos)",
                !r.hayErrores() && r.getAdvertencias().size() == 12);
        debeAceptar("--", "Al guardar se crean los archivos con su encabezado", () -> {
            gestor.guardarTodo(cargado[0]);
            exigir(Files.readAllLines(carpeta.resolve("reservas.csv")).get(0).startsWith("codigo;"),
                    "no se creó el encabezado");
        });
    }

    // =====================================================================
    //  6. RESPALDOS Y RESTAURACIÓN
    // =====================================================================

    private static void pruebaRespaldosYRestauracion(Path origen, Path carpeta) throws Exception {
        System.out.println("\n--- Respaldos automáticos y restauración ---");
        copiarCarpeta(origen, carpeta);
        GestorPersistencia gestor = new GestorPersistencia(carpeta);
        ComplejoDeportivo complejo = gestor.cargarTodo();

        complejo.agregarCliente(new Cliente(complejo.generarCodigo(GeneradorCodigo.Tipo.CLIENTE),
                "Para Respaldo", "11223344", "", LocalDate.of(2026, 10, 1)));
        gestor.guardarTodo(complejo);

        List<Path> respaldos = gestor.listarRespaldos();
        verificar("--", "Guardar crea un respaldo con la versión anterior (4 clientes)",
                respaldos.size() == 1
                && leerSinBlancos(respaldos.get(0).resolve("clientes.csv")).size() == 5);

        for (int i = 0; i < 6; i++) {
            gestor.guardarTodo(complejo);
        }
        verificar("--", "Solo se conservan los últimos " + GestorPersistencia.MAX_RESPALDOS + " respaldos",
                gestor.listarRespaldos().size() == GestorPersistencia.MAX_RESPALDOS);

        debeAceptar("--", "Restaurar datos de prueba devuelve el estado inicial (4 clientes)", () -> {
            gestor.restaurarDesde(origen);
            exigir(gestor.cargarTodo().getClientes().size() == 4, "no se restauraron los clientes");
        });
    }

    // =====================================================================
    //  APOYO: archivos
    // =====================================================================

    /** Usa datos_prueba/ (copia limpia); si no existe, data/. */
    private static Path buscarDatosOrigen() {
        if (Files.isDirectory(GestorPersistencia.CARPETA_DATOS_PRUEBA)) {
            return GestorPersistencia.CARPETA_DATOS_PRUEBA;
        }
        if (Files.isDirectory(GestorPersistencia.CARPETA_DATOS)) {
            return GestorPersistencia.CARPETA_DATOS;
        }
        return null;
    }

    private static void copiarCarpeta(Path origen, Path destino) throws IOException {
        Files.createDirectories(destino);
        for (String nombre : GestorPersistencia.ARCHIVOS) {
            Path archivo = origen.resolve(nombre);
            if (Files.isRegularFile(archivo)) {
                Files.copy(archivo, destino.resolve(nombre));
            }
        }
    }

        /**
     * Agrega líneas al final de un CSV. Si el archivo NO termina en salto de línea
     * (pasa al crearlo a mano en NetBeans), primero agrega uno; si no, la primera
     * línea nueva quedaría pegada a la última línea original.
     */
    private static void agregarLineas(Path archivo, String... lineas) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (!terminaEnSaltoDeLinea(archivo)) {
            sb.append(System.lineSeparator());
        }
        for (String linea : lineas) {
            sb.append(linea).append(System.lineSeparator());
        }
        Files.writeString(archivo, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
     private static boolean terminaEnSaltoDeLinea(Path archivo) throws IOException {
        byte[] contenido = Files.readAllBytes(archivo);
        return contenido.length == 0 || contenido[contenido.length - 1] == '\n';
    }

    private static List<String> leerSinBlancos(Path archivo) throws IOException {
        List<String> lineas = new ArrayList<>(Files.readAllLines(archivo, StandardCharsets.UTF_8));
        lineas.removeIf(String::isBlank);
        if (!lineas.isEmpty() && lineas.get(0).startsWith("\uFEFF")) { // BOM de UTF-8
            lineas.set(0, lineas.get(0).substring(1));
        }
        return lineas;
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

    // =====================================================================
    //  MINI-FRAMEWORK DE PRUEBAS (igual que en PruebasModelo)
    // =====================================================================

    private static void debeAceptar(String numero, String descripcion, Accion accion) {
        try {
            accion.ejecutar();
            registrar(true, numero, descripcion, "correcto");
        } catch (Exception e) {
            registrar(false, numero, descripcion, "falló: " + e.getMessage());
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

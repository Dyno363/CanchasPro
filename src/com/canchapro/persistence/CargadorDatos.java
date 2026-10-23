/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.ConfiguracionSistema;
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
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.Cancelacion;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.models.reservas.ServicioReserva;
import com.canchapro.models.tienda.DetalleVenta;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
/**
 *
 * @author SANTANA
 */
/**
 * Convierte los archivos CSV en objetos del modelo al INICIAR el sistema.
 *
 * El orden de carga importa: primero lo que no depende de nada
 * (usuarios, clientes, canchas...) y después lo que hace referencia a ello.
 * Cuando una reserva dice "CL0003", ese cliente ya debe estar en memoria.
 *
 * 
 *  1. config.properties
 *  2. usuarios  3. clientes  4. canchas  5. tarifas  6. servicios  7. productos
 *  8. reservas          (usa clientes, canchas, usuarios)
 *  9. servicios_reserva (usa reservas, servicios)
 * 10. pagos             (usa reservas; después de los servicios para validar el saldo)
 * 11. ventas            (usa reservas, usuarios)
 * 12. detalle_ventas    (usa ventas, productos)
 * 
 *
 * Prueba #11 del PDF: cada línea se procesa en su propio try/catch.
 * Si una línea tiene una fecha inválida, un monto no numérico o un código
 * inexistente, se anota el error con su número de línea y se continúa.
 *
 * Toda la validación la hacen los constructores del modelo: el cargador
 * solo convierte texto a tipos (fecha, número, enum) y arma los objetos.
 */
public class CargadorDatos {
    // Nombres de archivo (también los usa GuardadorDatos)
    public static final String ARCHIVO_USUARIOS = "usuarios.csv";
    public static final String ARCHIVO_CLIENTES = "clientes.csv";
    public static final String ARCHIVO_CANCHAS = "canchas.csv";
    public static final String ARCHIVO_TARIFAS = "tarifas.csv";
    public static final String ARCHIVO_SERVICIOS = "servicios.csv";
    public static final String ARCHIVO_PRODUCTOS = "productos.csv";
    public static final String ARCHIVO_RESERVAS = "reservas.csv";
    public static final String ARCHIVO_SERVICIOS_RESERVA = "servicios_reserva.csv";
    public static final String ARCHIVO_PAGOS = "pagos.csv";
    public static final String ARCHIVO_VENTAS = "ventas.csv";
    public static final String ARCHIVO_DETALLE_VENTAS = "detalle_ventas.csv";

    /** Separador de la lista de días en tarifas.csv: MONDAY|TUESDAY|... */
    public static final String SEPARADOR_DIAS = "|";

    private final Path carpeta;
    private ComplejoDeportivo complejo;
    private ResultadoCarga resultado;

    /** Total que dice ventas.csv, para verificarlo contra los detalles al final. */
    private final Map<String, Double> totalesDeclarados = new HashMap<>();

    /**
     * @param carpeta carpeta donde están los archivos (normalmente "data")
     */
    public CargadorDatos(Path carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * Lee todos los archivos y reconstruye el sistema en memoria.
     *
     * @param resultado bitácora donde se anota lo cargado, los errores y las advertencias
     * @return el complejo con todas sus colecciones llenas (nunca null)
     */
    public ComplejoDeportivo cargar(ResultadoCarga resultado) {
        this.resultado = resultado;
        this.totalesDeclarados.clear();

        ConfiguracionSistema config = ArchivoConfiguracion.leer(
                carpeta.resolve(ArchivoConfiguracion.NOMBRE_ARCHIVO), resultado);
        this.complejo = new ComplejoDeportivo(config);

        procesar(ARCHIVO_USUARIOS, 7, this::cargarUsuario);
        procesar(ARCHIVO_CLIENTES, 5, this::cargarCliente);
        procesar(ARCHIVO_CANCHAS, 7, this::cargarCancha);
        procesar(ARCHIVO_TARIFAS, 10, this::cargarTarifa);
        procesar(ARCHIVO_SERVICIOS, 5, this::cargarServicio);
        procesar(ARCHIVO_PRODUCTOS, 7, this::cargarProducto);
        procesar(ARCHIVO_RESERVAS, 12, this::cargarReserva);
        procesar(ARCHIVO_SERVICIOS_RESERVA, 5, this::cargarServicioReserva);
        procesar(ARCHIVO_PAGOS, 6, this::cargarPago);
        procesar(ARCHIVO_VENTAS, 6, 7, this::cargarVenta); // 6 = formato anterior (sin fechaPago)
        procesar(ARCHIVO_DETALLE_VENTAS, 5, this::cargarDetalleVenta);
        verificarTotalesDeVentas();

        return complejo;
    }

    // =====================================================================
    //  MOTOR GENÉRICO: lee un archivo y procesa cada línea con su propio try/catch
    // =====================================================================

    /** Acción que convierte UNA línea en objeto(s) y lo agrega al complejo. */
    @FunctionalInterface
    private interface ProcesadorLinea {
        void procesar(ArchivoCsv.Linea linea) throws CanchaProException;
    }

    /** Archivo con una cantidad FIJA de columnas (la mayoría). */
    private void procesar(String nombreArchivo, int columnas, ProcesadorLinea procesador) {
        procesar(nombreArchivo, columnas, columnas, procesador);
    }

    /**
     * Archivo que acepta entre {@code columnasMinimas} y {@code columnas}: así un
     * CSV guardado con el formato anterior (menos columnas) se sigue leyendo.
     */
    private void procesar(String nombreArchivo, int columnasMinimas, int columnas, ProcesadorLinea procesador) {
        List<ArchivoCsv.Linea> lineas = ArchivoCsv.leer(carpeta.resolve(nombreArchivo), columnasMinimas,
                columnas, resultado);
        for (ArchivoCsv.Linea linea : lineas) {
            try {
                procesador.procesar(linea);
                resultado.registrarCarga(nombreArchivo);
            } catch (CanchaProException e) {
                resultado.registrarError(nombreArchivo, linea.getNumero(), e.getMessage());
            } catch (RuntimeException e) {
                // Red de seguridad: un error inesperado en una línea no debe detener la carga.
                resultado.registrarError(nombreArchivo, linea.getNumero(),
                        "Error inesperado: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
    }

    // =====================================================================
    //  1-6. CATÁLOGOS (no dependen de otros archivos)
    // =====================================================================

    /** codigo;nombre;telefono;username;passwordHash;rol;activo */
    private void cargarUsuario(ArchivoCsv.Linea l) throws CanchaProException {
        Usuario usuario = new Usuario(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4),
                leerEnum(RolUsuario.class, l.get(5), "rol"));
        usuario.setActivo(Validador.leerBooleano(l.get(6), "activo"));
        complejo.agregarUsuario(usuario);
    }

    /** codigo;nombre;telefono;correo;fechaRegistro */
    private void cargarCliente(ArchivoCsv.Linea l) throws CanchaProException {
        complejo.agregarCliente(new Cliente(l.get(0), l.get(1), l.get(2), l.get(3),
                FechaUtil.leerFecha(l.get(4))));
    }

    /** codigo;nombre;tipo;capacidad;estado;horaApertura;horaCierre */
    private void cargarCancha(ArchivoCsv.Linea l) throws CanchaProException {
        complejo.agregarCancha(new Cancha(l.get(0), l.get(1),
                leerEnum(TipoCancha.class, l.get(2), "tipo"),
                Validador.leerEntero(l.get(3), "capacidad"),
                leerEnum(EstadoCancha.class, l.get(4), "estado"),
                FechaUtil.leerHora(l.get(5)),
                FechaUtil.leerHora(l.get(6))));
    }

    /** codigo;tipoTarifa;nombre;precioHora;prioridad;dias;horaInicio;horaFin;fecha;motivo */
    private void cargarTarifa(ArchivoCsv.Linea l) throws CanchaProException {
        String tipo = l.getRequerido(1, "tipoTarifa").toUpperCase(Locale.ROOT);
        double precio = MonedaUtil.leer(l.get(3));
        int prioridad = Validador.leerEntero(l.get(4), "prioridad");

        Tarifa tarifa;
        if (TarifaFranja.TIPO.equals(tipo)) {
            tarifa = new TarifaFranja(l.get(0), l.get(2), precio, prioridad,
                    leerDias(l.getRequerido(5, "dias")),
                    FechaUtil.leerHora(l.get(6)),
                    FechaUtil.leerHora(l.get(7)));
        } else if (TarifaEspecial.TIPO.equals(tipo)) {
            tarifa = new TarifaEspecial(l.get(0), l.get(2), precio, prioridad,
                    FechaUtil.leerFecha(l.get(8)), l.get(9));
        } else {
            throw new DatoInvalidoException("Tipo de tarifa desconocido: '" + l.get(1)
                    + "'. Use FRANJA o ESPECIAL.");
        }
        complejo.agregarTarifa(tarifa);
    }

    /** codigo;nombre;precio;cobroPorHora;activo */
    private void cargarServicio(ArchivoCsv.Linea l) throws CanchaProException {
        ServicioAdicional servicio = new ServicioAdicional(l.get(0), l.get(1),
                MonedaUtil.leer(l.get(2)),
                Validador.leerBooleano(l.get(3), "cobroPorHora"));
        servicio.setActivo(Validador.leerBooleano(l.get(4), "activo"));
        complejo.agregarServicio(servicio);
    }

    /** codigo;nombre;categoria;precio;existencia;stockMinimo;estado */
    private void cargarProducto(ArchivoCsv.Linea l) throws CanchaProException {
        complejo.agregarProducto(new Producto(l.get(0), l.get(1),
                leerEnum(CategoriaProducto.class, l.get(2), "categoria"),
                MonedaUtil.leer(l.get(3)),
                Validador.leerEntero(l.get(4), "existencia"),
                Validador.leerEntero(l.get(5), "stockMinimo"),
                leerEnum(EstadoProducto.class, l.get(6), "estado")));
    }

    // =====================================================================
    //  7-9. RESERVAS Y SUS PARTES
    // =====================================================================

    /**
     * codigo;codigoCliente;codigoCancha;inicio;fin;precioCancha;estado;
     * fechaCreacion;creadaPor;fechaCancelacion;motivoCancelacion;canceladaPor
     */
    private void cargarReserva(ArchivoCsv.Linea l) throws CanchaProException {
        Cliente cliente = buscarCliente(l.getRequerido(1, "codigoCliente"));
        Cancha cancha = buscarCancha(l.getRequerido(2, "codigoCancha"));
        Usuario creadaPor = buscarUsuario(l.getRequerido(8, "creadaPor"));
        EstadoReserva estado = leerEnum(EstadoReserva.class, l.get(6), "estado");

        // Si hay CUALQUIER dato de cancelación, se arma la Cancelacion. El constructor de
        // Reserva verifica que solo exista si el estado es CANCELADA (consistencia).
        Cancelacion cancelacion = null;
        boolean hayDatosCancelacion = !l.estaVacio(9) || !l.estaVacio(10) || !l.estaVacio(11);
        if (estado == EstadoReserva.CANCELADA || hayDatosCancelacion) {
            cancelacion = new Cancelacion(
                    FechaUtil.leerFechaHora(l.getRequerido(9, "fechaCancelacion")),
                    l.getRequerido(10, "motivoCancelacion"),
                    buscarUsuario(l.getRequerido(11, "canceladaPor")));
        }

        Reserva reserva = new Reserva(l.get(0), cliente, cancha,
                FechaUtil.leerFechaHora(l.get(3)),
                FechaUtil.leerFechaHora(l.get(4)),
                MonedaUtil.leer(l.get(5)),
                estado,
                FechaUtil.leerFechaHora(l.get(7)),
                creadaPor,
                cancelacion);
        complejo.agregarReserva(reserva); // detecta duplicados y traslapes
    }

    /** codigoReserva;codigoServicio;cantidad;precioUnitario;horas */
    private void cargarServicioReserva(ArchivoCsv.Linea l) throws CanchaProException {
        Reserva reserva = buscarReserva(l.getRequerido(0, "codigoReserva"));
        ServicioAdicional servicio = buscarServicio(l.getRequerido(1, "codigoServicio"));
        reserva.restaurarServicio(new ServicioReserva(servicio,
                Validador.leerEntero(l.get(2), "cantidad"),
                MonedaUtil.leer(l.get(3)),
                leerDecimal(l.get(4), "horas")));
    }

    /** codigo;codigoReserva;fechaHora;monto;metodo;observacion */
    private void cargarPago(ArchivoCsv.Linea l) throws CanchaProException {
        Reserva reserva = buscarReserva(l.getRequerido(1, "codigoReserva"));
        reserva.restaurarPago(new Pago(l.get(0),
                FechaUtil.leerFechaHora(l.get(2)),
                MonedaUtil.leer(l.get(3)),
                leerEnum(MetodoPago.class, l.get(4), "metodo"),
                l.get(5)));
    }

    // =====================================================================
    //  10-11. TIENDA
    // =====================================================================

    /**
     * numero;fechaHora;codigoReserva;metodoPago;vendedor;total;fechaPago
     * <br>metodoPago vacío = venta "a cuenta" de la reserva, todavía sin cobrar.
     * <br>fechaPago puede no existir (archivos del formato anterior, de 6 columnas).
     */
    private void cargarVenta(ArchivoCsv.Linea l) throws CanchaProException {
        Reserva reserva = l.estaVacio(2) ? null : buscarReserva(l.get(2));
        MetodoPago metodo = l.estaVacio(3) ? null : leerEnum(MetodoPago.class, l.get(3), "metodoPago");
        Venta venta = new Venta(l.get(0),
                FechaUtil.leerFechaHora(l.get(1)),
                reserva,
                buscarUsuario(l.getRequerido(4, "vendedor")),
                metodo,
                l.estaVacio(6) ? null : FechaUtil.leerFechaHora(l.get(6)));
        double totalDeclarado = MonedaUtil.leer(l.get(5));
        complejo.agregarVenta(venta);
        totalesDeclarados.put(venta.getNumero(), totalDeclarado);
    }

    /** numeroVenta;codigoProducto;cantidad;precioUnitario;subtotal */
    private void cargarDetalleVenta(ArchivoCsv.Linea l) throws CanchaProException {
        String numero = l.getRequerido(0, "numeroVenta");
        Venta venta = complejo.buscarVenta(numero).orElseThrow(
                () -> new EntidadNoEncontradaException("La venta " + numero + " no existe."));
        Producto producto = buscarProducto(l.getRequerido(1, "codigoProducto"));
        DetalleVenta detalle = new DetalleVenta(producto,
                Validador.leerEntero(l.get(2), "cantidad"),
                MonedaUtil.leer(l.get(3)));

        double subtotalDeclarado = MonedaUtil.leer(l.get(4));
        if (!MonedaUtil.sonIguales(subtotalDeclarado, detalle.getSubtotal())) {
            resultado.registrarAdvertencia(ARCHIVO_DETALLE_VENTAS, l.getNumero(), "El subtotal guardado ("
                    + MonedaUtil.formatear(subtotalDeclarado) + ") no coincide con cantidad x precio ("
                    + MonedaUtil.formatear(detalle.getSubtotal()) + "). Se usa el calculado.");
        }
        venta.restaurarDetalle(detalle);
    }

    /** Compara el total de ventas.csv con la suma real de sus detalles. */
    private void verificarTotalesDeVentas() {
        for (Venta venta : complejo.getVentas()) {
            if (venta.getDetalles().isEmpty()) {
                resultado.registrarAdvertencia(ARCHIVO_VENTAS, 0,
                        "La venta " + venta.getNumero() + " no tiene productos en detalle_ventas.csv.");
                continue;
            }
            Double declarado = totalesDeclarados.get(venta.getNumero());
            if (declarado != null && !MonedaUtil.sonIguales(declarado, venta.calcularTotal())) {
                resultado.registrarAdvertencia(ARCHIVO_VENTAS, 0, "El total de la venta " + venta.getNumero()
                        + " (" + MonedaUtil.formatear(declarado) + ") no coincide con sus detalles ("
                        + MonedaUtil.formatear(venta.calcularTotal()) + "). Se usa el calculado.");
            }
        }
    }

    // =====================================================================
    //  BÚSQUEDAS: convierten un código en el objeto ya cargado
    // =====================================================================

    private Cliente buscarCliente(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarCliente(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("El cliente " + codigo + " no existe."));
    }

    private Cancha buscarCancha(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarCancha(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("La cancha " + codigo + " no existe."));
    }

    private Usuario buscarUsuario(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarUsuario(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("El usuario " + codigo + " no existe."));
    }

    private Reserva buscarReserva(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarReserva(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("La reserva " + codigo + " no existe."));
    }

    private ServicioAdicional buscarServicio(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarServicio(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("El servicio " + codigo + " no existe."));
    }

    private Producto buscarProducto(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarProducto(codigo).orElseThrow(
                () -> new EntidadNoEncontradaException("El producto " + codigo + " no existe."));
    }

    // =====================================================================
    //  CONVERSIONES DE TEXTO
    // =====================================================================

    /**
     * Convierte texto en un valor de enum (sin importar mayúsculas).
     * Ej.: leerEnum(EstadoCancha.class, "mantenimiento", "estado") → MANTENIMIENTO
     *
     * Es un método genérico: {@code <E extends Enum<E>>} significa "cualquier enum".
     * Así una sola función sirve para los 7 enums del sistema.
     */
    static <E extends Enum<E>> E leerEnum(Class<E> tipo, String texto, String columna)
            throws DatoInvalidoException {
        String valor = Validador.validarRequerido(texto, columna).toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            StringJoiner permitidos = new StringJoiner(", ");
            for (E constante : tipo.getEnumConstants()) {
                permitidos.add(constante.name()); // name() = como se escribe en el CSV
            }
            throw new DatoInvalidoException("Valor '" + texto + "' no válido en '" + columna
                    + "'. Permitidos: " + permitidos + ".", e);
        }
    }

    /** "MONDAY|TUESDAY|FRIDAY" → {MONDAY, TUESDAY, FRIDAY} */
    static Set<DayOfWeek> leerDias(String texto) throws DatoInvalidoException {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (String parte : texto.split("\\" + SEPARADOR_DIAS)) {
            if (!parte.isBlank()) {
                dias.add(leerEnum(DayOfWeek.class, parte.trim(), "dias"));
            }
        }
        return dias;
    }

    /** Número decimal que no es dinero (ej. horas: "1.5"). */
    static double leerDecimal(String texto, String columna) throws DatoInvalidoException {
        String valor = Validador.validarRequerido(texto, columna);
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo '" + columna + "' debe ser numérico: '" + valor + "'.", e);
        }
    }
}
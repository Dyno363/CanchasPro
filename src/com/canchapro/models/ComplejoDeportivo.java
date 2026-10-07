/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models;
import com.canchapro.models.canchas.CalculadoraTarifa;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.canchas.Tarifa;
import com.canchapro.models.exceptions.ConflictoHorarioException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.personas.Usuario;
import com.canchapro.models.reservas.MotorDisponibilidad;
import com.canchapro.models.reservas.Pago;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.models.tienda.Producto;
import com.canchapro.models.tienda.Venta;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.MonedaUtil;
import com.canchapro.utils.Validador;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
/**
 *
 * @author SANTANA
 */
/**
 * Raíz del modelo en memoria de CanchaPro.
 *
 * Regla de oro del proyecto: al iniciar, la capa de persistencia lee los
 * CSV y llena estas colecciones. Durante la ejecución, TODAS las operaciones y
 * validaciones trabajan solo con estos objetos. Al cerrar (o con "Guardar Sistema")
 * se escriben de vuelta a los archivos.
 *
 * Estructuras de datos elegidas:
 * 
 *   {@code Map<String, X>} para lo que se busca por código (clientes, canchas...):
 *   la búsqueda es inmediata. Se usa LinkedHashMap para conservar el orden del CSV.
 *   {@code List<X>} para lo que se recorre en orden (reservas, ventas, tarifas).
 * 
 *
 * Esta clase solo AGRUPA y CONSULTA. Las reglas de negocio están en cada
 * entidad (Reserva, Producto...) y en los servicios (MotorDisponibilidad,
 * CalculadoraTarifa); así no se convierte en una clase que controla todo.
 */
public class ComplejoDeportivo {
    private final Map<String, Cliente> clientes;
    private final Map<String, Usuario> usuarios;
    private final Map<String, Cancha> canchas;
    private final Map<String, ServicioAdicional> servicios;
    private final Map<String, Producto> productos;
    private final List<Tarifa> tarifas;
    private final List<Reserva> reservas;
    private final List<Venta> ventas;
    private final ConfiguracionSistema configuracion;

    /** Servicios de dominio que trabajan sobre las colecciones de arriba. */
    private final CalculadoraTarifa calculadoraTarifa;
    private final MotorDisponibilidad motorDisponibilidad;

    /**
     * @param configuracion parámetros del sistema (si es null se usan los valores por defecto)
     */
    public ComplejoDeportivo(ConfiguracionSistema configuracion) {
        this.configuracion = configuracion != null ? configuracion : new ConfiguracionSistema();
        this.clientes = new LinkedHashMap<>();
        this.usuarios = new LinkedHashMap<>();
        this.canchas = new LinkedHashMap<>();
        this.servicios = new LinkedHashMap<>();
        this.productos = new LinkedHashMap<>();
        this.tarifas = new ArrayList<>();
        this.reservas = new ArrayList<>();
        this.ventas = new ArrayList<>();
        // La calculadora recibe la lista VIVA de tarifas (no una copia).
        this.calculadoraTarifa = new CalculadoraTarifa(tarifas);
        this.motorDisponibilidad = crearMotor();
    }

    // =====================================================================
    //  CONFIGURACIÓN Y SERVICIOS DE DOMINIO
    // =====================================================================

    public ConfiguracionSistema getConfiguracion() {
        return configuracion;
    }

    public String getNombre() {
        return configuracion.getNombreComplejo();
    }

    public CalculadoraTarifa getCalculadoraTarifa() {
        return calculadoraTarifa;
    }

    /**
     * Devuelve el motor sincronizado con los minutos de limpieza actuales de la
     * configuración. Si el administrador cambia la limpieza en pantalla, la
     * siguiente consulta ya la aplica.
     */
    public MotorDisponibilidad getMotorDisponibilidad() {
        try {
            motorDisponibilidad.setMinutosLimpieza(configuracion.getMinutosLimpieza());
        } catch (DatoInvalidoException e) {
            // ConfiguracionSistema ya garantiza un valor entre 0 y 120.
            throw new IllegalStateException(e);
        }
        return motorDisponibilidad;
    }

    private MotorDisponibilidad crearMotor() {
        try {
            return new MotorDisponibilidad(configuracion.getMinutosLimpieza());
        } catch (DatoInvalidoException e) {
            throw new IllegalStateException(e);
        }
    }

    // =====================================================================
    //  CLIENTES
    // =====================================================================

    public void agregarCliente(Cliente cliente) throws DatoInvalidoException {
        exigirNoNulo(cliente, "cliente");
        exigirCodigoLibre(clientes, cliente.getCodigo(), "cliente");
        clientes.put(cliente.getCodigo(), cliente);
    }

    public Optional<Cliente> buscarCliente(String codigo) {
        return Optional.ofNullable(clientes.get(codigo));
    }

    /**
     * Búsqueda por código, nombre o teléfono (sin importar tildes ni mayúsculas).
     * Se usa antes de registrar un cliente para evitar duplicados.
     *
     * @return coincidencias ordenadas por nombre
     */
    public List<Cliente> buscarClientes(String criterio) {
        List<Cliente> resultado = new ArrayList<>();
        for (Cliente cliente : clientes.values()) {
            if (cliente.coincideCon(criterio)) {
                resultado.add(cliente);
            }
        }
        resultado.sort(Comparator.comparing(c -> Validador.normalizarTexto(c.getNombre())));
        return resultado;
    }

    /** @return vista de solo lectura de todos los clientes */
    public Collection<Cliente> getClientes() {
        return Collections.unmodifiableCollection(clientes.values());
    }

    // =====================================================================
    //  USUARIOS
    // =====================================================================

    public void agregarUsuario(Usuario usuario) throws DatoInvalidoException {
        exigirNoNulo(usuario, "usuario");
        exigirCodigoLibre(usuarios, usuario.getCodigo(), "usuario");
        if (buscarUsuarioPorUsername(usuario.getUsername()).isPresent()) {
            throw new DatoInvalidoException("Ya existe un usuario con el nombre '" + usuario.getUsername() + "'.");
        }
        usuarios.put(usuario.getCodigo(), usuario);
    }

    public Optional<Usuario> buscarUsuario(String codigo) {
        return Optional.ofNullable(usuarios.get(codigo));
    }

    /** Búsqueda para el inicio de sesión (sin importar mayúsculas). */
    public Optional<Usuario> buscarUsuarioPorUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String buscado = username.trim().toLowerCase(Locale.ROOT);
        for (Usuario usuario : usuarios.values()) {
            if (usuario.getUsername().equals(buscado)) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    public Collection<Usuario> getUsuarios() {
        return Collections.unmodifiableCollection(usuarios.values());
    }

    // =====================================================================
    //  CANCHAS, TARIFAS Y SERVICIOS
    // =====================================================================

    public void agregarCancha(Cancha cancha) throws DatoInvalidoException {
        exigirNoNulo(cancha, "cancha");
        exigirCodigoLibre(canchas, cancha.getCodigo(), "cancha");
        canchas.put(cancha.getCodigo(), cancha);
    }

    public Optional<Cancha> buscarCancha(String codigo) {
        return Optional.ofNullable(canchas.get(codigo));
    }

    public Collection<Cancha> getCanchas() {
        return Collections.unmodifiableCollection(canchas.values());
    }

    public void agregarTarifa(Tarifa tarifa) throws DatoInvalidoException {
        exigirNoNulo(tarifa, "tarifa");
        if (buscarTarifa(tarifa.getCodigo()).isPresent()) {
            throw new DatoInvalidoException("Ya existe una tarifa con el código " + tarifa.getCodigo() + ".");
        }
        tarifas.add(tarifa);
    }

    public Optional<Tarifa> buscarTarifa(String codigo) {
        for (Tarifa tarifa : tarifas) {
            if (tarifa.getCodigo().equals(codigo)) {
                return Optional.of(tarifa);
            }
        }
        return Optional.empty();
    }

    /**
     * Las tarifas sí se pueden eliminar: las reservas ya hechas guardaron su
     * precio (precioCancha), así que no se pierde historial.
     */
    public void eliminarTarifa(String codigo) throws DatoInvalidoException {
        Tarifa tarifa = buscarTarifa(codigo).orElseThrow(
                () -> new DatoInvalidoException("No existe la tarifa " + codigo + "."));
        tarifas.remove(tarifa);
    }

    /** @return vista de solo lectura (refleja los cambios de la lista real) */
    public List<Tarifa> getTarifas() {
        return Collections.unmodifiableList(tarifas);
    }

    public void agregarServicio(ServicioAdicional servicio) throws DatoInvalidoException {
        exigirNoNulo(servicio, "servicio");
        exigirCodigoLibre(servicios, servicio.getCodigo(), "servicio");
        servicios.put(servicio.getCodigo(), servicio);
    }

    public Optional<ServicioAdicional> buscarServicio(String codigo) {
        return Optional.ofNullable(servicios.get(codigo));
    }

    public Collection<ServicioAdicional> getServicios() {
        return Collections.unmodifiableCollection(servicios.values());
    }

    // =====================================================================
    //  RESERVAS
    // =====================================================================

    /**
     * Agrega una reserva a memoria. Última línea de defensa: aunque alguien se
     * salte el MotorDisponibilidad, aquí se impide tener dos reservas activas
     * traslapadas en la misma cancha.
     *
     * @throws DatoInvalidoException     si el código ya existe (reserva duplicada)
     * @throws ConflictoHorarioException si choca con otra reserva activa
     */
    public void agregarReserva(Reserva reserva) throws DatoInvalidoException, ConflictoHorarioException {
        exigirNoNulo(reserva, "reserva");
        if (buscarReserva(reserva.getCodigo()).isPresent()) {
            throw new DatoInvalidoException("Reserva duplicada: ya existe la reserva " + reserva.getCodigo() + ".");
        }
        if (reserva.ocupaHorario()) {
            for (Reserva existente : reservas) {
                if (existente.getCancha().equals(reserva.getCancha())
                        && existente.ocupaHorario()
                        && existente.seTraslapaCon(reserva.getInicio(), reserva.getFin())) {
                    throw new ConflictoHorarioException("La reserva " + reserva.getCodigo()
                            + " choca con " + existente.getCodigo() + " en " + reserva.getCancha().getNombre()
                            + " (" + FechaUtil.formatearRango(existente.getInicio(), existente.getFin()) + ").");
                }
            }
        }
        reservas.add(reserva);
    }

    public Optional<Reserva> buscarReserva(String codigo) {
        for (Reserva reserva : reservas) {
            if (reserva.getCodigo().equals(codigo)) {
                return Optional.of(reserva);
            }
        }
        return Optional.empty();
    }

    /** @return vista de solo lectura de todas las reservas (incluye canceladas) */
    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    public List<Reserva> getReservasDeCancha(Cancha cancha) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getCancha().equals(cancha)) {
                resultado.add(reserva);
            }
        }
        return ordenarPorInicio(resultado);
    }

    public List<Reserva> getReservasDeCliente(Cliente cliente) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getCliente().equals(cliente)) {
                resultado.add(reserva);
            }
        }
        return ordenarPorInicio(resultado);
    }

    /** Reservas cuyo inicio cae en la fecha indicada (para el dashboard y la agenda). */
    public List<Reserva> getReservasDelDia(LocalDate fecha) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getInicio().toLocalDate().equals(fecha)) {
                resultado.add(reserva);
            }
        }
        return ordenarPorInicio(resultado);
    }

    /**
     * Reserva que se está jugando ahora en una cancha. El mapa dinámico la usa
     * para pintar la cancha como "EN CURSO" y abrir el POS.
     */
    public Optional<Reserva> getReservaEnCurso(Cancha cancha, LocalDateTime ahora) {
        for (Reserva reserva : reservas) {
            if (reserva.getCancha().equals(cancha) && reserva.estaEnCurso(ahora)) {
                return Optional.of(reserva);
            }
        }
        return Optional.empty();
    }

    // =====================================================================
    //  TIENDA (módulo opcional)
    // =====================================================================

    public void agregarProducto(Producto producto) throws DatoInvalidoException {
        exigirNoNulo(producto, "producto");
        exigirCodigoLibre(productos, producto.getCodigo(), "producto");
        productos.put(producto.getCodigo(), producto);
    }

    public Optional<Producto> buscarProducto(String codigo) {
        return Optional.ofNullable(productos.get(codigo));
    }

    public Collection<Producto> getProductos() {
        return Collections.unmodifiableCollection(productos.values());
    }

    /** Productos que llegaron a su stock mínimo (alerta de reabastecimiento). */
    public List<Producto> getProductosBajoMinimo() {
        List<Producto> resultado = new ArrayList<>();
        for (Producto producto : productos.values()) {
            if (producto.estaBajoMinimo()) {
                resultado.add(producto);
            }
        }
        return resultado;
    }

    public void agregarVenta(Venta venta) throws DatoInvalidoException {
        exigirNoNulo(venta, "venta");
        for (Venta existente : ventas) {
            if (existente.getNumero().equals(venta.getNumero())) {
                throw new DatoInvalidoException("Ya existe la venta " + venta.getNumero() + ".");
            }
        }
        ventas.add(venta);
    }

    public Optional<Venta> buscarVenta(String numero) {
        for (Venta venta : ventas) {
            if (venta.getNumero().equals(numero)) {
                return Optional.of(venta);
            }
        }
        return Optional.empty();
    }

    public List<Venta> getVentas() {
        return Collections.unmodifiableList(ventas);
    }

    /** Consumo de tienda cargado a una reserva (la Reserva no conoce sus ventas). */
    public List<Venta> getVentasDeReserva(Reserva reserva) {
        List<Venta> resultado = new ArrayList<>();
        for (Venta venta : ventas) {
            if (reserva != null && reserva.equals(venta.getReserva())) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    /** Total consumido en la tienda por una reserva. */
    public double calcularConsumoTienda(Reserva reserva) {
        double total = 0;
        for (Venta venta : getVentasDeReserva(reserva)) {
            total += venta.calcularTotal();
        }
        return MonedaUtil.redondear(total);
    }

    // =====================================================================
    //  CÓDIGOS CORRELATIVOS
    // =====================================================================

    /**
     * Calcula el siguiente código libre a partir de lo que hay en memoria.
     * Ejemplo: con R0001..R0005 cargadas → "R0006".
     */
    public String generarCodigo(GeneradorCodigo.Tipo tipo) {
        return GeneradorCodigo.siguiente(tipo, codigosExistentes(tipo));
    }

    private List<String> codigosExistentes(GeneradorCodigo.Tipo tipo) {
        List<String> codigos = new ArrayList<>();
        switch (tipo) {
            case USUARIO:
                codigos.addAll(usuarios.keySet());
                break;
            case CLIENTE:
                codigos.addAll(clientes.keySet());
                break;
            case CANCHA:
                codigos.addAll(canchas.keySet());
                break;
            case SERVICIO:
                codigos.addAll(servicios.keySet());
                break;
            case PRODUCTO:
                codigos.addAll(productos.keySet());
                break;
            case TARIFA:
                for (Tarifa tarifa : tarifas) {
                    codigos.add(tarifa.getCodigo());
                }
                break;
            case RESERVA:
                for (Reserva reserva : reservas) {
                    codigos.add(reserva.getCodigo());
                }
                break;
            case PAGO:
                for (Reserva reserva : reservas) {
                    for (Pago pago : reserva.getPagos()) {
                        codigos.add(pago.getCodigo());
                    }
                }
                break;
            case VENTA:
                for (Venta venta : ventas) {
                    codigos.add(venta.getNumero());
                }
                break;
            default:
                break;
        }
        return codigos;
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static List<Reserva> ordenarPorInicio(List<Reserva> lista) {
        lista.sort(Comparator.comparing(Reserva::getInicio));
        return lista;
    }

    private static void exigirNoNulo(Object objeto, String nombre) throws DatoInvalidoException {
        if (objeto == null) {
            throw new DatoInvalidoException("El dato '" + nombre + "' es obligatorio.");
        }
    }

    private static void exigirCodigoLibre(Map<String, ?> mapa, String codigo, String nombre)
            throws DatoInvalidoException {
        if (mapa.containsKey(codigo)) {
            throw new DatoInvalidoException("Código duplicado: ya existe " + nombre + " " + codigo + ".");
        }
    }
}

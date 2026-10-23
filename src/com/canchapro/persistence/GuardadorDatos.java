/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.canchas.Cancha;
import com.canchapro.models.canchas.Tarifa;
import com.canchapro.models.canchas.TarifaEspecial;
import com.canchapro.models.canchas.TarifaFranja;
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
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
/**
 *
 * @author SANTANA
 */
/**
 * Convierte los objetos en memoria a archivos CSV al CERRAR el sistema
 * (o con la acción "Guardar Sistema"). Es el camino inverso de CargadorDatos.
 *
 * Cada archivo se reescribe completo a partir de las colecciones del
 * ComplejoDeportivo. Las columnas y su orden deben coincidir EXACTAMENTE con
 * lo que espera CargadorDatos; por eso los encabezados están definidos aquí
 * como constantes, uno por archivo.
 *
 * Guardado "todo o nada": primero se arman TODAS las filas en memoria y solo
 * después se escriben los archivos. Si algo falla al armar, ningún archivo se toca.
 *
 * Formato de los valores
 *
 *   Fechas: ISO (2026-10-06T15:00) con FechaUtil.aCsv().
 *   Montos: punto decimal y 2 decimales (150.00) con MonedaUtil.aCsv().
 *   Enums: name() (MANTENIMIENTO), nunca toString() ("En mantenimiento").
 *   Opcionales vacíos: "" (dos ; seguidos).
 */
public class GuardadorDatos {

    // Encabezados: el número de columnas debe coincidir con CargadorDatos.
    static final String[] ENC_USUARIOS = {"codigo", "nombre", "telefono", "username", "passwordHash", "rol", "activo"};
    static final String[] ENC_CLIENTES = {"codigo", "nombre", "telefono", "correo", "fechaRegistro"};
    static final String[] ENC_CANCHAS = {"codigo", "nombre", "tipo", "capacidad", "estado", "horaApertura", "horaCierre"};
    static final String[] ENC_TARIFAS = {"codigo", "tipoTarifa", "nombre", "precioHora", "prioridad", "dias",
        "horaInicio", "horaFin", "fecha", "motivo"};
    static final String[] ENC_SERVICIOS = {"codigo", "nombre", "precio", "cobroPorHora", "activo"};
    static final String[] ENC_PRODUCTOS = {"codigo", "nombre", "categoria", "precio", "existencia", "stockMinimo", "estado"};
    static final String[] ENC_RESERVAS = {"codigo", "codigoCliente", "codigoCancha", "inicio", "fin", "precioCancha",
        "estado", "fechaCreacion", "creadaPor", "fechaCancelacion", "motivoCancelacion", "canceladaPor"};
    static final String[] ENC_SERVICIOS_RESERVA = {"codigoReserva", "codigoServicio", "cantidad", "precioUnitario", "horas"};
    static final String[] ENC_PAGOS = {"codigo", "codigoReserva", "fechaHora", "monto", "metodo", "observacion"};
    static final String[] ENC_VENTAS = {"numero", "fechaHora", "codigoReserva", "metodoPago", "vendedor", "total",
        "fechaPago"};
    static final String[] ENC_DETALLE_VENTAS = {"numeroVenta", "codigoProducto", "cantidad", "precioUnitario", "subtotal"};

    private final Path carpeta;

    /**
     * @param carpeta carpeta destino (normalmente "data")
     */
    public GuardadorDatos(Path carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * Escribe la configuración y los 11 CSV.
     *
     * @param complejo sistema en memoria
     * @return cantidad total de registros escritos (para el mensaje al operador)
     * @throws IOException si algún archivo no se puede escribir
     */
    public int guardar(ComplejoDeportivo complejo) throws IOException {
        // 1) Armar TODAS las filas en memoria. Si algo falla aquí, ningún archivo se tocó.
        Map<String, List<String[]>> filas = new LinkedHashMap<>();
        filas.put(CargadorDatos.ARCHIVO_USUARIOS, filasUsuarios(complejo));
        filas.put(CargadorDatos.ARCHIVO_CLIENTES, filasClientes(complejo));
        filas.put(CargadorDatos.ARCHIVO_CANCHAS, filasCanchas(complejo));
        filas.put(CargadorDatos.ARCHIVO_TARIFAS, filasTarifas(complejo));
        filas.put(CargadorDatos.ARCHIVO_SERVICIOS, filasServicios(complejo));
        filas.put(CargadorDatos.ARCHIVO_PRODUCTOS, filasProductos(complejo));
        filas.put(CargadorDatos.ARCHIVO_RESERVAS, filasReservas(complejo));
        filas.put(CargadorDatos.ARCHIVO_SERVICIOS_RESERVA, filasServiciosReserva(complejo));
        filas.put(CargadorDatos.ARCHIVO_PAGOS, filasPagos(complejo));
        filas.put(CargadorDatos.ARCHIVO_VENTAS, filasVentas(complejo));
        filas.put(CargadorDatos.ARCHIVO_DETALLE_VENTAS, filasDetalleVentas(complejo));

        // 2) Recién ahora se escriben los archivos (mismo orden).
        Map<String, String[]> encabezados = new LinkedHashMap<>();
        encabezados.put(CargadorDatos.ARCHIVO_USUARIOS, ENC_USUARIOS);
        encabezados.put(CargadorDatos.ARCHIVO_CLIENTES, ENC_CLIENTES);
        encabezados.put(CargadorDatos.ARCHIVO_CANCHAS, ENC_CANCHAS);
        encabezados.put(CargadorDatos.ARCHIVO_TARIFAS, ENC_TARIFAS);
        encabezados.put(CargadorDatos.ARCHIVO_SERVICIOS, ENC_SERVICIOS);
        encabezados.put(CargadorDatos.ARCHIVO_PRODUCTOS, ENC_PRODUCTOS);
        encabezados.put(CargadorDatos.ARCHIVO_RESERVAS, ENC_RESERVAS);
        encabezados.put(CargadorDatos.ARCHIVO_SERVICIOS_RESERVA, ENC_SERVICIOS_RESERVA);
        encabezados.put(CargadorDatos.ARCHIVO_PAGOS, ENC_PAGOS);
        encabezados.put(CargadorDatos.ARCHIVO_VENTAS, ENC_VENTAS);
        encabezados.put(CargadorDatos.ARCHIVO_DETALLE_VENTAS, ENC_DETALLE_VENTAS);

        ArchivoConfiguracion.guardar(complejo.getConfiguracion(),
                carpeta.resolve(ArchivoConfiguracion.NOMBRE_ARCHIVO));
        int total = 0;
        for (Map.Entry<String, List<String[]>> archivo : filas.entrySet()) {
            total += escribir(archivo.getKey(), encabezados.get(archivo.getKey()), archivo.getValue());
        }
        return total;
    }

    private int escribir(String nombre, String[] encabezado, List<String[]> filas) throws IOException {
        ArchivoCsv.escribir(carpeta.resolve(nombre), encabezado, filas);
        return filas.size();
    }

    // =====================================================================
    //  CATÁLOGOS
    // =====================================================================

    private static List<String[]> filasUsuarios(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Usuario u : complejo.getUsuarios()) {
            filas.add(new String[]{u.getCodigo(), u.getNombre(), u.getTelefono(), u.getUsername(),
                u.getPasswordHash(), u.getRol().name(), String.valueOf(u.isActivo())});
        }
        return filas;
    }

    private static List<String[]> filasClientes(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Cliente c : complejo.getClientes()) {
            filas.add(new String[]{c.getCodigo(), c.getNombre(), c.getTelefono(), c.getCorreo(),
                FechaUtil.aCsv(c.getFechaRegistro())});
        }
        return filas;
    }

    private static List<String[]> filasCanchas(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Cancha c : complejo.getCanchas()) {
            filas.add(new String[]{c.getCodigo(), c.getNombre(), c.getTipo().name(),
                String.valueOf(c.getCapacidad()), c.getEstado().name(),
                FechaUtil.aCsv(c.getHoraApertura()), FechaUtil.aCsv(c.getHoraCierre())});
        }
        return filas;
    }

    /**
     * Las columnas que no aplican a un tipo de tarifa quedan vacías:
     * una FRANJA no tiene fecha ni motivo; una ESPECIAL no tiene días ni horas.
     */
    private static List<String[]> filasTarifas(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Tarifa t : complejo.getTarifas()) {
            String dias = "";
            String horaInicio = "";
            String horaFin = "";
            String fecha = "";
            String motivo = "";
            if (t instanceof TarifaFranja) {
                TarifaFranja franja = (TarifaFranja) t;
                dias = unirDias(franja);
                horaInicio = FechaUtil.aCsv(franja.getHoraInicio());
                horaFin = FechaUtil.aCsv(franja.getHoraFin());
            } else if (t instanceof TarifaEspecial) {
                TarifaEspecial especial = (TarifaEspecial) t;
                fecha = FechaUtil.aCsv(especial.getFecha());
                motivo = especial.getMotivo();
            }
            filas.add(new String[]{t.getCodigo(), t.getTipoTarifa(), t.getNombre(),
                MonedaUtil.aCsv(t.getPrecioHora()), String.valueOf(t.getPrioridad()),
                dias, horaInicio, horaFin, fecha, motivo});
        }
        return filas;
    }

    /** {MONDAY, FRIDAY} → "MONDAY|FRIDAY" (en orden de la semana, gracias a EnumSet). */
    private static String unirDias(TarifaFranja franja) {
        StringJoiner sj = new StringJoiner(CargadorDatos.SEPARADOR_DIAS);
        for (DayOfWeek dia : franja.getDias()) {
            sj.add(dia.name());
        }
        return sj.toString();
    }

    private static List<String[]> filasServicios(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (ServicioAdicional s : complejo.getServicios()) {
            filas.add(new String[]{s.getCodigo(), s.getNombre(), MonedaUtil.aCsv(s.getPrecio()),
                String.valueOf(s.isCobroPorHora()), String.valueOf(s.isActivo())});
        }
        return filas;
    }

    private static List<String[]> filasProductos(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Producto p : complejo.getProductos()) {
            filas.add(new String[]{p.getCodigo(), p.getNombre(), p.getCategoria().name(),
                MonedaUtil.aCsv(p.getPrecio()), String.valueOf(p.getExistencia()),
                String.valueOf(p.getStockMinimo()), p.getEstado().name()});
        }
        return filas;
    }

    // =====================================================================
    //  RESERVAS Y SUS PARTES
    // =====================================================================

    private static List<String[]> filasReservas(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            Cancelacion c = r.getCancelacion();
            filas.add(new String[]{r.getCodigo(), r.getCliente().getCodigo(), r.getCancha().getCodigo(),
                FechaUtil.aCsv(r.getInicio()), FechaUtil.aCsv(r.getFin()),
                MonedaUtil.aCsv(r.getPrecioCancha()), r.getEstado().name(),
                FechaUtil.aCsv(r.getFechaCreacion()), r.getCreadaPor().getCodigo(),
                c == null ? "" : FechaUtil.aCsv(c.getFechaHora()),
                c == null ? "" : c.getMotivo(),
                c == null ? "" : c.getResponsable().getCodigo()});
        }
        return filas;
    }

    /** Una fila por cada servicio de cada reserva (composición "aplanada"). */
    private static List<String[]> filasServiciosReserva(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            for (ServicioReserva s : r.getServicios()) {
                filas.add(new String[]{r.getCodigo(), s.getServicio().getCodigo(),
                    String.valueOf(s.getCantidad()), MonedaUtil.aCsv(s.getPrecioUnitario()),
                    String.valueOf(s.getHoras())});
            }
        }
        return filas;
    }

    /** Una fila por cada pago de cada reserva. */
    private static List<String[]> filasPagos(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Reserva r : complejo.getReservas()) {
            for (Pago p : r.getPagos()) {
                filas.add(new String[]{p.getCodigo(), r.getCodigo(), FechaUtil.aCsv(p.getFechaHora()),
                    MonedaUtil.aCsv(p.getMonto()), p.getMetodo().name(), p.getObservacion()});
            }
        }
        return filas;
    }

    // =====================================================================
    //  TIENDA
    // =====================================================================

    /**
     * Solo se guardan las ventas CONFIRMADAS: un carrito a medio armar no es una venta.
     * metodoPago y fechaPago vacíos = venta "a cuenta" de una reserva, todavía sin cobrar.
     */
    private static List<String[]> filasVentas(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Venta v : complejo.getVentas()) {
            if (!v.isConfirmada()) {
                continue;
            }
            filas.add(new String[]{v.getNumero(), FechaUtil.aCsv(v.getFechaHora()),
                v.getReserva() == null ? "" : v.getReserva().getCodigo(),
                v.isPendienteDePago() ? "" : v.getMetodoPago().name(),
                v.getVendedor().getCodigo(),
                MonedaUtil.aCsv(v.calcularTotal()),
                v.getFechaPago() == null ? "" : FechaUtil.aCsv(v.getFechaPago())});
        }
        return filas;
    }

    private static List<String[]> filasDetalleVentas(ComplejoDeportivo complejo) {
        List<String[]> filas = new ArrayList<>();
        for (Venta v : complejo.getVentas()) {
            if (!v.isConfirmada()) {
                continue;
            }
            for (DetalleVenta d : v.getDetalles()) {
                filas.add(new String[]{v.getNumero(), d.getProducto().getCodigo(),
                    String.valueOf(d.getCantidad()), MonedaUtil.aCsv(d.getPrecioUnitario()),
                    MonedaUtil.aCsv(d.getSubtotal())});
            }
        }
        return filas;
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.personas.Cliente;
import com.canchapro.models.reservas.Reserva;
import com.canchapro.utils.FechaUtil;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.Validador;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.canchapro.models.enums.EstadoReserva;
import com.canchapro.utils.MonedaUtil;
import java.time.LocalDateTime;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del módulo Clientes (registrar, buscar, editar, ver historial).
 *
 * Cualquier usuario con sesión (administrador u operador) puede gestionar
 * clientes, porque registrarlos es parte del trabajo diario de recepción.
 *
 * Reglas de negocio que se aplican aquí:
 *
 *  Buscar antes de registrar: no se permiten dos clientes con el mismo
 *       teléfono (sí puede haber dos "Juan Pérez" con teléfonos distintos).
 *  Los clientes no se eliminan: tienen reservas y pagos en el historial.
 *  El código y la fecha de registro los pone el sistema, no el operador.
 * 
 */

public class ClienteController {
 
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;
 
    /**
     * @param complejo sistema en memoria
     * @param sesion   sesión del usuario (para el permiso de eliminar)
     */
    public ClienteController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }
 
    // =====================================================================
    //  CONSULTAS
    // =====================================================================
 
    /** @return todos los clientes ordenados por nombre */
    public List<Cliente> listar() {
        return complejo.buscarClientes("");
    }
 
    /**
     * Búsqueda por código, nombre (sin tildes ni mayúsculas) o teléfono.
     * Criterio vacío = todos.
     */
    public List<Cliente> buscar(String criterio) {
        return complejo.buscarClientes(criterio);
    }
 
    /**
     * @throws EntidadNoEncontradaException si el código no existe
     */
    public Cliente obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarCliente(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el cliente " + codigo + "."));
    }
 
    /** @return reservas del cliente, de la más antigua a la más reciente */
    public List<Reserva> getHistorial(String codigoCliente) throws EntidadNoEncontradaException {
        return complejo.getReservasDeCliente(obtener(codigoCliente));
    }
  /**
     * Números del cliente para su ficha (pantalla Clientes). Se calculan aquí,
     * no en la vista, para que la ventana solo muestre resultados.
     *
     * @throws EntidadNoEncontradaException si el código no existe
     */
    public ResumenCliente getResumen(String codigoCliente) throws EntidadNoEncontradaException {
        int total = 0;
        int activas = 0;
        int canceladas = 0;
        double pagado = 0;
        double saldo = 0;
        LocalDateTime ultima = null;
        for (Reserva r : getHistorial(codigoCliente)) {
            total++;
            pagado += r.calcularTotalPagado();
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                canceladas++;
                continue; // una cancelada no es deuda
            }
            if (!r.getEstado().esTerminal()) {
                activas++;
            }
            saldo += r.calcularSaldo();
            if (ultima == null || r.getInicio().isAfter(ultima)) {
                ultima = r.getInicio();
            }
        }
        return new ResumenCliente(total, activas, canceladas, MonedaUtil.redondear(pagado),
                MonedaUtil.redondear(saldo), ultima);
    }

    /** Solo el administrador ve habilitado el botón Eliminar. */
    public boolean puedeEliminar() {
        return sesion.esAdministrador();
    }
    
    /** @return el código que recibirá el próximo cliente (para mostrarlo en el formulario) */
    public String getSiguienteCodigo() {
        return complejo.generarCodigo(GeneradorCodigo.Tipo.CLIENTE);
    }
 
    // =====================================================================
    //  OPERACIONES
    // =====================================================================
 
    /**
     * Registra un cliente nuevo en memoria.
     *
     * @param nombre   nombre completo
     * @param telefono 8 dígitos (se aceptan guiones o espacios)
     * @param correo   opcional
     * @return el cliente creado (con su código asignado)
     * @throws CanchaProException si un dato es inválido o el teléfono ya está registrado
     */
    public Cliente registrar(String nombre, String telefono, String correo) throws CanchaProException {
        exigirTelefonoLibre(telefono, null);
        Cliente cliente = new Cliente(getSiguienteCodigo(), nombre, telefono, correo,
                FechaUtil.ahora().toLocalDate());
        complejo.agregarCliente(cliente);
        return cliente;
    }
 
    /**
     * Modifica nombre, teléfono y correo. El código y la fecha de registro no cambian.
     *
     * Primero se validan TODOS los datos y después se aplican: así nunca queda
     * un cliente "a medias" (nombre cambiado pero teléfono no) si algo falla.
     *
     * @throws CanchaProException si un dato es inválido o el teléfono pertenece a otro cliente
     */
    public Cliente actualizar(String codigo, String nombre, String telefono, String correo)
            throws CanchaProException {
        Cliente cliente = obtener(codigo);
 
        // 1) Validar todo
        Validador.validarRequerido(nombre, "nombre");
        Validador.validarTelefono(telefono);
        Validador.validarCorreoOpcional(correo);
        exigirTelefonoLibre(telefono, cliente);
 
        // 2) Aplicar (ya no puede fallar)
        cliente.setNombre(nombre);
        cliente.setTelefono(telefono);
        cliente.setCorreo(correo);
        return cliente;
    }
 
    /**
     * Elimina un cliente registrado por error. Solo administrador y solo si el
     * cliente nunca hizo una reserva (la regla vive en ComplejoDeportivo).
     */
    public void eliminar(String codigo) throws CanchaProException {
        sesion.exigirAdministrador("eliminar clientes");
        complejo.eliminarCliente(codigo);
    }
 
    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================
 
    /** Busca otro cliente con el mismo teléfono (comparando solo dígitos). */
    private Optional<Cliente> buscarPorTelefono(String telefono) {
        String digitos = Validador.normalizarTelefono(telefono);
        for (Cliente c : complejo.getClientes()) {
            if (c.getTelefono().equals(digitos)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
 
    /**
     * @param actual el cliente que se está editando (null si es uno nuevo);
     *               su propio teléfono no cuenta como duplicado
     */
    private void exigirTelefonoLibre(String telefono, Cliente actual) throws DatoInvalidoException {
        if (!Validador.esTelefonoValido(telefono)) {
            return; // el constructor o el setter darán el mensaje de formato
        }
        Optional<Cliente> otro = buscarPorTelefono(telefono);
        if (otro.isPresent() && !otro.get().equals(actual)) {
            throw new DatoInvalidoException("Ya existe un cliente con el teléfono "
                    + otro.get().getTelefono() + ": " + otro.get() + ".");
        }
    }
      // =====================================================================
    //  CLASE ANIDADA: resumen para la ficha del cliente
    // =====================================================================

    /** Datos ya calculados que la pantalla Clientes muestra en la ficha. Inmutable. */
    public static final class ResumenCliente {

        private final int reservas;
        private final int activas;
        private final int canceladas;
        private final double totalPagado;
        private final double saldoPendiente;
        private final LocalDateTime ultimaReserva;

        private ResumenCliente(int reservas, int activas, int canceladas, double totalPagado,
                               double saldoPendiente, LocalDateTime ultimaReserva) {
            this.reservas = reservas;
            this.activas = activas;
            this.canceladas = canceladas;
            this.totalPagado = totalPagado;
            this.saldoPendiente = saldoPendiente;
            this.ultimaReserva = ultimaReserva;
        }

        /** Total de reservas en el historial (incluye canceladas). */
        public int getReservas() {
            return reservas;
        }

        /** Reservas que todavía no terminan (pendiente, confirmada o en curso). */
        public int getActivas() {
            return activas;
        }

        public int getCanceladas() {
            return canceladas;
        }

        /** Suma de todo lo que el cliente ha pagado. */
        public double getTotalPagado() {
            return totalPagado;
        }

        /** Lo que aún debe en reservas no canceladas. */
        public double getSaldoPendiente() {
            return saldoPendiente;
        }

        public boolean tieneSaldo() {
            return !MonedaUtil.esCero(saldoPendiente);
        }

        /** @return inicio de su reserva más reciente (no cancelada), o null si no tiene */
        public LocalDateTime getUltimaReserva() {
            return ultimaReserva;
        }
    }
}
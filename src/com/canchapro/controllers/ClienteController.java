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

    /**
     * @param complejo sistema en memoria
     */
    public ClienteController(ComplejoDeportivo complejo) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
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
}

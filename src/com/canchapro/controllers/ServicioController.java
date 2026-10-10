/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.reservas.ServicioAdicional;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.Validador;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del catálogo de servicios adicionales (balones, chalecos,
 * árbitro, iluminación...).
 *
 * Permisos: consultar y cotizar es libre (lo usa Nueva Reserva);
 * crear, modificar y activar/desactivar es solo del ADMINISTRADOR.
 *
 * Los servicios no se eliminan: se desactivan. Las reservas antiguas que
 * los usaron conservan su línea con el precio de aquel momento.
 */
public class ServicioController {
 
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;
 
    public ServicioController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }
 
    // =====================================================================
    //  CONSULTAS (cualquier usuario)
    // =====================================================================
 
    /** @return todos los servicios (activos e inactivos), para la pantalla de administración */
    public List<ServicioAdicional> listar() {
        return new ArrayList<>(complejo.getServicios());
    }
 
    /** @return solo los que se pueden agregar a una reserva hoy */
    public List<ServicioAdicional> listarActivos() {
        List<ServicioAdicional> activos = new ArrayList<>();
        for (ServicioAdicional servicio : complejo.getServicios()) {
            if (servicio.isActivo()) {
                activos.add(servicio);
            }
        }
        return activos;
    }
 
    public ServicioAdicional obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarServicio(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el servicio " + codigo + "."));
    }
 
    /**
     * Cuánto costaría el servicio con el precio actual (vista previa en Nueva Reserva).
     *
     * @param horas duración de la reserva; solo influye si el servicio se cobra por hora
     */
    public double cotizar(String codigo, int cantidad, double horas) throws CanchaProException {
        Validador.validarEnteroPositivo(cantidad, "cantidad");
        return obtener(codigo).calcularCosto(cantidad, horas);
    }
 
    public boolean puedeAdministrar() {
        return sesion.esAdministrador();
    }
 
    // =====================================================================
    //  OPERACIONES (solo administrador)
    // =====================================================================
 
    /**
     * @param cobroPorHora true = se multiplica por las horas de la reserva (árbitro);
     *                     false = precio fijo por reserva (balón)
     */
    public ServicioAdicional crear(String nombre, double precio, boolean cobroPorHora) throws CanchaProException {
        sesion.exigirAdministrador("crear servicios");
        exigirNombreLibre(nombre, null);
        ServicioAdicional servicio = new ServicioAdicional(
                complejo.generarCodigo(GeneradorCodigo.Tipo.SERVICIO), nombre, precio, cobroPorHora);
        complejo.agregarServicio(servicio);
        return servicio;
    }
 
    /**
     * Cambiar el precio NO altera reservas ya hechas: cada una guardó el precio
     * del servicio al momento de agregarlo ("foto del precio").
     */
    public ServicioAdicional actualizar(String codigo, String nombre, double precio, boolean cobroPorHora)
            throws CanchaProException {
        sesion.exigirAdministrador("modificar servicios");
        ServicioAdicional servicio = obtener(codigo);
 
        // 1) Validar todo con un objeto de prueba
        new ServicioAdicional(codigo, nombre, precio, cobroPorHora);
        exigirNombreLibre(nombre, servicio);
 
        // 2) Aplicar
        servicio.setNombre(nombre);
        servicio.setPrecio(precio);
        servicio.setCobroPorHora(cobroPorHora);
        return servicio;
    }
 
    public void cambiarEstado(String codigo, boolean activo) throws CanchaProException {
        sesion.exigirAdministrador("activar o desactivar servicios");
        obtener(codigo).setActivo(activo);
    }
 
    /** Elimina un servicio que nunca se usó en una reserva. */
    public void eliminar(String codigo) throws CanchaProException {
        sesion.exigirAdministrador("eliminar servicios");
        complejo.eliminarServicio(codigo);
    }
 
    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================
 
    /** Evita dos servicios llamados igual (sin importar tildes ni mayúsculas). */
    private void exigirNombreLibre(String nombre, ServicioAdicional actual) throws DatoInvalidoException {
        if (Validador.esVacio(nombre)) {
            return; // el constructor dará el mensaje de campo obligatorio
        }
        String buscado = Validador.normalizarTexto(nombre);
        for (ServicioAdicional otro : complejo.getServicios()) {
            if (!otro.equals(actual) && Validador.normalizarTexto(otro.getNombre()).equals(buscado)) {
                throw new DatoInvalidoException("Ya existe el servicio " + otro + ".");
            }
        }
    }
}
 
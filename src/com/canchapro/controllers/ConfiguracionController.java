/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.persistence.ResultadoCarga;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del menú CONFIG: parámetros del sistema, resultado de la carga
 * de archivos y la acción explícita "Guardar Sistema".
 *
 * Regla de oro de persistencia: durante la ejecución nadie escribe
 * archivos... excepto {@link #guardarSistema()}, que es justamente la acción
 * EXPLÍCITA que permite el enunciado. Todo lo demás (cambiar la limpieza,
 * apagar la tienda) solo cambia la memoria y se guarda al cerrar.
 *
 * Permisos: cambiar parámetros es del ADMINISTRADOR; guardar y
 * consultar el resultado de la carga lo puede hacer cualquier usuario.
 */
public class ConfiguracionController {
    private final ComplejoDeportivo complejo;
    private final Sesion sesion;
    private final GestorPersistencia gestor;

    /**
     * @param gestor el MISMO gestor que cargó los datos al iniciar (conoce la
     *               carpeta y el resultado de la carga)
     */
    public ConfiguracionController(ComplejoDeportivo complejo, Sesion sesion, GestorPersistencia gestor) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
        this.gestor = Objects.requireNonNull(gestor, "El gestor de persistencia es obligatorio.");
    }

    // =====================================================================
    //  CONSULTAS (para llenar el formulario)
    // =====================================================================

    public ConfiguracionSistema getConfiguracion() {
        return complejo.getConfiguracion();
    }

    public boolean puedeAdministrar() {
        return sesion.esAdministrador();
    }

    // =====================================================================
    //  CAMBIAR PARÁMETROS (solo administrador)
    // =====================================================================

    /**
     * Aplica los parámetros en MEMORIA (efecto inmediato; se escriben en
     * config.properties al guardar o cerrar).
     *
     * Primero se validan en una configuración de prueba; si todo es válido
     * se copian a la real. Así no queda una configuración a medias.
     *
     * @param nombreComplejo        nombre que aparece en el login y el dashboard
     * @param moduloTienda          true = Tienda/Cafetería visible
     * @param minutosLimpieza       0 a 120 minutos libres entre reservas
     * @param porcentajeAnticipo    0 a 100 % del total para confirmar una reserva
     */
    public void actualizar(String nombreComplejo, boolean moduloTienda, int minutosLimpieza,
                           double porcentajeAnticipo) throws CanchaProException {
        sesion.exigirAdministrador("cambiar la configuración");

        // 1) Validar con un objeto de prueba
        ConfiguracionSistema prueba = new ConfiguracionSistema();
        prueba.setNombreComplejo(nombreComplejo);
        prueba.setMinutosLimpieza(minutosLimpieza);
        prueba.setPorcentajeAnticipoMinimo(porcentajeAnticipo);

        // 2) Aplicar a la configuración real
        ConfiguracionSistema real = complejo.getConfiguracion();
        real.setNombreComplejo(nombreComplejo);
        real.setModuloTiendaActivo(moduloTienda);
        real.setMinutosLimpieza(minutosLimpieza);
        real.setPorcentajeAnticipoMinimo(porcentajeAnticipo);
    }

    // =====================================================================
    //  GUARDAR SISTEMA (acción explícita)
    // =====================================================================

    /**
     * Escribe TODA la memoria en los CSV (con respaldo previo automático).
     *
     * @return mensaje para mostrar, ej. "Sistema guardado: 52 registros en C:\...\data"
     * @throws CanchaProException si el disco falla (el mensaje explica la causa)
     */
    public String guardarSistema() throws CanchaProException {
        try {
            int registros = gestor.guardarTodo(complejo);
            return "Sistema guardado: " + registros + " registros en "
                    + gestor.getCarpetaDatos().toAbsolutePath() + ".";
        } catch (IOException e) {
            throw new CanchaProException("No se pudo guardar el sistema: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    //  RESULTADO DE LA CARGA DE ARCHIVOS
    // =====================================================================

    /** Ej.: "Carga completada: 43 registros, 0 errores, 0 advertencias." */
    public String getResumenCarga() {
        return gestor.getUltimoResultado().getResumen();
    }

    /** Errores de la carga (línea descartada), listos para mostrar en una lista. */
    public List<String> getErroresCarga() {
        return comoTexto(gestor.getUltimoResultado().getErrores());
    }

    /** Advertencias de la carga (archivo faltante, valor por defecto...). */
    public List<String> getAdvertenciasCarga() {
        return comoTexto(gestor.getUltimoResultado().getAdvertencias());
    }

    public String getCarpetaDatos() {
        return gestor.getCarpetaDatos().toAbsolutePath().toString();
    }

    /** @return nombres de las carpetas de respaldo (de la más vieja a la más nueva) */
    public List<String> getRespaldos() throws CanchaProException {
        try {
            List<String> nombres = new ArrayList<>();
            for (Path p : gestor.listarRespaldos()) {
                nombres.add(p.getFileName().toString());
            }
            return nombres;
        } catch (IOException e) {
            throw new CanchaProException("No se pudo leer la carpeta de respaldos: " + e.getMessage(), e);
        }
    }

    private static List<String> comoTexto(List<ResultadoCarga.Incidencia> incidencias) {
        List<String> textos = new ArrayList<>();
        for (ResultadoCarga.Incidencia i : incidencias) {
            textos.add(i.toString());
        }
        return textos;
    }
}

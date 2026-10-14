/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.persistence.ResultadoCarga;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author SANTANA
 */
/**
 * "Director de orquesta" del sistema: controla el CICLO DE VIDA completo.
 *
 * 
 *  Main ──► iniciar()            carga los CSV en memoria (UNA sola vez)
 *        ──► iniciarSesion(...)  login + crea todos los controladores
 *        ──► (el usuario trabaja: TODO en memoria)
 *        ──► cerrarSesion()      vuelve al login sin perder datos
 *        ──► cerrar()            guarda los CSV al salir (UNA sola vez)
 * 
 *
 * Es el único objeto que conoce al {@link GestorPersistencia} y al
 * {@link ComplejoDeportivo} a la vez. Las vistas reciben de aquí cada
 * controlador ya armado (inyección por constructor), así ninguna vista crea
 * modelos ni toca archivos.
 */
public class SistemaController {
 
    private final GestorPersistencia gestor;
    private ComplejoDeportivo complejo;
    private LoginController loginController;
    /** Panel oculto Ctrl+Shift+D: existe desde el arranque, no depende de la sesión. */
    private DesarrolladorController desarrolladorController;
    
    // Controladores de la sesión activa (null mientras nadie haya iniciado sesión)
    private Sesion sesion;
    private ClienteController clienteController;
    private UsuarioController usuarioController;
    private CanchaController canchaController;
    private TarifaController tarifaController;
    private ServicioController servicioController;
    private ReservaController reservaController;
    private PagoController pagoController;
    private MapaController mapaController;
    private TiendaController tiendaController;
    private PosController posController;
    private ConfiguracionController configuracionController;
    private ReporteController reporteController;
 
    /** true después de restaurar datos de prueba: al cerrar NO se debe guardar la memoria vieja. */
    private boolean restauracionPendiente;
 
    /** Usa la carpeta "data" junto al programa. */
    public SistemaController() {
        this(new GestorPersistencia());
    }
 
    /** Para pruebas: trabaja con otra carpeta de datos. */
    public SistemaController(Path carpetaDatos) {
        this(new GestorPersistencia(carpetaDatos));
    }
 
    private SistemaController(GestorPersistencia gestor) {
        this.gestor = Objects.requireNonNull(gestor);
    }
 
    // =====================================================================
    //  1. ARRANQUE
    // =====================================================================
 
    /**
     * Lee TODOS los CSV y arma el sistema en memoria. Nunca falla por un dato
     * malo: las líneas incorrectas quedan en el resultado (prueba #11).
     *
     * @return bitácora de la carga (para avisar al usuario si hubo errores)
     */
    public ResultadoCarga iniciar() {
        complejo = gestor.cargarTodo();
        loginController = new LoginController(complejo);
        desarrolladorController = new DesarrolladorController(complejo, gestor,
                () -> restauracionPendiente = true);
        restauracionPendiente = false;
        return gestor.getUltimoResultado();
    }
 
    public boolean estaIniciado() {
        return complejo != null;
    }
 
    public LoginController getLoginController() {
        exigirIniciado();
        return loginController;
    }
     /** Panel del desarrollador (protegido por su propia clave, no por la sesión). */
    public DesarrolladorController getDesarrolladorController() {
        exigirIniciado();
        return desarrolladorController;
    }
    public String getNombreComplejo() {
        exigirIniciado();
        return complejo.getNombre();
    }
 
    // =====================================================================
    //  2. SESIÓN
    // =====================================================================
 
    /**
     * Valida las credenciales y, si son correctas, crea todos los controladores
     * con la sesión del usuario. También pone al día los estados de las reservas
     * según la hora actual.
     *
     * @return la sesión abierta
     * @throws CanchaProException si las credenciales fallan (mensaje del LoginController)
     */
    public Sesion iniciarSesion(String username, char[] contrasena) throws CanchaProException {
        exigirIniciado();
        Sesion nueva = loginController.iniciarSesion(username, contrasena);
        crearControladores(nueva);
        reservaController.actualizarEstadosPorHora();
        return nueva;
    }
 
    /** Vuelve al login. Los datos siguen en memoria; NO se guarda nada aquí. */
    public void cerrarSesion() {
        sesion = null;
        clienteController = null;
        usuarioController = null;
        canchaController = null;
        tarifaController = null;
        servicioController = null;
        reservaController = null;
        pagoController = null;
        mapaController = null;
        tiendaController = null;
        posController = null;
        configuracionController = null;
        reporteController = null;
    }
 
    public boolean haySesion() {
        return sesion != null;
    }
 
    /**
     * Crea los controladores en orden de dependencia: MapaController necesita
     * al ReservaController, por eso se crea después.
     */
    private void crearControladores(Sesion nueva) {
        sesion = nueva;
        clienteController = new ClienteController(complejo, sesion);
        usuarioController = new UsuarioController(complejo, sesion);
        canchaController = new CanchaController(complejo, sesion);
        tarifaController = new TarifaController(complejo, sesion);
        servicioController = new ServicioController(complejo, sesion);
        reservaController = new ReservaController(complejo, sesion);
        pagoController = new PagoController(complejo, sesion);
        mapaController = new MapaController(complejo, reservaController);
        tiendaController = new TiendaController(complejo, sesion);
        posController = new PosController(complejo, sesion);
        configuracionController = new ConfiguracionController(complejo, sesion, gestor);
        reporteController = new ReporteController(complejo);
    }
 
    // =====================================================================
    //  3. CIERRE
    // =====================================================================
 
    /**
     * Guarda la memoria en los CSV al salir del programa (regla de oro).
     * Si se restauraron datos de prueba, NO guarda: la memoria tiene los datos
     * viejos y sobrescribiría la restauración.
     *
     * @return mensaje para mostrar o registrar
     * @throws CanchaProException si no se pudo escribir (la vista debe preguntar
     *                            si de todas formas desea salir)
     */
       public String cerrar() throws CanchaProException {
        if (!estaIniciado()) {
            return "El sistema no se había iniciado; no hay nada que guardar.";
        }
        if (restauracionPendiente) {
            return "Se restauraron archivos (datos de prueba o respaldo); se cargarán al volver a abrir el programa.";
        }
        try {
            int registros = gestor.guardarTodo(complejo);
            return "Datos guardados al cerrar: " + registros + " registros.";
        } catch (IOException e) {
            throw new CanchaProException("No se pudieron guardar los datos al cerrar: " + e.getMessage(), e);
        }
    }
 
    // =====================================================================
    //  4. RESTAURAR DATOS DE PRUEBA (requisito del README del PDF)
    // =====================================================================
 
    /**
     * Copia datos_prueba/ sobre data/ (con respaldo previo). Los datos nuevos se
     * ven al reiniciar el programa, y al cerrar NO se guarda la memoria actual.
     *
     * @return mensaje para mostrar
     * @throws CanchaProException si no es administrador o la copia falla
     */
    public String restaurarDatosPrueba() throws CanchaProException {
        exigirSesion();
        sesion.exigirAdministrador("restaurar los datos de prueba");
        try {
            int copiados = gestor.restaurarDesde(GestorPersistencia.CARPETA_DATOS_PRUEBA);
            restauracionPendiente = true;
            return "Se restauraron " + copiados + " archivos de datos de prueba. "
                    + "Cierre y vuelva a abrir el programa para verlos.";
        } catch (IOException e) {
            throw new CanchaProException("No se pudieron restaurar los datos de prueba: " + e.getMessage(), e);
        }
    }
 
    public boolean isRestauracionPendiente() {
        return restauracionPendiente;
    }
 
    // =====================================================================
    //  ACCESO A LOS CONTROLADORES (solo con sesión)
    // =====================================================================
 
    public Sesion getSesion() {
        return exigirSesion();
    }
 
    public ClienteController getClienteController() {
        exigirSesion();
        return clienteController;
    }
 
    public UsuarioController getUsuarioController() {
        exigirSesion();
        return usuarioController;
    }
 
    public CanchaController getCanchaController() {
        exigirSesion();
        return canchaController;
    }
 
    public TarifaController getTarifaController() {
        exigirSesion();
        return tarifaController;
    }
 
    public ServicioController getServicioController() {
        exigirSesion();
        return servicioController;
    }
 
    public ReservaController getReservaController() {
        exigirSesion();
        return reservaController;
    }
 
    public PagoController getPagoController() {
        exigirSesion();
        return pagoController;
    }
 
    public MapaController getMapaController() {
        exigirSesion();
        return mapaController;
    }
 
    public TiendaController getTiendaController() {
        exigirSesion();
        return tiendaController;
    }
 
    public PosController getPosController() {
        exigirSesion();
        return posController;
    }
 
    public ConfiguracionController getConfiguracionController() {
        exigirSesion();
        return configuracionController;
    }
 
    public ReporteController getReporteController() {
        exigirSesion();
        return reporteController;
    }
 
    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================
 
    /** Error de PROGRAMACIÓN (no del usuario): por eso es unchecked. */
    private void exigirIniciado() {
        if (complejo == null) {
            throw new IllegalStateException("Llame a iniciar() antes de usar el sistema.");
        }
    }
 
    private Sesion exigirSesion() {
        if (sesion == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return sesion;
    }
}
 
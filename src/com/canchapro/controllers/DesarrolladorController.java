/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.persistence.GestorPersistencia;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.utils.Seguridad;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
/**
 * Controlador del PANEL OCULTO DEL DESARROLLADOR (Ctrl + Shift + D).
 *
 * No depende de la sesión de un usuario: el acceso se protege con una clave
 * propia del desarrollador (hash SHA-256 en config.properties, de fábrica
 * "dev2026"). Tras {@value #MAX_INTENTOS} intentos fallidos queda bloqueado hasta
 * reiniciar el programa.
 *
 * Funciones (todas exigen haber ingresado la clave):
 * 
 *   Activar / desactivar el módulo Tienda/Cafetería.
 *   Eliminar todos los datos (conserva usuarios y configuración, con respaldo previo).
 *   Listar y restaurar respaldos; restaurar los datos de prueba.
 *   Cambiar colores de la interfaz y la clave del desarrollador.
 * 
 *
 * Colores: este controlador solo guarda el texto "#RRGGBB" en la
 * configuración (modelo). Aplicarlos en pantalla ({@code Tema.cargarColores})
 * lo hace la vista: los controladores no dependen de Swing.
 */
public class DesarrolladorController {
    public static final int MAX_INTENTOS = 3;
    public static final int MIN_CLAVE = 6;
    /** Palabra que hay que escribir para confirmar "Eliminar todos los datos". */
    public static final String PALABRA_CONFIRMACION = "ELIMINAR";

    private final ComplejoDeportivo complejo;
    private final GestorPersistencia gestor;
    private final Runnable alRestaurarArchivos;
    private boolean autenticado;
    private int intentosFallidos;

    /**
     * @param alRestaurarArchivos acción que avisa al SistemaController de que los
     *        archivos fueron reemplazados y NO debe guardar la memoria al cerrar
     */
    public DesarrolladorController(ComplejoDeportivo complejo, GestorPersistencia gestor,
                                   Runnable alRestaurarArchivos) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.gestor = Objects.requireNonNull(gestor, "El gestor es obligatorio.");
        this.alRestaurarArchivos = Objects.requireNonNull(alRestaurarArchivos);
    }

    // =====================================================================
    //  ACCESO
    // =====================================================================

    /**
     * Valida la clave del desarrollador. El arreglo se borra al terminar.
     *
     * @throws CanchaProException si la clave es incorrecta o el acceso está bloqueado
     */
    public void autenticar(char[] clave) throws CanchaProException {
        try {
            if (isBloqueado()) {
                throw new CanchaProException("Acceso de desarrollador bloqueado. Reinicie el programa.");
            }
            if (clave == null || clave.length == 0) {
                throw new DatoInvalidoException("Ingrese la clave del desarrollador.");
            }
            if (!config().verificarClaveDesarrollador(Seguridad.hashContrasena(clave))) {
                intentosFallidos++;
                int restantes = MAX_INTENTOS - intentosFallidos;
                throw new CanchaProException(restantes > 0
                        ? "Clave incorrecta. Le quedan " + restantes + (restantes == 1 ? " intento." : " intentos.")
                        : "Clave incorrecta. El acceso de desarrollador quedó bloqueado.");
            }
            intentosFallidos = 0;
            autenticado = true;
        } finally {
            borrar(clave);
        }
    }

    /** Se llama al cerrar el panel: la próxima vez vuelve a pedir la clave. */
    public void cerrarAcceso() {
        autenticado = false;
    }

    public boolean isAutenticado() {
        return autenticado;
    }

    public boolean isBloqueado() {
        return intentosFallidos >= MAX_INTENTOS;
    }

    /** Para mostrar un aviso: "Está usando la clave de fábrica, cámbiela". */
    public boolean usaClaveDeFabrica() {
        return config().usaClaveDeFabrica();
    }

    // =====================================================================
    //  MÓDULOS
    // =====================================================================

    public boolean isTiendaActiva() {
        return config().isModuloTiendaActivo();
    }

    /** Activa u oculta el módulo Tienda/Cafetería (se guarda al cerrar el programa). */
    public void setTiendaActiva(boolean activa) throws CanchaProException {
        exigirAcceso();
        config().setModuloTiendaActivo(activa);
    }

    // =====================================================================
    //  DATOS Y RESPALDOS
    // =====================================================================

    /**
     * Borra todos los datos menos usuarios y configuración.
     *
     * Pasos: 1) guarda la memoria actual (así el respaldo incluye lo no guardado),
     * 2) vacía la memoria, 3) guarda de nuevo: ese guardado respalda primero los
     * datos completos en data/backup/ y luego escribe los archivos vacíos.
     *
     * @param confirmacion debe ser exactamente "ELIMINAR"
     * @return mensaje con el resumen
     */
    public String eliminarTodosLosDatos(String confirmacion) throws CanchaProException {
        exigirAcceso();
        if (!PALABRA_CONFIRMACION.equals(confirmacion == null ? "" : confirmacion.trim())) {
            throw new DatoInvalidoException("Para confirmar escriba " + PALABRA_CONFIRMACION + " en mayúsculas.");
        }
        try {
            gestor.guardarTodo(complejo);
            int eliminados = complejo.vaciarDatos();
            gestor.guardarTodo(complejo);
            List<String> respaldos = listarRespaldos();
            String ultimo = respaldos.isEmpty() ? "-" : respaldos.get(0);
            return "Se eliminaron " + eliminados + " registros (se conservaron usuarios y configuración). "
                    + "Respaldo de los datos anteriores: " + ultimo + ".";
        } catch (IOException e) {
            throw new CanchaProException("No se pudo completar la eliminación: " + e.getMessage(), e);
        }
    }

    /** @return nombres de los respaldos, del más reciente al más antiguo */
    public List<String> listarRespaldos() throws CanchaProException {
        try {
            List<String> nombres = new ArrayList<>();
            for (Path p : gestor.listarRespaldos()) {
                nombres.add(p.getFileName().toString());
            }
            Collections.reverse(nombres);
            return nombres;
        } catch (IOException e) {
            throw new CanchaProException("No se pudo leer la carpeta de respaldos: " + e.getMessage(), e);
        }
    }

    /**
     * Restaura un respaldo. Los datos se ven al REINICIAR el programa, y al cerrar
     * no se guarda la memoria actual (si no, sobrescribiría lo restaurado).
     */
    public String restaurarRespaldo(String nombre) throws CanchaProException {
        exigirAcceso();
        if (nombre == null || !listarRespaldos().contains(nombre)) {
            throw new DatoInvalidoException("No existe el respaldo '" + nombre + "'.");
        }
        Path temporal = null;
        try {
            // Se copia primero a una carpeta temporal: restaurarDesde() crea un respaldo
            // nuevo y podría borrar justo este si es el más antiguo (máximo 5).
            temporal = Files.createTempDirectory("canchapro_restaurar_");
            Path origen = gestor.getCarpetaRespaldos().resolve(nombre);
            for (String archivo : GestorPersistencia.ARCHIVOS) {
                Path a = origen.resolve(archivo);
                if (Files.isRegularFile(a)) {
                    Files.copy(a, temporal.resolve(archivo), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            int copiados = gestor.restaurarDesde(temporal);
            alRestaurarArchivos.run();
            return "Se restauraron " + copiados + " archivos del respaldo " + nombre
                    + ". Cierre y vuelva a abrir el programa para verlos.";
        } catch (IOException e) {
            throw new CanchaProException("No se pudo restaurar el respaldo: " + e.getMessage(), e);
        } finally {
            borrarCarpeta(temporal);
        }
    }

    /** Copia datos_prueba/ sobre data/ (con respaldo previo). Se ve al reiniciar. */
    public String restaurarDatosPrueba() throws CanchaProException {
        exigirAcceso();
        try {
            int copiados = gestor.restaurarDesde(GestorPersistencia.CARPETA_DATOS_PRUEBA);
            alRestaurarArchivos.run();
            return "Se restauraron " + copiados + " archivos de datos de prueba. "
                    + "Cierre y vuelva a abrir el programa para verlos.";
        } catch (IOException e) {
            throw new CanchaProException("No se pudieron restaurar los datos de prueba: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    //  COLORES (solo texto; la vista los aplica con Tema)
    // =====================================================================

    /** @return colores personalizados guardados ("color.PRIMARIO" → "#1E7A46"). No requiere clave. */
    public Map<String, String> getColores() {
        return config().getColores();
    }

    /** Guarda un color nuevo en la configuración (se escribe en disco al cerrar). */
    public void cambiarColor(ColorTema color, String hex) throws CanchaProException {
        exigirAcceso();
        Objects.requireNonNull(color, "El color es obligatorio.");
        config().setColor(color.getClave(), hex);
    }

    /** Vuelve todos los colores a los de fábrica. */
    public void restablecerColores() throws CanchaProException {
        exigirAcceso();
        config().restablecerColores();
    }

    // =====================================================================
    //  CLAVE DEL DESARROLLADOR
    // =====================================================================

    public void cambiarClave(char[] actual, char[] nueva, char[] confirmacion) throws CanchaProException {
        try {
            exigirAcceso();
            if (actual == null || !config().verificarClaveDesarrollador(Seguridad.hashContrasena(actual))) {
                throw new CanchaProException("La clave actual no es correcta.");
            }
            if (nueva == null || nueva.length < MIN_CLAVE) {
                throw new DatoInvalidoException("La clave nueva debe tener al menos " + MIN_CLAVE + " caracteres.");
            }
            if (!Arrays.equals(nueva, confirmacion)) {
                throw new DatoInvalidoException("La clave nueva y su confirmación no coinciden.");
            }
            config().setClaveDesarrolladorHash(Seguridad.hashContrasena(nueva));
        } finally {
            borrar(actual);
            borrar(nueva);
            borrar(confirmacion);
        }
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private ConfiguracionSistema config() {
        return complejo.getConfiguracion();
    }

    private void exigirAcceso() throws CanchaProException {
        if (!autenticado) {
            throw new CanchaProException("Ingrese primero la clave del desarrollador.");
        }
    }

    private static void borrar(char[] arreglo) {
        if (arreglo != null) {
            Arrays.fill(arreglo, '\0');
        }
    }

    private static void borrarCarpeta(Path carpeta) {
        if (carpeta == null) {
            return;
        }
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            List<Path> rutas = new ArrayList<>();
            recorrido.forEach(rutas::add);
            Collections.reverse(rutas);
            for (Path ruta : rutas) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            // Carpeta temporal: si no se puede borrar, el sistema operativo la limpiará.
        }
    }
}

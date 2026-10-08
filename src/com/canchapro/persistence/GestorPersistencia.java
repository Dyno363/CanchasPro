/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import com.canchapro.models.ComplejoDeportivo;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
/**
 *
 * @author SANTANA
 */
/**
 * Fachada ÚNICA de persistencia: el resto del sistema solo habla con esta clase.
 *
 * La regla de oro del proyecto queda concentrada en dos métodos:
 * 
 *   {@link #cargarTodo()}: se llama UNA vez, al iniciar el programa.
 *   {@link #guardarTodo(ComplejoDeportivo)}: se llama al cerrar el programa
 *       o con la acción explícita "Guardar Sistema".
 * 
 * Durante la ejecución normal NADIE escribe archivos.
 *
 * Antes de sobrescribir los CSV, {@code guardarTodo} copia los archivos
 * actuales a {@code data/backup/AAAAMMDD_HHMMSS/}. Se conservan los últimos
 * {@value #MAX_RESPALDOS} respaldos.
 */
public class GestorPersistencia {
    /** Carpeta de datos por defecto, relativa a donde se ejecuta el programa. */
    public static final Path CARPETA_DATOS = Paths.get("data");

    /** Carpeta con la copia limpia de los datos de demostración. */
    public static final Path CARPETA_DATOS_PRUEBA = Paths.get("datos_prueba");

    public static final String NOMBRE_CARPETA_RESPALDO = "backup";
    public static final int MAX_RESPALDOS = 5;

    /** Todos los archivos que forman el "estado" del sistema. */
    public static final List<String> ARCHIVOS = List.of(
            ArchivoConfiguracion.NOMBRE_ARCHIVO,
            CargadorDatos.ARCHIVO_USUARIOS,
            CargadorDatos.ARCHIVO_CLIENTES,
            CargadorDatos.ARCHIVO_CANCHAS,
            CargadorDatos.ARCHIVO_TARIFAS,
            CargadorDatos.ARCHIVO_SERVICIOS,
            CargadorDatos.ARCHIVO_PRODUCTOS,
            CargadorDatos.ARCHIVO_RESERVAS,
            CargadorDatos.ARCHIVO_SERVICIOS_RESERVA,
            CargadorDatos.ARCHIVO_PAGOS,
            CargadorDatos.ARCHIVO_VENTAS,
            CargadorDatos.ARCHIVO_DETALLE_VENTAS);

    private static final DateTimeFormatter FORMATO_RESPALDO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path carpetaDatos;
    private ResultadoCarga ultimoResultado;

    /** Usa la carpeta "data" junto al programa. */
    public GestorPersistencia() {
        this(CARPETA_DATOS);
    }

    /**
     * @param carpetaDatos carpeta donde están los CSV (útil para pruebas con otra carpeta)
     */
    public GestorPersistencia(Path carpetaDatos) {
        this.carpetaDatos = carpetaDatos;
        this.ultimoResultado = new ResultadoCarga();
    }

    public Path getCarpetaDatos() {
        return carpetaDatos;
    }

    public Path getCarpetaRespaldos() {
        return carpetaDatos.resolve(NOMBRE_CARPETA_RESPALDO);
    }

    /** @return la bitácora de la última carga (errores, advertencias y conteos) */
    public ResultadoCarga getUltimoResultado() {
        return ultimoResultado;
    }

    // =====================================================================
    //  CARGAR (al iniciar)
    // =====================================================================

    /**
     * Lee todos los archivos y reconstruye el sistema en memoria.
     * Nunca falla por un dato malo: los problemas quedan en {@link #getUltimoResultado()}.
     *
     * @return el complejo listo para usar (si no hay archivos, vacío con configuración por defecto)
     */
    public ComplejoDeportivo cargarTodo() {
        ultimoResultado = new ResultadoCarga();
        return new CargadorDatos(carpetaDatos).cargar(ultimoResultado);
    }

    // =====================================================================
    //  GUARDAR (al cerrar o con "Guardar Sistema")
    // =====================================================================

    /**
     * Respalda los archivos actuales y luego escribe la memoria a disco.
     * Si el respaldo falla, NO se guarda (para no arriesgar los datos sin copia).
     *
     * @param complejo sistema en memoria
     * @return cantidad de registros escritos
     * @throws IOException si no se pudo respaldar o escribir
     */
    public int guardarTodo(ComplejoDeportivo complejo) throws IOException {
        crearRespaldo();
        return new GuardadorDatos(carpetaDatos).guardar(complejo);
    }

    // =====================================================================
    //  RESPALDOS
    // =====================================================================

    /**
     * Copia los archivos actuales de data/ a data/backup/AAAAMMDD_HHMMSS/
     * y elimina los respaldos más viejos si hay más de {@value #MAX_RESPALDOS}.
     *
     * @return carpeta del respaldo creado, o null si no había archivos que respaldar
     * @throws IOException si no se pudo copiar
     */
    public Path crearRespaldo() throws IOException {
        List<Path> existentes = new ArrayList<>();
        for (String nombre : ARCHIVOS) {
            Path archivo = carpetaDatos.resolve(nombre);
            if (Files.isRegularFile(archivo)) {
                existentes.add(archivo);
            }
        }
        if (existentes.isEmpty()) {
            return null; // primera ejecución: no hay nada que respaldar
        }

        Path destino = carpetaNuevaDeRespaldo();
        Files.createDirectories(destino);
        for (Path archivo : existentes) {
            Files.copy(archivo, destino.resolve(archivo.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        eliminarRespaldosViejos();
        return destino;
    }

    /** @return carpetas de respaldo existentes, de la más vieja a la más nueva */
    public List<Path> listarRespaldos() throws IOException {
        List<Path> respaldos = new ArrayList<>();
        Path carpeta = getCarpetaRespaldos();
        if (!Files.isDirectory(carpeta)) {
            return respaldos;
        }
        try (DirectoryStream<Path> contenido = Files.newDirectoryStream(carpeta)) {
            for (Path p : contenido) {
                if (Files.isDirectory(p)) {
                    respaldos.add(p);
                }
            }
        }
        // El nombre AAAAMMDD_HHMMSS ordena alfabéticamente igual que cronológicamente.
        respaldos.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return respaldos;
    }

    private Path carpetaNuevaDeRespaldo() {
        String base = LocalDateTime.now().format(FORMATO_RESPALDO);
        Path destino = getCarpetaRespaldos().resolve(base);
        int contador = 1;
        while (Files.exists(destino)) { // dos guardados en el mismo segundo
            destino = getCarpetaRespaldos().resolve(base + String.format("_%02d", contador++));
        }
        return destino;
    }

    private void eliminarRespaldosViejos() throws IOException {
        List<Path> respaldos = listarRespaldos();
        for (int i = 0; i < respaldos.size() - MAX_RESPALDOS; i++) {
            eliminarCarpeta(respaldos.get(i));
        }
    }

    private static void eliminarCarpeta(Path carpeta) throws IOException {
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            List<Path> rutas = new ArrayList<>();
            recorrido.forEach(rutas::add);
            // Primero los archivos y al final la carpeta (orden inverso).
            Collections.reverse(rutas);
            for (Path ruta : rutas) {
                Files.deleteIfExists(ruta);
            }
        }
    }

    // =====================================================================
    //  RESTAURAR DATOS DE PRUEBA
    // =====================================================================

    /**
     * Reemplaza los archivos de data/ por los de otra carpeta (normalmente
     * datos_prueba/). Antes hace un respaldo de lo actual.
     *
     * Después de restaurar hay que volver a llamar a {@link #cargarTodo()}
     * (o reiniciar el programa) para que la memoria refleje los nuevos archivos.
     *
     * @param origen carpeta con la copia limpia
     * @return cantidad de archivos copiados
     * @throws IOException si el origen no existe o no se puede copiar
     */
    public int restaurarDesde(Path origen) throws IOException {
        if (!Files.isDirectory(origen)) {
            throw new IOException("No existe la carpeta de origen: " + origen.toAbsolutePath());
        }
        crearRespaldo();
        Files.createDirectories(carpetaDatos);
        int copiados = 0;
        for (String nombre : ARCHIVOS) {
            Path archivo = origen.resolve(nombre);
            if (Files.isRegularFile(archivo)) {
                Files.copy(archivo, carpetaDatos.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
                copiados++;
            }
        }
        return copiados;
    }
}

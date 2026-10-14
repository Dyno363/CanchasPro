/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import com.canchapro.models.ConfiguracionSistema;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Map;
/**
 *
 * @author SANTANA
 */
/**
 * Lee y guarda {@code data/config.properties} hacia y desde {@link ConfiguracionSistema}.
 *
 * Formato (clave=valor, una por línea, # para comentarios):
 * 
 *   nombre_complejo=Complejo Deportivo La 10
 *   modulo_tienda=true
 *   minutos_limpieza=0
 *   porcentaje_anticipo_minimo=0
 *   clave_desarrollador=7691a81c...   
 *   color.PRIMARIO=#1E7A46           
 * Regla de tolerancia: un valor faltante o inválido NUNCA impide que el sistema
 * arranque. Se usa el valor por defecto y se anota en el ResultadoCarga.
 */
public class ArchivoConfiguracion {
    public static final String NOMBRE_ARCHIVO = "config.properties";

    public static final String CLAVE_NOMBRE = "nombre_complejo";
    public static final String CLAVE_TIENDA = "modulo_tienda";
    public static final String CLAVE_LIMPIEZA = "minutos_limpieza";
    public static final String CLAVE_ANTICIPO = "porcentaje_anticipo_minimo";
    public static final String CLAVE_DESARROLLADOR = "clave_desarrollador";
    public static final String PREFIJO_COLOR = "color.";

    private ArchivoConfiguracion() {
        // No se instancia: solo métodos estáticos.
    }

    // =====================================================================
    //  LECTURA
    // =====================================================================

    /**
     * Lee la configuración. Si el archivo no existe, devuelve la configuración
     * por defecto (tienda activa, sin limpieza, sin anticipo mínimo).
     *
     * @param archivo   ruta de config.properties
     * @param resultado bitácora donde se anotan problemas
     * @return configuración lista para usar (nunca null)
     */
    public static ConfiguracionSistema leer(Path archivo, ResultadoCarga resultado) {
        ConfiguracionSistema config = new ConfiguracionSistema();
        String nombreArchivo = archivo.getFileName().toString();

        if (!Files.exists(archivo)) {
            resultado.registrarAdvertencia(nombreArchivo, 0,
                    "No existe; se usa la configuración por defecto.");
            return config;
        }

        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException | IllegalArgumentException e) {
            resultado.registrarError(nombreArchivo, 0,
                    "No se pudo leer (" + e.getMessage() + "); se usa la configuración por defecto.");
            return config;
        }

        // Cada clave se procesa por separado: si una falla, las demás se cargan igual.
        String nombre = obtener(propiedades, CLAVE_NOMBRE, nombreArchivo, resultado);
        if (nombre != null) {
            try {
                config.setNombreComplejo(nombre);
            } catch (DatoInvalidoException e) {
                reportarInvalido(CLAVE_NOMBRE, e, nombreArchivo, resultado);
            }
        }

        String tienda = obtener(propiedades, CLAVE_TIENDA, nombreArchivo, resultado);
        if (tienda != null) {
            try {
                config.setModuloTiendaActivo(Validador.leerBooleano(tienda, CLAVE_TIENDA));
            } catch (DatoInvalidoException e) {
                reportarInvalido(CLAVE_TIENDA, e, nombreArchivo, resultado);
            }
        }

        String limpieza = obtener(propiedades, CLAVE_LIMPIEZA, nombreArchivo, resultado);
        if (limpieza != null) {
            try {
                config.setMinutosLimpieza(Validador.leerEntero(limpieza, CLAVE_LIMPIEZA));
            } catch (DatoInvalidoException e) {
                reportarInvalido(CLAVE_LIMPIEZA, e, nombreArchivo, resultado);
            }
        }

        String anticipo = obtener(propiedades, CLAVE_ANTICIPO, nombreArchivo, resultado);
        if (anticipo != null) {
            try {
                config.setPorcentajeAnticipoMinimo(leerDecimal(anticipo, CLAVE_ANTICIPO));
            } catch (DatoInvalidoException e) {
                reportarInvalido(CLAVE_ANTICIPO, e, nombreArchivo, resultado);
            }
        }

        // Claves OPCIONALES: si faltan no hay advertencia (se usan los valores de fábrica).
        String claveDev = propiedades.getProperty(CLAVE_DESARROLLADOR);
        if (claveDev != null && !claveDev.isBlank()) {
            try {
                config.setClaveDesarrolladorHash(claveDev.trim());
            } catch (DatoInvalidoException e) {
                reportarInvalido(CLAVE_DESARROLLADOR, e, nombreArchivo, resultado);
            }
        }
        // Cada color se valida por separado: uno malo no impide cargar los demás.
        for (String clave : new TreeSet<>(propiedades.stringPropertyNames())) {
            if (clave.startsWith(PREFIJO_COLOR)) {
                try {
                    config.setColor(clave, propiedades.getProperty(clave).trim());
                } catch (DatoInvalidoException e) {
                    reportarInvalido(clave, e, nombreArchivo, resultado);
                }
            }
        }

        resultado.registrarCarga(nombreArchivo);
        return config;
    }

    // =====================================================================
    //  ESCRITURA
    // =====================================================================

    /**
     * Guarda la configuración actual. Escribe primero un temporal y luego lo
     * renombra, igual que ArchivoCsv, para no dañar el archivo si algo falla.
     *
     * @throws IOException si no se puede escribir
     */
    public static void guardar(ConfiguracionSistema config, Path archivo) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("# Configuración de CanchaPro (editable sin recompilar)");
        lineas.add("# modulo_tienda: true = mostrar Tienda/Cafetería, false = ocultarla");
        lineas.add("# minutos_limpieza: minutos libres obligatorios entre reservas (0 a 120)");
        lineas.add("# porcentaje_anticipo_minimo: % del total requerido para confirmar (0 a 100)");
        lineas.add(CLAVE_NOMBRE + "=" + escapar(config.getNombreComplejo()));
        lineas.add(CLAVE_TIENDA + "=" + config.isModuloTiendaActivo());
        lineas.add(CLAVE_LIMPIEZA + "=" + config.getMinutosLimpieza());
        lineas.add(CLAVE_ANTICIPO + "=" + formatearDecimal(config.getPorcentajeAnticipoMinimo()));
        lineas.add("# clave_desarrollador: hash SHA-256 de la clave del panel oculto (Ctrl+Shift+D)");
        lineas.add(CLAVE_DESARROLLADOR + "=" + config.getClaveDesarrolladorHash());
        if (!config.getColores().isEmpty()) {
            lineas.add("# Colores de la interfaz (#RRGGBB). Si se borran, se usan los de fábrica.");
            for (Map.Entry<String, String> color : config.getColores().entrySet()) {
                lineas.add(color.getKey() + "=" + color.getValue());
            }
        }

        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.write(temporal, lineas, StandardCharsets.UTF_8);
        try {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    /** Devuelve el valor de la clave, o null (con advertencia) si falta o está vacía. */
    private static String obtener(Properties propiedades, String clave, String archivo, ResultadoCarga resultado) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            resultado.registrarAdvertencia(archivo, 0,
                    "Falta '" + clave + "'; se usa el valor por defecto.");
            return null;
        }
        return valor.trim();
    }

    private static void reportarInvalido(String clave, DatoInvalidoException e, String archivo,
                                         ResultadoCarga resultado) {
        resultado.registrarError(archivo, 0, "Valor inválido en '" + clave + "': "
                + e.getMessage() + " Se usa el valor por defecto.");
    }

    /** Acepta "25", "25.5" o "25,5". */
    private static double leerDecimal(String texto, String clave) throws DatoInvalidoException {
        try {
            double valor = Double.parseDouble(texto.replace(',', '.'));
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new NumberFormatException();
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo '" + clave + "' debe ser numérico: '" + texto + "'.", e);
        }
    }

    /** 25.0 -> "25"; 12.5 -> "12.5" (siempre con punto decimal). */
    private static String formatearDecimal(double valor) {
        if (valor == Math.floor(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor); // Double.toString siempre usa punto decimal
    }

    /**
     * En un .properties la barra invertida es carácter de escape; se duplica para
     * que se lea igual. Los saltos de línea se reemplazan por espacios.
     */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\r", " ").replace("\n", " ");
    }
}

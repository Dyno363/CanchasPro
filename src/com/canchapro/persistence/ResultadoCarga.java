/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author SANTANA
 */
/**
 * Bitácora de lo que pasó al cargar los archivos CSV.
 *
 * Prueba #11 del PDF: un archivo con un registro incorrecto debe producir un
 * error controlado. Por eso el cargador NO se detiene en la primera
 * línea mala: la anota aquí, la salta y sigue con las demás. Al final, el
 * sistema puede mostrar al operador un resumen como:
 *
 * 
 *   Carga completada: 38 registros, 1 error, 0 advertencias.
 *   [ERROR] reservas.csv, línea 4: Fecha y hora inválida: '2026-13-40T10:00'.
 * 
 *
 * Diferencia entre error y advertencia:
 * 
 *  Error: se perdió un registro (la línea no se pudo cargar).
 *  Advertencia: algo raro que no hizo perder datos
 *       (ej. un archivo opcional que no existe).
 * 
 */
public class ResultadoCarga {
    /** Registros cargados correctamente por archivo, en el orden en que se leyeron. */
    private final Map<String, Integer> cargadosPorArchivo;
    private final List<Incidencia> errores;
    private final List<Incidencia> advertencias;

    public ResultadoCarga() {
        this.cargadosPorArchivo = new LinkedHashMap<>();
        this.errores = new ArrayList<>();
        this.advertencias = new ArrayList<>();
    }

    // =====================================================================
    //  REGISTRO
    // =====================================================================

    /** Suma un registro cargado correctamente para el archivo indicado. */
    public void registrarCarga(String archivo) {
        cargadosPorArchivo.merge(archivo, 1, Integer::sum);
    }

    /**
     * Anota una línea que no se pudo cargar.
     *
     * @param archivo nombre del archivo (ej. "reservas.csv")
     * @param linea   número de línea en el archivo (1 = encabezado); 0 si aplica a todo el archivo
     * @param mensaje motivo del error, pensado para el operador
     */
    public void registrarError(String archivo, int linea, String mensaje) {
        errores.add(new Incidencia(archivo, linea, mensaje));
    }

    /** Anota algo inusual que no hizo perder datos. */
    public void registrarAdvertencia(String archivo, int linea, String mensaje) {
        advertencias.add(new Incidencia(archivo, linea, mensaje));
    }

    // =====================================================================
    //  CONSULTA
    // =====================================================================

    public boolean hayErrores() {
        return !errores.isEmpty();
    }

    public boolean hayAdvertencias() {
        return !advertencias.isEmpty();
    }

    public List<Incidencia> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    public List<Incidencia> getAdvertencias() {
        return Collections.unmodifiableList(advertencias);
    }

    public int getCargados(String archivo) {
        return cargadosPorArchivo.getOrDefault(archivo, 0);
    }

    public int getTotalCargados() {
        int total = 0;
        for (int cantidad : cargadosPorArchivo.values()) {
            total += cantidad;
        }
        return total;
    }

    /** Ej.: "Carga completada: 38 registros, 1 error, 0 advertencias." */
    public String getResumen() {
        return "Carga completada: " + getTotalCargados() + " registros, "
                + errores.size() + (errores.size() == 1 ? " error, " : " errores, ")
                + advertencias.size() + (advertencias.size() == 1 ? " advertencia." : " advertencias.");
    }

    /** Resumen + detalle por archivo + cada incidencia, una por línea. Útil para consola o un diálogo. */
    public String getDetalle() {
        StringBuilder sb = new StringBuilder(getResumen()).append(System.lineSeparator());
        for (Map.Entry<String, Integer> entrada : cargadosPorArchivo.entrySet()) {
            sb.append("  ").append(entrada.getKey()).append(": ")
              .append(entrada.getValue()).append(" registros").append(System.lineSeparator());
        }
        for (Incidencia error : errores) {
            sb.append("[ERROR] ").append(error).append(System.lineSeparator());
        }
        for (Incidencia advertencia : advertencias) {
            sb.append("[AVISO] ").append(advertencia).append(System.lineSeparator());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getResumen();
    }

    // =====================================================================
    //  CLASE ANIDADA
    // =====================================================================

    /** Un error o advertencia: en qué archivo, en qué línea y qué pasó. Inmutable. */
    public static final class Incidencia {

        private final String archivo;
        private final int linea;
        private final String mensaje;

        public Incidencia(String archivo, int linea, String mensaje) {
            this.archivo = archivo;
            this.linea = linea;
            this.mensaje = mensaje;
        }

        public String getArchivo() {
            return archivo;
        }

        public int getLinea() {
            return linea;
        }

        public String getMensaje() {
            return mensaje;
        }

        /** Ej.: "reservas.csv, línea 4: Fecha y hora inválida." */
        @Override
        public String toString() {
            return archivo + (linea > 0 ? ", línea " + linea : "") + ": " + mensaje;
        }
    }
    
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.persistence;
import com.canchapro.models.exceptions.DatoInvalidoException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author SANTANA
 */
/**
 * Lectura y escritura de BAJO NIVEL de archivos CSV.
 *
 * Esta clase solo sabe de "líneas" y "campos" (texto). No conoce clientes ni
 * reservas: convertir texto en objetos es trabajo del CargadorDatos, y objetos
 * en texto del GuardadorDatos. Así, si mañana cambia el formato (por ejemplo,
 * a otro separador), solo se toca este archivo.
 *
 * Formato de CanchaPro:
 * 
 *   Separador: punto y coma ({@code ;}).
 *   Codificación: UTF-8 (se acepta y descarta el BOM que agrega el Bloc de notas).
 *   Primera línea: encabezado con los nombres de columna.
 *   Líneas en blanco: se ignoran.
 * 
 */
public class ArchivoCsv {
    public static final String SEPARADOR = ";";

    /** Marca invisible que el Bloc de notas de Windows pone al inicio de algunos archivos UTF-8. */
    private static final char BOM = '\uFEFF';

    private ArchivoCsv() {
        // No se instancia: solo métodos estáticos.
    }

    // =====================================================================
    //  LECTURA
    // =====================================================================

    /**
     * Lee un CSV y devuelve sus líneas de datos (sin el encabezado).
     *
     * Nunca lanza excepción: todo problema se anota en {@code resultado} y la
     * lectura continúa. Así un archivo dañado no impide que el sistema arranque.
     *
     * 
     *   Archivo inexistente → advertencia y lista vacía.
     *   Archivo vacío → advertencia y lista vacía.
     *   Archivo solo con encabezado → lista vacía (es válido: aún no hay registros).
     *   Encabezado con otra cantidad de columnas → advertencia.
     *   Línea con cantidad de columnas incorrecta → error, se salta la línea.
     * 
     *
     * @param archivo   ruta del archivo
     * @param columnas  cantidad de columnas esperada en cada línea
     * @param resultado bitácora donde se anotan errores y advertencias
     * @return líneas válidas, cada una con su número de línea original
     */
    public static List<Linea> leer(Path archivo, int columnas, ResultadoCarga resultado) {
        List<Linea> lineas = new ArrayList<>();
        String nombre = archivo.getFileName().toString();

        if (!Files.exists(archivo)) {
            resultado.registrarAdvertencia(nombre, 0, "El archivo no existe; se inicia sin registros.");
            return lineas;
        }

        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String texto;
            int numero = 0;
            boolean encabezadoLeido = false;

            while ((texto = lector.readLine()) != null) {
                numero++;
                if (numero == 1 && !texto.isEmpty() && texto.charAt(0) == BOM) {
                    texto = texto.substring(1);
                }
                if (texto.isBlank()) {
                    continue;
                }
                String[] campos = dividir(texto);

                if (!encabezadoLeido) {
                    encabezadoLeido = true;
                    if (campos.length != columnas) {
                        resultado.registrarAdvertencia(nombre, numero, "El encabezado tiene " + campos.length
                                + " columnas y se esperaban " + columnas + ".");
                    }
                    continue;
                }

                if (campos.length != columnas) {
                    resultado.registrarError(nombre, numero, "Se esperaban " + columnas
                            + " columnas y hay " + campos.length + ". Línea ignorada.");
                    continue;
                }
                lineas.add(new Linea(nombre, numero, campos));
            }
            if (!encabezadoLeido) {
                resultado.registrarAdvertencia(nombre, 0, "El archivo está vacío; se inicia sin registros.");
            }
        } catch (IOException e) {
            resultado.registrarError(nombre, 0, "No se pudo leer el archivo: " + e.getMessage());
        }
        return lineas;
    }

    /**
     * Divide una línea por el separador, conservando los campos vacíos del final.
     * Ej.: "R0002;CL0001;;" → ["R0002", "CL0001", "", ""]
     */
    static String[] dividir(String linea) {
        String[] campos = linea.split(SEPARADOR, -1);
        for (int i = 0; i < campos.length; i++) {
            campos[i] = campos[i].trim();
        }
        return campos;
    }

    // =====================================================================
    //  ESCRITURA
    // =====================================================================

    /**
     * Escribe un CSV completo: encabezado + filas. Reemplaza el archivo anterior.
     *
     * Escritura segura: primero escribe en un archivo temporal ({@code .tmp})
     * y solo al terminar lo renombra al nombre definitivo. Si el programa falla a
     * la mitad, el archivo original queda intacto.
     *
     * @param archivo    ruta destino
     * @param encabezado nombres de columna
     * @param filas      cada fila es un arreglo de campos (ya convertidos a texto)
     * @throws IOException si no se puede escribir
     */
    public static void escribir(Path archivo, String[] encabezado, List<String[]> filas) throws IOException {
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");

        try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
            escritor.write(unir(encabezado));
            escritor.newLine();
            for (String[] fila : filas) {
                escritor.write(unir(fila));
                escritor.newLine();
            }
        }

        try {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Une los campos con el separador, limpiando cada uno. */
    static String unir(String[] campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(limpiar(campos[i]));
        }
        return sb.toString();
    }

    /**
     * Prepara un texto para guardarlo en una celda del CSV:
     * null → "", el separador ";" → "," y los saltos de línea → espacio.
     *
     * Sin esto, un motivo de cancelación como "Lluvia; cancha mojada"
     * partiría la línea en una columna de más y se perdería al recargar.
     */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(SEPARADOR, ",")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    // =====================================================================
    //  CLASE ANIDADA: una línea de datos leída
    // =====================================================================

    /**
     * Una línea de datos ya dividida en campos, que recuerda de qué archivo
     * y de qué número de línea vino (para mensajes de error precisos).
     */
    public static final class Linea {

        private final String archivo;
        private final int numero;
        private final String[] campos;

        Linea(String archivo, int numero, String[] campos) {
            this.archivo = archivo;
            this.numero = numero;
            this.campos = campos;
        }

        public String getArchivo() {
            return archivo;
        }

        /** Número de línea en el archivo (la 1 es el encabezado). */
        public int getNumero() {
            return numero;
        }

        public int cantidadCampos() {
            return campos.length;
        }

        /** @return el campo en la posición indicada (puede ser "" si está vacío) */
        public String get(int indice) {
            return campos[indice];
        }

        /**
         * Igual que {@link #get}, pero exige que no esté vacío.
         *
         * @param indice posición de la columna
         * @param nombre nombre de la columna para el mensaje (ej. "codigoCliente")
         * @throws DatoInvalidoException si el campo está vacío
         */
        public String getRequerido(int indice, String nombre) throws DatoInvalidoException {
            String valor = campos[indice];
            if (valor.isEmpty()) {
                throw new DatoInvalidoException("La columna '" + nombre + "' está vacía.");
            }
            return valor;
        }

        /** @return true si el campo está vacío (columna opcional sin valor) */
        public boolean estaVacio(int indice) {
            return campos[indice].isEmpty();
        }

        @Override
        public String toString() {
            return archivo + ":" + numero + " " + String.join(SEPARADOR, campos);
        }
    }
}

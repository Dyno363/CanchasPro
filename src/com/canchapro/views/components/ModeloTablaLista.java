/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.components;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;
/**
 *
 * Modelo de tabla GENÉRICO de solo lectura: sirve para cualquier lista de objetos.
 *
 * En vez de escribir un AbstractTableModel por cada pantalla, se le dice:
 * qué columnas hay, de qué tipo es cada una (para ordenar bien) y cómo convertir
 * un objeto en una fila. Ejemplo:
 *
 * 
 * new ModeloTablaLista&lt;Cancha&gt;(
 *     new String[]{"Código", "Nombre"}, new Class&lt;?&gt;[]{String.class, String.class},
 *     c -&gt; new Object[]{c.getCodigo(), c.getNombre()});
 * 
 *
 * @param <T> tipo de objeto de cada fila (Cancha, Tarifa, ServicioAdicional…)
 *
 * @author SANTANA
 */
public class ModeloTablaLista<T> extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private final String[] columnas;
    private final Class<?>[] clases;
    private final transient Function<T, Object[]> aFila;
    private transient List<T> elementos = new ArrayList<>();

    /**
     * @param columnas títulos de las columnas
     * @param clases   tipo de cada columna (String, Integer, Double, enum…) para ordenar bien
     * @param aFila    cómo se convierte un objeto en los valores de su fila
     */
    public ModeloTablaLista(String[] columnas, Class<?>[] clases, Function<T, Object[]> aFila) {
        if (columnas.length != clases.length) {
            throw new IllegalArgumentException("Debe haber un tipo por cada columna.");
        }
        this.columnas = columnas.clone();
        this.clases = clases.clone();
        this.aFila = aFila;
    }

    public void setElementos(List<T> nuevos) {
        elementos = new ArrayList<>(nuevos);
        fireTableDataChanged();
    }

    /** @param filaModelo índice en el MODELO (usar convertRowIndexToModel si la tabla está ordenada) */
    public T getElementoEn(int filaModelo) {
        return elementos.get(filaModelo);
    }

    @Override
    public int getRowCount() {
        return elementos.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int columna) {
        return columnas[columna];
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        return clases[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return aFila.apply(elementos.get(fila))[columna];
    }
}

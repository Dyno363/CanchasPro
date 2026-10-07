/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.personas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */



/**
 * Superclase abstracta de toda persona registrada en el sistema.
 *
 * Agrupa lo que Cliente y Usuario tienen en común: código, nombre,
 * teléfono y la búsqueda por criterio. Es abstracta porque en el negocio
 * no existe "una persona" a secas: siempre es un cliente o un operador.
 *
 * Regla de diseño: el constructor y los setters validan los datos, así
 * que nunca puede existir en memoria una Persona con datos inválidos.
 */
public abstract class Persona {

    private final String codigo;
    private String nombre;
    private String telefono;

    /**
     * @param codigo   identificador único (no cambia nunca)
     * @param nombre   nombre completo
     * @param telefono 8 dígitos; se aceptan guiones o espacios ("4411-2233")
     * @throws DatoInvalidoException si algún dato está vacío o tiene formato inválido
     */
    protected Persona(String codigo, String nombre, String telefono) throws DatoInvalidoException {
        this.codigo = Validador.validarRequerido(codigo, "código");
        setNombre(nombre);
        setTelefono(telefono);
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) throws DatoInvalidoException {
        this.nombre = Validador.validarRequerido(nombre, "nombre");
    }

    public String getTelefono() {
        return telefono;
    }

    public final void setTelefono(String telefono) throws DatoInvalidoException {
        this.telefono = Validador.validarTelefono(telefono);
    }

    // =====================================================================
    //  LÓGICA
    // =====================================================================

    /**
     * Indica si esta persona coincide con un texto de búsqueda.
     * Busca en código, nombre (sin importar tildes ni mayúsculas) y teléfono.
     *
     * Se usa antes de registrar un cliente nuevo para evitar duplicados.
     * Un criterio vacío coincide con todos (muestra la lista completa).
     *
     * @param criterio texto escrito por el operador
     * @return true si coincide en alguno de los tres campos
     */
    public boolean coincideCon(String criterio) {
        if (Validador.esVacio(criterio)) {
            return true;
        }
        String digitos = Validador.normalizarTelefono(criterio);
        return Validador.contiene(codigo, criterio)
                || Validador.contiene(nombre, criterio)
                || (!digitos.isEmpty() && telefono.contains(digitos));
    }

    /**
     * Descripción breve según el tipo de persona. Cada subclase la define
     * a su manera (polimorfismo).
     */
    public abstract String getDescripcion();

    // =====================================================================
    //  IDENTIDAD: dos personas son la misma si tienen el mismo código.
    // =====================================================================

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        return codigo.equals(((Persona) otro).codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Texto para combos y listas: "CL0001 - Carlos Pérez". */
    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}

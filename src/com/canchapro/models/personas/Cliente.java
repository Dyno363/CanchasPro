/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.personas;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
import java.time.LocalDate;
/**
 *
 * @author SANTANA
 */



/**
 * Cliente del complejo deportivo: quien reserva y paga las canchas.
 *
 * Hereda de Persona el código, nombre y teléfono, y agrega el correo
 * (opcional) y la fecha en que se registró.
 */
public class Cliente extends Persona {

    /** Correo electrónico. Vacío ("") si el cliente no lo proporcionó. */
    private String correo;
    private final LocalDate fechaRegistro;

    /**
     * @param codigo        código único, ej. "CL0001"
     * @param nombre        nombre completo
     * @param telefono      8 dígitos
     * @param correo        opcional: puede ser vacío o null
     * @param fechaRegistro fecha de alta; no puede ser futura
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public Cliente(String codigo, String nombre, String telefono,
                   String correo, LocalDate fechaRegistro) throws DatoInvalidoException {
        super(codigo, nombre, telefono);
        setCorreo(correo);
        if (fechaRegistro == null) {
            throw new DatoInvalidoException("La fecha de registro es obligatoria.");
        }
        if (fechaRegistro.isAfter(LocalDate.now())) {
            throw new DatoInvalidoException("La fecha de registro no puede ser futura.");
        }
        this.fechaRegistro = fechaRegistro;
    }

    public String getCorreo() {
        return correo;
    }

    public final void setCorreo(String correo) throws DatoInvalidoException {
        this.correo = Validador.validarCorreoOpcional(correo);
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    /** @return true si el cliente registró un correo electrónico */
    public boolean tieneCorreo() {
        return !correo.isEmpty();
    }

    /** Ejemplo: "Cliente Carlos Pérez - Tel. 44112233" */
    @Override
    public String getDescripcion() {
        return "Cliente " + getNombre() + " - Tel. " + getTelefono();
    }
}

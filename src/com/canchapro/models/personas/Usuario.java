/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.models.personas;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.utils.Validador;
import java.util.Locale;
import java.util.regex.Pattern;
/**
 *
 * @author SANTANA
 */



/**
 * Operador del sistema: la persona que inicia sesión y registra reservas,
 * pagos y ventas.
 *
 * La contraseña nunca se guarda en texto plano: este objeto solo conoce
 * su hash SHA-256 (ver utils.Seguridad).
 *
 * Un usuario no se elimina: se desactiva. Así se conserva quién creó o
 * canceló cada reserva en el historial.
 */
public class Usuario extends Persona {

    /** 3 a 20 caracteres: letras, números, punto o guion bajo. */
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,20}$");

    /** Un hash SHA-256 en hexadecimal tiene exactamente 64 caracteres. */
    private static final Pattern HASH_SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private final String username;
    private String passwordHash;
    private RolUsuario rol;
    private boolean activo;

    /**
     * Crea un usuario activo.
     *
     * @param codigo       código único, ej. "U001"
     * @param nombre       nombre completo
     * @param telefono     8 dígitos
     * @param username     nombre de inicio de sesión (se guarda en minúsculas)
     * @param passwordHash hash SHA-256 de la contraseña (64 caracteres hexadecimales)
     * @param rol          ADMINISTRADOR u OPERADOR
     * @throws DatoInvalidoException si algún dato es inválido
     */
    public Usuario(String codigo, String nombre, String telefono,
                   String username, String passwordHash, RolUsuario rol) throws DatoInvalidoException {
        super(codigo, nombre, telefono);
        this.username = validarUsername(username);
        cambiarPasswordHash(passwordHash);
        setRol(rol);
        this.activo = true;
    }

    // =====================================================================
    //  GETTERS Y SETTERS
    // =====================================================================

    public String getUsername() {
        return username;
    }

    /** Solo para persistencia (guardar en usuarios.csv). */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Reemplaza la contraseña. Recibe el hash ya calculado, nunca el texto plano.
     */
    public final void cambiarPasswordHash(String nuevoHash) throws DatoInvalidoException {
        String hash = Validador.validarRequerido(nuevoHash, "contraseña").toLowerCase(Locale.ROOT);
        if (!HASH_SHA256.matcher(hash).matches()) {
            throw new DatoInvalidoException("El hash de la contraseña no es un SHA-256 válido.");
        }
        this.passwordHash = hash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public final void setRol(RolUsuario rol) throws DatoInvalidoException {
        if (rol == null) {
            throw new DatoInvalidoException("El rol del usuario es obligatorio.");
        }
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // =====================================================================
    //  LÓGICA
    // =====================================================================

    /**
     * Verifica un intento de inicio de sesión.
     *
     * Solo es válido si el usuario está activo, el nombre de usuario
     * coincide (sin importar mayúsculas) y el hash coincide.
     *
     * @param username     lo que el operador escribió como usuario
     * @param passwordHash hash de lo que el operador escribió como contraseña
     * @return true si las credenciales son correctas
     */
    public boolean validarCredenciales(String username, String passwordHash) {
        if (!activo || username == null || passwordHash == null) {
            return false;
        }
        return this.username.equals(username.trim().toLowerCase(Locale.ROOT))
                && this.passwordHash.equalsIgnoreCase(passwordHash.trim());
    }

    /** @return true si tiene permisos de administración */
    public boolean esAdministrador() {
        return rol == RolUsuario.ADMINISTRADOR;
    }

    /** Ejemplo: "Administrador Administrador General (admin)" */
    @Override
    public String getDescripcion() {
        return rol.getDescripcion() + " " + getNombre() + " (" + username + ")";
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private static String validarUsername(String username) throws DatoInvalidoException {
        String valor = Validador.validarRequerido(username, "usuario").toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(valor).matches()) {
            throw new DatoInvalidoException(
                    "Usuario inválido: use de 3 a 20 letras, números, punto o guion bajo, sin espacios.");
        }
        return valor;
    }
}

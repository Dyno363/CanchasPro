/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.controllers;
import com.canchapro.models.ComplejoDeportivo;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.exceptions.DatoInvalidoException;
import com.canchapro.models.exceptions.EntidadNoEncontradaException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.GeneradorCodigo;
import com.canchapro.utils.Seguridad;
import com.canchapro.utils.Validador;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/**
 *
 * @author SANTANA
 */
/**
 * Controlador del módulo Usuarios (operadores del sistema).
 *
 * Permisos: gestionar usuarios es exclusivo del ADMINISTRADOR; cada
 * método protegido empieza con {@code sesion.exigirAdministrador(...)}.
 * La única excepción es {@link #cambiarMiContrasena}, que cualquiera puede usar
 * para su propia cuenta.
 *
 * Reglas para no dejar el sistema sin acceso:
 * 
 *   Nadie puede desactivarse a sí mismo ni quitarse el rol de administrador.
 *   Siempre debe quedar al menos un administrador activo.
 *   Los usuarios no se eliminan, se desactivan (conservan el historial de
 *       quién registró o canceló cada reserva).
 * 
 */
public class UsuarioController {
    /** Longitud mínima de contraseña. */
    public static final int MIN_CONTRASENA = 6;

    private final ComplejoDeportivo complejo;
    private final Sesion sesion;

    /**
     * @param complejo sistema en memoria
     * @param sesion   sesión del usuario que está operando
     */
    public UsuarioController(ComplejoDeportivo complejo, Sesion sesion) {
        this.complejo = Objects.requireNonNull(complejo, "El complejo es obligatorio.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
    }

    // =====================================================================
    //  CONSULTAS
    // =====================================================================

    /** La vista lo usa para mostrar u ocultar el botón "Usuarios" del menú. */
    public boolean puedeGestionarUsuarios() {
        return sesion.esAdministrador();
    }

    /** @return todos los usuarios (activos e inactivos), en orden de código */
    public List<Usuario> listar() throws CanchaProException {
        sesion.exigirAdministrador("ver la lista de usuarios");
        return new ArrayList<>(complejo.getUsuarios());
    }

    public Usuario obtener(String codigo) throws EntidadNoEncontradaException {
        return complejo.buscarUsuario(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el usuario " + codigo + "."));
    }

    /** @return los roles para llenar el combo del formulario */
    public RolUsuario[] getRoles() {
        return RolUsuario.values();
    }
        /** @return true si ese código es el del usuario que tiene la sesión abierta (para marcar "tú") */
    public boolean esUsuarioActual(String codigo) {
        return sesion.getUsuario().getCodigo().equals(codigo);
    }
    // =====================================================================
    //  OPERACIONES DEL ADMINISTRADOR
    // =====================================================================

    /**
     * Crea un usuario nuevo (queda activo).
     *
     * @param contrasena   {@code JPasswordField.getPassword()}; se borra al terminar
     * @param confirmacion segunda escritura de la contraseña; se borra al terminar
     * @return el usuario creado
     * @throws CanchaProException si no es administrador, un dato es inválido,
     *                            las contraseñas no coinciden o el username ya existe
     */
    public Usuario crear(String nombre, String telefono, String username, RolUsuario rol,
                         char[] contrasena, char[] confirmacion) throws CanchaProException {
        try {
            sesion.exigirAdministrador("crear usuarios");
            validarContrasenaNueva(contrasena, confirmacion);
            String hash = Seguridad.hashContrasena(contrasena);
            Usuario usuario = new Usuario(complejo.generarCodigo(GeneradorCodigo.Tipo.USUARIO),
                    nombre, telefono, username, hash, rol);
            complejo.agregarUsuario(usuario); // aquí se rechaza un username repetido
            return usuario;
        } finally {
            borrar(contrasena, confirmacion);
        }
    }

    /**
     * Modifica nombre, teléfono y rol. El username no cambia (es la "llave" del login).
     */
    public Usuario actualizar(String codigo, String nombre, String telefono, RolUsuario rol)
            throws CanchaProException {
        sesion.exigirAdministrador("modificar usuarios");
        Usuario usuario = obtener(codigo);

        // 1) Validar todo
        Validador.validarRequerido(nombre, "nombre");
        Validador.validarTelefono(telefono);
        if (rol == null) {
            throw new DatoInvalidoException("El rol del usuario es obligatorio.");
        }
        boolean pierdeAdministracion = usuario.esAdministrador() && rol != RolUsuario.ADMINISTRADOR;
        if (pierdeAdministracion && esUsuarioDeLaSesion(usuario)) {
            throw new CanchaProException("No puede quitarse a sí mismo el rol de administrador.");
        }
        if (pierdeAdministracion && usuario.isActivo()) {
            exigirOtroAdministradorActivo(usuario);
        }

        // 2) Aplicar
        usuario.setNombre(nombre);
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        return usuario;
    }

    /**
     * Activa o desactiva un usuario. Un usuario desactivado no puede iniciar sesión,
     * pero sigue apareciendo en el historial de reservas.
     */
    public void cambiarEstado(String codigo, boolean activo) throws CanchaProException {
        sesion.exigirAdministrador("activar o desactivar usuarios");
        Usuario usuario = obtener(codigo);
        if (!activo) {
            if (esUsuarioDeLaSesion(usuario)) {
                throw new CanchaProException("No puede desactivar su propio usuario.");
            }
            if (usuario.esAdministrador() && usuario.isActivo()) {
                exigirOtroAdministradorActivo(usuario);
            }
        }
        usuario.setActivo(activo);
    }

    /**
     * El administrador asigna una contraseña nueva a otro usuario (por ejemplo,
     * si la olvidó). No necesita conocer la anterior.
     */
    public void restablecerContrasena(String codigo, char[] nueva, char[] confirmacion)
            throws CanchaProException {
        try {
            sesion.exigirAdministrador("restablecer contraseñas");
            Usuario usuario = obtener(codigo);
            validarContrasenaNueva(nueva, confirmacion);
            usuario.cambiarPasswordHash(Seguridad.hashContrasena(nueva));
        } finally {
            borrar(nueva, confirmacion);
        }
    }

    // =====================================================================
    //  OPERACIÓN DE CUALQUIER USUARIO
    // =====================================================================

    /**
     * Cambia la contraseña del usuario que tiene la sesión abierta.
     * Exige la contraseña actual para confirmar que es él.
     */
    public void cambiarMiContrasena(char[] actual, char[] nueva, char[] confirmacion)
            throws CanchaProException {
        try {
            if (actual == null || actual.length == 0) {
                throw new DatoInvalidoException("Ingrese su contraseña actual.");
            }
            Usuario yo = sesion.getUsuario();
            if (!yo.getPasswordHash().equalsIgnoreCase(Seguridad.hashContrasena(actual))) {
                throw new CanchaProException("La contraseña actual no es correcta.");
            }
            validarContrasenaNueva(nueva, confirmacion);
            yo.cambiarPasswordHash(Seguridad.hashContrasena(nueva));
        } finally {
            borrar(actual, nueva, confirmacion);
        }
    }

    // =====================================================================
    //  APOYO INTERNO
    // =====================================================================

    private boolean esUsuarioDeLaSesion(Usuario usuario) {
        return usuario.equals(sesion.getUsuario());
    }

    /** Garantiza que, si "saliente" deja de ser admin activo, quede al menos otro. */
    private void exigirOtroAdministradorActivo(Usuario saliente) throws CanchaProException {
        for (Usuario u : complejo.getUsuarios()) {
            if (!u.equals(saliente) && u.esAdministrador() && u.isActivo()) {
                return;
            }
        }
        throw new CanchaProException("Debe quedar al menos un administrador activo en el sistema.");
    }

    /**
     * Política de contraseña: mínimo {@value #MIN_CONTRASENA} caracteres, sin
     * espacios y confirmada. Se revisa sobre char[] para no crear Strings.
     */
    private static void validarContrasenaNueva(char[] contrasena, char[] confirmacion)
            throws DatoInvalidoException {
        if (contrasena == null || contrasena.length == 0) {
            throw new DatoInvalidoException("La contraseña es obligatoria.");
        }
        if (contrasena.length < MIN_CONTRASENA) {
            throw new DatoInvalidoException("La contraseña debe tener al menos "
                    + MIN_CONTRASENA + " caracteres.");
        }
        for (char c : contrasena) {
            if (Character.isWhitespace(c)) {
                throw new DatoInvalidoException("La contraseña no puede contener espacios.");
            }
        }
        if (!Arrays.equals(contrasena, confirmacion)) {
            throw new DatoInvalidoException("La contraseña y su confirmación no coinciden.");
        }
    }

    /** Borra de memoria los arreglos de contraseña recibidos. */
    private static void borrar(char[]... arreglos) {
        for (char[] arreglo : arreglos) {
            if (arreglo != null) {
                Arrays.fill(arreglo, '\0');
            }
        }
    }
}

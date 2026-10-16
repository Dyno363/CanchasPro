/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro.views.panels;
import com.canchapro.controllers.SistemaController;
import com.canchapro.controllers.UsuarioController;
import com.canchapro.models.enums.ColorTema;
import com.canchapro.models.enums.RolUsuario;
import com.canchapro.models.exceptions.CanchaProException;
import com.canchapro.models.personas.Usuario;
import com.canchapro.utils.Tema;
import com.canchapro.views.dialogs.UsuarioDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
/**
 ** Módulo USUARIOS del menú lateral (solo ADMINISTRADOR, construido por código).
 *
 * 
 *  ┌ Usuarios ──────────────────────────────────────────────────────────────┐
 *  │ Personas que pueden iniciar sesión en CanchaPro.                        │
 *  │ 2 usuarios · 2 activos      [+ Nuevo] [Editar] [Desactivar] [Contraseña] │
 *  ├─────────────────────────────────────────────────────────────────────────┤
 *  │ Código │ Nombre               │ Usuario  │ Teléfono │ Rol    │ Estado   │
 *  │ U001   │ Administrador (tú)   │ admin    │ 55550001 │ Admin. │ ACTIVO   │
 *  │ U002   │ Operador Turno Tarde │ operador │ 55550002 │ Oper.  │ ACTIVO   │
 *  ├─────────────────────────────────────────────────────────────────────────┤
 *  │   Un usuario desactivado no puede entrar, pero su nombre sigue en el    │
 *  │   historial. Siempre debe quedar al menos un administrador activo.      │
 *  └─────────────────────────────────────────────────────────────────────────┘
 * 
 *
 * Las reglas (no desactivarse a sí mismo, dejar siempre un administrador
 * activo, política de contraseñas) viven en el {@link UsuarioController}; esta
 * pantalla solo muestra la lista y el mensaje de error si algo no se permite.
 * @author SANTANA
 */
public class UsuariosPanel extends JPanel implements PanelModulo{
    private static final long serialVersionUID = 1L;

    private final transient UsuarioController usuarios;

    private final JLabel lblTitulo = new JLabel("Usuarios");
    private final JLabel lblSubtitulo = new JLabel("Personas que pueden iniciar sesión en CanchaPro.");
    private final JLabel lblConteo = new JLabel(" ");
    private final JButton btnNuevo = new JButton("+ Nuevo usuario");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEstado = new JButton("Desactivar");
    private final JButton btnContrasena = new JButton("Restablecer contraseña");

    private final ModeloUsuarios modelo = new ModeloUsuarios();
    private final JTable tblUsuarios = new JTable();
    private final JScrollPane scrUsuarios = new JScrollPane(tblUsuarios);
    private final JLabel lblReglas = new JLabel("<html> Un usuario <b>desactivado</b> no puede iniciar sesión, "
            + "pero su nombre se conserva en el historial de reservas y pagos.<br>"
            + "Siempre debe quedar al menos un <b>administrador activo</b>, y nadie puede desactivarse a sí mismo.</html>");

    public UsuariosPanel(SistemaController sistema) {
        this.usuarios = sistema.getUsuarioController();
        construir();
        registrarEventos();
        aplicarTema();
        recargar();
    }

    // =====================================================================
    //  CONSTRUCCIÓN
    // =====================================================================

    private void construir() {
        setLayout(new BorderLayout(0, Tema.ESPACIO));
        setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 2));
        titulos.setOpaque(false);
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESPACIO, 0));
        acciones.setOpaque(false);
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnEstado);
        acciones.add(btnContrasena);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(lblConteo, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, Tema.MARGEN));
        norte.setOpaque(false);
        norte.add(titulos, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        modelo.configurar(tblUsuarios);
        add(scrUsuarios, BorderLayout.CENTER);
        add(lblReglas, BorderLayout.SOUTH);
    }

    private void registrarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEstado.addActionListener(e -> cambiarEstado());
        btnContrasena.addActionListener(e -> restablecerContrasena());
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tblUsuarios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && seleccionado() != null) {
                    editar();
                }
            }
        });
    }

    @Override
    public final void aplicarTema() {
        setBackground(Tema.color(ColorTema.FONDO));
        Tema.titulo(lblTitulo);
        Tema.textoSecundario(lblSubtitulo);
        Tema.textoSecundario(lblConteo);
        Tema.botonPrimario(btnNuevo);
        Tema.botonSecundario(btnEditar);
        Tema.botonSecundario(btnEstado);
        Tema.botonSecundario(btnContrasena);
        Tema.tabla(tblUsuarios);
        scrUsuarios.getViewport().setBackground(Tema.color(ColorTema.SUPERFICIE));
        Tema.textoSecundario(lblReglas);
        lblReglas.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, 2, 0, 0));
        repaint();
    }

    // =====================================================================
    //  DATOS
    // =====================================================================

    @Override
    public void recargar() {
        Usuario antes = seleccionado();
        try {
            List<Usuario> lista = usuarios.listar(); // el controlador exige administrador
            modelo.setUsuarios(lista);
            int activos = 0;
            for (Usuario u : lista) {
                if (u.isActivo()) {
                    activos++;
                }
            }
            lblConteo.setText(lista.size() + " usuarios  ·  " + activos + " activos");
        } catch (CanchaProException ex) {
            modelo.setUsuarios(new ArrayList<>());
            lblConteo.setText(ex.getMessage());
        }
        if (antes != null) {
            seleccionar(antes.getCodigo());
        }
        actualizarBotones();
    }

    private Usuario seleccionado() {
        int fila = tblUsuarios.getSelectedRow();
        return fila < 0 ? null : modelo.getUsuarioEn(tblUsuarios.convertRowIndexToModel(fila));
    }

    private void seleccionar(String codigo) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getUsuarioEn(i).getCodigo().equals(codigo)) {
                int fila = tblUsuarios.convertRowIndexToView(i);
                tblUsuarios.setRowSelectionInterval(fila, fila);
                return;
            }
        }
    }

    /** El texto del botón cambia: "Desactivar" para un activo, "Activar" para uno inactivo. */
    private void actualizarBotones() {
        Usuario u = seleccionado();
        btnEditar.setEnabled(u != null);
        btnContrasena.setEnabled(u != null);
        btnEstado.setEnabled(u != null);
        btnEstado.setText(u != null && !u.isActivo() ? "Activar" : "Desactivar");
    }

    // =====================================================================
    //  ACCIONES
    // =====================================================================

    private void nuevo() {
        UsuarioDialog d = new UsuarioDialog(this, usuarios, null);
        d.setVisible(true);
        Usuario nuevo = d.getUsuarioGuardado();
        if (nuevo != null) {
            recargar();
            seleccionar(nuevo.getCodigo());
            JOptionPane.showMessageDialog(this, "Usuario " + nuevo.getUsername() + " creado ("
                    + nuevo.getRol().getDescripcion() + "). Ya puede iniciar sesión.",
                    "Usuarios", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editar() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        UsuarioDialog d = new UsuarioDialog(this, usuarios, u);
        d.setVisible(true);
        if (d.getUsuarioGuardado() != null) {
            recargar();
        }
    }

    /** Activar o desactivar: el controlador decide si se permite. */
    private void cambiarEstado() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        boolean activar = !u.isActivo();
        String pregunta = activar
                ? "¿Activar a " + u.getNombre() + "? Podrá volver a iniciar sesión."
                : "¿Desactivar a " + u.getNombre() + "? Ya no podrá iniciar sesión.";
        if (JOptionPane.showConfirmDialog(this, pregunta, "Usuarios", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            usuarios.cambiarEstado(u.getCodigo(), activar);
            recargar();
        } catch (CanchaProException ex) {
            error(ex); // p. ej. "No puede desactivar su propio usuario."
        }
    }

    /** Pide la contraseña nueva dos veces en un cuadro pequeño y se la entrega al controlador. */
    private void restablecerContrasena() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        JPasswordField pwdNueva = new JPasswordField(18);
        JPasswordField pwdConfirmacion = new JPasswordField(18);
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        formulario.add(new JLabel("Nueva contraseña para " + u.getUsername() + " (mínimo "
                + UsuarioController.MIN_CONTRASENA + " caracteres):"), g);
        g.gridwidth = 1;
        g.gridy = 1;
        formulario.add(new JLabel("Nueva"), g);
        g.gridx = 1;
        formulario.add(pwdNueva, g);
        g.gridx = 0;
        g.gridy = 2;
        formulario.add(new JLabel("Confirmar"), g);
        g.gridx = 1;
        formulario.add(pwdConfirmacion, g);

        int r = JOptionPane.showConfirmDialog(this, formulario, "Restablecer contraseña",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            usuarios.restablecerContrasena(u.getCodigo(), pwdNueva.getPassword(), pwdConfirmacion.getPassword());
            JOptionPane.showMessageDialog(this, "Contraseña de " + u.getUsername() + " actualizada.",
                    "Usuarios", JOptionPane.INFORMATION_MESSAGE);
        } catch (CanchaProException ex) {
            error(ex);
        } finally {
            pwdNueva.setText(""); // no dejar la contraseña escrita en el cuadro
            pwdConfirmacion.setText("");
        }
    }

    private void error(CanchaProException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Usuarios", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  MODELO DE LA TABLA
    // =====================================================================

    /** Columnas: Código, Nombre, Usuario, Teléfono, Rol, Estado. Solo lectura. */
    private final class ModeloUsuarios extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final String[] columnas = {"Código", "Nombre", "Usuario", "Teléfono", "Rol", "Estado"};
        private final int[] anchos = {70, 220, 120, 100, 120, 100};

        private transient List<Usuario> lista = new ArrayList<>();

        void setUsuarios(List<Usuario> nuevos) {
            lista = new ArrayList<>(nuevos);
            fireTableDataChanged();
        }

        Usuario getUsuarioEn(int filaModelo) {
            return lista.get(filaModelo);
        }

        void configurar(JTable tabla) {
            tabla.setModel(this);
            Tema.tabla(tabla);
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.setRowSorter(new TableRowSorter<>(this));
            for (int i = 0; i < anchos.length; i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            }
            tabla.getColumnModel().getColumn(5).setCellRenderer(new RenderizadorEstado());
        }

        @Override
        public int getRowCount() {
            return lista.size();
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
            return columna == 5 ? Boolean.class : columna == 4 ? RolUsuario.class : String.class;
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Usuario u = lista.get(fila);
            switch (columna) {
                case 0:  return u.getCodigo();
                case 1:  return u.getNombre() + (usuarios.esUsuarioActual(u.getCodigo()) ? "  (tú)" : "");
                case 2:  return u.getUsername();
                case 3:  return u.getTelefono();
                case 4:  return u.getRol();
                default: return u.isActivo();
            }
        }
    }

    /** Boolean → píldora "ACTIVO" (verde) o "INACTIVO" (gris). */
    private static final class RenderizadorEstado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            boolean activo = Boolean.TRUE.equals(valor);
            Color fondo = Tema.color(activo ? ColorTema.EXITO : ColorTema.TEXTO_SECUNDARIO);
            l.setText(activo ? "ACTIVO" : "INACTIVO");
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(Tema.FUENTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
            l.setBackground(fondo);
            l.setForeground(Tema.textoSobre(fondo));
            l.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, tabla.getBackground()));
            l.setOpaque(true);
            return l;
        }
    }
}

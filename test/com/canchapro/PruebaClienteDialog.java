/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.canchapro;
import com.canchapro.controllers.SistemaController;
import com.canchapro.models.personas.Cliente;
import com.canchapro.utils.Tema;
import com.canchapro.views.dialogs.ClienteDialog;
import javax.swing.SwingUtilities;

/**
 *
 * @author SANTANA
 */
public class PruebaClienteDialog {
    public static void main(String[] args) throws Exception {
        SistemaController sistema = new SistemaController();
        sistema.iniciar();
        sistema.iniciarSesion("admin", "admin123".toCharArray());

        SwingUtilities.invokeAndWait(() -> {
            Tema.aplicar();
            // 1) Registrar uno nuevo
            ClienteDialog nuevo = new ClienteDialog(null, sistema.getClienteController(), null);
            nuevo.setVisible(true); // modal: espera hasta que se cierre
            Cliente creado = nuevo.getClienteGuardado();
            System.out.println("Registrado: " + (creado == null ? "(cancelado)" : creado));

            // 2) Editar el que se acaba de crear
            if (creado != null) {
                ClienteDialog editar = new ClienteDialog(null, sistema.getClienteController(), creado);
                editar.setVisible(true);
                System.out.println("Editado: " + (editar.getClienteGuardado() == null
                        ? "(cancelado)" : editar.getClienteGuardado()));
            }
            System.out.println("Total de clientes en memoria: " + sistema.getClienteController().listar().size());
        });
        System.exit(0); // sin guardar: los CSV no cambian
    }
}

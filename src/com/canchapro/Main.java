/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.canchapro;

import com.canchapro.controllers.SistemaController;
import com.canchapro.persistence.ResultadoCarga;
import com.canchapro.utils.Tema;
import com.canchapro.views.LoginView;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
/**
 *
 * @author SANTANA
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // 1. Cargar los CSV en memoria
        SistemaController sistema = new SistemaController();
        ResultadoCarga carga = sistema.iniciar();

        // 2. Swing se maneja SIEMPRE desde su propio hilo (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            // 3. Colores del desarrollador + estilo general, ANTES de crear ventanas
            Tema.cargarColores(sistema.getDesarrolladorController().getColores());
            Tema.aplicar();

            // 4. Si algún CSV tenía líneas dañadas, avisar (pero el sistema arranca igual)
            if (carga.hayErrores()) {
                JOptionPane.showMessageDialog(null, carga.getResumen()
                        + "\nLas líneas con error se omitieron. Revise Configuración para ver el detalle.",
                        "CanchaPro - Carga de datos", JOptionPane.WARNING_MESSAGE);
            }

            // 5. Mostrar el login
            new LoginView(sistema).setVisible(true);
        });
        }
}
  

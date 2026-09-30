package com.mycompany.gestordeencuestas;

import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada y paleta visual de la aplicacion. */
public class GestorDeEncuestas {

    public static final Color COLOR_FONDO = new Color(242, 246, 243);
    public static final Color COLOR_SUPERFICIE = Color.WHITE;
    public static final Color COLOR_PRIMARIO = new Color(28, 77, 66);
    public static final Color COLOR_ACENTO = new Color(197, 91, 54);
    public static final Color COLOR_TEXTO = new Color(36, 49, 45);
    public static final Color COLOR_SECUNDARIO = new Color(103, 121, 113);

    private GestorDeEncuestas() {
    }

    public static void main(String[] args) {
        UIManager.put("TabbedPane.selected", COLOR_SUPERFICIE);
        SwingUtilities.invokeLater(() -> new EncuestaGUI().setVisible(true));
    }
}

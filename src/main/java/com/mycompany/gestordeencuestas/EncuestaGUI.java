package com.mycompany.gestordeencuestas;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Ventana para crear preguntas, capturar respuestas y verificar la encuesta. */
public final class EncuestaGUI extends JFrame {

    private final List<Pregunta> preguntas = new ArrayList<>();
    private final Respuestas respuestas = new Respuestas();
    private final DefaultListModel<String> modeloPreguntas = new DefaultListModel<>();
    private final JTextField campoPregunta = new JTextField();
    private final JTextField campoOpciones = new JTextField();
    private final JPanel panelRespuestas = new JPanel();
    private final JTextArea informe = new JTextArea();
    private final JLabel estadoRespuestas = new JLabel("Aun no hay respuestas guardadas.");
    private final List<JComboBox<String>> selectores = new ArrayList<>();

    public EncuestaGUI() {
        super("Gestor de encuestas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(980, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(GestorDeEncuestas.COLOR_FONDO);
        setLayout(new BorderLayout());

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearContenido(), BorderLayout.CENTER);
        actualizarPanelRespuestas();
        actualizarInforme();
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(GestorDeEncuestas.COLOR_PRIMARIO);
        encabezado.setBorder(new EmptyBorder(22, 30, 22, 30));

        JLabel titulo = new JLabel("GESTOR DE ENCUESTAS");
        titulo.setForeground(GestorDeEncuestas.COLOR_SUPERFICIE);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        JLabel subtitulo = new JLabel("Preguntas claras. Respuestas verificables.");
        subtitulo.setForeground(new java.awt.Color(218, 231, 224));
        subtitulo.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel textos = new JPanel(new GridLayout(0, 1));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);
        encabezado.add(textos, BorderLayout.CENTER);
        return encabezado;
    }

    private JTabbedPane crearContenido() {
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setBackground(GestorDeEncuestas.COLOR_FONDO);
        pestanas.setForeground(GestorDeEncuestas.COLOR_TEXTO);
        pestanas.setBorder(new EmptyBorder(14, 18, 18, 18));
        pestanas.addTab("Preguntas", crearPanelPreguntas());
        pestanas.addTab("Respuestas", crearPanelRespuestas());
        pestanas.addTab("Verificar", crearPanelVerificacion());
        return pestanas;
    }

    private JPanel crearPanelPreguntas() {
        JPanel pagina = crearPagina();
        pagina.setLayout(new BorderLayout(12, 12));

        JPanel formulario = crearSuperficie();
        formulario.setLayout(new BorderLayout(8, 8));
        formulario.add(etiqueta("Nueva pregunta"), BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 8));
        campos.setOpaque(false);
        campoPregunta.setToolTipText("Ejemplo: Que actividad prefieres?");
        campoOpciones.setToolTipText("Escribe al menos dos opciones separadas por comas");
        campos.add(campoPregunta);
        campos.add(campoOpciones);
        formulario.add(campos, BorderLayout.CENTER);

        JButton agregar = boton("Agregar pregunta", GestorDeEncuestas.COLOR_ACENTO);
        agregar.addActionListener(evento -> agregarPregunta());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        acciones.add(agregar);
        formulario.add(acciones, BorderLayout.SOUTH);

        JList<String> lista = new JList<>(modeloPreguntas);
        lista.setBackground(GestorDeEncuestas.COLOR_SUPERFICIE);
        lista.setForeground(GestorDeEncuestas.COLOR_TEXTO);
        lista.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane desplazamiento = new JScrollPane(lista);
        desplazamiento.setBorder(BorderFactory.createTitledBorder("Preguntas de la encuesta"));
        pagina.add(formulario, BorderLayout.NORTH);
        pagina.add(desplazamiento, BorderLayout.CENTER);
        return pagina;
    }

    private JPanel crearPanelRespuestas() {
        JPanel pagina = crearPagina();
        pagina.setLayout(new BorderLayout(10, 10));
        pagina.add(etiqueta("Selecciona una respuesta para cada pregunta. Puedes cambiarla y guardarla otra vez."),
                BorderLayout.NORTH);

        panelRespuestas.setLayout(new javax.swing.BoxLayout(panelRespuestas, javax.swing.BoxLayout.Y_AXIS));
        panelRespuestas.setBackground(GestorDeEncuestas.COLOR_FONDO);
        JScrollPane desplazamiento = new JScrollPane(panelRespuestas);
        desplazamiento.setBorder(null);
        desplazamiento.getViewport().setBackground(GestorDeEncuestas.COLOR_FONDO);
        pagina.add(desplazamiento, BorderLayout.CENTER);

        JButton guardar = boton("Guardar / modificar", GestorDeEncuestas.COLOR_PRIMARIO);
        guardar.addActionListener(evento -> guardarRespuestas());
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(estadoRespuestas, BorderLayout.CENTER);
        pie.add(guardar, BorderLayout.EAST);
        pagina.add(pie, BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel crearPanelVerificacion() {
        JPanel pagina = crearPagina();
        pagina.setLayout(new BorderLayout(10, 10));

        JButton verificar = boton("Verificar respuestas", GestorDeEncuestas.COLOR_ACENTO);
        verificar.addActionListener(evento -> actualizarInforme());
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barra.setOpaque(false);
        barra.add(verificar);
        pagina.add(barra, BorderLayout.NORTH);

        informe.setEditable(false);
        informe.setLineWrap(true);
        informe.setWrapStyleWord(true);
        informe.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        informe.setForeground(GestorDeEncuestas.COLOR_TEXTO);
        informe.setBackground(GestorDeEncuestas.COLOR_SUPERFICIE);
        informe.setBorder(new EmptyBorder(14, 14, 14, 14));
        pagina.add(new JScrollPane(informe), BorderLayout.CENTER);
        return pagina;
    }

    private void agregarPregunta() {
        try {
            List<String> opciones = Arrays.stream(campoOpciones.getText().split(","))
                    .map(String::trim)
                    .toList();
            Pregunta pregunta = new Pregunta(campoPregunta.getText(), opciones);
            preguntas.add(pregunta);
            modeloPreguntas.addElement((preguntas.size()) + ". " + pregunta.texto()
                    + "  |  " + String.join(" / ", pregunta.opciones()));
            campoPregunta.setText("");
            campoOpciones.setText("");
            actualizarPanelRespuestas();
            actualizarInforme();
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Revisa la pregunta",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actualizarPanelRespuestas() {
        panelRespuestas.removeAll();
        selectores.clear();
        if (preguntas.isEmpty()) {
            panelRespuestas.add(etiqueta("Primero agrega preguntas en la pestana Preguntas."));
        } else {
            for (Pregunta pregunta : preguntas) {
                JPanel fila = crearSuperficie();
                fila.setLayout(new BorderLayout(12, 8));
                fila.setBorder(new EmptyBorder(12, 14, 12, 14));
                fila.add(etiqueta(pregunta.texto()), BorderLayout.CENTER);

                JComboBox<String> selector = new JComboBox<>(pregunta.opciones().toArray(String[]::new));
                String actual = respuestas.respuestaPara(pregunta);
                if (actual != null) {
                    selector.setSelectedItem(actual);
                }
                selectores.add(selector);
                fila.add(selector, BorderLayout.EAST);
                fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
                panelRespuestas.add(fila);
                panelRespuestas.add(javax.swing.Box.createVerticalStrut(8));
            }
        }
        panelRespuestas.revalidate();
        panelRespuestas.repaint();
    }

    private void guardarRespuestas() {
        if (preguntas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega preguntas antes de responder.",
                    "Encuesta vacia", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (int indice = 0; indice < preguntas.size(); indice++) {
            String opcion = (String) selectores.get(indice).getSelectedItem();
            if (opcion == null || !preguntas.get(indice).contieneOpcion(opcion)) {
                JOptionPane.showMessageDialog(this, "Selecciona una opcion valida para cada pregunta.",
                        "Respuesta incompleta", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        for (int indice = 0; indice < preguntas.size(); indice++) {
            Pregunta pregunta = preguntas.get(indice);
            String opcion = (String) selectores.get(indice).getSelectedItem();
            if (respuestas.tieneRespuesta(pregunta)) {
                respuestas.modificarRespuesta(pregunta, opcion);
            } else {
                respuestas.responder(pregunta, opcion);
            }
        }
        estadoRespuestas.setText("Respuestas guardadas. Puedes modificarlas cuando quieras.");
        actualizarInforme();
    }

    private void actualizarInforme() {
        List<String> errores = VerificarRespuestas.errores(preguntas, respuestas);
        if (errores.isEmpty()) {
            StringBuilder texto = new StringBuilder("VERIFICACION CORRECTA\n\n");
            for (Pregunta pregunta : preguntas) {
                texto.append("OK  ").append(pregunta.texto()).append("\n    ")
                        .append(respuestas.respuestaPara(pregunta)).append("\n\n");
            }
            informe.setText(texto.toString());
        } else {
            StringBuilder texto = new StringBuilder("FALTAN DATOS POR REVISAR\n\n");
            for (String error : errores) {
                texto.append("- ").append(error).append("\n");
            }
            informe.setText(texto.toString());
        }
        informe.setCaretPosition(0);
    }

    private static JPanel crearPagina() {
        JPanel panel = new JPanel();
        panel.setBackground(GestorDeEncuestas.COLOR_FONDO);
        panel.setBorder(new EmptyBorder(14, 8, 8, 8));
        return panel;
    }

    private static JPanel crearSuperficie() {
        JPanel panel = new JPanel();
        panel.setBackground(GestorDeEncuestas.COLOR_SUPERFICIE);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        return panel;
    }

    private static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(GestorDeEncuestas.COLOR_TEXTO);
        return etiqueta;
    }

    private static JButton boton(String texto, java.awt.Color fondo) {
        JButton boton = new JButton(texto);
        boton.setBackground(fondo);
        boton.setForeground(GestorDeEncuestas.COLOR_SUPERFICIE);
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(10, 16, 10, 16));
        boton.setFont(boton.getFont().deriveFont(Font.BOLD));
        return boton;
    }
}
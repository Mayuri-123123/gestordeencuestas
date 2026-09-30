package com.mycompany.gestordeencuestas;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Es la VENTANA GRÁFICA del programa con estilo de panel de control moderno:
// barra lateral de navegación, tarjetas blancas con bordes redondeados,
// tipografía clara y gráficos de resultados dibujados a medida con colores.
// Funciones:
// 1. "Votar": formulario con barra de progreso en vivo, bienvenida y limpieza.
// 2. "Resultados": gráficos con paleta multicolor, porcentajes y exportador.
// 3. "+ Crear formulario": asistente para publicar nuevas encuestas.
// 4. "Enviar otra respuesta": reiniciar la vista de votar cómodamente.
// ----------------------------------------------------------------------------
// TIPOGRAFÍA LIMPIA: utiliza fuentes del sistema (Segoe UI) y símbolos seguros
// para evitar caracteres no compatibles (cuadros vacíos) en Windows.
// ============================================================================

/**
 * Panel de control moderno del Gestor de Encuestas: voto anónimo, resultados
 * interactivos en vivo y creador de formularios.
 */
public class EncuestaGUI extends JFrame {

    // ================= Paleta del diseño (tema claro moderno) =================
    private static final Color FONDO_APP = new Color(241, 244, 249); // gris azulado claro
    private static final Color FONDO_TARJETA = Color.WHITE;          // tarjetas blancas
    private static final Color LATERAL = new Color(22, 35, 58);      // azul noche (sidebar)
    private static final Color LATERAL_SEL = new Color(41, 128, 185);// acento al seleccionar
    private static final Color ACENTO = new Color(41, 128, 185);     // azul principal

    // Fondos de botones con contraste garantizado
    private static final Color BOTON_FONDO = new Color(30, 110, 166); // azul oscuro: Enviar + nav activa
    private static final Color VERDE_OSCURO = new Color(30, 132, 73); // verde oscuro: Crear formulario
    private static final Color VERDE = new Color(39, 174, 96);       // sello "anónimo" y 100% completado
    private static final Color TEXTO = new Color(33, 43, 61);        // texto principal
    private static final Color TEXTO_SUAVE = new Color(120, 134, 155);// texto secundario
    private static final Color BORDE = new Color(226, 232, 240);     // bordes sutiles
    private static final Color PISTA_BARRA = new Color(230, 236, 243);// fondo de las barras

    // Paleta multicolor para las barras de resultados de opciones
    private static final Color[] PALETA_RESULTADOS = {
        new Color(37, 99, 235),  // Azul vibrante
        new Color(16, 185, 129), // Verde esmeralda
        new Color(139, 92, 246), // Violeta / Púrpura
        new Color(245, 158, 11), // Ámbar / Naranja
        new Color(14, 165, 233), // Celeste
        new Color(236, 72, 153), // Rosa
        new Color(99, 102, 241)  // Índigo
    };

    // Nombres de las vistas del panel central (CardLayout).
    private static final String VISTA_VOTAR = "VOTAR";
    private static final String VISTA_RESULTADOS = "RESULTADOS";

    // ================= Lógica del negocio =================
    private final EncuestaService service = new EncuestaService();
    private Encuesta encuesta; // encuesta activa en pantalla

    // ================= Componentes vivos =================
    private final CardLayout cartas = new CardLayout();
    private JPanel panelCentral;
    private JPanel vistaVotar;
    private JPanel vistaResultados;
    private JToggleButton navVotar;
    private JToggleButton navResultados;
    private JLabel etiquetaTotal;
    private JLabel etiquetaEstado;
    private boolean recienVotado = false;
    private final List<ButtonGroup> grupos = new ArrayList<>();
    private final List<List<JRadioButton>> botonesPorPregunta = new ArrayList<>();

    // Componentes de la barra de progreso de llenado en vivo
    private BarraProgresoLlenado barraProgresoLlenado;
    private JLabel etiquetaProgresoPorcentaje;

    /** Arma la ventana completa y muestra la vista de votar. */
    public EncuestaGUI() {
        super("Gestor de Encuestas — Panel de control");
        cargarDemo(); // encuesta publicada con 2 preguntas + 3 votos de ejemplo
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 660));
        setSize(1100, 720);
        setLocationRelativeTo(null); // centrar en la pantalla
        armarContenido();
    }

    // ============================================================================
    // 1) CONSTRUCCIÓN DE LA VENTANA (encabezado + lateral + centro + estado)
    // ============================================================================

    private void armarContenido() {
        recienVotado = false;
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(crearEncabezado(), BorderLayout.NORTH);
        getContentPane().add(crearLateral(), BorderLayout.WEST);
        getContentPane().add(crearCentro(), BorderLayout.CENTER);
        getContentPane().add(crearBarraEstado(), BorderLayout.SOUTH);
        irAVotar();
        revalidate();
        repaint();
    }

    private JComponent crearEncabezado() {
        var panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO_TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        var titulo = new JLabel("Gestor de Encuestas");
        titulo.setFont(fuente(Font.BOLD, 21));
        titulo.setForeground(TEXTO);

        var subtitulo = new JLabel("Panel de control  ·  " + tituloCorto() + "  ·  Java 25");
        subtitulo.setFont(fuente(Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SUAVE);
        var textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        // Sello verde estilo píldora
        var sello = new RoundedPanel(18, new Color(232, 248, 240));
        sello.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        var selloTxt = new JLabel("● 100% ANÓNIMO");
        selloTxt.setFont(fuente(Font.BOLD, 12));
        selloTxt.setForeground(VERDE);
        sello.add(selloTxt);

        var derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(sello);
        panel.add(textos, BorderLayout.WEST);
        panel.add(derecha, BorderLayout.EAST);
        return panel;
    }

    private JComponent crearLateral() {
        var lateral = new JPanel();
        lateral.setBackground(LATERAL);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        lateral.setPreferredSize(new Dimension(210, 10));

        lateral.add(etiquetaLateral("MENÚ"));
        lateral.add(Box.createVerticalStrut(8));

        var grupoNav = new ButtonGroup();
        navVotar = botonLateral("Votar", true);
        navResultados = botonLateral("Resultados", false);
        grupoNav.add(navVotar);
        grupoNav.add(navResultados);
        navVotar.addActionListener(e -> mostrarCarta(VISTA_VOTAR));
        navResultados.addActionListener(e -> {
            refrescarResultados();
            mostrarCarta(VISTA_RESULTADOS);
        });
        lateral.add(navVotar);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(navResultados);
        lateral.add(Box.createVerticalStrut(6));

        var crear = botonAccionLateral("+ Crear formulario", VERDE_OSCURO);
        crear.addActionListener(e -> abrirCreadorFormulario());
        lateral.add(crear);
        lateral.add(Box.createVerticalStrut(22));

        lateral.add(etiquetaLateral("ACTIVIDAD"));
        lateral.add(Box.createVerticalStrut(8));

        var mini = new RoundedPanel(14, new Color(33, 50, 80));
        mini.setLayout(new GridLayout(2, 1, 0, 2));
        mini.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        etiquetaTotal = new JLabel("0", SwingConstants.LEFT);
        etiquetaTotal.setFont(fuente(Font.BOLD, 26));
        etiquetaTotal.setForeground(Color.WHITE);
        var cap = new JLabel("respuestas anónimas");
        cap.setFont(fuente(Font.PLAIN, 12));
        cap.setForeground(new Color(160, 178, 205));
        mini.add(etiquetaTotal);
        mini.add(cap);
        mini.setMaximumSize(new Dimension(220, 80));
        lateral.add(mini);

        lateral.add(Box.createVerticalGlue());
        var reiniciar = new JButton("Reiniciar demo");
        reiniciar.setFocusPainted(false);
        reiniciar.setFont(fuente(Font.PLAIN, 12));
        reiniciar.setForeground(new Color(200, 214, 232));
        reiniciar.setBackground(new Color(33, 50, 80));
        reiniciar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        reiniciar.setContentAreaFilled(false);
        reiniciar.setOpaque(true);
        reiniciar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reiniciar.addActionListener(e -> {
            cargarDemo();
            grupos.clear();
            botonesPorPregunta.clear();
            armarContenido();
            actualizarTotal();
            mensaje("Demo reiniciada: encuesta publicada con 3 votos de ejemplo.");
        });
        lateral.add(reiniciar);
        lateral.add(Box.createVerticalStrut(8));
        var pie = new JLabel("Java 25 · Swing", SwingConstants.CENTER);
        pie.setFont(fuente(Font.PLAIN, 11));
        pie.setForeground(new Color(120, 140, 170));
        pie.setAlignmentX(Component.CENTER_ALIGNMENT);
        lateral.add(pie);
        return lateral;
    }

    private JComponent crearCentro() {
        panelCentral = new JPanel(cartas);
        panelCentral.setBackground(FONDO_APP);
        vistaVotar = construirVistaVotar();
        vistaResultados = new JPanel();
        vistaResultados.setBackground(FONDO_APP);
        panelCentral.add(new JScrollPane(vistaVotar), VISTA_VOTAR);
        panelCentral.add(new JScrollPane(vistaResultados), VISTA_RESULTADOS);
        var contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO_APP);
        contenedor.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenedor.add(panelCentral, BorderLayout.CENTER);
        return contenedor;
    }

    private JComponent crearBarraEstado() {
        var panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO_TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDE),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        etiquetaEstado = new JLabel("Marca una opción por pregunta y pulsa Enviar.");
        etiquetaEstado.setFont(fuente(Font.PLAIN, 12));
        etiquetaEstado.setForeground(TEXTO_SUAVE);
        panel.add(etiquetaEstado, BorderLayout.WEST);
        return panel;
    }

    // ============================================================================
    // 2) VISTA "VOTAR" (tarjeta bienvenida + barra progreso en vivo + preguntas)
    // ============================================================================

    private JPanel construirVistaVotar() {
        grupos.clear();
        botonesPorPregunta.clear();
        var vista = new JPanel();
        vista.setBackground(FONDO_APP);
        vista.setLayout(new BoxLayout(vista, BoxLayout.Y_AXIS));

        // 1. Tarjeta de bienvenida
        vista.add(tarjetaBienvenida());
        vista.add(Box.createVerticalStrut(12));

        // 2. Barra de progreso interactiva de llenado en vivo
        vista.add(crearTarjetaProgresoLlenado());
        vista.add(Box.createVerticalStrut(14));

        var preguntas = encuesta.preguntas();
        if (preguntas.isEmpty()) {
            vista.add(tarjetaAviso("Esta encuesta aún no tiene preguntas."));
            return vista;
        }

        // 3. Una tarjeta por pregunta
        for (int i = 0; i < preguntas.size(); i++) {
            vista.add(tarjetaPregunta(preguntas.get(i), i + 1));
            vista.add(Box.createVerticalStrut(14));
        }

        // 4. Panel de acciones (Enviar respuesta + Limpiar respuestas)
        var panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelAcciones.setOpaque(false);
        panelAcciones.setAlignmentX(Component.LEFT_ALIGNMENT);

        var enviar = new JButton("Enviar respuesta anónima");
        enviar.setFont(fuente(Font.BOLD, 15));
        enviar.setBackground(BOTON_FONDO);
        enviar.setForeground(Color.WHITE);
        enviar.setFocusPainted(false);
        enviar.setContentAreaFilled(false);
        enviar.setOpaque(true);
        enviar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        enviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        enviar.addActionListener(e -> enviarVoto());

        var limpiar = new JButton("Limpiar respuestas");
        limpiar.setFont(fuente(Font.PLAIN, 13));
        limpiar.setBackground(FONDO_TARJETA);
        limpiar.setForeground(TEXTO_SUAVE);
        limpiar.setFocusPainted(false);
        limpiar.setContentAreaFilled(false);
        limpiar.setOpaque(true);
        limpiar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(11, 16, 11, 16)));
        limpiar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        limpiar.addActionListener(e -> {
            grupos.forEach(ButtonGroup::clearSelection);
            actualizarProgresoLlenado();
            mensaje("Respuestas limpiadas. Progreso restablecido a 0%.");
        });

        panelAcciones.add(enviar);
        panelAcciones.add(limpiar);
        vista.add(panelAcciones);
        vista.add(Box.createVerticalStrut(8));

        var ayuda = new JLabel("No se pide ni se guarda ningún dato personal: el voto es anónimo por diseño.");
        ayuda.setFont(fuente(Font.ITALIC, 12));
        ayuda.setForeground(TEXTO_SUAVE);
        vista.add(ayuda);
        getRootPane().setDefaultButton(enviar);

        actualizarProgresoLlenado();
        return vista;
    }

    private static JComponent tarjetaBienvenida() {
        var tarjeta = new RoundedPanel(16, new Color(234, 243, 250));
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 224, 242)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        var hola = new JLabel("¡Tu opinión cuenta!");
        hola.setFont(fuente(Font.BOLD, 17));
        hola.setForeground(BOTON_FONDO);
        var guia = new JLabel("Marca una opción por pregunta y pulsa Enviar. Tarda menos de un minuto y es 100% anónimo.");
        guia.setFont(fuente(Font.PLAIN, 13));
        guia.setForeground(TEXTO);
        tarjeta.add(hola);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(guia);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjeta.getPreferredSize().height + 40));
        return tarjeta;
    }

    private JComponent crearTarjetaProgresoLlenado() {
        var tarjeta = new RoundedPanel(16, FONDO_TARJETA);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        var cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        var tituloProgreso = new JLabel("Progreso de llenado");
        tituloProgreso.setFont(fuente(Font.BOLD, 13));
        tituloProgreso.setForeground(TEXTO);

        int total = (encuesta == null || encuesta.preguntas() == null) ? 0 : encuesta.preguntas().size();
        etiquetaProgresoPorcentaje = new JLabel("0 de " + total + " respondidas (0%)");
        etiquetaProgresoPorcentaje.setFont(fuente(Font.BOLD, 13));
        etiquetaProgresoPorcentaje.setForeground(TEXTO_SUAVE);

        cabecera.add(tituloProgreso, BorderLayout.WEST);
        cabecera.add(etiquetaProgresoPorcentaje, BorderLayout.EAST);

        barraProgresoLlenado = new BarraProgresoLlenado();

        tarjeta.add(cabecera);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(barraProgresoLlenado);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        return tarjeta;
    }

    private void actualizarProgresoLlenado() {
        if (barraProgresoLlenado == null || etiquetaProgresoPorcentaje == null) {
            return;
        }
        int total = (encuesta == null || encuesta.preguntas() == null) ? 0 : encuesta.preguntas().size();
        if (total == 0) {
            etiquetaProgresoPorcentaje.setText("Sin preguntas");
            barraProgresoLlenado.setProgreso(0);
            return;
        }
        int respondidas = 0;
        for (var grupo : grupos) {
            if (grupo.getSelection() != null) {
                respondidas++;
            }
        }
        double pct = (respondidas * 100.0) / total;
        barraProgresoLlenado.setProgreso(pct);

        if (respondidas == total) {
            etiquetaProgresoPorcentaje.setText("✓ " + respondidas + " de " + total + " respondidas (100%) — ¡Listo para enviar!");
            etiquetaProgresoPorcentaje.setForeground(VERDE);
            mensaje("✓ Formulario completado al 100%. Pulsa 'Enviar respuesta anónima'.");
        } else {
            etiquetaProgresoPorcentaje.setText(respondidas + " de " + total + " respondidas (" + (int) Math.round(pct) + "%)");
            etiquetaProgresoPorcentaje.setForeground(TEXTO_SUAVE);
            mensaje("Has respondido " + respondidas + " de " + total + " preguntas.");
        }
    }

    private JComponent tarjetaPregunta(com.mycompany.gestordeencuestas.Pregunta pregunta, int numero) {
        var tarjeta = new RoundedPanel(16, FONDO_TARJETA);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        var enunciado = new JLabel(numero + ". " + pregunta.texto());
        enunciado.setFont(fuente(Font.BOLD, 15));
        enunciado.setForeground(TEXTO);
        tarjeta.add(enunciado);
        tarjeta.add(Box.createVerticalStrut(8));

        var grupo = new ButtonGroup();
        var botones = new ArrayList<JRadioButton>();
        var opciones = pregunta.opciones();
        for (int j = 0; j < opciones.size(); j++) {
            var opcion = opciones.get(j);
            var radio = new JRadioButton(opcion.texto());
            radio.setBackground(FONDO_TARJETA);
            radio.setFont(fuente(Font.PLAIN, 14));
            radio.setForeground(TEXTO);
            radio.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            radio.putClientProperty("opcionId", opcion.id());
            radio.addActionListener(e -> actualizarProgresoLlenado());
            grupo.add(radio);
            tarjeta.add(radio);
            botones.add(radio);
        }
        grupos.add(grupo);
        botonesPorPregunta.add(botones);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjeta.getPreferredSize().height + 40));
        return tarjeta;
    }

    // ============================================================================
    // 3) VISTA "RESULTADOS" (resumen + agradecimiento + gráficos multicolor + exportador)
    // ============================================================================

    private void refrescarResultados() {
        vistaResultados.removeAll();
        vistaResultados.setLayout(new BoxLayout(vistaResultados, BoxLayout.Y_AXIS));

        // 1. Fila de tarjetas resumen (total / estado / preguntas)
        var fila = new JPanel(new GridLayout(1, 3, 12, 0));
        fila.setBackground(FONDO_APP);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        fila.add(tarjetaResumen(String.valueOf(totalSeguro()), "respuestas anónimas"));
        fila.add(tarjetaResumen(encuesta.estado().toString(), "estado"));
        fila.add(tarjetaResumen(String.valueOf(encuesta.preguntas().size()), "preguntas"));
        vistaResultados.add(fila);
        vistaResultados.add(Box.createVerticalStrut(14));

        // 2. Fila de acciones (Exportar reporte + Enviar otra respuesta)
        vistaResultados.add(barraAccionesResultados());
        vistaResultados.add(Box.createVerticalStrut(14));

        // 3. Agradecimiento justo después de votar
        if (recienVotado) {
            recienVotado = false;
            vistaResultados.add(tarjetaAgradecimiento());
            vistaResultados.add(Box.createVerticalStrut(14));
        }

        // 4. Tarjetas gráficas por pregunta con paleta multicolor
        var preguntas = encuesta.preguntas();
        var porcentajes = service.resultados(encuesta.id());
        var conteos = service.conteo(encuesta.id());
        for (int i = 0; i < preguntas.size(); i++) {
            var pregunta = preguntas.get(i);
            vistaResultados.add(tarjetaGrafica(pregunta, i + 1, porcentajes, conteos));
            vistaResultados.add(Box.createVerticalStrut(14));
        }
        actualizarTotal();
        vistaResultados.revalidate();
        vistaResultados.repaint();
    }

    private JComponent barraAccionesResultados() {
        var fila = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        var exportar = new JButton("Exportar reporte (.txt)");
        exportar.setFont(fuente(Font.PLAIN, 13));
        exportar.setBackground(FONDO_TARJETA);
        exportar.setForeground(TEXTO);
        exportar.setFocusPainted(false);
        exportar.setContentAreaFilled(false);
        exportar.setOpaque(true);
        exportar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        exportar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportar.addActionListener(e -> exportarResultados());

        fila.add(exportar);
        fila.add(botonPrimario("Enviar otra respuesta", e -> votarDeNuevo()));
        return fila;
    }

    private static JComponent tarjetaAgradecimiento() {
        var tarjeta = new RoundedPanel(16, new Color(232, 248, 240));
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(168, 224, 189)),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        var gracias = new JLabel("¡Gracias por contestar la encuesta!");
        gracias.setFont(fuente(Font.BOLD, 19));
        gracias.setForeground(VERDE_OSCURO);
        var sub = new JLabel("Tu respuesta ya cuenta en los resultados. Abajo puedes ver cómo va la votación.");
        sub.setFont(fuente(Font.PLAIN, 13));
        sub.setForeground(TEXTO);
        tarjeta.add(gracias);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(sub);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjeta.getPreferredSize().height + 50));
        return tarjeta;
    }

    private void votarDeNuevo() {
        irAVotar();
        mensaje("Formulario listo para otra respuesta anónima.");
    }

    private JComponent tarjetaResumen(String valor, String etiqueta) {
        var tarjeta = new RoundedPanel(16, FONDO_TARJETA);
        tarjeta.setLayout(new GridLayout(2, 1, 0, 2));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        var num = new JLabel(valor);
        num.setFont(fuente(Font.BOLD, 24));
        num.setForeground(ACENTO);
        var cap = new JLabel(etiqueta);
        cap.setFont(fuente(Font.PLAIN, 12));
        cap.setForeground(TEXTO_SUAVE);
        tarjeta.add(num);
        tarjeta.add(cap);
        return tarjeta;
    }

    private JComponent tarjetaGrafica(com.mycompany.gestordeencuestas.Pregunta pregunta, int numero,
                                      Map<UUID, Map<UUID, Double>> porcentajes,
                                      Map<UUID, Map<UUID, Long>> conteos) {
        var tarjeta = new RoundedPanel(16, FONDO_TARJETA);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        var titulo = new JLabel(numero + ". " + pregunta.texto());
        titulo.setFont(fuente(Font.BOLD, 15));
        titulo.setForeground(TEXTO);
        tarjeta.add(titulo);
        tarjeta.add(Box.createVerticalStrut(10));

        var pcts = porcentajes.getOrDefault(pregunta.id(), Map.of());
        var votos = conteos.getOrDefault(pregunta.id(), Map.of());
        var opciones = pregunta.opciones();
        for (int j = 0; j < opciones.size(); j++) {
            var opcion = opciones.get(j);
            double pct = pcts.getOrDefault(opcion.id(), 0.0);
            long v = votos.getOrDefault(opcion.id(), 0L);

            // Asignación de color armonioso de la paleta
            Color colorOpcion = PALETA_RESULTADOS[j % PALETA_RESULTADOS.length];

            var fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);

            // Indicador de color + nombre de opción
            var panelIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panelIzquierda.setOpaque(false);
            var puntoColor = new JLabel("● ");
            puntoColor.setFont(fuente(Font.BOLD, 14));
            puntoColor.setForeground(colorOpcion);
            var nombre = new JLabel(opcion.texto());
            nombre.setFont(fuente(Font.PLAIN, 13));
            nombre.setForeground(TEXTO);
            panelIzquierda.add(puntoColor);
            panelIzquierda.add(nombre);

            var cifra = new JLabel("%s  ·  %d voto%s".formatted(pctSeguro(pct), v, (v == 1 ? "" : "s")));
            cifra.setFont(fuente(Font.BOLD, 13));
            cifra.setForeground(TEXTO);

            fila.add(panelIzquierda, BorderLayout.WEST);
            fila.add(cifra, BorderLayout.EAST);
            tarjeta.add(fila);
            tarjeta.add(Box.createVerticalStrut(4));

            // Barra gráfica estilizada con el color asignado a la opción
            var barra = new BarraResultado(colorOpcion);
            barra.setPorcentaje(pct);
            tarjeta.add(barra);
            tarjeta.add(Box.createVerticalStrut(10));
        }
        return tarjeta;
    }

    private void exportarResultados() {
        var chooser = new JFileChooser();
        String sugerido = "Resultados_" + encuesta.titulo().replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt";
        chooser.setSelectedFile(new File(sugerido));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.getSelectedFile();
            try (var writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.println("==================================================");
                writer.println("REPORTE DE RESULTADOS - GESTOR DE ENCUESTAS");
                writer.println("Título: " + encuesta.titulo());
                writer.println("Estado: " + encuesta.estado());
                writer.println("Total de respuestas anónimas: " + totalSeguro());
                writer.println("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("==================================================\n");

                var porcentajes = service.resultados(encuesta.id());
                var conteos = service.conteo(encuesta.id());
                var preguntas = encuesta.preguntas();
                for (int i = 0; i < preguntas.size(); i++) {
                    var p = preguntas.get(i);
                    writer.println((i + 1) + ". " + p.texto());
                    var pcts = porcentajes.getOrDefault(p.id(), Map.of());
                    var votos = conteos.getOrDefault(p.id(), Map.of());
                    for (var op : p.opciones()) {
                        double pct = pcts.getOrDefault(op.id(), 0.0);
                        long v = votos.getOrDefault(op.id(), 0L);
                        writer.printf("   - %-25s : %6.1f%% (%d votos)%n", op.texto(), pct, v);
                    }
                    writer.println();
                }
                mensaje("Reporte exportado exitosamente en: " + file.getName());
                JOptionPane.showMessageDialog(this,
                        "Reporte exportado correctamente en:\n" + file.getAbsolutePath(),
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                mensaje("Error al exportar: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "No se pudo exportar el archivo: " + ex.getMessage(),
                        "Error al exportar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ============================================================================
    // 4) LÓGICA DE VOTACIÓN Y DATOS DE EJEMPLO
    // ============================================================================

    private void cargarDemo() {
        encuesta = service.crearEncuesta("Hábitos de estudio");
        var p1 = service.agregarPregunta(encuesta.id(), "¿Cuántas horas estudias al día?",
                List.of("Menos de 1h", "1-3h", "Más de 3h"));
        var p2 = service.agregarPregunta(encuesta.id(), "¿Dónde prefieres estudiar?",
                List.of("Casa", "Biblioteca", "Café"));
        service.publicar(encuesta.id());

        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(0).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(2).id(), p2.id(), p2.opciones().get(1).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(1).id()));
    }

    private void enviarVoto() {
        var preguntas = encuesta.preguntas();
        if (botonesPorPregunta.size() != preguntas.size()) {
            mensaje("Error interno: formulario desactualizado. Pulsa Reiniciar demo.");
            return;
        }
        var seleccion = new HashMap<UUID, UUID>();
        for (int i = 0; i < preguntas.size(); i++) {
            UUID marcada = null;
            var botones = botonesPorPregunta.get(i);
            for (int j = 0; j < botones.size(); j++) {
                var radio = botones.get(j);
                if (radio.isSelected()) {
                    marcada = (UUID) radio.getClientProperty("opcionId");
                }
            }
            if (marcada == null) {
                String falta = preguntas.get(i).texto();
                mensaje("Te falta responder: \"" + falta + "\"");
                JOptionPane.showMessageDialog(this,
                        "Responde TODAS las preguntas antes de enviar.\nTe falta responder: \"" + falta + "\"",
                        "Respuesta incompleta", JOptionPane.WARNING_MESSAGE);
                return;
            }
            seleccion.put(preguntas.get(i).id(), marcada);
        }
        try {
            service.responder(encuesta.id(), seleccion);
            grupos.forEach(ButtonGroup::clearSelection);
            actualizarProgresoLlenado();
            recienVotado = true;
            refrescarResultados();
            mostrarCarta(VISTA_RESULTADOS);
            navResultados.setSelected(true);
            mensaje("¡Gracias por contestar la encuesta! Total de respuestas: " + totalSeguro() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje("Voto rechazado: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Voto rechazado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarCarta(String nombre) {
        cartas.show(panelCentral, nombre);
    }

    private void irAVotar() {
        navVotar.setSelected(true);
        mostrarCarta(VISTA_VOTAR);
        actualizarTotal();
    }

    private void actualizarTotal() {
        etiquetaTotal.setText(String.valueOf(totalSeguro()));
    }

    private int totalSeguro() {
        try {
            return service.totalRespuestas(encuesta.id());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private void mensaje(String texto) {
        etiquetaEstado.setText(texto == null ? "" : texto);
    }

    // ============================================================================
    // 5) AYUDAS VISUALES Y BOTONES
    // ============================================================================

    private static Font fuente(int estilo, int tamanio) {
        return new Font("Segoe UI", estilo, tamanio);
    }

    private static JLabel etiquetaLateral(String texto) {
        var l = new JLabel(texto);
        l.setFont(fuente(Font.BOLD, 11));
        l.setForeground(new Color(130, 150, 180));
        return l;
    }

    private static JToggleButton botonLateral(String texto, boolean activo) {
        var b = new JToggleButton(texto, activo);
        b.setFocusPainted(false);
        b.setFont(fuente(Font.BOLD, 14));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setMaximumSize(new Dimension(220, 40));
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(activo ? BOTON_FONDO : LATERAL);
        b.setForeground(Color.WHITE);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> b.setBackground(b.isSelected() ? BOTON_FONDO : LATERAL));
        return b;
    }

    private static JButton botonAccionLateral(String texto, Color fondo) {
        var b = new JButton(texto);
        b.setFocusPainted(false);
        b.setFont(fuente(Font.BOLD, 13));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setMaximumSize(new Dimension(220, 40));
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton botonPrimario(String texto, java.awt.event.ActionListener accion) {
        var b = new JButton(texto);
        b.setFont(fuente(Font.BOLD, 14));
        b.setBackground(BOTON_FONDO);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(accion);
        return b;
    }

    private String tituloCorto() {
        String t = (encuesta == null) ? "" : encuesta.titulo();
        return (t.length() <= 40) ? t : (t.substring(0, 37) + "...");
    }

    private static JComponent tarjetaAviso(String texto) {
        var tarjeta = new RoundedPanel(16, FONDO_TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        var l = new JLabel(texto);
        l.setFont(fuente(Font.PLAIN, 14));
        l.setForeground(TEXTO_SUAVE);
        tarjeta.add(l);
        return tarjeta;
    }

    // ============================================================================
    // 6) COMPONENTES A MEDIDA (RoundedPanel, BarraResultado, BarraProgresoLlenado)
    // ============================================================================

    private static class RoundedPanel extends JPanel {
        private final int radio;
        private final Color fondo;

        RoundedPanel(int radio, Color fondo) {
            super();
            this.radio = Math.clamp(radio, 0, 60);
            this.fondo = fondo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radio, radio);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Barra de resultado para las opciones con color configurable y sin errores de rango.
     */
    private static final class BarraResultado extends JComponent {
        private double porcentaje;
        private final Color colorBarra;

        BarraResultado() {
            this(ACENTO);
        }

        BarraResultado(Color color) {
            this.porcentaje = 0.0;
            this.colorBarra = (color != null) ? color : ACENTO;
            setPreferredSize(new Dimension(10, 14));
            setMinimumSize(new Dimension(10, 14));
            var acc = getAccessibleContext();
            if (acc != null) {
                acc.setAccessibleName("Barra de porcentaje");
            }
        }

        void setPorcentaje(double valor) {
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                valor = 0.0;
            }
            this.porcentaje = Math.clamp(valor, 0.0, 100.0);
            var acc = getAccessibleContext();
            if (acc != null) {
                acc.setAccessibleDescription("%.1f por ciento".formatted(porcentaje));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = Math.max(0, getWidth());
                int h = Math.max(0, getHeight());
                int radio = Math.clamp(h, 0, 20);

                // Fondo de pista
                g2.setColor(PISTA_BARRA);
                g2.fillRoundRect(0, 0, w, h, radio, radio);

                // Relleno con el color de la opción
                int relleno = (int) Math.round(w * (porcentaje / 100.0));
                relleno = Math.clamp(relleno, 0, w);
                if (relleno > 0 && h > 0) {
                    g2.setColor(colorBarra);
                    g2.fillRoundRect(0, 0, relleno, h, radio, radio);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Barra interactiva de llenado en vivo que cambia a verde al 100%.
     */
    private static final class BarraProgresoLlenado extends JComponent {
        private double porcentaje = 0.0;
        private Color colorActual = ACENTO;

        BarraProgresoLlenado() {
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            var acc = getAccessibleContext();
            if (acc != null) {
                acc.setAccessibleName("Barra de progreso de llenado");
            }
        }

        void setProgreso(double valor) {
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                valor = 0.0;
            }
            this.porcentaje = Math.clamp(valor, 0.0, 100.0);
            this.colorActual = (this.porcentaje >= 100.0) ? VERDE : ACENTO;
            var acc = getAccessibleContext();
            if (acc != null) {
                acc.setAccessibleDescription("%.1f por ciento completado".formatted(porcentaje));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = Math.max(0, getWidth());
                int h = Math.max(0, getHeight());
                int radio = Math.clamp(h, 0, 20);

                g2.setColor(PISTA_BARRA);
                g2.fillRoundRect(0, 0, w, h, radio, radio);

                int relleno = (int) Math.round(w * (porcentaje / 100.0));
                relleno = Math.clamp(relleno, 0, w);
                if (relleno > 0 && h > 0) {
                    g2.setColor(colorActual);
                    g2.fillRoundRect(0, 0, relleno, h, radio, radio);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ============================================================================
    // 7) CREADOR DE FORMULARIO PROPIO
    // ============================================================================

    private void abrirCreadorFormulario() {
        var dialogo = new JDialog(this, "Crear mi propio formulario", true);
        dialogo.setSize(680, 640);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());

        var norte = new JPanel(new BorderLayout(0, 4));
        norte.setBackground(FONDO_TARJETA);
        norte.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
        var etTitulo = new JLabel("Título del formulario");
        etTitulo.setFont(fuente(Font.BOLD, 14));
        etTitulo.setForeground(TEXTO);
        var campoTitulo = new JTextField();
        campoTitulo.setFont(fuente(Font.PLAIN, 14));
        norte.add(etTitulo, BorderLayout.NORTH);
        norte.add(campoTitulo, BorderLayout.CENTER);
        dialogo.add(norte, BorderLayout.NORTH);

        var contenedor = new JPanel();
        contenedor.setBackground(FONDO_APP);
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        var bloques = new ArrayList<BloquePregunta>();
        Runnable refrescar = () -> {
            contenedor.removeAll();
            for (int i = 0; i < bloques.size(); i++) {
                bloques.get(i).setNumero(i + 1);
                contenedor.add(bloques.get(i));
                contenedor.add(Box.createVerticalStrut(10));
            }
            contenedor.revalidate();
            contenedor.repaint();
        };
        bloques.add(new BloquePregunta(() -> quitarBloque(contenedor, bloques, refrescar)));
        bloques.add(new BloquePregunta(() -> quitarBloque(contenedor, bloques, refrescar)));
        refrescar.run();
        dialogo.add(new JScrollPane(contenedor), BorderLayout.CENTER);

        var sur = new JPanel(new BorderLayout(10, 0));
        sur.setBackground(FONDO_TARJETA);
        sur.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));
        var agregar = new JButton("+ Agregar pregunta");
        agregar.setFocusPainted(false);
        agregar.setFont(fuente(Font.BOLD, 13));
        agregar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        agregar.addActionListener(e -> {
            bloques.add(new BloquePregunta(() -> quitarBloque(contenedor, bloques, refrescar)));
            refrescar.run();
        });
        var botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        var cancelar = new JButton("Cancelar");
        cancelar.setFocusPainted(false);
        cancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelar.addActionListener(e -> dialogo.dispose());
        var publicar = new JButton("Publicar formulario");
        publicar.setFont(fuente(Font.BOLD, 13));
        publicar.setBackground(BOTON_FONDO);
        publicar.setForeground(Color.WHITE);
        publicar.setFocusPainted(false);
        publicar.setContentAreaFilled(false);
        publicar.setOpaque(true);
        publicar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        publicar.addActionListener(e -> publicarFormularioPropio(dialogo, campoTitulo, bloques));
        botones.add(cancelar);
        botones.add(publicar);
        sur.add(agregar, BorderLayout.WEST);
        sur.add(botones, BorderLayout.EAST);
        dialogo.add(sur, BorderLayout.SOUTH);

        dialogo.setVisible(true);
    }

    private void quitarBloque(JPanel contenedor, List<BloquePregunta> bloques, Runnable refrescar) {
        if (bloques.size() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "El formulario necesita al menos 1 pregunta.",
                    "No se puede quitar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        bloques.remove(bloques.size() - 1);
        refrescar.run();
    }

    private void publicarFormularioPropio(JDialog dialogo, JTextField campoTitulo,
                                          List<BloquePregunta> bloques) {
        String titulo = campoTitulo.getText().strip();
        if (titulo.isEmpty()) {
            avisar(dialogo, "Escribe el título del formulario.");
            return;
        }
        var datos = new ArrayList<Map.Entry<String, List<String>>>();
        for (int i = 0; i < bloques.size(); i++) {
            String en = bloques.get(i).enunciado();
            if (en.isEmpty()) {
                avisar(dialogo, "La pregunta " + (i + 1) + " no tiene enunciado.");
                return;
            }
            var ops = bloques.get(i).opciones();
            if (ops.size() < 2) {
                avisar(dialogo, "La pregunta " + (i + 1) + " necesita al menos 2 opciones con texto.");
                return;
            }
            datos.add(Map.entry(en, ops));
        }
        try {
            var nueva = service.crearEncuesta(titulo);
            for (var d : datos) {
                service.agregarPregunta(nueva.id(), d.getKey(), d.getValue());
            }
            service.publicar(nueva.id());
            encuesta = nueva;
            dialogo.dispose();
            armarContenido();
            mensaje("Formulario \"" + tituloCorto() + "\" publicado. ¡Ya puedes votar!");
        } catch (RuntimeException ex) {
            avisar(dialogo, "No se pudo publicar: " + ex.getMessage());
        }
    }

    private static void avisar(JDialog dialogo, String texto) {
        JOptionPane.showMessageDialog(dialogo, texto,
                "Revisa tu formulario", JOptionPane.WARNING_MESSAGE);
    }

    private static final class BloquePregunta extends RoundedPanel {
        private final JLabel etiquetaNum = new JLabel();
        private final JTextField campoEnunciado = new JTextField();
        private final JPanel listaOpciones = new JPanel();
        private final List<JTextField> camposOpcion = new ArrayList<>();
        private final JButton btnMas = new JButton("+ Opción");
        private final JButton btnMenos = new JButton("− Opción");

        BloquePregunta(Runnable alQuitar) {
            super(14, FONDO_TARJETA);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDE),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));

            var fila = new JPanel(new BorderLayout());
            fila.setOpaque(false);
            etiquetaNum.setFont(fuente(Font.BOLD, 14));
            etiquetaNum.setForeground(TEXTO);
            var quitar = new JButton("Quitar");
            quitar.setFocusPainted(false);
            quitar.setFont(fuente(Font.PLAIN, 12));
            quitar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            quitar.addActionListener(e -> alQuitar.run());
            fila.add(etiquetaNum, BorderLayout.WEST);
            fila.add(quitar, BorderLayout.EAST);
            add(fila);
            add(Box.createVerticalStrut(6));

            campoEnunciado.setFont(fuente(Font.PLAIN, 13));
            add(campoEnunciado);
            add(Box.createVerticalStrut(8));

            listaOpciones.setOpaque(false);
            listaOpciones.setLayout(new BoxLayout(listaOpciones, BoxLayout.Y_AXIS));
            add(listaOpciones);
            agregarCampoOpcion();
            agregarCampoOpcion();

            var filaBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            filaBtns.setOpaque(false);
            btnMas.setFocusPainted(false);
            btnMas.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMas.addActionListener(e -> {
                if (camposOpcion.size() < 10) {
                    agregarCampoOpcion();
                    refrescarBloque();
                }
            });
            btnMenos.setFocusPainted(false);
            btnMenos.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMenos.addActionListener(e -> {
                if (camposOpcion.size() > 2) {
                    listaOpciones.remove(camposOpcion.remove(camposOpcion.size() - 1));
                    refrescarBloque();
                }
            });
            filaBtns.add(btnMas);
            filaBtns.add(btnMenos);
            add(Box.createVerticalStrut(4));
            add(filaBtns);
            refrescarBloque();
        }

        private void agregarCampoOpcion() {
            var campo = new JTextField();
            campo.setFont(fuente(Font.PLAIN, 13));
            campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            camposOpcion.add(campo);
            listaOpciones.add(campo);
            listaOpciones.add(Box.createVerticalStrut(4));
        }

        private void refrescarBloque() {
            btnMas.setEnabled(camposOpcion.size() < 10);
            btnMenos.setEnabled(camposOpcion.size() > 2);
            revalidate();
            repaint();
        }

        void setNumero(int n) {
            etiquetaNum.setText("Pregunta " + n);
        }

        String enunciado() {
            return campoEnunciado.getText().strip();
        }

        List<String> opciones() {
            var textos = new ArrayList<String>();
            for (int i = 0; i < camposOpcion.size(); i++) {
                String t = camposOpcion.get(i).getText().strip();
                if (!t.isEmpty()) {
                    textos.add(t);
                }
            }
            return textos;
        }
    }

    // ============================================================================
    // 8) ARRANQUE DEL PROGRAMA
    // ============================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new EncuestaGUI().setVisible(true));
    }

    private static String pctSeguro(double pct) {
        if (Double.isNaN(pct) || Double.isInfinite(pct)) {
            pct = 0.0;
        }
        return "%.1f%%".formatted(Math.clamp(pct, 0.0, 100.0));
    }
}

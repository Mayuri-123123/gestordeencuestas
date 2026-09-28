package com.mycompany.gestordeencuestas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Es la VENTANA GRÁFICA del programa (interfaz bonita con botones y barras).
// Permite votar de forma anónima y ver los resultados en vivo, sin usar la
// consola. Internamente usa las mismas clases seguras (EncuestaService,
// Encuesta, Pregunta...), así que TODAS las reglas se siguen aplicando:
// si intentas votar sin responder todo, la ventana te avisa (anti-parcial).
// ----------------------------------------------------------------------------
// ¿CÓMO EJECUTARLO?
//   En NetBeans: clic derecho en este archivo -> Run File (Mayús+F6).
//   La ventana se abre con una encuesta de ejemplo ya publicada y 3 votos.
// ============================================================================

/**
 * Ventana principal del Gestor de Encuestas: voto anónimo + resultados en vivo.
 */
public class EncuestaGUI extends JFrame {

    // ---- Colores corporativos de la ventana (azul serio + acento) ----
    private static final Color AZUL_OSCURO = new Color(31, 58, 95);
    private static final Color AZUL_ACENTO = new Color(41, 128, 185);
    private static final Color FONDO = new Color(245, 247, 250);

    // ---- Lógica del negocio (la ventana nunca toca los datos directamente) ----
    private final EncuestaService service = new EncuestaService();
    private Encuesta encuesta; // Encuesta de demostración (publicada)

    // ---- Componentes que necesitamos actualizar al votar ----
    private final List<ButtonGroup> grupos = new ArrayList<>(); // un grupo por pregunta
    private final List<List<JRadioButton>> botonesPorPregunta = new ArrayList<>();
    private JPanel panelResultados; // se reconstruye con cada voto
    private JLabel etiquetaEstado;  // barra inferior de mensajes

    /** Arma la ventana (título, tamaño, paneles, encuesta de ejemplo). */
    public EncuestaGUI() {
        super("Gestor de Encuestas — Voto anónimo");

        // 1) Datos de ejemplo: encuesta publicada con 2 preguntas + 3 votos.
        cargarDemo();

        // 2) Aspecto general de la ventana.
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null); // centrar en pantalla
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout(0, 0));

        // 3) Armado por partes: encabezado, centro (votar | resultados), pie.
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearPie(), BorderLayout.SOUTH);

        // 4) Mostrar los resultados iniciales.
        refrescarResultados();
    }

    // ==================== Construcción de la ventana ====================

    /**
     * ¿QUÉ HACE? Crea la franja superior con el título del programa.
     */
    private JPanel crearEncabezado() {
        var panel = new JPanel(new BorderLayout());
        panel.setBackground(AZUL_OSCURO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        var titulo = new JLabel("Gestor de Encuestas");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));

        var subtitulo = new JLabel("Respuestas 100% anónimas  ·  Una opción por pregunta  ·  Java 25");
        subtitulo.setForeground(new Color(180, 200, 225));
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        var caja = new JPanel(new GridLayout(2, 1));
        caja.setOpaque(false);
        caja.add(titulo);
        caja.add(subtitulo);
        panel.add(caja, BorderLayout.WEST);

        var sello = new JLabel("ANÓNIMO", SwingConstants.CENTER);
        sello.setForeground(AZUL_OSCURO);
        sello.setBackground(new Color(46, 204, 113));
        sello.setOpaque(true);
        sello.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sello.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        panel.add(sello, BorderLayout.EAST);
        return panel;
    }

    /**
     * ¿QUÉ HACE? Crea el área central dividida: a la izquierda se vota,
     * a la derecha se ven los resultados con barras de progreso.
     */
    private JSplitPane crearCentro() {
        var votar = new JPanel(new BorderLayout());
        votar.setBackground(FONDO);
        votar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));

        var tituloVotar = new JLabel("Tu voto (sin nombre ni datos personales)");
        tituloVotar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        votar.add(tituloVotar, BorderLayout.NORTH);
        votar.add(new JScrollPane(panelVotacion()), BorderLayout.CENTER);

        var botonVotar = new JButton("Enviar respuesta anónima");
        botonVotar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        botonVotar.setBackground(AZUL_ACENTO);
        botonVotar.setForeground(Color.WHITE);
        botonVotar.setFocusPainted(false);
        // Al pulsar, se recogen las opciones marcadas y se registra el voto.
        botonVotar.addActionListener(e -> enviarVoto());
        var sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        sur.add(botonVotar, BorderLayout.CENTER);
        votar.add(sur, BorderLayout.SOUTH);

        panelResultados = new JPanel();
        panelResultados.setBackground(Color.WHITE);
        var scrollResultados = new JScrollPane(panelResultados);
        scrollResultados.setBorder(BorderFactory.createTitledBorder("Resultados en vivo"));

        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, votar, scrollResultados);
        split.setResizeWeight(0.5); // mitad y mitad
        split.setBorder(null);
        return split;
    }

    /**
     * ¿QUÉ HACE? Crea un formulario con una pregunta y sus opciones (botones
     * redondos donde solo se puede marcar UNA opción por pregunta).
     */
    private JPanel panelVotacion() {
        var panel = new JPanel(new GridLayout(0, 1, 0, 12));
        panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Una cajita por pregunta, con sus opciones dentro.
        for (var pregunta : encuesta.preguntas()) {
            var caja = new JPanel(new GridLayout(0, 1));
            caja.setBackground(Color.WHITE);
            caja.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 226, 235)),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));

            var enunciado = new JLabel(pregunta.texto());
            enunciado.setFont(new Font("Segoe UI", Font.BOLD, 14));
            caja.add(enunciado);

            // ButtonGroup = "solo se puede marcar una opción de este grupo".
            var grupo = new ButtonGroup();
            var botones = new ArrayList<JRadioButton>();
            for (var opcion : pregunta.opciones()) {
                var radio = new JRadioButton(opcion.texto());
                radio.setBackground(Color.WHITE);
                radio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                // Guardamos el id REAL de la opción dentro del botón (invisible
                // para el usuario) para luego votar con ids seguros.
                radio.putClientProperty("opcionId", opcion.id());
                grupo.add(radio);
                caja.add(radio);
                botones.add(radio);
            }
            grupos.add(grupo);
            botonesPorPregunta.add(botones);
            panel.add(caja);
        }
        return panel;
    }

    /**
     * ¿QUÉ HACE? Crea la barra inferior con mensajes de estado y el botón
     * para reiniciar la demostración.
     */
    private JPanel crearPie() {
        var panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(234, 238, 244));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        etiquetaEstado = new JLabel("Listo. Marca una opción por pregunta y pulsa Enviar.");
        etiquetaEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(etiquetaEstado, BorderLayout.WEST);

        var botonDemo = new JButton("Reiniciar demo");
        botonDemo.setFocusPainted(false);
        // Borra todo y vuelve a cargar la encuesta de ejemplo con 3 votos.
        botonDemo.addActionListener(e -> {
            grupos.clear();
            botonesPorPregunta.clear();
            getContentPane().removeAll();
            cargarDemo();
            add(crearEncabezado(), BorderLayout.NORTH);
            add(crearCentro(), BorderLayout.CENTER);
            add(crearPie(), BorderLayout.SOUTH);
            etiquetaEstado.setText("Demo reiniciada: encuesta publicada con 3 votos de ejemplo.");
            revalidate();
            repaint();
        });
        panel.add(botonDemo, BorderLayout.EAST);
        return panel;
    }

    // ==================== Lógica de la ventana ====================

    /**
     * ¿QUÉ HACE? Prepara la encuesta de ejemplo: la crea, le agrega 2
     * preguntas, la PUBLICA y registra 3 votos anónimos iniciales.
     * ¿PARA QUÉ? Para que al abrir la ventana ya haya algo que ver y probar.
     */
    private void cargarDemo() {
        encuesta = service.crearEncuesta("Hábitos de estudio");
        var p1 = service.agregarPregunta(encuesta.id(),
                "¿Cuántas horas estudias al día?", List.of("Menos de 1h", "1-3h", "Más de 3h"));
        var p2 = service.agregarPregunta(encuesta.id(),
                "¿Dónde prefieres estudiar?", List.of("Casa", "Biblioteca", "Café"));
        service.publicar(encuesta.id());
        // Tres votos de ejemplo (solo ids técnicos, nada personal).
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(0).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(2).id(), p2.id(), p2.opciones().get(1).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(1).id()));
    }

    /**
     * ¿QUÉ HACE? Lee las opciones marcadas y registra el voto anónimo.
     * ¿PARA QUÉ? Es el botón principal: convierte lo marcado en un mapa
     * preguntaId -&gt; opcionId y lo envía al servicio, que valida todo.
     * Si falta alguna pregunta por responder, AVISA (anti-parcial) y no vota.
     */
    private void enviarVoto() {
        var preguntas = encuesta.preguntas();
        var seleccion = new HashMap<UUID, UUID>();

        // Recorremos cada pregunta y vemos qué opción marcó el usuario.
        for (int i = 0; i < preguntas.size(); i++) {
            UUID marcada = null;
            for (var radio : botonesPorPregunta.get(i)) {
                if (radio.isSelected()) {
                    marcada = (UUID) radio.getClientProperty("opcionId");
                }
            }
            // Si en alguna pregunta no marcó nada, es voto INCOMPLETO: avisar.
            if (marcada == null) {
                etiquetaEstado.setText("Te falta responder: \"" + preguntas.get(i).texto() + "\"");
                JOptionPane.showMessageDialog(this,
                        "Responde TODAS las preguntas antes de enviar.\nTe falta: \"" + preguntas.get(i).texto() + "\"",
                        "Respuesta incompleta", JOptionPane.WARNING_MESSAGE);
                return;
            }
            seleccion.put(preguntas.get(i).id(), marcada);
        }

        // Todo completo: el servicio valida (opción propia, encuesta abierta...)
        // y guarda el voto anónimo. Aquí solo puede fallar si algo es ajeno.
        try {
            service.responder(encuesta.id(), seleccion);
            etiquetaEstado.setText("¡Gracias! Voto anónimo registrado. Total: "
                    + service.totalRespuestas(encuesta.id()));
            // Limpiamos lo marcado para el siguiente votante (anonimato total).
            grupos.forEach(ButtonGroup::clearSelection);
            refrescarResultados();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Voto rechazado", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ¿QUÉ HACE? Redibuja el panel de resultados con los porcentajes actuales.
     * ¿PARA QUÉ? Para ver en vivo cómo cambian las barras con cada voto.
     * Muestra por opción: nombre, barra de progreso, porcentaje y nº de votos.
     */
    private void refrescarResultados() {
        panelResultados.removeAll();
        panelResultados.setLayout(new GridLayout(0, 1, 0, 12));
        panelResultados.setBackground(Color.WHITE);
        panelResultados.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        var porcentajes = service.resultados(encuesta.id());
        var conteos = service.conteo(encuesta.id());

        for (var pregunta : encuesta.preguntas()) {
            var caja = new JPanel(new GridLayout(0, 1, 4, 4));
            caja.setBackground(Color.WHITE);
            var enunciado = new JLabel(pregunta.texto());
            enunciado.setFont(new Font("Segoe UI", Font.BOLD, 14));
            caja.add(enunciado);

            var pcts = porcentajes.get(pregunta.id());
            var votos = conteos.get(pregunta.id());
            for (var opcion : pregunta.opciones()) {
                double pct = pcts.get(opcion.id());
                long v = votos.get(opcion.id());

                // Primera fila: nombre + porcentaje (votos).
                var fila = new JPanel(new BorderLayout(8, 0));
                fila.setBackground(Color.WHITE);
                var nombre = new JLabel(opcion.texto());
                nombre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                var cifra = new JLabel("%.1f%% (%d)".formatted(pct, v));
                cifra.setFont(new Font("Segoe UI", Font.BOLD, 13));
                fila.add(nombre, BorderLayout.WEST);
                fila.add(cifra, BorderLayout.EAST);
                caja.add(fila);

                // Segunda fila: barra de progreso 0-100.
                var barra = new JProgressBar(0, 100, (int) Math.round(pct));
                barra.setStringPainted(false);
                barra.setForeground(AZUL_ACENTO);
                caja.add(barra);
            }
            panelResultados.add(caja);
        }

        var total = new JLabel("Total de respuestas anónimas: " + service.totalRespuestas(encuesta.id()));
        total.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        panelResultados.add(total);

        panelResultados.revalidate();
        panelResultados.repaint();
    }

    /**
     * Punto de entrada de la VENTANA.
     * Activa el aspecto Nimbus (más moderno) y abre la ventana de forma segura.
     */
    public static void main(String[] args) {
        // Intentamos el aspecto "Nimbus": se ve más presentable que el clásico.
        try {
            for (var tema : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(tema.getName())) {
                    UIManager.setLookAndFeel(tema.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Si falla, se usa el aspecto por defecto; el programa sigue igual.
        }
        // Swing debe iniciarse en su hilo especial (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> new EncuestaGUI().setVisible(true));
    }
}

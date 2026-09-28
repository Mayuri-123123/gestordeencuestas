package com.mycompany.gestordeencuestas;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Es la VENTANA GRÁFICA del programa con estilo de panel de control moderno:
// barra lateral de navegación, tarjetas (cards) blancas con bordes
// redondeados, tipografía clara y gráficos de resultados dibujados a medida.
// Tiene 2 vistas: "Votar" (voto anónimo) y "Resultados" (barras en vivo).
// ----------------------------------------------------------------------------
// NOTA DE ROBUSTEZ: esta versión NO usa JProgressBar. Las barras son el
// componente BarraResultado (dibujado manualmente con valores limitados
// entre 0 y 100 mediante Math.clamp), por lo que la familia de errores
// "rango inválido" de los modelos de rango de Swing ya no puede ocurrir.
// Además, todo acceso por índice está protegido por su tamaño.
// ----------------------------------------------------------------------------
// REGLAS INTACTAS: la ventana solo habla con EncuestaService, así que la
// Regla 1 (publicada no cambia), la Regla 2 (opción válida o rechazo) y la
// Regla 3 (anonimato total, sin pedir datos personales) siguen vigentes.
// ----------------------------------------------------------------------------
// ¿CÓMO EJECUTARLO? En NetBeans: clic derecho aquí -> Run File (Mayús+F6),
// o pulsa F6 (Run Project): este proyecto ya trae nbactions.xml que abre
// directamente esta ventana.
// ============================================================================

/**
 * Panel de control moderno del Gestor de Encuestas: voto anónimo + resultados.
 */
public class EncuestaGUI extends JFrame {

    // ================= Paleta del diseño (tema claro minimalista) =================
    private static final Color FONDO_APP = new Color(241, 244, 249); // gris azulado claro
    private static final Color FONDO_TARJETA = Color.WHITE;          // tarjetas blancas
    private static final Color LATERAL = new Color(22, 35, 58);      // azul noche (sidebar)
    private static final Color LATERAL_SEL = new Color(41, 128, 185);// acento al seleccionar
    private static final Color ACENTO = new Color(41, 128, 185);     // azul botones y barras
    private static final Color VERDE = new Color(39, 174, 96);       // sello "anónimo"
    private static final Color TEXTO = new Color(33, 43, 61);        // texto principal
    private static final Color TEXTO_SUAVE = new Color(120, 134, 155);// texto secundario
    private static final Color BORDE = new Color(226, 232, 240);     // bordes sutiles
    private static final Color PISTA_BARRA = new Color(230, 236, 243);// fondo de las barras

    // Nombres de las vistas del panel central (CardLayout = "baraja de vistas").
    private static final String VISTA_VOTAR = "VOTAR";
    private static final String VISTA_RESULTADOS = "RESULTADOS";

    // ================= Lógica del negocio (la ventana no toca datos directo) =================
    private final EncuestaService service = new EncuestaService();
    private Encuesta encuesta; // encuesta de demostración, siempre PUBLICADA

    // ================= Componentes vivos (se actualizan al votar) =================
    private final CardLayout cartas = new CardLayout();
    private JPanel panelCentral;                 // contenedor de las 2 vistas
    private JPanel vistaVotar;                   // formulario de voto
    private JPanel vistaResultados;              // gráficos de resultados
    private JToggleButton navVotar;              // botón lateral "Votar"
    private JToggleButton navResultados;         // botón lateral "Resultados"
    private JLabel etiquetaTotal;                // "N respuestas" (sidebar + resumen)
    private JLabel etiquetaEstado;               // barra inferior de mensajes
    private final List<ButtonGroup> grupos = new ArrayList<>();          // 1 grupo por pregunta
    private final List<List<JRadioButton>> botonesPorPregunta = new ArrayList<>();

    /** Arma la ventana completa y muestra la vista de votar. */
    public EncuestaGUI() {
        super("Gestor de Encuestas — Panel de control");
        cargarDemo(); // encuesta publicada con 2 preguntas + 3 votos de ejemplo
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setSize(1100, 700);
        setLocationRelativeTo(null); // centrar en la pantalla
        armarContenido();
    }

    // ============================================================================
    // 1) CONSTRUCCIÓN DE LA VENTANA (encabezado + lateral + centro + estado)
    // ============================================================================

    /**
     * ¿QUÉ HACE? Coloca las 4 zonas de la ventana. Se usa al abrir y al
     * reiniciar la demo, para no duplicar código.
     */
    private void armarContenido() {
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

    /**
     * ¿QUÉ HACE? Crea la franja superior: título a la izquierda y, a la
     * derecha, el sello verde "100% ANÓNIMO" + contador de respuestas.
     */
    private JComponent crearEncabezado() {
        var panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO_TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        var titulo = new JLabel("Gestor de Encuestas");
        titulo.setFont(fuente(Font.BOLD, 21));
        titulo.setForeground(TEXTO);
        var subtitulo = new JLabel("Panel de control  ·  Una opción por pregunta  ·  Java 25");
        subtitulo.setFont(fuente(Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SUAVE);
        var textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        // Sello verde tipo "píldora": comunica el anonimato de un vistazo.
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

    /**
     * ¿QUÉ HACE? Crea la barra lateral oscura con la navegación (Votar /
     * Resultados), el contador de votos y el botón de reiniciar demo.
     */
    private JComponent crearLateral() {
        var lateral = new JPanel();
        lateral.setBackground(LATERAL);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        lateral.setPreferredSize(new Dimension(210, 10));

        lateral.add(etiquetaLateral("MENÚ"));
        lateral.add(Box.createVerticalStrut(8));

        // Grupo: solo una vista activa a la vez (como pestañas).
        var grupoNav = new ButtonGroup();
        navVotar = botonLateral("Votar", true);
        navResultados = botonLateral("Resultados", false);
        grupoNav.add(navVotar);
        grupoNav.add(navResultados);
        navVotar.addActionListener(e -> mostrarCarta(VISTA_VOTAR));
        navResultados.addActionListener(e -> {
            refrescarResultados(); // siempre dibuja datos frescos
            mostrarCarta(VISTA_RESULTADOS);
        });
        lateral.add(navVotar);
        lateral.add(Box.createVerticalStrut(6));
        lateral.add(navResultados);
        lateral.add(Box.createVerticalStrut(22));

        lateral.add(etiquetaLateral("ACTIVIDAD"));
        lateral.add(Box.createVerticalStrut(8));
        // Tarjetita con el total de respuestas anónimas registradas.
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

        lateral.add(Box.createVerticalGlue()); // empuja lo siguiente al fondo
        var reiniciar = new JButton("Reiniciar demo");
        reiniciar.setFocusPainted(false);
        reiniciar.setFont(fuente(Font.PLAIN, 12));
        reiniciar.setForeground(new Color(200, 214, 232));
        reiniciar.setBackground(new Color(33, 50, 80));
        reiniciar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        // Restaura la encuesta de ejemplo con sus 3 votos iniciales.
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

    /**
     * ¿QUÉ HACE? Crea el área central con las 2 vistas (votar y resultados).
     * CardLayout muestra UNA a la vez, como un mazo de cartas.
     */
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

    /**
     * ¿QUÉ HACE? Crea la barra inferior de mensajes (confirma votos y avisa
     * de errores de validación sin ventanas emergentes innecesarias).
     */
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
    // 2) VISTA "VOTAR" (tarjetas de preguntas + botón de envío)
    // ============================================================================

    /**
     * ¿QUÉ HACE? Arma el formulario: una tarjeta blanca por pregunta con sus
     * opciones (botones redondos, solo UNO marcable por pregunta) y el botón
     * grande de envío al final.
     */
    private JPanel construirVistaVotar() {
        grupos.clear();
        botonesPorPregunta.clear();
        var vista = new JPanel();
        vista.setBackground(FONDO_APP);
        vista.setLayout(new BoxLayout(vista, BoxLayout.Y_AXIS));

        var preguntas = encuesta.preguntas();
        if (preguntas.isEmpty()) {
            vista.add(tarjetaAviso("Esta encuesta aún no tiene preguntas."));
            return vista;
        }
        // Una tarjeta por pregunta (índice protegido: se usa la lista local).
        for (int i = 0; i < preguntas.size(); i++) {
            vista.add(tarjetaPregunta(preguntas.get(i), i + 1));
            vista.add(Box.createVerticalStrut(14));
        }

        var enviar = new JButton("Enviar respuesta anónima");
        enviar.setFont(fuente(Font.BOLD, 15));
        enviar.setBackground(ACENTO);
        enviar.setForeground(Color.WHITE);
        enviar.setFocusPainted(false);
        enviar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        enviar.setAlignmentX(Component.LEFT_ALIGNMENT);
        enviar.setMaximumSize(new Dimension(340, 52));
        // Al pulsar: valida que todo esté respondido y registra el voto.
        enviar.addActionListener(e -> enviarVoto());
        vista.add(enviar);
        vista.add(Box.createVerticalStrut(6));
        var ayuda = new JLabel("No se pide ni se guarda ningún dato personal: el voto es anónimo por diseño.");
        ayuda.setFont(fuente(Font.ITALIC, 12));
        ayuda.setForeground(TEXTO_SUAVE);
        vista.add(ayuda);
        getRootPane().setDefaultButton(enviar); // Enter también envía
        return vista;
    }

    /**
     * ¿QUÉ HACE? Crea la tarjeta de UNA pregunta: número + enunciado y sus
     * opciones. El id real de cada opción se guarda invisible dentro de su
     * botón para votar con identificadores seguros.
     */
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

        // ButtonGroup = solo se puede marcar UNA opción de esta pregunta.
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
            radio.putClientProperty("opcionId", opcion.id()); // id seguro, invisible
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
    // 3) VISTA "RESULTADOS" (tarjetas resumen + gráficos por pregunta)
    // ============================================================================

    /**
     * ¿QUÉ HACE? Redibuja la vista de resultados con los datos ACTUALES:
     * 3 tarjetas resumen (total, estado, preguntas) y una tarjeta gráfica
     * por pregunta con una barra por opción (porcentaje + votos).
     */
    private void refrescarResultados() {
        vistaResultados.removeAll();
        vistaResultados.setLayout(new BoxLayout(vistaResultados, BoxLayout.Y_AXIS));

        // Fila de tarjetas resumen (total / estado / preguntas).
        var fila = new JPanel(new GridLayout(1, 3, 12, 0));
        fila.setBackground(FONDO_APP);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        fila.add(tarjetaResumen(String.valueOf(totalSeguro()), "respuestas anónimas"));
        fila.add(tarjetaResumen(encuesta.estado().toString(), "estado"));
        fila.add(tarjetaResumen(String.valueOf(encuesta.preguntas().size()), "preguntas"));
        vistaResultados.add(fila);
        vistaResultados.add(Box.createVerticalStrut(14));

        // Una tarjeta gráfica por pregunta (índices siempre verificados).
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

    /** ¿QUÉ HACE? Crea una mini-tarjeta de resumen (número grande + etiqueta). */
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

    /**
     * ¿QUÉ HACE? Crea la tarjeta gráfica de UNA pregunta: por cada opción,
     * una fila con nombre, votos, porcentaje y barra estilizada.
     * Los mapas se consultan con getOrDefault: si un id faltara, se muestra
     * 0 en lugar de fallar (programación defensiva).
     */
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
            // getOrDefault evita NullPointerException si un id no existiera.
            double pct = pcts.getOrDefault(opcion.id(), 0.0);
            long v = votos.getOrDefault(opcion.id(), 0L);

            var fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);
            var nombre = new JLabel(opcion.texto());
            nombre.setFont(fuente(Font.PLAIN, 13));
            nombre.setForeground(TEXTO);
            nombre.setPreferredSize(new Dimension(140, 20));
            // Texto seguro: si el % viniera corrupto, se muestra 0.0 (nunca NaN).
            var cifra = new JLabel("%s  ·  %d voto%s".formatted(pctSeguro(pct), v, (v == 1 ? "" : "s")));
            cifra.setFont(fuente(Font.BOLD, 13));
            cifra.setForeground(TEXTO);
            fila.add(nombre, BorderLayout.WEST);
            fila.add(cifra, BorderLayout.EAST);
            tarjeta.add(fila);
            tarjeta.add(Box.createVerticalStrut(4));

            // Barra dibujada a medida (sin JProgressBar: sin errores de rango).
            var barra = new BarraResultado();
            barra.setPorcentaje(pct);
            tarjeta.add(barra);
            tarjeta.add(Box.createVerticalStrut(10));
        }
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjeta.getPreferredSize().height + 60));
        return tarjeta;
    }

    // ============================================================================
    // 4) LÓGICA DE LA VENTANA (demo, voto, navegación y mensajes)
    // ============================================================================

    /**
     * ¿QUÉ HACE? Prepara la encuesta de ejemplo: la crea, le agrega 2
     * preguntas, la PUBLICA (Regla 1: queda congelada) y registra 3 votos
     * anónimos iniciales solo con ids técnicos (Regla 3).
     */
    private void cargarDemo() {
        encuesta = service.crearEncuesta("Hábitos de estudio");
        var p1 = service.agregarPregunta(encuesta.id(),
                "¿Cuántas horas estudias al día?", List.of("Menos de 1h", "1-3h", "Más de 3h"));
        var p2 = service.agregarPregunta(encuesta.id(),
                "¿Dónde prefieres estudiar?", List.of("Casa", "Biblioteca", "Café"));
        service.publicar(encuesta.id());
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(0).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(2).id(), p2.id(), p2.opciones().get(1).id()));
        service.responder(encuesta.id(), Map.of(p1.id(), p1.opciones().get(1).id(), p2.id(), p2.opciones().get(1).id()));
    }

    /**
     * ¿QUÉ HACE? Lee lo marcado y registra el voto anónimo.
     * ¿CÓMO PROTEGE LAS REGLAS?
     * - Si falta alguna pregunta: AVISA y no vota (anti-parcial, defensa).
     * - service.responder valida opción propia y encuesta publicada
     *   (Reglas 1 y 2): lo ajeno se rechaza y se muestra el motivo.
     * - Jamás se pide un dato personal (Regla 3).
     */
    private void enviarVoto() {
        var preguntas = encuesta.preguntas();
        // Defensa: las listas de botones siempre deben calzar con las preguntas.
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
            // Anti-parcial: cada pregunta es obligatoria (opción única completa).
            if (marcada == null) {
                String falta = preguntas.get(i).texto();
                mensaje("Te falta responder: \"" + falta + "\"");
                JOptionPane.showMessageDialog(this,
                        "Responde TODAS las preguntas antes de enviar.\nTe falta: \"" + falta + "\"",
                        "Respuesta incompleta", JOptionPane.WARNING_MESSAGE);
                return;
            }
            seleccion.put(preguntas.get(i).id(), marcada);
        }
        try {
            service.responder(encuesta.id(), seleccion); // validación estricta aquí
            grupos.forEach(g -> g.clearSelection());    // limpia para el siguiente votante
            refrescarResultados();
            mostrarCarta(VISTA_RESULTADOS);
            navResultados.setSelected(true);
            mensaje("¡Gracias! Voto anónimo registrado. Total: " + totalSeguro() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje("Voto rechazado: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Voto rechazado", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** ¿QUÉ HACE? Muestra la vista indicada y marca su botón lateral. */
    private void mostrarCarta(String nombre) {
        cartas.show(panelCentral, nombre);
    }

    /** ¿QUÉ HACE? Atajo para ir a la vista de votar. */
    private void irAVotar() {
        navVotar.setSelected(true);
        mostrarCarta(VISTA_VOTAR);
        actualizarTotal();
    }

    /** ¿QUÉ HACE? Actualiza el contador del lateral con el total real. */
    private void actualizarTotal() {
        etiquetaTotal.setText(String.valueOf(totalSeguro()));
    }

    /** ¿QUÉ HACE? Total de votos, o 0 si la encuesta no existiera. */
    private int totalSeguro() {
        try {
            return service.totalRespuestas(encuesta.id());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    /** ¿QUÉ HACE? Escribe un mensaje en la barra inferior de estado. */
    private void mensaje(String texto) {
        etiquetaEstado.setText(texto == null ? "" : texto);
    }

    // ============================================================================
    // 5) PEQUEÑAS AYUDAS VISUALES (fuentes, etiquetas, tarjetas de aviso)
    // ============================================================================

    /** ¿QUÉ HACE? Crea la fuente de la app (clara y legible). */
    private static Font fuente(int estilo, int tamanio) {
        return new Font("Segoe UI", estilo, tamanio);
    }

    /** ¿QUÉ HACE? Etiqueta pequeña gris para los títulos del lateral. */
    private static JLabel etiquetaLateral(String texto) {
        var l = new JLabel(texto);
        l.setFont(fuente(Font.BOLD, 11));
        l.setForeground(new Color(130, 150, 180));
        return l;
    }

    /** ¿QUÉ HACE? Botón de navegación del lateral (claro cuando está activo). */
    private static JToggleButton botonLateral(String texto, boolean activo) {
        var b = new JToggleButton(texto, activo);
        b.setFocusPainted(false);
        b.setFont(fuente(Font.BOLD, 14));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setMaximumSize(new Dimension(220, 40));
        // Colores distintos según esté seleccionado o no (se actualizan solos).
        b.setBackground(activo ? LATERAL_SEL : LATERAL);
        b.setForeground(Color.WHITE);
        b.addChangeListener(e -> b.setBackground(b.isSelected() ? LATERAL_SEL : LATERAL));
        return b;
    }

    /** ¿QUÉ HACE? Tarjeta de aviso para casos vacíos (ej: sin preguntas). */
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
    // 6) COMPONENTES A MEDIDA (tarjeta redondeada + barra sin errores de rango)
    // ============================================================================

    /**
     * ¿QUÉ ES? Un JPanel con fondo de esquinas redondeadas.
     * ¿PARA QUÉ? Da el aspecto moderno de "tarjetas" del panel de control.
     */
    private static final class RoundedPanel extends JPanel {
        private final int radio; // qué tan redondas son las esquinas
        private final Color fondo;

        RoundedPanel(int radio, Color fondo) {
            super();
            this.radio = Math.clamp(radio, 0, 60); // valor siempre válido
            this.fondo = fondo;
            setOpaque(false); // transparente: solo se ve el redondeado
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // primero lo normal (casi nada, es transparente)
            var g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON); // bordes suaves
                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radio, radio);
            } finally {
                g2.dispose(); // liberar recursos gráficos (buena práctica)
            }
        }
    }

    /**
     * ¿QUÉ ES? Una barra de progreso dibujada a mano (fondo + relleno).
     * ¿PARA QUÉ? Muestra el % de cada opción con estilo propio.
     * ¿POR QUÉ ES MÁS SEGURA? No usa el modelo de rango de Swing: el valor
     * se limita con Math.clamp(0..100) y el ancho se calcula con aritmética
     * protegida, así que el error "rango inválido" no puede ocurrir.
     */
    private static final class BarraResultado extends JComponent {
        private double porcentaje; // siempre entre 0 y 100

        BarraResultado() {
            this.porcentaje = 0.0;
            setPreferredSize(new Dimension(10, 14));
            setMinimumSize(new Dimension(10, 14));
            // Texto accesible (protegido: en algunos contextos puede ser null).
            var acc = getAccessibleContext();
            if (acc != null) {
                acc.setAccessibleName("Barra de porcentaje");
            }
        }

        /** Fija el valor limitándolo a [0, 100]; acepta NaN/infinitos sin romperse. */
        void setPorcentaje(double valor) {
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                valor = 0.0; // valor corrupto -> se muestra 0 en vez de fallar
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = Math.max(0, getWidth());
                int h = Math.max(0, getHeight());
                int radio = Math.clamp(h, 0, 20);
                // 1) Pista (fondo gris claro a todo lo ancho).
                g2.setColor(PISTA_BARRA);
                g2.fillRoundRect(0, 0, w, h, radio, radio);
                // 2) Relleno azul proporcional, limitado al ancho real.
                int relleno = (int) Math.round(w * (porcentaje / 100.0));
                relleno = Math.clamp(relleno, 0, w);
                if (relleno > 0 && h > 0) {
                    g2.setColor(ACENTO);
                    g2.fillRoundRect(0, 0, relleno, h, radio, radio);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ============================================================================
    // 7) ARRANQUE DEL PROGRAMA (aspecto nativo + hilo seguro de Swing)
    // ============================================================================

    /**
     * Punto de entrada de la VENTANA.
     * Usa el aspecto NATIVO del sistema (se ve moderno en Windows) y abre la
     * ventana en el hilo especial de Swing (Event Dispatch Thread).
     */
    public static void main(String[] args) {
        try {
            // Aspecto nativo de Windows: botones y radios modernos del sistema.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, Swing usa su aspecto por defecto; el programa sigue igual.
        }
        SwingUtilities.invokeLater(() -> new EncuestaGUI().setVisible(true));
    }

    /**
     * Texto de porcentaje seguro para la vista: nunca muestra "NaN" ni
     * valores fuera de 0-100 (los limita y formatea con 1 decimal).
     */
    private static String pctSeguro(double pct) {
        if (Double.isNaN(pct) || Double.isInfinite(pct)) {
            pct = 0.0;
        }
        return "%.1f%%".formatted(Math.clamp(pct, 0.0, 100.0));
    }
}

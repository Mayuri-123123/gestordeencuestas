package com.mycompany.gestordeencuestas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * APLICACIÓN PRINCIPAL - GESTOR DE ENCUESTAS
 * Menú interactivo para consola
 * Integrante: Yo (rama pruebas)
 * 
 * Módulos:
 * - Módulo A: Creación y publicación de encuestas
 * - Módulo B: Respuestas y estadísticas
 * - Integración: Contrato de validación de respuestas
 */
public class GestorDeEncuestas {
    
    private EncuestaService encuestaService;
    private RespuestaService respuestaService;
    private ValidacionService validacionService;
    private Scanner scanner;

    public GestorDeEncuestas() {
        this.encuestaService = new EncuestaService();
        this.respuestaService = new RespuestaService(encuestaService);
        this.validacionService = new ValidacionService();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        GestorDeEncuestas app = new GestorDeEncuestas();
        app.ejecutar();
    }

    public void ejecutar() {
        mostrarBanner();
        
        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            int opcion = leerOpcion();
            
            switch (opcion) {
                case 1: crearEncuesta(); break;
                case 2: verEncuestas(); break;
                case 3: responderEncuesta(); break;
                case 4: verResultados(); break;
                case 5: verEstadisticas(); break;
                case 6: eliminarEncuesta(); break;
                case 0: salir = true; break;
                default: System.out.println("\n❌ Opción no válida. Intenta de nuevo.\n");
            }
        }
        
        System.out.println("\n👋 ¡Gracias por usar el Gestor de Encuestas!\n");
    }

    private void mostrarBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                            ║");
        System.out.println("║           📊 GESTOR DE ENCUESTAS PROFESIONAL 📊           ║");
        System.out.println("║                                                            ║");
        System.out.println("║     Crea, publica y analiza encuestas de forma fácil      ║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void mostrarMenu() {
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│  MENÚ PRINCIPAL                                            │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.println("│  [1] 📝 Crear nueva encuesta                               │");
        System.out.println("│  [2] 📋 Ver todas las encuestas                             │");
        System.out.println("│  [3] ✏️  Responder encuesta                                 │");
        System.out.println("│  [4] 📊 Ver resultados de encuesta                          │");
        System.out.println("│  [5] 📈 Ver estadísticas generales                          │");
        System.out.println("│  [6] 🗑️  Eliminar encuesta                                  │");
        System.out.println("│  [0] 🚪 Salir                                               │");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        System.out.print("\n👉 Selecciona una opción: ");
    }

    private int leerOpcion() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ============================================
    // MÓDULO A: CREAR ENCUESTA
    // ============================================
    private void crearEncuesta() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    📝 CREAR NUEVA ENCUESTA");
        System.out.println("════════════════════════════════════════════════════════════\n");

        // Título
        System.out.print("📌 Título de la encuesta: ");
        String titulo = scanner.nextLine().trim();
        if (titulo.length() < 5) {
            System.out.println("\n❌ El título debe tener al menos 5 caracteres.\n");
            return;
        }

        // Descripción
        System.out.print("📄 Descripción: ");
        String descripcion = scanner.nextLine().trim();
        if (descripcion.length() < 10) {
            System.out.println("\n❌ La descripción debe tener al menos 10 caracteres.\n");
            return;
        }

        // Categoría
        System.out.println("\n📂 Categorías disponibles:");
        System.out.println("   [1] Comida");
        System.out.println("   [2] Productos");
        System.out.println("   [3] Belleza");
        System.out.println("   [4] Moda");
        System.out.println("   [5] Tecnología");
        System.out.println("   [6] Deportes");
        System.out.println("   [7] Viajes");
        System.out.println("   [8] General");
        System.out.print("\n👉 Selecciona categoría (1-8): ");
        
        String[] categorias = {"comida", "productos", "belleza", "moda", "tecnologia", "deportes", "viajes", "general"};
        int catIndex;
        try {
            catIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (catIndex < 0 || catIndex >= categorias.length) {
                System.out.println("\n❌ Categoría no válida.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Opción no válida.\n");
            return;
        }
        String categoria = categorias[catIndex];

        // Crear encuesta
        Encuesta encuesta = encuestaService.crearEncuesta(titulo, descripcion, categoria);

        // Agregar preguntas
        boolean agregarPreguntas = true;
        int numPregunta = 1;
        while (agregarPreguntas) {
            System.out.println("\n────────────────────────────────────────────────────────────");
            System.out.println("              PREGUNTA #" + numPregunta);
            System.out.println("────────────────────────────────────────────────────────────");
            
            System.out.print("❓ Texto de la pregunta: ");
            String textoPregunta = scanner.nextLine().trim();
            if (textoPregunta.length() < 5) {
                System.out.println("\n❌ La pregunta debe tener al menos 5 caracteres.");
                continue;
            }

            Pregunta pregunta = new Pregunta(textoPregunta);

            // Agregar opciones
            boolean agregarOpciones = true;
            int numOpcion = 1;
            while (agregarOpciones) {
                System.out.print("   Opción " + numOpcion + ": ");
                String opcion = scanner.nextLine().trim();
                if (opcion.isEmpty()) {
                    System.out.println("   ⚠️ La opción no puede estar vacía.");
                    continue;
                }
                pregunta.agregarOpcion(opcion);
                numOpcion++;

                if (numOpcion > 2) {
                    System.out.print("   ¿Agregar otra opción? (s/n): ");
                    String continuar = scanner.nextLine().trim().toLowerCase();
                    if (!continuar.equals("s")) {
                        agregarOpciones = false;
                    }
                }
            }

            encuesta.agregarPregunta(pregunta);
            numPregunta++;

            System.out.print("\n¿Agregar otra pregunta? (s/n): ");
            String continuar = scanner.nextLine().trim().toLowerCase();
            if (!continuar.equals("s")) {
                agregarPreguntas = false;
            }
        }

        // Validar y publicar
        if (validacionService.validarEncuestaParaPublicar(encuesta)) {
            try {
                encuestaService.publicarEncuesta(encuesta.getId());
                System.out.println("\n✅ ¡Encuesta publicada exitosamente!");
                System.out.println("   ID: " + encuesta.getId());
                System.out.println("   Título: " + encuesta.getTitulo());
                System.out.println("   Preguntas: " + encuesta.getPreguntas().size());
            } catch (Exception e) {
                System.out.println("\n❌ Error al publicar: " + e.getMessage());
            }
        } else {
            System.out.println("\n❌ Errores de validación:");
            for (String error : validacionService.obtenerErrores()) {
                System.out.println("   • " + error);
            }
        }
        System.out.println();
    }

    // ============================================
    // VER ENCUESTAS
    // ============================================
    private void verEncuestas() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    📋 LISTA DE ENCUESTAS");
        System.out.println("════════════════════════════════════════════════════════════\n");

        List<Encuesta> encuestas = encuestaService.obtenerEncuestas();
        if (encuestas.isEmpty()) {
            System.out.println("📭 No hay encuestas registradas.\n");
            return;
        }

        System.out.println("┌──────┬────────────────────────────────────┬─────────────┬──────────┐");
        System.out.println("│  ID  │ Título                             │ Categoría   │ Estado   │");
        System.out.println("├──────┼────────────────────────────────────┼─────────────┼──────────┤");
        
        for (Encuesta e : encuestas) {
            String estado = e.isPublicada() ? "✅ Publicada" : "📝 Borrador";
            System.out.printf("│ %s │ %-34s │ %-11s │ %-8s │%n", 
                e.getId(), 
                e.getTitulo().length() > 34 ? e.getTitulo().substring(0, 31) + "..." : e.getTitulo(),
                e.getCategoria(),
                estado);
        }
        System.out.println("└──────┴────────────────────────────────────┴─────────────┴──────────┘");
        System.out.println();
    }

    // ============================================
    // MÓDULO B: RESPONDER ENCUESTA
    // ============================================
    private void responderEncuesta() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    ✏️ RESPONDER ENCUESTA");
        System.out.println("════════════════════════════════════════════════════════════\n");

        List<Encuesta> publicadas = encuestaService.obtenerEncuestasPublicadas();
        if (publicadas.isEmpty()) {
            System.out.println("📭 No hay encuestas publicadas para responder.\n");
            return;
        }

        System.out.println("Encuestas disponibles:");
        for (int i = 0; i < publicadas.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + publicadas.get(i).getTitulo());
        }
        System.out.print("\n👉 Selecciona una encuesta (1-" + publicadas.size() + "): ");
        
        int seleccion;
        try {
            seleccion = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (seleccion < 0 || seleccion >= publicadas.size()) {
                System.out.println("\n❌ Selección no válida.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Opción no válida.\n");
            return;
        }

        Encuesta encuesta = publicadas.get(seleccion);
        System.out.println("\n📋 " + encuesta.getTitulo());
        System.out.println("   " + encuesta.getDescripcion());
        System.out.println("   🔒 Tus respuestas son 100% anónimas\n");

        Map<String, Integer> respuestas = new HashMap<>();

        for (int i = 0; i < encuesta.getPreguntas().size(); i++) {
            Pregunta pregunta = encuesta.getPreguntas().get(i);
            System.out.println("────────────────────────────────────────────────────────────");
            System.out.println((i + 1) + ". " + pregunta.getTexto());
            
            for (int j = 0; j < pregunta.getOpciones().size(); j++) {
                System.out.println("   [" + (j + 1) + "] " + pregunta.getOpciones().get(j));
            }
            
            System.out.print("   Tu respuesta (1-" + pregunta.getOpciones().size() + "): ");
            try {
                int opcion = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (opcion < 0 || opcion >= pregunta.getOpciones().size()) {
                    System.out.println("   ❌ Opción no válida. Se omitirá esta pregunta.");
                    continue;
                }
                respuestas.put(pregunta.getId(), opcion);
            } catch (NumberFormatException e) {
                System.out.println("   ❌ Entrada no válida. Se omitirá esta pregunta.");
            }
        }

        // Validar y registrar
        if (validacionService.validarRespuestasCompletas(encuesta, respuestas)) {
            try {
                respuestaService.registrarRespuestas(encuesta.getId(), respuestas);
                System.out.println("\n✅ ¡Gracias por participar! Tus respuestas han sido registradas.");
            } catch (Exception e) {
                System.out.println("\n❌ Error: " + e.getMessage());
            }
        } else {
            System.out.println("\n❌ Errores de validación:");
            for (String error : validacionService.obtenerErrores()) {
                System.out.println("   • " + error);
            }
        }
        System.out.println();
    }

    // ============================================
    // VER RESULTADOS
    // ============================================
    private void verResultados() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    📊 RESULTADOS DE ENCUESTA");
        System.out.println("════════════════════════════════════════════════════════════\n");

        List<Encuesta> publicadas = encuestaService.obtenerEncuestasPublicadas();
        if (publicadas.isEmpty()) {
            System.out.println("📭 No hay encuestas publicadas.\n");
            return;
        }

        System.out.println("Encuestas disponibles:");
        for (int i = 0; i < publicadas.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + publicadas.get(i).getTitulo());
        }
        System.out.print("\n👉 Selecciona una encuesta (1-" + publicadas.size() + "): ");
        
        int seleccion;
        try {
            seleccion = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (seleccion < 0 || seleccion >= publicadas.size()) {
                System.out.println("\n❌ Selección no válida.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Opción no válida.\n");
            return;
        }

        Encuesta encuesta = publicadas.get(seleccion);
        
        try {
            List<RespuestaService.EstadisticaPregunta> stats = respuestaService.calcularEstadisticas(encuesta.getId());
            
            System.out.println("\n┌────────────────────────────────────────────────────────────┐");
            System.out.println("│  RESULTADOS: " + String.format("%-43s", encuesta.getTitulo()) + "│");
            System.out.println("└────────────────────────────────────────────────────────────┘");

            for (int i = 0; i < stats.size(); i++) {
                RespuestaService.EstadisticaPregunta stat = stats.get(i);
                System.out.println("\n" + (i + 1) + ". " + stat.getPregunta());
                System.out.println("   " + "─".repeat(50));

                for (RespuestaService.OpcionEstadistica op : stat.getOpciones()) {
                    int barLength = (int) (op.getPorcentaje() / 2);
                    String bar = "█".repeat(Math.max(0, barLength));
                    System.out.printf("   %-20s │%-25s│ %5.1f%% (%d votos)%n", 
                        op.getOpcion(), bar, op.getPorcentaje(), op.getCount());
                }
            }
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        }
        System.out.println();
    }

    // ============================================
    // VER ESTADÍSTICAS GENERALES
    // ============================================
    private void verEstadisticas() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    📈 ESTADÍSTICAS GENERALES");
        System.out.println("════════════════════════════════════════════════════════════\n");

        int totalEncuestas = encuestaService.obtenerTotalEncuestas();
        int totalPreguntas = encuestaService.obtenerTotalPreguntas();
        int totalRespuestas = respuestaService.obtenerTotalRespuestas();

        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                            │");
        System.out.printf("│   📋 Total de encuestas:    %d%n", totalEncuestas);
        System.out.printf("│   ❓ Total de preguntas:     %d%n", totalPreguntas);
        System.out.printf("│   ✏️  Total de respuestas:   %d%n", totalRespuestas);
        System.out.println("│                                                            │");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    // ============================================
    // ELIMINAR ENCUESTA
    // ============================================
    private void eliminarEncuesta() {
        System.out.println("\n════════════════════════════════════════════════════════════");
        System.out.println("                    🗑️ ELIMINAR ENCUESTA");
        System.out.println("════════════════════════════════════════════════════════════\n");

        List<Encuesta> encuestas = encuestaService.obtenerEncuestas();
        if (encuestas.isEmpty()) {
            System.out.println("📭 No hay encuestas para eliminar.\n");
            return;
        }

        System.out.println("Encuestas registradas:");
        for (int i = 0; i < encuestas.size(); i++) {
            System.out.println("   [" + (i + 1) + "] " + encuestas.get(i).getTitulo());
        }
        System.out.print("\n👉 Selecciona una encuesta para eliminar (1-" + encuestas.size() + "): ");
        
        int seleccion;
        try {
            seleccion = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (seleccion < 0 || seleccion >= encuestas.size()) {
                System.out.println("\n❌ Selección no válida.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Opción no válida.\n");
            return;
        }

        Encuesta encuesta = encuestas.get(seleccion);
        System.out.print("⚠️ ¿Estás seguro de eliminar '" + encuesta.getTitulo() + "'? (s/n): ");
        String confirmacion = scanner.nextLine().trim().toLowerCase();
        
        if (confirmacion.equals("s")) {
            encuestaService.eliminarEncuesta(encuesta.getId());
            System.out.println("\n✅ Encuesta eliminada exitosamente.\n");
        } else {
            System.out.println("\n❌ Operación cancelada.\n");
        }
    }
}

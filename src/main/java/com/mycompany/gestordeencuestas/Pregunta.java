package com.mycompany.gestordeencuestas;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pregunta de opción única. Inmutable tras su construcción.
 *
 * <p>Regla 2 del dominio (apoyo): conoce sus propias {@link Opcion}es y
 * ofrece {@link #contieneOpcion(UUID)} para la validación estricta de
 * pertenencia que realiza {@link EncuestaService#responder}.
 *
 * <p>Privacidad: solo texto de la pregunta y opciones. Sin datos personales.
 */
public final class Pregunta {

    private final UUID id;
    private final String texto;
    private final List<Opcion> opciones;

    public Pregunta(UUID id, String texto, List<Opcion> opciones) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(texto, "texto no puede ser null");
        Objects.requireNonNull(opciones, "opciones no puede ser null");
        String limpio = texto.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("texto de pregunta no puede estar vacío");
        }
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("texto de pregunta supera 500 caracteres");
        }
        if (opciones.size() < 2) {
            throw new IllegalArgumentException("una pregunta requiere al menos 2 opciones");
        }
        if (opciones.size() > 10) {
            throw new IllegalArgumentException("una pregunta admite como máximo 10 opciones");
        }
        // Ids de opción únicos (bloquea duplicados / inyección del mismo id).
        var ids = new HashSet<UUID>();
        for (var o : opciones) {
            Objects.requireNonNull(o, "opción no puede ser null");
            if (!ids.add(o.id())) {
                throw new IllegalArgumentException("opciones duplicadas (id repetido): " + o.id());
            }
        }
        this.texto = limpio;
        // Copia defensiva inmodificable: el exterior nunca muta el interior.
        this.opciones = List.copyOf(opciones);
    }

    /** Factoría segura: ids generados por el sistema a partir de textos. */
    public static Pregunta de(String texto, List<String> textosOpciones) {
        Objects.requireNonNull(textosOpciones, "textosOpciones no puede ser null");
        var opciones = textosOpciones.stream()
                .map(Opcion::de)
                .toList();
        return new Pregunta(UUID.randomUUID(), texto, opciones);
    }

    public UUID id() {
        return id;
    }

    public String texto() {
        return texto;
    }

    /** Vista inmodificable de las opciones. */
    public List<Opcion> opciones() {
        return opciones; // List.copyOf ya es inmodificable
    }

    /**
     * Verificación estricta de pertenencia: {@code true} solo si la opción
     * fue creada para ESTA pregunta (comparación por UUID).
     */
    public boolean contieneOpcion(UUID opcionId) {
        Objects.requireNonNull(opcionId, "opcionId no puede ser null");
        return opciones.stream().anyMatch(o -> o.id().equals(opcionId));
    }

    /** Devuelve la opción o lanza si es ajena a esta pregunta. */
    public Opcion obtenerOpcion(UUID opcionId) {
        return buscarOpcion(opcionId).orElseThrow(() ->
                new IllegalArgumentException(
                        "Opción ajena o manipulada: " + opcionId
                        + " no pertenece a la pregunta " + id));
    }

    public Optional<Opcion> buscarOpcion(UUID opcionId) {
        return opciones.stream().filter(o -> o.id().equals(opcionId)).findFirst();
    }

    // Identidad por UUID (coherente con el uso en Mapas del servicio).
    @Override
    public boolean equals(Object o) {
        return (o instanceof Pregunta otra) && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Pregunta[id=%s, texto=%s, opciones=%d]".formatted(id, texto, opciones.size());
    }
}

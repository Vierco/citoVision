package dev.lovelace.citovision.domain.entities

/**
 * Prioridad de revisión profesional de una muestra (SPEC-0006, RN-9). **No es un diagnóstico**: es una
 * sugerencia de orden de revisión para un profesional humano, derivada de la relevancia morfológica de las
 * células detectadas ([PriorityCalculator]). [label] es el texto que aparece en el resumen persistido.
 */
enum class Priority(
    val label: String,
) {
    BAJA("BAJA"),
    MEDIA("MEDIA"),
    ALTA("ALTA"),
}

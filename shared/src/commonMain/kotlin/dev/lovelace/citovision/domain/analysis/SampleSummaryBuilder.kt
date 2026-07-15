package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.Priority

/**
 * Genera el resumen textual descriptivo del análisis (SPEC-0006, RF-3), **sin lenguaje diagnóstico**. Separa
 * los hallazgos con peso morfológico (`> 0`, agrupación por relevancia clínica) del resto de tipos celulares
 * y clases no celulares. Es **contenido generado y persistido**, no un texto de interfaz.
 */
object SampleSummaryBuilder {
    fun build(
        detections: List<Detection>,
        priority: Priority,
    ): String {
        val tally = CellTally.of(detections)
        val relevant = tally.presentSortedWhere { it.priorityWeight > 0 }
        val others = tally.presentSortedWhere { it.priorityWeight == 0 }
        return buildString {
            append("${tally.totalCells} células detectadas. ")
            append("Prioridad de revisión: ${priority.label}.")
            if (relevant.isNotEmpty()) {
                val list = relevant.joinToString { entry(it, tally) }
                append(" Hallazgos principalmente relevantes: $list.")
            }
            if (others.isNotEmpty()) {
                val list = others.joinToString { entry(it, tally) }
                append(" Otros hallazgos encontrados en la revisión: $list.")
            }
        }
    }

    private fun CellTally.presentSortedWhere(predicate: (CellClass) -> Boolean): List<CellClass> =
        counts.keys
            .filter(predicate)
            .sortedWith(compareByDescending<CellClass> { counts.getValue(it) }.thenBy { it.index })

    private fun entry(
        cellClass: CellClass,
        tally: CellTally,
    ): String = "${cellClass.label} ${tally.counts.getValue(cellClass)}"
}

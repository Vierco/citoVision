package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.Detection

/**
 * Construye el conteo celular (`List<CellCount>`) a partir de las detecciones (SPEC-0006, RF-2). Cada tipo
 * presente produce una entrada con `value` en formato `"N (P%)"` (o `"N"` para clases no celulares, RN-6),
 * ordenada por recuento descendente y, a igualdad, por índice de clase.
 */
object CellCountBuilder {
    fun build(detections: List<Detection>): List<CellCount> {
        val tally = CellTally.of(detections)
        return tally.counts.keys
            .sortedWith(compareByDescending<CellClass> { tally.counts.getValue(it) }.thenBy { it.index })
            .map { CellCount(name = it.label, value = tally.formattedValue(it)) }
    }
}

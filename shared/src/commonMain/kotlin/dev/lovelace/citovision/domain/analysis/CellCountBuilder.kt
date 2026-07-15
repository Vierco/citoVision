package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.Detection

/**
 * Construye el conteo celular (`List<CellCount>`) a partir de las detecciones (SPEC-0006, RF-2). Cada tipo
 * presente produce una entrada con su recuento y, **solo para tipos celulares reales**, la lista de
 * confianzas por célula ordenada de mayor a menor (las clases no celulares quedan sin confianzas, RN-6). El
 * resultado lista **primero las células reales y al final las no celulares** (Artefacto, Restos); dentro de
 * cada grupo, por recuento descendente y, a igualdad, por índice de clase.
 */
object CellCountBuilder {
    fun build(detections: List<Detection>): List<CellCount> =
        detections
            .groupBy { it.cellClass }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<CellClass, List<Detection>>> { it.key.isCell }
                    .thenByDescending { it.value.size }
                    .thenBy { it.key.index },
            ).map { (cellClass, group) ->
                val confidences =
                    if (cellClass.isCell) group.map { it.confidence }.sortedDescending() else emptyList()
                CellCount(name = cellClass.label, count = group.size, confidences = confidences)
            }
}

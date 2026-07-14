package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import kotlin.math.roundToInt

/**
 * Recuento por tipo derivado de las detecciones (SPEC-0006). [counts] solo contiene los tipos presentes;
 * [totalCells] es el denominador del porcentaje: **suma de instancias de clases celulares reales** (RN-6),
 * de modo que `Artefacto` y `Restos celulares` no inflan los porcentajes.
 */
data class CellTally(
    val counts: Map<CellClass, Int>,
    val totalCells: Int,
) {
    /** Valor mostrado (RF-2): `"N (P%)"` para células reales; `"N"` para clases no celulares o sin total. */
    fun formattedValue(cellClass: CellClass): String {
        val count = counts[cellClass] ?: 0
        return if (cellClass.isCell && totalCells > 0) {
            "$count (${percentage(count)}%)"
        } else {
            "$count"
        }
    }

    private fun percentage(count: Int): Int = (count * PERCENT_SCALE / totalCells).roundToInt()

    companion object {
        private const val PERCENT_SCALE = 100.0

        fun of(detections: List<Detection>): CellTally {
            val counts = detections.groupingBy { it.cellClass }.eachCount()
            val totalCells = counts.filterKeys { it.isCell }.values.sum()
            return CellTally(counts = counts, totalCells = totalCells)
        }
    }
}

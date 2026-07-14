package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.Detection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Conteo celular (SPEC-0006, RF-2/RN-6): `"N (P%)"` para células reales, `"N"` para no-células, porcentaje
 * solo sobre el total de células reales, orden por recuento descendente y luego por índice.
 */
class CellCountBuilderTest {
    @Test
    fun `given cells and a non-cell when building then only real cells enter the percentage denominator`() {
        val detections =
            detectionsOf(
                CellClass.NEUTROFILO_SEGMENTADO to 6,
                CellClass.BLASTO to 2,
                CellClass.LINFOCITO to 2,
                CellClass.ARTEFACTO to 1,
            )
        // Total de células reales = 10 (el artefacto no cuenta para el %).
        assertEquals(
            listOf(
                CellCount("Neutrófilo segmentado", "6 (60%)"),
                CellCount("Blasto", "2 (20%)"),
                CellCount("Linfocito", "2 (20%)"),
                CellCount("Artefacto", "1"),
            ),
            CellCountBuilder.build(detections),
        )
    }

    @Test
    fun `given counts that do not divide evenly when building then percentages are rounded`() {
        val detections = detectionsOf(CellClass.BLASTO to 2, CellClass.MIELOCITO to 3, CellClass.LINFOCITO to 7)
        // Total 12 → 7/12=58%, 3/12=25%, 2/12=17%.
        assertEquals(
            listOf(
                CellCount("Linfocito", "7 (58%)"),
                CellCount("Mielocito", "3 (25%)"),
                CellCount("Blasto", "2 (17%)"),
            ),
            CellCountBuilder.build(detections),
        )
    }

    private fun detectionsOf(vararg pairs: Pair<CellClass, Int>): List<Detection> =
        pairs.flatMap { (cellClass, count) -> List(count) { detection(cellClass) } }

    private fun detection(cellClass: CellClass): Detection =
        Detection(cellClass = cellClass, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f))
}

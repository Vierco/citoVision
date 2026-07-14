package dev.lovelace.citovision.domain.analysis

import dev.lovelace.citovision.domain.entities.BoundingBox
import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.Priority
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Resumen descriptivo (SPEC-0006, RF-3): agrupa por relevancia clínica (peso > 0) y omite secciones vacías.
 */
class SampleSummaryBuilderTest {
    @Test
    fun `given relevant and habitual findings when building then it groups by clinical relevance`() {
        val detections = detectionsOf(CellClass.BLASTO to 2, CellClass.MIELOCITO to 3, CellClass.LINFOCITO to 7)
        assertEquals(
            "12 células detectadas. Prioridad de revisión: ALTA. " +
                "Hallazgos principalmente relevantes: Mielocito 3 (25%), Blasto 2 (17%). " +
                "Otros hallazgos encontrados en la revisión: Linfocito 7 (58%).",
            SampleSummaryBuilder.build(detections, Priority.ALTA),
        )
    }

    @Test
    fun `given only habitual cells when building then the relevant section is omitted`() {
        val detections = detectionsOf(CellClass.LINFOCITO to 5)
        assertEquals(
            "5 células detectadas. Prioridad de revisión: BAJA. " +
                "Otros hallazgos encontrados en la revisión: Linfocito 5 (100%).",
            SampleSummaryBuilder.build(detections, Priority.BAJA),
        )
    }

    private fun detectionsOf(vararg pairs: Pair<CellClass, Int>): List<Detection> =
        pairs.flatMap { (cellClass, count) -> List(count) { detection(cellClass) } }

    private fun detection(cellClass: CellClass): Detection =
        Detection(cellClass = cellClass, confidence = 0.9f, box = BoundingBox(0f, 0f, 1f, 1f))
}

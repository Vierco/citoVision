package dev.lovelace.citovision.domain.inference

import dev.lovelace.citovision.domain.entities.CellClass
import dev.lovelace.citovision.domain.entities.Detection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Postprocesado (SPEC-0006): argmax de clase, umbral de confianza, mapeo de cajas a la imagen original y NMS
 * por clase. Se construyen salidas sintéticas con `atributos = 4 + 14` (rama de detección, sin máscaras).
 */
class YoloPostprocessorTest {
    private val identityTransform =
        LetterboxTransform(
            scale = 1f,
            padX = 0f,
            padY = 0f,
            originalWidth = 100,
            originalHeight = 100,
            inputSize = 100,
        )

    @Test
    fun `given anchors above and below threshold when decoding then only strong detections survive`() {
        val anchors = 2
        val output = FloatArray(ATTRIBUTES * anchors)

        fun set(
            channel: Int,
            anchor: Int,
            value: Float,
        ) {
            output[channel * anchors + anchor] = value
        }
        // Anchor 0: blasto fuerte, caja centrada 20×20 en (50, 50).
        set(0, 0, 50f)
        set(1, 0, 50f)
        set(2, 0, 20f)
        set(3, 0, 20f)
        set(BOX_CHANNELS + CellClass.BLASTO.index, 0, 0.9f)
        // Anchor 1: todas las clases por debajo del umbral.
        set(0, 1, 10f)
        set(1, 1, 10f)
        set(2, 1, 4f)
        set(3, 1, 4f)
        for (c in 0 until CellClass.entries.size) {
            set(BOX_CHANNELS + c, 1, 0.1f)
        }

        val detections =
            YoloPostprocessor.decode(
                output = output,
                attributes = ATTRIBUTES,
                transform = identityTransform,
                confidenceThreshold = 0.25f,
                iouThreshold = 0.5f,
            )

        assertEquals(1, detections.size)
        val detection = detections.first()
        assertEquals(CellClass.BLASTO, detection.cellClass)
        assertEquals(0.9, detection.confidence.toDouble(), 0.0001)
        assertEquals(0.4, detection.box.left.toDouble(), 0.001)
        assertEquals(0.4, detection.box.top.toDouble(), 0.001)
        assertEquals(0.6, detection.box.right.toDouble(), 0.001)
        assertEquals(0.6, detection.box.bottom.toDouble(), 0.001)
    }

    @Test
    fun `given two overlapping boxes of the same class when decoding then NMS keeps the strongest`() {
        val detections = decodeTwoBoxes(CellClass.BLASTO, CellClass.BLASTO)
        assertEquals(1, detections.size)
        assertEquals(0.9, detections.first().confidence.toDouble(), 0.0001)
    }

    @Test
    fun `given two overlapping boxes of different classes when decoding then NMS keeps both`() {
        val detections = decodeTwoBoxes(CellClass.BLASTO, CellClass.LINFOCITO)
        assertEquals(2, detections.size)
    }

    private fun decodeTwoBoxes(
        first: CellClass,
        second: CellClass,
    ): List<Detection> {
        val anchors = 2
        val output = FloatArray(ATTRIBUTES * anchors)

        fun set(
            channel: Int,
            anchor: Int,
            value: Float,
        ) {
            output[channel * anchors + anchor] = value
        }
        // Dos cajas muy solapadas: (50,50,20,20) y (52,52,20,20).
        set(0, 0, 50f)
        set(1, 0, 50f)
        set(2, 0, 20f)
        set(3, 0, 20f)
        set(BOX_CHANNELS + first.index, 0, 0.9f)
        set(0, 1, 52f)
        set(1, 1, 52f)
        set(2, 1, 20f)
        set(3, 1, 20f)
        set(BOX_CHANNELS + second.index, 1, 0.6f)
        return YoloPostprocessor.decode(
            output = output,
            attributes = ATTRIBUTES,
            transform = identityTransform,
            confidenceThreshold = 0.25f,
            iouThreshold = 0.5f,
        )
    }

    private companion object {
        const val BOX_CHANNELS = 4
        const val ATTRIBUTES = BOX_CHANNELS + 14
    }
}

package dev.lovelace.citovision.domain.entities

/**
 * Una célula detectada por el modelo (SPEC-0006): su [cellClass], la [confidence] (0..1) y su [box]. La
 * [box] se conserva para la futura visualización de cajas/máscaras (fuera de alcance en SPEC-0006).
 */
data class Detection(
    val cellClass: CellClass,
    val confidence: Float,
    val box: BoundingBox,
)

/** Caja contenedora en coordenadas normalizadas 0..1 (esquina superior-izquierda a inferior-derecha). */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

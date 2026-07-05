package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun AnalysisCardPreview() {
    AnalysisCard(
        title = "Análisis de Sangre - Muestra B",
        date = "24/10/2023",
        patient = "PAC-2023-8942",
        description = "Conteo celular completado. Se han detectado neutrófilos y linfocitos según el patrón estándar.",
        image = ColorPainter(Color.LightGray),
        onClick = {}
    )
}

@Composable
fun AnalysisCard(
    title: String,
    date: String,
    patient: String,
    description: String,
    image: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // medium radius
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), // high elevation
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            // Imagen de la muestra
            Image(
                painter = image,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Título del análisis
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF282828) // onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Meta información: Fecha y Paciente
                Text(
                    text = "Fecha: $date | Paciente: $patient",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6F6F6F) // onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Descripción o resultados breves
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF282828)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de acción
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2FA7F0) // Primary
                    ),
                    shape = RoundedCornerShape(16.dp) // medium
                ) {
                    Text(
                        text = "Ver Detalle",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lovelace.citovision.presentation.components.AnalysisCard

@Composable
fun HistoryScreen() {
    var hasData by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasData) {
            // Estado Vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 64.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFFD1D5DB)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "No hay análisis previos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6F6F),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Los análisis guardados aparecerán aquí.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6F6F6F).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Estado con Datos (Lista Temporal)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AnalysisCard(
                        title = "Análisis de Sangre - Muestra A",
                        date = "01/11/2023",
                        patient = "PAC-2023-001",
                        description = "Detección de glóbulos blancos completada satisfactoriamente.",
                        image = ColorPainter(Color.LightGray),
                        onClick = {}
                    )
                }
                item {
                    AnalysisCard(
                        title = "Análisis de Sangre - Muestra B",
                        date = "02/11/2023",
                        patient = "PAC-2023-002",
                        description = "Presencia de anomalías en el conteo de plaquetas detectada.",
                        image = ColorPainter(Color.Gray),
                        onClick = {}
                    )
                }
            }
        }

        // Interruptor temporal abajo a la derecha
        Switch(
            checked = hasData,
            onCheckedChange = { hasData = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

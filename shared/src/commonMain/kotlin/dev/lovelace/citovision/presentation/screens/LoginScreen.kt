package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Fondo blanco base
                drawRect(Color.White)

                // Efecto de resplandor azul horizontal (rectángulo desenfocado)
                val primaryColor = Color(0xFF2FA7F0)
                scale(scaleX = 2.2f, scaleY = 1.6f, pivot = center) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.width * 0.4f,
                        ),
                        radius = size.width * 0.4f,
                        center = center,
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Entrar con usuario y contraseña")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGoogleLoginClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Entrar con Google")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onGuestClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Modo invitado")
            }
        }
    }
}

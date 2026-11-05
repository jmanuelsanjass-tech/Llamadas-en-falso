package com.example.myapplication.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.CallLogEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportDialog(entries: List<CallLogEntry>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { com.example.myapplication.data.SettingsRepository.getInstance(context) }
    val savedEmail by settingsRepository.reportEmail.collectAsState(initial = null)
    
    var email by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedEmail) {
        if (savedEmail != null) {
            email = savedEmail!!
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Nuevo Informe",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Por favor, introduce tu correo para recibir el informe detallado de las ${entries.size} llamadas identificadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("ejemplo@correo.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.saveReportEmail(email)
                    }
                    val reportBody = generateReportText(entries)
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        putExtra(Intent.EXTRA_SUBJECT, "Informe de Seguridad de Llamadas")
                        putExtra(Intent.EXTRA_TEXT, reportBody)
                    }
                    
                    try {
                        context.startActivity(Intent.createChooser(intent, "Enviar informe..."))
                    } catch (e: Exception) {
                        // Handle case where no email app is installed
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                enabled = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ENVIO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun generateReportText(entries: List<CallLogEntry>): String {
    val sb = StringBuilder()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    sb.append("INFORME DE SEGURIDAD DE LLAMADAS\n")
    sb.append("================================\n\n")
    
    if (entries.isEmpty()) {
        sb.append("No hay llamadas registradas en el historial.\n")
    } else {
        entries.reversed().forEachIndexed { index, entry ->
            sb.append("${index + 1}. DETALLE DE LLAMADA\n")
            sb.append("   📞 Número: ${entry.number}\n")
            sb.append("   🏢 Empresa: ${entry.callerName ?: "No identificada"}\n")
            sb.append("   📍 Ubicación: ${entry.geographicId ?: "Desconocida"}\n")
            sb.append("   📢 Campaña: ${entry.campaignId ?: "No disponible"}\n")
            sb.append("   ⏰ Fecha/Hora: ${dateFormat.format(entry.timestamp)}\n")
            sb.append("   🛡️ Resultado: ${entry.identifier}\n")
            sb.append("--------------------------------\n\n")
        }
    }
    
    sb.append("Generado automáticamente por AntiFraude.")
    return sb.toString()
}

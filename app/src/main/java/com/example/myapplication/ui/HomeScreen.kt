package com.example.myapplication.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AntiFraudeTheme

@Composable
fun HomeScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // State for protection status
    var phonePermissionGranted by remember { mutableStateOf(false) }
    var contactsPermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var overlayPermissionGranted by remember { mutableStateOf(false) }
    var batteryOptIgnored by remember { mutableStateOf(false) }
    var roleHeld by remember { mutableStateOf(false) }

    val requiredPermissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun checkAllStatuses() {
        phonePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        contactsPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        overlayPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        batteryOptIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        
        roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else true
    }

    // Refresh status when returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkAllStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val (checkTrigger, setCheckTrigger) = remember { mutableStateOf(0) }
    
    val isHonorDevice = remember { 
        val manufacturer = Build.MANUFACTURER.lowercase()
        manufacturer.contains("honor") || manufacturer.contains("huawei")
    }
    
    var showHonorDialog by remember { mutableStateOf(false) }
    
    fun handleStart() {
        setCheckTrigger(checkTrigger + 1)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> handleStart() }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> handleStart() }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> handleStart() }

    LaunchedEffect(checkTrigger) {
        if (checkTrigger > 0) {
            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayLauncher.launch(intent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !roleHeld) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleLauncher.launch(intent)
                } else {
                    Toast.makeText(context, "El sistema Android reporta que el Rol de Filtrado no está disponible en este dispositivo.", Toast.LENGTH_LONG).show()
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !batteryOptIgnored) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback: take user to the list of apps if the direct dialog fails
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, "Busca la app y selecciona 'Sin restringir'", Toast.LENGTH_LONG).show()
                }
            } else {
                onStart()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Text(
                text = "AntiFraude",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,

                    brush = Brush.horizontalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)

                    )
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "PROTECCIÓN  ACTIVA",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Pulse Animation Setup
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseScale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseAlpha"
            )

            Box(contentAlignment = Alignment.Center) {
                // Pulse Rings
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                )
                
                // Main Button
                Surface(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            shadowElevation = 20.dp.toPx()
                            shape = CircleShape
                            clip = true
                        }
                        .clickable { handleStart() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo_main),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "ACTIVAR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "PROTECCIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Protection Status Dashboard
            StatusDashboard(
                phoneGranted = phonePermissionGranted,
                contactsGranted = contactsPermissionGranted,
                notificationGranted = notificationPermissionGranted,
                overlayGranted = overlayPermissionGranted,
                batteryIgnored = batteryOptIgnored,
                roleGranted = roleHeld,
                onFix = { handleStart() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isHonorDevice) {
                TextButton(
                    onClick = { showHonorDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GUÍA PARA HONOR / MAGICOS", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = "Pulsa para activar la protección y ver registros",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showHonorDialog) {
        AlertDialog(
            onDismissRequest = { showHonorDialog = false },
            title = { Text("Configuración para HONOR") },
            text = {
                Column {
                    Text("Para que el identificador funcione siempre en tu dispositivo HONOR (MagicOS):")
                    Spacer(Modifier.height(12.dp))
                    Text("1. Ve a Ajustes > Aplicaciones > Inicio de aplicaciones.")
                    Text("2. Busca esta app y desactiva 'Gestionar automáticamente'.")
                    Text("3. Asegúrate de que 'Inicio automático', 'Inicio secundario' y 'Ejecutar en segundo plano' estén ACTIVOS.")
                }
            },
            confirmButton = {
                Button(onClick = { showHonorDialog = false }) {
                    Text("ENTENDIDO")
                }
            }
        )
    }
}

@Composable
fun StatusDashboard(
    phoneGranted: Boolean,
    contactsGranted: Boolean,
    notificationGranted: Boolean,
    overlayGranted: Boolean,
    batteryIgnored: Boolean,
    roleGranted: Boolean,
    onFix: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Estado de Protección",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusItem("Permisos de teléfono", phoneGranted)
            StatusItem("Permisos de contactos", contactsGranted)
            StatusItem("Notificaciones", notificationGranted)
            StatusItem("Ventana flotante", overlayGranted)
            StatusItem("Optimización de batería", batteryIgnored)
            StatusItem("Identificador predeterminado", roleGranted)

            if (!phoneGranted || !contactsGranted || !notificationGranted || !overlayGranted || !batteryIgnored || !roleGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFix,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("REVISAR Y ACTIVAR", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StatusItem(label: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isOk) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isOk) {
            Text("ACTIVO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
        } else {
            Text("FALTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    AntiFraudeTheme {
        HomeScreen {}
    }
}

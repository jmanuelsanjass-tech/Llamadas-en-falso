package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AntiFraudeTheme

class CallOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var telephonyManager: TelephonyManager
    private var overlayView: ComposeView? = null
    private val phoneNumberState = androidx.compose.runtime.mutableStateOf("Desconocido")
    private val callStateLabel = androidx.compose.runtime.mutableStateOf("Identificando...")

    private var telephonyCallback: Any? = null // To hold either TelephonyCallback or PhoneStateListener

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        startForegroundService()
        registerCallListener()
    }

    private fun registerCallListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            telephonyCallback = callback
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state, phoneNumber)
                }
            }
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            telephonyCallback = listener
        }
    }

    private fun handleCallState(state: Int, incomingNumber: String? = null) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d("CallOverlayService", "Estado: TIMBRANDO. Número: $incomingNumber")
                callStateLabel.value = "TIMBRANDO"
                incomingNumber?.let { 
                    if (it.isNotEmpty() && it != "Unknown") {
                        phoneNumberState.value = it 
                    }
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d("CallOverlayService", "Estado: EN CURSO (Llamada contestada)")
                callStateLabel.value = "EN CURSO"
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d("CallOverlayService", "Estado: INACTIVO (Llamada finalizada)")
                callStateLabel.value = "FINALIZADA"
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("PHONE_NUMBER") ?: "Desconocido"
        Log.d("CallOverlayService", "onStartCommand with number: $phoneNumber")
        phoneNumberState.value = phoneNumber
        
        // Initial state using getCallState()
        @Suppress("DEPRECATION")
        handleCallState(telephonyManager.callState, phoneNumber)
        
        showOverlay()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val channelId = "call_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Servicio de Identificación",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("AntiFraude")
                .setContentText("Protección activa")
                .setSmallIcon(R.drawable.app_logo_main)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("AntiFraude")
                .setContentText("Protección activa")
                .setSmallIcon(R.drawable.app_logo_main)
                .build()
        }
        startForeground(1, notification)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP
            y = 100
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CallOverlayService)
            setViewTreeViewModelStoreOwner(this@CallOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CallOverlayService)

            setContent {
                AntiFraudeTheme {
                    OverlayContent(
                        phoneNumber = phoneNumberState.value,
                        callState = callStateLabel.value,
                        onClose = { stopSelf() }
                    )
                }
            }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        // Unregister listener
        telephonyCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.unregisterTelephonyCallback(it as TelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        }

        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun OverlayContent(phoneNumber: String, callState: String, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 12.dp.toPx()
                shape = RoundedCornerShape(24.dp)
                clip = true
            },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.05f)
                )
            )
        ),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_main),
                    contentDescription = "Identificador",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = callState,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (callState == "TIMBRANDO") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Block Button
            Button(
                onClick = {
                    com.example.myapplication.data.CallLogRepository.blockNumber(phoneNumber)
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Bloquear", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

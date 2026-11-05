package com.example.myapplication.service

import android.content.Intent
import android.os.Build
import android.util.Log
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import com.example.myapplication.data.CallLogEntry
import com.example.myapplication.data.CallLogRepository
import com.example.myapplication.data.RecipeRepository
import android.net.Uri
import android.provider.ContactsContract
import java.util.Date

class AppCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val phoneNumber = handle?.schemeSpecificPart ?: "Unknown"
        
        Log.d("AppCallScreeningService", "Incoming Call Handle: $handle")
        
        var shouldBlock = false
        var reason = ""

        // 1. Manual block list check (Priority 1)
        if (CallLogRepository.isNumberBlocked(phoneNumber)) {
            shouldBlock = true
            reason = "Bloqueado Manualmente"
        }

        // 2. Recipe-based logic (Priority 2)
        if (!shouldBlock && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
            val isInContacts = isNumberInContacts(phoneNumber)
            
            if (isIncoming) {
                val isAggressiveEnabled = RecipeRepository.isRecipeEnabled("1")
                val isSafeModeEnabled = RecipeRepository.isRecipeEnabled("2")

                // Rule: If number is in contacts, ALWAYS allow (exempt from filters)
                if (isInContacts) {
                    respondToCall(callDetails, CallResponse.Builder().build())
                    return
                }

                // Apply Aggressive Filter (either directly or via Safe Mode)
                if (isAggressiveEnabled || isSafeModeEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val status = callDetails.callerNumberVerificationStatus
                        if (status == Connection.VERIFICATION_STATUS_FAILED || 
                            status == Connection.VERIFICATION_STATUS_NOT_VERIFIED) {
                            shouldBlock = true
                            reason = if (isSafeModeEnabled) "Modo Seguro: No verificado" else "Filtro Agresivo: No verificado"
                        }
                    }
                }

                // Protección IA (id="3")
                if (!shouldBlock && RecipeRepository.isRecipeEnabled("3")) {
                    // Simulación de análisis de patrones sospechosos
                    if (isPotentialSpamPattern(phoneNumber)) {
                        shouldBlock = true
                        reason = "Protección IA: Patrón de spam detectado"
                    }
                }
            } else {
                // Outgoing calls are not blocked
                respondToCall(callDetails, CallResponse.Builder().build())
                return
            }
        }

        // Retrieve Caller ID / Company Name
        val callerName = callDetails.callerDisplayName
        val isAggressiveFilterOn = RecipeRepository.isRecipeEnabled("1")

        // Log the call result
        Log.d("AppCallScreeningService", "Screening call from: $phoneNumber (ID: $callerName) - Result: ${if (shouldBlock) "BLOCK ($reason)" else "ALLOW"}")
        
        // Rule: If Aggressive Filter is on, log EVERYTHING. 
        // Otherwise, only log if blocked.
        if (shouldBlock || isAggressiveFilterOn) {
            val logReason = if (shouldBlock) reason else "Identificada (Agresivo)"
            
            // Geographic simulation based on prefix
            val geoId = when {
                phoneNumber.startsWith("+52") -> "México"
                phoneNumber.startsWith("+1") -> "USA/Canadá"
                phoneNumber.startsWith("+34") -> "España"
                else -> "Internacional"
            }

            // Campaign simulation (can be improved with DB lookup)
            val campId = if (shouldBlock) "Potencial Telemarketing" else "Verificada"

            val entry = CallLogEntry(
                number = phoneNumber,
                timestamp = Date(),
                identifier = logReason,
                callerName = callerName ?: "Empresa No Identificada",
                geographicId = geoId,
                campaignId = campId
            )
            CallLogRepository.addEntry(entry)
        }

        // Trigger Overlay for all incoming calls (including spam warning)
        try {
            val serviceIntent = Intent(this, CallOverlayService::class.java).apply {
                putExtra("PHONE_NUMBER", phoneNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("AppCallScreeningService", "Starting foreground service for overlay")
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AppCallScreeningService", "Failed to start overlay service", e)
        }

        if (shouldBlock) {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)
        } else {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    /**
     * Simulación de un motor de IA que detecta patrones de spam.
     * En una app real, esto podría consultar una base de datos local o remota.
     */
    private fun isPotentialSpamPattern(number: String): Boolean {
        // Ejemplo: Números con muchos ceros, secuencias repetitivas o prefijos conocidos de telemarketing
        val spamPrefixes = listOf("800", "900", "01800")
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        
        return spamPrefixes.any { cleanNumber.startsWith(it) } || 
               cleanNumber.contains("000") || 
               cleanNumber.length > 15
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        if (phoneNumber == "Unknown" || phoneNumber.isEmpty()) return false
        
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    }
}



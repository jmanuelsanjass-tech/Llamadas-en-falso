package com.example.myapplication.data

import java.util.Date

data class CallLogEntry(
    val number: String,
    val timestamp: Date,
    val identifier: String, // e.g., "Spam", "Invalid", "Out of Service"
    val callerName: String? = null, // Company Name (Caller ID)
    val geographicId: String? = null, // Location ID
    val campaignId: String? = null // Campaign ID
)

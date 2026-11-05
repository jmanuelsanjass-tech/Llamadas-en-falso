package com.example.myapplication.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallLogRepository {
    private val _entries = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val entries: StateFlow<List<CallLogEntry>> = _entries.asStateFlow()

    private val _blockedNumbers = MutableStateFlow<Set<String>>(emptySet())
    val blockedNumbers: StateFlow<Set<String>> = _blockedNumbers.asStateFlow()

    fun blockNumber(number: String) {
        _blockedNumbers.value = _blockedNumbers.value + number
    }

    fun unblockNumber(number: String) {
        _blockedNumbers.value = _blockedNumbers.value - number
    }

    fun isNumberBlocked(number: String): Boolean {
        return _blockedNumbers.value.contains(number)
    }

    fun addEntry(entry: CallLogEntry) {
        _entries.value = _entries.value + entry
    }

    fun removeEntry(entry: CallLogEntry) {
        _entries.value = _entries.value.filter { it != entry }
    }


}

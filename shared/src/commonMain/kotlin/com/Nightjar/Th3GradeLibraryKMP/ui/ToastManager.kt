package com.Nightjar.Th3GradeLibraryKMP.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object ToastManager {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()
    private var dismissJob: Job? = null

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun showToast(message: String, durationMs: Long = 5000L) {
        dismissJob?.cancel()
        _toastMessage.value = message
        // Auto dismiss after 5 seconds (or custom duration)
        dismissJob = GlobalScope.launch {
            delay(durationMs)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        dismissJob?.cancel()
        _toastMessage.value = null
    }
}

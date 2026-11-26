package com.Nightjar.Th3GradeLibraryKMP.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ToastManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()
    private var dismissJob: Job? = null

    fun showToast(message: String, durationMs: Long = 5000L) {
        dismissJob?.cancel()
        _toastMessage.value = message
        dismissJob = scope.launch {
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

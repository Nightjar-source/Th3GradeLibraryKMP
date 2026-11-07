package com.Nightjar.Th3GradeLibraryKMP.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object ToastManager {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dismissJob: Job? = null

    fun showToast(message: String) {
        _toastMessage.value = message
        // Cancel any pending dismissal and launch structured auto-dismiss
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(3000)
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

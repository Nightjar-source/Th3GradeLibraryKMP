package com.Nightjar.gradeiraqi3library.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object ToastManager {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
        // Auto dismiss after 3 seconds
        GlobalScope.launch {
            delay(3000)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

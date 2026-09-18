package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun NativeWebView(
    url: String,
    modifier: Modifier,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onLoadingStateChange: (isLoading: Boolean, hasError: Boolean) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    navigatorController: (NativeWebViewNavigator) -> Unit,
    topPaddingPx: Float
) {
    LaunchedEffect(Unit) {
        onTitleChange("Web (Wasm)")
        onUrlChange(url)
        onLoadingStateChange(false, false)
        onCanGoBackChange(false)
        navigatorController(object : NativeWebViewNavigator {
            override fun goBack() {}
            override fun reload() {}
        })
    }
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("WebView is currently supported on Android.")
    }
}

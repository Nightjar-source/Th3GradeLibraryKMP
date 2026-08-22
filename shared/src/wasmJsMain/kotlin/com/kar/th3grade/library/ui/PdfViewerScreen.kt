package com.Nightjar.gradeiraqi3library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.gradeiraqi3library.data.BookItem
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun PdfViewerScreen(
    item: BookItem,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClose: () -> Unit
) {
    // Construct the relative path to the asset resource served by Wasm dev/prod server
    val resourcePath = "composeResources/com.Nightjar.gradeiraqi3library.generated.resources.Res/" + item.pdfPath

    DisposableEffect(item) {
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.setAttribute("src", resourcePath)
        iframe.style.position = "absolute"
        iframe.style.top = "64px"
        iframe.style.left = "0px"
        iframe.style.width = "100%"
        iframe.style.height = "calc(100% - 64px)"
        iframe.style.border = "none"
        iframe.style.zIndex = "1000"

        document.body?.appendChild(iframe)

        onDispose {
            iframe.remove()
        }
    }

    // Top control bar drawn in Compose. The iframe starts at top = 64px.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = item.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )

            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.background(
                    if (isSaved) Color(0x33F59E0B) else Color.White.copy(alpha = 0.1f),
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Bookmark",
                    tint = if (isSaved) Color(0xFFF59E0B) else Color.White
                )
            }
        }
        
        // The rest of the screen area (below the 64dp bar) is occupied by the iframe overlay in the DOM.
        Box(modifier = Modifier.fillMaxSize())
    }
}

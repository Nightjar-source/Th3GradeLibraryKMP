import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.Nightjar.Th3GradeLibraryKMP.App
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler

@JsFun("(msg) => alert(msg)")
private external fun jsAlert(msg: String)

@JsFun("(url) => { window.open(url, '_blank'); }")
private external fun jsOpenUrl(url: String)

@JsFun("() => { try { new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3').play(); } catch(e) {} }")
private external fun jsPlayAudio()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val platformHandler = object : PlatformActionHandler {
        override fun addHomeScreenShortcut(item: BookItem) {
            jsAlert("تم إضافة الاختصار لـ \"${item.title}\" بنجاح إلى المتصفح!")
        }

        override fun showToast(message: String) {
            jsAlert(message)
        }

        override fun openUrl(url: String) {
            jsOpenUrl(url)
        }

        override fun playNotificationSound() {
            jsPlayAudio()
        }
    }

    // Register web network status listener
    try {
        com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.setOnline(kotlinx.browser.window.navigator.onLine)
        kotlinx.browser.window.addEventListener("online", {
            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.setOnline(true)
        })
        kotlinx.browser.window.addEventListener("offline", {
            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.setOnline(false)
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }

    CanvasBasedWindow(
        title = "مكتبة الثالث متوسط",
        canvasElementId = "compose-app"
    ) {
        App(
            platformActionHandler = platformHandler,
            initialBookId = null,
            initialIsNote = false,
            initialPage = null,
            initialSearchQuery = null
        )
    }
}

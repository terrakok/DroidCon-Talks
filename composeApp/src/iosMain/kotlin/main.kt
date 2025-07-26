import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.github.terrakok.dctalks.App
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        parallelRendering = true
    }
) { App() }

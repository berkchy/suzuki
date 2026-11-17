package hime.suzuki

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.SuzukiRoot
import hime.suzuki.ui.theme.SuzukiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SuzukiTheme {
                val vm: MainViewModel = viewModel()
                SuzukiRoot(vm)
            }
        }
    }
}

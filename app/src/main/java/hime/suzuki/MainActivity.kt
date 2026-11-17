package hime.suzuki

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import hime.suzuki.core.AppLocale
import hime.suzuki.core.Onboarding
import hime.suzuki.ui.MainViewModel
import hime.suzuki.ui.SuzukiRoot
import hime.suzuki.ui.screens.OnboardingScreen
import hime.suzuki.ui.theme.SuzukiTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SuzukiTheme {
                val vm: MainViewModel = viewModel()
                var onboarded by rememberSaveable {
                    mutableStateOf(Onboarding.isDone(this@MainActivity))
                }
                if (onboarded) {
                    SuzukiRoot(vm)
                } else {
                    OnboardingScreen(
                        vm = vm,
                        onFinish = {
                            Onboarding.complete(this@MainActivity)
                            onboarded = true
                        },
                    )
                }
            }
        }
    }
}

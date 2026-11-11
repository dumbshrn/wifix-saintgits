package team.codialo.wifix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import team.codialo.wifix.ui.screens.WifixHomeScreen
import team.codialo.wifix.ui.theme.WifixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WifixTheme {
                WifixHomeScreen()
            }
        }
    }
}
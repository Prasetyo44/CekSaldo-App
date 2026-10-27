package id.prasetyo.ceksaldo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import id.prasetyo.ceksaldo.ui.CekSaldoTheme
import id.prasetyo.ceksaldo.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CekSaldoTheme {
                HomeScreen(vm)
            }
        }
    }
}

package de.orgeljahr.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.orgeljahr.demo.data.AppRepository
import de.orgeljahr.demo.ui.OrgeljahrApp
import de.orgeljahr.demo.ui.OrgeljahrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AppRepository(applicationContext)
        setContent {
            OrgeljahrTheme {
                OrgeljahrApp(repository = repository)
            }
        }
    }
}

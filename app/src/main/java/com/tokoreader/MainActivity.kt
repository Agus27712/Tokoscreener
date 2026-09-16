package com.tokoreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tokoreader.ui.theme.TokoReaderTheme
import com.tokoreader.presentation.MainAppScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TokoReaderTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainAppScreen()
        }
      }
    }
  }
}

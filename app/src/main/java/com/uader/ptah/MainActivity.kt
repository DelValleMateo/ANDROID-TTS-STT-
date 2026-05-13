package com.uader.ptah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uader.ptah.ui.chat.ChatScreen
import com.uader.ptah.ui.theme.PTAHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PTAHTheme {
                ChatScreen()
            }
        }
    }
}

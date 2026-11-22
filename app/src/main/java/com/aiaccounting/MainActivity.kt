package com.aiaccounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aiaccounting.ui.AppRoot
import com.aiaccounting.ui.theme.AIAccountingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAccountingTheme {
                AppRoot()
            }
        }
    }
}

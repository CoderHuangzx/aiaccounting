package com.aiaccounting

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.aiaccounting.bubble.FloatingBubbleService
import com.aiaccounting.ui.AppRoot
import com.aiaccounting.ui.theme.AIAccountingTheme

class MainActivity : ComponentActivity() {

    /** 悬浮球点进来时置为 true，让首页一进来就弹记账面板。 */
    private val openEntryRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openEntryRequest.value = intent?.getBooleanExtra(EXTRA_OPEN_ENTRY, false) == true

        setContent {
            AIAccountingTheme {
                AppRoot(
                    openEntryOnLaunch = openEntryRequest.value,
                    onEntryRequestConsumed = { openEntryRequest.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_ENTRY, false)) {
            openEntryRequest.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingBubbleService.setVisible(false)
    }

    override fun onPause() {
        super.onPause()
        FloatingBubbleService.setVisible(true)
    }

    companion object {
        const val EXTRA_OPEN_ENTRY = "open_entry"
    }
}

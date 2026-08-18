package com.aa.ledger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aa.ledger.ui.navigation.NavGraph
import com.aa.ledger.ui.theme.AALedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var widgetNavigateTo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 沉浸式导航栏：内容延伸至底部小白条，导航栏自动取色匹配 App 底色
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.rgb(245, 244, 241),  // MontraBackground
                android.graphics.Color.BLACK
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.rgb(245, 244, 241),  // MontraBackground
                android.graphics.Color.BLACK
            )
        )
        super.onCreate(savedInstanceState)

        // Read widget navigation intent
        widgetNavigateTo = intent.getStringExtra("navigate_to")

        try {
            setContent {
                AALedgerTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NavGraph(initialNavigateTo = widgetNavigateTo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AALedger", "MainActivity crash", e)
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle widget click when app is already running (singleTask)
        widgetNavigateTo = intent.getStringExtra("navigate_to")
    }
}

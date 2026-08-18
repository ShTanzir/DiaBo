package com.tanzir.diabo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.tanzir.diabo.data.repository.TrashPurgeScheduler
import com.tanzir.diabo.ui.navigation.DiaBoNavHost
import com.tanzir.diabo.ui.theme.DiaBoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var trashPurgeScheduler: TrashPurgeScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        trashPurgeScheduler.schedule()
        setContent {
            DiaBoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NatureGradientBackground {
                        DiaBoNavHost()
                    }
                }
            }
        }
    }
}

/**
 * Subtle nature-toned gradient behind every screen so GlassCard's frosted/translucent
 * fill actually has depth to show against, instead of sitting on a flat single color.
 */
@Composable
private fun NatureGradientBackground(content: @Composable () -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, bg)))
    ) {
        content()
    }
}

package com.example.handsfreepick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val pickItems = listOf(
        PickItem(location = "A-01-01", productName = "WIDGET BLUE 10MM", quantity = 3),
        PickItem(location = "B-12-03", productName = "BOLT M8 STAINLESS", quantity = 12),
        PickItem(location = "C-02-07", productName = "TAPE PACKING 50M", quantity = 1),
        PickItem(location = "D-09-02", productName = "LABEL THERMAL 100x150", quantity = 2),
        PickItem(location = "E-04-11", productName = "GLOVE NITRILE L", quantity = 5),
    )

    private val uiState = MutableStateFlow(PickUiState(index = 0, isComplete = false))
    private val confirmEvents = Channel<Unit>(capacity = Channel.BUFFERED)

    private val confirmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == KeyReceiver.ACTION_CONFIRM) {
                confirmEvents.trySend(Unit)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        registerConfirmReceiver()

        setContent {
            val state by uiState.collectAsState()

            LaunchedEffect(Unit) {
                confirmEvents.receiveAsFlow().collect {
                    advance()
                }
            }

            if (state.isComplete) {
                CompleteHud(total = pickItems.size)
            } else {
                val item = pickItems.getOrNull(state.index)
                if (item == null) {
                    CompleteHud(total = pickItems.size)
                } else {
                    PickHud(
                        item = item,
                        index = state.index,
                        total = pickItems.size,
                        onBack = { goBack() },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(confirmReceiver)
        super.onDestroy()
    }

    private fun registerConfirmReceiver() {
        val filter = IntentFilter(KeyReceiver.ACTION_CONFIRM)
        ContextCompat.registerReceiver(
            this,
            confirmReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun advance() {
        uiState.update { state ->
            if (state.isComplete) state
            else {
                val nextIndex = state.index + 1
                if (nextIndex >= pickItems.size) {
                    PickUiState(index = state.index, isComplete = true)
                } else {
                    PickUiState(index = nextIndex, isComplete = false)
                }
            }
        }
    }

    private fun goBack() {
        uiState.update { state ->
            if (state.isComplete) {
                PickUiState(index = pickItems.lastIndex.coerceAtLeast(0), isComplete = false)
            } else {
                PickUiState(index = (state.index - 1).coerceAtLeast(0), isComplete = false)
            }
        }
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private data class PickUiState(
        val index: Int,
        val isComplete: Boolean,
    )
}

@Composable
private fun PickHud(
    item: PickItem,
    index: Int,
    total: Int,
    onBack: () -> Unit,
) {
    val locale = remember { Locale.getDefault() }
    val locationText = remember(item.location, locale) { item.location.uppercase(locale) }
    val nameText = remember(item.productName, locale) { item.productName.uppercase(locale) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .twoFingerSwipeBack(onBack = onBack)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HudText(
                    text = "PICK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                HudText(
                    text = "${index + 1}/$total",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ProgressBar(
                progress = (index + 1).toFloat() / total.coerceAtLeast(1).toFloat(),
                height = 10.dp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            HudText(
                text = locationText,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    HudText(
                        text = nameText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                QuantityPill(quantity = item.quantity)
            }

            Spacer(modifier = Modifier.weight(1f))

            HudText(
                text = "SINGLE CLICK: CONFIRM",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompleteHud(total: Int) {
    val locale = remember { Locale.getDefault() }
    val title = remember(locale) { "COMPLETE".uppercase(locale) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HudText(
                text = title,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(16.dp))
            HudText(
                text = "ALL $total ITEMS CONFIRMED",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun HudText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = 1.2.sp,
        ),
    )
}

@Composable
private fun QuantityPill(quantity: Int) {
    val locale = remember { Locale.getDefault() }
    val text = remember(quantity, locale) { "QTY $quantity".uppercase(locale) }
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.DarkGray, RoundedCornerShape(999.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .background(Color.White, RoundedCornerShape(999.dp)),
        )
    }
}

private fun Modifier.twoFingerSwipeBack(
    onBack: () -> Unit,
): Modifier {
    return pointerInput(Unit) {
        awaitEachGesture {
            var tracking = false
            var start = Offset.Zero
            var ids: Pair<PointerId, PointerId>? = null
            var triggered = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }

                if (!tracking) {
                    if (pressed.size >= 2) {
                        val c0 = pressed[0]
                        val c1 = pressed[1]
                        ids = Pair(c0.id, c1.id)
                        start = centroid(c0, c1)
                        tracking = true
                    }
                } else {
                    val (id0, id1) = ids ?: break
                    val c0 = pressed.firstOrNull { it.id == id0 }
                    val c1 = pressed.firstOrNull { it.id == id1 }
                    if (c0 == null || c1 == null) break
                    val now = centroid(c0, c1)
                    val dx = now.x - start.x
                    if (!triggered && dx >= 160f) {
                        triggered = true
                        onBack()
                        break
                    }
                }

                if (event.changes.all { !it.pressed }) break
            }
        }
    }
}

private fun centroid(a: PointerInputChange, b: PointerInputChange): Offset {
    return Offset(
        x = (a.position.x + b.position.x) / 2f,
        y = (a.position.y + b.position.y) / 2f,
    )
}

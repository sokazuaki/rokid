package com.example.handsfreepick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent

class KeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = intent.keyEventOrNull() ?: return
        if (event.action != KeyEvent.ACTION_UP || event.repeatCount != 0) return
        if (event.keyCode !in CONFIRM_KEY_CODES) return

        context.sendBroadcast(
            Intent(ACTION_CONFIRM).setPackage(context.packageName),
        )

        if (isOrderedBroadcast) abortBroadcast()
    }

    private fun Intent.keyEventOrNull(): KeyEvent? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.example.handsfreepick.ACTION_CONFIRM"

        private val CONFIRM_KEY_CODES = setOf(
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
        )
    }
}

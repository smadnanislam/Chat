package com.bubbles.messages

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat

/** Receives incoming SMS when this app is the default SMS app and stores it. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val from = parts.firstOrNull()?.originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val v = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, from)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
        }
        try { context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, v) } catch (e: Exception) {}
        notify(context, SmsRepository(context).nameFor(from) ?: from, body)
    }

    private fun notify(ctx: Context, title: String, body: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("msgs", "Messages", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n: Notification = NotificationCompat.Builder(ctx, "msgs")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { nm.notify(title.hashCode(), n) } catch (e: SecurityException) {}
    }
}

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

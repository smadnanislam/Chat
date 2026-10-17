package com.bubbles.messages

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun has(c: Context, p: String) =
    ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED

fun smsGranted(c: Context) =
    has(c, Manifest.permission.READ_SMS) && has(c, Manifest.permission.SEND_SMS)

/** Messaging + Contacts + Storage (+ Phone for calling, Notifications). */
fun allPerms(): Array<String> = buildList<String> {
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.CALL_PHONE)
    if (Build.VERSION.SDK_INT >= 33) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= 28) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}.toTypedArray()

/** Works for saved contacts AND unknown numbers. Falls back to the dialer if CALL_PHONE is denied. */
fun callNumber(ctx: Context, number: String) {
    val uri = Uri.fromParts("tel", number, null)
    val intent = if (has(ctx, Manifest.permission.CALL_PHONE)) Intent(Intent.ACTION_CALL, uri)
    else Intent(Intent.ACTION_DIAL, uri)
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Can't place call", Toast.LENGTH_SHORT).show()
    }
}

fun cleanNumber(s: String) = s.filter { it.isDigit() || it == '+' }

fun shortTime(ms: Long): String {
    val now = Calendar.getInstance()
    val t = Calendar.getInstance().apply { timeInMillis = ms }
    val sameDay = now.get(Calendar.YEAR) == t.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR)
    val days = (now.timeInMillis - ms) / 86_400_000L
    val pattern = when {
        sameDay -> "h:mm a"
        days < 7 -> "EEE"
        else -> "dd/MM/yy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
}

fun headerTime(ms: Long): String =
    SimpleDateFormat("EEE, d MMM  h:mm a", Locale.getDefault()).format(Date(ms))

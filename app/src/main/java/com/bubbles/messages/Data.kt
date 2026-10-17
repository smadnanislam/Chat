package com.bubbles.messages

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.MediaStore
import android.provider.Telephony
import android.telephony.SmsManager
import java.io.File

data class Conversation(
    val address: String,
    val name: String,
    val snippet: String,
    val date: Long,
    val unread: Boolean
)

data class Msg(val id: Long, val body: String, val date: Long, val sent: Boolean)
data class ContactItem(val name: String, val number: String)

class SmsRepository(private val ctx: Context) {
    private val names = HashMap<String, String?>()

    fun nameFor(number: String): String? {
        if (names.containsKey(number)) return names[number]
        val n = lookup(number)
        names[number] = n
        return n
    }

    private fun lookup(number: String): String? = try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        ctx.contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    fun conversations(): List<Conversation> {
        names.clear()
        val seen = LinkedHashMap<Long, Conversation>()
        val unreadThreads = HashSet<Long>()
        try {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.READ, Telephony.Sms.TYPE
                ),
                null, null, "date DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val t = c.getLong(0)
                    val unread = c.getInt(4) == 0 && c.getInt(5) == Telephony.Sms.MESSAGE_TYPE_INBOX
                    if (unread) unreadThreads.add(t)
                    if (t !in seen) {
                        val a = c.getString(1) ?: ""
                        seen[t] = Conversation(a, nameFor(a) ?: a, c.getString(2) ?: "", c.getLong(3), false)
                    }
                }
            }
        } catch (e: SecurityException) {
        }
        return seen.map { (t, c) -> c.copy(unread = t in unreadThreads) }
    }

    fun threadId(address: String): Long? = try {
        Telephony.Threads.getOrCreateThreadId(ctx, address)
    } catch (e: Exception) {
        null
    }

    fun messages(address: String): List<Msg> {
        val out = ArrayList<Msg>()
        val tid = threadId(address) ?: return out
        try {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                "${Telephony.Sms.THREAD_ID}=?", arrayOf(tid.toString()), "date ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    out += Msg(
                        c.getLong(0), c.getString(1) ?: "", c.getLong(2),
                        c.getInt(3) != Telephony.Sms.MESSAGE_TYPE_INBOX
                    )
                }
            }
        } catch (e: SecurityException) {
        }
        return out
    }

    fun markRead(address: String) {
        try {
            val tid = threadId(address) ?: return
            val v = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            ctx.contentResolver.update(
                Telephony.Sms.CONTENT_URI, v,
                "${Telephony.Sms.THREAD_ID}=? AND ${Telephony.Sms.READ}=0", arrayOf(tid.toString())
            )
        } catch (e: Exception) {
        }
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java)
        else @Suppress("DEPRECATION") SmsManager.getDefault()

    fun send(address: String, body: String): Boolean {
        return try {
            val sm = smsManager()
            sm.sendMultipartTextMessage(address, null, sm.divideMessage(body), null, null)
            val v = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
            }
            // Only works when this app is the default SMS app.
            try { ctx.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, v) } catch (e: Exception) {}
            true
        } catch (e: Exception) {
            false
        }
    }

    fun contacts(): List<ContactItem> {
        val out = LinkedHashMap<String, ContactItem>()
        try {
            ctx.contentResolver.query(
                Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null,
                "${Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val n = c.getString(0) ?: continue
                    val num = c.getString(1) ?: continue
                    out.putIfAbsent(n + num.filter { it.isDigit() }, ContactItem(n, num))
                }
            }
        } catch (e: SecurityException) {
        }
        return out.values.toList()
    }

    /** Saves the chat as a .txt file in Downloads. */
    fun export(title: String, list: List<Msg>): Boolean {
        val text = list.joinToString("\n") {
            "[${headerTime(it.date)}] ${if (it.sent) "Me" else title}: ${it.body}"
        }
        val name = "chat_${title.filter { it.isLetterOrDigit() }}_${System.currentTimeMillis()}.txt"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                    ?: return false
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, name).writeText(text)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

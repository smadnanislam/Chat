package com.bubbles.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------- shared bits ----------

@Composable
fun Avatar(name: String, size: Dp) {
    val initial = name.firstOrNull { it.isLetter() }?.uppercase()
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFA5ABB8), Color(0xFF858A96)))),
        contentAlignment = Alignment.Center
    ) {
        if (initial != null)
            Text(initial, color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
        else
            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun PillField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val fill = if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    TextField(
        value, onChange, modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = fill,
            unfocusedContainerColor = fill
        )
    )
}

// ---------- permission screen ----------

@Composable
fun PermissionGate(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF5AC8FA), Blue))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Email, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Messages", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "To work properly the app needs:\n\n" +
                "•  Messages – read and send texts\n" +
                "•  Contacts – show names and pick people\n" +
                "•  Storage – export chats and attachments\n" +
                "•  Phone – call contacts or any number",
            color = Gray, fontSize = 15.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAllow, shape = CircleShape,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Allow access", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
    }
}

// ---------- home / conversation list ----------

@Composable
fun HomeScreen(
    convos: List<Conversation>,
    isDefault: Boolean,
    onDefault: () -> Unit,
    onOpen: (Conversation) -> Unit,
    onNew: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    val shown = remember(convos, q) {
        if (q.isBlank()) convos
        else convos.filter { it.name.contains(q, true) || it.snippet.contains(q, true) }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onNew) { Icon(Icons.Filled.Edit, "New message", tint = Blue) }
        }
        PillField(q, { q = it }, "Search", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (!isDefault) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Blue.copy(alpha = 0.12f))
                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set as default SMS app to send & receive here", fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onDefault) { Text("Set", color = Blue, fontWeight = FontWeight.Bold) }
            }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No conversations", color = Gray) }
        } else LazyColumn(Modifier.fillMaxSize()) {
            items(shown) { c ->
                ConversationRow(c) { onOpen(c) }
                HorizontalDivider(Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Gray.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun ConversationRow(c: Conversation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (c.unread) Blue else Color.Transparent))
        Spacer(Modifier.width(6.dp))
        Avatar(c.name, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name, fontWeight = if (c.unread) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(shortTime(c.date), color = Gray, fontSize = 14.sp)
            }
            Text(c.snippet, color = Gray, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------- new message / dial ----------

@Composable
fun NewScreen(repo: SmsRepository, onBack: () -> Unit, onOpen: (String, String) -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<ContactItem>()) }
    LaunchedEffect(Unit) { contacts = withContext(Dispatchers.IO) { repo.contacts() } }

    val typed = q.trim()
    val isNumber = typed.length >= 3 && typed.all { it.isDigit() || it in "+-() " }
    val shown = remember(contacts, typed) {
        if (typed.isEmpty()) contacts
        else contacts.filter { it.name.contains(typed, true) || it.number.contains(typed) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Blue) }
            Text("New Message", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        PillField(q, { q = it }, "To: name or number", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            if (isNumber) item {
                val n = cleanNumber(typed)
                PersonRow(typed, "Not in contacts · message or call", { onOpen(n, typed) }, { callNumber(ctx, n) })
            }
            items(shown) { c ->
                PersonRow(c.name, c.number, { onOpen(cleanNumber(c.number), c.name) }, { callNumber(ctx, cleanNumber(c.number)) })
            }
        }
    }
}

@Composable
fun PersonRow(title: String, sub: String, onClick: () -> Unit, onCall: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(title, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Gray, fontSize = 13.sp, maxLines = 1)
        }
        IconButton(onCall) { Icon(Icons.Filled.Call, "Call", tint = Color(0xFF34C759)) }
    }
}

// ---------- chat ----------

@Composable
fun ChatScreen(repo: SmsRepository, s: Screen.Chat, refresh: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    var msgs by remember { mutableStateOf(emptyList<Msg>()) }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { repo.markRead(s.address) } }
    LaunchedEffect(refresh) { msgs = withContext(Dispatchers.IO) { repo.messages(s.address) } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.scrollToItem(msgs.lastIndex) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Blue) }
            Avatar(s.title, 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(s.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { repo.export(s.title, msgs) }
                    Toast.makeText(ctx, if (ok) "Saved to Downloads" else "Export failed", Toast.LENGTH_SHORT).show()
                }
            }) { Icon(Icons.Filled.Download, "Export chat", tint = Blue) }
            IconButton({ callNumber(ctx, s.address) }) { Icon(Icons.Filled.Call, "Call", tint = Blue) }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Gray.copy(alpha = 0.3f))

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = list,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(msgs, key = { _, m -> m.id }) { i, m ->
                val prev = msgs.getOrNull(i - 1)
                val next = msgs.getOrNull(i + 1)
                if (prev == null || m.date - prev.date > 3_600_000L)
                    Text(headerTime(m.date), color = Gray, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                val tail = next == null || next.sent != m.sent || next.date - m.date > 3_600_000L
                Bubble(m, tail)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
            val line = Gray.copy(alpha = 0.5f)
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                    .border(1.dp, line, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (text.isEmpty()) Text("Text Message", color = Gray, fontSize = 16.sp)
                BasicTextField(
                    text, { text = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp),
                    cursorBrush = SolidColor(Blue), maxLines = 5, modifier = Modifier.fillMaxWidth()
                )
            }
            if (text.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Blue).clickable {
                        val body = text.trim()
                        text = ""
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { repo.send(s.address, body) }
                            if (!ok) Toast.makeText(ctx, "Couldn't send message", Toast.LENGTH_SHORT).show()
                            msgs = withContext(Dispatchers.IO) { repo.messages(s.address) }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ArrowUpward, "Send", tint = Color.White) }
            }
        }
    }
}

@Composable
fun Bubble(m: Msg, tail: Boolean) {
    val dark = isSystemInDarkTheme()
    val bg = when {
        m.sent -> Blue
        dark -> Color(0xFF26262A)
        else -> Color(0xFFE9E9EB)
    }
    val fg = if (m.sent || dark) Color.White else Color.Black
    val big = 20.dp
    val small = if (tail) 5.dp else big
    val shape = RoundedCornerShape(
        topStart = big, topEnd = big,
        bottomEnd = if (m.sent) small else big,
        bottomStart = if (m.sent) big else small
    )
    Box(
        Modifier.fillMaxWidth().padding(bottom = if (tail) 6.dp else 0.dp),
        contentAlignment = if (m.sent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            m.body, color = fg, fontSize = 16.sp,
            modifier = Modifier.widthIn(max = 290.dp).clip(shape).background(bg)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

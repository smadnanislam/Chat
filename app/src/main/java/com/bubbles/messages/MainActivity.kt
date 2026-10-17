package com.bubbles.messages

import android.app.role.RoleManager
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object New : Screen
    data class Chat(val address: String, val title: String) : Screen
}

class MainActivity : ComponentActivity() {
    private val resumes = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = intent?.data?.schemeSpecificPart?.substringBefore('?')
            ?.let { cleanNumber(it) }?.takeIf { it.isNotBlank() }
        setContent { MessagesTheme { App(start, resumes.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        resumes.intValue++
    }
}

@Composable
fun App(startAddress: String?, resume: Int) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    var screen by remember {
        mutableStateOf<Screen>(
            if (startAddress != null) Screen.Chat(startAddress, startAddress) else Screen.Home
        )
    }
    var tick by remember { mutableIntStateOf(0) }
    var smsOk by remember { mutableStateOf(smsGranted(ctx)) }
    var convos by remember { mutableStateOf(emptyList<Conversation>()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { smsOk = smsGranted(ctx); tick++ }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { tick++ }

    LaunchedEffect(resume) { smsOk = smsGranted(ctx) }

    DisposableEffect(smsOk) {
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { tick++ }
        }
        if (smsOk) ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, obs)
        onDispose { ctx.contentResolver.unregisterContentObserver(obs) }
    }

    LaunchedEffect(smsOk, resume, tick) {
        if (smsOk) convos = withContext(Dispatchers.IO) { repo.conversations() }
    }

    val isDefault = remember(resume, tick) {
        Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    }

    fun askDefault() {
        val i = if (Build.VERSION.SDK_INT >= 29)
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
        else
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
        roleLauncher.launch(i)
    }

    BackHandler(screen !is Screen.Home) { screen = Screen.Home }

    Surface(
        Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!smsOk) {
            PermissionGate { permLauncher.launch(allPerms()) }
        } else when (val s = screen) {
            Screen.Home -> HomeScreen(
                convos, isDefault,
                onDefault = { askDefault() },
                onOpen = { screen = Screen.Chat(it.address, it.name) },
                onNew = { screen = Screen.New }
            )
            Screen.New -> NewScreen(
                repo,
                onBack = { screen = Screen.Home },
                onOpen = { a, t -> screen = Screen.Chat(a, t) }
            )
            is Screen.Chat -> ChatScreen(repo, s, resume + tick) { screen = Screen.Home }
        }
    }
}

package cz.honestlead.mezera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.honestlead.mezera.data.AppCatalog
import cz.honestlead.mezera.data.AppInfo
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.data.Target
import cz.honestlead.mezera.feedback.Feedback
import cz.honestlead.mezera.update.UpdateInfo
import cz.honestlead.mezera.update.Updater
import cz.honestlead.mezera.ui.theme.Blue
import cz.honestlead.mezera.ui.theme.BlueSoft
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.BlueWash
import cz.honestlead.mezera.ui.theme.Bg
import cz.honestlead.mezera.ui.theme.BgDeep
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkFaint
import cz.honestlead.mezera.ui.theme.InkSoft
import cz.honestlead.mezera.ui.theme.LineStrong
import cz.honestlead.mezera.ui.theme.MezeraTheme
import cz.honestlead.mezera.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MezeraTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }

    var refreshKey by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(store.enabled) }
    var tab by remember { mutableIntStateOf(0) } // 0 = appky, 1 = statistiky

    var targets by remember { mutableStateOf(store.getTargets()) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Target?>(null) }
    var events by remember { mutableStateOf(store.getEvents()) }

    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var showFeedback by remember { mutableStateOf(false) }

    val accessibilityOn = remember(refreshKey) { Perm.accessibilityEnabled(context) }
    val overlayOn = remember(refreshKey) { Perm.overlayGranted(context) }

    // Při otevření appky tiše zkontrolujeme, jestli není novější verze.
    LaunchedEffect(Unit) {
        updateInfo = Updater.check()
    }

    // Na Androidu 13+ si řekneme o povolení notifikací (kvůli upozornění na update).
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun doCheckUpdate() {
        scope.launch {
            updateBusy = true
            updateMsg = "Kontroluji..."
            val info = Updater.check()
            updateInfo = info
            updateBusy = false
            updateMsg = if (info == null) "Máš nejnovější verzi." else null
        }
    }

    fun doUpdate() {
        val info = updateInfo ?: return
        if (!Updater.canInstall(context)) {
            updateMsg = "Povol \"instalovat neznámé appky\" a zkus to znovu."
            Updater.openUnknownSourcesSettings(context)
            return
        }
        scope.launch {
            updateBusy = true
            updateProgress = 0f
            updateMsg = "Stahuji novou verzi..."
            try {
                val file = Updater.downloadApk(context, info.apkUrl) { p -> updateProgress = p }
                updateMsg = "Spouštím instalaci..."
                Updater.installApk(context, file)
            } catch (e: Exception) {
                updateMsg = "Stažení se nepovedlo, zkus to znovu."
            }
            updateBusy = false
        }
    }

    // Načtení appek mimo hlavní vlákno.
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context) }
        apps = loaded
        loadingApps = false
    }

    // Po návratu z nastavení znovu zkontrolujeme oprávnění a data.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                refreshKey++
                targets = store.getTargets()
                events = store.getEvents()
                enabled = store.enabled
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    fun toggleTarget(app: AppInfo, on: Boolean) {
        if (on) {
            store.upsertTarget(Target(packageName = app.packageName, label = app.label))
        } else {
            store.removeTarget(app.packageName)
        }
        targets = store.getTargets()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            BrandHeader(
                enabled = enabled,
                onToggle = {
                    enabled = it
                    store.enabled = it
                }
            )
            Spacer(Modifier.height(22.dp))
        }

        item {
            PermissionSection(
                accessibilityOn = accessibilityOn,
                overlayOn = overlayOn,
                onAccessibility = { Perm.openAccessibilitySettings(context) },
                onOverlay = { Perm.openOverlaySettings(context) }
            )
            Spacer(Modifier.height(22.dp))
        }

        item {
            UpdateCard(
                info = updateInfo,
                busy = updateBusy,
                progress = updateProgress,
                message = updateMsg,
                onCheck = { doCheckUpdate() },
                onUpdate = { doUpdate() }
            )
            Spacer(Modifier.height(12.dp))
            FeedbackEntry(onClick = { showFeedback = true })
            Spacer(Modifier.height(22.dp))
        }

        item {
            Tabs(tab = tab, onTab = { tab = it })
            Spacer(Modifier.height(18.dp))
        }

        if (tab == 0) {
            appsSection(
                apps = apps,
                targets = targets,
                loading = loadingApps,
                query = query,
                onQuery = { query = it },
                onToggle = ::toggleTarget,
                onEdit = { editing = it }
            )
        } else {
            statsSection(events = events, onClear = {
                store.clearEvents()
                events = emptyList()
            })
        }
    }

    editing?.let { t ->
        EditTargetDialog(
            target = t,
            onDismiss = { editing = null },
            onSave = { updated ->
                store.upsertTarget(updated)
                targets = store.getTargets()
                editing = null
            }
        )
    }

    if (showFeedback) {
        FeedbackDialog(
            onDismiss = { showFeedback = false },
            onSend = { text -> Feedback.send(text) }
        )
    }
}

@Composable
private fun BrandHeader(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.orb(46.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Pause", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("vteřina na rozmyšlenou", color = InkFaint, fontSize = 13.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Blue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = LineStrong,
                uncheckedBorderColor = LineStrong
            )
        )
    }
}

@Composable
private fun PermissionSection(
    accessibilityOn: Boolean,
    overlayOn: Boolean,
    onAccessibility: () -> Unit,
    onOverlay: () -> Unit
) {
    if (accessibilityOn && overlayOn) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Blue)
            )
            Spacer(Modifier.width(12.dp))
            Text("Vše připraveno, Mezera hlídá.", color = InkSoft, fontSize = 15.sp)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!accessibilityOn) {
            PermissionCard(
                title = "Zapni hlídání appek",
                body = "Mezera potřebuje vidět, kterou appku otvíráš. Zapni ji v Přístupnosti (Accessibility).",
                cta = "Otevřít nastavení",
                onClick = onAccessibility
            )
        }
        if (!overlayOn) {
            PermissionCard(
                title = "Povol překrytí ostatních appek",
                body = "Aby se klidná obrazovka mohla ukázat přes appku, kterou otvíráš.",
                cta = "Povolit překrytí",
                onClick = onOverlay
            )
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, cta: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(20.dp)
    ) {
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = InkSoft, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(BlueWash)
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(cta, color = BlueStrong, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Tabs(tab: Int, onTab: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(BgDeep)
            .padding(4.dp)
    ) {
        TabPill("Appky", tab == 0) { onTab(0) }
        Spacer(Modifier.width(4.dp))
        TabPill("Statistiky", tab == 1) { onTab(1) }
    }
}

@Composable
private fun TabPill(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(if (active) Surface else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (active) Ink else InkSoft,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UpdateCard(
    info: UpdateInfo?,
    busy: Boolean,
    progress: Float,
    message: String?,
    onCheck: () -> Unit,
    onUpdate: () -> Unit
) {
    when {
        busy -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
                    .padding(20.dp)
            ) {
                Text(
                    message ?: "Pracuji...",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        color = Blue,
                        trackColor = BgDeep
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${(progress * 100).toInt()} %", color = InkSoft, fontSize = 13.sp)
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        color = Blue,
                        trackColor = BgDeep
                    )
                }
            }
        }

        info != null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
                    .padding(20.dp)
            ) {
                Text(
                    "Nová verze ${info.versionName}",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Co je nového",
                    color = BlueStrong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                val lines = info.notes.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) {
                    Text("Vylepšení a opravy.", color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                } else {
                    lines.forEach { line ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("•  ", color = BlueStrong, fontSize = 14.sp)
                            Text(line, color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(Blue)
                        .clickable { onUpdate() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Stáhnout a nainstalovat",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Data i nastavení zůstanou.", color = InkFaint, fontSize = 12.sp)
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Verze ${Updater.currentVersionName()}",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        message ?: "Aktualizace se stáhnou ze serveru.",
                        color = InkFaint,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(BgDeep)
                        .clickable { onCheck() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Zkontrolovat",
                        color = BlueStrong,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

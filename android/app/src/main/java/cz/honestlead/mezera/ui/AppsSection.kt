package cz.honestlead.mezera.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.honestlead.mezera.data.AppCatalog
import cz.honestlead.mezera.data.AppInfo
import cz.honestlead.mezera.data.Target
import cz.honestlead.mezera.ui.theme.Blue
import cz.honestlead.mezera.ui.theme.BlueSoft
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.BgDeep
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkFaint
import cz.honestlead.mezera.ui.theme.InkSoft
import cz.honestlead.mezera.ui.theme.LineStrong
import cz.honestlead.mezera.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun LazyListScope.appsSection(
    apps: List<AppInfo>,
    targets: List<Target>,
    loading: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onToggle: (AppInfo, Boolean) -> Unit,
    onEdit: (Target) -> Unit
) {
    val targetMap = targets.associateBy { it.packageName }

    item {
        Text(
            "Vyber appky, u kterých se tě Mezera zeptá, proč tam jdeš.",
            color = InkSoft,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))
    }

    item {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Hledat appku...", color = InkFaint) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlueSoft,
                unfocusedBorderColor = LineStrong,
                cursorColor = Blue,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface
            )
        )
        Spacer(Modifier.height(16.dp))
    }

    if (loading) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Načítám appky...", color = InkFaint, fontSize = 15.sp)
            }
        }
        return
    }

    val q = query.trim().lowercase()
    val filtered = apps
        .filter { q.isEmpty() || it.label.lowercase().contains(q) }
        .sortedWith(
            compareByDescending<AppInfo> { targetMap.containsKey(it.packageName) }
                .thenBy { it.label.lowercase() }
        )

    itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
        val target = targetMap[app.packageName]
        AppRow(
            app = app,
            target = target,
            first = index == 0,
            last = index == filtered.lastIndex,
            onToggle = { on -> onToggle(app, on) },
            onEdit = { target?.let(onEdit) }
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    target: Target?,
    first: Boolean,
    last: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, app.packageName) {
        val bmp = withContext(Dispatchers.IO) { AppCatalog.loadIcon(context, app.packageName) }
        value = bmp?.asImageBitmap()
    }
    val isTarget = target != null

    // Kartu skládáme spojitě: zaoblíme jen první a poslední řádek.
    val shape = RoundedCornerShape(
        topStart = if (first) 22.dp else 0.dp,
        topEnd = if (first) 22.dp else 0.dp,
        bottomStart = if (last) 22.dp else 0.dp,
        bottomEnd = if (last) 22.dp else 0.dp
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Surface)
                .clickable(enabled = isTarget) { onEdit() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(BgDeep),
                contentAlignment = Alignment.Center
            ) {
                val bmp = icon
                if (bmp != null) {
                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(34.dp))
                } else {
                    Text(
                        app.label.take(1).uppercase(),
                        color = InkSoft,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (target != null) {
                    Text(
                        "nádech ${target.cooldownSec}s · důvod min. ${target.reasonMinChars} znaků · klepni pro úpravu",
                        color = InkFaint,
                        fontSize = 12.5.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = isTarget,
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
        if (!last) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(start = 70.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cz.honestlead.mezera.ui.theme.Line)
                )
            }
        }
    }
}

@Composable
fun EditTargetDialog(
    target: Target,
    onDismiss: () -> Unit,
    onSave: (Target) -> Unit
) {
    var cooldown by remember { mutableIntStateOf(target.cooldownSec) }
    var minChars by remember { mutableIntStateOf(target.reasonMinChars) }
    var gap by remember { mutableIntStateOf(target.sessionGapSec) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    target.copy(
                        cooldownSec = cooldown,
                        reasonMinChars = minChars,
                        sessionGapSec = gap
                    )
                )
            }) { Text("Uložit", color = BlueStrong, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit", color = InkSoft) }
        },
        title = { Text(target.label, fontWeight = FontWeight.Bold, color = Ink) },
        containerColor = Surface,
        text = {
            Column {
                StepperRow("Nádech (s)", cooldown, 0, 120, 1) { cooldown = it }
                Spacer(Modifier.height(8.dp))
                StepperRow("Min. znaků důvodu", minChars, 0, 200, 5) { minChars = it }
                Spacer(Modifier.height(8.dp))
                StepperRow("Klid mezi dotazy (s)", gap, 0, 600, 15) { gap = it }
            }
        }
    )
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = InkSoft, fontSize = 15.sp, modifier = Modifier.weight(1f))
        StepBtn("-") { onChange((value - step).coerceAtLeast(min)) }
        Box(modifier = Modifier.width(46.dp), contentAlignment = Alignment.Center) {
            Text("$value", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        StepBtn("+") { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepBtn(sign: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(BgDeep)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(sign, color = BlueStrong, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

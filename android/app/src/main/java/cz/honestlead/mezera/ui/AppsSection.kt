package cz.honestlead.mezera.ui

import cz.honestlead.mezera.R
import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
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
import cz.honestlead.mezera.data.TimeRule
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
            stringResource(R.string.choose_apps),
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
            placeholder = { Text(stringResource(R.string.search_apps), color = InkFaint) },
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
                Text(stringResource(R.string.loading_apps), color = InkFaint, fontSize = 15.sp)
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
                        stringResource(R.string.target_summary, target.cooldownSec, target.reasonMinChars),
                        color = InkFaint,
                        fontSize = 12.5.sp
                    )
                    if (target.timeRules.isNotEmpty()) {
                        Text(
                            stringResource(R.string.time_rules_count, target.timeRules.size),
                            color = BlueStrong,
                            fontSize = 12.5.sp
                        )
                    }
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
    var pauseText by remember { mutableStateOf(target.pauseText) }
    var rules by remember { mutableStateOf(target.timeRules) }
    var editingRuleIndex by remember { mutableStateOf<Int?>(null) }

    editingRuleIndex?.let { index ->
        val initial = if (index >= 0) rules[index] else TimeRule(
            startMinute = 7 * 60,
            endMinute = 9 * 60,
            cooldownSec = cooldown,
            reasonMinChars = minChars,
            sessionGapSec = gap
        )
        EditTimeRuleDialog(
            initial = initial,
            canDelete = index >= 0,
            onDismiss = { editingRuleIndex = null },
            onSave = { updated ->
                rules = if (index >= 0) {
                    rules.toMutableList().also { it[index] = updated }
                } else {
                    rules + updated
                }
                editingRuleIndex = null
            },
            onDelete = {
                if (index >= 0) rules = rules.filterIndexed { i, _ -> i != index }
                editingRuleIndex = null
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    target.copy(
                        cooldownSec = cooldown,
                        reasonMinChars = minChars,
                        sessionGapSec = gap,
                        pauseText = pauseText.trim(),
                        timeRules = rules
                    )
                )
            }) { Text(stringResource(R.string.save), color = BlueStrong, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = InkSoft) }
        },
        title = { Text(target.label, fontWeight = FontWeight.Bold, color = Ink) },
        containerColor = Surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.default_rules), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                StepperRow(stringResource(R.string.breath_seconds), cooldown, 0, 120, 1) { cooldown = it }
                Spacer(Modifier.height(8.dp))
                StepperRow(stringResource(R.string.reason_minimum), minChars, 0, 200, 5) { minChars = it }
                Spacer(Modifier.height(8.dp))
                StepperRow(stringResource(R.string.session_gap), gap, 0, 3600, 15) { gap = it }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pauseText,
                    onValueChange = { pauseText = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    label = { Text(stringResource(R.string.custom_pause_text)) },
                    placeholder = { Text(stringResource(R.string.custom_pause_text_hint), color = InkFaint) },
                    supportingText = { Text(stringResource(R.string.custom_pause_text_help), color = InkFaint) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueSoft,
                        unfocusedBorderColor = LineStrong,
                        cursorColor = Blue,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        focusedContainerColor = BgDeep,
                        unfocusedContainerColor = BgDeep
                    )
                )
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.time_rules), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.time_rules_help), color = InkFaint, fontSize = 12.5.sp)
                Spacer(Modifier.height(8.dp))
                rules.forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgDeep)
                            .clickable { editingRuleIndex = index }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatRange(rule), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (rule.blocked) stringResource(R.string.fully_blocked)
                                else stringResource(R.string.rule_summary, rule.cooldownSec, rule.reasonMinChars),
                                color = if (rule.blocked) BlueStrong else InkFaint,
                                fontSize = 12.sp
                            )
                        }
                        Text(stringResource(R.string.edit), color = BlueStrong, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                }
                TextButton(onClick = { editingRuleIndex = -1 }) {
                    Text(stringResource(R.string.add_time_rule), color = BlueStrong, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable
private fun EditTimeRuleDialog(
    initial: TimeRule,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (TimeRule) -> Unit,
    onDelete: () -> Unit
) {
    var startHour by remember { mutableIntStateOf(initial.startMinute / 60) }
    var startMinute by remember { mutableIntStateOf(initial.startMinute % 60) }
    var endHour by remember { mutableIntStateOf(initial.endMinute / 60) }
    var endMinute by remember { mutableIntStateOf(initial.endMinute % 60) }
    var cooldown by remember { mutableIntStateOf(initial.cooldownSec) }
    var minChars by remember { mutableIntStateOf(initial.reasonMinChars) }
    var gap by remember { mutableIntStateOf(initial.sessionGapSec) }
    var blocked by remember { mutableStateOf(initial.blocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    TimeRule(
                        startMinute = startHour * 60 + startMinute,
                        endMinute = endHour * 60 + endMinute,
                        cooldownSec = cooldown,
                        reasonMinChars = minChars,
                        sessionGapSec = gap,
                        blocked = blocked
                    )
                )
            }) { Text(stringResource(R.string.done), color = BlueStrong, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.back), color = InkSoft) }
        },
        title = { Text(stringResource(R.string.time_rule), fontWeight = FontWeight.Bold, color = Ink) },
        containerColor = Surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.from_time), color = InkSoft, fontSize = 13.sp)
                TimePickerRow(startHour, startMinute, onHour = { startHour = it }, onMinute = { startMinute = it })
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.to_time), color = InkSoft, fontSize = 13.sp)
                TimePickerRow(endHour, endMinute, onHour = { endHour = it }, onMinute = { endMinute = it })
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.fully_blocked), color = Ink, fontSize = 15.sp)
                        Text(stringResource(R.string.fully_blocked_help), color = InkFaint, fontSize = 12.sp)
                    }
                    Switch(checked = blocked, onCheckedChange = { blocked = it })
                }
                if (!blocked) {
                    Spacer(Modifier.height(14.dp))
                    StepperRow(stringResource(R.string.breath_seconds), cooldown, 0, 120, 1) { cooldown = it }
                    Spacer(Modifier.height(8.dp))
                    StepperRow(stringResource(R.string.reason_minimum), minChars, 0, 200, 5) { minChars = it }
                    Spacer(Modifier.height(8.dp))
                    StepperRow(stringResource(R.string.session_gap), gap, 0, 3600, 15) { gap = it }
                }
                if (canDelete) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.remove_time_rule), color = Color(0xFFFF8195))
                    }
                }
            }
        }
    )
}

@Composable
private fun TimePickerRow(hour: Int, minute: Int, onHour: (Int) -> Unit, onMinute: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("-") { onHour((hour + 23) % 24) }
        Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.Center) {
            Text("%02d".format(hour), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        StepBtn("+") { onHour((hour + 1) % 24) }
        Text(" : ", color = InkSoft, fontSize = 17.sp)
        StepBtn("-") { onMinute((minute + 55) % 60) }
        Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.Center) {
            Text("%02d".format(minute), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        StepBtn("+") { onMinute((minute + 5) % 60) }
    }
}

private fun formatRange(rule: TimeRule): String {
    fun time(value: Int) = "%02d:%02d".format(value / 60, value % 60)
    return "${time(rule.startMinute)} - ${time(rule.endMinute)}"
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

package cz.honestlead.mezera.ui

import cz.honestlead.mezera.R
import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.honestlead.mezera.data.InterventionEvent
import cz.honestlead.mezera.ui.theme.Abandon
import cz.honestlead.mezera.ui.theme.AbandonWash
import cz.honestlead.mezera.ui.theme.Blue
import cz.honestlead.mezera.ui.theme.BlueSoft
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.BlueWash
import cz.honestlead.mezera.ui.theme.BgDeep
import cz.honestlead.mezera.ui.theme.Hero1
import cz.honestlead.mezera.ui.theme.Hero2
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkFaint
import cz.honestlead.mezera.ui.theme.InkSoft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun LazyListScope.statsSection(
    events: List<InterventionEvent>,
    onClear: () -> Unit
) {
    val total = events.size
    val abandoned = events.count { it.outcome == "abandoned" }
    val counts = LinkedHashMap<String, Int>()
    for (e in events) counts[e.label] = (counts[e.label] ?: 0) + 1
    val top = counts.maxByOrNull { it.value }?.key ?: "-"
    val bars = counts.entries.sortedByDescending { it.value }
    val maxCount = bars.firstOrNull()?.value ?: 1

    val savedMin = abandoned * 15
    item {
        HeroSummary(total = total, saved = fmtSaved(savedMin))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(abandoned.toString(), stringResource(R.string.changed_mind), Modifier.weight(1f))
            StatTile(top, stringResource(R.string.top_app), Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
    }

    item {
        Text(stringResource(R.string.frequent_apps), color = InkSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        if (bars.isEmpty()) {
            EmptyBox(stringResource(R.string.no_stats))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (b in bars) {
                    BarRow(name = b.key, count = b.value, fraction = b.value.toFloat() / maxCount)
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recent_reasons),
                color = InkSoft,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (events.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(BgDeep)
                        .clickable { onClear() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.clear), color = InkSoft, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    val recent = events.asReversed().take(50)
    if (recent.isEmpty()) {
        item { EmptyBox(stringResource(R.string.no_reasons)) }
    } else {
        items(recent.size) { i ->
            TimelineItem(recent[i])
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .card()
            .padding(20.dp)
    ) {
        Text(value, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(label, color = InkFaint, fontSize = 13.sp)
    }
}

@Composable
private fun HeroSummary(total: Int, saved: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Hero1, Hero2)))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(total.toString(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(stringResource(R.string.total_pauses), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text(saved, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(stringResource(R.string.time_saved), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
}

private fun fmtSaved(minutes: Int): String {
    if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        return if (m > 0) "$h h $m min" else "$h h"
    }
    return "$minutes min"
}

@Composable
private fun BarRow(name: String, count: Int, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            color = Ink,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.width(110.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(BgDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.06f, 1f))
                    .height(12.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Blue)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            count.toString(),
            color = InkSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun TimelineItem(e: InterventionEvent) {
    val abandoned = e.outcome == "abandoned"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .card(16.dp)
            .padding(16.dp)
    ) {
        Text(
            fmtWhen(e.ts),
            color = InkFaint,
            fontSize = 12.5.sp,
            modifier = Modifier.width(96.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (abandoned) AbandonWash else BlueWash)
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (abandoned) stringResource(R.string.abandoned) else stringResource(R.string.continued),
                        color = if (abandoned) Abandon else BlueStrong,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                e.reason.ifBlank { stringResource(R.string.no_reason) },
                color = InkSoft,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun EmptyBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(vertical = 34.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = InkFaint, fontSize = 15.sp)
    }
}

@Composable
private fun fmtWhen(ts: Long): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val hhmm = SimpleDateFormat("HH:mm", locale).format(Date(ts))
    val now = Calendar.getInstance()
    val d = Calendar.getInstance().apply { timeInMillis = ts }
    val sameDay = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yest = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay -> stringResource(R.string.today_at, hhmm)
        yest -> stringResource(R.string.yesterday_at, hhmm)
        else -> java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale).format(Date(ts))
    }
}

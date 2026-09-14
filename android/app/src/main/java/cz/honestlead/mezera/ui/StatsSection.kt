package cz.honestlead.mezera.ui

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

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(total.toString(), "zásahů celkem", Modifier.weight(1f))
            StatTile(abandoned.toString(), "rozmyslel sis to", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        StatTile(top, "kam nejčastěji", Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }

    item {
        Text("Kam nejčastěji chodíš", color = InkSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        if (bars.isEmpty()) {
            EmptyBox("Zatím žádná data. Až tě Mezera zastaví, uvidíš to tu.")
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
                "Poslední důvody",
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
                    Text("Vymazat", color = InkSoft, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    val recent = events.asReversed().take(50)
    if (recent.isEmpty()) {
        item { EmptyBox("Žádné důvody zatím.") }
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
                        if (abandoned) "rozmyslel" else "vešel",
                        color = if (abandoned) Abandon else BlueStrong,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                e.reason.ifBlank { "(bez důvodu)" },
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

private fun fmtWhen(ts: Long): String {
    val hhmm = SimpleDateFormat("HH:mm", Locale("cs")).format(Date(ts))
    val now = Calendar.getInstance()
    val d = Calendar.getInstance().apply { timeInMillis = ts }
    val sameDay = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yest = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay -> "dnes $hhmm"
        yest -> "včera $hhmm"
        else -> "${d.get(Calendar.DAY_OF_MONTH)}.${d.get(Calendar.MONTH) + 1}. $hhmm"
    }
}

package cz.honestlead.mezera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.honestlead.mezera.ui.theme.Bg
import cz.honestlead.mezera.ui.theme.Blue
import cz.honestlead.mezera.ui.theme.BlueSoft
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.BlueWash
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkFaint
import cz.honestlead.mezera.ui.theme.InkSoft
import cz.honestlead.mezera.ui.theme.LineStrong
import cz.honestlead.mezera.ui.theme.Surface
import kotlinx.coroutines.launch

@Composable
fun FeedbackEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BlueWash),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Blue,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.30f, w * 0.30f)
                )
                val tail = Path().apply {
                    moveTo(w * 0.28f, h * 0.66f)
                    lineTo(w * 0.20f, h * 0.98f)
                    lineTo(w * 0.50f, h * 0.66f)
                    close()
                }
                drawPath(tail, Blue)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Máš nápad nebo něco nefunguje?", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Napiš mi, přečtu si každý vzkaz.", color = InkFaint, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("Napsat", color = BlueStrong, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit, onSend: suspend (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSend = text.trim().length >= 3 && !busy

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            if (done) {
                TextButton(onClick = onDismiss) {
                    Text("Zavřít", color = BlueStrong, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(
                    enabled = canSend,
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            val ok = onSend(text)
                            busy = false
                            if (ok) done = true
                            else error = "Nepovedlo se odeslat. Zkontroluj připojení a zkus to znovu."
                        }
                    }
                ) {
                    Text(
                        if (busy) "Odesílám..." else "Odeslat",
                        color = if (canSend) BlueStrong else InkFaint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            if (!done && !busy) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = InkSoft) }
            }
        },
        title = {
            Text(if (done) "Odesláno" else "Napiš mi", fontWeight = FontWeight.Bold, color = Ink)
        },
        containerColor = Surface,
        text = {
            if (done) {
                Text("Úspěšně odesláno. Díky, mrknu na to.", color = InkSoft, fontSize = 14.sp)
            } else {
                Column {
                    Text("Co nefunguje, nebo co bys chtěl přidat?", color = InkSoft, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("Sem napiš svůj vzkaz...", color = InkFaint) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueSoft,
                            unfocusedBorderColor = LineStrong,
                            cursorColor = Blue,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                            focusedContainerColor = Bg,
                            unfocusedContainerColor = Bg
                        )
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = Color(0xFFFF6B81), fontSize = 13.sp)
                    }
                }
            }
        }
    )
}

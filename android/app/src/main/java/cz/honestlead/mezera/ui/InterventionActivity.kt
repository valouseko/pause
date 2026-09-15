package cz.honestlead.mezera.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.honestlead.mezera.data.InterventionEvent
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.ui.theme.Bg
import cz.honestlead.mezera.ui.theme.Blue
import cz.honestlead.mezera.ui.theme.BlueSoft
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.BlueWash
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkFaint
import cz.honestlead.mezera.ui.theme.InkSoft
import cz.honestlead.mezera.ui.theme.LineStrong
import cz.honestlead.mezera.ui.theme.MezeraTheme
import cz.honestlead.mezera.ui.theme.Surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "packageName"
        const val EXTRA_LABEL = "label"
        const val EXTRA_COOLDOWN = "cooldownSec"
        const val EXTRA_MIN_CHARS = "reasonMinChars"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "aplikace"
        val cooldownSec = intent.getIntExtra(EXTRA_COOLDOWN, 8)
        val minChars = intent.getIntExtra(EXTRA_MIN_CHARS, 20)

        setContent {
            MezeraTheme {
                InterventionScreen(
                    label = label,
                    cooldownSec = cooldownSec,
                    minChars = minChars,
                    onContinue = { reason ->
                        Store.get(this).addEvent(
                            InterventionEvent(pkg, label, System.currentTimeMillis(), reason, "continued")
                        )
                        finish()
                    },
                    onAbandon = { reason ->
                        Store.get(this).addEvent(
                            InterventionEvent(pkg, label, System.currentTimeMillis(), reason, "abandoned")
                        )
                        goHome()
                        finish()
                    }
                )
            }
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (e: Exception) {
        }
    }
}

private enum class Phase { Breath, Reason }

@Composable
private fun InterventionScreen(
    label: String,
    cooldownSec: Int,
    minChars: Int,
    onContinue: (String) -> Unit,
    onAbandon: (String) -> Unit
) {
    var phase by remember { mutableStateOf(Phase.Breath) }
    var breathLabel by remember { mutableStateOf("Nadechni se") }
    var reason by remember { mutableStateOf("") }

    val scale = remember { Animatable(0.82f) }
    val progress = remember { Animatable(0f) }
    val cooldownMs = (cooldownSec.coerceAtLeast(1)) * 1000

    // Dýchání + odpočet cooldownu
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val breathJob = launch {
            while (true) {
                breathLabel = "Nadechni se"
                scale.animateTo(1.3f, tween(4000, easing = FastOutSlowInEasing))
                breathLabel = "Zadrž"
                delay(1200)
                breathLabel = "Vydechni"
                scale.animateTo(0.82f, tween(4200, easing = FastOutSlowInEasing))
            }
        }
        launch { progress.animateTo(1f, tween(cooldownMs, easing = LinearEasing)) }
        delay(cooldownMs.toLong())
        breathJob.cancel()
        phase = Phase.Reason
    }

    // Únik zpět: během dýchání nic, u dotazu = rozmyslel jsem si to
    BackHandler(enabled = true) {
        if (phase == Phase.Reason) onAbandon(reason.trim())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = phase == Phase.Breath,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            BreathContent(label = label, breathLabel = breathLabel, scale = scale.value, progress = progress.value)
        }

        AnimatedVisibility(
            visible = phase == Phase.Reason,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(300))
        ) {
            ReasonContent(
                label = label,
                reason = reason,
                minChars = minChars,
                onReasonChange = { reason = it },
                onContinue = { onContinue(reason.trim()) },
                onAbandon = { onAbandon(reason.trim()) }
            )
        }
    }
}

@Composable
private fun BreathContent(label: String, breathLabel: String, scale: Float, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            // Prstenec postupu
            Canvas(modifier = Modifier.size(300.dp)) {
                val stroke = 3.dp.toPx()
                val d = size.minDimension - stroke
                drawCircle(
                    color = BlueWash,
                    radius = d / 2f,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = BlueSoft,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(d, d),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            // Koule
            Canvas(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            ) {
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFC9D5FF),
                            Color(0xFF7F9BFF),
                            Color(0xFF5570F4)
                        ),
                        center = Offset(size.width * 0.36f, size.height * 0.32f),
                        radius = r * 1.35f
                    ),
                    radius = r
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(40.dp))
        Text(
            text = breathLabel,
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        Text(
            text = buildAnnotatedString {
                append("Dej si vteřinu, než otevřeš ")
                withStyle(SpanStyle(color = BlueStrong, fontWeight = FontWeight.SemiBold)) {
                    append(label)
                }
                append(".")
            },
            color = InkSoft,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReasonContent(
    label: String,
    reason: String,
    minChars: Int,
    onReasonChange: (String) -> Unit,
    onContinue: () -> Unit,
    onAbandon: () -> Unit
) {
    val focus = LocalFocusManager.current
    val len = reason.trim().length
    val ok = len >= minChars

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Malá koule
        Canvas(modifier = Modifier.size(60.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFC9D5FF),
                        Color(0xFF7F9BFF),
                        Color(0xFF5570F4)
                    ),
                    center = Offset(size.width * 0.36f, size.height * 0.32f),
                    radius = r * 1.35f
                ),
                radius = r
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(26.dp))
        Text(
            text = buildAnnotatedString {
                append("Proč jdeš do ")
                withStyle(SpanStyle(color = BlueStrong)) { append(label) }
                append("?")
            },
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        Text(
            text = "Napiš to popravdě. Uvidíš to pak ve statistikách.",
            color = InkSoft,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Napiš aspoň $minChars znaků...", color = InkFaint) },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlueSoft,
                unfocusedBorderColor = LineStrong,
                cursorColor = Blue,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(
            text = "$len / $minChars",
            color = if (ok) BlueStrong else InkFaint,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                focus.clearFocus()
                onContinue()
            },
            enabled = ok,
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue,
                contentColor = Color.White,
                disabledContainerColor = BlueSoft.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Pokračovat do $label", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        TextButton(onClick = onAbandon) {
            Text("Rozmyslel jsem si to", color = InkSoft, fontSize = 15.sp)
        }
    }
}

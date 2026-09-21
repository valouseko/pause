package cz.honestlead.mezera.ui

import cz.honestlead.mezera.R
import androidx.compose.ui.res.stringResource

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import cz.honestlead.mezera.service.AppWatchService
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

class InterventionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "packageName"
        const val EXTRA_LABEL = "label"
        const val EXTRA_COOLDOWN = "cooldownSec"
        const val EXTRA_MIN_CHARS = "reasonMinChars"
        const val EXTRA_BLOCKED = "blocked"
        const val EXTRA_PAUSE_TEXT = "pauseText"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val label = intent.getStringExtra(EXTRA_LABEL) ?: getString(R.string.application)
        val cooldownSec = intent.getIntExtra(EXTRA_COOLDOWN, 8)
        val minChars = intent.getIntExtra(EXTRA_MIN_CHARS, 20)
        val blocked = intent.getBooleanExtra(EXTRA_BLOCKED, false)
        val pauseText = intent.getStringExtra(EXTRA_PAUSE_TEXT).orEmpty()

        setContent {
            MezeraTheme {
                InterventionScreen(
                    label = label,
                    cooldownSec = cooldownSec,
                    minChars = minChars,
                    blocked = blocked,
                    pauseText = pauseText,
                    onContinue = { reason ->
                        Store.get(this).addEvent(
                            InterventionEvent(pkg, label, System.currentTimeMillis(), reason, "continued")
                        )
                        reportResult(pkg, "continued")
                        finish()
                    },
                    onAbandon = { reason ->
                        Store.get(this).addEvent(
                            InterventionEvent(pkg, label, System.currentTimeMillis(), reason, "abandoned")
                        )
                        reportResult(pkg, "abandoned")
                        goHome()
                        finish()
                    }
                )
            }
        }
    }

    private fun reportResult(pkg: String, outcome: String) {
        val result = Intent(AppWatchService.ACTION_INTERVENTION_RESULT).apply {
            setPackage(packageName)
            putExtra(AppWatchService.EXTRA_RESULT_PACKAGE, pkg)
            putExtra(AppWatchService.EXTRA_RESULT_OUTCOME, outcome)
        }
        sendBroadcast(result)
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
    blocked: Boolean,
    pauseText: String,
    onContinue: (String) -> Unit,
    onAbandon: (String) -> Unit
) {
    if (blocked) {
        BackHandler(enabled = true) { onAbandon("") }
        BlockedContent(label = label, onLeave = { onAbandon("") })
        return
    }

    var phase by remember { mutableStateOf(if (cooldownSec <= 0) Phase.Reason else Phase.Breath) }
    var breathLabel by remember { mutableStateOf(R.string.breathe_in) }
    var reason by remember { mutableStateOf("") }

    val scale = remember { Animatable(0.82f) }
    val progress = remember { Animatable(0f) }
    val cooldownMs = (cooldownSec.coerceAtLeast(1)) * 1000

    // Dýchání + odpočet cooldownu
    androidx.compose.runtime.LaunchedEffect(cooldownSec) {
        if (cooldownSec <= 0) return@LaunchedEffect
        val breathJob = launch {
            while (true) {
                breathLabel = R.string.breathe_in
                scale.animateTo(1.3f, tween(4000, easing = FastOutSlowInEasing))
                breathLabel = R.string.hold
                delay(1200)
                breathLabel = R.string.breathe_out
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
            BreathContent(
                label = label,
                breathLabel = pauseText.ifBlank { stringResource(breathLabel) },
                scale = scale.value,
                progress = progress.value,
                customText = pauseText.isNotBlank()
            )
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
private fun BlockedContent(label: String, onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(92.dp)) {
                drawCircle(color = BlueWash)
                drawCircle(color = BlueSoft, radius = size.minDimension * 0.22f)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.blocked_title, label),
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.blocked_body),
                color = InkSoft,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(30.dp))
            Button(
                onClick = onLeave,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.back_home), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BreathContent(label: String, breathLabel: String, scale: Float, progress: Float, customText: Boolean) {
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
            fontSize = if (customText) 22.sp else 30.sp,
            lineHeight = if (customText) 30.sp else 36.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.before_opening, label),
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
            text = stringResource(R.string.why_opening, label),
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.reason_hint),
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
            placeholder = { Text(stringResource(R.string.reason_placeholder, minChars), color = InkFaint) },
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
            Text(stringResource(R.string.continue_to, label), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        TextButton(onClick = onAbandon) {
            Text(stringResource(R.string.abandon), color = InkSoft, fontSize = 15.sp)
        }
    }
}

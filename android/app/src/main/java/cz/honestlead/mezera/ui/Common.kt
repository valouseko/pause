package cz.honestlead.mezera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.honestlead.mezera.ui.theme.Line
import cz.honestlead.mezera.ui.theme.Surface

// Bílá karta s jemnou linkou místo těžkého stínu.
fun Modifier.card(radius: Dp = 22.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(Surface)
    .border(1.dp, Line, RoundedCornerShape(radius))

// Přechod pro dýchací kouli (logo).
val OrbBrush: Brush = Brush.radialGradient(
    colors = listOf(
        Color.White,
        Color(0xFFC9D5FF),
        Color(0xFF7F9BFF),
        Color(0xFF5570F4)
    )
)

fun Modifier.orb(size: Dp): Modifier = this
    .size(size)
    .clip(CircleShape)
    .background(OrbBrush)

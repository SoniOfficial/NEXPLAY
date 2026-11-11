package com.nexplay.game.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexplay.game.core.ui.*
import com.nexplay.game.data.Ranks
import com.nexplay.game.data.S
import com.nexplay.game.feature.checkers.levelName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Battle-royale style "match found" loading screen shown before AI / local games. */
@Composable
fun VersusScreen(game: String, mode: String, level: Int, onReady: () -> Unit) {
    val repo = rememberRepo()
    val p by repo.profile.collectAsState()
    val left = remember { Animatable(-1f) }
    val right = remember { Animatable(1f) }
    val vs = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    var found by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(900)
        found = true
        coroutineScope {
            launch { left.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
            launch { right.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
            launch { delay(350); vs.animateTo(1f, spring(dampingRatio = 0.35f)) }
            launch { bar.animateTo(1f, tween(1700, easing = LinearEasing)) }
        }
        delay(250)
        onReady()
    }
    val tips = listOf(S.t("tip1"), S.t("tip2"), S.t("tip3"))
    val tip = remember { tips.random() }
    val oppName = if (mode == "ai") (if (game == "checkers" && level == 3) "SONI" else "AI") else S.t("player") + " 2"
    val oppSub = if (mode == "ai") levelName(level) else S.t("mode_local")
    val rank = Ranks.of(p.level)

    NexBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text(S.t(game).uppercase(), color = NexColors.Cyan, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (found) S.t("match_found") else S.t("versus_searching"),
                color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            if (!found) Radar() else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.weight(1f).graphicsLayer { translationX = left.value * 600f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(p.avatar, 92)
                        Spacer(Modifier.height(8.dp))
                        Text(p.username.uppercase(), color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(rank.emoji + " " + S.t(rank.nameKey) + " " + rank.division, color = Color(rank.color), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "VS", fontSize = 40.sp, fontWeight = FontWeight.Black, color = NexColors.Pink, letterSpacing = 2.sp,
                        modifier = Modifier.graphicsLayer { scaleX = vs.value; scaleY = vs.value }
                    )
                    Column(
                        Modifier.weight(1f).graphicsLayer { translationX = right.value * 600f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(92.dp).clip(RoundedCornerShape(46.dp)).background(NexColors.SurfaceHi),
                            contentAlignment = Alignment.Center
                        ) { Text(if (mode == "ai") "🤖" else "🧑", fontSize = 46.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(oppName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(oppSub, color = NexColors.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(tip, color = NexColors.TextDim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.alpha(if (found) 1f else 0.5f))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(NexColors.SurfaceHi)) {
                Box(Modifier.fillMaxWidth(bar.value).fillMaxHeight().background(NexColors.Brand))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Radar() {
    val t by rememberInfiniteTransition(label = "radar").animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "radarT")
    Canvas(Modifier.size(180.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        for (i in 0..2) {
            val f = (t + i / 3f) % 1f
            drawCircle(NexColors.Cyan.copy(alpha = (1f - f) * 0.8f), size.width / 2f * f, c, style = Stroke(width = 4f))
        }
        drawCircle(NexColors.Cyan, 8f, c)
    }
}

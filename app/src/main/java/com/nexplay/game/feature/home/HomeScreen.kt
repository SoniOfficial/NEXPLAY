package com.nexplay.game.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.nexplay.game.data.Ranks
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexplay.game.core.nav.GameRegistry
import com.nexplay.game.core.ui.*
import com.nexplay.game.data.Profile
import com.nexplay.game.data.S
import com.nexplay.game.feature.leaderboard.LeaderboardList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onGame: (String) -> Unit,
    onSettings: () -> Unit,
    onNotifs: () -> Unit,
    onLeaderboard: () -> Unit,
    onQuickPlay: () -> Unit,
    onProfile: () -> Unit,
    onOnline: () -> Unit
) {
    val repo = rememberRepo()
    val scope = rememberCoroutineScope()
    val p by repo.profile.collectAsState()
    var online by remember { mutableStateOf<Int?>(null) }
    var reward by remember { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(GameRegistry.games.first().id) }
    LaunchedEffect(p.serverUrl) {
        while (true) {
            online = repo.onlineCount()
            delay(30_000)
        }
    }
    NexBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { LobbyTop(p) }
            item { LobbyStage(p, onProfile, onNotifs, onLeaderboard, onSettings) }
            item { StartPanel(selected, { selected = it }, { onGame(selected) }, onQuickPlay, onOnline) }
            item { StatsRow(p, online) }
            item { RankRoad(p) }
            item {
                ChestCard(p, reward) { scope.launch { reward = repo.claimLogin() } }
            }
            item { SectionTitle(S.t("games")) }
            items(GameRegistry.games.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (g in row) {
                        GameCard(
                            g.emoji, S.t(g.id),
                            if (g.onlineSupported) S.t("mode_online") + " · " + S.t("mode_ai") else S.t("mode_ai") + " · " + S.t("mode_local"),
                            g.colors, Modifier.weight(1f).height(150.dp)
                        ) { onGame(g.id) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { SectionTitle(S.t("daily")) }
            item { DailyCard(p) { bit, xp -> scope.launch { repo.claimDaily(bit, xp) } } }
            item { SectionTitle(S.t("recent")) }
            if (p.matches.isEmpty()) item { NexCard(Modifier.fillMaxWidth()) { EmptyBox(S.t("no_matches")) } }
            else items(p.matches.take(5)) { m ->
                val df = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
                NexCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(S.t(m.game) + " · " + S.t("mode_" + m.mode), color = NexColors.Text, fontWeight = FontWeight.Bold)
                            Text(m.opponent + " · " + df.format(Date(m.ts)), color = NexColors.TextDim, fontSize = 12.sp)
                        }
                        val c = when (m.result) { "win" -> NexColors.Green; "loss" -> NexColors.Red; else -> NexColors.Amber }
                        Text(S.t("res_" + m.result).uppercase(), color = c, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(S.t("ranking_title"), Modifier.weight(1f))
                    TextButton(onClick = onLeaderboard) { Text(S.t("ranking").uppercase(), color = NexColors.Cyan, fontWeight = FontWeight.Black) }
                }
            }
            item { LeaderboardList(game = "overall", limit = 3) }
        }
    }
}

private fun Modifier.androidxAlpha(a: Float): Modifier = this.then(Modifier.alpha(a))

@Composable
private fun ChestCard(p: Profile, reward: Int, onOpen: () -> Unit) {
    NexCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (p.canClaimLogin) "🎁" else "📦", fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(S.t("login_reward").uppercase(), color = NexColors.Text, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(S.t("streak") + ": ${p.streak} 🔥", color = NexColors.TextDim, fontSize = 12.sp)
                if (reward > 0) Text("+$reward 💰", color = NexColors.Amber, fontWeight = FontWeight.Black)
            }
            if (p.canClaimLogin) GlowButton(S.t("open_chest"), onOpen, colors = listOf(NexColors.Amber, Color(0xFFFF6D00)))
            else Text(S.t("come_back"), color = NexColors.TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DailyCard(p: Profile, onClaim: (Int, Int) -> Unit) {
    val d = p.daily
    val items = listOf(
        Triple(S.t("d_play3"), minOf(d.played, 3) to 3, 30),
        Triple(S.t("d_win1"), minOf(d.wins, 1) to 1, 40),
        Triple(S.t("d_beat_med"), minOf(d.beatMedium, 1) to 1, 60)
    )
    NexCard(Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, (title, prog, xp) ->
            val bit = 1 shl i
            val done = prog.first >= prog.second
            val claimed = d.claimed and bit != 0
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0x22FFFFFF))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = NexColors.Text, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { prog.first.toFloat() / prog.second },
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = NexColors.Cyan, trackColor = Color(0x33FFFFFF)
                    )
                    Text("${prog.first}/${prog.second} · +$xp XP", color = NexColors.TextDim, fontSize = 11.sp)
                }
                if (claimed) Text(S.t("claimed"), color = NexColors.Green, fontSize = 12.sp)
                else OutlineButton(S.t("claim"), { onClaim(bit, xp) }, enabled = done)
            }
        }
    }
}

@Composable
private fun LobbyTop(p: Profile) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BrandText("NEXPLAY", 26, Modifier.weight(1f))
        HudPill("💰", "${p.coins}", NexColors.Amber)
        Spacer(Modifier.width(6.dp))
        HudPill("🔥", "${p.streak}", NexColors.Pink)
    }
}

@Composable
private fun SideButton(emoji: String, label: String, badge: Int = 0, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        BadgedBox(badge = { if (badge > 0) Badge { Text("$badge") } }) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color(0x55101636))
                    .border(1.5.dp, Brush.linearGradient(listOf(NexColors.Violet, NexColors.Cyan)), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }
        }
        Text(label.uppercase(), color = NexColors.TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** Lobby "stage": rotating neon ring, platform, floating avatar, rank plate, side shortcuts. */
@Composable
private fun LobbyStage(p: Profile, onProfile: () -> Unit, onNotifs: () -> Unit, onLeaderboard: () -> Unit, onSettings: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "stage")
    val spin by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val bob by tr.animateFloat(-8f, 8f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    val rank = Ranks.of(p.level)
    val rankColor = Color(rank.color)
    Box(Modifier.fillMaxWidth().height(330.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.46f
            val r = size.width * 0.30f
            drawCircle(Brush.radialGradient(listOf(rankColor.copy(alpha = 0.35f), Color.Transparent), Offset(cx, cy), r * 1.9f), r * 1.9f, Offset(cx, cy))
            // rotating dashed ring
            for (i in 0 until 12) {
                drawArc(NexColors.Cyan.copy(alpha = 0.85f), spin + i * 30f, 16f, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(width = 6f))
            }
            for (i in 0 until 6) {
                drawArc(NexColors.Pink.copy(alpha = 0.8f), -spin * 1.4f + i * 60f, 28f, false, Offset(cx - r * 1.18f, cy - r * 1.18f), Size(r * 2.36f, r * 2.36f), style = Stroke(width = 3f))
            }
            // platform
            drawOval(Brush.radialGradient(listOf(rankColor.copy(alpha = 0.7f), Color.Transparent), Offset(cx, cy + r * 1.05f), r * 1.3f), Offset(cx - r * 1.3f, cy + r * 0.85f), Size(r * 2.6f, r * 0.5f))
        }
        Text(
            com.nexplay.game.core.ui.AVATARS[p.avatar.coerceIn(0, com.nexplay.game.core.ui.AVATARS.lastIndex)], fontSize = 110.sp,
            modifier = Modifier.align(Alignment.Center).offset(y = (-34).dp).graphicsLayer { translationY = bob }
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SideButton("🏅", S.t("achievements"), onClick = onProfile)
            SideButton("🔔", S.t("notifications"), badge = p.notifs.size, onClick = onNotifs)
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SideButton("🏆", S.t("ranking"), onClick = onLeaderboard)
            SideButton("⚙️", S.t("settings"), onClick = onSettings)
        }
        // name plate
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.82f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(6.dp))
                Text((S.t(rank.nameKey) + " " + rank.division).uppercase(), color = rankColor, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 16.sp)
            }
            Text(p.username.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LV ${p.level}", color = NexColors.Cyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { p.levelProgress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = NexColors.Cyan, trackColor = Color(0x33FFFFFF)
                )
                Spacer(Modifier.width(8.dp))
                Text("${p.xp % 100}/100", color = NexColors.TextDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StartPanel(selected: String, onSelect: (String) -> Unit, onStart: () -> Unit, onQuick: () -> Unit, onOnline: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "start").animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "startG")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (g in GameRegistry.games) {
                val on = g.id == selected
                Row(
                    Modifier
                        .clip(CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
                        .background(if (on) Brush.horizontalGradient(g.colors) else Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
                        .border(if (on) 2.dp else 1.dp, if (on) Color.White.copy(alpha = 0.8f) else Color(0x33FFFFFF), CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
                        .clickable { onSelect(g.id) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(g.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(S.t(g.id).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                }
            }
        }
        GlowButton(
            S.t("start") + "  ▶", onStart,
            Modifier.fillMaxWidth().height(62.dp).graphicsLayer { scaleX = 0.97f + 0.03f * glow; scaleY = 0.97f + 0.03f * glow },
            colors = listOf(Color(0xFFFF6D00), NexColors.Amber)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("⚡ " + S.t("quick_play"), onQuick, Modifier.weight(1f))
            OutlineButton("🌐 " + S.t("mode_online"), onOnline, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsRow(p: Profile, online: Int?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip(S.t("wins"), "${p.totalWins}", NexColors.Green, Modifier.weight(1f))
        StatChip(S.t("losses"), "${p.totalLosses}", NexColors.Red, Modifier.weight(1f))
        val t = p.totalWins + p.totalLosses
        StatChip(S.t("winrate"), if (t == 0) "—" else "${p.totalWins * 100 / t}%", NexColors.Amber, Modifier.weight(1f))
        StatChip("ONLINE", online?.toString() ?: "—", NexColors.Cyan, Modifier.weight(1f))
    }
}

/** Horizontal road of levels with rank milestones (like a season track). */
@Composable
private fun RankRoad(p: Profile) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(S.t("rank_road"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            val from = maxOf(1, p.level - 1)
            for (lv in from..(from + 11)) {
                val rk = Ranks.of(lv)
                val milestone = lv == 1 || Ranks.of(lv - 1).nameKey != rk.nameKey
                val reached = lv <= p.level
                val col = Color(rk.color)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                    Box(
                        Modifier.size(if (milestone) 46.dp else 34.dp).clip(CircleShape)
                            .background(if (reached) Brush.linearGradient(listOf(col, col.copy(alpha = 0.5f))) else Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
                            .border(if (lv == p.level) 3.dp else 1.dp, if (lv == p.level) Color.White else col.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(if (milestone) rk.emoji else "$lv", color = Color.White, fontWeight = FontWeight.Black, fontSize = if (milestone) 20.sp else 12.sp) }
                    Text(if (milestone) S.t(rk.nameKey).uppercase() else "", color = col, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
    }
}

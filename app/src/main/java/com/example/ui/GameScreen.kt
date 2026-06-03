package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.Highscore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Deep Cyberpunk Slate/Retro Arcade Hexes -> High Density Theme (Pastel M3)
val ColorDeepCoal = Color(0xFFFEF7FF)        // #FEF7FF Main background
val ColorMutedGray = Color(0xFFF7F2FA)      // #F7F2FA Slate Card container background
val ColorBrimstone = Color(0xFF6750A4)      // #6750A4 Deep brand purple
val ColorNeonGreen = Color(0xFF006A60)      // #006A60 Teal Accent
val ColorDamageRed = Color(0xFFB3261E)      // #B3261E Vibrant Crimson
val ColorFogBlack = Color(0xFF1B1B1F)       // Deep fog of war remains dark
val ColorGoldCoin = Color(0xFF21005D)       // #21005D Gold label text dark purple
val ColorWallBricks = Color(0xFFE6E1E5)     // #E6E1E5 Light grey border/wall background

val ColorHighDensityBg = Color(0xFFFEF7FF)        // #FEF7FF
val ColorHighDensityCard = Color(0xFFF7F2FA)      // #F7F2FA
val ColorHighDensityBorder = Color(0xFFCAC4D0)    // #CAC4D0
val ColorHighDensityPurple = Color(0xFF6750A4)    // #6750A4
val ColorHighDensityLightPurple = Color(0xFFEADDFF) // #EADDFF
val ColorHighDensityTextDark = Color(0xFF1D1B20)  // #1D1B20
val ColorHighDensityTextMuted = Color(0xFF49454F) // #49454F
val ColorHighDensityTextPurple = Color(0xFF21005D) // #21005D

// Console Dark theme colors (kept distinct for perfect readability inside terminals)
val ColorHighDensityConsoleDark = Color(0xFF2B2930) // #2B2930
val ColorHighDensityConsoleLog = Color(0xFFE6E1E5)   // #E6E1E5
val ColorHighDensityConsolePurple = Color(0xFFD0BCFF) // #D0BCFF
val ColorHighDensityConsoleRed = Color(0xFFF2B8B5)    // #F2B8B5
val ColorHighDensityRedAtk = Color(0xFFB3261E)       // #B3261E
val ColorHighDensityGreenHeal = Color(0xFF006A60)    // #006A60
val ColorHighDensityTileEmpty = Color(0xFFFFFFFF)     // #FFFFFF
val ColorHighDensityTileWall = Color(0xFFE6E1E5)     // #E6E1E5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val highscores by viewModel.highscores.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorHighDensityBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Main Controller / Outer state switch
        when (state.gameState) {
            GameState.EXPLORING -> {
                ExploringView(
                    state = state,
                    onMove = { dr, dc -> viewModel.movePlayer(dr, dc) },
                    onOpenInventory = { /* Managed via simple tab/sheet natively */ },
                    onShowScores = { viewModel.showHighScores() },
                    onUseItem = { viewModel.useInventoryItem(it) },
                    onResetGame = { viewModel.resetGame() },
                    onUpgradeSkill = { viewModel.upgradeSkill(it) },
                    onUpgradeElementalAffinity = { viewModel.upgradeElementalAffinity(it) },
                    onUnlockFusionSkill = { viewModel.unlockFusionSkill(it) }
                )
            }
            GameState.COMBAT -> {
                CombatView(
                    state = state,
                    onUseItem = { viewModel.useInventoryItem(it) },
                    onFlee = { viewModel.playerFlee() },
                    onCastSkill = { viewModel.castActiveSkill(it) },
                    onElementalAttack = { viewModel.executeElementalAttack(it) },
                    onFusionAttack = { viewModel.executeFusionAttack(it) }
                )
            }
            GameState.SHOP -> {
                ShopView(
                    state = state,
                    onBuy = { viewModel.buyShopItem(it) },
                    onLeave = { viewModel.leaveShop() }
                )
            }
            GameState.LEVEL_TRANSITION -> {
                LevelTransitionView(message = state.uiTransitionMessage)
            }
            GameState.GAME_OVER -> {
                GameOverView(
                    state = state,
                    onSaveScore = { viewModel.saveHighScore(it) },
                    onReset = { viewModel.resetGame() }
                )
            }
            GameState.HIGH_SCORES -> {
                HighScoresView(
                    highscores = highscores,
                    onClearScores = { viewModel.clearScores() },
                    onBack = { viewModel.leaveHighScores() }
                )
            }
        }
    }
}

// ==========================================
// 1. EXPLORING VIEW (Main Dungeon Crawl Map)
// ==========================================
@Composable
fun ExploringView(
    state: GameUIState,
    onMove: (Int, Int) -> Unit,
    onOpenInventory: () -> Unit,
    onShowScores: () -> Unit,
    onUseItem: (ShopItem) -> Unit,
    onResetGame: () -> Unit,
    onUpgradeSkill: (String) -> Unit,
    onUpgradeElementalAffinity: (ElementType) -> Unit,
    onUnlockFusionSkill: (String) -> Unit
) {
    var showInventorySheet by remember { mutableStateOf(false) }
    var showSkillsDialog by remember { mutableStateOf(false) }

    val lastLogMessage = state.logMessages.lastOrNull()?.lowercase() ?: ""
    val isPoisonLog = lastLogMessage.contains("veneno") || lastLogMessage.contains("envenenado") || lastLogMessage.contains("poison")
    val isCriticalLog = lastLogMessage.contains("crítico") || lastLogMessage.contains("critico") || lastLogMessage.contains("crítica") || lastLogMessage.contains("critica")

    val exploreFlashColor by animateColorAsState(
        targetValue = when {
            isPoisonLog -> Color(0xFF2E755D) // green poison flash
            isCriticalLog -> Color(0xFFB3261E) // crimson red critical flash
            else -> Color.Transparent
        },
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                Color.Transparent at 0
                when {
                    isPoisonLog -> Color(0xFF2E755D).copy(alpha = 0.35f) at 400
                    isCriticalLog -> Color(0xFFB3261E).copy(alpha = 0.35f) at 400
                    else -> Color.Transparent at 400
                }
                Color.Transparent at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ExploreConsoleFlash"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top HUD Dashboard
        ExplorationHeader(
            state = state,
            onShowScores = onShowScores,
            onOpenInventory = { showInventorySheet = true },
            onOpenSkills = { showSkillsDialog = true },
            onReset = onResetGame
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Center Grid: The Dungeon Map itself
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DungeonMapGrid(state = state)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Dashboard: Move DPAD controls, recent text actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Move DPAD Console
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(ColorHighDensityCard, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.5.dp, ColorHighDensityBorder), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                DPadControls(onMove = onMove)
            }

            // Right: Rich Text Log Messages (Dark High-Density Console Terminal)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .then(
                        if (exploreFlashColor != Color.Transparent) {
                            Modifier.border(2.dp, exploreFlashColor, RoundedCornerShape(24.dp))
                        } else {
                            Modifier
                        }
                    ),
                colors = CardDefaults.cardColors(containerColor = ColorHighDensityConsoleDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Console :: Mire Logs",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorHighDensityConsolePurple,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true
                        ) {
                            items(state.logMessages.reversed()) { log ->
                                val logLower = log.lowercase()
                                val logColor = when {
                                    logLower.contains("veneno") || logLower.contains("envenenado") || logLower.contains("poison") -> Color(0xFF2E755D) // poison green
                                    logLower.contains("crítico") || logLower.contains("critico") || logLower.contains("crítica") || logLower.contains("critica") -> Color(0xFFB3261E) // critical crimson
                                    logLower.contains("daño") || logLower.contains("daños") || logLower.contains("pierde") || logLower.contains("derrota") || logLower.contains("muerto") -> ColorHighDensityConsoleRed
                                    logLower.contains("encontr") || logLower.contains("poc") || logLower.contains("oro") || logLower.contains("espada") || logLower.contains("escudo") || logLower.contains("compr") || logLower.contains("sube") -> ColorHighDensityConsolePurple
                                    else -> ColorHighDensityConsoleLog
                                }
                                Text(
                                    text = "> $log",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = logColor,
                                    modifier = Modifier.padding(vertical = 1.dp),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInventorySheet) {
        InventoryDialog(
            state = state,
            onClose = { showInventorySheet = false },
            onUseItem = onUseItem
        )
    }

    if (showSkillsDialog) {
        SkillsDialog(
            state = state,
            onClose = { showSkillsDialog = false },
            onUpgrade = onUpgradeSkill,
            onUpgradeElemental = onUpgradeElementalAffinity,
            onUpgradeFusion = onUnlockFusionSkill
        )
    }
}

@Composable
fun ExplorationHeader(
    state: GameUIState,
    onShowScores: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSkills: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorHighDensityCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, ColorHighDensityBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // First Row: Level and Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mire Depth :: Marsh Floor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorHighDensityPurple,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Floor ${state.floorsCleared + 1}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = ColorHighDensityTextDark
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShowScores,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3EDF7), CircleShape)
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 15.sp
                        )
                    }

                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3EDF7), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar",
                            tint = ColorHighDensityTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Skills Upgrades Crystal Trigger
                    Button(
                        onClick = onOpenSkills,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.skillPoints > 0) ColorHighDensityLightPurple else ColorHighDensityCard,
                            contentColor = ColorHighDensityTextPurple
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .border(1.dp, if (state.skillPoints > 0) ColorHighDensityPurple else ColorHighDensityBorder, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "🔮",
                            fontSize = 13.sp
                        )
                        if (state.skillPoints > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+${state.skillPoints}",
                                color = ColorHighDensityTextPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Inventory Button
                    Button(
                        onClick = onOpenInventory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorHighDensityLightPurple,
                            contentColor = ColorHighDensityTextPurple
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("inventory_button")
                    ) {
                        Text(
                            text = "🎒",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "(${state.inventory.size})",
                            color = ColorHighDensityTextPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ColorHighDensityBorder)

            // Second Row: Character status parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HP and Mana column
                Column(modifier = Modifier.weight(1.3f)) {
                    // HP Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "❤️ HP: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityTextDark)
                            Text(text = "${state.playerHp}/${state.playerMaxHp}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityRedAtk)
                            if (state.playerPoisonTurns > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E755D), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "VENENO (${state.playerPoisonTurns}t)",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(text = "ATK: ${state.playerAtk}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ColorHighDensityTextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { state.playerHp.toFloat() / state.playerMaxHp.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ColorHighDensityRedAtk,
                        trackColor = ColorHighDensityTileWall
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Mana Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧪 PM: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityTextDark)
                            Text(text = "${state.playerMana}/${state.playerMaxMana}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityPurple)
                        }
                        Text(text = "DEF: ${state.playerDef}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ColorHighDensityTextMuted)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { state.playerMana.toFloat() / state.playerMaxMana.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ColorHighDensityPurple,
                        trackColor = ColorHighDensityTileWall
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // LEVEL & XP Badge & GOLD Pill GP Box
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NIVEL ${state.playerLevel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorHighDensityTextPurple
                    )
                    
                    Text(
                        text = "XP: ${state.playerXp}/${state.playerMaxXp}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorHighDensityTextMuted
                    )

                    // Gold Pill GP Box
                    Row(
                        modifier = Modifier
                            .background(ColorHighDensityLightPurple, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.playerGold} GP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorHighDensityTextPurple
                        )
                    }
                }
            }
        }
    }
}

// --- PIXEL-ART GRID CONFIGURATIONS & COMPACT SPRITES ---
val PLAYER_GRID = arrayOf(
    intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(0, 1, 2, 2, 2, 2, 1, 0),
    intArrayOf(0, 1, 2, 3, 3, 2, 1, 0), // 3: glowing cyan eyes
    intArrayOf(0, 1, 1, 2, 2, 1, 1, 0),
    intArrayOf(0, 0, 4, 4, 4, 4, 0, 0), // 4: cloak
    intArrayOf(0, 4, 4, 4, 4, 4, 4, 0),
    intArrayOf(0, 4, 0, 4, 4, 0, 4, 0)
)
val PLAYER_COLORS = listOf(
    Color(0xFF5B37A6), // 1: Hood purple
    Color(0xFF1D1D21), // 2: Face Shadow
    Color(0xFF00FFD1), // 3: Cyan eyes
    Color(0xFF3F2575)  // 4: Dark cloak
)

val SPIDER_GRID = arrayOf(
    intArrayOf(0, 1, 0, 0, 0, 0, 1, 0),
    intArrayOf(1, 0, 1, 0, 0, 1, 0, 1),
    intArrayOf(0, 1, 2, 2, 2, 2, 1, 0), // 2: Body
    intArrayOf(1, 2, 3, 2, 2, 3, 2, 1), // 3: Glowing red eyes
    intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
    intArrayOf(1, 0, 2, 2, 2, 2, 0, 1),
    intArrayOf(0, 1, 0, 0, 0, 0, 1, 0),
    intArrayOf(0, 0, 1, 0, 0, 1, 0, 0)
)
val SPIDER_COLORS = listOf(
    Color(0xFF1E272C), // 1: Legs
    Color(0xFF2C3E50), // 2: Body
    Color(0xFFFF2D55)  // 3: Glowing red eyes
)

val ELF_GRID = arrayOf(
    intArrayOf(0, 0, 1, 1, 1, 1, 0, 0), // 1: Green hat
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(2, 1, 3, 3, 3, 3, 1, 2), // 2: Ears, 3: Pale skin
    intArrayOf(0, 3, 4, 3, 3, 4, 3, 0), // 4: Green Eyes
    intArrayOf(0, 0, 5, 5, 5, 5, 0, 0), // 5: Outfit
    intArrayOf(0, 5, 6, 5, 5, 6, 5, 0), // 6: Bow
    intArrayOf(0, 5, 5, 5, 5, 5, 5, 0),
    intArrayOf(0, 2, 0, 0, 0, 0, 2, 0)
)
val ELF_COLORS = listOf(
    Color(0xFF27AE60), // 1: Green hat
    Color(0xFFD5C4A1), // 2: Pointy ears
    Color(0xFFFFE0BD), // 3: Skin tone
    Color(0xFF00FF87), // 4: Green glowing eyes
    Color(0xFF1E824C), // 5: Tunic
    Color(0xFFD35400)  // 6: Bow element
)

val GOBLIN_GRID = arrayOf(
    intArrayOf(0, 1, 0, 0, 0, 0, 1, 0), // Pointy ears
    intArrayOf(1, 1, 2, 2, 2, 2, 1, 1), // Green skin
    intArrayOf(1, 2, 2, 2, 2, 2, 2, 1),
    intArrayOf(0, 2, 3, 2, 2, 3, 2, 0), // Glowing orange eyes
    intArrayOf(0, 2, 2, 4, 4, 2, 2, 0), // Teeth
    intArrayOf(0, 0, 5, 5, 5, 5, 0, 0), // Tunic
    intArrayOf(0, 5, 5, 5, 5, 5, 5, 0),
    intArrayOf(0, 5, 0, 0, 0, 0, 5, 0)
)
val GOBLIN_COLORS = listOf(
    Color(0xFF2E7D32), // 1: Dark green
    Color(0xFF4CAF50), // 2: Light green face
    Color(0xFFFF9800), // 3: Orange eyes
    Color(0xFFE0E0E0), // 4: Teeth
    Color(0xFF3E2723)  // 5: Tunic
)

val GOLEM_GRID = arrayOf(
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0), // Stone blocks
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 1, 2, 1, 1, 2, 1, 1), // Blue crystal eyes
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(0, 1, 1, 3, 3, 1, 1, 0), // Stone jaw
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(1, 1, 0, 0, 0, 0, 1, 1)
)
val GOLEM_COLORS = listOf(
    Color(0xFF78909C), // 1: Stone Slate
    Color(0xFF00E5FF), // 2: Cyan energy eyes
    Color(0xFF455A64)  // 3: Shadow stone
)

val DRAGON_GRID = arrayOf(
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 1), // Horns
    intArrayOf(1, 2, 2, 2, 2, 2, 2, 1), // Dragon scales
    intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
    intArrayOf(2, 2, 3, 2, 2, 3, 2, 2), // Yellow eyes
    intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
    intArrayOf(0, 2, 4, 4, 4, 4, 1, 0), // Fangs
    intArrayOf(0, 0, 2, 2, 2, 2, 0, 0),
    intArrayOf(0, 2, 2, 2, 2, 2, 2, 0)
)
val DRAGON_COLORS = listOf(
    Color(0xFFFFB300), // 1: Gold horns
    Color(0xFFC62828), // 2: Crimson dragon scales
    Color(0xFFFFEE58), // 3: Yellow eyes
    Color(0xFFFFFFFF)  // 4: White fangs
)

val SKELETON_GRID = arrayOf(
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0), // Skull
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 2, 1, 2, 2, 1, 2, 1), // Empty eye sockets
    intArrayOf(0, 1, 1, 1, 1, 1, 1, 0),
    intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
    intArrayOf(0, 1, 2, 1, 1, 2, 1, 0), // Chest ribcage
    intArrayOf(0, 0, 1, 0, 0, 1, 0, 0),
    intArrayOf(0, 1, 0, 0, 0, 0, 1, 0)
)
val SKELETON_COLORS = listOf(
    Color(0xFFECEFF1), // 1: Bone white
    Color(0xFF263238)  // 2: Shadows
)

@Composable
fun PixelSprite(
    grid: Array<IntArray>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val rows = grid.size
        val cols = grid[0].size
        val pixelW = size.width / cols
        val pixelH = size.height / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val colorIndex = grid[r][c]
                if (colorIndex > 0 && colorIndex <= colors.size) {
                    drawRect(
                        color = colors[colorIndex - 1],
                        topLeft = androidx.compose.ui.geometry.Offset(c * pixelW, r * pixelH),
                        size = androidx.compose.ui.geometry.Size(pixelW + 0.35f, pixelH + 0.35f)
                    )
                }
            }
        }
    }
}

// 2D DUNGEON MAP COMPOSABLE WITH FOG OF WAR
@Composable
fun DungeonMapGrid(state: GameUIState) {
    if (state.grid.isEmpty()) return

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .border(1.5.dp, ColorHighDensityBorder, RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2C22)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            for (r in 0 until 10) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (c in 0 until 10) {
                        val tile = state.grid[r][c]
                        val isPlayerHere = state.playerRow == r && state.playerCol == c

                        // Dynamic relative border styling for active exploration context
                        val tileBorder = when {
                            isPlayerHere -> BorderStroke(2.dp, Color(0xFF00FFD1))
                            !tile.revealed -> null
                            tile.type == TileType.WALL -> null
                            else -> BorderStroke(0.5.dp, Color(0xFF4A5D4E).copy(alpha = 0.5f))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (tileBorder != null) Modifier.border(tileBorder, RoundedCornerShape(6.dp)) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Canvas rendering of tile background textures
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                if (!tile.revealed) {
                                    // REQUERIMIENTO: Brumas translúcidas grises en vez de bloques negros sólidos
                                    drawRect(color = Color(0xBB707A8A)) // Translucent grey background
                                    
                                    // Soft misty circular puffs
                                    drawCircle(
                                        color = Color(0x22FFFFFF),
                                        radius = size.width * 0.4f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.45f)
                                    )
                                    drawCircle(
                                        color = Color(0x11FFFFFF),
                                        radius = size.width * 0.5f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.65f)
                                    )
                                } else {
                                    if (tile.type == TileType.WALL) {
                                        // Paredes Mágicas de la Ciénaga Oscura
                                        drawRect(color = Color(0xFF253329))
                                        
                                        // Texture / brick cracks
                                        val borderLine = size.width * 0.12f
                                        drawRect(
                                            color = Color(0xFF141C16),
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            size = androidx.compose.ui.geometry.Size(size.width, borderLine)
                                        )
                                        drawRect(
                                            color = Color(0xFF141C16),
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            size = androidx.compose.ui.geometry.Size(borderLine, size.height)
                                        )
                                        // Moss highlight spot on walls
                                        drawCircle(
                                            color = Color(0xFF324F3B),
                                            radius = size.width * 0.22f,
                                            center = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.4f)
                                        )
                                    } else {
                                        // REQUERIMIENTO: Celdas transitables con tonos de lodo (#4A5D4E) y agua verde (#2E755D), musgo/hongos
                                        drawRect(color = Color(0xFF4A5D4E)) // lodo base

                                        // Canal de agua verde procedimental estable
                                        val streamSeed = (r * 7 + c * 13) % 10
                                        if (streamSeed < 4) {
                                            if (streamSeed == 0) {
                                                // Horizontal green stream
                                                drawRect(
                                                    color = Color(0xFF2E755D),
                                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.3f),
                                                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.4f)
                                                )
                                            } else if (streamSeed == 1) {
                                                // Vertical green stream
                                                drawRect(
                                                    color = Color(0xFF2E755D),
                                                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.3f, 0f),
                                                    size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height)
                                                )
                                            } else {
                                                // Splash puddle of marsh water
                                                drawCircle(
                                                    color = Color(0xFF2E755D),
                                                    radius = size.width * 0.45f,
                                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
                                                )
                                            }
                                        }

                                        // Pequeños círculos simulando musgo verde
                                        val mossSeed = (r * 17 + c * 31) % 5
                                        if (mossSeed > 1) {
                                            drawCircle(
                                                color = Color(0xFF6B8E23), // moss circle
                                                radius = size.width * 0.09f,
                                                center = androidx.compose.ui.geometry.Offset(size.width * 0.22f * mossSeed, size.height * 0.15f * (6 - mossSeed))
                                            )
                                        }

                                        // Pequeños círculos simulando hongos (setas silvestres diminutas)
                                        val mushroomSeed = (r * 11 + c * 23) % 4
                                        if (mushroomSeed == 1) {
                                            // Red mud mushroom
                                            val mX = size.width * 0.72f
                                            val mY = size.height * 0.72f
                                            drawRect(
                                                color = Color(0xFFECEFF1), // Stem
                                                topLeft = androidx.compose.ui.geometry.Offset(mX - size.width * 0.03f, mY),
                                                size = androidx.compose.ui.geometry.Size(size.width * 0.06f, size.height * 0.15f)
                                            )
                                            drawCircle(
                                                color = Color(0xFFE53935), // Red cap
                                                radius = size.width * 0.1f,
                                                center = androidx.compose.ui.geometry.Offset(mX, mY)
                                            )
                                        } else if (mushroomSeed == 2) {
                                            // Glowing yellow marsh mushroom
                                            val mX = size.width * 0.78f
                                            val mY = size.height * 0.32f
                                            drawRect(
                                                color = Color(0xFFECEFF1), // Stem
                                                topLeft = androidx.compose.ui.geometry.Offset(mX - size.width * 0.03f, mY),
                                                size = androidx.compose.ui.geometry.Size(size.width * 0.06f, size.height * 0.12f)
                                            )
                                            drawCircle(
                                                color = Color(0xFFFFEB3B), // Glowing cap
                                                radius = size.width * 0.08f,
                                                center = androidx.compose.ui.geometry.Offset(mX, mY)
                                            )
                                        }
                                    }
                                }
                            }

                            // Foreground layer: player, monsters, active nodes
                            if (isPlayerHere) {
                                // REQUERIMIENTO: Explorador con capucha en píxeles.
                                PixelSprite(
                                    grid = PLAYER_GRID,
                                    colors = PLAYER_COLORS,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            } else {
                                if (tile.revealed) {
                                    if (tile.type == TileType.ENEMY && !tile.combatDefeated) {
                                        // REQUERIMIENTO: Enemigo en píxeles (araña/elfo, etc.)
                                        val enemyType = tile.enemyType
                                        val (grid, colors) = when (enemyType) {
                                            EnemyType.ARACNIDA -> Pair(SPIDER_GRID, SPIDER_COLORS)
                                            EnemyType.ARQUERO -> Pair(ELF_GRID, ELF_COLORS)
                                            EnemyType.CLERIGO -> Pair(
                                                ELF_GRID,
                                                listOf(
                                                    Color(0xFF8E44AD), // Hood
                                                    Color(0xFFF39C12), // Trim
                                                    Color(0xFFFDEBD0), // skin
                                                    Color(0xFF9B59B6), // Purple eyes
                                                    Color(0xFF8E44AD), // Robe
                                                    Color(0xFFE67E22)  // Staff
                                                )
                                            )
                                            EnemyType.GOBLIN -> Pair(GOBLIN_GRID, GOBLIN_COLORS)
                                            EnemyType.ORCO -> Pair(
                                                GOBLIN_GRID,
                                                listOf(
                                                    Color(0xFF1B5E20), // Dark skin
                                                    Color(0xFF2E7D32), // Face
                                                    Color(0xFFFF1744), // Red eyes
                                                    Color(0xFFEEEEEE), // Ivory teeth
                                                    Color(0xFF212121)  // dark armor
                                                )
                                            )
                                            EnemyType.GOLEM -> Pair(GOLEM_GRID, GOLEM_COLORS)
                                            EnemyType.DRAGON -> Pair(DRAGON_GRID, DRAGON_COLORS)
                                            else -> Pair(SKELETON_GRID, SKELETON_COLORS)
                                        }
                                        
                                        PixelSprite(
                                            grid = grid,
                                            colors = colors,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                        )
                                    } else if (tile.combatDefeated) {
                                        // Show clean corridor walk indicator
                                        Text(
                                            text = "·",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        // Standard landmark items in modern layouts
                                        val itemIcon = when (tile.type) {
                                            TileType.START -> "🚪"
                                            TileType.EXIT -> "🌀"
                                            TileType.POTION -> "🧪"
                                            TileType.GOLD -> "🪙"
                                            TileType.SHOP -> "🧙"
                                            else -> ""
                                        }

                                        if (itemIcon.isNotEmpty()) {
                                            Text(
                                                text = itemIcon,
                                                fontSize = 13.sp,
                                                lineHeight = 13.sp
                                            )
                                        } else if (tile.type == TileType.EMPTY) {
                                            Text(
                                                text = "·",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Background color fallback helper
@Composable
fun getTileBackgroundColor(tile: Tile, isPlayer: Boolean): Color {
    if (isPlayer) return ColorHighDensityPurple
    if (!tile.revealed) return ColorHighDensityTileWall.copy(alpha = 0.4f)
    return when (tile.type) {
        TileType.WALL -> ColorHighDensityTileWall
        else -> Color.White
    }
}

// DPAD MOVEMENT CONSOLE WITH TOUCH CONTROLS
@Composable
fun DPadControls(onMove: (Int, Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Core Center button / logo
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ColorHighDensityBg, CircleShape)
                .border(1.5.dp, ColorHighDensityPurple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⚔️", fontSize = 14.sp)
        }

        // UP
        IconButton(
            onClick = { onMove(-1, 0) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(42.dp)
                .background(ColorHighDensityLightPurple, RoundedCornerShape(10.dp))
                .testTag("dpad_up")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Arriba",
                tint = ColorHighDensityPurple
            )
        }

        // DOWN
        IconButton(
            onClick = { onMove(1, 0) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(42.dp)
                .background(ColorHighDensityLightPurple, RoundedCornerShape(10.dp))
                .testTag("dpad_down")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Abajo",
                tint = ColorHighDensityPurple
            )
        }

        // LEFT
        IconButton(
            onClick = { onMove(0, -1) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(42.dp)
                .background(ColorHighDensityLightPurple, RoundedCornerShape(10.dp))
                .testTag("dpad_left")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Izquierda",
                tint = ColorHighDensityPurple
            )
        }

        // RIGHT
        IconButton(
            onClick = { onMove(0, 1) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(42.dp)
                .background(ColorHighDensityLightPurple, RoundedCornerShape(10.dp))
                .testTag("dpad_right")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Derecha",
                tint = ColorHighDensityPurple
            )
        }
    }
}

// ==========================================
// 2. TURN-BASED COMBAT VIEW (Turnos Combate)
// ==========================================
@Composable
fun CombatView(
    state: GameUIState,
    onUseItem: (ShopItem) -> Unit,
    onFlee: () -> Unit,
    onCastSkill: (String) -> Unit,
    onElementalAttack: (ElementType) -> Unit,
    onFusionAttack: (String) -> Unit
) {
    val combat = state.combatState
    val enemy = combat.activeEnemy ?: return

    val lastCombatLog = combat.log.lastOrNull()?.lowercase() ?: ""
    val isCombatPoison = lastCombatLog.contains("veneno") || lastCombatLog.contains("envenenado") || lastCombatLog.contains("poison")
    val isCombatCritical = lastCombatLog.contains("crítico") || lastCombatLog.contains("critico") || lastCombatLog.contains("crítica") || lastCombatLog.contains("critica") || lastCombatLog.contains("grave")

    val combatFlashColor by animateColorAsState(
        targetValue = when {
            isCombatPoison -> Color(0xFF2E755D) // green poison flash
            isCombatCritical -> Color(0xFFB3261E) // crimson red critical flash
            else -> Color.Transparent
        },
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                Color.Transparent at 0
                when {
                    isCombatPoison -> Color(0xFF2E755D).copy(alpha = 0.35f) at 400
                    isCombatCritical -> Color(0xFFB3261E).copy(alpha = 0.35f) at 400
                    else -> Color.Transparent at 400
                }
                Color.Transparent at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "CombatConsoleFlash"
    )

    // Attack Animators
    val playerAttackScale by animateFloatAsState(
        targetValue = if (combat.playerAttackTriggerAnimation) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "PlayerAttack"
    )

    val enemyAttackScale by animateFloatAsState(
        targetValue = if (combat.enemyAttackTriggerAnimation) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "EnemyAttack"
    )

    val shakeOffset by animateDpAsState(
        targetValue = if (combat.shakeScreenTrigger) 12.dp else 0.dp,
        animationSpec = keyframes {
            durationMillis = 300
            (-8).dp at 50
            8.dp at 100
            (-4).dp at 150
            4.dp at 200
            (-2).dp at 250
            0.dp at 300
        },
        label = "ShakeOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131C24), // deep bluish-gray
                        Color(0xFF0F2615)  // dark swampy green
                    )
                )
            )
            .padding(16.dp)
            .offset(x = shakeOffset),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Combat Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚔️ BATTLE ARENA ⚔️",
                fontSize = 11.sp,
                color = ColorHighDensityConsolePurple,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Battle Floor ${state.floorsCleared + 1}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Battlegrounds: Characters VS Monster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Player Block
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .graphicsLayer(
                        scaleX = playerAttackScale,
                        scaleY = playerAttackScale
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035).copy(alpha = 0.85f)),
                border = BorderStroke(1.5.dp, if (combat.isPlayerTurn) ColorHighDensityConsolePurple else Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                        PixelSprite(
                            grid = PLAYER_GRID,
                            colors = PLAYER_COLORS,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TU HÉROE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (combat.isPlayerTurn) ColorHighDensityConsolePurple else Color.White
                    )
                    
                    // HP Status Bar
                    Text(text = "HP: ${state.playerHp}/${state.playerMaxHp}", fontSize = 11.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { state.playerHp.toFloat() / state.playerMaxHp.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp)),
                        color = ColorHighDensityRedAtk,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // PM Status Bar
                    Text(text = "PM: ${state.playerMana}/${state.playerMaxMana}", fontSize = 11.sp, color = Color(0xFF9E8BFF), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { state.playerMana.toFloat() / state.playerMaxMana.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp)),
                        color = ColorHighDensityConsolePurple,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    if (state.playerPoisonTurns > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E755D), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "Veneno (${state.playerPoisonTurns}t)",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚔️ ATK: ${state.playerAtk}", fontSize = 11.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        Text(text = "🛡️ DEF: ${state.playerDef}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }

                    if (combat.isPlayerTurn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(ColorHighDensityPurple, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("TU TURNO", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // VS Logo
            Text(
                text = "VS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = ColorHighDensityRedAtk,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // RIGHT: Monster Block
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .graphicsLayer(
                        scaleX = enemyAttackScale,
                        scaleY = enemyAttackScale
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035).copy(alpha = 0.85f)),
                border = BorderStroke(1.5.dp, if (!combat.isPlayerTurn) ColorHighDensityConsoleRed else Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val enemyType = enemy.enemyType
                    val (grid, colors) = when (enemyType) {
                        EnemyType.ARACNIDA -> Pair(SPIDER_GRID, SPIDER_COLORS)
                        EnemyType.ARQUERO -> Pair(ELF_GRID, ELF_COLORS)
                        EnemyType.CLERIGO -> Pair(
                            ELF_GRID,
                            listOf(
                                Color(0xFF8E44AD), // Hood
                                Color(0xFFF39C12), // Trim
                                Color(0xFFFDEBD0), // skin
                                Color(0xFF9B59B6), // Purple eyes
                                Color(0xFF8E44AD), // Robe
                                Color(0xFFE67E22)  // Staff
                            )
                        )
                        EnemyType.GOBLIN -> Pair(GOBLIN_GRID, GOBLIN_COLORS)
                        EnemyType.ORCO -> Pair(
                            GOBLIN_GRID,
                            listOf(
                                Color(0xFF1B5E20), // Dark skin
                                Color(0xFF2E7D32), // Face
                                Color(0xFFFF1744), // Red eyes
                                Color(0xFFEEEEEE), // Ivory teeth
                                Color(0xFF212121)  // dark armor
                            )
                        )
                        EnemyType.GOLEM -> Pair(GOLEM_GRID, GOLEM_COLORS)
                        EnemyType.DRAGON -> Pair(DRAGON_GRID, DRAGON_COLORS)
                        EnemyType.BOSS_FANGO -> Pair(
                            SPIDER_GRID,
                            listOf(
                                Color(0xFF27AE60), // Mud green body
                                Color(0xFF2E7D32), // Dark mud
                                Color(0xFFFFCC00), // Gorgon eyes
                                Color(0xFF1B5E20), // Swamp highlights
                                Color(0xFFFF5722)  // Contrast tongue/dots
                            )
                        )
                        EnemyType.BOSS_CIENAGA -> Pair(
                            GOLEM_GRID,
                            listOf(
                                Color(0xFF3E2723), // Dark wood inquisitor
                                Color(0xFF00E676), // Green glowing eyes/symbols
                                Color(0xFF4E342E), // Accents
                                Color(0xFF8D6E63), // Trim
                                Color(0xFFB0BEC5)  // Staff/relic
                            )
                        )
                        EnemyType.BOSS_REINA -> Pair(
                            DRAGON_GRID,
                            listOf(
                                Color(0xFF4A148C), // Royal purple
                                Color(0xFF8E24AA), // Magenta venom
                                Color(0xFFFF1744), // Crimson eyes
                                Color(0xFFD500F9), // Neon purple glow
                                Color(0xFF212121)  // Obsidian armor
                            )
                        )
                        else -> Pair(SKELETON_GRID, SKELETON_COLORS)
                    }

                    Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                        PixelSprite(
                            grid = grid,
                            colors = colors,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = enemy.enemyType.enemyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!combat.isPlayerTurn) ColorHighDensityConsoleRed else Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "PS: ${enemy.currentHp}/${enemy.maxHp}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "⚔️ Daño: ${enemy.enemyType.baseAtk}", fontSize = 11.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)

                    LinearProgressIndicator(
                        progress = { enemy.currentHp.toFloat() / enemy.maxHp.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ColorHighDensityRedAtk,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logging: Combat Actions Stream Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
                .then(
                    if (combatFlashColor != Color.Transparent) {
                        Modifier.border(2.dp, combatFlashColor, RoundedCornerShape(24.dp))
                    } else {
                        Modifier
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = ColorHighDensityConsoleDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Console :: Mire Logs",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = ColorHighDensityConsolePurple,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                     modifier = Modifier.fillMaxSize(),
                     reverseLayout = true
                ) {
                    items(combat.log.reversed()) { logEvent ->
                        val logEventLower = logEvent.lowercase()
                        val col = when {
                            logEventLower.contains("veneno") || logEventLower.contains("envenenado") || logEventLower.contains("poison") -> Color(0xFF2E755D) // poison green
                            logEventLower.contains("crítico") || logEventLower.contains("critico") || logEventLower.contains("crítica") || logEventLower.contains("critica") -> Color(0xFFFF5252) // bright critical crimson
                            logEventLower.contains("daño") || logEventLower.contains("pierde") || logEventLower.contains("derrota") -> ColorHighDensityConsoleRed
                            logEventLower.contains("curado") || logEventLower.contains("gana") || logEventLower.contains("victoria") -> ColorHighDensityConsolePurple
                            else -> ColorHighDensityConsoleLog
                        }
                        Text(
                            text = "> $logEvent",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            color = col,
                            modifier = Modifier.padding(vertical = 3.5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SECCIÓN DE ATAQUES ELEMENTALES Y FUSIONES MÍSTICAS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035)),
            border = BorderStroke(1.5.dp, ColorHighDensityConsolePurple.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header weakness indicator & info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚔️ CORTES ELEMENTALES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorHighDensityConsolePurple,
                        letterSpacing = 0.5.sp
                    )
                    // Showing Enemy weakness!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Debilidad: ",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(enemy.enemyType.weakness.colorHex).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${enemy.enemyType.weakness.icon} ${enemy.enemyType.weakness.displayName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(enemy.enemyType.weakness.colorHex)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // Basic Elemental options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val elements = listOf(
                        ElementType.FUEGO,
                        ElementType.AGUA,
                        ElementType.TIERRA,
                        ElementType.AIRE
                    )
                    
                    elements.forEach { elem ->
                        val affinityLvl = when(elem) {
                            ElementType.FUEGO -> state.fireAffinityLvl
                            ElementType.AGUA -> state.waterAffinityLvl
                            ElementType.TIERRA -> state.earthAffinityLvl
                            ElementType.AIRE -> state.airAffinityLvl
                            else -> 0
                        }
                        
                        Button(
                            onClick = { onElementalAttack(elem) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B2930), // Gris Oscuro Container
                                contentColor = Color(elem.colorHex) // Bright saturated element color
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(1.5.dp, Color(elem.colorHex), RoundedCornerShape(10.dp)), // Glowing solid border
                            enabled = combat.isPlayerTurn
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${elem.icon} Nv.$affinityLvl", 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fusions Section header
                Text(
                    text = "🌀 FUSIONES CREADAS (ÁRBOL DE HABILIDADES)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Vapor
                    FusionButton(
                        id = "vapor",
                        name = "Vapor 🔥💧",
                        unlocked = state.vaporUnlocked,
                        manaCost = 12,
                        onClick = { onFusionAttack("vapor") },
                        enabled = combat.isPlayerTurn && state.playerMana >= 12,
                        modifier = Modifier.weight(1f)
                    )
                    // Tormenta
                    FusionButton(
                        id = "tormenta",
                        name = "Arena 🌪️🪨",
                        unlocked = state.tormentaUnlocked,
                        manaCost = 15,
                        onClick = { onFusionAttack("tormenta") },
                        enabled = combat.isPlayerTurn && state.playerMana >= 15,
                        modifier = Modifier.weight(1f)
                    )
                    // Lava
                    FusionButton(
                        id = "lava",
                        name = "Lava 🌋🔥",
                        unlocked = state.lavaUnlocked,
                        manaCost = 18,
                        onClick = { onFusionAttack("lava") },
                        enabled = combat.isPlayerTurn && state.playerMana >= 18,
                        modifier = Modifier.weight(1f)
                    )
                    // Hielo
                    FusionButton(
                        id = "hielo",
                        name = "Hielo ❄️💧",
                        unlocked = state.hieloUnlocked,
                        manaCost = 20,
                        onClick = { onFusionAttack("hielo") },
                        enabled = combat.isPlayerTurn && state.playerMana >= 20,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mystic Spells Selection Section: Level Up Spells
        if (state.fireBallLevel > 0 || state.healingLvl > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔮 CONJUROS CLÁSICOS (PM: ${state.playerMana}/${state.playerMaxMana})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorHighDensityPurple,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.fireBallLevel > 0) {
                        Button(
                            onClick = { onCastSkill("fireball") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorHighDensityPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            enabled = combat.isPlayerTurn && state.playerMana >= 10
                        ) {
                            Text(
                                text = "Bola Fuego 🔥 (10 PM)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (state.healingLvl > 0) {
                        Button(
                            onClick = { onCastSkill("healing") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorHighDensityGreenHeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            enabled = combat.isPlayerTurn && state.playerMana >= 12
                        ) {
                            Text(
                                text = "Curación ✨ (12 PM)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Combat Control Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // FLEE (Correr)
            Button(
                onClick = onFlee,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorHighDensityCard,
                    contentColor = ColorHighDensityTextMuted
                ),
                border = BorderStroke(1.dp, ColorHighDensityBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("combat_flee"),
                shape = RoundedCornerShape(20.dp),
                enabled = combat.isPlayerTurn
            ) {
                Text("Huir 🏃", fontWeight = FontWeight.Bold)
            }

            // INVENTORY POTION HEAL
            var showCombatHealDialog by remember { mutableStateOf(false) }
            Button(
                onClick = { showCombatHealDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorHighDensityLightPurple,
                    contentColor = ColorHighDensityTextPurple
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("combat_heal"),
                shape = RoundedCornerShape(20.dp),
                enabled = combat.isPlayerTurn
            ) {
                Text("Ítem 🎒", fontWeight = FontWeight.Bold)
            }

            if (showCombatHealDialog) {
                InventoryDialog(
                    state = state,
                    onClose = { showCombatHealDialog = false },
                    onUseItem = {
                        onUseItem(it)
                        showCombatHealDialog = false
                    }
                )
            }
        }
    }
}

// ==========================================
// 3. MERCHANT SHOP VIEW (Tienda de Objetos)
// ==========================================
@Composable
fun ShopView(
    state: GameUIState,
    onBuy: (ShopItem) -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Merchant Greeting Banner
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🧙 DUNGEON MERCH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorHighDensityTextMuted,
                letterSpacing = 2.sp
            )
            Text(
                text = "“Fine wares for shining gold...”",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = ColorHighDensityTextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Gold Wallet Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorHighDensityLightPurple),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GOLD COINS: 🪙 ${state.playerGold} GP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorHighDensityTextPurple,
                    letterSpacing = 1.sp
                )
            }
        }

        // Listing Items to Buy
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.shopItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, ColorHighDensityBorder, RoundedCornerShape(24.dp))
                        .background(ColorHighDensityCard),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤝", fontSize = 48.sp)
                        Text(
                            text = "¡Stock Agotado!",
                            color = ColorHighDensityPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Vuelve al mapa para seguir explorando.",
                            color = ColorHighDensityTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                state.shopItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ColorHighDensityCard),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, ColorHighDensityBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color.White, RoundedCornerShape(10.dp))
                                        .border(1.dp, ColorHighDensityBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.icon, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ColorHighDensityTextDark
                                    )
                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = ColorHighDensityTextDark,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Buy Action Button
                            Button(
                                onClick = { onBuy(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.playerGold >= item.cost) ColorHighDensityLightPurple else Color(0xFFF3EDF7)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = state.playerGold >= item.cost,
                                modifier = Modifier.testTag("buy_item_${item.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.cost}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.playerGold >= item.cost) ColorHighDensityTextPurple else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "🪙",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Return Button
        Button(
            onClick = onLeave,
            colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("shop_leave"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Volver a Explorar 🏃", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==========================================
// 4. LEVEL TRANSITION INTRO VIEW (Piso Siguiente)
// ==========================================
@Composable
fun LevelTransitionView(message: String) {
    val infiniteTransition = rememberInfiniteTransition("PortalZoom")
    val portalPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorHighDensityBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🌀",
                fontSize = 100.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = portalPulse
                    scaleY = portalPulse
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = ColorHighDensityTextDark,
                letterSpacing = 1.sp
            )
            Text(
                text = "Opening secure portal...",
                color = ColorHighDensityTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ==========================================
// 5. GAME OVER & SCORE SUBMIT VIEW
// ==========================================
@Composable
fun GameOverView(
    state: GameUIState,
    onSaveScore: (String) -> Unit,
    onReset: () -> Unit
) {
    var heroName by remember { mutableStateOf("Héroe") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💀 GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityRedAtk)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Has caído en el fragor de la batalla. Tu viaje ha terminado.",
            textAlign = TextAlign.Center,
            color = ColorHighDensityTextMuted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorHighDensityCard),
            border = BorderStroke(1.5.dp, ColorHighDensityBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RESUMEN DE RUN", fontSize = 11.sp, color = ColorHighDensityTextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pisos Superados:", color = ColorHighDensityTextDark)
                    Text("${state.floorsCleared} pisos", fontWeight = FontWeight.Bold, color = ColorHighDensityPurple)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Oro Acumulado:", color = ColorHighDensityTextDark)
                    Text("🪙 ${state.playerGold} GP", fontWeight = FontWeight.Bold, color = ColorHighDensityTextPurple)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ColorHighDensityBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Puntuación Final:", fontSize = 16.sp, color = ColorHighDensityTextDark, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${state.finalScore} PTS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorHighDensityPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saving high scores input
        Text(
            text = "REGISTRAR EN EL CUADRO DE HONOR",
            fontSize = 11.sp,
            color = ColorHighDensityTextMuted,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = heroName,
            onValueChange = { if (it.length <= 12) heroName = it },
            label = { Text("Nombre del Héroe") },
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorHighDensityPurple,
                unfocusedBorderColor = ColorHighDensityBorder,
                focusedLabelColor = ColorHighDensityPurple,
                focusedTextColor = ColorHighDensityTextDark,
                unfocusedTextColor = ColorHighDensityTextDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("score_name_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorHighDensityCard,
                    contentColor = ColorHighDensityTextMuted
                ),
                border = BorderStroke(1.dp, ColorHighDensityBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("gameover_retry"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Reintentar 🔄", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onSaveScore(heroName) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityPurple),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("gameover_save"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cargar Score 🏆", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ==========================================
// 6. HIGH SCORES BOARD VIEW
// ==========================================
@Composable
fun HighScoresView(
    highscores: List<Highscore>,
    onClearScores: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🏆 CAPOLÍDERES DE LA MAZMORRA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorHighDensityTextMuted,
                letterSpacing = 2.sp
            )
            Text(
                text = "Eternas Leyendas",
                color = ColorHighDensityTextDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Highscore listing
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorHighDensityCard),
            border = BorderStroke(1.5.dp, ColorHighDensityBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (highscores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay leyendas registradas aún. ¡Sé el primero!", color = ColorHighDensityTextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(highscores.size) { index ->
                        val item = highscores[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index == 0) ColorHighDensityLightPurple else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val rankCup = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "💀"
                                }
                                Text(rankCup, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.playerName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) ColorHighDensityPurple else ColorHighDensityTextDark,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Nivel: Piso ${item.floorsCleared}",
                                        fontSize = 11.sp,
                                        color = ColorHighDensityTextMuted
                                    )
                                }
                            }

                            Text(
                                text = "${item.score} PTS",
                                fontWeight = FontWeight.Bold,
                                color = ColorHighDensityPurple,
                                fontSize = 14.sp
                            )
                        }
                        HorizontalDivider(color = ColorHighDensityBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onClearScores,
                colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityRedAtk.copy(alpha = 0.8f)),
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp)
                    .testTag("scores_clear"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Limpiar 🗑️", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityPurple),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("scores_back"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Volver a Mazmorra", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =======================================================
// 7. DIALOG COMPONENT: INVENTORY (Manejo de Pociones/Armas)
// =======================================================
@Composable
fun InventoryDialog(
    state: GameUIState,
    onClose: () -> Unit,
    onUseItem: (ShopItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityPurple),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cerrar", color = Color.White)
            }
        },
        title = {
            Text(
                text = "🎒 MOCHILA DEL HÉROE",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorHighDensityTextDark
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Text(
                    text = "Presiona un objeto para utilizarlo o equiparlo. Las estadísticas se incrementarán al usar.",
                    fontSize = 11.sp,
                    color = ColorHighDensityTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.inventory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.5.dp, ColorHighDensityBorder, RoundedCornerShape(24.dp))
                            .background(ColorHighDensityCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("La mochila está vacía.", color = ColorHighDensityTextMuted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.inventory) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUseItem(item) }
                                    .testTag("inventory_item_${item.id}"),
                                colors = CardDefaults.cardColors(containerColor = ColorHighDensityCard),
                                border = BorderStroke(1.5.dp, ColorHighDensityBorder),
                                shape = RoundedCornerShape(20.dp)
                              ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.icon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ColorHighDensityTextDark
                                            )
                                            Text(
                                                text = item.description,
                                                fontSize = 10.sp,
                                                color = ColorHighDensityTextMuted,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .background(ColorHighDensityLightPurple, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (item.id.startsWith("pot")) "CONSUMIR" else "ACTIVO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorHighDensityTextPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = ColorHighDensityBg,
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun SkillsDialog(
    state: GameUIState,
    onClose: () -> Unit,
    onUpgrade: (String) -> Unit,
    onUpgradeElemental: (ElementType) -> Unit,
    onUpgradeFusion: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Clasico (Puntos Nvl), 1 = Fusiones (Monedas Or)

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = ColorHighDensityPurple),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cerrar", color = Color.White)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🔮 SENDEROS DEL PODER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorHighDensityTextDark
                )
                Text(
                    text = "Mejora tus habilidades mágicas gastando puntos de nivel o monedas de oro.",
                    fontSize = 11.sp,
                    color = ColorHighDensityTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Modern Tab selectors with pills/segments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorHighDensityCard, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1 button
                    Button(
                        onClick = { activeTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) ColorHighDensityPurple else Color.Transparent,
                            contentColor = if (activeTab == 0) Color.White else ColorHighDensityTextMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clásicos 🔮", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Tab 2 button
                    Button(
                        onClick = { activeTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) ColorHighDensityPurple else Color.Transparent,
                            contentColor = if (activeTab == 1) Color.White else ColorHighDensityTextMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Árbol Fusiones 🌀", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column {
                if (activeTab == 0) {
                    // Classical / Level skills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(ColorHighDensityLightPurple, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cristales de Nivel: ⭐ ${state.skillPoints}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorHighDensityTextPurple
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .border(1.5.dp, ColorHighDensityBorder, RoundedCornerShape(20.dp))
                            .background(ColorHighDensityCard)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Skill 1: Bola de Fuego
                        item {
                            SkillCard(
                                name = "Bola de Fuego",
                                icon = "🔥",
                                level = state.fireBallLevel,
                                description = "Hechizo activo ígneo. Ignora la defensa enemiga. Coste: 10 PM. Daño actual: ${18 + state.fireBallLevel * 8}",
                                canUpgrade = state.skillPoints > 0 && state.fireBallLevel < 3,
                                onUpgrade = { onUpgrade("fireball") }
                            )
                        }
                        // Skill 2: Curación divina
                        item {
                            SkillCard(
                                name = "Curación Divina",
                                icon = "✨",
                                level = state.healingLvl,
                                description = "Hechizo activo celestial. Sana tus PS al instante. Coste: 12 PM. Salud actual: ${20 + state.healingLvl * 10} PS",
                                canUpgrade = state.skillPoints > 0 && state.healingLvl < 3,
                                onUpgrade = { onUpgrade("healing") }
                            )
                        }
                        // Skill 3: Piel de Piedra
                        item {
                            SkillCard(
                                name = "Piel de Piedra",
                                icon = "🪨",
                                level = state.stoneSkinLvl,
                                description = "Mejora pasiva. Aumenta tus PS Máximos en +10 y tu DEF en +1 por nivel permanentemente.",
                                canUpgrade = state.skillPoints > 0 && state.stoneSkinLvl < 3,
                                onUpgrade = { onUpgrade("stoneskin") }
                            )
                        }
                        // Skill 4: Claridad Ancestral
                        item {
                            SkillCard(
                                name = "Claridad Ancestral",
                                icon = "🧠",
                                level = state.manaClarityLvl,
                                description = "Mejora pasiva. Aumenta tus PM Máx en +10 por nivel y restaura PM al ganar combates.",
                                canUpgrade = state.skillPoints > 0 && state.manaClarityLvl < 3,
                                onUpgrade = { onUpgrade("manaclarity") }
                            )
                        }
                    }
                } else {
                    // Elemental fusions branch layout!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aprende con Monedas (🪙)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorHighDensityTextMuted
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF1C5), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🪙 ${state.playerGold} Monedas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B4B00)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .border(1.5.dp, ColorHighDensityBorder, RoundedCornerShape(20.dp))
                            .background(ColorHighDensityCard)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "🔥 AFINIDADES PRIMARIAS (Coste: 30 🪙)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorHighDensityPurple,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Grid of element affinities inside lazy column
                        item {
                            val elems = listOf(
                                Triple(ElementType.FUEGO, state.fireAffinityLvl, "Fuego"),
                                Triple(ElementType.AGUA, state.waterAffinityLvl, "Agua"),
                                Triple(ElementType.TIERRA, state.earthAffinityLvl, "Tierra"),
                                Triple(ElementType.AIRE, state.airAffinityLvl, "Aire")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                elems.forEach { (elem, lvl, name) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(elem.colorHex).copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(Color(elem.colorHex).copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = elem.icon, fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = "Afinidad de $name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHighDensityTextDark)
                                                    Text(text = "Nvl $lvl/3 (+${lvl * 5} Daño elemental básico)", fontSize = 9.sp, color = Color(elem.colorHex), fontWeight = FontWeight.Medium)
                                                }
                                            }
                                            Button(
                                                onClick = { onUpgradeElemental(elem) },
                                                enabled = state.playerGold >= 30 && lvl < 3,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(elem.colorHex).copy(alpha = 0.12f),
                                                    contentColor = Color(elem.colorHex)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(text = if (lvl == 0) "Aprender" else "+Mejorar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Fusions Tree section
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🌀 CONJURO DE FUSIONES (Coste: 50 🪙)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorHighDensityPurple,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Display the 4 fusions in visual tree-link format
                        val fusions = listOf(
                            Triple("vapor", "Vapor Explosivo 💨🔥", Pair(state.fireAffinityLvl >= 1 && state.waterAffinityLvl >= 1, state.vaporUnlocked)),
                            Triple("tormenta", "Tormenta de Arena 🌪️🪨", Pair(state.earthAffinityLvl >= 1 && state.airAffinityLvl >= 1, state.tormentaUnlocked)),
                            Triple("lava", "Lava Ardiente 🌋🔥", Pair(state.fireAffinityLvl >= 1 && state.earthAffinityLvl >= 1, state.lavaUnlocked)),
                            Triple("hielo", "Hielo Glacial ❄️💧", Pair(state.waterAffinityLvl >= 1 && state.airAffinityLvl >= 1, state.hieloUnlocked))
                        )

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                fusions.forEach { (fid, name, stateInfo) ->
                                    val (prereqMet, isUnlocked) = stateInfo
                                    val reqText = when(fid) {
                                        "vapor" -> "Req: Fuego 🔥 Nv.1 + Agua 💧 Nv.1"
                                        "tormenta" -> "Req: Tierra 🪨 Nv.1 + Aire 💨 Nv.1"
                                        "lava" -> "Req: Fuego 🔥 Nv.1 + Tierra 🪨 Nv.1"
                                        "hielo" -> "Req: Agua 💧 Nv.1 + Aire 💨 Nv.1"
                                        else -> ""
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUnlocked) ColorHighDensityLightPurple.copy(alpha = 0.3f) else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp, 
                                            if (isUnlocked) ColorHighDensityPurple.copy(alpha = 0.5f) else ColorHighDensityBorder.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ColorHighDensityTextPurple
                                                    )
                                                    if (isUnlocked) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFFE2F9E5), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("Activo", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E5C27))
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = reqText,
                                                    fontSize = 8.sp,
                                                    fontWeight = if (prereqMet) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (prereqMet) Color(0xFF006A60) else ColorHighDensityRedAtk
                                                )
                                            }
                                            Button(
                                                onClick = { onUpgradeFusion(fid) },
                                                enabled = state.playerGold >= 50 && prereqMet && !isUnlocked,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ColorHighDensityPurple,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(
                                                    text = if (isUnlocked) "Conjurado" else "Unir Fusión",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SkillCard(
    name: String,
    icon: String,
    level: Int,
    description: String,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ColorHighDensityBorder.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ColorHighDensityLightPurple.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorHighDensityTextDark
                    )
                    Text(
                        text = if (level > 0) "Nivel $level/3" else "No Aprendida",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (level > 0) ColorHighDensityPurple else ColorHighDensityTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 9.sp,
                        color = ColorHighDensityTextDark,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorHighDensityLightPurple,
                    contentColor = ColorHighDensityTextPurple
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = if (level == 0) "Aprender" else "+Mejorar",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FusionButton(
    id: String,
    name: String,
    unlocked: Boolean,
    manaCost: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (unlocked) ColorHighDensityLightPurple else Color.Black.copy(alpha = 0.2f),
            contentColor = if (unlocked) ColorHighDensityTextPurple else Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier
            .height(38.dp)
            .border(
                1.dp,
                if (unlocked) ColorHighDensityPurple.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        enabled = unlocked && enabled
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (unlocked) name else "🔒 Bloq.",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (unlocked) {
                Text(
                    text = "${manaCost}PM",
                    fontSize = 7.sp,
                    color = ColorHighDensityTextPurple.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Árbol",
                    fontSize = 7.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

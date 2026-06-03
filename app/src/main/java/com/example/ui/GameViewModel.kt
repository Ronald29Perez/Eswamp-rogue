package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Highscore
import com.example.data.HighscoreRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class TileType(val symbol: String, val displayName: String) {
    START("🚪", "Entrada"),
    EXIT("🌀", "Portal"),
    POTION("🧪", "Poción"),
    ENEMY("👾", "Monstruo"),
    GOLD("🪙", "Cofre"),
    SHOP("🧙", "Mercader"),
    EMPTY(".", "Camino"),
    WALL("🧱", "Muro")
}

data class Tile(
    val row: Int,
    val col: Int,
    val type: TileType,
    val revealed: Boolean = false,
    val combatDefeated: Boolean = false,
    val enemyType: EnemyType? = null
)

enum class EnemyBehavior {
    NORMAL,
    RANGED_SNEAK, // 🏹 double damage on start unless holding Barrier Ring
    HEALER,       // 🧙‍♂️ regenerates HP every few turns
    VENOMOUS,     // 🕷️ inflicts poison damage over time
    STEEL_BODY    // 💎 blocks 4 damage from non-magic attacks
}

enum class ElementType(val displayName: String, val icon: String, val colorHex: Long) {
    NONE("Físico", "⚪", 0xFF49454F),
    AGUA("Agua", "💧", 0xFF1D8CF8),
    FUEGO("Fuego", "🔥", 0xFFFF4A55),
    TIERRA("Tierra", "🪨", 0xFFFFB236),
    AIRE("Aire", "💨", 0xFF00F1B4)
}

enum class EnemyType(
    val enemyName: String,
    val icon: String,
    val maxHp: Int,
    val baseAtk: Int,
    val scoreValue: Int,
    val behavior: EnemyBehavior,
    val weakness: ElementType
) {
    GOBLIN("Goblin Travieso", "👹", 35, 6, 50, EnemyBehavior.NORMAL, ElementType.FUEGO),
    ESQUELETO("Esqueleto Guerrero", "💀", 55, 9, 80, EnemyBehavior.NORMAL, ElementType.TIERRA),
    ARQUERO("Arquero Elfo Caído", "🏹", 45, 10, 75, EnemyBehavior.RANGED_SNEAK, ElementType.AIRE),
    ARACNIDA("Araña de las Sombras", "🕷️", 50, 8, 90, EnemyBehavior.VENOMOUS, ElementType.FUEGO),
    CLERIGO("Clérigo Siniestro", "🧙‍♂️", 65, 11, 110, EnemyBehavior.HEALER, ElementType.AGUA),
    ORCO("Orco Sangriento", "🧌", 80, 14, 150, EnemyBehavior.NORMAL, ElementType.AIRE),
    GOLEM("Golem de Obsidiana", "💎", 110, 18, 220, EnemyBehavior.STEEL_BODY, ElementType.AGUA),
    DRAGON("Dragón Rojo Primigenio", "🐉", 150, 24, 400, EnemyBehavior.NORMAL, ElementType.AGUA),
    BOSS_FANGO("Gorgona del Fango", "🐍", 110, 14, 300, EnemyBehavior.VENOMOUS, ElementType.FUEGO),
    BOSS_CIENAGA("Inquisidor de la Ciénaga", "👁️", 170, 19, 500, EnemyBehavior.HEALER, ElementType.AIRE),
    BOSS_REINA("Reina de la Ciénaga Oscura", "👾", 250, 26, 800, EnemyBehavior.VENOMOUS, ElementType.FUEGO);

    val goldFactor: Int get() = scoreValue / 10
}

enum class GameState {
    EXPLORING,
    COMBAT,
    SHOP,
    LEVEL_TRANSITION,
    GAME_OVER,
    HIGH_SCORES
}

data class ShopItem(
    val id: String,
    val name: String,
    val icon: String,
    val cost: Int,
    val hpBonus: Int = 0,
    val maxHpBonus: Int = 0,
    val atkBonus: Int = 0,
    val defBonus: Int = 0,
    val description: String
)

data class EnemyState(
    val enemyType: EnemyType,
    val currentHp: Int,
    val maxHp: Int
)

data class CombatSystemState(
    val activeEnemy: EnemyState? = null,
    val log: List<String> = emptyList(),
    val isPlayerTurn: Boolean = true,
    val playerAttackTriggerAnimation: Boolean = false,
    val enemyAttackTriggerAnimation: Boolean = false,
    val shakeScreenTrigger: Boolean = false,
    val enemyRow: Int = -1,
    val enemyCol: Int = -1,
    val turnNumber: Int = 1,
    val enemyStunnedTurns: Int = 0,
    val enemyBurnTurns: Int = 0,
    val enemyAttackReducedTurns: Int = 0
)

data class GameUIState(
    val gameState: GameState = GameState.EXPLORING,
    val floorsCleared: Int = 0,
    val playerHp: Int = 100,
    val playerMaxHp: Int = 100,
    val playerAtk: Int = 12,
    val playerDef: Int = 0,
    val playerGold: Int = 15,
    val grid: List<List<Tile>> = emptyList(),
    val playerRow: Int = 0,
    val playerCol: Int = 0,
    val inventory: List<ShopItem> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val combatState: CombatSystemState = CombatSystemState(),
    val logMessages: List<String> = emptyList(),
    val uiTransitionMessage: String = "",
    val activeHighScoreName: String = "Héroe",
    val finalScore: Int = 0,
    // Special Skills / Upgrades System Properties
    val playerLevel: Int = 1,
    val playerXp: Int = 0,
    val playerMaxXp: Int = 100,
    val playerMana: Int = 30,
    val playerMaxMana: Int = 30,
    val skillPoints: Int = 1,
    val fireBallLevel: Int = 0,
    val healingLvl: Int = 0,
    val stoneSkinLvl: Int = 0,
    val manaClarityLvl: Int = 0,
    val playerPoisonTurns: Int = 0,
    // Ancient Elemental Affinities
    val fireAffinityLvl: Int = 0,
    val waterAffinityLvl: Int = 0,
    val earthAffinityLvl: Int = 0,
    val airAffinityLvl: Int = 0,
    // Elements Fusions Unlocked States
    val vaporUnlocked: Boolean = false,
    val tormentaUnlocked: Boolean = false,
    val lavaUnlocked: Boolean = false,
    val hieloUnlocked: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HighscoreRepository
    val highscores: StateFlow<List<Highscore>>

    private val _state = MutableStateFlow(GameUIState())
    val state: StateFlow<GameUIState> = _state.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HighscoreRepository(database.highscoreDao())
        highscores = repository.topHighscores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        resetGame()
    }

    fun resetGame() {
        val initialGrid = generateLevel(1)
        val revealedGrid = revealFog(initialGrid, 0, 0)
        
        // Base inventory starts with a Small Potion
        val initialInventory = listOf(
            getPredefinedItems()[0]
        )

        _state.value = GameUIState(
            gameState = GameState.EXPLORING,
            floorsCleared = 0,
            playerHp = 100,
            playerMaxHp = 100,
            playerAtk = 12,
            playerDef = 0,
            playerGold = 15,
            grid = revealedGrid,
            playerRow = 0,
            playerCol = 0,
            inventory = initialInventory,
            shopItems = getRandomShopList(),
            logMessages = listOf("¡Bienvenido a la Mazmorra! Encuentra el portal (🌀) para descender."),
            playerLevel = 1,
            playerXp = 0,
            playerMaxXp = 100,
            playerMana = 30,
            playerMaxMana = 30,
            skillPoints = 1,
            fireBallLevel = 0,
            healingLvl = 0,
            stoneSkinLvl = 0,
            manaClarityLvl = 0,
            playerPoisonTurns = 0,
            fireAffinityLvl = 0,
            waterAffinityLvl = 0,
            earthAffinityLvl = 0,
            airAffinityLvl = 0,
            vaporUnlocked = false,
            tormentaUnlocked = false,
            lavaUnlocked = false,
            hieloUnlocked = false
        )
    }

    private fun getPredefinedItems() = listOf(
        ShopItem("pot_sm", "Poción Menor", "🧪", 12, hpBonus = 30, description = "Restaura 30 PS del jugador instantly."),
        ShopItem("pot_lg", "Poción Mayor", "🧪", 25, hpBonus = 70, description = "Restaura 70 PS, ideal para batallas difíciles."),
        ShopItem("pot_ant", "Hierba Antitóxica", "🌿", 15, hpBonus = 10, description = "Sana 10 PS y cura el veneno instantáneamente."),
        ShopItem("pot_mana", "Elixir de Maná", "🧪", 12, description = "Restaura 20 PM (Maná) para poder lanzar hechizos."),
        ShopItem("ring_barr", "Anillo de Barrera", "💍", 32, defBonus = 1, description = "Fina sortija que otorga +1 DEF y bloquea sneak-attacks de arqueros."),
        ShopItem("sword_1", "Espada de Acero", "⚔️", 30, atkBonus = 4, description = "Incremente tu ATK permanentemente en +4."),
        ShopItem("iron_sh", "Escudo de Hierro", "🛡️", 30, defBonus = 2, description = "Otorga +2 de DEF permanentemente para reducir el daño."),
        ShopItem("elix_str", "Elixir de Fuerza", "🔮", 45, maxHpBonus = 20, atkBonus = 3, description = "Aumenta +20 PS Máx y +3 ATK permanentemente.")
    )

    private fun getRandomShopList(): List<ShopItem> {
        val all = getPredefinedItems()
        return all.shuffled().take(3)
    }

    private fun addLogMessage(msg: String) {
        _state.update { original ->
            val updatedList = (listOf(msg) + original.logMessages).take(20)
            original.copy(logMessages = updatedList)
        }
    }

    fun movePlayer(dRow: Int, dCol: Int) {
        val current = _state.value
        if (current.gameState != GameState.EXPLORING) return

        val newR = current.playerRow + dRow
        val newC = current.playerCol + dCol

        // Bounds check
        if (newR !in 0..9 || newC !in 0..9) return

        // Wall check
        val targetTile = current.grid[newR][newC]
        if (targetTile.type == TileType.WALL) {
            addLogMessage("¡Hay un muro sólido de piedra ahí!")
            return
        }

        // Move player and reveal fog
        val updatedGrid = revealFog(current.grid, newR, newC)
        _state.update { it.copy(playerRow = newR, playerCol = newC, grid = updatedGrid) }

        // Process step landings
        handleTileLanding(newR, newC)
    }

    private fun handleTileLanding(r: Int, c: Int) {
        val current = _state.value
        val tile = current.grid[r][c]

        if (tile.combatDefeated) {
            // Already interacted with, but if it is the EXIT portal allow progression!
            if (tile.type == TileType.EXIT) {
                triggerLevelTransition()
            }
            return
        }

        when (tile.type) {
            TileType.POTION -> {
                // Collect potion and add to inventory
                val potion = getPredefinedItems()[Random.nextInt(0, 2)] // Potion small or big
                val newInv = current.inventory + potion
                val cleanGrid = playSettleTile(r, c)
                _state.update { it.copy(inventory = newInv, grid = cleanGrid) }
                addLogMessage("¡Has recogido una ${potion.name} (${potion.icon}) en el suelo!")
            }
            TileType.GOLD -> {
                val foundGold = Random.nextInt(10, 25)
                val newGold = current.playerGold + foundGold
                val cleanGrid = playSettleTile(r, c)
                _state.update { it.copy(playerGold = newGold, grid = cleanGrid) }
                addLogMessage("¡Encontraste un cofre antiguo con 🪙 $foundGold monedas de oro!")
            }
            TileType.SHOP -> {
                // Enter shop keeper state
                _state.update { it.copy(gameState = GameState.SHOP, shopItems = getRandomShopList()) }
                addLogMessage("Te encuentras con el Mercader Ambulante de la Mazmorra. '¿Qué deseas comprar, guerrero?'")
            }
            TileType.ENEMY -> {
                val enemyType = tile.enemyType ?: EnemyType.GOBLIN
                startCombat(enemyType, r, c)
            }
            TileType.EXIT -> {
                val boss = tile.enemyType
                if (boss != null) {
                    addLogMessage("⚠️ ¡La salida está resguardada por el JEFE FINAL: ${boss.enemyName}! Véncelo para activar el portal.")
                    startCombat(boss, r, c)
                } else {
                    triggerLevelTransition()
                }
            }
            else -> {}
        }
    }

    private fun playSettleTile(r: Int, c: Int): List<List<Tile>> {
        return _state.value.grid.map { rowVal ->
            rowVal.map { tileVal ->
                if (tileVal.row == r && tileVal.col == c) {
                    tileVal.copy(combatDefeated = true)
                } else tileVal
            }
        }
    }

    private fun triggerLevelTransition() {
        val nextFloor = _state.value.floorsCleared + 1
        _state.update {
            it.copy(
                gameState = GameState.LEVEL_TRANSITION,
                uiTransitionMessage = "Descendiendo al Piso $nextFloor...",
                floorsCleared = nextFloor
            )
        }

        viewModelScope.launch {
            delay(1500)
            val newGrid = generateLevel(nextFloor + 1)
            val revealedGrid = revealFog(newGrid, 0, 0)
            _state.update {
                it.copy(
                    gameState = GameState.EXPLORING,
                    playerRow = 0,
                    playerCol = 0,
                    grid = revealedGrid,
                    shopItems = getRandomShopList()
                )
            }
            addLogMessage("¡Has ingresado al Piso $nextFloor de la Mazmorra! Los peligros aumentan.")
        }
    }

    // --- COMBAT ACTIONS ---
    private fun startCombat(enemy: EnemyType, r: Int, c: Int) {
        val enemyState = EnemyState(enemyType = enemy, currentHp = enemy.maxHp, maxHp = enemy.maxHp)
        val current = _state.value
        val logLines = mutableListOf("¡Un salvaje ${enemy.enemyName} ${enemy.icon} te corta el paso!")
        
        var finalPlayerHp = current.playerHp
        if (enemy.behavior == EnemyBehavior.RANGED_SNEAK) {
            val hasBarrierRing = current.inventory.any { it.id == "ring_barr" }
            if (hasBarrierRing) {
                logLines.add("🛡️ ¡Tu Anillo de Barrera bloquea por completo la emboscada a distancia!")
            } else {
                val ambushDmg = (enemy.baseAtk * 1.5).toInt()
                finalPlayerHp = (finalPlayerHp - ambushDmg).coerceAtLeast(1)
                logLines.add("🏹 ¡El ${enemy.enemyName} te embosca a distancia e inflige **$ambushDmg** de daño directo antes de empezar!")
            }
        }

        _state.update {
            it.copy(
                gameState = GameState.COMBAT,
                playerHp = finalPlayerHp,
                combatState = CombatSystemState(
                    activeEnemy = enemyState,
                    log = logLines,
                    isPlayerTurn = true,
                    enemyRow = r,
                    enemyCol = c,
                    turnNumber = 1
                )
            )
        }
    }

    fun executePlayerAttack() {
        val current = _state.value
        val combat = current.combatState
        val enemy = combat.activeEnemy ?: return
        if (!combat.isPlayerTurn || current.gameState != GameState.COMBAT) return

        viewModelScope.launch {
            // Trigger Player Attack animation / flash
            _state.update {
                it.copy(
                    combatState = combat.copy(
                        playerAttackTriggerAnimation = true,
                        shakeScreenTrigger = true
                    )
                )
            }

            // Damage formula
            val critChance = Random.nextFloat() < 0.15f
            val baseDmg = current.playerAtk
            val variance = Random.nextInt(-2, 3)
            var rawDmg = (baseDmg + variance).coerceAtLeast(1)
            
            // STEEL_BODY protection adjustment
            var reductionLog = ""
            if (enemy.enemyType.behavior == EnemyBehavior.STEEL_BODY) {
                rawDmg = (rawDmg - 4).coerceAtLeast(1)
                reductionLog = " (🛡️ Obsidiana absorbe 4)"
            }

            val dmg = if (critChance) (rawDmg * 1.5).toInt() else rawDmg
            val critText = if (critChance) " ¡GOLPE CRÍTICO!" else ""

            val newEnemyHp = (enemy.currentHp - dmg).coerceAtLeast(0)
            val updatedEnemy = enemy.copy(currentHp = newEnemyHp)

            val logEntry = "⚔️ Atacas al ${enemy.enemyType.enemyName} infligiendo **$dmg** de daño!$critText$reductionLog"
            val updatedCombatLog = combat.log + logEntry

            delay(300) // Keep screen shaking/flashing

            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        activeEnemy = updatedEnemy,
                        log = updatedCombatLog,
                        playerAttackTriggerAnimation = false,
                        shakeScreenTrigger = false
                    )
                )
            }

            delay(400) // Extra time for reaction

            if (newEnemyHp <= 0) {
                // Enemy defeated!
                handleCombatVictory(updatedEnemy)
            } else {
                // Enemy's turn
                _state.update { it.copy(combatState = it.combatState.copy(isPlayerTurn = false)) }
                executeEnemyAttack()
            }
        }
    }

    fun executeElementalAttack(element: ElementType) {
        val current = _state.value
        val combat = current.combatState
        val enemy = combat.activeEnemy ?: return
        if (!combat.isPlayerTurn || current.gameState != GameState.COMBAT) return

        viewModelScope.launch {
            // Player attack triggers
            _state.update {
                it.copy(
                    combatState = combat.copy(
                        playerAttackTriggerAnimation = true,
                        shakeScreenTrigger = true
                    )
                )
            }

            // Normal damage calculation
            val critChance = Random.nextFloat() < 0.15f
            val baseDmg = current.playerAtk
            
            // Elemental damage scaling based on affinity levels:
            val affinityLevel = when (element) {
                ElementType.FUEGO -> current.fireAffinityLvl
                ElementType.AGUA -> current.waterAffinityLvl
                ElementType.TIERRA -> current.earthAffinityLvl
                ElementType.AIRE -> current.airAffinityLvl
                else -> 0
            }
            val elementalBonus = affinityLevel * 5
            
            // Core damage
            val variance = Random.nextInt(-2, 3)
            var rawDmg = (baseDmg + elementalBonus + variance).coerceAtLeast(1)

            // STEEL_BODY modifier
            var reductionLog = ""
            if (enemy.enemyType.behavior == EnemyBehavior.STEEL_BODY) {
                rawDmg = (rawDmg - 4).coerceAtLeast(1)
                reductionLog = " (🛡️ Obsidiana absorbe 4)"
            }

            // Check enemy weakness!
            var weaknessLog = ""
            if (enemy.enemyType.weakness == element) {
                rawDmg = (rawDmg * 1.5).toInt()
                weaknessLog = " ¡Efecto Súper Eficaz (Weakness x1.5)! 💥"
            }

            val dmg = if (critChance) (rawDmg * 1.5).toInt() else rawDmg
            val critText = if (critChance) " ¡GOLPE ELEMENTAL CRÍTICO!" else ""

            val newEnemyHp = (enemy.currentHp - dmg).coerceAtLeast(0)
            val updatedEnemy = enemy.copy(currentHp = newEnemyHp)

            val logEntry = "${element.icon} Atacas con ${element.displayName} infligiendo **$dmg** de daño!$critText$reductionLog$weaknessLog"
            val updatedCombatLog = combat.log + logEntry

            delay(300)

            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        activeEnemy = updatedEnemy,
                        log = updatedCombatLog,
                        playerAttackTriggerAnimation = false,
                        shakeScreenTrigger = false
                    )
                )
            }

            delay(400)

            if (newEnemyHp <= 0) {
                handleCombatVictory(updatedEnemy)
            } else {
                applyEnemyContinuousEffectsAndPassTurn()
            }
        }
    }

    fun executeFusionAttack(fusionId: String) {
        val current = _state.value
        val combat = current.combatState
        val enemy = combat.activeEnemy ?: return
        if (!combat.isPlayerTurn || current.gameState != GameState.COMBAT) return

        // Cost check and unlock status validation
        val manaCost = when (fusionId) {
            "vapor" -> 12
            "tormenta" -> 15
            "lava" -> 18
            "hielo" -> 20
            else -> 0
        }
        
        val isUnlocked = when (fusionId) {
            "vapor" -> current.vaporUnlocked
            "tormenta" -> current.tormentaUnlocked
            "lava" -> current.lavaUnlocked
            "hielo" -> current.hieloUnlocked
            else -> false
        }

        if (!isUnlocked) {
            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        log = it.combatState.log + "❌ ¡Esta fusión no ha sido desbloqueada en tu árbol elemental!"
                    )
                )
            }
            return
        }

        if (current.playerMana < manaCost) {
            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        log = it.combatState.log + "❌ ¡Maná insuficiente! Requiere $manaCost PM."
                    )
                )
            }
            return
        }

        // Deduct mana
        _state.update { it.copy(playerMana = (it.playerMana - manaCost).coerceAtLeast(0)) }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    combatState = combat.copy(
                        playerAttackTriggerAnimation = true,
                        shakeScreenTrigger = true
                    )
                )
            }

            var baseMult = 1.0f
            var statusInflicted = ""
            var stunTurns = 0
            var burnTurns = 0
            var atkReducedTurns = 0

            when (fusionId) {
                "vapor" -> {
                    baseMult = 2.2f
                    statusInflicted = "💨🔥 ¡Fusión de Vapor quema la piel de la criatura!"
                }
                "tormenta" -> {
                    baseMult = 1.8f
                    atkReducedTurns = 3
                    statusInflicted = "🌪️🪨 ¡Tormenta de Arena reduce la precisión y ataque del enemigo por 3 turnos!"
                }
                "lava" -> {
                    baseMult = 2.5f
                    burnTurns = 3
                    statusInflicted = "🌋🔥 ¡Magma Fundido quema ferozmente infligiendo daño continuo por 3 turnos!"
                }
                "hielo" -> {
                    baseMult = 1.5f
                    stunTurns = 1
                    statusInflicted = "❄️💧 ¡Hielo Glacial CONGELA por completo al enemigo, perdiendo su siguiente turno!"
                }
            }

            // Normal damage calculation
            val critChance = Random.nextFloat() < 0.15f
            var damageBase = (current.playerAtk * baseMult).toInt()

            // Is enemy weak to any of the associated elements?
            val isWeak = when (fusionId) {
                "vapor" -> enemy.enemyType.weakness == ElementType.FUEGO || enemy.enemyType.weakness == ElementType.AGUA
                "tormenta" -> enemy.enemyType.weakness == ElementType.TIERRA || enemy.enemyType.weakness == ElementType.AIRE
                "lava" -> enemy.enemyType.weakness == ElementType.FUEGO || enemy.enemyType.weakness == ElementType.TIERRA
                "hielo" -> enemy.enemyType.weakness == ElementType.AGUA || enemy.enemyType.weakness == ElementType.AIRE
                else -> false
            }

            var weaknessLog = ""
            if (isWeak) {
                damageBase = (damageBase * 1.8).toInt() // Massive elemental weakness boost!
                weaknessLog = " ¡Súper Eficaz Absoluto (x1.8)! 💥⚡"
            }

            val dmg = if (critChance) (damageBase * 1.5).toInt() else damageBase
            val critText = if (critChance) " ¡GOLPE MÍSTICO CRÍTICO!" else ""

            val newEnemyHp = (enemy.currentHp - dmg).coerceAtLeast(0)
            val updatedEnemy = enemy.copy(currentHp = newEnemyHp)

            val logEntry = "⚡ ¡CONJURO FUSIÓN! Usas $fusionId infligiendo **$dmg** de daño!$critText$weaknessLog. $statusInflicted"
            val updatedCombatLog = combat.log + logEntry

            delay(300)

            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        activeEnemy = updatedEnemy,
                        log = updatedCombatLog,
                        enemyStunnedTurns = it.combatState.enemyStunnedTurns + stunTurns,
                        enemyBurnTurns = it.combatState.enemyBurnTurns + burnTurns,
                        enemyAttackReducedTurns = it.combatState.enemyAttackReducedTurns + atkReducedTurns,
                        playerAttackTriggerAnimation = false,
                        shakeScreenTrigger = false
                    )
                )
            }

            delay(400)

            if (newEnemyHp <= 0) {
                handleCombatVictory(updatedEnemy)
            } else {
                applyEnemyContinuousEffectsAndPassTurn()
            }
        }
    }

    private fun applyEnemyContinuousEffectsAndPassTurn() {
        val current = _state.value
        val combat = current.combatState
        val enemy = combat.activeEnemy ?: return

        var enemyHp = enemy.currentHp
        val logLines = mutableListOf<String>()

        // 1. Burn continuous damage
        var burnTurns = combat.enemyBurnTurns
        if (burnTurns > 0) {
            val burnDmg = 10
            enemyHp = (enemyHp - burnDmg).coerceAtLeast(0)
            logLines.add("🔥 ¡El magma quema a la criatura por **$burnDmg** PS! (Restan $burnTurns turnos quemando)")
            burnTurns--
        }

        val updatedEnemy = enemy.copy(currentHp = enemyHp)

        // Update state
        _state.update {
            it.copy(
                combatState = it.combatState.copy(
                    activeEnemy = updatedEnemy,
                    enemyBurnTurns = burnTurns,
                    log = it.combatState.log + logLines
                )
            )
        }

        // If continuous damage kills the enemy
        if (enemyHp <= 0) {
            handleCombatVictory(updatedEnemy)
            return
        }

        // 2. Stun skipping check
        val stunTurns = combat.enemyStunnedTurns
        if (stunTurns > 0) {
            // Skips enemy turn!
            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        enemyStunnedTurns = stunTurns - 1,
                        isPlayerTurn = true,
                        log = it.combatState.log + "❄️ ¡La bestia está congelada y pierde su turno de ataque!",
                        turnNumber = it.combatState.turnNumber + 1
                    )
                )
            }
            checkPlayerPoisonStateAfterTurn()
            return
        }

        // otherwise, enemy's normal turn
        _state.update { it.copy(combatState = it.combatState.copy(isPlayerTurn = false)) }
        executeEnemyAttack()
    }

    fun upgradeElementalAffinity(element: ElementType) {
        val current = _state.value
        val cost = 30 // Flat cost per level
        if (current.playerGold < cost) {
            addLogMessage("❌ ¡Faltan monedas de oro! Se necesitan $cost 🪙.")
            return
        }

        var newFire = current.fireAffinityLvl
        var newWater = current.waterAffinityLvl
        var newEarth = current.earthAffinityLvl
        var newAir = current.airAffinityLvl

        when (element) {
            ElementType.FUEGO -> if (newFire < 3) newFire++ else return
            ElementType.AGUA -> if (newWater < 3) newWater++ else return
            ElementType.TIERRA -> if (newEarth < 3) newEarth++ else return
            ElementType.AIRE -> if (newAir < 3) newAir++ else return
            else -> return
        }

        _state.update {
            it.copy(
                playerGold = it.playerGold - cost,
                fireAffinityLvl = newFire,
                waterAffinityLvl = newWater,
                earthAffinityLvl = newEarth,
                airAffinityLvl = newAir
            )
        }
        addLogMessage("🔥 ¡Afinidad ${element.displayName} mejorada al Nvl ${when(element){ElementType.FUEGO -> newFire; ElementType.AGUA -> newWater; ElementType.TIERRA -> newEarth; ElementType.AIRE -> newAir; else -> 0}}!")
    }

    fun unlockFusionSkill(fusionId: String) {
        val current = _state.value
        val cost = 50 // Flat cost for fusions
        if (current.playerGold < cost) {
            addLogMessage("❌ ¡Faltan monedas de oro! Se necesitan $cost 🪙.")
            return
        }

        var newVapor = current.vaporUnlocked
        var newTormenta = current.tormentaUnlocked
        var newLava = current.lavaUnlocked
        var newHielo = current.hieloUnlocked

        when (fusionId) {
            "vapor" -> {
                if (current.fireAffinityLvl < 1 || current.waterAffinityLvl < 1) {
                    addLogMessage("❌ Requiere nivel 1 en Fuego 🔥 y Agua 💧.")
                    return
                }
                if (newVapor) return
                newVapor = true
            }
            "tormenta" -> {
                if (current.earthAffinityLvl < 1 || current.airAffinityLvl < 1) {
                    addLogMessage("❌ Requiere nivel 1 en Tierra 🪨 y Aire 💨.")
                    return
                }
                if (newTormenta) return
                newTormenta = true
            }
            "lava" -> {
                if (current.fireAffinityLvl < 1 || current.earthAffinityLvl < 1) {
                    addLogMessage("❌ Requiere nivel 1 en Fuego 🔥 y Tierra 🪨.")
                    return
                }
                if (newLava) return
                newLava = true
            }
            "hielo" -> {
                if (current.waterAffinityLvl < 1 || current.airAffinityLvl < 1) {
                    addLogMessage("❌ Requiere nivel 1 en Agua 💧 y Aire 💨.")
                    return
                }
                if (newHielo) return
                newHielo = true
            }
            else -> return
        }

        _state.update {
            it.copy(
                playerGold = it.playerGold - cost,
                vaporUnlocked = newVapor,
                tormentaUnlocked = newTormenta,
                lavaUnlocked = newLava,
                hieloUnlocked = newHielo
            )
        }
        addLogMessage("🌀 ¡Desbloqueada la combinación elemental mística: ${fusionId.uppercase()}!")
    }

    private fun executeEnemyAttack() {
        viewModelScope.launch {
            delay(600)
            val current = _state.value
            val combat = current.combatState
            val enemy = combat.activeEnemy ?: return@launch
            if (current.gameState != GameState.COMBAT) return@launch

            // Check HEALER behavior: every 3rd turn, healer restores 15 HP
            if (enemy.enemyType.behavior == EnemyBehavior.HEALER && combat.turnNumber % 3 == 0) {
                val healAmt = 15
                val newEnemyHp = (enemy.currentHp + healAmt).coerceAtMost(enemy.maxHp)
                val updatedEnemy = enemy.copy(currentHp = newEnemyHp)
                
                val logEntry = "🔮 ¡${enemy.enemyType.enemyName} usa Milagro Celestial y recupera **$healAmt** PS!"
                val updatedCombatLog = combat.log + logEntry
                
                _state.update {
                    it.copy(
                        combatState = it.combatState.copy(
                            activeEnemy = updatedEnemy,
                            log = updatedCombatLog,
                            isPlayerTurn = true,
                            turnNumber = it.combatState.turnNumber + 1
                        )
                    )
                }
                
                checkPlayerPoisonStateAfterTurn()
                return@launch
            }

            // Trigger enemy attack trigger animation
            _state.update {
                it.copy(
                    combatState = combat.copy(
                        enemyAttackTriggerAnimation = true,
                        shakeScreenTrigger = true
                    )
                )
            }

            // Enemy damages player (minus defense)
            val enemyAtk = enemy.enemyType.baseAtk
            val variance = Random.nextInt(-1, 3)
            var rawDmg = enemyAtk + variance

            // Check sandstorm attack-reduction debuff
            var debuffLog = ""
            var remainingReducedTurns = combat.enemyAttackReducedTurns
            if (remainingReducedTurns > 0) {
                rawDmg = (rawDmg * 0.5f).toInt()
                debuffLog = " (🌪️ Daño de la criatura reducido un 50% por arena)"
                remainingReducedTurns--
            }

            val netDmg = (rawDmg - current.playerDef).coerceAtLeast(1)

            val newPlayerHp = (current.playerHp - netDmg).coerceAtLeast(0)
            var logEntry = "☠️ El ${enemy.enemyType.enemyName} te ataca y te hace **$netDmg** de daño (DEF absorbe ${current.playerDef}).$debuffLog"
            
            // Check VENOMOUS behavior: poison player for 3 turns on normal attack
            var newPoisonTurns = current.playerPoisonTurns
            if (enemy.enemyType.behavior == EnemyBehavior.VENOMOUS && Random.nextFloat() < 0.6f && newPoisonTurns <= 0) {
                newPoisonTurns = 3
                logEntry += " 🕷️ ¡La mordedura te INYECTA VENENO por 3 turnos!"
            }

            val updatedCombatLog = combat.log + logEntry

            delay(350)

            _state.update {
                it.copy(
                    playerHp = newPlayerHp,
                    playerPoisonTurns = newPoisonTurns,
                    combatState = it.combatState.copy(
                        log = updatedCombatLog,
                        enemyAttackTriggerAnimation = false,
                        shakeScreenTrigger = false,
                        isPlayerTurn = true,
                        enemyAttackReducedTurns = remainingReducedTurns,
                        turnNumber = it.combatState.turnNumber + 1
                    )
                )
            }

            if (newPlayerHp <= 0) {
                // Player died!
                handlePlayerDefeat()
            } else {
                checkPlayerPoisonStateAfterTurn()
            }
        }
    }

    private fun checkPlayerPoisonStateAfterTurn() {
        val current = _state.value
        val remainingPoison = current.playerPoisonTurns
        if (remainingPoison > 0) {
            val poisonDmg = 4
            val postPoisonHp = (current.playerHp - poisonDmg).coerceAtLeast(0)
            val poisonLog = "🤢 El veneno te quema la sangre: pierdes **$poisonDmg** de vida! (Veneno restante: ${remainingPoison - 1} turnos)"
            
            _state.update {
                it.copy(
                    playerHp = postPoisonHp,
                    playerPoisonTurns = remainingPoison - 1,
                    combatState = it.combatState.copy(
                        log = it.combatState.log + poisonLog
                    )
                )
            }
            if (postPoisonHp <= 0) {
                handlePlayerDefeat()
            }
        }
    }

    private fun handleCombatVictory(defeatedEnemy: EnemyState) {
        val current = _state.value
        val goldWon = defeatedEnemy.enemyType.goldFactor + Random.nextInt(2, 8)
        val scoreIncrement = defeatedEnemy.enemyType.scoreValue

        // XP Reward based on enemy strength
        val baseXP = when (defeatedEnemy.enemyType.behavior) {
            EnemyBehavior.NORMAL -> 20
            EnemyBehavior.RANGED_SNEAK -> 30
            EnemyBehavior.VENOMOUS -> 35
            EnemyBehavior.HEALER -> 40
            EnemyBehavior.STEEL_BODY -> 55
        }
        val bossBonus = when (defeatedEnemy.enemyType) {
            EnemyType.DRAGON -> 100
            EnemyType.BOSS_FANGO -> 120
            EnemyType.BOSS_CIENAGA -> 180
            EnemyType.BOSS_REINA -> 250
            else -> 0
        }
        val xpWon = baseXP + bossBonus

        val finalGold = current.playerGold + goldWon
        val nextFinalScore = current.finalScore + scoreIncrement

        // Level up check
        var newXp = current.playerXp + xpWon
        var newLevel = current.playerLevel
        var newMaxXp = current.playerMaxXp
        var newMaxHp = current.playerMaxHp
        var newMaxMana = current.playerMaxMana
        var newHp = current.playerHp
        var newMana = current.playerMana
        var newSkillPoints = current.skillPoints
        var levelUpOccurred = false

        if (newXp >= newMaxXp) {
            newLevel++
            newXp -= newMaxXp
            newMaxXp = newLevel * 100
            newSkillPoints++ // Earn 1 Skill Point
            
            // Recalculate based on passive skills
            val stoneSkinMaxHpBonus = current.stoneSkinLvl * 10
            val manaClarityMaxManaBonus = current.manaClarityLvl * 10

            newMaxHp = 100 + stoneSkinMaxHpBonus
            newMaxMana = 30 + manaClarityMaxManaBonus
            newHp = newMaxHp // Full HP
            newMana = newMaxMana // Full Mana
            levelUpOccurred = true
        } else {
            // Restore a tiny bit of mana after victory
            val bonusManaRestore = 3 + current.manaClarityLvl * 3
            newMana = (newMana + bonusManaRestore).coerceAtMost(newMaxMana)
        }

        // Clean enemy from grid MAP
        val cleanGrid = playSettleTile(current.combatState.enemyRow, current.combatState.enemyCol)

        val isBossDebacle = defeatedEnemy.enemyType in listOf(EnemyType.BOSS_FANGO, EnemyType.BOSS_CIENAGA, EnemyType.BOSS_REINA)
        val victoryMessage = if (isBossDebacle) {
            "🏆 ¡DERROTASTES AL JEFE: ${defeatedEnemy.enemyType.enemyName}! El portal (🌀) a las profundidades de la ciénaga se ha activado. Interactúa con él para avanzar. Ganas 🪙 $goldWon de oro y +$xpWon XP."
        } else {
            "¡Victoria! Derrotaste al ${defeatedEnemy.enemyType.enemyName} y obtuviste 🪙 $goldWon de oro, +$xpWon XP."
        }
        val levelUpMessage = if (levelUpOccurred) "🌟 ¡NUEVO NIVEL! Subes al Nivel $newLevel. HP/Mana restablecidos y ganas +1 Punto de Habilidad (🔮)." else ""
        
        addLogMessage(victoryMessage)
        if (levelUpOccurred) {
            addLogMessage(levelUpMessage)
        }

        _state.update {
            it.copy(
                gameState = GameState.EXPLORING,
                playerGold = finalGold,
                grid = cleanGrid,
                finalScore = nextFinalScore,
                playerXp = newXp,
                playerLevel = newLevel,
                playerMaxXp = newMaxXp,
                playerMaxHp = newMaxHp,
                playerHp = newHp,
                playerMaxMana = newMaxMana,
                playerMana = newMana,
                skillPoints = newSkillPoints
            )
        }
    }

    private fun handlePlayerDefeat() {
        addLogMessage("💥 ¡Has sucumbido ante los peligros de la mazmorra!")
        val current = _state.value
        val calcScore = (current.floorsCleared * 500) + (current.finalScore) + (current.playerGold * 2)

        _state.update {
            it.copy(
                gameState = GameState.GAME_OVER,
                finalScore = calcScore
            )
        }
    }

    fun playerFlee() {
        val current = _state.value
        if (current.gameState != GameState.COMBAT) return

        viewModelScope.launch {
            val success = Random.nextFloat() < 0.6f
            val combat = current.combatState
            val enemy = combat.activeEnemy ?: return@launch

            if (success) {
                addLogMessage("¡Escapaste con éxito del combate!")
                _state.update {
                    it.copy(
                        gameState = GameState.EXPLORING
                    )
                }
            } else {
                val escapeLog = "🏃 Intentas huir pero el ${enemy.enemyType.enemyName} bloquea tu escape y te ataca por la espalda!"
                _state.update {
                    it.copy(
                        combatState = combat.copy(
                            log = combat.log + escapeLog,
                            isPlayerTurn = false
                        )
                    )
                }
                executeEnemyAttack()
            }
        }
    }

    // --- CONSUME INVENTORY ITEM ---
    fun useInventoryItem(item: ShopItem) {
        val current = _state.value
        
        // Find if user owns item
        val hasItem = current.inventory.contains(item)
        if (!hasItem) return

        // Consume item effects
        var newHp = current.playerHp
        var newMaxHp = current.playerMaxHp
        var newAtk = current.playerAtk
        var newDef = current.playerDef
        var newMana = current.playerMana
        var newMaxMana = current.playerMaxMana
        var poisonTurns = current.playerPoisonTurns

        if (item.id == "pot_ant") {
            poisonTurns = 0
            newHp = (newHp + item.hpBonus).coerceAtMost(newMaxHp)
        } else if (item.id == "pot_mana") {
            newMana = (newMana + 20).coerceAtMost(newMaxMana)
        } else {
            if (item.hpBonus > 0) {
                newHp = (newHp + item.hpBonus).coerceAtMost(newMaxHp)
            }
            if (item.maxHpBonus > 0) {
                newMaxHp += item.maxHpBonus
                newHp = (newHp + item.maxHpBonus).coerceAtMost(newMaxHp) // heals too
            }
            if (item.atkBonus > 0) {
                newAtk += item.atkBonus
            }
            if (item.defBonus > 0) {
                newDef += item.defBonus
            }
        }

        // Remove 1 instance of item from inventory
        val mutableInv = current.inventory.toMutableList()
        mutableInv.remove(item)

        _state.update {
            it.copy(
                playerHp = newHp,
                playerMaxHp = newMaxHp,
                playerAtk = newAtk,
                playerDef = newDef,
                playerMana = newMana,
                playerPoisonTurns = poisonTurns,
                inventory = mutableInv
            )
        }

        val logText = "🎒 Usaste: ${item.icon} ${item.name}. PS: $newHp/$newMaxHp | PM: $newMana/$newMaxMana | ATK: $newAtk | DEF: $newDef"
        addLogMessage(logText)

        // If in combat, logs should also update
        if (current.gameState == GameState.COMBAT) {
            val poisonCleanMsg = if (item.id == "pot_ant") " y neutralizaste el VENENO." else ""
            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        log = it.combatState.log + "🛡️ Usaste ${item.name} desde tu mochila$poisonCleanMsg"
                    )
                )
            }
        }
    }

    // --- SKILL UPGRADES AND CASTING ---
    fun upgradeSkill(skillId: String) {
        val current = _state.value
        if (current.skillPoints <= 0) return

        var newFireBall = current.fireBallLevel
        var newHealing = current.healingLvl
        var newStoneSkin = current.stoneSkinLvl
        var newManaClarity = current.manaClarityLvl

        when (skillId) {
            "fireball" -> if (newFireBall < 3) newFireBall++ else return
            "healing" -> if (newHealing < 3) newHealing++ else return
            "stoneskin" -> if (newStoneSkin < 3) newStoneSkin++ else return
            "manaclarity" -> if (newManaClarity < 3) newManaClarity++ else return
            else -> return
        }

        val newSkillPoints = current.skillPoints - 1

        // Calculate permanent passive skills upgrades
        val stoneSkinMaxHpBonus = newStoneSkin * 10
        val stoneSkinDefBonus = newStoneSkin * 1
        val manaClarityMaxManaBonus = newManaClarity * 10

        _state.update {
            val baseStatsMaxHp = 100 + stoneSkinMaxHpBonus
            val baseStatsDef = stoneSkinDefBonus
            val baseStatsMaxMana = 30 + manaClarityMaxManaBonus
            
            it.copy(
                fireBallLevel = newFireBall,
                healingLvl = newHealing,
                stoneSkinLvl = newStoneSkin,
                manaClarityLvl = newManaClarity,
                skillPoints = newSkillPoints,
                playerMaxHp = baseStatsMaxHp,
                playerHp = it.playerHp.coerceAtMost(baseStatsMaxHp),
                playerMaxMana = baseStatsMaxMana,
                playerMana = it.playerMana.coerceAtMost(baseStatsMaxMana)
            )
        }
        addLogMessage("🔮 ¡Habilidad mejorada! Nivel actual incrementado.")
    }

    fun castActiveSkill(skillId: String) {
        val current = _state.value
        val combat = current.combatState
        val enemy = combat.activeEnemy ?: return
        if (!combat.isPlayerTurn || current.gameState != GameState.COMBAT) return

        val fireBallLvl = current.fireBallLevel
        val healingLvl = current.healingLvl

        val manaCost = when (skillId) {
            "fireball" -> 10
            "healing" -> 12
            else -> 0
        }

        if (current.playerMana < manaCost) {
            _state.update {
                it.copy(
                    combatState = it.combatState.copy(
                        log = it.combatState.log + "❌ ¡Maná insuficiente para lanzar esta habilidad!"
                    )
                )
            }
            return
        }

        // Deduct mana
        val remainingMana = current.playerMana - manaCost
        _state.update { it.copy(playerMana = remainingMana) }

        viewModelScope.launch {
            if (skillId == "fireball") {
                _state.update {
                    it.copy(
                        combatState = it.combatState.copy(
                            playerAttackTriggerAnimation = true,
                            shakeScreenTrigger = true
                        )
                    )
                }

                // Fireball damage: ignores enemy defense
                val dmg = 18 + fireBallLvl * 8
                val newEnemyHp = (enemy.currentHp - dmg).coerceAtLeast(0)
                val updatedEnemy = enemy.copy(currentHp = newEnemyHp)

                val logEntry = "🔥 ¡Lanzas Bola de Fuego Nvl $fireBallLvl e infliges **$dmg** de daño místico ígneo (ignora defensas)!"
                val updatedCombatLog = combat.log + logEntry

                delay(300)

                _state.update {
                    it.copy(
                        combatState = it.combatState.copy(
                            activeEnemy = updatedEnemy,
                            log = updatedCombatLog,
                            playerAttackTriggerAnimation = false,
                            shakeScreenTrigger = false
                        )
                    )
                }

                delay(400)

                if (newEnemyHp <= 0) {
                    handleCombatVictory(updatedEnemy)
                } else {
                    _state.update {
                        it.copy(
                            combatState = it.combatState.copy(
                                isPlayerTurn = false,
                                turnNumber = it.combatState.turnNumber + 1
                            )
                        )
                    }
                    executeEnemyAttack()
                }
            } else if (skillId == "healing") {
                val healAmount = 20 + healingLvl * 10
                val newHp = (current.playerHp + healAmount).coerceAtMost(current.playerMaxHp)

                val logEntry = "✨ ¡Lanzas Plegaria Celestial Nvl $healingLvl y sanas **$healAmount** de vida!"
                val updatedCombatLog = combat.log + logEntry

                _state.update {
                    it.copy(
                        playerHp = newHp,
                        combatState = it.combatState.copy(
                            log = updatedCombatLog
                        )
                    )
                }

                delay(500)

                _state.update {
                    it.copy(
                        combatState = it.combatState.copy(
                            isPlayerTurn = false,
                            turnNumber = it.combatState.turnNumber + 1
                        )
                    )
                }
                executeEnemyAttack()
            }
        }
    }

    // --- SHOP ACTIONS ---
    fun buyShopItem(item: ShopItem) {
        val current = _state.value
        if (current.playerGold < item.cost) {
            addLogMessage("❌ ¡No tienes suficiente oro para comprar ${item.name}!")
            return
        }

        val newGold = current.playerGold - item.cost
        var newAtk = current.playerAtk
        var newDef = current.playerDef
        var newMaxHp = current.playerMaxHp
        var newHp = current.playerHp

        val inventoryUpdate = current.inventory.toMutableList()

        if (item.id.startsWith("pot")) {
            inventoryUpdate.add(item)
            addLogMessage("🛒 Compraste ${item.name} por 🪙 ${item.cost} de oro. Guardada en el inventario.")
        } else {
            if (item.atkBonus > 0) newAtk += item.atkBonus
            if (item.defBonus > 0) newDef += item.defBonus
            if (item.maxHpBonus > 0) {
                newMaxHp += item.maxHpBonus
                newHp += item.maxHpBonus
            }
            inventoryUpdate.add(item)
            addLogMessage("🛒 Compraste y equipaste ${item.icon} ${item.name} por 🪙 ${item.cost} de oro (ATK: +${item.atkBonus}, DEF: +${item.defBonus}, PS Máx: +${item.maxHpBonus}).")
        }

        val updatedShop = current.shopItems.filter { it.id != item.id }

        _state.update {
            it.copy(
                playerGold = newGold,
                playerAtk = newAtk,
                playerDef = newDef,
                playerMaxHp = newMaxHp,
                playerHp = newHp,
                inventory = inventoryUpdate,
                shopItems = updatedShop
            )
        }
    }

    fun leaveShop() {
        _state.update { it.copy(gameState = GameState.EXPLORING) }
        addLogMessage("Te despides de la tienda y sigues explorando...")
    }

    // --- HIGH SCORES SAVING ---
    fun saveHighScore(name: String) {
        val cleanName = name.ifBlank { "Héroe Desconocido" }
        _state.update { it.copy(activeHighScoreName = cleanName) }
        val current = _state.value

        viewModelScope.launch {
            repository.insertHighscore(
                Highscore(
                    playerName = cleanName,
                    floorsCleared = current.floorsCleared,
                    score = current.finalScore
                )
            )
            // Redirect to high scores page
            _state.update { it.copy(gameState = GameState.HIGH_SCORES) }
        }
    }

    fun showHighScores() {
        _state.update { it.copy(gameState = GameState.HIGH_SCORES) }
    }

    fun leaveHighScores() {
        _state.update { it.copy(gameState = GameState.EXPLORING) }
    }

    fun clearScores() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- PROCEDURAL GENERATING MAP LOGIC ---
    private fun generateLevel(floor: Int): List<List<Tile>> {
        val size = 10
        val grid = MutableList(size) { r ->
            MutableList(size) { c ->
                Tile(row = r, col = c, type = TileType.WALL)
            }
        }

        val startR = 0
        val startC = 0
        val exitR = size - 1
        val exitC = size - 1

        // 1. Carve a guaranteed pathway from Start to Exit
        var currR = startR
        var currC = startC
        grid[currR][currC] = Tile(row = currR, col = currC, type = TileType.START)

        val pathCells = mutableSetOf<Pair<Int, Int>>()
        pathCells.add(Pair(currR, currC))

        while (currR != exitR || currC != exitC) {
            val moveRow = Random.nextBoolean()
            if (moveRow && currR < exitR) {
                currR++
            } else if (currC < exitC) {
                currC++
            } else if (currR < exitR) {
                currR++
            }
            pathCells.add(Pair(currR, currC))
            
            if (currR == exitR && currC == exitC) {
                grid[currR][currC] = Tile(row = currR, col = currC, type = TileType.EXIT)
            } else {
                grid[currR][currC] = Tile(row = currR, col = currC, type = TileType.EMPTY)
            }
        }

        // 2. Expand: carve random corridors adjacent to the guaranteed path
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c].type == TileType.WALL) {
                    val isAdjacentToPath = pathCells.any { Math.abs(it.first - r) + Math.abs(it.second - c) <= 1 }
                    if (isAdjacentToPath && Random.nextFloat() < 0.65f) {
                        grid[r][c] = Tile(row = r, col = c, type = TileType.EMPTY)
                    } else if (Random.nextFloat() < 0.22f) { // Random dead ends
                        grid[r][c] = Tile(row = r, col = c, type = TileType.EMPTY)
                    }
                }
            }
        }

        // Ensure Start & Exit are intact
        grid[startR][startC] = Tile(row = startR, col = startC, type = TileType.START)
        
        // Define dangerous bosses to guard the Exit, scaling with floor depth
        val bossType = when (floor) {
            1 -> EnemyType.BOSS_FANGO
            2 -> EnemyType.BOSS_CIENAGA
            else -> EnemyType.BOSS_REINA
        }
        grid[exitR][exitC] = Tile(row = exitR, col = exitC, type = TileType.EXIT, enemyType = bossType)

        // 3. Assemble components on clean empty tiles
        val emptyTiles = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c].type == TileType.EMPTY) {
                    emptyTiles.add(Pair(r, c))
                }
            }
        }
        emptyTiles.shuffle()

        // 1 Shop Merchant
        if (emptyTiles.isNotEmpty()) {
            val (r, c) = emptyTiles.removeAt(0)
            grid[r][c] = Tile(row = r, col = c, type = TileType.SHOP)
        }

        // Monsters based on deep floor level
        val enemiesCount = (3 + (floor / 2).coerceAtMost(5)..5 + (floor / 2).coerceAtMost(5)).random()
        for (i in 0 until enemiesCount) {
            if (emptyTiles.isEmpty()) break
            val (r, c) = emptyTiles.removeAt(0)
            
            val enemyType = when {
                floor >= 6 && Random.nextFloat() < 0.22f -> EnemyType.DRAGON
                floor >= 4 && Random.nextFloat() < 0.28f -> EnemyType.GOLEM
                floor >= 3 && Random.nextFloat() < 0.35f -> EnemyType.ORCO
                floor >= 2 && Random.nextFloat() < 0.40f -> {
                    listOf(EnemyType.CLERIGO, EnemyType.ARACNIDA).random()
                }
                floor >= 1 && Random.nextFloat() < 0.45f -> {
                    listOf(EnemyType.ARQUERO, EnemyType.ESQUELETO).random()
                }
                else -> EnemyType.GOBLIN
            }
            grid[r][c] = Tile(row = r, col = c, type = TileType.ENEMY, enemyType = enemyType)
        }

        // HP Potions
        val potCount = (2..3).random()
        for (i in 0 until potCount) {
            if (emptyTiles.isEmpty()) break
            val (r, c) = emptyTiles.removeAt(0)
            grid[r][c] = Tile(row = r, col = c, type = TileType.POTION)
        }

        // Chest Gold Coins
        val goldCount = (3..5).random()
        for (i in 0 until goldCount) {
            if (emptyTiles.isEmpty()) break
            val (r, c) = emptyTiles.removeAt(0)
            grid[r][c] = Tile(row = r, col = c, type = TileType.GOLD)
        }

        return grid
    }

    private fun revealFog(grid: List<List<Tile>>, pRow: Int, pCol: Int): List<List<Tile>> {
        return grid.map { rowList ->
            rowList.map { tile ->
                val dRow = Math.abs(tile.row - pRow)
                val dCol = Math.abs(tile.col - pCol)
                if (dRow <= 1 && dCol <= 1) {
                    tile.copy(revealed = true)
                } else {
                    tile
                }
            }
        }
    }
}

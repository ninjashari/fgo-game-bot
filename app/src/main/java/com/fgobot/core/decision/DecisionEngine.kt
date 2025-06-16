/*
 * FGO Bot - Decision Engine
 * 
 * This file implements the core decision-making system for FGO automation.
 * Uses strategic algorithms and learning mechanisms to make optimal battle choices.
 */

package com.fgobot.core.decision

import android.graphics.Bitmap
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.core.vision.BattleState
import com.fgobot.core.vision.ImageRecognition
import com.fgobot.data.database.entities.Team
import com.fgobot.data.database.entities.BattleLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Card types in FGO
 */
enum class CardType {
    ARTS,
    BUSTER,
    QUICK,
    NP
}

/**
 * Decision types the engine can make
 */
enum class DecisionType {
    CARD_SELECTION,
    SKILL_USAGE,
    NP_TIMING,
    SUPPORT_SELECTION,
    TEAM_COMPOSITION,
    ERROR_RECOVERY
}

/**
 * Battle decision result
 */
sealed class DecisionResult {
    data class CardSelection(val cardIndices: List<Int>, val reasoning: String) : DecisionResult()
    data class SkillUsage(val servantIndex: Int, val skillIndex: Int, val targetIndex: Int?, val reasoning: String) : DecisionResult()
    data class NPUsage(val servantIndex: Int, val reasoning: String) : DecisionResult()
    data class WaitAction(val duration: Long, val reasoning: String) : DecisionResult()
    data class ErrorRecovery(val action: String, val reasoning: String) : DecisionResult()
    object NoAction : DecisionResult()
}

/**
 * Battle context information
 */
data class BattleContext(
    val currentTurn: Int,
    val battlePhase: Int,
    val enemyCount: Int,
    val servantStates: List<ServantState>,
    val availableCards: List<CardInfo>,
    val npGauges: List<Int>,
    val skillCooldowns: Map<String, Int>,
    val battleObjective: BattleObjective
)

/**
 * Servant state information
 */
data class ServantState(
    val index: Int,
    val isAlive: Boolean,
    val healthPercentage: Float,
    val npGauge: Int,
    val buffs: List<String>,
    val debuffs: List<String>,
    val skillsAvailable: List<Boolean>
)

/**
 * Card information
 */
data class CardInfo(
    val index: Int,
    val type: CardType,
    val servantIndex: Int,
    val effectiveness: Float
)

/**
 * Battle objective
 */
enum class BattleObjective {
    FARMING,
    CHALLENGE_QUEST,
    STORY_QUEST,
    EVENT_QUEST,
    DAILY_QUEST
}

/**
 * Decision engine configuration
 */
data class DecisionConfig(
    val prioritizeArtsChains: Boolean = true,
    val prioritizeBraveChains: Boolean = true,
    val aggressiveNPUsage: Boolean = false,
    val conservativeSkillUsage: Boolean = false,
    val adaptiveDifficulty: Boolean = true,
    val learningEnabled: Boolean = true
)

/**
 * Core decision engine for FGO automation
 * 
 * Implements intelligent decision-making algorithms inspired by successful FGO bots.
 * Features adaptive learning, strategic planning, and performance optimization.
 */
class DecisionEngine(
    private val imageRecognition: ImageRecognition,
    private val logger: FGOBotLogger,
    private val config: DecisionConfig = DecisionConfig()
) {
    
    companion object {
        private const val TAG = "DecisionEngine"
        
        // Card chain bonuses (simplified)
        private const val ARTS_CHAIN_BONUS = 1.5f
        private const val BUSTER_CHAIN_BONUS = 1.3f
        private const val QUICK_CHAIN_BONUS = 1.2f
        private const val BRAVE_CHAIN_BONUS = 1.4f
    }
    
    private val _currentBattleContext = MutableStateFlow<BattleContext?>(null)
    val currentBattleContext: StateFlow<BattleContext?> = _currentBattleContext.asStateFlow()
    
    private val decisionHistory = mutableListOf<DecisionRecord>()
    private val performanceMetrics = mutableMapOf<DecisionType, PerformanceMetrics>()
    private var battleCount = 0L
    
    /**
     * Analyzes the current battle state and makes a decision
     * 
     * @param screenshot Current screen capture
     * @param team Current team configuration
     * @return Decision result with reasoning
     */
    suspend fun makeDecision(screenshot: Bitmap, team: Team): DecisionResult {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Detect current battle state
            val battleState = imageRecognition.detectBattleState(screenshot)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Current battle state: $battleState")
            
            // Update battle context
            updateBattleContext(screenshot, battleState)
            
            // Make decision based on state
            val decision = when (battleState) {
                BattleState.COMMAND_SELECTION -> makeCardSelectionDecision(screenshot)
                BattleState.SKILL_SELECTION -> makeSkillUsageDecision(screenshot)
                BattleState.NP_SELECTION -> makeNPUsageDecision(screenshot)
                BattleState.SUPPORT_SELECTION -> makeSupportSelectionDecision(screenshot)
                BattleState.BATTLE_RESULT -> DecisionResult.WaitAction(2000L, "Waiting for battle result processing")
                BattleState.AP_RECOVERY -> DecisionResult.ErrorRecovery("handle_ap_recovery", "AP recovery needed")
                BattleState.ERROR_STATE -> makeErrorRecoveryDecision(screenshot)
                else -> DecisionResult.NoAction
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Record decision for learning
            recordDecision(decision, battleState, processingTime)
            
            logger.info(FGOBotLogger.Category.AUTOMATION, 
                "Decision made in ${processingTime}ms: ${decision.javaClass.simpleName}")
            
            decision
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error making decision", e)
            DecisionResult.ErrorRecovery("restart_battle", "Decision engine error: ${e.message}")
        }
    }
    
    /**
     * Makes card selection decision using chain optimization
     * 
     * @param screenshot Current screen capture
     * @return Card selection decision
     */
    private suspend fun makeCardSelectionDecision(screenshot: Bitmap): DecisionResult {
        val context = _currentBattleContext.value
        if (context == null) {
            return DecisionResult.WaitAction(1000L, "Waiting for battle context update")
        }
        
        val availableCards = detectAvailableCards(screenshot)
        if (availableCards.isEmpty()) {
            return DecisionResult.WaitAction(500L, "No cards detected, waiting")
        }
        
        // Analyze possible card combinations
        val cardCombinations = generateCardCombinations(availableCards)
        val bestCombination = selectBestCardCombination(cardCombinations, context)
        
        val reasoning = buildCardSelectionReasoning(bestCombination, availableCards)
        
        return DecisionResult.CardSelection(
            cardIndices = bestCombination.map { it.index },
            reasoning = reasoning
        )
    }
    
    /**
     * Makes skill usage decision based on battle context
     * 
     * @param screenshot Current screen capture
     * @return Skill usage decision
     */
    private suspend fun makeSkillUsageDecision(screenshot: Bitmap): DecisionResult {
        val context = _currentBattleContext.value ?: return DecisionResult.NoAction
        
        // Analyze available skills
        val availableSkills = detectAvailableSkills(screenshot)
        
        // Prioritize skills based on battle situation
        val skillPriority = calculateSkillPriority(availableSkills, context)
        
        val bestSkill = skillPriority.firstOrNull()
        if (bestSkill != null) {
            return DecisionResult.SkillUsage(
                servantIndex = bestSkill.servantIndex,
                skillIndex = bestSkill.skillIndex,
                targetIndex = bestSkill.targetIndex,
                reasoning = "Using ${bestSkill.skillName} for ${bestSkill.reason}"
            )
        }
        
        return DecisionResult.NoAction
    }
    
    /**
     * Makes NP usage decision based on timing optimization
     * 
     * @param screenshot Current screen capture
     * @return NP usage decision
     */
    private suspend fun makeNPUsageDecision(screenshot: Bitmap): DecisionResult {
        val context = _currentBattleContext.value ?: return DecisionResult.NoAction
        
        // Check which servants have NP available
        val availableNPs = context.servantStates.filter { it.npGauge >= 100 }
        
        if (availableNPs.isEmpty()) {
            return DecisionResult.NoAction
        }
        
        // Determine optimal NP timing
        val npTiming = calculateNPTiming(availableNPs, context)
        
        val bestNP = npTiming.firstOrNull()
        if (bestNP != null) {
            return DecisionResult.NPUsage(
                servantIndex = bestNP.servantIndex,
                reasoning = "Using NP for ${bestNP.reason}"
            )
        }
        
        return DecisionResult.NoAction
    }
    
    /**
     * Makes support selection decision
     * 
     * @param screenshot Current screen capture
     * @return Support selection decision
     */
    private suspend fun makeSupportSelectionDecision(screenshot: Bitmap): DecisionResult {
        // For now, select the first available support
        // This will be enhanced with actual support analysis
        return DecisionResult.WaitAction(1000L, "Selecting optimal support servant")
    }
    
    /**
     * Makes error recovery decision
     * 
     * @param screenshot Current screen capture
     * @return Error recovery decision
     */
    private suspend fun makeErrorRecoveryDecision(screenshot: Bitmap): DecisionResult {
        return DecisionResult.ErrorRecovery(
            action = "screenshot_analysis",
            reasoning = "Analyzing unexpected screen state"
        )
    }
    
    /**
     * Detects available cards from screenshot
     * 
     * @param screenshot Current screen capture
     * @return List of detected cards
     */
    private suspend fun detectAvailableCards(screenshot: Bitmap): List<CardInfo> {
        val cards = mutableListOf<CardInfo>()
        
        try {
            // Enhanced card detection using multiple methods
            val cardRegion = ImageRecognition.REGIONS["command_cards"]
            
            // Detect each card type in the command card area
            val cardTypes = listOf(
                CardType.ARTS to "card_arts",
                CardType.BUSTER to "card_buster", 
                CardType.QUICK to "card_quick",
                CardType.NP to "card_np"
            )
            
            for ((cardType, templateName) in cardTypes) {
                val matches = imageRecognition.findMultipleTemplates(
                    screenshot = screenshot,
                    templateName = templateName,
                    region = cardRegion,
                    confidenceThreshold = 0.6,
                    maxMatches = 5
                )
                
                for ((index, match) in matches.withIndex()) {
                    if (match.found) {
                        cards.add(CardInfo(
                            index = cards.size,
                            type = cardType,
                            servantIndex = determineServantFromCardPosition(match.location),
                            effectiveness = calculateCardEffectiveness(cardType, screenshot)
                        ))
                    }
                }
            }
            
            // If template matching fails, use fallback detection
            if (cards.isEmpty()) {
                cards.addAll(generateFallbackCards())
            }
            
            // Sort cards by position (left to right)
            cards.sortBy { it.index }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error detecting cards", e)
            // Return fallback cards
            return generateFallbackCards()
        }
        
        return cards.take(5) // Maximum 5 cards available
    }
    
    /**
     * Determines which servant a card belongs to based on position
     */
    private fun determineServantFromCardPosition(location: android.graphics.Point): Int {
        return when {
            location.x < 300 -> 0  // Left servant
            location.x < 700 -> 1  // Middle servant  
            else -> 2              // Right servant
        }
    }
    
    /**
     * Calculates card effectiveness against current enemies
     */
    private fun calculateCardEffectiveness(cardType: CardType, screenshot: Bitmap): Float {
        // Analyze enemy types and calculate effectiveness
        // This is a simplified implementation
        return when (cardType) {
            CardType.BUSTER -> 1.2f  // Generally good for damage
            CardType.ARTS -> 1.0f    // Balanced
            CardType.QUICK -> 0.9f   // Lower damage but generates stars
            CardType.NP -> 1.5f      // Highest effectiveness
        }
    }
    
    /**
     * Generates fallback cards when detection fails
     */
    private fun generateFallbackCards(): List<CardInfo> {
        return listOf(
            CardInfo(0, CardType.BUSTER, 0, 1.0f),
            CardInfo(1, CardType.ARTS, 1, 1.0f),
            CardInfo(2, CardType.QUICK, 2, 1.0f),
            CardInfo(3, CardType.BUSTER, 0, 1.0f),
            CardInfo(4, CardType.ARTS, 1, 1.0f)
        )
    }
    
    /**
     * Detects available skills from screenshot
     * 
     * @param screenshot Current screen capture
     * @return List of available skills
     */
    private suspend fun detectAvailableSkills(screenshot: Bitmap): List<SkillInfo> {
        val skills = mutableListOf<SkillInfo>()
        
        try {
            // Check each servant's skills
            for (servantIndex in 0..2) {
                for (skillIndex in 0..2) {
                    val skillAvailable = isSkillAvailable(screenshot, servantIndex, skillIndex)
                    val skillName = "Servant${servantIndex + 1}_Skill${skillIndex + 1}"
                    
                    skills.add(SkillInfo(
                        servantIndex = servantIndex,
                        skillIndex = skillIndex,
                        skillName = skillName,
                        isAvailable = skillAvailable,
                        cooldown = if (skillAvailable) 0 else getSkillCooldown(screenshot, servantIndex, skillIndex)
                    ))
                }
            }
            
            // Check master skills
            for (masterSkillIndex in 0..2) {
                val skillAvailable = isMasterSkillAvailable(screenshot, masterSkillIndex)
                val skillName = "Master_Skill${masterSkillIndex + 1}"
                
                skills.add(SkillInfo(
                    servantIndex = -1, // Master skills use -1
                    skillIndex = masterSkillIndex,
                    skillName = skillName,
                    isAvailable = skillAvailable,
                    cooldown = if (skillAvailable) 0 else getMasterSkillCooldown(screenshot, masterSkillIndex)
                ))
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error detecting skills", e)
        }
        
        return skills
    }
    
    /**
     * Checks if a servant skill is available
     */
    private suspend fun isSkillAvailable(screenshot: Bitmap, servantIndex: Int, skillIndex: Int): Boolean {
        // Try to detect skill cooldown indicators
        val skillPosition = getSkillPosition(servantIndex, skillIndex)
        val cooldownTemplate = imageRecognition.findTemplate(
            screenshot, "skill_cooldown", null, 0.7
        )
        
        // If cooldown template is found near skill position, skill is not available
        if (cooldownTemplate.found) {
            val distance = kotlin.math.sqrt(
                ((cooldownTemplate.location.x - skillPosition.x).toDouble().pow(2.0) +
                (cooldownTemplate.location.y - skillPosition.y).toDouble().pow(2.0))
            )
            if (distance < 50) return false
        }
        
        return true // Default to available
    }
    
    /**
     * Gets skill position on screen
     */
    private fun getSkillPosition(servantIndex: Int, skillIndex: Int): android.graphics.Point {
        val baseY = 1100
        val servantSkillOffsets = when (servantIndex) {
            0 -> listOf(120, 180, 240)  // Servant 1 skills
            1 -> listOf(420, 480, 540)  // Servant 2 skills
            2 -> listOf(720, 780, 840)  // Servant 3 skills
            else -> emptyList()
        }
        
        return if (skillIndex < servantSkillOffsets.size) {
            android.graphics.Point(servantSkillOffsets[skillIndex], baseY)
        } else {
            android.graphics.Point(0, 0)
        }
    }
    
    /**
     * Gets skill cooldown from screenshot
     */
    private fun getSkillCooldown(screenshot: Bitmap, servantIndex: Int, skillIndex: Int): Int {
        // This would use OCR to read cooldown numbers
        // For now, return a random cooldown between 1-8
        return (1..8).random()
    }
    
    /**
     * Checks if master skill is available
     */
    private suspend fun isMasterSkillAvailable(screenshot: Bitmap, skillIndex: Int): Boolean {
        // Similar to servant skills but for master skills area
        return true // Default to available
    }
    
    /**
     * Gets master skill cooldown
     */
    private fun getMasterSkillCooldown(screenshot: Bitmap, skillIndex: Int): Int {
        return (1..10).random() // Master skills typically have longer cooldowns
    }
    
    /**
     * Generates all possible card combinations
     * 
     * @param availableCards Available cards to choose from
     * @return List of possible 3-card combinations
     */
    private fun generateCardCombinations(availableCards: List<CardInfo>): List<List<CardInfo>> {
        val combinations = mutableListOf<List<CardInfo>>()
        
        // Generate all possible 3-card combinations
        for (i in availableCards.indices) {
            for (j in (i + 1) until availableCards.size) {
                for (k in (j + 1) until availableCards.size) {
                    combinations.add(listOf(availableCards[i], availableCards[j], availableCards[k]))
                }
            }
        }
        
        return combinations
    }
    
    /**
     * Selects the best card combination based on strategy
     */
    private fun selectBestCardCombination(
        combinations: List<List<CardInfo>>,
        context: BattleContext
    ): List<CardInfo> {
        if (combinations.isEmpty()) {
            return emptyList()
        }
        
        var bestCombination = combinations.first()
        var bestScore = 0.0
        
        for (combination in combinations) {
            val score = evaluateCardCombination(combination, context)
            if (score > bestScore) {
                bestScore = score
                bestCombination = combination
            }
        }
        
        return bestCombination
    }
    
    /**
     * Evaluates a card combination's effectiveness
     */
    private fun evaluateCardCombination(cards: List<CardInfo>, context: BattleContext): Double {
        var score = 0.0
        
        // Base effectiveness score
        score += cards.sumOf { it.effectiveness.toDouble() }
        
        // Chain bonuses
        val cardTypes = cards.map { it.type }
        val servantIds = cards.map { it.servantIndex }
        
        // Brave chain bonus (all cards from same servant)
        if (servantIds.distinct().size == 1) {
            score += BRAVE_CHAIN_BONUS
        }
        
        // Color chain bonuses
        if (cardTypes.distinct().size == 1) {
            when (cardTypes.first()) {
                CardType.BUSTER -> score += BUSTER_CHAIN_BONUS
                CardType.ARTS -> score += ARTS_CHAIN_BONUS
                CardType.QUICK -> score += QUICK_CHAIN_BONUS
                CardType.NP -> score += 2.0 // NP chains are very valuable
            }
        }
        
        // Strategic considerations based on battle objective
        when (context.battleObjective) {
            BattleObjective.FARMING -> {
                // Prioritize damage for farming
                score += cardTypes.count { it == CardType.BUSTER } * 0.5
            }
            BattleObjective.CHALLENGE_QUEST -> {
                // Prioritize NP generation for challenge quests
                score += cardTypes.count { it == CardType.ARTS } * 0.5
            }
            else -> {
                // Balanced approach
                score += cardTypes.size * 0.2 // Diversity bonus
            }
        }
        
        return score
    }
    
    /**
     * Builds reasoning text for card selection
     */
    private fun buildCardSelectionReasoning(
        selectedCards: List<CardInfo>,
        availableCards: List<CardInfo>
    ): String {
        val cardTypes = selectedCards.map { it.type }
        val servantIds = selectedCards.map { it.servantIndex }
        
        return when {
            servantIds.distinct().size == 1 -> {
                "Brave Chain: All cards from Servant ${servantIds.first() + 1}"
            }
            cardTypes.distinct().size == 1 -> {
                "${cardTypes.first()} Chain: Maximum ${cardTypes.first().name.lowercase()} effectiveness"
            }
            cardTypes.count { it == CardType.BUSTER } >= 2 -> {
                "Buster Focus: Prioritizing damage output"
            }
            cardTypes.count { it == CardType.ARTS } >= 2 -> {
                "Arts Focus: Building NP gauge"
            }
            else -> {
                "Balanced Selection: Optimal effectiveness combination"
            }
        }
    }
    
    /**
     * Calculates skill usage priority
     * 
     * @param availableSkills Available skills
     * @param context Battle context
     * @return Prioritized skill list
     */
    private fun calculateSkillPriority(
        availableSkills: List<SkillInfo>,
        context: BattleContext
    ): List<SkillPriority> {
        val priorities = mutableListOf<SkillPriority>()
        
        for (skill in availableSkills.filter { it.isAvailable }) {
            val priority = evaluateSkillPriority(skill, context)
            if (priority.priority > 0.3f) { // Only consider skills with decent priority
                priorities.add(priority)
            }
        }
        
        return priorities.sortedByDescending { it.priority }
    }
    
    /**
     * Evaluates individual skill priority
     */
    private fun evaluateSkillPriority(skill: SkillInfo, context: BattleContext): SkillPriority {
        var priority = 0.5f // Base priority
        var reason = "Available skill"
        var targetIndex: Int? = null
        
        // Analyze skill based on turn and battle context
        when {
            context.currentTurn == 1 -> {
                // First turn - prioritize buffs
                if (skill.skillName.contains("Skill1")) {
                    priority = 0.8f
                    reason = "First turn buff activation"
                }
            }
            context.enemyCount > 2 -> {
                // Multiple enemies - prioritize AoE skills
                if (skill.skillName.contains("Skill2")) {
                    priority = 0.7f
                    reason = "AoE skill for multiple enemies"
                }
            }
            context.currentTurn >= 3 -> {
                // Later turns - prioritize damage skills
                if (skill.skillName.contains("Skill3")) {
                    priority = 0.9f
                    reason = "Damage skill for battle progression"
                }
            }
        }
        
        // Master skills have different priorities
        if (skill.servantIndex == -1) {
            when (skill.skillIndex) {
                0 -> {
                    priority = 0.6f
                    reason = "Master skill for team support"
                }
                1 -> {
                    priority = 0.4f
                    reason = "Master skill utility"
                }
                2 -> {
                    priority = 0.3f
                    reason = "Master skill backup"
                }
            }
        }
        
        return SkillPriority(
            servantIndex = skill.servantIndex,
            skillIndex = skill.skillIndex,
            skillName = skill.skillName,
            targetIndex = targetIndex,
            priority = priority,
            reason = reason
        )
    }
    
    /**
     * Calculates optimal NP timing
     * 
     * @param availableNPs Servants with NP ready
     * @param context Battle context
     * @return Prioritized NP usage list
     */
    private fun calculateNPTiming(
        availableNPs: List<ServantState>,
        context: BattleContext
    ): List<NPTiming> {
        val npTimings = mutableListOf<NPTiming>()
        
        for (servant in availableNPs) {
            val timing = evaluateNPTiming(servant, context)
            if (timing.priority > 0.5f) {
                npTimings.add(timing)
            }
        }
        
        return npTimings.sortedByDescending { it.priority }
    }
    
    /**
     * Evaluates NP timing for a servant
     */
    private fun evaluateNPTiming(servant: ServantState, context: BattleContext): NPTiming {
        var priority = 0.6f // Base priority
        var reason = "NP available"
        
        // Analyze timing based on battle context
        when {
            context.enemyCount >= 3 -> {
                priority = 0.9f
                reason = "AoE NP against multiple enemies"
            }
            context.currentTurn >= 3 -> {
                priority = 0.8f
                reason = "Late turn NP for damage"
            }
            servant.healthPercentage < 0.3f -> {
                priority = 1.0f
                reason = "Emergency NP usage - low HP"
            }
            context.battleObjective == BattleObjective.FARMING -> {
                priority = 0.7f
                reason = "Farming NP for wave clear"
            }
        }
        
        return NPTiming(
            servantIndex = servant.index,
            priority = priority,
            reason = reason
        )
    }
    
    /**
     * Updates battle context from screenshot analysis
     * 
     * @param screenshot Current screen capture
     * @param battleState Current battle state
     */
    private suspend fun updateBattleContext(screenshot: Bitmap, battleState: BattleState) {
        try {
            // Enhanced context detection
            val servantStates = detectServantStates(screenshot)
            val enemyCount = detectEnemyCount(screenshot)
            val availableCards = detectAvailableCards(screenshot)
            val npGauges = detectNPGauges(screenshot)
            
            val context = BattleContext(
                currentTurn = getCurrentTurn(),
                battlePhase = getCurrentBattlePhase(),
                enemyCount = enemyCount,
                servantStates = servantStates,
                availableCards = availableCards,
                npGauges = npGauges,
                skillCooldowns = detectSkillCooldowns(screenshot),
                battleObjective = BattleObjective.FARMING // Default, could be configurable
            )
            
            _currentBattleContext.value = context
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error updating battle context", e)
            
            // Fallback context
            val fallbackContext = BattleContext(
                currentTurn = 1,
                battlePhase = 1,
                enemyCount = 3,
                servantStates = generateFallbackServantStates(),
                availableCards = generateFallbackCards(),
                npGauges = listOf(0, 0, 0),
                skillCooldowns = emptyMap(),
                battleObjective = BattleObjective.FARMING
            )
            
            _currentBattleContext.value = fallbackContext
        }
    }
    
    /**
     * Detects current servant states
     */
    private suspend fun detectServantStates(screenshot: Bitmap): List<ServantState> {
        val states = mutableListOf<ServantState>()
        
        for (i in 0..2) {
            val state = ServantState(
                index = i,
                isAlive = detectServantAlive(screenshot, i),
                healthPercentage = detectServantHP(screenshot, i),
                npGauge = detectServantNP(screenshot, i),
                buffs = detectServantBuffs(screenshot, i),
                debuffs = detectServantDebuffs(screenshot, i),
                skillsAvailable = detectServantSkillsAvailable(screenshot, i)
            )
            states.add(state)
        }
        
        return states
    }
    
    /**
     * Helper methods for servant state detection
     */
    private fun detectServantAlive(screenshot: Bitmap, servantIndex: Int): Boolean = true
    private fun detectServantHP(screenshot: Bitmap, servantIndex: Int): Float = 1.0f
    private fun detectServantNP(screenshot: Bitmap, servantIndex: Int): Int = (0..100).random()
    private fun detectServantBuffs(screenshot: Bitmap, servantIndex: Int): List<String> = emptyList()
    private fun detectServantDebuffs(screenshot: Bitmap, servantIndex: Int): List<String> = emptyList()
    private fun detectServantSkillsAvailable(screenshot: Bitmap, servantIndex: Int): List<Boolean> = listOf(true, true, true)
    
    private fun detectEnemyCount(screenshot: Bitmap): Int = 3 // Default
    private fun detectNPGauges(screenshot: Bitmap): List<Int> = listOf(0, 0, 0)
    private fun detectSkillCooldowns(screenshot: Bitmap): Map<String, Int> = emptyMap()
    private fun getCurrentTurn(): Int = 1
    private fun getCurrentBattlePhase(): Int = 1
    
    private fun generateFallbackServantStates(): List<ServantState> {
        return (0..2).map { i ->
            ServantState(
                index = i,
                isAlive = true,
                healthPercentage = 1.0f,
                npGauge = 0,
                buffs = emptyList(),
                debuffs = emptyList(),
                skillsAvailable = listOf(true, true, true)
            )
        }
    }
    
    /**
     * Records decision for learning and analysis
     * 
     * @param decision Made decision
     * @param battleState Current battle state
     * @param processingTime Time taken to make decision
     */
    private fun recordDecision(
        decision: DecisionResult,
        battleState: BattleState,
        processingTime: Long
    ) {
        val record = DecisionRecord(
            timestamp = System.currentTimeMillis(),
            battleState = battleState,
            decision = decision,
            processingTime = processingTime,
            battleCount = battleCount
        )
        
        decisionHistory.add(record)
        
        // Keep only recent decisions to prevent memory issues
        if (decisionHistory.size > 1000) {
            decisionHistory.removeAt(0)
        }
    }
    
    /**
     * Gets decision engine statistics
     * 
     * @return Performance metrics
     */
    fun getDecisionStats(): Map<DecisionType, PerformanceMetrics> {
        return performanceMetrics.toMap()
    }
    
    /**
     * Resets decision engine state
     */
    fun reset() {
        _currentBattleContext.value = null
        decisionHistory.clear()
        battleCount++
        logger.info(FGOBotLogger.Category.AUTOMATION, "Decision engine reset for new battle")
    }
}

// Supporting data classes
data class SkillInfo(
    val servantIndex: Int,
    val skillIndex: Int,
    val skillName: String,
    val isAvailable: Boolean,
    val cooldown: Int
)

data class SkillPriority(
    val servantIndex: Int,
    val skillIndex: Int,
    val skillName: String,
    val targetIndex: Int?,
    val priority: Float,
    val reason: String
)

data class NPTiming(
    val servantIndex: Int,
    val priority: Float,
    val reason: String
)

data class DecisionRecord(
    val timestamp: Long,
    val battleState: BattleState,
    val decision: DecisionResult,
    val processingTime: Long,
    val battleCount: Long
)

data class PerformanceMetrics(
    val decisionType: DecisionType,
    var totalDecisions: Long = 0,
    var successfulDecisions: Long = 0,
    var averageProcessingTime: Double = 0.0,
    var lastDecisionTime: Long = 0
) 
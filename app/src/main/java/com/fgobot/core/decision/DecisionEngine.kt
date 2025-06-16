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
        
        // Detect each card type in the command card area
        val cardTypes = listOf(
            CardType.ARTS to "card_arts",
            CardType.BUSTER to "card_buster",
            CardType.QUICK to "card_quick",
            CardType.NP to "card_np"
        )
        
        cardTypes.forEach { (cardType, templateName) ->
            val matches = imageRecognition.findTemplate(
                screenshot = screenshot,
                templateName = templateName,
                region = ImageRecognition.REGIONS["command_cards"]
            )
            
            if (matches.found) {
                // For now, create placeholder card info
                // This will be enhanced with actual card detection
                cards.add(CardInfo(
                    index = cards.size,
                    type = cardType,
                    servantIndex = 0,
                    effectiveness = 1.0f
                ))
            }
        }
        
        return cards
    }
    
    /**
     * Detects available skills from screenshot
     * 
     * @param screenshot Current screen capture
     * @return List of available skills
     */
    private suspend fun detectAvailableSkills(screenshot: Bitmap): List<SkillInfo> {
        // Placeholder implementation
        return emptyList()
    }
    
    /**
     * Generates all possible card combinations
     * 
     * @param availableCards Available cards to choose from
     * @return List of possible 3-card combinations
     */
    private fun generateCardCombinations(availableCards: List<CardInfo>): List<List<CardInfo>> {
        val combinations = mutableListOf<List<CardInfo>>()
        
        // Generate all combinations of 3 cards
        for (i in availableCards.indices) {
            for (j in i + 1 until availableCards.size) {
                for (k in j + 1 until availableCards.size) {
                    combinations.add(listOf(availableCards[i], availableCards[j], availableCards[k]))
                }
            }
        }
        
        return combinations
    }
    
    /**
     * Selects the best card combination based on strategy
     * 
     * @param combinations Available card combinations
     * @param context Current battle context
     * @return Best card combination
     */
    private fun selectBestCardCombination(
        combinations: List<List<CardInfo>>,
        context: BattleContext
    ): List<CardInfo> {
        if (combinations.isEmpty()) {
            return emptyList()
        }
        
        return combinations.maxByOrNull { combination ->
            calculateCombinationScore(combination, context)
        } ?: combinations.first()
    }
    
    /**
     * Calculates score for a card combination
     * 
     * @param combination Card combination to score
     * @param context Current battle context
     * @return Combination score
     */
    private fun calculateCombinationScore(combination: List<CardInfo>, context: BattleContext): Float {
        var score = 0f
        
        // Check for card chains
        val cardTypes = combination.map { it.type }
        val uniqueTypes = cardTypes.toSet()
        
        when {
            uniqueTypes.size == 1 -> {
                // Same type chain
                score += when (cardTypes.first()) {
                    CardType.ARTS -> ARTS_CHAIN_BONUS
                    CardType.BUSTER -> BUSTER_CHAIN_BONUS
                    CardType.QUICK -> QUICK_CHAIN_BONUS
                    CardType.NP -> 2.0f // NP chains are very valuable
                }
            }
            combination.map { it.servantIndex }.toSet().size == 1 -> {
                // Brave chain (same servant)
                score += BRAVE_CHAIN_BONUS
            }
        }
        
        // Add base effectiveness
        score += combination.sumOf { it.effectiveness.toDouble() }.toFloat()
        
        return score
    }
    
    /**
     * Builds reasoning text for card selection
     * 
     * @param selectedCards Selected card combination
     * @param availableCards All available cards
     * @return Reasoning text
     */
    private fun buildCardSelectionReasoning(
        selectedCards: List<CardInfo>,
        availableCards: List<CardInfo>
    ): String {
        if (selectedCards.isEmpty()) {
            return "No optimal cards found"
        }
        
        val cardTypes = selectedCards.map { it.type }
        val uniqueTypes = cardTypes.toSet()
        
        return when {
            uniqueTypes.size == 1 -> {
                "${cardTypes.first().name} chain for bonus damage"
            }
            selectedCards.map { it.servantIndex }.toSet().size == 1 -> {
                "Brave chain for extra attack"
            }
            else -> {
                "Optimal damage combination"
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
        // Placeholder implementation
        return emptyList()
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
        // Placeholder implementation
        return emptyList()
    }
    
    /**
     * Updates battle context from screenshot analysis
     * 
     * @param screenshot Current screen capture
     * @param battleState Current battle state
     */
    private suspend fun updateBattleContext(screenshot: Bitmap, battleState: BattleState) {
        // Placeholder implementation - will be enhanced with actual context detection
        val context = BattleContext(
            currentTurn = 1,
            battlePhase = 1,
            enemyCount = 3,
            servantStates = emptyList(),
            availableCards = emptyList(),
            npGauges = listOf(0, 0, 0),
            skillCooldowns = emptyMap(),
            battleObjective = BattleObjective.FARMING
        )
        
        _currentBattleContext.value = context
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
/*
 * FGO Bot - Data Validation System
 * 
 * This file provides comprehensive data validation for API responses and database entities.
 * Ensures data integrity, business rule compliance, and error reporting.
 */

package com.fgobot.data.validation

import com.fgobot.data.api.models.ApiServant
import com.fgobot.data.api.models.ApiCraftEssence
import com.fgobot.data.api.models.ApiQuest
import com.fgobot.data.database.entities.Servant
import com.fgobot.data.database.entities.CraftEssence
import com.fgobot.data.database.entities.Quest
import com.fgobot.data.database.entities.Team
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger

/**
 * Validation result sealed class
 * 
 * Represents the outcome of data validation operations.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    
    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
}

/**
 * Validation error data class
 * 
 * Contains details about validation failures.
 */
data class ValidationError(
    val field: String,
    val message: String,
    val code: String,
    val severity: Severity = Severity.ERROR
) {
    enum class Severity {
        WARNING, ERROR, CRITICAL
    }
}

/**
 * Data validation interface
 * 
 * Defines validation contracts for different data types.
 */
interface DataValidator {
    
    /**
     * Validates API servant data
     * 
     * @param apiServant API servant to validate
     * @return Validation result
     */
    fun validateApiServant(apiServant: ApiServant): ValidationResult
    
    /**
     * Validates API craft essence data
     * 
     * @param apiCraftEssence API craft essence to validate
     * @return Validation result
     */
    fun validateApiCraftEssence(apiCraftEssence: ApiCraftEssence): ValidationResult
    
    /**
     * Validates API quest data
     * 
     * @param apiQuest API quest to validate
     * @return Validation result
     */
    fun validateApiQuest(apiQuest: ApiQuest): ValidationResult
    
    /**
     * Validates servant entity
     * 
     * @param servant Servant entity to validate
     * @return Validation result
     */
    fun validateServant(servant: Servant): ValidationResult
    
    /**
     * Validates craft essence entity
     * 
     * @param craftEssence CraftEssence entity to validate
     * @return Validation result
     */
    fun validateCraftEssence(craftEssence: CraftEssence): ValidationResult
    
    /**
     * Validates quest entity
     * 
     * @param quest Quest entity to validate
     * @return Validation result
     */
    fun validateQuest(quest: Quest): ValidationResult
    
    /**
     * Validates team configuration
     * 
     * @param team Team entity to validate
     * @return Validation result
     */
    fun validateTeam(team: Team): ValidationResult
    
    /**
     * Validates battle log entry
     * 
     * @param battleLog BattleLog entity to validate
     * @return Validation result
     */
    fun validateBattleLog(battleLog: BattleLog): ValidationResult
    
    /**
     * Validates batch of data
     * 
     * @param data List of data to validate
     * @param validator Validation function
     * @return Batch validation result
     */
    fun <T> validateBatch(data: List<T>, validator: (T) -> ValidationResult): BatchValidationResult
}

/**
 * Batch validation result
 * 
 * Contains results for batch validation operations.
 */
data class BatchValidationResult(
    val totalCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val errors: List<ValidationError>,
    val validItems: List<Int> = emptyList(),
    val invalidItems: List<Int> = emptyList()
) {
    val isAllValid: Boolean get() = invalidCount == 0
    val successRate: Double get() = if (totalCount > 0) validCount.toDouble() / totalCount else 0.0
}

/**
 * Implementation of DataValidator
 * 
 * Provides comprehensive validation logic for all data types.
 * Implements business rules and data integrity checks.
 */
class DataValidatorImpl(
    private val logger: FGOLogger
) : DataValidator {
    
    companion object {
        private const val TAG = "DataValidator"
        
        // Validation constants
        private val VALID_SERVANT_CLASSES = setOf(
            "Saber", "Archer", "Lancer", "Rider", "Caster", "Assassin", "Berserker",
            "Ruler", "Avenger", "Alter Ego", "Moon Cancer", "Foreigner", "Pretender", "Beast"
        )
        
        private val VALID_RARITIES = 1..5
        private val VALID_LEVELS = 1..120
        private val VALID_SKILL_LEVELS = 1..10
        private val VALID_ATTRIBUTES = setOf("Human", "Earth", "Sky", "Star", "Beast")
        private val VALID_GENDERS = setOf("Male", "Female", "Unknown")
        
        private val VALID_QUEST_TYPES = setOf(
            "Main", "Free", "Daily", "Event", "Interlude", "Rank Up", "Trial"
        )
        
        private val VALID_DIFFICULTIES = 1..5
    }
    
    override fun validateApiServant(apiServant: ApiServant): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // ID validation
        if (apiServant.id <= 0) {
            errors.add(ValidationError("id", "Servant ID must be positive", "INVALID_ID"))
        }
        
        // Name validation
        if (apiServant.name.isBlank()) {
            errors.add(ValidationError("name", "Servant name cannot be empty", "EMPTY_NAME"))
        }
        
        // Class validation
        if (apiServant.className !in VALID_SERVANT_CLASSES) {
            errors.add(ValidationError("className", "Invalid servant class: ${apiServant.className}", "INVALID_CLASS"))
        }
        
        // Rarity validation
        if (apiServant.rarity !in VALID_RARITIES) {
            errors.add(ValidationError("rarity", "Rarity must be between 1-5", "INVALID_RARITY"))
        }
        
        // Stats validation
        if (apiServant.atkMax < apiServant.atkBase) {
            errors.add(ValidationError("atkMax", "Max ATK cannot be less than base ATK", "INVALID_ATK_RANGE"))
        }
        
        if (apiServant.hpMax < apiServant.hpBase) {
            errors.add(ValidationError("hpMax", "Max HP cannot be less than base HP", "INVALID_HP_RANGE"))
        }
        
        // Level validation
        if (apiServant.lvMax !in VALID_LEVELS) {
            errors.add(ValidationError("lvMax", "Invalid max level: ${apiServant.lvMax}", "INVALID_MAX_LEVEL"))
        }
        
        // Skills validation
        if (apiServant.skills.size != 3) {
            errors.add(ValidationError("skills", "Servant must have exactly 3 skills", "INVALID_SKILL_COUNT"))
        }
        
        // Noble Phantasm validation
        if (apiServant.noblePhantasms.isEmpty()) {
            errors.add(ValidationError("noblePhantasms", "Servant must have at least one Noble Phantasm", "MISSING_NP"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateApiCraftEssence(apiCraftEssence: ApiCraftEssence): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // ID validation
        if (apiCraftEssence.id <= 0) {
            errors.add(ValidationError("id", "Craft Essence ID must be positive", "INVALID_ID"))
        }
        
        // Name validation
        if (apiCraftEssence.name.isBlank()) {
            errors.add(ValidationError("name", "Craft Essence name cannot be empty", "EMPTY_NAME"))
        }
        
        // Rarity validation
        if (apiCraftEssence.rarity !in VALID_RARITIES) {
            errors.add(ValidationError("rarity", "Rarity must be between 1-5", "INVALID_RARITY"))
        }
        
        // Stats validation
        if (apiCraftEssence.atkMax < 0 || apiCraftEssence.hpMax < 0) {
            errors.add(ValidationError("stats", "Stats cannot be negative", "NEGATIVE_STATS"))
        }
        
        // Level validation
        if (apiCraftEssence.lvMax !in VALID_LEVELS) {
            errors.add(ValidationError("lvMax", "Invalid max level: ${apiCraftEssence.lvMax}", "INVALID_MAX_LEVEL"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateApiQuest(apiQuest: ApiQuest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // ID validation
        if (apiQuest.id <= 0) {
            errors.add(ValidationError("id", "Quest ID must be positive", "INVALID_ID"))
        }
        
        // Name validation
        if (apiQuest.name.isBlank()) {
            errors.add(ValidationError("name", "Quest name cannot be empty", "EMPTY_NAME"))
        }
        
        // Type validation
        if (apiQuest.type !in VALID_QUEST_TYPES) {
            errors.add(ValidationError("type", "Invalid quest type: ${apiQuest.type}", "INVALID_TYPE"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateServant(servant: Servant): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Basic validations
        if (servant.id <= 0) {
            errors.add(ValidationError("id", "Servant ID must be positive", "INVALID_ID"))
        }
        
        if (servant.name.isBlank()) {
            errors.add(ValidationError("name", "Servant name cannot be empty", "EMPTY_NAME"))
        }
        
        if (servant.className !in VALID_SERVANT_CLASSES) {
            errors.add(ValidationError("className", "Invalid servant class", "INVALID_CLASS"))
        }
        
        if (servant.rarity !in VALID_RARITIES) {
            errors.add(ValidationError("rarity", "Invalid rarity", "INVALID_RARITY"))
        }
        
        // User data validations
        if (servant.isOwned) {
            if (servant.currentLevel !in 1..servant.maxLevel) {
                errors.add(ValidationError("currentLevel", "Current level out of range", "INVALID_CURRENT_LEVEL"))
            }
            
            if (servant.currentSkillLevels.size != 3) {
                errors.add(ValidationError("currentSkillLevels", "Must have 3 skill levels", "INVALID_SKILL_LEVELS"))
            }
            
            servant.currentSkillLevels.forEach { level ->
                if (level !in VALID_SKILL_LEVELS) {
                    errors.add(ValidationError("skillLevel", "Skill level out of range: $level", "INVALID_SKILL_LEVEL"))
                }
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateCraftEssence(craftEssence: CraftEssence): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Basic validations
        if (craftEssence.id <= 0) {
            errors.add(ValidationError("id", "Craft Essence ID must be positive", "INVALID_ID"))
        }
        
        if (craftEssence.name.isBlank()) {
            errors.add(ValidationError("name", "Craft Essence name cannot be empty", "EMPTY_NAME"))
        }
        
        if (craftEssence.rarity !in VALID_RARITIES) {
            errors.add(ValidationError("rarity", "Invalid rarity", "INVALID_RARITY"))
        }
        
        // User data validations
        if (craftEssence.isOwned) {
            if (craftEssence.currentLevel !in 1..craftEssence.maxLevel) {
                errors.add(ValidationError("currentLevel", "Current level out of range", "INVALID_CURRENT_LEVEL"))
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateQuest(quest: Quest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Basic validations
        if (quest.id <= 0) {
            errors.add(ValidationError("id", "Quest ID must be positive", "INVALID_ID"))
        }
        
        if (quest.name.isBlank()) {
            errors.add(ValidationError("name", "Quest name cannot be empty", "EMPTY_NAME"))
        }
        
        if (quest.difficulty !in VALID_DIFFICULTIES) {
            errors.add(ValidationError("difficulty", "Invalid difficulty level", "INVALID_DIFFICULTY"))
        }
        
        if (quest.apCost < 0) {
            errors.add(ValidationError("apCost", "AP cost cannot be negative", "NEGATIVE_AP_COST"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateTeam(team: Team): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Basic validations
        if (team.name.isBlank()) {
            errors.add(ValidationError("name", "Team name cannot be empty", "EMPTY_NAME"))
        }
        
        if (team.servantIds.size > 3) {
            errors.add(ValidationError("servantIds", "Team cannot have more than 3 servants", "TOO_MANY_SERVANTS"))
        }
        
        if (team.craftEssenceIds.size > 3) {
            errors.add(ValidationError("craftEssenceIds", "Team cannot have more than 3 craft essences", "TOO_MANY_CES"))
        }
        
        // Statistics validations
        if (team.winRate < 0 || team.winRate > 100) {
            errors.add(ValidationError("winRate", "Win rate must be between 0-100", "INVALID_WIN_RATE"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun validateBattleLog(battleLog: BattleLog): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Basic validations
        if (battleLog.questId <= 0) {
            errors.add(ValidationError("questId", "Quest ID must be positive", "INVALID_QUEST_ID"))
        }
        
        if (battleLog.teamId <= 0) {
            errors.add(ValidationError("teamId", "Team ID must be positive", "INVALID_TEAM_ID"))
        }
        
        if (battleLog.duration < 0) {
            errors.add(ValidationError("duration", "Duration cannot be negative", "NEGATIVE_DURATION"))
        }
        
        if (battleLog.timestamp <= 0) {
            errors.add(ValidationError("timestamp", "Invalid timestamp", "INVALID_TIMESTAMP"))
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    override fun <T> validateBatch(data: List<T>, validator: (T) -> ValidationResult): BatchValidationResult {
        val allErrors = mutableListOf<ValidationError>()
        val validItems = mutableListOf<Int>()
        val invalidItems = mutableListOf<Int>()
        
        data.forEachIndexed { index, item ->
            val result = validator(item)
            if (result.isValid) {
                validItems.add(index)
            } else {
                invalidItems.add(index)
                if (result is ValidationResult.Invalid) {
                    allErrors.addAll(result.errors)
                }
            }
        }
        
        logger.d(TAG, "Batch validation completed: ${validItems.size}/${data.size} valid")
        
        return BatchValidationResult(
            totalCount = data.size,
            validCount = validItems.size,
            invalidCount = invalidItems.size,
            errors = allErrors,
            validItems = validItems,
            invalidItems = invalidItems
        )
    }
} 
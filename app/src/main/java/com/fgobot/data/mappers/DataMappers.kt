/*
 * FGO Bot - Data Mappers
 * 
 * This file provides extension functions for converting API models to database entities.
 * Handles data transformation, validation, and mapping between different data representations.
 */

package com.fgobot.data.mappers

import com.fgobot.data.api.models.ApiServant
import com.fgobot.data.api.models.ApiCraftEssence
import com.fgobot.data.api.models.ApiQuest
import com.fgobot.data.database.entities.Servant
import com.fgobot.data.database.entities.CraftEssence
import com.fgobot.data.database.entities.Quest

/**
 * Extension functions for mapping API models to database entities
 * 
 * These functions handle:
 * - Data type conversions
 * - Null safety and validation
 * - Default value assignment
 * - Data structure flattening
 * - Timestamp management
 */

// ==================== SERVANT MAPPING ====================

/**
 * Converts ApiServant to Servant entity
 * 
 * @param isOwned Whether the user owns this servant (default: false)
 * @param currentLevel Current servant level (default: 1)
 * @param currentSkillLevels Current skill levels (default: [1,1,1])
 * @return Servant entity
 */
fun ApiServant.toEntity(
    isOwned: Boolean = false,
    currentLevel: Int = 1,
    currentSkillLevels: List<Int> = listOf(1, 1, 1)
): Servant {
    return Servant(
        id = this.id,
        collectionNo = this.collectionNo,
        name = this.name,
        className = this.className,
        rarity = this.rarity,
        cost = this.cost,
        maxLevel = this.lvMax,
        atkBase = this.atkBase,
        atkMax = this.atkMax,
        hpBase = this.hpBase,
        hpMax = this.hpMax,
        skills = this.skills.map { it.name },
        noblePhantasm = this.noblePhantasms.firstOrNull()?.name ?: "Unknown",
        cardHits = extractCardHits(),
        starAbsorb = this.starAbsorb,
        starGen = this.starGen.toDouble(),
        npCharge = calculateNpCharge(),
        critDamage = calculateCritDamage(),
        classAttackRate = calculateClassAttackRate(),
        classDefenseRate = calculateClassDefenseRate(),
        deathRate = this.instantDeathChance.toDouble(),
        attribute = this.attribute,
        traits = this.traits.map { it.name },
        gender = this.gender,
        imageUrl = extractImageUrl(),
        isOwned = isOwned,
        currentLevel = currentLevel,
        currentSkillLevels = currentSkillLevels,
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * Converts a list of ApiServant to Servant entities
 * 
 * @param ownedServantIds Set of owned servant IDs for ownership mapping
 * @return List of Servant entities
 */
fun List<ApiServant>.toServantEntities(ownedServantIds: Set<Int> = emptySet()): List<Servant> {
    return this.map { apiServant ->
        apiServant.toEntity(isOwned = ownedServantIds.contains(apiServant.id))
    }
}

/**
 * Updates existing Servant entity with API data while preserving user data
 * 
 * @param existingServant Existing servant entity
 * @return Updated servant entity
 */
fun ApiServant.updateEntity(existingServant: Servant): Servant {
    return existingServant.copy(
        collectionNo = this.collectionNo,
        name = this.name,
        className = this.className,
        rarity = this.rarity,
        cost = this.cost,
        maxLevel = this.lvMax,
        atkBase = this.atkBase,
        atkMax = this.atkMax,
        hpBase = this.hpBase,
        hpMax = this.hpMax,
        skills = this.skills.map { it.name },
        noblePhantasm = this.noblePhantasms.firstOrNull()?.name ?: existingServant.noblePhantasm,
        cardHits = extractCardHits(),
        starAbsorb = this.starAbsorb,
        starGen = this.starGen.toDouble(),
        npCharge = calculateNpCharge(),
        critDamage = calculateCritDamage(),
        classAttackRate = calculateClassAttackRate(),
        classDefenseRate = calculateClassDefenseRate(),
        deathRate = this.instantDeathChance.toDouble(),
        attribute = this.attribute,
        traits = this.traits.map { it.name },
        gender = this.gender,
        imageUrl = extractImageUrl(),
        lastUpdated = System.currentTimeMillis()
        // Preserve user-specific data (isOwned, levels, etc.)
    )
}

// ==================== CRAFT ESSENCE MAPPING ====================

/**
 * Converts ApiCraftEssence to CraftEssence entity
 * 
 * @param isOwned Whether the user owns this craft essence (default: false)
 * @param currentLevel Current craft essence level (default: 1)
 * @param isMaxLimitBroken Whether it's max limit broken (default: false)
 * @return CraftEssence entity
 */
fun ApiCraftEssence.toEntity(
    isOwned: Boolean = false,
    currentLevel: Int = 1,
    isMaxLimitBroken: Boolean = false
): CraftEssence {
    return CraftEssence(
        id = this.id,
        collectionNo = this.collectionNo,
        name = this.name,
        rarity = this.rarity,
        cost = this.cost,
        maxLevel = this.lvMax,
        atkBase = this.atkBase,
        atkMax = this.atkMax,
        hpBase = this.hpBase,
        hpMax = this.hpMax,
        effects = this.skills.map { it.name },
        skillIcon = extractSkillIcon(),
        imageUrl = extractCraftEssenceImageUrl(),
        isOwned = isOwned,
        currentLevel = currentLevel,
        isMaxLimitBroken = isMaxLimitBroken,
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * Converts a list of ApiCraftEssence to CraftEssence entities
 * 
 * @param ownedCraftEssenceIds Set of owned craft essence IDs for ownership mapping
 * @return List of CraftEssence entities
 */
fun List<ApiCraftEssence>.toCraftEssenceEntities(ownedCraftEssenceIds: Set<Int> = emptySet()): List<CraftEssence> {
    return this.map { apiCraftEssence ->
        apiCraftEssence.toEntity(isOwned = ownedCraftEssenceIds.contains(apiCraftEssence.id))
    }
}

/**
 * Updates existing CraftEssence entity with API data while preserving user data
 * 
 * @param existingCraftEssence Existing craft essence entity
 * @return Updated craft essence entity
 */
fun ApiCraftEssence.updateEntity(existingCraftEssence: CraftEssence): CraftEssence {
    return existingCraftEssence.copy(
        collectionNo = this.collectionNo,
        name = this.name,
        rarity = this.rarity,
        cost = this.cost,
        maxLevel = this.lvMax,
        atkBase = this.atkBase,
        atkMax = this.atkMax,
        hpBase = this.hpBase,
        hpMax = this.hpMax,
        effects = this.skills.map { it.name },
        skillIcon = extractSkillIcon(),
        imageUrl = extractCraftEssenceImageUrl(),
        lastUpdated = System.currentTimeMillis()
        // Preserve user-specific data (isOwned, currentLevel, etc.)
    )
}

// ==================== QUEST MAPPING ====================

/**
 * Converts ApiQuest to Quest entity
 * 
 * @param isUnlocked Whether the quest is unlocked (default: false)
 * @param completionCount Number of times completed (default: 0)
 * @return Quest entity
 */
fun ApiQuest.toEntity(
    isUnlocked: Boolean = false,
    completionCount: Int = 0
): Quest {
    return Quest(
        id = this.id,
        name = this.name,
        type = this.type,
        chapter = extractChapter(),
        apCost = extractApCost(),
        bondPoints = extractBondPoints(),
        experience = extractExperience(),
        qp = extractQp(),
        drops = extractDrops(),
        enemies = extractEnemies(),
        phases = extractPhases(),
        difficulty = extractDifficulty(),
        isRepeatable = determineRepeatable(),
        isUnlocked = isUnlocked,
        completionCount = completionCount,
        bestTime = 0L,
        lastCompleted = if (completionCount > 0) System.currentTimeMillis() else 0L,
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * Converts a list of ApiQuest to Quest entities
 * 
 * @param unlockedQuestIds Set of unlocked quest IDs
 * @param completedQuestCounts Map of quest ID to completion count
 * @return List of Quest entities
 */
fun List<ApiQuest>.toQuestEntities(
    unlockedQuestIds: Set<Int> = emptySet(),
    completedQuestCounts: Map<Int, Int> = emptyMap()
): List<Quest> {
    return this.map { apiQuest ->
        apiQuest.toEntity(
            isUnlocked = unlockedQuestIds.contains(apiQuest.id),
            completionCount = completedQuestCounts[apiQuest.id] ?: 0
        )
    }
}

/**
 * Updates existing Quest entity with API data while preserving user data
 * 
 * @param existingQuest Existing quest entity
 * @return Updated quest entity
 */
fun ApiQuest.updateEntity(existingQuest: Quest): Quest {
    return existingQuest.copy(
        name = this.name,
        type = this.type,
        chapter = extractChapter(),
        apCost = extractApCost(),
        bondPoints = extractBondPoints(),
        experience = extractExperience(),
        qp = extractQp(),
        drops = extractDrops(),
        enemies = extractEnemies(),
        phases = extractPhases(),
        difficulty = extractDifficulty(),
        isRepeatable = determineRepeatable(),
        lastUpdated = System.currentTimeMillis()
        // Preserve user-specific data (isUnlocked, completionCount, etc.)
    )
}

// ==================== HELPER FUNCTIONS ====================

/**
 * Extracts card hit counts from servant data
 * 
 * @return List of hit counts [Quick, Arts, Buster, Extra, NP]
 */
private fun ApiServant.extractCardHits(): List<Int> {
    return try {
        val hits = mutableListOf<Int>()
        // Extract from hitsDistribution if available
        this.hitsDistribution["quick"]?.sum()?.let { hits.add(it) } ?: hits.add(1)
        this.hitsDistribution["arts"]?.sum()?.let { hits.add(it) } ?: hits.add(1)
        this.hitsDistribution["buster"]?.sum()?.let { hits.add(it) } ?: hits.add(1)
        this.hitsDistribution["extra"]?.sum()?.let { hits.add(it) } ?: hits.add(1)
        this.hitsDistribution["np"]?.sum()?.let { hits.add(it) } ?: hits.add(1)
        hits
    } catch (e: Exception) {
        listOf(1, 1, 1, 1, 1) // Default values
    }
}

/**
 * Calculates NP charge rate from servant data
 * 
 * @return NP charge rate
 */
private fun ApiServant.calculateNpCharge(): Double {
    // This would need actual calculation based on servant data
    // For now, return a default value
    return 1.0
}

/**
 * Calculates critical damage multiplier
 * 
 * @return Critical damage multiplier
 */
private fun ApiServant.calculateCritDamage(): Double {
    // Default critical damage multiplier
    return 2.0
}

/**
 * Calculates class attack rate
 * 
 * @return Class attack rate
 */
private fun ApiServant.calculateClassAttackRate(): Double {
    return when (this.className.lowercase()) {
        "saber", "archer", "lancer" -> 1.0
        "rider", "caster", "assassin" -> 0.9
        "berserker" -> 1.1
        else -> 1.0
    }
}

/**
 * Calculates class defense rate
 * 
 * @return Class defense rate
 */
private fun ApiServant.calculateClassDefenseRate(): Double {
    return when (this.className.lowercase()) {
        "saber", "archer", "lancer" -> 1.0
        "rider", "caster", "assassin" -> 1.0
        "berserker" -> 0.9
        else -> 1.0
    }
}

/**
 * Extracts image URL from servant data
 * 
 * @return Image URL
 */
private fun ApiServant.extractImageUrl(): String {
    return this.extraAssets?.faces?.ascension?.get("1") ?: ""
}

/**
 * Extracts skill icon from craft essence data
 * 
 * @return Skill icon identifier
 */
private fun ApiCraftEssence.extractSkillIcon(): String {
    return this.skills.firstOrNull()?.icon ?: ""
}

/**
 * Extracts image URL from craft essence data
 * 
 * @return Image URL
 */
private fun ApiCraftEssence.extractCraftEssenceImageUrl(): String {
    return this.extraAssets?.charaGraph?.ascension?.get("1") ?: ""
}

/**
 * Extracts chapter information from quest data
 * 
 * @return Chapter name
 */
private fun ApiQuest.extractChapter(): String {
    return "Chapter ${this.id / 1000}" // Simple chapter extraction
}

/**
 * Extracts AP cost from quest data
 * 
 * @return AP cost
 */
private fun ApiQuest.extractApCost(): Int {
    // This would need to be extracted from actual quest data
    return when (this.type.lowercase()) {
        "daily" -> 20
        "free" -> 15
        "main" -> 25
        else -> 20
    }
}

/**
 * Extracts bond points from quest data
 * 
 * @return Bond points
 */
private fun ApiQuest.extractBondPoints(): Int {
    return when (this.type.lowercase()) {
        "daily" -> 50
        "free" -> 30
        "main" -> 100
        else -> 50
    }
}

/**
 * Extracts experience from quest data
 * 
 * @return Experience points
 */
private fun ApiQuest.extractExperience(): Int {
    return when (this.type.lowercase()) {
        "daily" -> 100
        "free" -> 75
        "main" -> 150
        else -> 100
    }
}

/**
 * Extracts QP from quest data
 * 
 * @return QP amount
 */
private fun ApiQuest.extractQp(): Int {
    return when (this.type.lowercase()) {
        "daily" -> 1000
        "free" -> 750
        "main" -> 1500
        else -> 1000
    }
}

/**
 * Extracts drop information from quest data
 * 
 * @return List of possible drops
 */
private fun ApiQuest.extractDrops(): List<String> {
    // This would need to be extracted from actual quest data
    return listOf("QP", "Experience")
}

/**
 * Extracts enemy information from quest data
 * 
 * @return List of enemy types
 */
private fun ApiQuest.extractEnemies(): List<String> {
    // This would need to be extracted from actual quest data
    return listOf("Unknown Enemy")
}

/**
 * Extracts number of phases from quest data
 * 
 * @return Number of phases
 */
private fun ApiQuest.extractPhases(): Int {
    // Default to 3 phases
    return 3
}

/**
 * Extracts difficulty from quest data
 * 
 * @return Difficulty rating (1-5)
 */
private fun ApiQuest.extractDifficulty(): Int {
    return when (this.type.lowercase()) {
        "daily" -> 2
        "free" -> 3
        "main" -> 4
        else -> 3
    }
}

/**
 * Determines if quest is repeatable based on quest type
 * 
 * @return True if quest is repeatable
 */
private fun ApiQuest.determineRepeatable(): Boolean {
    return when (this.type.lowercase()) {
        "free", "daily", "training" -> true
        "main", "event" -> false
        else -> true
    }
}

// ==================== VALIDATION HELPERS ====================

/**
 * Validates servant data before mapping
 * 
 * @return True if data is valid
 */
fun ApiServant.isValid(): Boolean {
    return this.id > 0 && 
           this.name.isNotBlank() && 
           this.rarity in 1..5 &&
           this.atkMax >= 0 &&
           this.hpMax >= 0
}

/**
 * Validates craft essence data before mapping
 * 
 * @return True if data is valid
 */
fun ApiCraftEssence.isValid(): Boolean {
    return this.id > 0 && 
           this.name.isNotBlank() && 
           this.rarity in 1..5 &&
           this.atkMax >= 0 &&
           this.hpMax >= 0
}

/**
 * Validates quest data before mapping
 * 
 * @return True if data is valid
 */
fun ApiQuest.isValid(): Boolean {
    return this.id > 0 && 
           this.name.isNotBlank()
} 
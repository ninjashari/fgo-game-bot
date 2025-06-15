/*
 * FGO Bot - Atlas Academy API Service
 * 
 * This file defines the Retrofit service interface for Atlas Academy API endpoints.
 * Provides methods for fetching servant, craft essence, and quest data.
 */

package com.fgobot.data.api

import com.fgobot.data.api.models.ApiServant
import com.fgobot.data.api.models.ApiCraftEssence
import com.fgobot.data.api.models.ApiQuest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Atlas Academy API Service Interface
 * 
 * Defines all API endpoints for fetching FGO game data from Atlas Academy.
 * Supports servant data, craft essence data, quest information, and more.
 */
interface AtlasAcademyService {
    
    // ==================== SERVANT ENDPOINTS ====================
    
    /**
     * Get all servants
     * 
     * @param region Server region (NA, JP, etc.)
     * @return List of all servants
     */
    @GET("{region}/servant")
    suspend fun getAllServants(
        @Path("region") region: String = "NA"
    ): Response<List<ApiServant>>
    
    /**
     * Get servant by ID
     * 
     * @param region Server region
     * @param servantId Servant ID
     * @return Servant details
     */
    @GET("{region}/servant/{id}")
    suspend fun getServantById(
        @Path("region") region: String = "NA",
        @Path("id") servantId: Int
    ): Response<ApiServant>
    
    /**
     * Search servants by name
     * 
     * @param region Server region
     * @param name Servant name to search
     * @return List of matching servants
     */
    @GET("{region}/servant/search")
    suspend fun searchServants(
        @Path("region") region: String = "NA",
        @Query("name") name: String
    ): Response<List<ApiServant>>
    
    // ==================== CRAFT ESSENCE ENDPOINTS ====================
    
    /**
     * Get all craft essences
     * 
     * @param region Server region
     * @return List of all craft essences
     */
    @GET("{region}/craft-essence")
    suspend fun getAllCraftEssences(
        @Path("region") region: String = "NA"
    ): Response<List<ApiCraftEssence>>
    
    /**
     * Get craft essence by ID
     * 
     * @param region Server region
     * @param ceId Craft essence ID
     * @return Craft essence details
     */
    @GET("{region}/craft-essence/{id}")
    suspend fun getCraftEssenceById(
        @Path("region") region: String = "NA",
        @Path("id") ceId: Int
    ): Response<ApiCraftEssence>
    
    /**
     * Search craft essences by name
     * 
     * @param region Server region
     * @param name Craft essence name to search
     * @return List of matching craft essences
     */
    @GET("{region}/craft-essence/search")
    suspend fun searchCraftEssences(
        @Path("region") region: String = "NA",
        @Query("name") name: String
    ): Response<List<ApiCraftEssence>>
    
    // ==================== QUEST ENDPOINTS ====================
    
    /**
     * Get all quests
     * 
     * @param region Server region
     * @return List of all quests
     */
    @GET("{region}/quest")
    suspend fun getAllQuests(
        @Path("region") region: String = "NA"
    ): Response<List<ApiQuest>>
    
    /**
     * Get quest by ID
     * 
     * @param region Server region
     * @param questId Quest ID
     * @return Quest details
     */
    @GET("{region}/quest/{id}")
    suspend fun getQuestById(
        @Path("region") region: String = "NA",
        @Path("id") questId: Int
    ): Response<ApiQuest>
    
    /**
     * Search quests by name
     * 
     * @param region Server region
     * @param name Quest name to search
     * @return List of matching quests
     */
    @GET("{region}/quest/search")
    suspend fun searchQuests(
        @Path("region") region: String = "NA",
        @Query("name") name: String
    ): Response<List<ApiQuest>>
    
    // ==================== FILTERED ENDPOINTS ====================
    
    /**
     * Get servants by class
     * 
     * @param region Server region
     * @param className Servant class name
     * @return List of servants of the specified class
     */
    @GET("{region}/servant")
    suspend fun getServantsByClass(
        @Path("region") region: String = "NA",
        @Query("className") className: String
    ): Response<List<ApiServant>>
    
    /**
     * Get servants by rarity
     * 
     * @param region Server region
     * @param rarity Star rating (1-5)
     * @return List of servants of the specified rarity
     */
    @GET("{region}/servant")
    suspend fun getServantsByRarity(
        @Path("region") region: String = "NA",
        @Query("rarity") rarity: Int
    ): Response<List<ApiServant>>
    
    /**
     * Get craft essences by rarity
     * 
     * @param region Server region
     * @param rarity Star rating (1-5)
     * @return List of craft essences of the specified rarity
     */
    @GET("{region}/craft-essence")
    suspend fun getCraftEssencesByRarity(
        @Path("region") region: String = "NA",
        @Query("rarity") rarity: Int
    ): Response<List<ApiCraftEssence>>
    
    /**
     * Get quests by type
     * 
     * @param region Server region
     * @param type Quest type (main, free, event, etc.)
     * @return List of quests of the specified type
     */
    @GET("{region}/quest")
    suspend fun getQuestsByType(
        @Path("region") region: String = "NA",
        @Query("type") type: String
    ): Response<List<ApiQuest>>
    
    // ==================== UTILITY ENDPOINTS ====================
    
    /**
     * Get basic servant list (minimal data for overview)
     * 
     * @param region Server region
     * @return List of servants with basic information
     */
    @GET("{region}/servant/basic")
    suspend fun getBasicServantList(
        @Path("region") region: String = "NA"
    ): Response<List<ApiServant>>
    
    /**
     * Get basic craft essence list (minimal data for overview)
     * 
     * @param region Server region
     * @return List of craft essences with basic information
     */
    @GET("{region}/craft-essence/basic")
    suspend fun getBasicCraftEssenceList(
        @Path("region") region: String = "NA"
    ): Response<List<ApiCraftEssence>>
    
    /**
     * Get basic quest list (minimal data for overview)
     * 
     * @param region Server region
     * @return List of quests with basic information
     */
    @GET("{region}/quest/basic")
    suspend fun getBasicQuestList(
        @Path("region") region: String = "NA"
    ): Response<List<ApiQuest>>
} 
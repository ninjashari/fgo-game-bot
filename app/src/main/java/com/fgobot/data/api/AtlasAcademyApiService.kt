/*
 * FGO Bot - Atlas Academy API Service
 * 
 * This file defines the Retrofit API service interface for communicating
 * with the Atlas Academy API to fetch FGO game data.
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
 * Retrofit API service interface for Atlas Academy API
 * 
 * Provides endpoints for fetching FGO game data including servants,
 * craft essences, quests, and other game information.
 */
interface AtlasAcademyApiService {
    
    companion object {
        /**
         * Base URL for Atlas Academy API
         */
        const val BASE_URL = "https://api.atlasacademy.io/"
        
        /**
         * API version path
         */
        const val API_VERSION = "nice/NA/"
    }
    
    /**
     * Get all servants from the API
     * 
     * @param region Game region (NA, JP, etc.)
     * @return Response containing list of servants
     */
    @GET("${API_VERSION}servant")
    suspend fun getAllServants(
        @Query("region") region: String = "NA"
    ): Response<List<ApiServant>>
    
    /**
     * Get a specific servant by ID
     * 
     * @param servantId Servant ID
     * @param region Game region
     * @return Response containing servant data
     */
    @GET("${API_VERSION}servant/{id}")
    suspend fun getServantById(
        @Path("id") servantId: Int,
        @Query("region") region: String = "NA"
    ): Response<ApiServant>
    
    /**
     * Get all craft essences from the API
     * 
     * @param region Game region
     * @return Response containing list of craft essences
     */
    @GET("${API_VERSION}equip")
    suspend fun getAllCraftEssences(
        @Query("region") region: String = "NA"
    ): Response<List<ApiCraftEssence>>
    
    /**
     * Get a specific craft essence by ID
     * 
     * @param craftEssenceId Craft essence ID
     * @param region Game region
     * @return Response containing craft essence data
     */
    @GET("${API_VERSION}equip/{id}")
    suspend fun getCraftEssenceById(
        @Path("id") craftEssenceId: Int,
        @Query("region") region: String = "NA"
    ): Response<ApiCraftEssence>
    
    /**
     * Get all quests from the API
     * 
     * @param region Game region
     * @return Response containing list of quests
     */
    @GET("${API_VERSION}quest")
    suspend fun getAllQuests(
        @Query("region") region: String = "NA"
    ): Response<List<ApiQuest>>
    
    /**
     * Get a specific quest by ID
     * 
     * @param questId Quest ID
     * @param region Game region
     * @return Response containing quest data
     */
    @GET("${API_VERSION}quest/{id}")
    suspend fun getQuestById(
        @Path("id") questId: Int,
        @Query("region") region: String = "NA"
    ): Response<ApiQuest>
    
    /**
     * Search servants by name
     * 
     * @param name Servant name to search for
     * @param region Game region
     * @return Response containing matching servants
     */
    @GET("${API_VERSION}servant")
    suspend fun searchServantsByName(
        @Query("name") name: String,
        @Query("region") region: String = "NA"
    ): Response<List<ApiServant>>
    
    /**
     * Get servants by class
     * 
     * @param className Servant class (saber, archer, etc.)
     * @param region Game region
     * @return Response containing servants of specified class
     */
    @GET("${API_VERSION}servant")
    suspend fun getServantsByClass(
        @Query("className") className: String,
        @Query("region") region: String = "NA"
    ): Response<List<ApiServant>>
    
    /**
     * Get servants by rarity
     * 
     * @param rarity Star rating (1-5)
     * @param region Game region
     * @return Response containing servants of specified rarity
     */
    @GET("${API_VERSION}servant")
    suspend fun getServantsByRarity(
        @Query("rarity") rarity: Int,
        @Query("region") region: String = "NA"
    ): Response<List<ApiServant>>
} 
package com.fgobot.data.api

import com.fgobot.data.api.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AtlasAcademyApi {
    
    @GET("basic/servant")
    suspend fun getAllServants(
        @Query("region") region: String = "NA"
    ): Response<List<ServantResponse>>
    
    @GET("basic/servant/{id}")
    suspend fun getServantById(
        @Path("id") id: Int,
        @Query("region") region: String = "NA"
    ): Response<ServantResponse>
    
    @GET("basic/equip")
    suspend fun getAllCraftEssences(
        @Query("region") region: String = "NA"
    ): Response<List<CraftEssenceResponse>>
    
    @GET("basic/equip/{id}")
    suspend fun getCraftEssenceById(
        @Path("id") id: Int,
        @Query("region") region: String = "NA"
    ): Response<CraftEssenceResponse>
    
    @GET("basic/quest")
    suspend fun getAllQuests(
        @Query("region") region: String = "NA"
    ): Response<List<QuestResponse>>
    
    @GET("basic/quest/{id}")
    suspend fun getQuestById(
        @Path("id") id: Int,
        @Query("region") region: String = "NA"
    ): Response<QuestResponse>
    
    @GET("basic/commandCode")
    suspend fun getAllCommandCodes(
        @Query("region") region: String = "NA"
    ): Response<List<CommandCodeResponse>>
    
    companion object {
        const val BASE_URL = "https://api.atlasacademy.io/"
    }
} 
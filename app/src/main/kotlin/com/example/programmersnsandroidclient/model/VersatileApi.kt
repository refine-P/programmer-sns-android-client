package com.example.programmersnsandroidclient.model

import retrofit2.Response
import retrofit2.http.*

interface VersatileApi {
    @GET("text/all?\$orderby=_created_at%20desc")
    suspend fun fetchTimeline(@Query("\$limit") limit: Int): Response<List<SnsContentInternal>>

    @GET("text/all?\$orderby=_created_at%20desc")
    suspend fun fetchTimelineWithFilter(
        @Query("\$limit") limit: Int,
        @Query("\$filter") filter: String
    ): Response<List<SnsContentInternal>>

    @GET("user/{user_id}")
    suspend fun fetchUser(@Path("user_id") userId: String): Response<SnsUser>

    @GET("user/all")
    suspend fun fetchAllUsers(): Response<List<SnsUser>>

    @Headers("Authorization: HelloWorld")
    @POST("text")
    suspend fun sendSnsPost(@Body post: SnsPost): Response<Void>

    @PUT("user/create_user")
    suspend fun updateUser(@Body userSetting: UserSetting): Response<UserId>
}
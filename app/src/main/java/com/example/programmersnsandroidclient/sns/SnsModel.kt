package com.example.programmersnsandroidclient.sns

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SnsModel(
    private val service : VersatileApi = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VersatileApi::class.java),
) {
    private val userCache by lazy { loadUserCache() }

    suspend fun fetchTimeline(timelineNumLimit: Int, refreshUserCache: Boolean): List<SnsContent>? {
        if (refreshUserCache) userCache.putAll(loadUserCache())

        val timelineInternal = service.fetchTimeline(timelineNumLimit).body() ?: return null
        return timelineInternal.mapNotNull {
            loadSnsPost(it)
        }
    }

    suspend fun sendSnsPost(content: String) {
        service.sendSnsPost(SnsPost(content, null, null))
    }

    private fun loadUserCache(): HashMap<String, SnsUser> {
        val allUsers = runBlocking {
            service.fetchAllUsers().body()
        }
        return allUsers?.associateTo(hashMapOf()) { it.id to it } ?: hashMapOf()
    }

    private fun loadSnsPost(postInternal: SnsContentInternal): SnsContent? {
        return userCache[postInternal._user_id]?.let { user ->
            SnsContent(postInternal.id, user.name, postInternal.text)
        }
    }
}
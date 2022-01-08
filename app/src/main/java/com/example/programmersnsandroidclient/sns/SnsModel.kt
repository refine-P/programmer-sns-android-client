package com.example.programmersnsandroidclient.sns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val INITIAL_TIMELINE_NUM_LIMIT = 50
const val INCREMENTAL_TIMELINE_NUM_LIMIT = 30

class SnsModel {
    private val service = Retrofit.Builder().apply {
        baseUrl("https://versatileapi.herokuapp.com/api/")
        addConverterFactory(GsonConverterFactory.create())
    }.build().create(VersatileApi::class.java)

    private var timelineNumLimit = INITIAL_TIMELINE_NUM_LIMIT
    private val userCache by lazy { loadUserCache() }

    suspend fun fetchTimeline(refreshUserCache: Boolean): List<SnsContent>? {
        if (refreshUserCache) userCache.putAll(loadUserCache())

        val timelineInternal = service.fetchTimeline(timelineNumLimit).body() ?: return null
        return timelineInternal.mapNotNull {
            loadSnsPost(it)
        }
    }

    suspend fun fetchTimelineMore(): List<SnsContent>? {
        timelineNumLimit += INCREMENTAL_TIMELINE_NUM_LIMIT
        val timeline = fetchTimeline(false)

        if (timeline == null) {
            timelineNumLimit -= INCREMENTAL_TIMELINE_NUM_LIMIT
        }
        return timeline
    }

    suspend fun sendSnsPost(content: String) {
        service.sendSnsPost(SnsPost(content, null, null))
    }

    private fun loadUserCache(): HashMap<String, SnsUser> {
        val allUsers = runBlocking(Dispatchers.IO) {
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
package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.SnsContentInternal
import com.example.programmersnsandroidclient.sns.SnsPost
import com.example.programmersnsandroidclient.sns.SnsUser
import com.example.programmersnsandroidclient.sns.VersatileApi
import retrofit2.Response
import retrofit2.mock.BehaviorDelegate

class MockVersatileApi(
    private val delegate : BehaviorDelegate<VersatileApi>
) : VersatileApi {
    var allTimeline : List<SnsContentInternal>? = null
    var allUsers : List<SnsUser>? = null

    override suspend fun fetchTimeline(limit: Int): Response<List<SnsContentInternal>> {
        return delegate.returningResponse(allTimeline?.take(limit)).fetchTimeline(limit)
    }

    override suspend fun fetchUser(userId: String): Response<SnsUser> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllUsers(): Response<List<SnsUser>> {
        return delegate.returningResponse(allUsers).fetchAllUsers()
    }

    override suspend fun sendSnsPost(post: SnsPost): Response<Void> {
        return delegate.returningResponse(null).sendSnsPost(post)
    }
}
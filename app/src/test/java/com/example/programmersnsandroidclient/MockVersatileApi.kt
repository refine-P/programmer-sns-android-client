package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.*
import retrofit2.Response
import retrofit2.mock.BehaviorDelegate

class MockVersatileApi(
    private val delegate: BehaviorDelegate<VersatileApi>
) : VersatileApi {
    lateinit var allTimeline: List<SnsContentInternal>
    lateinit var allUsers: List<SnsUser>
    lateinit var currentUserId: String

    override suspend fun fetchTimeline(limit: Int): Response<List<SnsContentInternal>> {
        return delegate.returningResponse(allTimeline.take(limit)).fetchTimeline(limit)
    }

    override suspend fun fetchTimelineWithFilter(
        limit: Int,
        filter: String
    ): Response<List<SnsContentInternal>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchUser(userId: String): Response<SnsUser> {
        val targetUser = allUsers.find { it.id == userId }
        return delegate.returningResponse(targetUser).fetchUser(userId)
    }

    override suspend fun fetchAllUsers(): Response<List<SnsUser>> {
        return delegate.returningResponse(allUsers).fetchAllUsers()
    }

    override suspend fun updateUser(userSetting: UserSetting): Response<UserId> {
        return delegate.returningResponse(UserId(currentUserId)).updateUser(userSetting)
    }

    override suspend fun sendSnsPost(post: SnsPost): Response<Void> {
        return delegate.returningResponse(null).sendSnsPost(post)
    }
}
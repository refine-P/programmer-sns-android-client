package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.*
import retrofit2.Response
import retrofit2.mock.BehaviorDelegate

class MockVersatileApi(
    private val delegate: BehaviorDelegate<VersatileApi>
) : VersatileApi {
    var allTimeline: List<SnsContentInternal>? = null
    var allUsers: List<SnsUser>? = null
    var currentUserId: String? = null

    override suspend fun fetchTimeline(limit: Int): Response<List<SnsContentInternal>> {
        return delegate.returningResponse(allTimeline?.take(limit)).fetchTimeline(limit)
    }

    override suspend fun fetchUser(userId: String): Response<SnsUser> {
        val targetUser = allUsers?.find { it.id == userId }
        return delegate.returningResponse(targetUser).fetchUser(userId)
    }

    override suspend fun fetchAllUsers(): Response<List<SnsUser>> {
        return delegate.returningResponse(allUsers).fetchAllUsers()
    }

    // TODO: allUsers は API の出力を指定するためのみに使われるべきでは？（allUsers を関数内で変更すべきでない？）
    override suspend fun updateUser(userSetting: UserSetting): Response<UserId> {
        val res = delegate.returningResponse(
            if (currentUserId == null) {
                null
            } else {
                UserId(currentUserId!!)
            }
        ).updateUser(userSetting)
        if (!res.isSuccessful) return res

        val userId = currentUserId ?: return res
        val targetUser = SnsUser(userId, userSetting.name, userSetting.description)
        allUsers = allUsers?.filterNot { it == targetUser }?.plus(targetUser)
        return res
    }

    // TODO: allTimeline は API の出力を指定するためのみに使われるべきでは？（allTimeline を関数内で変更すべきでない？）
    override suspend fun sendSnsPost(post: SnsPost): Response<Void> {
        val res = delegate.returningResponse(null).sendSnsPost(post)
        if (!res.isSuccessful || allTimeline == null) return res

        val userId = currentUserId ?: return res
        val contentId = "dummy_content_id%s".format(allTimeline!!.size + 1)
        val inReplyToUserId = post.in_reply_to_user_id ?: ""
        val inReplyToTextId = post.in_reply_to_text_id ?: ""
        allTimeline = allTimeline?.plus(
            SnsContentInternal(
                contentId,
                post.text,
                inReplyToUserId,
                inReplyToTextId,
                userId,
                "",
                ""
            )
        )
        return res
    }
}
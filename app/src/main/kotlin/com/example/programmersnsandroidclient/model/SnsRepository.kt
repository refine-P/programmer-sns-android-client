package com.example.programmersnsandroidclient.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: 異常系の処理について検討した方が良いかも？
class SnsRepository(
    private val service: VersatileApi,
    appContext: Context,
    private val userDao: UserDao,
    // 未登録ユーザーの名前をユーザーIDそのものにするかどうかのフラグ（テスト用）。
    // falseの場合、名前をユーザーIDの先頭8桁+" [未登録]"にする。
    // テスト時にのみtrueにする。
    private val shouldUseFullIdAsUnregisteredUserName: Boolean,
    private val dispatcher: CoroutineDispatcher
) {
    @Inject
    constructor(
        service: VersatileApi,
        @ApplicationContext appContext: Context,
        userDao: UserDao
    ) : this(service, appContext, userDao, false, Dispatchers.IO)

    private val prefs = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)

    suspend fun fetchTimeline(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean
    ): List<SnsContent>? {
        return fetchContents(timelineNumLimit, shouldRefreshUserCache)
    }

    suspend fun fetchUserContents(
        userId: String,
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean
    ): List<SnsContent>? {
        return fetchContents(timelineNumLimit, shouldRefreshUserCache, userId)
    }

    suspend fun fetchUser(userId: String): SnsUser? {
        return service.fetchUser(userId).body()
    }

    suspend fun loadUserFromCache(userId: String): SnsUser {
        return withContext(dispatcher) {
            userDao.getUser(userId)
        } ?: SnsUser(userId, getUnregisteredUserName(userId), "")
    }

    suspend fun sendSnsPost(content: String): Boolean {
        return service.sendSnsPost(SnsPost(content, null, null)).isSuccessful
    }

    suspend fun updateUser(name: String, description: String): String? {
        return service.updateUser(UserSetting(name, description)).body()?.id
    }

    fun loadCurrentUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun storeCurrentUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    suspend fun refreshUserCache() {
        withContext(dispatcher) {
            service.fetchAllUsers().body()?.let {
                userDao.insertUsers(it)
            }
        }
    }

    private suspend fun fetchContents(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean,
        userId: String? = null,
    ): List<SnsContent>? {
        if (shouldRefreshUserCache) refreshUserCache()

        val res = if (userId == null) {  // userIdがnullなら全ユーザーの投稿を取得
            service.fetchTimeline(timelineNumLimit)
        } else {
            service.fetchTimelineWithFilter(timelineNumLimit, "_user_id eq '%s'".format(userId))
        }

        val timelineInternal = res.body() ?: return null
        return timelineInternal.map {
            loadSnsPost(it)
        }
    }

    private suspend fun loadSnsPost(postInternal: SnsContentInternal): SnsContent {
        val unregisteredUserName = getUnregisteredUserName(postInternal._user_id)
        val contentFromUnregisteredUser =
            SnsContent(
                postInternal.id,
                postInternal._user_id,
                unregisteredUserName,
                postInternal.text
            )
        val user = loadUserFromCache(postInternal._user_id) ?: return contentFromUnregisteredUser
        return SnsContent(postInternal.id, postInternal._user_id, user.name, postInternal.text)
    }

    private fun getUnregisteredUserName(userId: String): String {
        return if (shouldUseFullIdAsUnregisteredUserName) {
            userId
        } else {
            userId.take(8) + " [未登録]"
        }
    }
}
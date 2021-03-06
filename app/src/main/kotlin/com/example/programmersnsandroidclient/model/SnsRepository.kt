package com.example.programmersnsandroidclient.model

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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

    companion object {
        const val TAG = "SnsRepository"
    }

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
        return try {
            service.fetchUser(userId).body()
        } catch (e: Exception) {
            Log.w(TAG, "Can't fetch user due to ${getCauseString(e)}: ${e.message ?: ""}")
            null
        }
    }

    suspend fun loadUserFromCache(userId: String): SnsUser {
        return withContext(dispatcher) {
            userDao.getUser(userId)
        } ?: SnsUser(userId, getUnregisteredUserName(userId), "")
    }

    suspend fun sendSnsPost(content: String): Boolean {
        return try {
            service.sendSnsPost(SnsPost(content, null, null)).isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Can't send a post due to ${getCauseString(e)}: ${e.message ?: ""}")
            false
        }
    }

    suspend fun updateUser(name: String, description: String): String? {
        return try {
            service.updateUser(UserSetting(name, description)).body()?.id
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Can't update user's infomation due to ${getCauseString(e)}: ${e.message ?: ""}"
            )
            null
        }
    }

    fun loadCurrentUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun storeCurrentUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    suspend fun refreshUserCache() {
        withContext(dispatcher) {
            try {
                service.fetchAllUsers().body()?.let {
                    userDao.insertUsers(it)
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Can't refresh UserCache due to ${getCauseString(e)}: ${e.message ?: ""}"
                )
            }
        }
    }

    private suspend fun fetchContents(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean,
        userId: String? = null,
    ): List<SnsContent>? {
        if (shouldRefreshUserCache) refreshUserCache()

        try {
            val res = if (userId == null) {  // userIdがnullなら全ユーザーの投稿を取得
                service.fetchTimeline(timelineNumLimit)
            } else {
                service.fetchTimelineWithFilter(timelineNumLimit, "_user_id eq '%s'".format(userId))
            }

            val timelineInternal = res.body() ?: return null
            return timelineInternal.map {
                loadSnsPost(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Can't fetch contents due to ${getCauseString(e)}: ${e.message ?: ""}")
            return null
        }
    }

    private suspend fun loadSnsPost(postInternal: SnsContentInternal): SnsContent {
        val user = loadUserFromCache(postInternal._user_id)
        val createdAt = try {
            ZonedDateTime.parse(postInternal._created_at)
                .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
        } catch (e: Exception) {
            ""
        }
        return SnsContent(
            postInternal.id,
            postInternal._user_id,
            user.name,
            postInternal.text,
            createdAt
        )
    }

    private fun getUnregisteredUserName(userId: String): String {
        return if (shouldUseFullIdAsUnregisteredUserName) {
            userId
        } else {
            userId.take(8) + " [未登録]"
        }
    }

    private fun getCauseString(cause: Exception): String {
        return when (cause) {
            is IOException -> "network failure"
            is HttpException -> "HTTP error"
            else -> "unknown reasons"
        }
    }
}
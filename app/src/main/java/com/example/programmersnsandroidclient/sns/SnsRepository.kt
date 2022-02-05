package com.example.programmersnsandroidclient.sns

import android.content.Context
import androidx.room.Room
import com.example.programmersnsandroidclient.ProgrammerSns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SnsRepository(
    private val service: VersatileApi = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VersatileApi::class.java),
    private val appContext: Context = ProgrammerSns.appContext,
    private val userDao: UserDao = Room.databaseBuilder(
        appContext, UserDatabase::class.java, "user-cache").build().userDao(),
    // 未登録ユーザーの名前をユーザーIDそのものにするかどうかのフラグ（テスト用）。
    // falseの場合、名前をユーザーIDの先頭8桁+" [未登録]"にする。
    // テスト時にのみtrueにする。
    private val shouldUseFullIdAsUnregisteredUserName: Boolean = false,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefs = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)

    suspend fun fetchTimeline(timelineNumLimit: Int, refreshUserCache: Boolean): List<SnsContent>? {
        if (refreshUserCache) loadUserCache()

        val timelineInternal = service.fetchTimeline(timelineNumLimit).body() ?: return null
        return timelineInternal.map {
            loadSnsPost(it)
        }
    }

    suspend fun fetchUser(userId: String): SnsUser? {
        return service.fetchUser(userId).body()
    }

    suspend fun sendSnsPost(content: String) {
        service.sendSnsPost(SnsPost(content, null, null))
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

    private suspend fun loadUserCache() {
        withContext(dispatcher) {
            service.fetchAllUsers().body()?.let {
                userDao.insertUsers(it)
            }
        }
    }

    private suspend fun loadSnsPost(postInternal: SnsContentInternal): SnsContent {
        // TODO: 未登録ユーザーの投稿の表示/非表示を設定で切り替えられると嬉しいかも？
        val unregisteredUserName = if (shouldUseFullIdAsUnregisteredUserName) {
            postInternal._user_id
        } else {
            postInternal._user_id.take(8) + " [未登録]"
        }
        val contentFromUnregisteredUser =
            SnsContent(postInternal.id, unregisteredUserName, postInternal.text)
        val user = withContext(dispatcher) {
            userDao.getUser(postInternal._user_id)
        } ?: return contentFromUnregisteredUser
        return SnsContent(postInternal.id, user.name, postInternal.text)
    }
}
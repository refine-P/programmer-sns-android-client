package com.example.programmersnsandroidclient.sns

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SnsModel(
    private val service: VersatileApi = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VersatileApi::class.java),
    // 未登録ユーザーの名前をユーザーIDそのものにするかどうかのフラグ（テスト用）。
    // falseの場合、名前をユーザーIDの先頭8桁+" [未登録]"にする。
    // テスト時にのみtrueにする。
    private val shouldUseFullIdAsUnregisteredUserName: Boolean = false
) {
    private val userCache by lazy { loadUserCache() }

    suspend fun fetchTimeline(timelineNumLimit: Int, refreshUserCache: Boolean): List<SnsContent>? {
        if (refreshUserCache) userCache.putAll(loadUserCache())

        val timelineInternal = service.fetchTimeline(timelineNumLimit).body() ?: return null
        return timelineInternal.mapNotNull {
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

    private fun loadUserCache(): HashMap<String, SnsUser> {
        val allUsers = runBlocking {
            service.fetchAllUsers().body()
        }
        return allUsers?.associateTo(hashMapOf()) { it.id to it } ?: hashMapOf()
    }

    private fun loadSnsPost(postInternal: SnsContentInternal): SnsContent? {
        // TODO: 未登録ユーザーの投稿の表示/非表示を設定で切り替えられると嬉しいかも？
        val unregisteredUserName = if (shouldUseFullIdAsUnregisteredUserName) {
            postInternal._user_id
        } else {
            postInternal._user_id.take(8) + " [未登録]"
        }
        val contentFromUnregisteredUser =
            SnsContent(postInternal.id, unregisteredUserName, postInternal.text)
        val user = userCache[postInternal._user_id] ?: return contentFromUnregisteredUser
        return SnsContent(postInternal.id, user.name, postInternal.text)
    }
}
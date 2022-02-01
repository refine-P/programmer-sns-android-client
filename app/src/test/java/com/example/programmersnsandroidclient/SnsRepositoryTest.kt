package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

class SnsRepositoryTest {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val behavior = NetworkBehavior.create()
    private val delegate = MockRetrofit.Builder(retrofit).networkBehavior(behavior).build()
        .create(VersatileApi::class.java)
    private val service = MockVersatileApi(delegate)
    private val repository = SnsRepository(service, shouldUseFullIdAsUnregisteredUserName = true)

    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", "", "", "dummy_user_id", "", ""),
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_name", "dummy_description"),
    )
    private val dummyCurrentUserId = dummyUsers[0].id

    private fun setUpService(isSuccess: Boolean) {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(
                if (isSuccess) {
                    0
                } else {
                    100
                }
            )
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers
        service.currentUserId = dummyCurrentUserId
    }

    @Test
    fun fetchTimeline_success() {
        setUpService(true)

        val actual = runBlocking {
            repository.fetchTimeline(1, false)
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_failure() {
        setUpService(false)

        val actual = runBlocking {
            repository.fetchTimeline(1, false)
        }
        assertNull(actual)
    }

    @Test
    fun fetchTimeline_refreshUserCache() {
        setUpService(true)

        // fetchTimelineを1度実行することで、UserCacheをloadさせる。
        val actualBeforeUserAdded = runBlocking {
            repository.fetchTimeline(1, false)
        }
        val expectedBeforeUserAdded = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeUserAdded, actualBeforeUserAdded)

        // UserとContentが追加される
        service.allTimeline = dummyTimeline.plus(
            SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id2", "", "")
        )
        service.allUsers = dummyUsers.plus(
            SnsUser("dummy_user_id2", "dummy_name2", "dummy_description2"),
        )

        // UserCacheをrefreshしない場合
        // UserCacheに新しいUserが存在しないので、未登録ユーザーが投稿したContentとして扱われる。
        val actualBeforeRefresh = runBlocking {
            repository.fetchTimeline(2, false)
        }
        val expectedBeforeRefresh = expectedBeforeUserAdded.plus(
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expectedBeforeRefresh, actualBeforeRefresh)

        // UserCacheをrefreshすることで、新しいUserが投稿したContentが取得される。
        val actual = runBlocking {
            repository.fetchTimeline(2, true)
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchUser_success() {
        setUpService(true)

        val actual = runBlocking {
            repository.fetchUser("dummy_user_id")
        }
        val expected = SnsUser("dummy_user_id", "dummy_name", "dummy_description")
        assertEquals(expected, actual)
    }

    @Test
    fun fetchUser_failure() {
        setUpService(false)

        val actual = runBlocking {
            repository.fetchUser("dummy_user_id")
        }
        assertNull(actual)
    }

    @Test
    fun sendSnsPost_success() {
        setUpService(true)

        runBlocking {
            repository.sendSnsPost("dummy_text2")
        }

        val expected = dummyTimeline.plus(
            SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id", "", "")
        )
        assertEquals(expected, service.allTimeline)
    }

    @Test
    fun sendSnsPost_failure() {
        setUpService(false)

        runBlocking {
            repository.sendSnsPost("dummy_text2")
        }
        assertEquals(dummyTimeline, service.allTimeline)
    }

    @Test
    fun updateUser_success() {
        setUpService(true)

        val actualUserId = runBlocking {
            repository.updateUser("dummy_name2", "dummy_description2")
        }
        assertEquals(dummyCurrentUserId, actualUserId)

        val expectUsers = dummyUsers.plus(
            SnsUser(dummyCurrentUserId, "dummy_name2", "dummy_description2")
        )
        assertEquals(expectUsers, service.allUsers)
    }

    @Test
    fun updateUser_failure() {
        setUpService(false)

        val actualUserId = runBlocking {
            repository.updateUser("dummy_name2", "dummy_description2")
        }
        assertNull(actualUserId)
        assertEquals(dummyUsers, service.allUsers)
    }
}

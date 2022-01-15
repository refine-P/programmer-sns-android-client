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

class SnsModelTest {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val behavior = NetworkBehavior.create()
    private val delegate = MockRetrofit.Builder(retrofit).networkBehavior(behavior).build()
        .create(VersatileApi::class.java)
    private val service = MockVersatileApi(delegate)
    private val model = SnsModel(service)

    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", "", "", "dummy_user_id", "", ""),
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_description", "dummy_name"),
    )

    @Test
    fun fetchTimeline_success() {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(0)
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        val actual = runBlocking {
            model.fetchTimeline(1, false)
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_failure() {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(100)
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        val actual = runBlocking {
            model.fetchTimeline(1, false)
        }
        assertNull(actual)
    }

    @Test
    fun fetchTimeline_refreshUserCache() {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(0)
        }

        // fetchTimelineを1度実行することで、UserCacheをloadさせる。
        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        val actualBeforeUserAdded = runBlocking {
            model.fetchTimeline(1, false)
        }
        val expectedBeforeUserAdded = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeUserAdded, actualBeforeUserAdded)

        // UserとContentが追加される
        service.allTimeline = dummyTimeline + listOf(
            SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id2", "", "")
        )
        service.allUsers = dummyUsers + listOf(
            SnsUser("dummy_user_id2", "dummy_description2", "dummy_name2"),
        )

        // UserCacheをrefreshしない場合
        // UserCacheに新しいUserが存在しないので、新しいUserが投稿したContentは存在しないものとして扱われる。
        val actualBeforeRefresh = runBlocking {
            model.fetchTimeline(2, false)
        }
        assertEquals(expectedBeforeUserAdded, actualBeforeRefresh)

        // UserCacheをrefreshすることで、新しいUserが投稿したContentが取得される。
        val actual = runBlocking {
            model.fetchTimeline(2, true)
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }
}

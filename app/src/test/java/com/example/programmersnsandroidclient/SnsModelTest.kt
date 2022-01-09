package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
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
    private val model = SnsModel(service, 1, 1)

    // Userが2人いて、それぞれが1つずつ投稿をしている状態を想定。
    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", "", "", "dummy_user_id", "", ""),
        SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id2", "", "")
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_description", "dummy_name"),
        SnsUser("dummy_user_id2", "dummy_description2", "dummy_name2")
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
            model.fetchTimeline(false)
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
            model.fetchTimeline(false)
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

        // UserとContentが1つずつしかない状態からスタート
        service.allTimeline = dummyTimeline.take(1)
        service.allUsers = dummyUsers.take(1)

        val actualBeforeUserAdded = runBlocking {
            model.fetchTimeline(false)
        }
        val expectedBeforeUserAdded = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeUserAdded, actualBeforeUserAdded)

        // UserとContentが追加される
        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        // fetchTimelineMore により、すべてのContentを取ろうとする。
        // しかし、UserCacheに新しいUserが存在しないので、新しいUserが投稿したContentは存在しないものとして扱われる。
        val actualBeforeRefresh = runBlocking {
            model.fetchTimelineMore()
        }
        assertEquals(expectedBeforeUserAdded, actualBeforeRefresh)

        // UserCacheをrefreshすることで、新しいUserが投稿したContentが取得される。
        val actual = runBlocking {
            model.fetchTimeline(true)
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimelineMore_success() {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(0)
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        val actual = runBlocking {
            model.fetchTimelineMore()
        }
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)

        // fetchTimelineMoreが成功した場合（戻り値がnullでない）、以降のContentの数の上限が増える。
        val actualAfterFetchMore = runBlocking {
            model.fetchTimeline(false)
        }
        assertEquals(expected, actualAfterFetchMore)
    }

    @Test
    fun fetchTimelineMore_failure() {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // 即座に結果が返ってくるようにする
            setVariancePercent(0)
            setFailurePercent(0)
            setErrorPercent(100)
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers

        val actual = runBlocking {
            model.fetchTimelineMore()
        }
        assertNull(actual)

        // fetchTimelineMoreが失敗した場合（戻り値がnull）、以降のContentの数の上限はfetchTimelineMore実行前と同じ。
        behavior.apply {
            setErrorPercent(0)
        }

        val actualAfterFetchMore = runBlocking {
            model.fetchTimeline(false)
        }
        val expectedAfterFetchMore = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(expectedAfterFetchMore, actualAfterFetchMore)
    }
}

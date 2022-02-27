package com.example.programmersnsandroidclient

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.example.programmersnsandroidclient.viewmodel.TimelineViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

// TODO: kotlinx-coroutines-test を使った実装にする
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TimelineViewModelTest {
    // LiveDataをテストするために必要
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            )
        )
        .build()
    private val behavior = NetworkBehavior.create()
    private val delegate = MockRetrofit.Builder(retrofit).networkBehavior(behavior).build()
        .create(VersatileApi::class.java)
    private val service = MockVersatileApi(delegate)

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val userDao = mock(UserDao::class.java)
    private val repository =
        spy(
            SnsRepository(
                service,
                appContext,
                userDao,
                shouldUseFullIdAsUnregisteredUserName = true,
                Dispatchers.IO
            )
        )

    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", null, null, "dummy_user_id", "", ""),
        SnsContentInternal("dummy_content_id2", "dummy_text2", null, null, "dummy_user_id2", "", "")
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_name", "dummy_description"),
        SnsUser("dummy_user_id2", "dummy_name2", "dummy_description2"),
    )
    private val dummyCurrentUser = dummyUsers[0]
    private val dummyCurrentUserId = dummyCurrentUser.id

    private fun setUpService(isSuccess: Boolean, userNum: Int = dummyUsers.size) {
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
        service.allUsers = dummyUsers.take(userNum)
        service.currentUserId = dummyCurrentUserId
    }

    private suspend fun setUpUserCache(userNum: Int = dummyUsers.size) {
        `when`(repository.refreshUserCache()).then {
            for (user in dummyUsers.take(userNum)) {
                `when`(userDao.getUser(user.id)).thenReturn(user)
            }
        }
    }

    @Test
    fun init_success() = runBlocking {
        setUpService(true)
        setUpUserCache()

        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        val job = viewmodel.init()

        // 初期化処理の直前の状態
        assertNull(viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)

        job.join()

        // 初期化処理完了時の状態
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, viewmodel.timeline.value?.contents)
        assertEquals(TimelineState.INIT, viewmodel.timeline.value?.state)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun init_failure() = runBlocking {
        setUpService(false)
        setUpUserCache()

        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        val job = viewmodel.init()

        // 初期化処理の直前の状態
        assertNull(viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)

        job.join()

        // 初期化処理完了時の状態
        assertNull(viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_success() = runBlocking {
        // 開始時点ではユーザーの情報は1人分しか読み込まれないようにする。
        // viewmodelを初期化した時点でユーザーの情報が読み込まれる。
        setUpService(true, 1)
        setUpUserCache(1)
        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        viewmodel.init().join()

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない場合、読み込まれてないユーザーは未登録ユーザーとして扱われる。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore().join()

        // refreshすると、ユーザーの情報および投稿がすべて読み込まれる。
        setUpUserCache()  // ユーザーが読み込まれるタイミングで UserCache を更新
        val job = viewmodel.refresh()

        // refresh直前の状態
        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(true, viewmodel.isRefreshing.value)

        job.join()

        // refresh完了時の状態
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, viewmodel.timeline.value?.contents)
        assertEquals(TimelineState.REFRESH, viewmodel.timeline.value?.state)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_failure() = runBlocking {
        // 開始時点ではユーザーの情報は1人分しか読み込まれないようにする。
        // viewmodelを初期化した時点でユーザーの情報が読み込まれる。
        setUpService(true, 1)
        setUpUserCache(1)
        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        viewmodel.init().join()

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない場合、読み込まれてないユーザーは未登録ユーザーとして扱われる。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore().join()

        // refreshを失敗させる。タイムラインはrefresh前のまま。
        setUpService(false)
        setUpUserCache()  // refresh のタイミングで UserCache を更新
        val job = viewmodel.refresh()

        // refresh直前の状態
        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(true, viewmodel.isRefreshing.value)

        job.join()

        // refresh完了時の状態
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)
        assertEquals(TimelineState.LOAD_MORE, viewmodel.timeline.value?.state)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_success() = runBlocking {
        setUpService(true)
        setUpUserCache()
        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        viewmodel.init().join()

        val job = viewmodel.loadMore()

        // loadMore直前の状態
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)

        job.join()

        // loadMore完了時の状態
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, viewmodel.timeline.value?.contents)
        assertEquals(TimelineState.LOAD_MORE, viewmodel.timeline.value?.state)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_failure() = runBlocking {
        // viewmodel の初期化は成功させる
        setUpService(true)
        setUpUserCache()
        val viewmodel = TimelineViewModel(repository, 1, 1, Dispatchers.IO, true)
        viewmodel.init().join()

        // それ以降は失敗
        setUpService(false)

        val job = viewmodel.loadMore()

        // loadMore直前の状態
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)

        job.join()

        // loadMore完了時の状態
        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        assertEquals(TimelineState.INIT, viewmodel.timeline.value?.state)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }
}

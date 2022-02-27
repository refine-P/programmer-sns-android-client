package com.example.programmersnsandroidclient

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.example.programmersnsandroidclient.viewmodel.UserContentsViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class UserContentsViewModelTest {
    // LiveDataをテストするために必要
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    companion object {
        // LiveDataを更新する際にsleepで待機する時間の長さ（ミリ秒）。
        // LiveDataの更新前にassertが実行されてしまうのを防ぐためにsleepで対処する。
        // TODO: sleepを使うのはあまり良い方法ではなさそうなので、より賢い方法を探す。
        private const val DELAY_FOR_LIVEDATA_MILLIS: Long = 300
    }

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
        SnsRepository(
            service,
            appContext,
            userDao,
            shouldUseFullIdAsUnregisteredUserName = true,
            Dispatchers.IO
        )

    private lateinit var viewmodel: UserContentsViewModel
    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", null, null, "dummy_user_id", "", ""),
        SnsContentInternal(
            "dummy_content_id2",
            "dummy_text2",
            null,
            null,
            "dummy_user_id2",
            "",
            ""
        ),
        SnsContentInternal("dummy_content_id3", "dummy_text3", null, null, "dummy_user_id", "", ""),
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_name", "dummy_description"),
        SnsUser("dummy_user_id2", "dummy_name2", "dummy_description2"),
    )
    private val dummyCurrentUser = dummyUsers[0]
    private val dummyCurrentUserId = dummyCurrentUser.id

    private fun setUpService(isSuccess: Boolean, contentNum: Int = dummyTimeline.size) {
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

        service.allTimeline = dummyTimeline.take(contentNum)
        service.allUsers = dummyUsers
        service.currentUserId = dummyCurrentUserId
    }

    private fun setUpUserDao(userNum: Int = dummyUsers.size) {
        for (user in dummyUsers.take(userNum)) {
            `when`(userDao.getUser(user.id)).thenReturn(user)
        }
    }

    @Test
    fun init_success() {
        setUpService(true)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Assert.assertNull(viewmodel.timeline.value)
        Assert.assertEquals(true, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text")
        )
        Assert.assertEquals(expected, viewmodel.timeline.value?.contents)
        Assert.assertEquals(TimelineState.INIT, viewmodel.timeline.value?.state)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun init_failure() {
        setUpService(false)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Assert.assertNull(viewmodel.timeline.value)
        Assert.assertEquals(true, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        Assert.assertNull(viewmodel.timeline.value)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_success() {
        // 開始時点では対象ユーザーの2つめの投稿が存在しない状態にする。
        setUpService(true, 2)
        setUpUserDao()
        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // loadMoreを実行することで、投稿の数の上限を2にする。
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // この時点では対象ユーザーの2つめの投稿が存在しないので、ユーザーの投稿は1つだけ。
        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        Assert.assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)

        // ここで、対象ユーザーの2つめの投稿を追加。
        setUpService(true)
        viewmodel.refresh()
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(true, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id3", "dummy_user_id", "dummy_name", "dummy_text3")
        )
        Assert.assertEquals(expected, viewmodel.timeline.value?.contents)
        Assert.assertEquals(TimelineState.REFRESH, viewmodel.timeline.value?.state)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_failure() {
        // 開始時点では対象ユーザーの2つめの投稿が存在しない状態にする。
        setUpService(true, 2)
        setUpUserDao()
        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // loadMoreを実行することで、投稿の数の上限を2にする。
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // この時点では対象ユーザーの2つめの投稿が存在しないので、ユーザーの投稿は1つだけ。
        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        Assert.assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)

        // refresh を失敗させる。ユーザーの投稿は1つだけになる。
        setUpService(false)
        viewmodel.refresh()
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(true, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        Assert.assertEquals(expectedBeforeRefresh, viewmodel.timeline.value?.contents)
        Assert.assertEquals(TimelineState.LOAD_MORE, viewmodel.timeline.value?.state)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_success() {
        setUpService(true)
        setUpUserDao()

        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        viewmodel.loadMore()
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        Assert.assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        Assert.assertEquals(true, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id3", "dummy_user_id", "dummy_name", "dummy_text3")
        )
        Assert.assertEquals(expected, viewmodel.timeline.value?.contents)
        Assert.assertEquals(TimelineState.LOAD_MORE, viewmodel.timeline.value?.state)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        setUpUserDao()
        viewmodel = UserContentsViewModel(repository, dummyCurrentUserId, 1, 1, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        viewmodel.loadMore()
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
        )
        Assert.assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        Assert.assertEquals(true, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        Assert.assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value?.contents)
        Assert.assertEquals(TimelineState.INIT, viewmodel.timeline.value?.state)
        Assert.assertEquals(false, viewmodel.isLoading.value)
        Assert.assertEquals(false, viewmodel.isRefreshing.value)
    }
}

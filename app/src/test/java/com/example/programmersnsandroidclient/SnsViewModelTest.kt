package com.example.programmersnsandroidclient

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.programmersnsandroidclient.sns.*
import junit.framework.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

class SnsViewModelTest {
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
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val behavior = NetworkBehavior.create()
    private val delegate = MockRetrofit.Builder(retrofit).networkBehavior(behavior).build()
        .create(VersatileApi::class.java)
    private val service = MockVersatileApi(delegate)
    private val model = SnsModel(service)

    private lateinit var viewmodel: SnsViewModel
    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", "", "", "dummy_user_id", "", ""),
        SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id2", "", "")
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_description", "dummy_name"),
        SnsUser("dummy_user_id2", "dummy_description2", "dummy_name2"),
    )
    private val dummyCurrentUserId = dummyUsers[0].id

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

    @Test
    fun init_success() {
        setUpService(true)

        viewmodel = SnsViewModel(model, 1, 1)
        assertEquals(emptyList<List<SnsContent>>(), viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun init_failure() {
        setUpService(false)

        viewmodel = SnsViewModel(model, 1, 1)
        assertEquals(emptyList<List<SnsContent>>(), viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(emptyList<SnsContent>(), viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_success() {
        // 開始時点ではユーザーの情報は1人分しか読み込まれないようにする。
        // viewmodelを初期化した時点でユーザーの情報が読み込まれる。
        setUpService(true, 1)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない限りは1人分の情報しか存在せず、1人分の投稿しか読み込まれない。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value)

        // refreshすると、ユーザーの情報および投稿がすべて読み込まれる。
        viewmodel.refresh()
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(true, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_failure() {
        // 開始時点ではユーザーの情報は1人分しか読み込まれないようにする。
        // viewmodelを初期化した時点でユーザーの情報が読み込まれる。
        setUpService(true, 1)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない限りは1人分の情報しか存在せず、1人分の投稿しか読み込まれない。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value)

        // refreshを失敗させる。タイムラインはrefresh前のまま。
        setUpService(false)
        viewmodel.refresh()
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(true, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_success() {
        setUpService(true)

        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        viewmodel.loadMore()
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun loadmore_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        viewmodel.loadMore()
        val expectedBeforeLoadMore = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(expectedBeforeLoadMore, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun updateCurrentUser_success() {
        setUpService(true)

        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        viewmodel.updateCurrentUser(dummyCurrentUserId)
        assertNull(viewmodel.currentUser.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(dummyUsers.find { it.id == dummyCurrentUserId }, viewmodel.currentUser.value)
    }

    @Test
    fun updateCurrentUser_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        viewmodel.updateCurrentUser(dummyCurrentUserId)
        assertNull(viewmodel.currentUser.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(viewmodel.currentUser.value)
    }

    @Test
    fun sendSnsPost_success() {
        setUpService(true)

        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val content = "dummy_text%s".format(dummyTimeline.size + 1)
        viewmodel.sendSnsPost(content)
        val expectedClientTimeline = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        assertEquals(dummyTimeline, service.allTimeline)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expectedServerTimeline = dummyTimeline.plus(
            SnsContentInternal(
                "dummy_content_id%s".format(dummyTimeline.size + 1),
                content,
                "",
                "",
                dummyCurrentUserId,
                "",
                ""
            )
        )
        // クライアント側は timeline を load してないのでそのまま
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)

        // サーバー側には投稿が追加される
        assertEquals(expectedServerTimeline, service.allTimeline)
    }

    @Test
    fun sendSnsPost_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        val content = "dummy_text%s".format(dummyTimeline.size + 1)
        viewmodel.sendSnsPost(content)
        val expectedClientTimeline = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        assertEquals(dummyTimeline, service.allTimeline)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // 投稿に失敗するので、クライアントとサーバーの両方とも変化なし
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        assertEquals(dummyTimeline, service.allTimeline)
    }

    @Test
    fun updateUserSetting_success() {
        setUpService(true)

        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)
        viewmodel.updateUserSetting(name, description)
        val expected = SnsUser(dummyCurrentUserId, description, name)
        assertNull(viewmodel.currentUser.value)
        assertEquals(false, service.allUsers?.contains(expected))
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(expected, viewmodel.currentUser.value)
        assertEquals(true, service.allUsers?.contains(expected))
    }

    @Test
    fun updateUserSetting_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        viewmodel = SnsViewModel(model, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)
        viewmodel.updateUserSetting(name, description)
        val expected = SnsUser(dummyCurrentUserId, description, name)
        assertNull(viewmodel.currentUser.value)
        assertEquals(false, service.allUsers?.contains(expected))
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(viewmodel.currentUser.value)
        assertEquals(false, service.allUsers?.contains(expected))
    }
}

package com.example.programmersnsandroidclient

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.sns.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
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

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val userDao = mock(UserDao::class.java)
    private val repository =
        SnsRepository(service, appContext, userDao, shouldUseFullIdAsUnregisteredUserName = true)

    private lateinit var viewmodel: SnsViewModel
    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", "", "", "dummy_user_id", "", ""),
        SnsContentInternal("dummy_content_id2", "dummy_text2", "", "", "dummy_user_id2", "", "")
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

        viewmodel = SnsViewModel(repository, 1, 1)
        assertEquals(emptyList<List<SnsContent>>(), viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(dummyCurrentUser, viewmodel.currentUser.value)
        assertEquals(expected, viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun init_failure() {
        setUpService(false)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        viewmodel = SnsViewModel(repository, 1, 1)
        assertEquals(emptyList<List<SnsContent>>(), viewmodel.timeline.value)
        assertEquals(true, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(viewmodel.currentUser.value)
        assertEquals(emptyList<SnsContent>(), viewmodel.timeline.value)
        assertEquals(false, viewmodel.isLoading.value)
        assertEquals(false, viewmodel.isRefreshing.value)
    }

    @Test
    fun refresh_success() {
        // 開始時点ではユーザーの情報は1人分しか読み込まれないようにする。
        // viewmodelを初期化した時点でユーザーの情報が読み込まれる。
        setUpService(true, 1)
        setUpUserDao(1)
        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない場合、読み込まれてないユーザーは未登録ユーザーとして扱われる。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value)

        // refreshすると、ユーザーの情報および投稿がすべて読み込まれる。
        setUpUserDao()  // ユーザーが読み込まれるタイミングで UserDao を更新
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
        setUpUserDao(1)
        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // ユーザーの情報が読み込まれた後で、ユーザーの情報を増やす。
        // refreshしない場合、読み込まれてないユーザーは未登録ユーザーとして扱われる。
        // loadMoreを実行することで、投稿の数の上限を2にする。
        setUpService(true)
        viewmodel.loadMore()
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val expectedBeforeRefresh = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expectedBeforeRefresh, viewmodel.timeline.value)

        // refreshを失敗させる。タイムラインはrefresh前のまま。
        setUpService(false)
        setUpUserDao()  // refresh のタイミングで UserDao を更新
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
        setUpUserDao()

        viewmodel = SnsViewModel(repository, 1, 1)
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
        setUpUserDao()
        viewmodel = SnsViewModel(repository, 1, 1)
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
    fun sendSnsPost_success() {
        setUpService(true)
        setUpUserDao()

        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        val content = "dummy_text%s".format(dummyTimeline.size + 1)
        viewmodel.sendSnsPost(content)
        val expectedClientTimeline = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // クライアント側は timeline を load してないのでそのまま
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)

        // サーバーへの投稿は成功
        assertEquals(true, viewmodel.sendSuccessful.value)
    }

    @Test
    fun sendSnsPost_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        setUpUserDao()
        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // それ以降は失敗
        setUpService(false)
        val content = "dummy_text%s".format(dummyTimeline.size + 1)
        viewmodel.sendSnsPost(content)
        val expectedClientTimeline = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
        )
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        // 投稿に失敗するので、クライアントとサーバーの両方とも変化なし
        assertEquals(expectedClientTimeline, viewmodel.timeline.value)
        assertEquals(false, viewmodel.sendSuccessful.value)
    }

    @Test
    fun updateUserProfile_success() {
        setUpService(true)
        setUpUserDao()

        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(repository.loadCurrentUserId())

        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)
        viewmodel.updateUserProfile(name, description)

        val expected = SnsUser(dummyCurrentUserId, name, description)
        assertNull(viewmodel.currentUser.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(expected, viewmodel.currentUser.value)
        assertEquals(true, viewmodel.updateSuccessful.value)
        assertEquals(expected.id, repository.loadCurrentUserId())
    }

    @Test
    fun updateUserProfile_failure() {
        // viewmodel の初期化は成功させる
        setUpService(true)
        setUpUserDao()
        viewmodel = SnsViewModel(repository, 1, 1)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(repository.loadCurrentUserId())

        // それ以降は失敗
        setUpService(false)
        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)
        viewmodel.updateUserProfile(name, description)

        assertNull(viewmodel.currentUser.value)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(viewmodel.currentUser.value)
        assertEquals(false, viewmodel.updateSuccessful.value)
        assertNull(repository.loadCurrentUserId())
    }
}

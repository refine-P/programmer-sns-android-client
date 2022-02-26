package com.example.programmersnsandroidclient

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.example.programmersnsandroidclient.viewmodel.SnsUserViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
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
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SnsUserViewModelTest {
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

    private lateinit var viewmodel: SnsUserViewModel
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

        viewmodel = SnsUserViewModel(repository, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertEquals(dummyCurrentUser, viewmodel.currentUser.value)
    }

    @Test
    fun init_failure() {
        setUpService(false)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        viewmodel = SnsUserViewModel(repository, Dispatchers.IO)
        Thread.sleep(DELAY_FOR_LIVEDATA_MILLIS)

        assertNull(viewmodel.currentUser.value)
    }

    @Test
    fun updateUserProfile_success() {
        setUpService(true)
        setUpUserDao()

        viewmodel = SnsUserViewModel(repository, Dispatchers.IO)
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
        viewmodel = SnsUserViewModel(repository, Dispatchers.IO)
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

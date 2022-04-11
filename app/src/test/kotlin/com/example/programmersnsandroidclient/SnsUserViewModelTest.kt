package com.example.programmersnsandroidclient

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.example.programmersnsandroidclient.viewmodel.SnsUserViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
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

// TODO: SnsRepositoryTest と同様にネットワーク障害の場合のテストを足すかどうか検討する
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SnsUserViewModelTest {
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
    private val dispatcher = StandardTestDispatcher()
    private val repository =
        SnsRepository(
            service,
            appContext,
            userDao,
            shouldUseFullIdAsUnregisteredUserName = true,
            dispatcher
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

    private fun setUpUserDao(userNum: Int = dummyUsers.size) {
        for (user in dummyUsers.take(userNum)) {
            `when`(userDao.getUser(user.id)).thenReturn(user)
        }
    }

    @Test
    fun init_success() = runTest(dispatcher) {
        setUpService(true)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        val viewmodel = SnsUserViewModel(repository, dispatcher, true)
        viewmodel.init().join()

        assertEquals(dummyCurrentUser, viewmodel.currentUser.value)
    }

    @Test
    fun init_failure() = runTest(dispatcher) {
        setUpService(false)
        setUpUserDao()
        repository.storeCurrentUserId(dummyCurrentUserId)

        val viewmodel = SnsUserViewModel(repository, dispatcher, true)
        viewmodel.init().join()

        assertNull(viewmodel.currentUser.value)
    }

    @Test
    fun updateUserProfile_success() = runTest(dispatcher) {
        setUpService(true)
        setUpUserDao()

        val viewmodel = SnsUserViewModel(repository, dispatcher, true)
        viewmodel.init().join()

        assertNull(repository.loadCurrentUserId())

        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)

        assertNull(viewmodel.currentUser.value)

        viewmodel.updateUserProfile(name, description).join()

        val expected = SnsUser(dummyCurrentUserId, name, description)
        assertEquals(expected, viewmodel.currentUser.value)
        assertEquals(true, viewmodel.updateSuccessful.value)
        assertEquals(expected.id, repository.loadCurrentUserId())
    }

    @Test
    fun updateUserProfile_failure() = runTest(dispatcher) {
        // viewmodel の初期化は成功させる
        setUpService(true)
        setUpUserDao()
        val viewmodel = SnsUserViewModel(repository, dispatcher, true)
        viewmodel.init().join()

        assertNull(repository.loadCurrentUserId())

        // それ以降は失敗
        setUpService(false)
        val name = "dummy_name%s".format(dummyUsers.size + 1)
        val description = "dummy_text%s".format(dummyUsers.size + 1)

        assertNull(viewmodel.currentUser.value)

        viewmodel.updateUserProfile(name, description).join()

        assertNull(viewmodel.currentUser.value)
        assertEquals(false, viewmodel.updateSuccessful.value)
        assertNull(repository.loadCurrentUserId())
    }
}

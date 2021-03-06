package com.example.programmersnsandroidclient

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SnsRepositoryTest {
    enum class NetworkState {
        SUCCESS,
        HTTP_ERROR,
        NETWORK_FAILURE,
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://versatileapi.herokuapp.com/api/")
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(
                    KotlinJsonAdapterFactory()
                ).build()
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
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_name", "dummy_description"),
    )
    private val dummyCurrentUser = dummyUsers[0]
    private val dummyCurrentUserId = dummyCurrentUser.id

    private fun setUpService(state: NetworkState) {
        behavior.apply {
            setDelay(0, TimeUnit.MILLISECONDS) // ????????????????????????????????????????????????
            setVariancePercent(0)
            setFailurePercent(
                if (state == NetworkState.NETWORK_FAILURE) {
                    100
                } else {
                    0
                }
            )
            setErrorPercent(
                if (state == NetworkState.HTTP_ERROR) {
                    100
                } else {
                    0
                }
            )
        }

        service.allTimeline = dummyTimeline
        service.allUsers = dummyUsers
        service.currentUserId = dummyCurrentUserId
    }

    @Test
    fun fetchTimeline_success() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        // refresh???????????????UserCache?????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actual = repository.fetchTimeline(1, true)

        verify(userDao, times(1)).insertUsers(dummyUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_HttpError() = runTest(dispatcher) {
        setUpService(NetworkState.HTTP_ERROR)

        // refresh???????????????UserCache?????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actual = repository.fetchTimeline(1, true)

        verify(userDao, times(0)).insertUsers(dummyUsers)
        verify(userDao, times(0)).getUser(dummyCurrentUserId)
        assertNull(actual)
    }

    @Test
    fun fetchTimeline_NetworkFailure() = runTest(dispatcher) {
        setUpService(NetworkState.NETWORK_FAILURE)

        // refresh???????????????UserCache?????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actual = repository.fetchTimeline(1, true)

        verify(userDao, times(0)).insertUsers(dummyUsers)
        verify(userDao, times(0)).getUser(dummyCurrentUserId)
        assertNull(actual)
    }

    @Test
    fun fetchTimeline_newUserAndContentAdded_refresh() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        // ?????????UserCache???refresh????????????????????????
        // ????????????UserCache?????????????????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        // User???Content??????????????????
        service.allTimeline = dummyTimeline.plus(
            SnsContentInternal(
                "dummy_content_id2",
                "dummy_text2",
                null,
                null,
                "dummy_user_id2",
                "",
                ""
            )
        )
        val newUser = SnsUser("dummy_user_id2", "dummy_name2", "dummy_description2")
        val latestUsers = dummyUsers.plus(newUser)
        service.allUsers = latestUsers

        // refresh???????????????UserCache??????????????????
        `when`(userDao.getUser(newUser.id)).thenReturn(newUser)

        // UserCache???refresh???????????????????????????User???????????????Content?????????????????????
        val actual = repository.fetchTimeline(2, true)

        verify(userDao, times(1)).insertUsers(latestUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        verify(userDao, times(1)).getUser(newUser.id)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_newUserAndContentAdded_notRefresh() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        // ?????????UserCache???refresh????????????????????????
        // ????????????UserCache?????????????????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        // User???Content??????????????????
        service.allTimeline = dummyTimeline.plus(
            SnsContentInternal(
                "dummy_content_id2",
                "dummy_text2",
                null,
                null,
                "dummy_user_id2",
                "",
                ""
            )
        )
        val newUser = SnsUser("dummy_user_id2", "dummy_name2", "dummy_description2")
        val latestUsers = dummyUsers.plus(newUser)
        service.allUsers = latestUsers

        // UserCache???refresh???????????????
        // UserCache????????????User???????????????????????????????????????????????????????????????Content????????????????????????
        val actual = repository.fetchTimeline(2, false)

        verify(userDao, times(0)).insertUsers(latestUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        verify(userDao, times(1)).getUser(newUser.id)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_user_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_user_id2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchUser_success() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        val actual = repository.fetchUser("dummy_user_id")

        val expected = SnsUser("dummy_user_id", "dummy_name", "dummy_description")
        assertEquals(expected, actual)
    }

    @Test
    fun fetchUser_HttpError() = runTest(dispatcher) {
        setUpService(NetworkState.HTTP_ERROR)

        val actual = repository.fetchUser("dummy_user_id")

        assertNull(actual)
    }

    @Test
    fun fetchUser_NetworkFailure() = runTest(dispatcher) {
        setUpService(NetworkState.NETWORK_FAILURE)

        val actual = repository.fetchUser("dummy_user_id")

        assertNull(actual)
    }

    @Test
    fun sendSnsPost_success() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        val isSuccessful = repository.sendSnsPost("dummy_text2")

        assertTrue(isSuccessful)
    }

    @Test
    fun sendSnsPost_HttpError() = runTest(dispatcher) {
        setUpService(NetworkState.HTTP_ERROR)

        val isSuccessful = repository.sendSnsPost("dummy_text2")

        assertFalse(isSuccessful)
    }

    @Test
    fun sendSnsPost_NetworkFailure() = runTest(dispatcher) {
        setUpService(NetworkState.NETWORK_FAILURE)

        val isSuccessful = repository.sendSnsPost("dummy_text2")

        assertFalse(isSuccessful)
    }

    @Test
    fun updateUser_success() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)

        val actualUserId = repository.updateUser("dummy_name2", "dummy_description2")

        assertEquals(dummyCurrentUserId, actualUserId)
    }

    @Test
    fun updateUser_HttpError() = runTest(dispatcher) {
        setUpService(NetworkState.HTTP_ERROR)

        val actualUserId = repository.updateUser("dummy_name2", "dummy_description2")

        assertNull(actualUserId)
    }

    @Test
    fun updateUser_NetworkFailure() = runTest(dispatcher) {
        setUpService(NetworkState.NETWORK_FAILURE)

        val actualUserId = repository.updateUser("dummy_name2", "dummy_description2")

        assertNull(actualUserId)
    }

    @Test
    fun loadUserFromCache_registeredUser() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)
        // ?????????UserCache???refresh????????????????????????
        // ????????????UserCache?????????????????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actualUser = repository.loadUserFromCache(dummyCurrentUserId)

        assertEquals(dummyCurrentUser, actualUser)
    }

    @Test
    fun loadUserFromCache_unregisteredUser() = runTest(dispatcher) {
        setUpService(NetworkState.SUCCESS)
        // ?????????UserCache???refresh????????????????????????
        // ????????????UserCache?????????????????????
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val unregisteredUserId = "unregistered_user_id"
        val actualUser = repository.loadUserFromCache(unregisteredUserId)

        val expectedUser = SnsUser(
            unregisteredUserId,
            unregisteredUserId,
            ""
        )
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun currentUserId() {
        assertNull(repository.loadCurrentUserId())
        val userId = "dummy_user_id"
        repository.storeCurrentUserId(userId)
        assertEquals(userId, repository.loadCurrentUserId())
    }
}

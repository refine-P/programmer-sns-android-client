package com.example.programmersnsandroidclient

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.programmersnsandroidclient.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SnsRepositoryTest {
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
    private val repository =
        SnsRepository(
            service,
            appContext,
            userDao,
            shouldUseFullIdAsUnregisteredUserName = true,
            Dispatchers.IO
        )

    private val dummyTimeline = listOf(
        SnsContentInternal("dummy_content_id", "dummy_text", null, null, "dummy_user_id", "", ""),
    )
    private val dummyUsers = listOf(
        SnsUser("dummy_user_id", "dummy_name", "dummy_description"),
    )
    private val dummyCurrentUser = dummyUsers[0]
    private val dummyCurrentUserId = dummyCurrentUser.id

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

        // refreshされた後のUserCacheを定義
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actual = runBlocking {
            repository.fetchTimeline(1, true)
        }

        verify(userDao, times(1)).insertUsers(dummyUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_failure() {
        setUpService(false)

        // refreshされた後のUserCacheを定義
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        val actual = runBlocking {
            repository.fetchTimeline(1, true)
        }

        verify(userDao, times(0)).insertUsers(dummyUsers)
        verify(userDao, times(0)).getUser(dummyCurrentUserId)
        assertNull(actual)
    }

    @Test
    fun fetchTimeline_newUserAndContentAdded_refresh() {
        setUpService(true)

        // 事前にUserCacheをrefreshしたものとする。
        // その際のUserCacheをここに定義。
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        // UserとContentが追加される
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

        // refreshされた後のUserCacheの差分を定義
        `when`(userDao.getUser(newUser.id)).thenReturn(newUser)

        // UserCacheをrefreshすることで、新しいUserが投稿したContentが取得される。
        val actual = runBlocking {
            repository.fetchTimeline(2, true)
        }

        verify(userDao, times(1)).insertUsers(latestUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        verify(userDao, times(1)).getUser(newUser.id)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_name2", "dummy_text2")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fetchTimeline_newUserAndContentAdded_notRefresh() {
        setUpService(true)

        // 事前にUserCacheをrefreshしたものとする。
        // その際のUserCacheをここに定義。
        `when`(userDao.getUser(dummyCurrentUserId)).thenReturn(dummyCurrentUser)

        // UserとContentが追加される
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

        // UserCacheをrefreshしない場合
        // UserCacheに新しいUserが存在しないので、未登録ユーザーが投稿したContentとして扱われる。
        val actual = runBlocking {
            repository.fetchTimeline(2, false)
        }

        verify(userDao, times(0)).insertUsers(latestUsers)
        verify(userDao, times(1)).getUser(dummyCurrentUserId)
        verify(userDao, times(1)).getUser(newUser.id)
        val expected = listOf(
            SnsContent("dummy_content_id", "dummy_name", "dummy_text"),
            SnsContent("dummy_content_id2", "dummy_user_id2", "dummy_text2")
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

        val isSuccessful = runBlocking {
            repository.sendSnsPost("dummy_text2")
        }

        assertTrue(isSuccessful)
    }

    @Test
    fun sendSnsPost_failure() {
        setUpService(false)

        val isSuccessful = runBlocking {
            repository.sendSnsPost("dummy_text2")
        }

        assertFalse(isSuccessful)
    }

    @Test
    fun updateUser_success() {
        setUpService(true)

        val actualUserId = runBlocking {
            repository.updateUser("dummy_name2", "dummy_description2")
        }

        assertEquals(dummyCurrentUserId, actualUserId)
    }

    @Test
    fun updateUser_failure() {
        setUpService(false)

        val actualUserId = runBlocking {
            repository.updateUser("dummy_name2", "dummy_description2")
        }

        assertNull(actualUserId)
    }

    @Test
    fun currentUserId() {
        assertNull(repository.loadCurrentUserId())
        val userId = "dummy_user_id"
        repository.storeCurrentUserId(userId)
        assertEquals(userId, repository.loadCurrentUserId())
    }
}

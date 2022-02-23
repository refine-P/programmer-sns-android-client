package com.example.programmersnsandroidclient.di

import android.content.Context
import androidx.room.Room
import com.example.programmersnsandroidclient.model.UserDao
import com.example.programmersnsandroidclient.model.UserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Singleton
    @Provides
    fun provideUserDao(@ApplicationContext appContext: Context): UserDao {
        return Room.databaseBuilder(
            appContext, UserDatabase::class.java, "user-cache"
        ).build().userDao()
    }
}
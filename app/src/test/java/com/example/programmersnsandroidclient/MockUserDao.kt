package com.example.programmersnsandroidclient

import com.example.programmersnsandroidclient.sns.SnsUser
import com.example.programmersnsandroidclient.sns.UserDao

class MockUserDao : UserDao {
    private val userCache: HashMap<String, SnsUser> = hashMapOf()

    override fun getUser(id: String): SnsUser? {
        return userCache[id]
    }

    override fun insertUsers(users: List<SnsUser>) {
        for (user in users) {
            userCache[user.id] = user
        }
    }
}
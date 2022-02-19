package com.example.programmersnsandroidclient

import android.app.Application
import android.content.Context

// TODO: Hilt か何かを使って applicationContext をよしなにできる方が良さそう？　
// 以下の実装を参考にした
// https://github.com/sEmoto0808/kt-Meditation-demo/blob/master/Meditation/app/src/main/java/com/example/semoto/meditation/MyApplication.kt
class ProgrammerSns : Application() {
    override fun onCreate() {
        super.onCreate()

        appContext = this
    }

    companion object {
        lateinit var appContext: Context
    }
}
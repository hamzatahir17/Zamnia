package com.zamnia.quizapp

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zamnia.quizapp.data.local.ZamniaDatabase
import com.zamnia.quizapp.data.remote.SupabaseService
import com.zamnia.quizapp.data.repository.ZamniaRepository
import com.zamnia.quizapp.util.NetworkObserver
import com.zamnia.quizapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Zamnia Backend Engine (Production Ready)
 * Handles initialization of Room, Supabase, and Repositories.
 */
object ZamniaEngine {
    lateinit var supabase: SupabaseClient
        private set
    lateinit var repository: ZamniaRepository
        private set
    lateinit var networkObserver: NetworkObserver
        private set

    fun initialize(context: Context) {
        val database = Room.databaseBuilder(
            context.applicationContext,
            ZamniaDatabase::class.java,
            "zamnia_db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON;")
            }
        })
        .fallbackToDestructiveMigration()
        .build()

        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }

        val supabaseService = SupabaseService(supabase)
        
        repository = ZamniaRepository(
            supabase = supabaseService,
            database = database,
            packageDao = database.packageDao(),
            quizDao = database.quizDao(),
            userDao = database.userDao(),
            userPrefsDao = database.userPrefsDao()
        )

        networkObserver = NetworkObserver(context)
    }
}

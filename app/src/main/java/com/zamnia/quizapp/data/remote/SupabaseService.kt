package com.zamnia.quizapp.data.remote

import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.data.model.Question
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.Pack
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseService(private val client: SupabaseClient) {

    fun getUserProfileStream(uid: String): Flow<User?> = callbackFlow {
        val channelTopic = "user_${uid}_${System.currentTimeMillis()}"
        val channel = client.realtime.channel(channelTopic)
        
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
            filter("uid", FilterOperator.EQ, uid)
        }

        val jsonDecoder = Json {
            ignoreUnknownKeys = true 
            coerceInputValues = true 
        }

        val job = launch {
            changeFlow.collect { action ->
                Log.d("SupabaseService", "Realtime PostgresAction received on $channelTopic: $action")
                val remoteUser = try {
                    when (action) {
                        is PostgresAction.Update -> jsonDecoder.decodeFromJsonElement(User.serializer(), action.record)
                        is PostgresAction.Insert -> jsonDecoder.decodeFromJsonElement(User.serializer(), action.record)
                        is PostgresAction.Delete -> {
                            Log.w("SupabaseService", "User record deleted in Realtime for $uid")
                            null
                        }
                        else -> getUserProfile(uid)
                    }
                } catch (e: Exception) {
                    Log.w("SupabaseService", "Error decoding realtime record, fetching via Postgrest: ${e.message}")
                    getUserProfile(uid)
                }

                trySend(remoteUser)
            }
        }

        // Subscribe channel IMMEDIATELY in parallel so no Realtime events are missed
        launch {
            try {
                client.realtime.connect()
                channel.subscribe()
                Log.d("SupabaseService", "Subscribed to realtime channel $channelTopic for UID $uid")
            } catch (e: Exception) {
                Log.e("SupabaseService", "Failed to subscribe to realtime channel $channelTopic: ${e.message}")
            }
        }

        // Initial fetch in parallel
        launch {
            try {
                val initialUser = getUserProfile(uid)
                if (initialUser != null) {
                    trySend(initialUser)
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "Initial fetch profile error: ${e.message}")
            }
        }

        awaitClose {
            job.cancel()
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    channel.unsubscribe()
                    Log.d("SupabaseService", "Unsubscribed realtime channel $channelTopic")
                } catch (e: Exception) {
                    Log.e("SupabaseService", "Error unsubscribing channel $channelTopic: ${e.message}")
                }
            }
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        val list = client.postgrest["users"].select {
            filter {
                eq("uid", uid)
            }
        }.decodeList<User>()
        return list.firstOrNull()
    }

    suspend fun saveUserProfile(user: User) {
        client.postgrest["users"].upsert(user)
    }

    suspend fun getAvailablePacks(classLevel: Int): List<Pack> {
        return client.postgrest["packs"].select {
            filter {
                eq("class_level", classLevel)
            }
            order("index_order", Order.ASCENDING)
        }.decodeList<Pack>()
    }

    suspend fun getAvailablePacksForAllClasses(): Result<List<Pack>> {
        return try {
            val packs = client.postgrest["packs"].select {
                order("index_order", Order.ASCENDING)
            }.decodeList<Pack>()
            Result.success(packs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByPublicId(publicId: String): User? {
        return try {
            val list = client.postgrest["users"].select {
                filter {
                    eq("user_id", publicId)
                }
            }.decodeList<User>()
            list.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isPublicIdUnique(publicId: String): Boolean {
        return try {
            val list = client.postgrest["users"].select(columns = Columns.list("user_id")) {
                filter {
                    eq("user_id", publicId)
                }
            }.decodeList<User>()
            list.isEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getQuestionsByPackage(packageId: String): List<Question> {
        return try {
            val list = client.postgrest["mcqs"].select {
                filter {
                    eq("package_id", packageId)
                }
            }.decodeList<Question>()
            Log.d("SupabaseService", "getQuestionsByPackage('$packageId') returned ${list.size} items from 'mcqs'")
            list
        } catch (e: Exception) {
            Log.e("SupabaseService", "getQuestionsByPackage('$packageId') error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getThemes(): List<Theme> {
        return try {
            client.postgrest["themes"].select().decodeList<Theme>()
        } catch (e: Exception) {
            Log.w("SupabaseService", "Themes table missing or query failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun purchaseTheme(uid: String, themeId: String, price: Long): Result<Unit> {
        return try {
            val user = getUserProfile(uid) ?: return Result.failure(Exception("User not found"))
            val currentBalance = user.coinBalance ?: 0L
            val unlockedList = user.unlockedThemes.orEmpty().toMutableList()
            if (!unlockedList.contains("default")) {
                unlockedList.add("default")
            }

            // If already unlocked, just select it
            if (unlockedList.contains(themeId)) {
                return selectTheme(uid, themeId)
            }

            if (currentBalance < price) {
                return Result.failure(Exception("Insufficient coins. Required: $price coins"))
            }

            if (!unlockedList.contains(themeId)) {
                unlockedList.add(themeId)
            }

            val newBalance = currentBalance - price

            client.postgrest["users"].update(
                buildJsonObject {
                    put("coin_balance", newBalance)
                    put("active_theme_id", themeId)
                    put("unlocked_themes", Json.encodeToJsonElement(
                        ListSerializer(String.serializer()),
                        unlockedList
                    ))
                }
            ) {
                filter {
                    eq("uid", uid)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "purchaseTheme failed for $themeId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun selectTheme(uid: String, themeId: String): Result<Unit> {
        return try {
            val user = getUserProfile(uid) ?: return Result.failure(Exception("User not found"))
            val unlockedList = user.unlockedThemes.orEmpty().toMutableList()
            if (!unlockedList.contains("default")) {
                unlockedList.add("default")
            }

            val isUnlocked = unlockedList.contains(themeId) || themeId == "default"

            if (!isUnlocked) {
                return Result.failure(Exception("Theme '$themeId' is locked. Please purchase it first."))
            }

            if (!unlockedList.contains(themeId)) {
                unlockedList.add(themeId)
            }

            client.postgrest["users"].update(
                buildJsonObject {
                    put("active_theme_id", themeId)
                    put("unlocked_themes", Json.encodeToJsonElement(
                        ListSerializer(String.serializer()),
                        unlockedList
                    ))
                }
            ) {
                filter {
                    eq("uid", uid)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "selectTheme failed for $themeId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun migrateGuestUserRpc(guestUid: String) {
        try {
            client.postgrest.rpc(
                function = "migrate_guest_user",
                parameters = buildJsonObject {
                    put("p_guest_uid", guestUid)
                }
            )
            Log.d("SupabaseService", "Migrated guest $guestUid via RPC")
        } catch (e: Exception) {
            Log.e("SupabaseService", "migrateGuestUserRpc error: ${e.message}", e)
            deleteUser(guestUid)
        }
    }

    suspend fun deleteUser(uid: String) {
        try {
            client.postgrest["users"].delete {
                filter {
                    eq("uid", uid)
                }
            }
            Log.d("SupabaseService", "Deleted user $uid from remote Supabase public.users table")
        } catch (e: Exception) {
            Log.w("SupabaseService", "Error deleting remote user $uid: ${e.message}")
        }
    }

    suspend fun updateQuizCoins(uid: String, isCorrect: Boolean) {
        val user = getUserProfile(uid) ?: return
        val currentCoins = user.coinBalance ?: 0L
        val updatedCoins = if (isCorrect) currentCoins + 10 else (currentCoins - 5).coerceAtLeast(0)
        
        saveUserProfile(user.copy(coinBalance = updatedCoins))
    }

    suspend fun transferCoinsRpc(toPublicId: String, amount: Long): String {
        return try {
            val result = client.postgrest.rpc(
                function = "transfer_coins",
                parameters = buildJsonObject {
                    put("to_public_id", toPublicId)
                    put("transfer_amount", amount)
                }
            )
            val rawData = result.data.trim().removeSurrounding("\"")
            Log.d("SupabaseService", "transferCoinsRpc response raw: '${result.data}', clean: '$rawData'")
            rawData
        } catch (e: Exception) {
            Log.e("SupabaseService", "transferCoinsRpc error: ${e.message}", e)
            "ERROR: ${e.localizedMessage}"
        }
    }

    suspend fun getDailyTransferCount(uid: String): Int {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val countStr = client.postgrest.rpc(
                function = "get_daily_transfer_count",
                parameters = buildJsonObject {
                    put("p_sender_id", uid)
                    put("p_date", today)
                }
            ).data
            countStr.trim().toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

package com.zamnia.quizapp.data.remote

import com.zamnia.quizapp.data.model.User
import com.zamnia.quizapp.data.model.Question
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.Pack
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import android.util.Log
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseService(private val client: SupabaseClient) {

    fun getUserProfileStream(uid: String): Flow<User?> {
        val channel = client.realtime.channel("public:users")
        return channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "users"
        }.map {
            getUserProfile(uid)
        }.onStart {
            emit(getUserProfile(uid))
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        return try {
            client.postgrest["users"].select {
                filter {
                    eq("uid", uid)
                }
            }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error getting user profile: ${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(user: User) {
        try {
            client.postgrest["users"].upsert(user)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error saving user profile: ${e.message}")
        }
    }

    suspend fun getAvailablePacks(classLevel: Int): List<Pack> {
        return try {
            client.postgrest["packs"].select {
                filter {
                    eq("class_level", classLevel)
                }
            }.decodeList<Pack>()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error getting packs: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAvailablePacksForAllClasses(): Result<List<Pack>> {
        return try {
            val list = client.postgrest["packs"].select().decodeList<Pack>()
            Result.success(list)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error getting all packs: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserByPublicId(publicId: String): User? {
        return try {
            client.postgrest["users"].select {
                filter {
                    eq("userId", publicId)
                }
            }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isPublicIdUnique(publicId: String): Boolean {
        return try {
            val response = client.postgrest["users"].select(columns = Columns.list("user_id")) {
                filter {
                    eq("user_id", publicId)
                }
            }
            response.data == "[]"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getQuestions(): List<Question> {
        return try {
            client.postgrest["mcqs"].select().decodeList<Question>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getQuestionsByPackage(packageId: String): List<Question> {
        return try {
            // Assuming we have a package_id or similar field in mcqs table in Supabase
            // If not, we can filter by subject/chapter or just return all for now
            client.postgrest["mcqs"].select {
                filter {
                    eq("package_id", packageId)
                }
            }.decodeList<Question>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getThemes(): List<Theme> {
        return try {
            client.postgrest["themes"].select().decodeList<Theme>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun purchaseTheme(uid: String, themeId: String, price: Long): Result<Unit> {
        return try {
            client.postgrest.rpc("purchase_theme", buildJsonObject {
                put("user_uid", uid)
                put("theme_id", themeId)
                put("theme_price", price)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuizCoins(uid: String, isCorrect: Boolean) {
        val amount = if (isCorrect) 10L else -5L
        try {
            client.postgrest.rpc("update_user_coins", buildJsonObject {
                put("user_uid", uid)
                put("amount_to_add", amount)
            })
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error updating coins: ${e.message}")
        }
    }

    suspend fun transferCoins(fromUid: String, toPublicId: String, amount: Long): Result<Unit> {
        return try {
            client.postgrest.rpc("transfer_coins", buildJsonObject {
                put("from_uid", fromUid)
                put("to_public_id", toPublicId)
                put("amount_to_transfer", amount)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.api.GeminiApiClient
import com.example.data.database.BreviaryDatabase
import com.example.data.database.CachedPrayer
import com.example.data.database.PrayerStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BreviaryRepository(private val context: Context) {

    private val db = BreviaryDatabase.getInstance(context)
    private val dao = db.dao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val prayerAdapter = moshi.adapter(LiturgicalPrayer::class.java)

    fun useRomanCalendar(): Boolean {
        val prefs = context.getSharedPreferences("breviary_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("use_roman_calendar", false)
    }

    suspend fun getPrayer(dateString: String, hour: LiturgicalHour, forceRefresh: Boolean = false): Result<LiturgicalPrayer> = withContext(Dispatchers.IO) {
        val useRoman = useRomanCalendar()
        val suffix = if (useRoman) "_ROMAN" else "_DEVOTIONAL"
        val cacheId = "${dateString}_${hour.name}$suffix"
        
        try {
            // Check cache first
            if (!forceRefresh) {
                val cached = dao.getCachedPrayer(cacheId)
                if (cached != null) {
                    val prayer = prayerAdapter.fromJson(cached.prayerJson)
                    if (prayer != null) {
                        Log.d("BreviaryRepo", "Loaded cached prayer for $cacheId")
                        return@withContext Result.success(prayer)
                    }
                }
            }

            // Attempt to load from AI
            Log.d("BreviaryRepo", "Attempting block fetch from Gemini API for $cacheId")
            val aiPrayer = GeminiApiClient.fetchLiturgyFromAi(context, dateString, hour, useRomanCalendar = useRoman)
            
            // Save to Cache
            val jsonString = prayerAdapter.toJson(aiPrayer)
            dao.insertCachedPrayer(
                CachedPrayer(
                    id = cacheId,
                    date = dateString,
                    hour = hour.name,
                    liturgicalTitle = aiPrayer.season,
                    prayerJson = jsonString,
                    cachedAt = System.currentTimeMillis()
                )
            )
            Log.d("BreviaryRepo", "Successfully cached Gemini response for $cacheId")
            return@withContext Result.success(aiPrayer)

        } catch (e: Exception) {
            Log.e("BreviaryRepo", "Error in fetching or caching prayer, falling back to local generator", e)
            
            // Revert immediately to high-fulfillment local generator
            val localPrayer = LocalPrayerGenerator.generateLocalPrayer(dateString, hour, useRomanCalendar = useRoman)
            
            // Let's also cache the local generation so future reads are instantaneous
            try {
                val jsonString = prayerAdapter.toJson(localPrayer)
                dao.insertCachedPrayer(
                    CachedPrayer(
                        id = cacheId,
                        date = dateString,
                        hour = hour.name,
                        liturgicalTitle = localPrayer.season,
                        prayerJson = jsonString,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            } catch (cacheErr: Exception) {
                Log.e("BreviaryRepo", "Failed to cache local prayer", cacheErr)
            }
            
            return@withContext Result.success(localPrayer)
        }
    }

    suspend fun getPrayerStatus(dateString: String, hour: LiturgicalHour): PrayerStatus {
        val statusId = "${dateString}_${hour.name}"
        return dao.getPrayerStatus(statusId) ?: PrayerStatus(
            id = statusId,
            date = dateString,
            hour = hour.name,
            isCompleted = false,
            completedAt = 0L
        )
    }

    suspend fun getPrayerStatusesForDate(dateString: String): List<PrayerStatus> {
        return dao.getPrayerStatusesForDate(dateString)
    }

    suspend fun setPrayerCompleted(dateString: String, hour: LiturgicalHour, isCompleted: Boolean) {
        val statusId = "${dateString}_${hour.name}"
        val status = PrayerStatus(
            id = statusId,
            date = dateString,
            hour = hour.name,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else 0L
        )
        dao.insertPrayerStatus(status)
    }

    // Bookmark operations
    val allBookmarks: kotlinx.coroutines.flow.Flow<List<com.example.data.database.Bookmark>> = dao.getAllBookmarksFlow()

    suspend fun addBookmark(bookmark: com.example.data.database.Bookmark) = withContext(Dispatchers.IO) {
        dao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(id)
    }

    suspend fun removeBookmarkByDetails(date: String, hour: String, sectionType: String, sectionTitle: String) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkByDetails(date, hour, sectionType, sectionTitle)
    }

    suspend fun isBookmarked(date: String, hour: String, sectionType: String, sectionTitle: String): Boolean = withContext(Dispatchers.IO) {
        dao.isBookmarked(date, hour, sectionType, sectionTitle)
    }
}

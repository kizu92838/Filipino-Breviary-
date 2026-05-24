package com.example.data.database

import android.content.Context
import androidx.room.*

@Entity(tableName = "cached_prayers")
data class CachedPrayer(
    @PrimaryKey val id: String, // e.g. "2026-05-23_LAUDS"
    val date: String,
    val hour: String,
    val liturgicalTitle: String,
    val prayerJson: String,
    val cachedAt: Long
)

@Entity(tableName = "prayer_statuses")
data class PrayerStatus(
    @PrimaryKey val id: String, // "2026-05-23_LAUDS"
    val date: String,
    val hour: String,
    val isCompleted: Boolean,
    val completedAt: Long
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,             // e.g. "2026-05-23"
    val hour: String,             // e.g. "LAUDS"
    val sectionType: String,      // e.g. "Hymn", "Psalm", "Reading", "Canticle", "Concluding Prayer"
    val sectionTitle: String,     // e.g. "Psalm 63:2-9"
    val snippet: String,          // e.g. "O God, you are my God, for you I long..."
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BreviaryDao {
    @Query("SELECT * FROM cached_prayers WHERE id = :id")
    suspend fun getCachedPrayer(id: String): CachedPrayer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedPrayer(cachedPrayer: CachedPrayer)

    @Query("SELECT * FROM prayer_statuses WHERE id = :id")
    suspend fun getPrayerStatus(id: String): PrayerStatus?

    @Query("SELECT * FROM prayer_statuses WHERE date = :date")
    suspend fun getPrayerStatusesForDate(date: String): List<PrayerStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerStatus(status: PrayerStatus)

    // Bookmark Queries
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): kotlinx.coroutines.flow.Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarks(): List<Bookmark>

    @Query("SELECT * FROM prayer_statuses")
    suspend fun getAllPrayerStatuses(): List<PrayerStatus>

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    @Query("DELETE FROM prayer_statuses")
    suspend fun clearAllPrayerStatuses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE date = :date AND hour = :hour AND sectionType = :sectionType AND sectionTitle = :sectionTitle")
    suspend fun deleteBookmarkByDetails(date: String, hour: String, sectionType: String, sectionTitle: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE date = :date AND hour = :hour AND sectionType = :sectionType AND sectionTitle = :sectionTitle)")
    suspend fun isBookmarked(date: String, hour: String, sectionType: String, sectionTitle: String): Boolean
}

@Database(entities = [CachedPrayer::class, PrayerStatus::class, Bookmark::class], version = 2, exportSchema = false)
abstract class BreviaryDatabase : RoomDatabase() {
    abstract fun dao(): BreviaryDao

    companion object {
        @Volatile
        private var INSTANCE: BreviaryDatabase? = null

        fun getInstance(context: Context): BreviaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BreviaryDatabase::class.java,
                    "breviary_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

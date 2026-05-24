package com.example.util

import android.content.Context
import com.example.data.database.BreviaryDatabase
import com.example.data.database.Bookmark
import com.example.data.database.PrayerStatus
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

@JsonClass(generateAdapter = true)
data class BreviaryBackup(
    val bookmarks: List<Bookmark>,
    val prayerStatuses: List<PrayerStatus>,
    val useRomanCalendar: Boolean,
    val soloMode: Boolean,
    val simplifiedRubrics: Boolean,
    val activeLanguage: String
)

object BreviaryBackupManager {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(BreviaryBackup::class.java)

    suspend fun exportBackup(context: Context, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = BreviaryDatabase.getInstance(context)
            val dao = db.dao()

            val bookmarks = dao.getAllBookmarks()
            val prayerStatuses = dao.getAllPrayerStatuses()
            
            val prefs = context.getSharedPreferences("breviary_prefs", Context.MODE_PRIVATE)
            val useRomanCalendar = prefs.getBoolean("use_roman_calendar", false)
            val soloMode = prefs.getBoolean("solo_mode", false)
            val simplifiedRubrics = prefs.getBoolean("simplified_rubrics", false)
            val activeLanguage = prefs.getString("active_language", "ENGLISH") ?: "ENGLISH"

            val backup = BreviaryBackup(
                bookmarks = bookmarks,
                prayerStatuses = prayerStatuses,
                useRomanCalendar = useRomanCalendar,
                soloMode = soloMode,
                simplifiedRubrics = simplifiedRubrics,
                activeLanguage = activeLanguage
            )

            val jsonStr = backupAdapter.toJson(backup)
            outputStream.write(jsonStr.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importBackup(context: Context, inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBytes = inputStream.readBytes()
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            inputStream.close()

            val backup = backupAdapter.fromJson(jsonStr) ?: return@withContext false

            val db = BreviaryDatabase.getInstance(context)
            val dao = db.dao()

            // Clear and insert
            dao.clearAllBookmarks()
            backup.bookmarks.forEach { dao.insertBookmark(it) }

            dao.clearAllPrayerStatuses()
            backup.prayerStatuses.forEach { dao.insertPrayerStatus(it) }

            // Save prefs
            val prefs = context.getSharedPreferences("breviary_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("use_roman_calendar", backup.useRomanCalendar)
                putBoolean("solo_mode", backup.soloMode)
                putBoolean("simplified_rubrics", backup.simplifiedRubrics)
                putString("active_language", backup.activeLanguage)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

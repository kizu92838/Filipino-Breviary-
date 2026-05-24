package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.BreviaryRepository
import com.example.data.LiturgicalHour
import com.example.data.LiturgicalPrayer
import com.example.data.database.PrayerStatus
import com.example.util.BreviaryBackupManager
import com.example.util.PrayerSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

sealed interface PrayerUiState {
    object Loading : PrayerUiState
    data class Success(val prayer: LiturgicalPrayer, val isCompleted: Boolean) : PrayerUiState
    data class Error(val message: String) : PrayerUiState
}

class BreviaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BreviaryRepository(application)

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedHour = MutableStateFlow(LiturgicalHour.VESPERS)
    val selectedHour: StateFlow<LiturgicalHour> = _selectedHour.asStateFlow()

    private val _uiState = MutableStateFlow<PrayerUiState>(PrayerUiState.Loading)
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val _dailyStatus = MutableStateFlow<Map<LiturgicalHour, Boolean>>(emptyMap())
    val dailyStatus: StateFlow<Map<LiturgicalHour, Boolean>> = _dailyStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Preferences and Features States
    private val _useRomanCalendar = MutableStateFlow(false)
    val useRomanCalendar: StateFlow<Boolean> = _useRomanCalendar.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(false)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    private val _activeLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val activeLanguage: StateFlow<AppLanguage> = _activeLanguage.asStateFlow()

    private val _soloMode = MutableStateFlow(false)
    val soloMode: StateFlow<Boolean> = _soloMode.asStateFlow()

    private val _simplifiedRubrics = MutableStateFlow(false)
    val simplifiedRubrics: StateFlow<Boolean> = _simplifiedRubrics.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow("Not Configured")
    val apiKeyStatus: StateFlow<String> = _apiKeyStatus.asStateFlow()

    private val _isCheckingApiKey = MutableStateFlow(false)
    val isCheckingApiKey: StateFlow<Boolean> = _isCheckingApiKey.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _hideApiWarning = MutableStateFlow(false)
    val hideApiWarning: StateFlow<Boolean> = _hideApiWarning.asStateFlow()

    private val _showOnboardingGuide = MutableStateFlow(false)
    val showOnboardingGuide: StateFlow<Boolean> = _showOnboardingGuide.asStateFlow()

    // TTS Control States
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeSentenceText = MutableStateFlow<String?>(null)
    val activeSentenceText: StateFlow<String?> = _activeSentenceText.asStateFlow()

    private val _speechSpeed = MutableStateFlow(1.0f)
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private var speechManager: PrayerSpeechManager? = null

    // Reactive Bookmarks Flow
    val allBookmarks: StateFlow<List<com.example.data.database.Bookmark>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<com.example.data.database.Bookmark>()
        )

    init {
        // Load preferences
        val prefs = application.getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        _useRomanCalendar.value = prefs.getBoolean("use_roman_calendar", true) // Default true for CBCP calendar!
        _remindersEnabled.value = com.example.util.PrayerReminderScheduler.isRemindersEnabled(application)
        
        val langStr = prefs.getString("active_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        _activeLanguage.value = try { AppLanguage.valueOf(langStr) } catch(e: Exception) { AppLanguage.ENGLISH }
        _soloMode.value = prefs.getBoolean("solo_mode", false)
        _simplifiedRubrics.value = prefs.getBoolean("simplified_rubrics", false)

        val customKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        _customApiKey.value = customKey
        _apiKeyStatus.value = if (customKey.isBlank()) "Not Configured" else prefs.getString("custom_gemini_api_key_status", "Saved (Unverified)") ?: "Saved (Unverified)"

        val isFirst = prefs.getBoolean("is_first_launch", true)
        val hideWarning = prefs.getBoolean("hide_api_warning", false)
        _isFirstLaunch.value = isFirst
        _hideApiWarning.value = hideWarning

        // Startup onboarding display rules:
        // Scenario A: First Run (is_first_launch == true) -> Show
        // Scenario B: Subsequent run without key & hide warning false -> Show
        // Scenario C: Dismissed or Configured -> Do not show
        val shouldShowWarning = if (customKey.isNotBlank()) {
            false
        } else if (isFirst) {
            true
        } else {
            !hideWarning
        }
        _showOnboardingGuide.value = shouldShowWarning

        // Initialize Speech Manager
        speechManager = PrayerSpeechManager(application) { success ->
            if (success) {
                speechManager?.setOnDoneListener {
                    _isSpeaking.value = false
                }
            }
        }

        // Initialize with CURRENT date and current LiturgicalHour
        val todayStr = getCurrentDateString()
        val currentHour = getCurrentLiturgicalHour()
        _selectedDate.value = todayStr
        _selectedHour.value = currentHour
        loadPrayer(todayStr, currentHour)
    }

    // Toggle and Selection Handlers
    fun toggleRomanCalendar() {
        val newValue = !_useRomanCalendar.value
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("use_roman_calendar", newValue).apply()
        _useRomanCalendar.value = newValue
        loadPrayer(_selectedDate.value, _selectedHour.value)
    }

    fun selectLanguage(lang: AppLanguage) {
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("active_language", lang.name).apply()
        _activeLanguage.value = lang
        stopSpeaking()
    }

    fun toggleSoloMode() {
        val newValue = !_soloMode.value
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("solo_mode", newValue).apply()
        _soloMode.value = newValue
        stopSpeaking()
    }

    fun toggleSimplifiedRubrics() {
        val newValue = !_simplifiedRubrics.value
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("simplified_rubrics", newValue).apply()
        _simplifiedRubrics.value = newValue
    }

    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key
        val statusStr = if (key.isBlank()) "Not Configured" else "Saved (Unverified)"
        _apiKeyStatus.value = statusStr
        
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_gemini_api_key", key)
            .putString("custom_gemini_api_key_status", statusStr)
            .apply()
    }

    fun dismissOnboarding(disableWarningFuture: Boolean) {
        _showOnboardingGuide.value = false
        if (disableWarningFuture) {
            _hideApiWarning.value = true
            val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("hide_api_warning", true).apply()
        }
    }

    fun completeFirstLaunchAndGoToSettings() {
        _isFirstLaunch.value = false
        val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_first_launch", false)
            .apply()
        _showOnboardingGuide.value = false
    }

    fun setShowOnboardingGuide(show: Boolean) {
        _showOnboardingGuide.value = show
    }

    fun testCustomApiKey(key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            _isCheckingApiKey.value = true
            _apiKeyStatus.value = "Testing..."
            val resultStatus = com.example.data.api.GeminiApiClient.testApiKey(key)
            _apiKeyStatus.value = resultStatus
            
            val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("custom_gemini_api_key_status", resultStatus).apply()
            _isCheckingApiKey.value = false
        }
    }

    fun toggleReminders() {
        val newValue = !_remindersEnabled.value
        com.example.util.PrayerReminderScheduler.setRemindersEnabled(getApplication(), newValue)
        _remindersEnabled.value = newValue
    }

    fun triggerTestNotification() {
        val currentHour = _selectedHour.value
        com.example.util.PrayerReminderReceiver.showNotification(
            getApplication(),
            currentHour.name,
            currentHour.displayName
        )
    }

    // Text to Speech Controls
    fun speakCurrentPrayer(textToSpeak: String) {
        if (_isSpeaking.value) {
            stopSpeaking()
        } else {
            _isSpeaking.value = true
            speechManager?.speak(textToSpeak, _activeLanguage.value, _speechSpeed.value) { activeText, idx ->
                _activeSentenceText.value = activeText
                if (activeText == null) {
                    _isSpeaking.value = false
                }
            }
        }
    }

    fun stopSpeaking() {
        speechManager?.stop()
        _isSpeaking.value = false
        _activeSentenceText.value = null
    }

    fun setSpeechSpeed(speed: Float) {
        _speechSpeed.value = speed
        if (_isSpeaking.value) {
            // Re-trigger with new speed
            _isSpeaking.value = false
            // Will require the screen component to play again if they want, or we can leave it stopped
        }
    }

    // Backup & Restore
    fun exportBackup(outputStream: OutputStream, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BreviaryBackupManager.exportBackup(getApplication(), outputStream)
            onComplete(result)
        }
    }

    fun importBackup(inputStream: InputStream, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BreviaryBackupManager.importBackup(getApplication(), inputStream)
            if (result) {
                // Reload states
                val prefs = getApplication<Application>().getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
                _useRomanCalendar.value = prefs.getBoolean("use_roman_calendar", true)
                val langStr = prefs.getString("active_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
                _activeLanguage.value = try { AppLanguage.valueOf(langStr) } catch(e: Exception) { AppLanguage.ENGLISH }
                _soloMode.value = prefs.getBoolean("solo_mode", false)
                _simplifiedRubrics.value = prefs.getBoolean("simplified_rubrics", false)
                
                // Reload current prayer
                loadPrayer(_selectedDate.value, _selectedHour.value)
            }
            onComplete(result)
        }
    }

    fun downloadTodayHours() {
        val dateStr = _selectedDate.value
        viewModelScope.launch {
            _downloadStatus.value = "Downloading..."
            var successCount = 0
            LiturgicalHour.entries.forEach { hr ->
                val res = repository.getPrayer(dateStr, hr, forceRefresh = true)
                if (res.isSuccess) {
                    successCount++
                }
            }
            if (successCount == LiturgicalHour.entries.size) {
                _downloadStatus.value = "Downloaded all 5 Hours!"
            } else {
                _downloadStatus.value = "Downloaded $successCount/5 Hours."
            }
            kotlinx.coroutines.delay(3000)
            _downloadStatus.value = null
        }
    }

    fun toggleSectionBookmark(sectionType: String, sectionTitle: String, snippet: String) {
        val dateStr = _selectedDate.value
        val hour = _selectedHour.value
        viewModelScope.launch {
            val currentlyBookmarked = repository.isBookmarked(dateStr, hour.name, sectionType, sectionTitle)
            if (currentlyBookmarked) {
                repository.removeBookmarkByDetails(dateStr, hour.name, sectionType, sectionTitle)
            } else {
                val newBm = com.example.data.database.Bookmark(
                    date = dateStr,
                    hour = hour.name,
                    sectionType = sectionType,
                    sectionTitle = sectionTitle,
                    snippet = snippet
                )
                repository.addBookmark(newBm)
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
        loadPrayer(dateString, _selectedHour.value)
    }

    fun selectHour(hour: LiturgicalHour) {
        _selectedHour.value = hour
        loadPrayer(_selectedDate.value, hour)
    }

    fun togglePrayerCompleted() {
        val currentState = _uiState.value
        if (currentState is PrayerUiState.Success) {
            val dateStr = _selectedDate.value
            val hour = _selectedHour.value
            val targetCompleted = !currentState.isCompleted
            
            viewModelScope.launch {
                repository.setPrayerCompleted(dateStr, hour, targetCompleted)
                _uiState.value = currentState.copy(isCompleted = targetCompleted)
                loadDailyStatus(dateStr)
            }
        }
    }

    fun refreshCurrentPrayer() {
        val dateStr = _selectedDate.value
        val hour = _selectedHour.value
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.getPrayer(dateStr, hour, forceRefresh = true)
            val status = repository.getPrayerStatus(dateStr, hour)
            
            result.onSuccess { prayer ->
                _uiState.value = PrayerUiState.Success(prayer, status.isCompleted)
            }.onFailure { err ->
                _uiState.value = PrayerUiState.Error(err.message ?: "Failed to update prayer")
            }
            _isRefreshing.value = false
        }
    }

    private var loadPrayerJob: kotlinx.coroutines.Job? = null

    private fun loadPrayer(dateString: String, hour: LiturgicalHour) {
        loadPrayerJob?.cancel()
        _uiState.value = PrayerUiState.Loading
        stopSpeaking()
        loadPrayerJob = viewModelScope.launch {
            loadDailyStatus(dateString)

            val result = repository.getPrayer(dateString, hour)
            val status = repository.getPrayerStatus(dateString, hour)

            if (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                result.onSuccess { prayer ->
                    _uiState.value = PrayerUiState.Success(prayer, status.isCompleted)
                }.onFailure { err ->
                    _uiState.value = PrayerUiState.Error(err.message ?: "Failed to load prayer")
                }
            }
        }
    }

    private suspend fun loadDailyStatus(dateString: String) {
        val statuses = repository.getPrayerStatusesForDate(dateString)
        val statusMap = LiturgicalHour.entries.associateWith { hour ->
            statuses.firstOrNull { it.hour == hour.name }?.isCompleted ?: false
        }
        _dailyStatus.value = statusMap
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun getCurrentLiturgicalHour(): LiturgicalHour {
        val cal = Calendar.getInstance()
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        return LiturgicalHour.fromHourOfDay(hourOfDay)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.shutdown()
        speechManager = null
    }
}

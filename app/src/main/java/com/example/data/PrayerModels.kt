package com.example.data

import com.squareup.moshi.JsonClass

enum class LiturgicalHour(val displayName: String, val latinName: String, val startHour: Int, val endHour: Int) {
    OFFICE_READINGS("Office of Readings", "Officium Lectionis", 0, 5),
    LAUDS("Morning Prayer", "Lauds", 5, 9),
    MIDDAY("Midday Prayer", "Ad Sextam", 9, 16),
    VESPERS("Evening Prayer", "Vespers", 16, 20),
    COMPLINE("Night Prayer", "Compline", 20, 24);

    companion object {
        fun fromHourOfDay(hour: Int): LiturgicalHour {
            return entries.firstOrNull { hour in it.startHour..<it.endHour } ?: VESPERS
        }
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    LATIN("la", "Latin"),
    TAGALOG("tl", "Tagalog")
}

@JsonClass(generateAdapter = true)
data class LiturgicalPrayer(
    val title: String,                 // e.g., "Morning Prayer (Lauds)"
    val dateString: String,            // e.g., "2026-05-23"
    val hostDay: String,               // e.g., "Saturday, May 23, 2026"
    val season: String,                // e.g., "Seventh Week of Easter"
    
    // Header Info (Trilingual)
    val titleEnglish: String,
    val titleLatin: String,
    val titleTagalog: String,
    
    val seasonEnglish: String,
    val seasonLatin: String,
    val seasonTagalog: String,

    // Opening Verses
    val openingVerseCallEnglish: String,
    val openingVerseCallLatin: String,
    val openingVerseCallTagalog: String,
    
    val openingVerseResponseEnglish: String,
    val openingVerseResponseLatin: String,
    val openingVerseResponseTagalog: String,
    
    val invitatoryAntiphonEnglish: String? = null,
    val invitatoryAntiphonLatin: String? = null,
    val invitatoryAntiphonTagalog: String? = null,
    
    // Hymn
    val hymnTitleEnglish: String,
    val hymnTitleLatin: String,
    val hymnTitleTagalog: String,
    val hymnTextEnglish: String,
    val hymnTextLatin: String,
    val hymnTextTagalog: String,
    
    val psalms: List<PsalmSection>,
    
    // Readings
    val readingReference: String,      // Share scripture citation
    val readingTextEnglish: String,
    val readingTextLatin: String,
    val readingTextTagalog: String,
    val readingResponseEnglish: String,
    val readingResponseLatin: String,
    val readingResponseTagalog: String,
    
    // Responsory
    val responsoryVersicleEnglish: String,
    val responsoryVersicleLatin: String,
    val responsoryVersicleTagalog: String,
    val responsoryResponseEnglish: String,
    val responsoryResponseLatin: String,
    val responsoryResponseTagalog: String,
    
    // Gospel Canticle
    val canticleAntiphonBeforeEnglish: String,
    val canticleAntiphonBeforeLatin: String,
    val canticleAntiphonBeforeTagalog: String,
    
    val canticleTitleEnglish: String,
    val canticleTitleLatin: String,
    val canticleTitleTagalog: String,
    val canticleTextEnglish: String,
    val canticleTextLatin: String,
    val canticleTextTagalog: String,
    
    val canticleAntiphonAfterEnglish: String,
    val canticleAntiphonAfterLatin: String,
    val canticleAntiphonAfterTagalog: String,
    
    // Intercessions
    val intercessionsPrefaceEnglish: String,
    val intercessionsPrefaceLatin: String,
    val intercessionsPrefaceTagalog: String,
    val intercessionsResponseEnglish: String,
    val intercessionsResponseLatin: String,
    val intercessionsResponseTagalog: String,
    val intercessionsList: List<IntercessionItem>,
    
    // Lord's Prayer
    val lordPrayerEnglish: String,
    val lordPrayerLatin: String,
    val lordPrayerTagalog: String,
    
    // Closing
    val closingPrayerEnglish: String,
    val closingPrayerLatin: String,
    val closingPrayerTagalog: String,
    
    val dismissalEnglish: String,
    val dismissalLatin: String,
    val dismissalTagalog: String,

    val rubricHymn: String? = null,
    val rubricPsalm: String? = null,
    val rubricReading: String? = null,
    val rubricCanticle: String? = null,
    val rubricIntercessions: String? = null,
    val rubricClosing: String? = null
) {
    // Helper accessors
    fun getTitle(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> titleEnglish
        AppLanguage.LATIN -> titleLatin
        AppLanguage.TAGALOG -> titleTagalog
    }
    fun getSeason(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> seasonEnglish
        AppLanguage.LATIN -> seasonLatin
        AppLanguage.TAGALOG -> seasonTagalog
    }
    fun getOpeningVerseCall(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> openingVerseCallEnglish
        AppLanguage.LATIN -> openingVerseCallLatin
        AppLanguage.TAGALOG -> openingVerseCallTagalog
    }
    fun getOpeningVerseResponse(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> openingVerseResponseEnglish
        AppLanguage.LATIN -> openingVerseResponseLatin
        AppLanguage.TAGALOG -> openingVerseResponseTagalog
    }
    fun getInvitatoryAntiphon(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> invitatoryAntiphonEnglish
        AppLanguage.LATIN -> invitatoryAntiphonLatin
        AppLanguage.TAGALOG -> invitatoryAntiphonTagalog
    }
    fun getHymnTitle(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> hymnTitleEnglish
        AppLanguage.LATIN -> hymnTitleLatin
        AppLanguage.TAGALOG -> hymnTitleTagalog
    }
    fun getHymnText(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> hymnTextEnglish
        AppLanguage.LATIN -> hymnTextLatin
        AppLanguage.TAGALOG -> hymnTextTagalog
    }
    fun getReadingText(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> readingTextEnglish
        AppLanguage.LATIN -> readingTextLatin
        AppLanguage.TAGALOG -> readingTextTagalog
    }
    fun getReadingResponse(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> readingResponseEnglish
        AppLanguage.LATIN -> readingResponseLatin
        AppLanguage.TAGALOG -> readingResponseTagalog
    }
    fun getResponsoryVersicle(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> responsoryVersicleEnglish
        AppLanguage.LATIN -> responsoryVersicleLatin
        AppLanguage.TAGALOG -> responsoryVersicleTagalog
    }
    fun getResponsoryResponse(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> responsoryResponseEnglish
        AppLanguage.LATIN -> responsoryResponseLatin
        AppLanguage.TAGALOG -> responsoryResponseTagalog
    }
    fun getCanticleAntiphonBefore(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> canticleAntiphonBeforeEnglish
        AppLanguage.LATIN -> canticleAntiphonBeforeLatin
        AppLanguage.TAGALOG -> canticleAntiphonBeforeTagalog
    }
    fun getCanticleTitle(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> canticleTitleEnglish
        AppLanguage.LATIN -> canticleTitleLatin
        AppLanguage.TAGALOG -> canticleTitleTagalog
    }
    fun getCanticleText(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> canticleTextEnglish
        AppLanguage.LATIN -> canticleTextLatin
        AppLanguage.TAGALOG -> canticleTextTagalog
    }
    fun getCanticleAntiphonAfter(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> canticleAntiphonAfterEnglish
        AppLanguage.LATIN -> canticleAntiphonAfterLatin
        AppLanguage.TAGALOG -> canticleAntiphonAfterTagalog
    }
    fun getIntercessionsPreface(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> intercessionsPrefaceEnglish
        AppLanguage.LATIN -> intercessionsPrefaceLatin
        AppLanguage.TAGALOG -> intercessionsPrefaceTagalog
    }
    fun getIntercessionsResponse(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> intercessionsResponseEnglish
        AppLanguage.LATIN -> intercessionsResponseLatin
        AppLanguage.TAGALOG -> intercessionsResponseTagalog
    }
    fun getLordPrayer(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> lordPrayerEnglish
        AppLanguage.LATIN -> lordPrayerLatin
        AppLanguage.TAGALOG -> lordPrayerTagalog
    }
    fun getClosingPrayer(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> closingPrayerEnglish
        AppLanguage.LATIN -> closingPrayerLatin
        AppLanguage.TAGALOG -> closingPrayerTagalog
    }
    fun getDismissal(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> dismissalEnglish
        AppLanguage.LATIN -> dismissalLatin
        AppLanguage.TAGALOG -> dismissalTagalog
    }
}

@JsonClass(generateAdapter = true)
data class PsalmSection(
    val title: String,                 // e.g., "Psalm 63:2-9"
    val subtitleEnglish: String? = null,
    val subtitleLatin: String? = null,
    val subtitleTagalog: String? = null,
    
    val antiphonBeforeEnglish: String,
    val antiphonBeforeLatin: String,
    val antiphonBeforeTagalog: String,
    
    val textEnglish: String,
    val textLatin: String,
    val textTagalog: String,
    
    val antiphonAfterEnglish: String,
    val antiphonAfterLatin: String,
    val antiphonAfterTagalog: String,
    val rubricText: String? = null
) {
    fun getSubtitle(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> subtitleEnglish
        AppLanguage.LATIN -> subtitleLatin
        AppLanguage.TAGALOG -> subtitleTagalog
    }
    fun getAntiphonBefore(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> antiphonBeforeEnglish
        AppLanguage.LATIN -> antiphonBeforeLatin
        AppLanguage.TAGALOG -> antiphonBeforeTagalog
    }
    fun getText(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> textEnglish
        AppLanguage.LATIN -> textLatin
        AppLanguage.TAGALOG -> textTagalog
    }
    fun getAntiphonAfter(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> antiphonAfterEnglish
        AppLanguage.LATIN -> antiphonAfterLatin
        AppLanguage.TAGALOG -> antiphonAfterTagalog
    }
}

@JsonClass(generateAdapter = true)
data class IntercessionItem(
    val petitionEnglish: String,
    val petitionLatin: String,
    val petitionTagalog: String,
    
    val responseEnglish: String,
    val responseLatin: String,
    val responseTagalog: String
) {
    fun getPetition(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> petitionEnglish
        AppLanguage.LATIN -> petitionLatin
        AppLanguage.TAGALOG -> petitionTagalog
    }
    fun getResponse(lang: AppLanguage) = when(lang) {
        AppLanguage.ENGLISH -> responseEnglish
        AppLanguage.LATIN -> responseLatin
        AppLanguage.TAGALOG -> responseTagalog
    }
}

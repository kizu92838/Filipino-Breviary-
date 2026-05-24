package com.example.data.api

import com.example.BuildConfig
import com.example.data.LiturgicalHour
import com.example.data.LiturgicalPrayer
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun fetchLiturgyFromAi(context: android.content.Context, dateString: String, hour: LiturgicalHour, useRomanCalendar: Boolean = false): LiturgicalPrayer {
        val prefs = context.getSharedPreferences("breviary_prefs", android.content.Context.MODE_PRIVATE)
        val customKey = prefs.getString("custom_gemini_api_key", null)
        val hasCustom = !customKey.isNullOrBlank()

        val cbcpDay = com.example.data.RomanCalendarCalculator.getRomanDayInfo(dateString)
        val holidayContext = "Today is ${cbcpDay.title} (${cbcpDay.rank}, Liturgical Color: ${cbcpDay.color.displayName}, Season: ${cbcpDay.season}). " +
                "You MUST formulate this Liturgy of the Hours in full compliance with the Philippine Liturgical Calendar of the Catholic Bishops\' Conference of the Philippines (CBCP)! " +
                "Include specialized CBCP solemnities, feasts, or novenas e.g. Santo Niño, San Pedro Calungsod, San Lorenzo Ruiz, Simbang Gabi / Aguinaldo liturgies if they fall on this date. " +
                "Make sure all translation texts reflect this feast."

        val prompt = """
            Create an authentic Catholic Liturgy of the Hours (Breviary) prayer.
            Date: $dateString
            Hour: ${hour.displayName} (${hour.latinName})
            
            $holidayContext
            
            You MUST return a single, complete JSON object containing accurate translations for ALL three languages: ENGLISH, LATIN (Liturgia Horarum), and TAGALOG (official CBCP translations from the Aklat ng Panalangin ng Simbahan / Aklat ng Pagmimisa sa Roma). 
            Avoid literal machine translations in Tagalog; make sure they use the approved, sacred liturgical language.
            
            Return the result in JSON matching this exact structure:
            {
              "title": "${hour.displayName} (${hour.latinName})",
              "dateString": "$dateString",
              "hostDay": "${cbcpDay.title}",
              "season": "${cbcpDay.season}",
              
              "titleEnglish": "Morning Prayer (for example)",
              "titleLatin": "Laudes (for example)",
              "titleTagalog": "Panalanging Pang-umaga (for example)",
              
              "seasonEnglish": "${cbcpDay.season} (in English)",
              "seasonLatin": "${cbcpDay.season} (in Latin)",
              "seasonTagalog": "${cbcpDay.season} (in Tagalog)",
              
              "openingVerseCallEnglish": "God, come to my assistance.",
              "openingVerseCallLatin": "Deus, in adiutórium meum inténde.",
              "openingVerseCallTagalog": "O Diyos, lumingap ka sa aking tulong.",
              
              "openingVerseResponseEnglish": "Lord, make haste to help me.",
              "openingVerseResponseLatin": "Dómine, ad adiuvándum me festína.",
              "openingVerseResponseTagalog": "Panginoon, magmadali ka sa pagsaklolo sa akin.",
              
              "invitatoryAntiphonEnglish": "Come, let us worship Christ, the Lord of Life.",
              "invitatoryAntiphonLatin": "Venite, adoremus Dominum.",
              "invitatoryAntiphonTagalog": "Halina, sambahin natin ang Kristong Panginoon ng Buhay.",
              
              "hymnTitleEnglish": "Hymn title in English",
              "hymnTitleLatin": "Hymn title in Latin",
              "hymnTitleTagalog": "Hymn title in Tagalog",
              
              "hymnTextEnglish": "Full text of the hymn in English (verses separated by \n\n)",
              "hymnTextLatin": "Full text of the hymn in Latin (verses separated by \n\n)",
              "hymnTextTagalog": "Full text of the hymn in Tagalog (verses separated by \n\n)",
              
              "psalms": [
                {
                  "title": "e.g., Psalm 63:2-9",
                  "subtitleEnglish": "A soul thirsting for God",
                  "subtitleLatin": "Deus sitit ad te anima mea",
                  "subtitleTagalog": "Uhaw ang kaluluwa ko sa iyo, o Diyos",
                  
                  "antiphonBeforeEnglish": "Antiphon text before reciting the psalm in English",
                  "antiphonBeforeLatin": "Antiphon text before reciting the psalm in Latin",
                  "antiphonBeforeTagalog": "Antiphon text before reciting the psalm in Tagalog",
                  
                  "textEnglish": "Full text in English, verses split with \n and stanzas separated by \n\n",
                  "textLatin": "Full text in Latin, verses split with \n and stanzas separated by \n\n",
                  "textTagalog": "Full text in Tagalog, verses split with \n and stanzas separated by \n\n",
                  
                  "antiphonAfterEnglish": "Antiphon repeated after the psalm in English",
                  "antiphonAfterLatin": "Antiphon repeated after the psalm in Latin",
                  "antiphonAfterTagalog": "Antiphon repeated after the psalm in Tagalog",
                  
                  "rubricText": "Detailed liturgical custom rubric (e.g., sit, stand, bow) or null"
                }
              ],
              
              "readingReference": "Scripture citation e.g. Romans 12:1-2",
              "readingTextEnglish": "Full text of the reading in English",
              "readingTextLatin": "Full text of the reading in Latin",
              "readingTextTagalog": "Full text of the reading in Tagalog",
              
              "readingResponseEnglish": "Thanks be to God.",
              "readingResponseLatin": "Deo gratias.",
              "readingResponseTagalog": "Salamat sa Diyos.",
              
              "responsoryVersicleEnglish": "V: In the morning, Lord, you hear my voice.",
              "responsoryVersicleLatin": "V: Mane exáudies vocem meam, Dómine.",
              "responsoryVersicleTagalog": "V: Sa umaga, Panginoon, diringgin mo ang aking tinig.",
              
              "responsoryResponseEnglish": "R: At sunrise I stand before you in prayer.",
              "responsoryResponseLatin": "R: Mane astábo tibi et vidého.",
              "responsoryResponseTagalog": "R: Sa pagsikat ng araw ako ay haharap sa iyong panalangin.",
              
              "canticleAntiphonBeforeEnglish": "Before Canticle Antiphon in English",
              "canticleAntiphonBeforeLatin": "Before Canticle Antiphon in Latin",
              "canticleAntiphonBeforeTagalog": "Before Canticle Antiphon in Tagalog",
              
              "canticleTitleEnglish": "e.g., Canticle of Mary (Magnificat) or Benedictus",
              "canticleTitleLatin": "e.g., Canticum Beatæ Mariæ Virginis (Magnificat)",
              "canticleTitleTagalog": "e.g., Awit ni Maria (Magnificat)",
              
              "canticleTextEnglish": "Full Canticle text in English",
              "canticleTextLatin": "Full Canticle text in Latin",
              "canticleTextTagalog": "Full Canticle text in Tagalog",
              
              "canticleAntiphonAfterEnglish": "After Canticle Antiphon in English",
              "canticleAntiphonAfterLatin": "After Canticle Antiphon in Latin",
              "canticleAntiphonAfterTagalog": "After Canticle Antiphon in Tagalog",
              
              "intercessionsPrefaceEnglish": "Preface to petitions in English",
              "intercessionsPrefaceLatin": "Preface to petitions in Latin",
              "intercessionsPrefaceTagalog": "Preface to petitions in Tagalog",
              
              "intercessionsResponseEnglish": "Response: Lord, hear our prayer.",
              "intercessionsResponseLatin": "R: Te rogámus, audi nos.",
              "intercessionsResponseTagalog": "Tugon: Panginoon, dinggin mo ang aming panalangin.",
              
              "intercessionsList": [
                {
                  "petitionEnglish": "Petiiton in English",
                  "petitionLatin": "Petition in Latin",
                  "petitionTagalog": "Petition in Tagalog",
                  "responseEnglish": "Response text in English",
                  "responseLatin": "Response text in Latin",
                  "responseTagalog": "Response text in Tagalog"
                }
              ],
              
              "lordPrayerEnglish": "Our Father...",
              "lordPrayerLatin": "Pater Noster...",
              "lordPrayerTagalog": "Ama Namin...",
              
              "closingPrayerEnglish": "Concluding prayer in English.",
              "closingPrayerLatin": "Concluding prayer in Latin.",
              "closingPrayerTagalog": "Concluding prayer in Tagalog.",
              
              "dismissalEnglish": "May the Lord bless us, protect us from all evil, and bring us to everlasting life. Amen.",
              "dismissalLatin": "Nos benedícat Dóminus, et ab omni malo deféndat, et dedúcat ad vitam ætérnam. Amen.",
              "dismissalTagalog": "Pagpalain nawa tayo ng Panginoon, iligtas tayo sa rurok ng kasamaan at ihatid tayo sa buhay na walang hanggan. Amen.",
              
              "rubricHymn": "Optional rubric text for hymn in English (e.g. Stand)",
              "rubricPsalm": "Optional rubric text for psalms in English (e.g. Sit)",
              "rubricReading": "Optional rubric text for reading in English (e.g. Sit)",
              "rubricCanticle": "Optional rubric text for canticle in English (e.g. Stand and make the sign of the cross)",
              "rubricIntercessions": "Optional rubric text for intercessions in English (e.g. Stand)",
              "rubricClosing": "Optional rubric text for concluding prayer in English (e.g. Stand)"
            }
            
            Generate this JSON beautifully. Crucially, avoid adding dummy placeholders like '[ ...]'; return actual sacred Liturgy of the Hours texts.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a professional Roman Catholic liturgy expert, a scholar of the Liturgia Horarum and a linguist specialized in approved liturgical Tagalog and English. Your job is to return precise, complete, and authentic trilingual (English, Latin, Tagalog) Liturgy of the Hours prayers in exactly the requested JSON format, following the CBCP Philippine Liturgical Calendar instructions precisely."))
            )
        )

        if (!hasCustom) {
            throw IllegalStateException("API key is not configured.")
        }
        val activeKey = customKey!!

        var jsonResult: String
        try {
            val response = service.generateContent(activeKey, request)
            jsonResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Gemini returned an empty response")
        } catch (e: Exception) {
            throw e
        }

        val cleanJson = jsonResult.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val itemAdapter = moshi.adapter(LiturgicalPrayer::class.java)
        return itemAdapter.fromJson(cleanJson) ?: throw IllegalStateException("Failed to parse prayer JSON")
    }

    suspend fun testApiKey(apiKey: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Not Configured"
        val testRequest = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Say: Hi")))
            ),
            generationConfig = GenerationConfig(temperature = 0.0f)
        )
        try {
            service.generateContent(apiKey, testRequest)
            "Active / Key Verified"
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401, 403 -> "Invalid API Key"
                429 -> "Rate Limit / Quota Exhausted"
                else -> "Error: ${e.code()}"
            }
        } catch (e: java.io.IOException) {
            "Connection Failed / Offline"
        } catch (e: Exception) {
            "Connection Failed / Offline"
        }
    }
}

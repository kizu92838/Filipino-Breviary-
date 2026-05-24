package com.example.data

import java.text.SimpleDateFormat
import java.util.*

enum class LiturgicalColor(val displayName: String, val hexBg: Long, val hexOnBg: Long) {
    GREEN("Green", 0xFFE8F5E9, 0xFF145A32),
    WHITE("White", 0xFFFFFEE0, 0xFF4A3B32), // Warm, rich parchment
    RED("Red", 0xFFFFEBEE, 0xFF7B241C),
    VIOLET("Violet", 0xFFF3E5F5, 0xFF512DA8)
}

data class RomanDayInfo(
    val title: String,          // e.g. "Feast of Saint Pedro Calungsod"
    val rank: String,           // "Solemnity", "Feast", "Memorial", "Optional Memorial", "Weekday"
    val color: LiturgicalColor,
    val season: String,         // "Eastertide", "Lent", "Ordinary Time", "Advent", etc.
    val customHymn: Pair<String, String>? = null,
    val customConcludingPrayer: String? = null
)

object RomanCalendarCalculator {

    fun getRomanDayInfo(dateString: String): RomanDayInfo {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            sdfInput.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val cal = Calendar.getInstance()
        cal.time = date

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // 1-based
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val monthDayStr = String.format("%02d-%02d", month, day)

        // 1. Determine Liturgical Season for year 2026 specifically, or general fallback
        var season: String
        var color = LiturgicalColor.GREEN
        var dayTitle = ""
        var dayRank = "Weekday"

        if (year == 2026) {
            // Ash Wednesday in 2026 is Feb 18; Easter is April 5; Pentecost is May 24; Advent begins Nov 29.
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            when (dayOfYear) {
                // Before Lent: Ordinary Time
                in 1..48 -> {
                    season = "Ordinary Time"
                    color = LiturgicalColor.GREEN
                    dayTitle = "Weekday in Ordinary Time"
                }
                // Lent: Feb 18 (49) to April 4 (94)
                in 49..94 -> {
                    season = "Lent"
                    color = LiturgicalColor.VIOLET
                    dayTitle = getLentenDayTitle(dayOfYear, dayOfWeek)
                    dayRank = if (dayOfWeek == Calendar.SUNDAY) "Sunday of Lent" else "Lenten Weekday"
                }
                // Eastertide: April 5 (95) to May 24 (144)
                in 95..144 -> {
                    season = "Easter Season"
                    color = LiturgicalColor.WHITE
                    dayTitle = getEastertideDayTitle(dayOfYear, dayOfWeek)
                    dayRank = if (dayOfWeek == Calendar.SUNDAY) "Sunday of Easter" else "Easter Weekday"
                    
                    // Specific high-solemnity overwrites:
                    if (dayOfYear == 144) {
                        dayTitle = "Pentecost Sunday"
                        dayRank = "Solemnity"
                        color = LiturgicalColor.RED
                    } else if (dayOfYear == 134) {
                        dayTitle = "Ascension of the Lord"
                        dayRank = "Solemnity"
                    }
                }
                // Advent: Nov 29 (333) to Dec 24 (358)
                in 333..358 -> {
                    season = "Advent"
                    color = LiturgicalColor.VIOLET
                    dayTitle = "Weekday of Advent"
                    dayRank = if (dayOfWeek == Calendar.SUNDAY) "Sunday of Advent" else "Advent Weekday"
                }
                // Christmastide: Dec 25 (359) onwards
                in 359..366 -> {
                    season = "Christmas Season"
                    color = LiturgicalColor.WHITE
                    dayTitle = if (monthDayStr == "12-25") "Nativity of Our Lord" else "Octave of Christmas"
                    dayRank = if (monthDayStr == "12-25") "Solemnity" else "Feast"
                }
                // Remainder is Ordinary Time
                else -> {
                    season = "Ordinary Time"
                    color = LiturgicalColor.GREEN
                    dayTitle = "Weekday in Ordinary Time"
                }
            }
        } else {
            // General fallback seasons
            season = when (month) {
                12 -> if (day < 25) "Advent" else "Christmas Season"
                1 -> if (day <= 10) "Christmas Season" else "Ordinary Time"
                2, 3, 4 -> "Lenten & Easter Season"
                5 -> "Easter Season"
                else -> "Ordinary Time"
            }
            color = when (season) {
                "Advent", "Lent" -> LiturgicalColor.VIOLET
                "Christmas Season", "Easter Season" -> LiturgicalColor.WHITE
                else -> LiturgicalColor.GREEN
            }
            dayTitle = "Weekday in $season"
        }

        // 2. Map Fixed Sanctorale Feasts and CBCP-Specific Solenities (Overwrites standard weekdays)
        var customHymn: Pair<String, String>? = null
        var customConcludingPrayer: String? = null

        // Calculate Santo Niño (Third Sunday of January)
        if (month == 1) {
            // Find Sunday occurrence in January
            val calTemp = Calendar.getInstance()
            calTemp.set(year, Calendar.JANUARY, 1)
            val firstDayOfJan = calTemp.get(Calendar.DAY_OF_WEEK)
            val firstSunday = if (firstDayOfJan == Calendar.SUNDAY) 1 else 1 + (8 - firstDayOfJan)
            val thirdSunday = firstSunday + 14
            if (day == thirdSunday) {
                return RomanDayInfo(
                    title = "Solemnity of the Santo Niño (Holy Child Jesus) - CBCP",
                    rank = "Solemnity",
                    color = LiturgicalColor.WHITE,
                    season = "Christmas Season / Ordinary Time",
                    customConcludingPrayer = "Panginoong Diyos, isinugo mo ang iyong tanging Anak sa karupukan ng aming katawan upang ipahayag ang iyong walang hanggang pag-ibig. Habang aming sinasamba ang Santo Niño, ipagkaloob mo na kami ay lumago sa karunungan at kabanalan at sumunod sa kanya nang may kapayakan ng puso. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."
                )
            }
        }

        // Simbang Gabi novena days (December 16 to 24)
        if (month == 12 && day in 16..24) {
            return RomanDayInfo(
                title = "Simbang Gabi (Dawn Mass of the Christmas Novena) - CBCP",
                rank = "Feast Novena",
                color = LiturgicalColor.WHITE,
                season = "Advent Novena",
                customConcludingPrayer = "Makapangyarihang Ama, ang pagdating ng iyong Anak sa aming katawang-lupa ang nagbibigay-liwanag sa aming bukang-liwayway at pag-asa sa aming kaligtasan. Habang aming ipinagdiriwang ang mga banal na umagang ito ng Simbang Gabi, ihanda mo ang aming mga puso na tanggapin ang kanyang biyaya. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."
            )
        }

        when (monthDayStr) {
            "01-01" -> {
                dayTitle = "Solemnity of Mary, Most Holy Mother of God"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "03-19" -> {
                dayTitle = "Saint Joseph, Spouse of the Blessed Virgin Mary"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "03-25" -> {
                dayTitle = "The Annunciation of the Lord"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "04-02" -> {
                dayTitle = "Feast of Saint Pedro Calungsod, Young Catechist & Martyr - CBCP"
                dayRank = "Feast"
                color = LiturgicalColor.RED
                customConcludingPrayer = "O Diyos na makapangyarihan at walang hanggan, pinagkalooban mo si San Pedro Calungsod ng biyaya na magpatotoo sa iyong Mabuting Balita nang may katapangan hanggang sa kamatayan. Sa pamamagitan ng kanyang panalangin, punuin mo kami ng sigasig sa pananampalataya at pagmamahal sa paglilingkod sa iyo. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."
            }
            "05-01" -> {
                dayTitle = "Saint Joseph the Worker"
                dayRank = "Memorial"
                color = LiturgicalColor.WHITE
            }
            "05-14" -> {
                dayTitle = "Saint Matthias, Apostle"
                dayRank = "Feast"
                color = LiturgicalColor.RED
            }
            "05-23" -> {
                dayTitle = "Saint John Baptist de Rossi, Priest"
                dayRank = "Optional Memorial"
                color = LiturgicalColor.WHITE
                customHymn = Pair(
                    "O Saint of Humble Hearts",
                    "Among the poor of Christ you walked,\nWith patience, love, and sacred grace;\nIn quiet silence, there you talked,\nAnd found in suffering, Jesus' face.\n\nNow lead us, Father John, we pray,\nTo comfort those in fear and pain;\nTo serve with love throughout this day,\nThat Heaven's peace in us may reign."
                )
                customConcludingPrayer = "Lord God, you adorned Saint John Baptist de Rossi with a marvelous spirit of patience and charity to comfort the sick, instruct the ignorant, and reconciliate the sinful in Rome. Grant that, inspired by his pure, pastoral charity, we may serve our brothers and sisters in distress with a whole and active devotion. Through our Lord Jesus Christ. Amen."
            }
            "05-26" -> {
                dayTitle = "Saint Philip Neri, Priest"
                dayRank = "Memorial"
                color = LiturgicalColor.WHITE
            }
            "05-31" -> {
                dayTitle = "The Visitation of the Blessed Virgin Mary"
                dayRank = "Feast"
                color = LiturgicalColor.WHITE
            }
            "06-24" -> {
                dayTitle = "The Nativity of Saint John the Baptist"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "06-29" -> {
                dayTitle = "Saints Peter and Paul, Apostles"
                dayRank = "Solemnity"
                color = LiturgicalColor.RED
            }
            "07-22" -> {
                dayTitle = "Saint Mary Magdalene"
                dayRank = "Feast"
                color = LiturgicalColor.WHITE
            }
            "08-06" -> {
                dayTitle = "The Transfiguration of the Lord"
                dayRank = "Feast"
                color = LiturgicalColor.WHITE
            }
            "08-15" -> {
                dayTitle = "The Assumption of the Blessed Virgin Mary"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "09-14" -> {
                dayTitle = "The Exaltation of the Holy Cross"
                dayRank = "Feast"
                color = LiturgicalColor.RED
            }
            "09-28" -> {
                dayTitle = "Feast of Saint Lorenzo Ruiz and Companions, Martyrs - First Filipino Saint - CBCP"
                dayRank = "Feast"
                color = LiturgicalColor.RED
                customConcludingPrayer = "Haring may kapangyarihan at walang hanggang Diyos, ipinagkaloob mo kay San Lorenzo Ruiz at sa kanyang mga kasama ang katatagan sa iyong paglilingkod at pananampalataya. Ipagkaloob mo na kami ay maging handa na magtiis ng anuman para sa iyong pag-ibig. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."
            }
            "09-29" -> {
                dayTitle = "Saints Michael, Gabriel, and Raphael, Archangels"
                dayRank = "Feast"
                color = LiturgicalColor.WHITE
            }
            "11-01" -> {
                dayTitle = "All Saints"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "11-02" -> {
                dayTitle = "The Commemoration of All the Faithful Departed (All Souls)"
                dayRank = "Solemnity"
                color = LiturgicalColor.VIOLET
            }
            "12-08" -> {
                dayTitle = "Solemnity of the Immaculate Conception of the Blessed Virgin Mary (Patroness of the Philippines)"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
                customConcludingPrayer = "O Diyos, sa pamamagitan ng Kalinis-linisang Paglilihi ng Mahal na Birhen, inihanda mo ang isang marapat na tahanan para sa iyong Anak. Sa kanyang pamamagitan bilang aming Tagapagtanggol at Pintakasi ng Pilipinas, ipagkaloob mong maprotektahan kami sa rurok ng kasamaan at ihatid kami sa buhay na walang hanggan. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."
            }
            "12-25" -> {
                dayTitle = "The Nativity of Our Lord Jesus Christ"
                dayRank = "Solemnity"
                color = LiturgicalColor.WHITE
            }
            "12-26" -> {
                dayTitle = "Saint Stephen, First Martyr"
                dayRank = "Feast"
                color = LiturgicalColor.RED
            }
            "12-27" -> {
                dayTitle = "Saint John, Apostle and Evangelist"
                dayRank = "Feast"
                color = LiturgicalColor.WHITE
            }
        }

        // Sunday rules override weekdays
        if (dayOfWeek == Calendar.SUNDAY && dayRank == "Weekday") {
            dayTitle = "Sunday in $season"
            dayRank = "Sunday Celebration"
            color = if (season == "Ordinary Time") LiturgicalColor.GREEN else color
        }

        return RomanDayInfo(
            title = dayTitle,
            rank = dayRank,
            color = color,
            season = season,
            customHymn = customHymn,
            customConcludingPrayer = customConcludingPrayer
        )
    }

    private fun getLentenDayTitle(dayOfYear: Int, dayOfWeek: Int): String {
        val week = (dayOfYear - 49) / 7 + 1
        val names = listOf("First", "Second", "Third", "Fourth", "Fifth", "Sixth")
        val weekName = if (week in 1..6) names[week - 1] else "Holy"
        return if (dayOfWeek == Calendar.SUNDAY) {
            "$weekName Sunday of Lent"
        } else {
            "Weekday in the $weekName Week of Lent"
        }
    }

    private fun getEastertideDayTitle(dayOfYear: Int, dayOfWeek: Int): String {
        val week = (dayOfYear - 95) / 7 + 1
        val names = listOf("First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Pentecost")
        val weekName = if (week in 1..8) names[week - 1] else "Seventh"
        return if (dayOfWeek == Calendar.SUNDAY) {
            if (week == 1) "Easter Sunday" else "$weekName Sunday of Easter"
        } else {
            "Weekday in the $weekName Week of Easter"
        }
    }
}

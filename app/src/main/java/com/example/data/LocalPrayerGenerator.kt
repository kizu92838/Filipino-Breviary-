package com.example.data

import java.text.SimpleDateFormat
import java.util.*

object LocalPrayerGenerator {

    fun generateLocalPrayer(dateString: String, hour: LiturgicalHour, useRomanCalendar: Boolean = false, locale: Locale = Locale.getDefault()): LiturgicalPrayer {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            sdfInput.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val cal = Calendar.getInstance()
        cal.time = date

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekName = SimpleDateFormat("EEEE", Locale.US).format(date)
        val monthAndDay = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)

        // Local Philippine/CBCP Day details
        val cbcpDay = RomanCalendarCalculator.getRomanDayInfo(dateString)

        val title = "${hour.displayName} (${hour.latinName})"
        val hostDay = "$dayOfWeekName, $monthAndDay"
        val seasonText = cbcpDay.season

        // Define opening verses
        val openingVerseCallEnglish = if (hour == LiturgicalHour.LAUDS) "Lord, open my lips." else "God, come to my assistance."
        val openingVerseCallLatin = if (hour == LiturgicalHour.LAUDS) "Dómine, lábia mea apéries." else "Deus, in adiutórium meum inténde."
        val openingVerseCallTagalog = if (hour == LiturgicalHour.LAUDS) "Panginoon, buksan mo ang aking mga labi." else "O Diyos, lumingap ka sa aking tulong."

        val openingVerseResponseEnglish = if (hour == LiturgicalHour.LAUDS) "And my mouth shall declare your praise." else "Lord, make haste to help me."
        val openingVerseResponseLatin = if (hour == LiturgicalHour.LAUDS) "Et os meum annuntiábit laudem tuam." else "Dómine, ad adiuvándum me festína."
        val openingVerseResponseTagalog = if (hour == LiturgicalHour.LAUDS) "At ang aking bibig ay magpupuri sa iyo." else "Panginoon, magmadali ka sa pagsaklolo sa akin."

        val invitatoryAntiphonEnglish = if (hour == LiturgicalHour.LAUDS) "Come, let us worship Christ, the Lord of Life." else null
        val invitatoryAntiphonLatin = if (hour == LiturgicalHour.LAUDS) "Regem Regum Dominum, venite adoremus." else null
        val invitatoryAntiphonTagalog = if (hour == LiturgicalHour.LAUDS) "Halina, sambahin natin ang Kristong Hari ng mga hari." else null

        // Hymns in three languages
        val hymnTitleEnglish = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Father of Mercies, Hear Our Prayer"
            LiturgicalHour.LAUDS -> "Maker of All, Eternal King"
            LiturgicalHour.MIDDAY -> "O God of Truth, O Lord of Might"
            LiturgicalHour.VESPERS -> "O Blest Creator of the Light"
            LiturgicalHour.COMPLINE -> "To Thee, Before the Close of Day"
        }
        val hymnTitleLatin = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Nocte Surgentes"
            LiturgicalHour.LAUDS -> "Aeterne Rerum Conditor"
            LiturgicalHour.MIDDAY -> "Rector Potens, Verax Deus"
            LiturgicalHour.VESPERS -> "Lucis Creator Optime"
            LiturgicalHour.COMPLINE -> "Te Lucis Ante Terminum"
        }
        val hymnTitleTagalog = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "O Amang Maawain, Dinggin aming Daing"
            LiturgicalHour.LAUDS -> "Dakilang Lumikha at Walang Hanggang Hari"
            LiturgicalHour.MIDDAY -> "Diyos ng Katotohanan, Panginoon ng Lakas"
            LiturgicalHour.VESPERS -> "O Dakilang Lumikha ng Liwanag"
            LiturgicalHour.COMPLINE -> "Sa Iyo, Bago Sumapit ang Gabi"
        }

        val hymnTextEnglish = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Father of mercies, hear our prayer,\nAs in the darkness of the night we watch and pray.\nIn song we lift our hearts to Thee,\nWho guidest all our steps along the way.\n\nSustain our souls with heavenly grace,\nAnd grant us strength to persevere in light."
            LiturgicalHour.LAUDS -> "Maker of all, Eternal King,\nWho rulest over night and day,\nNow at the dawn of morning light,\nWe rise to sing and humbly pray.\n\nBehold! The star of morning shines,\nTo drive the shadows far away."
            LiturgicalHour.MIDDAY -> "O God of truth, O Lord of might,\nWho rulest all the changing day,\nWith fields of gold and blinding light,\nAnd guidest order on its way."
            LiturgicalHour.VESPERS -> "O Blest Creator of the Light,\nWho broughtest forth the morning ray,\nAnd in the infant world began\nTo measure out the cycle of the day.\n\nNow as the evening shadows fall,\nWe gather in your holy name."
            LiturgicalHour.COMPLINE -> "To Thee, before the close of day,\nCreator of the world, we pray,\nThat with Thy wonted mercy, Thou\nWouldst be our Guard and Keeper now.\n\nFrom all ill dreams defend our eyes."
        }
        val hymnTextLatin = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Nocte surgéntes vigilémus omnes,\nsemper in psalmis meditémur,\net viribus totis Dómino canámus\ndúlciter hymnos.\n\nUt pio Regi páriter canéntes\ncum suis sanctis mereámur aulam."
            LiturgicalHour.LAUDS -> "ætérne rerum Cónditor,\nnoctem diémque qui regis,\net témporum das témpora,\nut álleves fastídium.\n\nPræco diéi iam sonat,\nnoctis profúndæ lúminar."
            LiturgicalHour.MIDDAY -> "Rector potens, verax Deus,\nqui témperas rerum vices,\nsplendóre mane ínstruis,\net ígnibus merídiem.\n\nExtíngue flammas lítium,\naufer calórem nóxium."
            LiturgicalHour.VESPERS -> "Lucis Creátor óptime,\nlucem diérum próferens,\nprimórdiis lucis novæ,\nmundi parans oríginem.\n\nQui mane iunctum vésperi\ndiem vocári præcipis."
            LiturgicalHour.COMPLINE -> "Te lucis ante términum,\nRerum Creátor, póscimus,\nUt pro tua cleméntia,\nSis præsul et custódia.\n\nProcul recédant sómnia,\nEt nóctium phantásmata."
        }
        val hymnTextTagalog = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Amang maawain, dinggin ang aming daing\nSa gitna ng kadiliman ng gabi kami'y nanalangin.\nIniaalay aming tinig sa iyong kapurihan,\nIkaw ang umaakay sa aming mga hakbang."
            LiturgicalHour.LAUDS -> "Dakilang Lumikha at Haring Walang Hanggan,\nNa namamahala sa gabi at sa araw,\nNgayon sa pagsikat ng umaga,\nKami'y bumabangon upang magpuri at sumamba."
            LiturgicalHour.MIDDAY -> "Diyos ng katotohanan, Panginoon ng lakas,\nNa nagpapanatili sa pagpapalit ng oras,\nPinupuno mo ang umaga ng karilagan,\nAt ang tanghali ng iyong kaluwalhatian."
            LiturgicalHour.VESPERS -> "O dakilang Lumikha ng liwanag,\nNa nagdala ng sinag ng umaga,\nAt sa simula ng daigdig ay nagtakda,\nNg pag-ikot ng araw at gabi sa aming lupa."
            LiturgicalHour.COMPLINE -> "Sa iyo, bago sumapit ang gabi,\nLumikha ng daigdig, kami'y dumadalangin,\nUpang sa iyong banal na pagkalinga,\nKami ay ingatan at bantayan tuwina."
        }

        // Psalms
        val psalmTitle = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Psalm 1"
            LiturgicalHour.LAUDS -> "Psalm 63:2-9"
            LiturgicalHour.MIDDAY -> "Psalm 119:105-112"
            LiturgicalHour.VESPERS -> "Psalm 110:1-5, 7"
            LiturgicalHour.COMPLINE -> "Psalm 91"
        }
        val psalmSubtitleEnglish = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Two ways of life"
            LiturgicalHour.LAUDS -> "A soul thirsting for God"
            LiturgicalHour.MIDDAY -> "Meditations on the Law of God"
            LiturgicalHour.VESPERS -> "The Messiah, King and Priest"
            LiturgicalHour.COMPLINE -> "Safe beneath the Lord's wings"
        }
        val psalmSubtitleLatin = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Duae viae vitae"
            LiturgicalHour.LAUDS -> "Deus sitit ad te anima mea"
            LiturgicalHour.MIDDAY -> "Lucerna pedibus meis verbum tuum"
            LiturgicalHour.VESPERS -> "Messias Rex et Sacerdos"
            LiturgicalHour.COMPLINE -> "In protectione Altissimi"
        }
        val psalmSubtitleTagalog = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Dalawang daan ng buhay"
            LiturgicalHour.LAUDS -> "Uhaw ang kaluluwa ko sa iyo, o Diyos"
            LiturgicalHour.MIDDAY -> "Tawlaw sa aking mga hakbang ang salita mo"
            LiturgicalHour.VESPERS -> "Ang Mesiyas, Hari at Saserdote"
            LiturgicalHour.COMPLINE -> "Ligtas sa ilalim ng mga pakpak ng Panginoon"
        }

        val antiphonBeforeEnglish = "Let us give glory to God today."
        val antiphonBeforeLatin = "Glorificemus Deum hodie."
        val antiphonBeforeTagalog = "Luwalhatiin natin ang Diyos ngayon."

        val psalmTextEnglish = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Happy indeed is the man\nwho follows not the counsel of the wicked;\nnor lingers in the way of sinners,\nnor sits in the company of scorners,\nbut whose delight is the law of the Lord."
            LiturgicalHour.LAUDS -> "O God, you are my God, for you I long;\nfor you my soul is thirsting.\nMy body pines for you\nlike a dry, weary land without water."
            LiturgicalHour.MIDDAY -> "Your word is a lamp for my steps\nand a light for my path.\nI have sworn and have made up my mind\nto obey your decrees."
            LiturgicalHour.VESPERS -> "The Lord's revelation to my Master:\n'Sit on my right:\nI will put your foes beneath your feet.'\nThe Lord will send from Zion\nyour scepter of power."
            LiturgicalHour.COMPLINE -> "He who dwells in the shelter of the Most High\nand abides in the shade of the Almighty\nsays to the Lord: 'My refuge,\nmy stronghold, my God in whom I trust!'"
        }
        val psalmTextLatin = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Beátus vir, qui non abiit in consílio impiórum,\net in via peccatórum non stetit,\net in cathédra derisórum non sedit,\nsed in lege Dómini voluntas eius."
            LiturgicalHour.LAUDS -> "Deus, Deus meus es tu,\nad te de luce vígilo.\nSitívit in te ánima mea,\nte desiderávit caro mea,\nin terra desérta et árida et inaquósa."
            LiturgicalHour.MIDDAY -> "Lucérna pédibus meis verbum tuum,\net lumen sémitis meis.\nIurávi et státui\ncustodíre iudícia iustítiæ tuæ."
            LiturgicalHour.VESPERS -> "Dixit Dóminus Dómino meo:\n'Sede a dextris meis,\ndonec ponam inimícos tuos\nscabéllum pedum tuórum.'\nVirgam virtútis tuæ emíttet Dóminus ex Sion."
            LiturgicalHour.COMPLINE -> "Qui hábitat in adiutório Altíssimi,\nin protectióne Dei cæli commorábitur.\nDicet Dómino: 'Suscéptor meus es tu\net refúgium meum; Deus meus, sperábo in eum.'"
        }
        val psalmTextTagalog = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Mapalad ang tao na hindi sumusunod sa payo ng masama,\nna hindi nananatili sa daan ng mga makasalanan,\nat hindi nauupo sa upuan ng mga mapanuya,\nkundi ang kanyang kagalakan ay nasa utos ng Panginoon."
            LiturgicalHour.LAUDS -> "O Diyos, ikaw ang aking Diyos, na aking hinahanap,\nuhaw sa iyo ang aking kaluluwa;\nnanabik ang aking katawan sa iyo\nsa lupang tigang, lanta at walang tubig."
            LiturgicalHour.MIDDAY -> "Ang salita mo ay ilawan sa aking mga paa\nat liwanag sa aking landas.\nAko ay nangako at aking tutuparin\nang pagsunod sa iyong mga utos."
            LiturgicalHour.VESPERS -> "Sinabi ng Panginoon sa aking Panginoon:\n'Maupo ka sa aking kanan,\nhanggang sa gawin ko ang iyong mga kaaway\nna tuntungan ng iyong mga paa.'\nIpadadala ng Panginoon mula sa Sion ang setro ng iyong kapangyarihan."
            LiturgicalHour.COMPLINE -> "Siyang naninirahan sa kalinga ng Kataas-taasan\nat nananatili sa lilim ng Makapangyarihan sa lahat\nay magsasabi sa Panginoon: 'Aking kuta, aking tanggulan,\naking Diyos na aking pinagkakatiwalaan!'"
        }

        val psalmsList = listOf(
            PsalmSection(
                title = psalmTitle,
                subtitleEnglish = psalmSubtitleEnglish,
                subtitleLatin = psalmSubtitleLatin,
                subtitleTagalog = psalmSubtitleTagalog,
                antiphonBeforeEnglish = antiphonBeforeEnglish,
                antiphonBeforeLatin = antiphonBeforeLatin,
                antiphonBeforeTagalog = antiphonBeforeTagalog,
                textEnglish = psalmTextEnglish,
                textLatin = psalmTextLatin,
                textTagalog = psalmTextTagalog,
                antiphonAfterEnglish = antiphonBeforeEnglish,
                antiphonAfterLatin = antiphonBeforeLatin,
                antiphonAfterTagalog = antiphonBeforeTagalog,
                rubricText = "Stand or Sit during reciting"
            )
        )

        // Reading reference
        val readingReference = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "1 Peter 1:13-15"
            LiturgicalHour.LAUDS -> "Romans 12:1-2"
            LiturgicalHour.MIDDAY -> "Philippians 4:8-9"
            LiturgicalHour.VESPERS -> "Colossians 3:16"
            LiturgicalHour.COMPLINE -> "1 Thessalonians 5:23"
        }

        val readingTextEnglish = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Set your hope fully on the grace that is coming to you at the revelation of Jesus... Be holy in all your conduct."
            LiturgicalHour.LAUDS -> "Offer your bodies as a living sacrifice, holy and pleasing to God, your spiritual worship."
            LiturgicalHour.MIDDAY -> "Whatever is true, whatever is honorable, whatever is just, whatever is pure... think on these things."
            LiturgicalHour.VESPERS -> "Let the word of Christ dwell in you richly, teaching and admonishing one another in all wisdom."
            LiturgicalHour.COMPLINE -> "May the God of peace himself sanctify you wholly; and may your spirit and soul and body be kept sound."
        }
        val readingTextLatin = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Spem habétote in eam, quæ offértur vobis grátiam in revelatióne Iesu... sicut is, qui vocávit vos, sanctus est."
            LiturgicalHour.LAUDS -> "Exhórto vos, fratres, ut exhibeátis córpora vestra hóstiam vivéntem, sanctam, Deo placéntem, rationábile obséquium vestrum."
            LiturgicalHour.MIDDAY -> "Quæcúmque sunt vera, quæcúmque pudíca, quæcúmque iusta, quæcúmque sancta... hæc cogitáte."
            LiturgicalHour.VESPERS -> "Verbum Christi hábitet in vobis abundánter, in omni sapiéntia docéntes, et commonéntes vosmetípsos."
            LiturgicalHour.COMPLINE -> "Ipse autem Deus pacis sanctíficet vos per ómnia, ut ínteger spíritus vester, et ánima, et corpus sine queréla servétur."
        }
        val readingTextTagalog = when(hour) {
            LiturgicalHour.OFFICE_READINGS -> "Isumite ninyo ang inyong pag-asa sa biyayang paparating sa pahayag ni Hesus... Maging banal kayo sa lahat ng inyong asal."
            LiturgicalHour.LAUDS -> "Iaalay ninyo ang inyong mga katawan bilang buhay na hain, banal at kalugud-lugod sa Diyos, na siya ninyong espirituwal na pagsamba."
            LiturgicalHour.MIDDAY -> "Anumang totoo, anumang marangal, anumang tapat, anumang malinis... isipin ninyo ang mga bagay na ito."
            LiturgicalHour.VESPERS -> "Manahan nawang sagana sa inyo ang salita ni Kristo sa lahat ng karunungan, habang nagtuturo at nagpapaalalahanan kayo sa isa't isa."
            LiturgicalHour.COMPLINE -> "Pakabanalin nawa kayo nang lubos ng Diyos ng kapayapaan; at ingatan ang inyong buong espiritu, kaluluwa, at katawan nang walang bintang."
        }

        val readingResponseEnglish = "Thanks be to God."
        val readingResponseLatin = "Deo gratias."
        val readingResponseTagalog = "Salamat sa Diyos."

        // Versicles
        val responsoryVersicleEnglish = "V: Listen to my prayer, O Lord."
        val responsoryVersicleLatin = "V: Exáudi, Dómine, oratiónem meam."
        val responsoryVersicleTagalog = "V: Dinggin mo ang aking dalangin, Panginoon."

        val responsoryResponseEnglish = "R: And let my cry come to you."
        val responsoryResponseLatin = "R: Et clamor meus ad te véniat."
        val responsoryResponseTagalog = "R: At makarating sa iyo ang aking dambana."

        // Gospel Canticle
        val canticleTitleEnglish = when(hour) {
            LiturgicalHour.LAUDS -> "Canticle of Zechariah (Benedictus)"
            LiturgicalHour.OFFICE_READINGS -> "Te Deum (Hymn of Praise)"
            LiturgicalHour.MIDDAY -> "Our Sustenance in Midday"
            LiturgicalHour.VESPERS -> "Canticle of Mary (Magnificat)"
            LiturgicalHour.COMPLINE -> "Canticle of Simeon (Nunc Dimittis)"
        }
        val canticleTitleLatin = when(hour) {
            LiturgicalHour.LAUDS -> "Canticum Zachariae (Benedictus)"
            LiturgicalHour.OFFICE_READINGS -> "Te Deum Laudamus"
            LiturgicalHour.MIDDAY -> "Ad Sextam Canticum"
            LiturgicalHour.VESPERS -> "Canticum Beatæ Mariæ Virginis (Magnificat)"
            LiturgicalHour.COMPLINE -> "Canticum Simeonis (Nunc Dimittis)"
        }
        val canticleTitleTagalog = when(hour) {
            LiturgicalHour.LAUDS -> "Awit ni Zacarias (Benedictus)"
            LiturgicalHour.OFFICE_READINGS -> "Awit ng Pagpupuri (Te Deum)"
            LiturgicalHour.MIDDAY -> "Tawlaw sa Araw"
            LiturgicalHour.VESPERS -> "Awit ni Maria (Magnificat)"
            LiturgicalHour.COMPLINE -> "Awit ni Simeon (Nunc Dimittis)"
        }

        val canticleAntiphonEnglish = "Blessed be the Lord."
        val canticleAntiphonLatin = "Benedictus Dominus."
        val canticleAntiphonTagalog = "Purihin ang Panginoon."

        val canticleTextEnglish = when(hour) {
            LiturgicalHour.LAUDS -> "Blessed be the Lord, the God of Israel;\nhe has come to his people and set them free.\nHe has raised up for us a mighty savior,\nborn of the house of his servant David."
            LiturgicalHour.VESPERS -> "My soul proclaims the greatness of the Lord,\nmy spirit rejoices in God my Savior;\nfor he has looked with favor on his lowly servant.\nFrom this day all generations will call me blessed."
            LiturgicalHour.COMPLINE -> "Protect us, Lord, while we are awake;\nwatch over us while we sleep,\nthat we may keep watch with Christ\nand rest in his peace."
            else -> "We praise you, O God, we acclaim you as Lord;\nall creation worships you, Father everlasting.\nTo you all angels, all the powers of heaven,\ncherubim and seraphim, sing in endless praise."
        }
        val canticleTextLatin = when(hour) {
            LiturgicalHour.LAUDS -> "Benedíctus Dóminus Deus Israel,\nquia visitávit et fecit redemptiónem plebi suæ;\net eréxit cornu salútis nobis\nin domo David púeri sui."
            LiturgicalHour.VESPERS -> "Magníficat ánima mea Dóminum,\net exsultávit spíritus meus in Deo salvatóre meo,\nquia respéxit humilitátem ancíllæ suæ.\nEcce enim ex hoc beátam me dicent omnes generatiónes."
            LiturgicalHour.COMPLINE -> "Salva nos, Dómine, vigilántes,\ncustódi nos dormiéntes,\nut vigilémus cum Christo\net requiescámus in pace."
            else -> "Te Deum laudámus: te Dóminum confitémur.\nTe ætérnum Patrem omnis terra venerátur.\nTibi omnes Angeli, tibi cæli et univérsæ potéstates."
        }
        val canticleTextTagalog = when(hour) {
            LiturgicalHour.LAUDS -> "Purihin ang Panginoon, ang Diyos ng Israel;\nsinuri niya at iniligtas ang kanyang bayan.\nNagpatayo siya para sa atin ng isang makapangyarihang tagapagligtas,\nmula sa lahi ng kanyang lingkod na si David."
            LiturgicalHour.VESPERS -> "Ang aking kaluluwa ay nagpupuri sa Panginoon,\nat nagagalak ang aking espiritu sa Diyos na aking Tagapagligtas;\nsapagkat nilingap niya ang kababaan ng kanyang lingkod.\nMula ngayon, tatawagin akong mapalad ng lahat ng salinlahi."
            LiturgicalHour.COMPLINE -> "Iligtas mo kami, Panginoon, habang kami ay gising;\nbantayan mo kami habang natutulog,\nupang kami ay makapagbantay kasama ni Kristo\nat makapagpahinga sa kanyang kapayapaan."
            else -> "Pinupuri ka namin, O Diyos; ipinahahayag ka naming Panginoon.\nSinasamba ka ng buong daigdig, Amang walang hanggan.\nSa iyo ang lahat ng anghel, ang lahat ng kapangyarihan sa langit."
        }

        // Intercessions
        val intercessionsPrefaceEnglish = "Let us call upon our Lord, who guides his people in love."
        val intercessionsPrefaceLatin = "Invocémus Dóminum Iesum, qui dírigit plebem suam in caritáte."
        val intercessionsPrefaceTagalog = "Tawagan natin ang ating Panginoon, na umaakay sa kanyang bayan nang may pagmamahal."

        val intercessionsResponseEnglish = "Lord, hear our prayer."
        val intercessionsResponseLatin = "Te rogámus, audi nos."
        val intercessionsResponseTagalog = "Panginoon, dinggin mo ang aming panalangin."

        val intercessionsList = listOf(
            IntercessionItem(
                petitionEnglish = "Grant peace to your holy Church throughout the world.",
                petitionLatin = "Da pacem Ecclésiæ tuæ sanctæ super terram.",
                petitionTagalog = "Ipagkaloob mo ang kapayapaan sa iyong banal na Simbahan sa buong daigdig.",
                responseEnglish = "Lord, hear our prayer.",
                responseLatin = "Te rogámus, audi nos.",
                responseTagalog = "Panginoon, dinggin mo ang aming panalangin."
            )
        )

        val lordPrayerEnglish = "Our Father, who art in heaven, hallowed be thy name; thy kingdom come; thy will be done on earth as it is in heaven. Give us this day our daily bread; and forgive us our trespasses as we forgive those who trespass against us; and lead us not into temptation, but deliver us from evil. Amen."
        val lordPrayerLatin = "Pater noster, qui es in cælis: sanctificétur nomen tuum; advéniat regnum tuum; fiat volúntas tua, sicut in cælo, et in terra. Panem nostrum cotidiánum da nobis hódie; et dimítte nobis débita nostra, sicut et nos dimíttimus debitóribus nostris; et ne nos indúcas in tentatiónem; sed líbera nos a malo. Amen."
        val lordPrayerTagalog = "Ama namin, sumasalangit ka, sambahin ang ngalan mo. Mapasaamin ang kaharian mo, sundin ang loob mo dito sa lupa para nang sa langit. Bigyan mo kami ngayon ng aming kakanin sa araw-araw; at patawarin mo kami sa aming mga sala, para nang pagpapatawad namin sa mga nagkakasala sa amin; at huwag mo kaming ipahintulot sa tukso, kundi iadya mo kami sa lahat ng masama. Amen."

        // Concluding prayer
        val closingPrayerEnglish = cbcpDay.customConcludingPrayer ?: "Look with favor, Lord, on this day's prayers. Let your light shine upon us and guide us in your ways of peace and truth. We ask this through Christ our Lord. Amen."
        val closingPrayerLatin = "Exáudi, quæsumus, Dómine, preces nostras, et lumen vultus tui super nos fúlgere concéde, ut in pace et veritáte vias tuas prosequámur. Per Christum Dóminum nostrum. Amen."
        val closingPrayerTagalog = cbcpDay.customConcludingPrayer ?: "Lumingap ka, Panginoon, sa mga panalangin ng araw na ito. Pagsikatin mo ang iyong liwanag sa amin at akayin kami sa iyong daan ng kapayapaan at katotohanan. Sa pamamagitan ni Hesukristo aming Panginoon. Amen."

        // Standard dismissal (Wait! If Solo mode is active, this will be lay blessing)
        val dismissalEnglish = "May the Lord bless us, protect us from all evil, and bring us to everlasting life. Amen."
        val dismissalLatin = "Nos benedícat Dóminus, et ab omni malo deféndat, et dedúcat ad vitam ætérnam. Amen."
        val dismissalTagalog = "Pagpalain nawa tayo ng Panginoon, iligtas tayo sa rurok ng kasamaan at ihatid tayo sa buhay na walang hanggan. Amen."

        return LiturgicalPrayer(
            title = title,
            dateString = dateString,
            hostDay = hostDay,
            season = seasonText,
            titleEnglish = hour.displayName,
            titleLatin = hour.latinName,
            titleTagalog = when(hour) {
                LiturgicalHour.OFFICE_READINGS -> "Tanggapan ng Pagbasa"
                LiturgicalHour.LAUDS -> "Panalanging Pang-umaga"
                LiturgicalHour.MIDDAY -> "Panalangin sa Tanghali"
                LiturgicalHour.VESPERS -> "Panalanging Pampanom"
                LiturgicalHour.COMPLINE -> "Panalangin sa Gabi"
            },
            seasonEnglish = cbcpDay.season,
            seasonLatin = "Tempus Liturgicum",
            seasonTagalog = "Panahon ng Liturhiya",
            openingVerseCallEnglish = openingVerseCallEnglish,
            openingVerseCallLatin = openingVerseCallLatin,
            openingVerseCallTagalog = openingVerseCallTagalog,
            openingVerseResponseEnglish = openingVerseResponseEnglish,
            openingVerseResponseLatin = openingVerseResponseLatin,
            openingVerseResponseTagalog = openingVerseResponseTagalog,
            invitatoryAntiphonEnglish = invitatoryAntiphonEnglish,
            invitatoryAntiphonLatin = invitatoryAntiphonLatin,
            invitatoryAntiphonTagalog = invitatoryAntiphonTagalog,
            hymnTitleEnglish = hymnTitleEnglish,
            hymnTitleLatin = hymnTitleLatin,
            hymnTitleTagalog = hymnTitleTagalog,
            hymnTextEnglish = hymnTextEnglish,
            hymnTextLatin = hymnTextLatin,
            hymnTextTagalog = hymnTextTagalog,
            psalms = psalmsList,
            readingReference = readingReference,
            readingTextEnglish = readingTextEnglish,
            readingTextLatin = readingTextLatin,
            readingTextTagalog = readingTextTagalog,
            readingResponseEnglish = readingResponseEnglish,
            readingResponseLatin = readingResponseLatin,
            readingResponseTagalog = readingResponseTagalog,
            responsoryVersicleEnglish = responsoryVersicleEnglish,
            responsoryVersicleLatin = responsoryVersicleLatin,
            responsoryVersicleTagalog = responsoryVersicleTagalog,
            responsoryResponseEnglish = responsoryResponseEnglish,
            responsoryResponseLatin = responsoryResponseLatin,
            responsoryResponseTagalog = responsoryResponseTagalog,
            canticleAntiphonBeforeEnglish = canticleAntiphonEnglish,
            canticleAntiphonBeforeLatin = canticleAntiphonLatin,
            canticleAntiphonBeforeTagalog = canticleAntiphonTagalog,
            canticleTitleEnglish = canticleTitleEnglish,
            canticleTitleLatin = canticleTitleLatin,
            canticleTitleTagalog = canticleTitleTagalog,
            canticleTextEnglish = canticleTextEnglish,
            canticleTextLatin = canticleTextLatin,
            canticleTextTagalog = canticleTextTagalog,
            canticleAntiphonAfterEnglish = canticleAntiphonEnglish,
            canticleAntiphonAfterLatin = canticleAntiphonLatin,
            canticleAntiphonAfterTagalog = canticleAntiphonTagalog,
            intercessionsPrefaceEnglish = intercessionsPrefaceEnglish,
            intercessionsPrefaceLatin = intercessionsPrefaceLatin,
            intercessionsPrefaceTagalog = intercessionsPrefaceTagalog,
            intercessionsResponseEnglish = intercessionsResponseEnglish,
            intercessionsResponseLatin = intercessionsResponseLatin,
            intercessionsResponseTagalog = intercessionsResponseTagalog,
            intercessionsList = intercessionsList,
            lordPrayerEnglish = lordPrayerEnglish,
            lordPrayerLatin = lordPrayerLatin,
            lordPrayerTagalog = lordPrayerTagalog,
            closingPrayerEnglish = closingPrayerEnglish,
            closingPrayerLatin = closingPrayerLatin,
            closingPrayerTagalog = closingPrayerTagalog,
            dismissalEnglish = dismissalEnglish,
            dismissalLatin = dismissalLatin,
            dismissalTagalog = dismissalTagalog,
            rubricHymn = "Sit during the hymn",
            rubricPsalm = "Sit during the psalms",
            rubricReading = "Sit during the reading",
            rubricCanticle = "Stand during the canticle",
            rubricIntercessions = "Stand for petition",
            rubricClosing = "Stand for closing pray"
        )
    }
}

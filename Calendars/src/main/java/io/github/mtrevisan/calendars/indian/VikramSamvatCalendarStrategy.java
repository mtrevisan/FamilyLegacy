/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Vikram Samvat, the lunisolar calendar of northern and western India (drik, with the Lahiri ayanamsa), e.g.
 * "Vikram Samvat 2080 Chaitra Shukla 5", "VS 2080 Adhika Shravana Krishna 11", "2081 Kartika vadi 14",
 * "kartikadi 2080 Kartika sudi 1".
 * <ul>
 *   <li>The (amanta) months run from new moon to new moon, and are named after the sidereal sign the Sun enters during
 *   the month (Chaitra contains the entry into Mesha); a month in which the Sun enters no sign is a leap month (adhika)
 *   and takes the name of the following one (nija).</li>
 *   <li>A day bears the tithi (lunar day, a 12° step of the elongation of the Moon) that prevails at sunrise in Ujjain;
 *   a tithi that covers two sunrises (vriddhi) stands for the first day, one that covers none (kshaya) for the day in
 *   which it falls.</li>
 *   <li>The year (the Gregorian one + 57) starts on Chaitra Shukla Pratipada (chaitradi), or, in Gujarat, on Kartika
 *   Shukla Pratipada (kartikadi, "kartikadi" in the expression).</li>
 *   <li>The months are purnimanta (from full moon to full moon, the dark fortnight first, as in the north) unless the
 *   expression says "amanta" (or kartikadi): the dark fortnight of a purnimanta month is the one of the preceding
 *   amanta month.</li>
 * </ul>
 */
public class VikramSamvatCalendarStrategy implements CalendarConverterPlugin{

	/** Ujjain, the prime meridian of Indian astronomy. */
	private static final double UJJAIN_LATITUDE = 23.1765;
	private static final double UJJAIN_LONGITUDE = 75.7885;

	/** The difference between the Vikram Samvat year and the Gregorian year in which it starts. */
	private static final int ERA_OFFSET = 57;
	private static final int CHAITRA = 0;
	private static final int ASHVINA = 6;
	private static final int KARTIKA = 7;
	private static final int TITHIS_PER_PAKSHA = 15;
	private static final double DEGREES_PER_TITHI = 12.;
	private static final double SYNODIC_MONTH = 29.530588853;

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	private static final Map<String, Integer> TITHIS = new HashMap<>();
	static{
		registerMonth(0, "Chaitra", "Chait", "Chaitr", "Chaitar", "चैत्र", "चैत");
		registerMonth(1, "Vaishakha", "Vaisakha", "Vaishakh", "Baisakh", "Baishakh", "Vaisakh", "वैशाख", "बैसाख");
		registerMonth(2, "Jyeshtha", "Jyaishtha", "Jyestha", "Jeth", "Jeshtha", "ज्येष्ठ", "जेठ");
		registerMonth(3, "Ashadha", "Asadha", "Ashadh", "Asadh", "Ashad", "Asarh", "आषाढ़", "आषाढ", "असाढ़");
		registerMonth(4, "Shravana", "Sravana", "Shravan", "Sawan", "Saawan", "Savan", "श्रावण", "सावन");
		registerMonth(5, "Bhadrapada", "Bhadrapad", "Bhadra", "Bhadon", "Bhado", "Bhadarvo", "भाद्रपद", "भादों", "भादो");
		registerMonth(6, "Ashvina", "Ashwin", "Asvina", "Ashwina", "Aswin", "Asoj", "Aaso", "Kwar", "आश्विन", "आसोज", "क्वार");
		registerMonth(7, "Kartika", "Kartik", "Karthik", "Kartak", "कार्तिक");
		registerMonth(8, "Margashirsha", "Margasirsha", "Margashirsh", "Agrahayana", "Aghan", "Magshar", "Margasira",
			"मार्गशीर्ष", "अगहन");
		registerMonth(9, "Pausha", "Paush", "Posh", "Pus", "Poush", "पौष", "पूस");
		registerMonth(10, "Magha", "Magh", "माघ");
		registerMonth(11, "Phalguna", "Phalgun", "Phagun", "Falgun", "Fagan", "फाल्गुन", "फागुन");

		registerTithi(1, "Pratipada", "Pratipat", "Prathama", "Padva", "Padwa", "Ekam", "Padvo", "प्रतिपदा", "पड़वा", "एकम");
		registerTithi(2, "Dwitiya", "Dvitiya", "Duj", "Bij", "Beej", "द्वितीया", "दूज");
		registerTithi(3, "Tritiya", "Trutiya", "Teej", "Trij", "तृतीया", "तीज");
		registerTithi(4, "Chaturthi", "Chauth", "Choth", "चतुर्थी", "चौथ");
		registerTithi(5, "Panchami", "Pancham", "पंचमी", "पञ्चमी");
		registerTithi(6, "Shashthi", "Shashti", "Sashti", "Chhath", "Chhatth", "षष्ठी", "छठ");
		registerTithi(7, "Saptami", "Satam", "सप्तमी");
		registerTithi(8, "Ashtami", "Atham", "अष्टमी");
		registerTithi(9, "Navami", "Nom", "नवमी");
		registerTithi(10, "Dashami", "Dasami", "Dasham", "दशमी");
		registerTithi(11, "Ekadashi", "Ekadasi", "Gyaras", "Agiyaras", "एकादशी", "ग्यारस");
		registerTithi(12, "Dwadashi", "Dvadashi", "Dvadasi", "Baras", "द्वादशी", "बारस");
		registerTithi(13, "Trayodashi", "Trayodasi", "Teras", "त्रयोदशी", "तेरस");
		registerTithi(14, "Chaturdashi", "Chaturdasi", "Chaudas", "चतुर्दशी", "चौदस");
		registerTithi(15, "Purnima", "Poornima", "Purnamasi", "Punam", "Poonam", "Pournami", "पूर्णिमा", "पूनम");
		registerTithi(30, "Amavasya", "Amavasi", "Amavas", "Amas", "अमावस्या", "अमावस");
	}

	private static final Set<String> SHUKLA = words("Shukla", "Sukla", "Sudi", "Sud", "Shudi", "Shuddha", "Sudhi",
		"शुक्ल", "सुदी", "सुद");
	private static final Set<String> KRISHNA = words("Krishna", "Krsna", "Vadi", "Vad", "Badi", "Bahula", "Bahul",
		"कृष्ण", "वदी", "बदी", "वद");
	private static final Set<String> ADHIKA = words("Adhika", "Adhik", "Adhic", "Mala", "Purushottam", "Purushottama",
		"Purushottam", "अधिक", "मल", "पुरुषोत्तम");
	private static final Set<String> NIJA = words("Nija", "Nij", "Shuddha", "निज");
	private static final Set<String> AMANTA = words("Amanta", "Amavasyant", "Amavasyanta", "अमान्त");
	private static final Set<String> PURNIMANTA = words("Purnimanta", "Purnimant", "Pournimanta", "पूर्णिमान्त");
	private static final Set<String> KARTIKADI = words("Kartikadi", "Gujarati", "कार्तिकादि");
	private static final Set<String> FILLERS = words("Vikram", "Vikrami", "Vikrama", "Samvat", "Sambat", "Samvata", "VS",
		"V", "S", "Paksha", "Paksh", "Tithi", "Masa", "Maas", "Mas", "Month", "of", "the", "विक्रम", "संवत्", "संवत", "वि",
		"सं", "पक्ष", "तिथि", "मास");

	private static void registerMonth(final int month, final String... names){
		for(final String name : names)
			MONTHS.put(TextNormalizer.normalize(name), month);
	}

	private static void registerTithi(final int tithi, final String... names){
		for(final String name : names)
			TITHIS.put(TextNormalizer.normalize(name), tithi);
	}

	private static Set<String> words(final String... words){
		final Set<String> set = new java.util.HashSet<>();
		for(final String word : words)
			set.add(TextNormalizer.normalize(word));
		return set;
	}


	/** An amanta month: from new moon to new moon. */
	private record LunarMonth(double newMoon, double nextNewMoon, int name, boolean adhika){}

	/** The amanta months of each chaitradi year. */
	private static final Map<Integer, List<LunarMonth>> YEARS = new ConcurrentHashMap<>();


	/** A parsed date. */
	private record LunisolarDate(int year, int month, boolean adhika, boolean krishna, int tithi, boolean amanta,
		boolean kartikadi){}


	@Override
	public long parseToJdn(final String rawDate){
		final LunisolarDate date = parse(rawDate);

		// the chaitradi year of the month
		final int year = (date.kartikadi() && date.month() != -1 && date.month() <= ASHVINA? date.year() + 1: date.year());
		final List<LunarMonth> months = YEARS.computeIfAbsent(year, VikramSamvatCalendarStrategy::computeYear);

		if(date.month() == -1){
			// a year alone starts on Chaitra (or Kartika) Shukla Pratipada
			final LunarMonth first = (date.kartikadi()? find(months, KARTIKA, false, rawDate): months.getFirst());
			return dayOfTithi(first, 1);
		}

		final LunarMonth month;
		int tithi = (date.tithi() != 0? date.tithi(): 1);
		final boolean krishna = (date.krishna() || date.tithi() == 0 && !date.amanta() && !date.adhika());
		if(krishna && !date.amanta() && !date.adhika()){
			// the dark fortnight of a purnimanta month is the one of the preceding amanta month
			month = precedingAmanta(months, date.month(), rawDate);
		}
		else
			month = find(months, date.month(), date.adhika(), rawDate);

		final int amantaTithi = (tithi == 30? 30: (krishna? TITHIS_PER_PAKSHA + tithi: tithi));
		if(amantaTithi < 1 || amantaTithi > 30 || !krishna && tithi > TITHIS_PER_PAKSHA)
			throw new IllegalArgumentException("Invalid tithi: " + rawDate);
		return dayOfTithi(month, amantaTithi);
	}

	private static LunarMonth find(final List<LunarMonth> months, final int name, final boolean adhika,
			final String rawDate){
		for(final LunarMonth month : months)
			if(month.name() == name && month.adhika() == adhika)
				return month;
		throw new IllegalArgumentException("The " + (adhika? "adhika ": StringUtils.EMPTY) + "month does not exist in the year: " + rawDate);
	}

	/**
	 * @return the amanta month whose dark fortnight belongs to the purnimanta (nija) month with the given name
	 */
	private static LunarMonth precedingAmanta(final List<LunarMonth> months, final int name, final String rawDate){
		if(name == CHAITRA)
			// the dark fortnight of Chaitra closes the year
			return months.getLast();

		for(int i = 1; i < months.size(); i ++)
			if(months.get(i).name() == name && !months.get(i).adhika()){
				int previous = i - 1;
				// an adhika month with the same name lies between the two halves of the nija month
				if(months.get(previous).adhika() && months.get(previous).name() == name && previous > 0)
					previous --;
				return months.get(previous);
			}
		throw new IllegalArgumentException("The month does not exist in the year: " + rawDate);
	}

	/**
	 * @param month  the amanta month
	 * @param tithi  the tithi from the new moon (1 to 30)
	 * @return the civil day bearing the tithi at sunrise (the first one for a tithi on two sunrises, the day in which it
	 * 	falls for a tithi on no sunrise)
	 */
	private static long dayOfTithi(final LunarMonth month, final int tithi){
		final double start = (tithi == 1? month.newMoon(): HinduAstronomy.elongationEvent(month.newMoon(),
			(tithi - 1) * DEGREES_PER_TITHI));
		final double end = (tithi == 30? month.nextNewMoon(): HinduAstronomy.elongationEvent(start, tithi * DEGREES_PER_TITHI));

		final long dayOfStart = sunriseDay(start);
		final long nextDay = dayOfStart + 1;
		return (sunrise(nextDay) < end? nextDay: dayOfStart);
	}

	/**
	 * @return the civil day whose sunrise (in Ujjain) precedes the instant, the next sunrise following it
	 */
	private static long sunriseDay(final double jd){
		final long day = HinduAstronomy.civilDay(jd);
		return (sunrise(day) <= jd? day: day - 1);
	}

	private static double sunrise(final long day){
		return HinduAstronomy.sunriseSunset(day, UJJAIN_LATITUDE, UJJAIN_LONGITUDE)[0];
	}

	/**
	 * Computes the amanta months of a chaitradi year, from the first Chaitra (adhika if any) to the last month before the
	 * next year.
	 */
	private static List<LunarMonth> computeYear(final int year){
		final double start = yearStart(year);
		final double nextStart = yearStart(year + 1);

		final EphemerisEngine engine = HinduAstronomy.engine();
		final List<LunarMonth> months = new ArrayList<>();
		double newMoon = start;
		while(newMoon < nextStart - 1.){
			final double nextNewMoon = engine.findNextNewMoon(newMoon + 1.);
			final int signAtStart = sign(newMoon);
			final int signAtEnd = sign(nextNewMoon);
			// named after the sign entered during the month; no sign entered: leap month, named after the next one
			months.add(new LunarMonth(newMoon, nextNewMoon, (signAtStart + 1) % 12, signAtStart == signAtEnd));
			newMoon = nextNewMoon;
		}
		return List.copyOf(months);
	}

	/**
	 * @return the new moon that starts the year (the start of Chaitra, adhika if any)
	 */
	private static double yearStart(final int year){
		final int gregorianYear = year - ERA_OFFSET;
		final double meshaSankranti = HinduAstronomy.sankranti(GregorianCalendarStrategy.toJdn(gregorianYear, 4, 14), 0);

		// the new moon that starts the month containing the sankranti (nija Chaitra)
		final EphemerisEngine engine = HinduAstronomy.engine();
		double newMoon = engine.findNextNewMoon(meshaSankranti - SYNODIC_MONTH - 2.);
		double next = engine.findNextNewMoon(newMoon + 1.);
		while(next <= meshaSankranti){
			newMoon = next;
			next = engine.findNextNewMoon(newMoon + 1.);
		}
		// an adhika Chaitra precedes it if the Sun enters no sign in the previous month
		final double previous = engine.findNextNewMoon(newMoon - SYNODIC_MONTH - 2.);
		return (sign(previous) == sign(newMoon)? previous: newMoon);
	}

	private static int sign(final double jd){
		return (int)(HinduAstronomy.siderealSunLongitude(jd) / 30.);
	}

	private static LunisolarDate parse(final String rawDate){
		int year = 0;
		int month = -1;
		boolean adhika = false;
		boolean krishna = false;
		boolean pakshaGiven = false;
		int tithi = 0;
		boolean amanta = false;
		boolean kartikadi = false;
		final String[] words = TextNormalizer.tokenize(rawDate);
		for(final String word : words){
			if(FILLERS.contains(word) || NIJA.contains(word) || PURNIMANTA.contains(word))
				continue;

			if(ADHIKA.contains(word))
				adhika = true;
			else if(AMANTA.contains(word))
				amanta = true;
			else if(KARTIKADI.contains(word)){
				kartikadi = true;
				amanta = true;
			}
			else if(SHUKLA.contains(word))
				pakshaGiven = true;
			else if(KRISHNA.contains(word)){
				krishna = true;
				pakshaGiven = true;
			}
			else if(MONTHS.containsKey(word))
				month = MONTHS.get(word);
			else if(TITHIS.containsKey(word))
				tithi = TITHIS.get(word);
			else if(StringUtils.isNumeric(word)){
				final int number = Integer.parseInt(word);
				if(number > 30 && year == 0)
					year = number;
				else if(tithi == 0)
					tithi = number;
				else
					throw new IllegalArgumentException("Invalid number '" + word + "' in the Vikram Samvat date: " + rawDate);
			}
			else
				throw new IllegalArgumentException("Unknown word '" + word + "' in the Vikram Samvat date: " + rawDate);
		}
		if(year == 0)
			throw new IllegalArgumentException("Missing year in the Vikram Samvat date: " + rawDate);
		if(tithi != 0 && month == -1)
			throw new IllegalArgumentException("Missing month in the Vikram Samvat date: " + rawDate);
		// a tithi numbered from the new moon (16 to 30) is in the dark fortnight; Amavasya is the 30th
		if(tithi > TITHIS_PER_PAKSHA && tithi < 30 && pakshaGiven)
			throw new IllegalArgumentException("A fortnight has 15 tithis: " + rawDate);
		if(tithi > TITHIS_PER_PAKSHA && tithi < 30){
			krishna = true;
			tithi -= TITHIS_PER_PAKSHA;
		}
		else if(tithi == 30)
			krishna = true;
		else if(tithi == TITHIS_PER_PAKSHA && !pakshaGiven)
			krishna = false;
		return new LunisolarDate(year, month, adhika, krishna, tithi, amanta, kartikadi);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final LunisolarDate date = parse(rawDate);
		return CalendarMetadata.createLunisolar(date.month(), date.tithi());
	}

}

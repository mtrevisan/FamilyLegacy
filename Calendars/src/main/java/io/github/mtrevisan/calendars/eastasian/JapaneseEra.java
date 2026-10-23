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
package io.github.mtrevisan.calendars.eastasian;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * The Japanese era names (元号) from Tenshō (1573).
 * <p>
 * The year 1 of an era is the whole lunar year in which the era was proclaimed (so the last year of an era and the
 * first one of the next coincide); Meiji is officially counted from 明治元年1月1日 (1868-01-25). Sources: ja.wikipedia
 * 「元号一覧 (日本)」, en.wikipedia "List of Japanese era names", and the National Astronomical Observatory of Japan.
 */
public enum JapaneseEra{

	TENSHO("天正", "Tenshō", 1573),
	BUNROKU("文禄", "Bunroku", 1592),
	KEICHO("慶長", "Keichō", 1596, "Kyōchō"),
	GENNA("元和", "Genna", 1615, "Genwa"),
	KANEI("寛永", "Kan'ei", 1624),
	SHOHO("正保", "Shōhō", 1644),
	KEIAN("慶安", "Keian", 1648, "Kyōan"),
	JOO("承応", "Jōō", 1652, "Shōō"),
	MEIREKI("明暦", "Meireki", 1655, "Myōryaku", "Meiryaku"),
	MANJI("万治", "Manji", 1658, "萬治"),
	KANBUN("寛文", "Kanbun", 1661),
	ENPO("延宝", "Enpō", 1673, "Enhō"),
	TENNA("天和", "Tenna", 1681, "Tenwa"),
	JOKYO("貞享", "Jōkyō", 1684),
	GENROKU("元禄", "Genroku", 1688),
	HOEI("宝永", "Hōei", 1704, "寶永"),
	SHOTOKU("正徳", "Shōtoku", 1711),
	KYOHO("享保", "Kyōhō", 1716),
	GENBUN("元文", "Genbun", 1736),
	KANPO("寛保", "Kanpō", 1741, "Kanhō"),
	ENKYO("延享", "Enkyō", 1744),
	KANEN("寛延", "Kan'en", 1748),
	HOREKI("宝暦", "Hōreki", 1751, "Hōryaku", "寶暦"),
	MEIWA("明和", "Meiwa", 1764, "Myōwa"),
	ANEI("安永", "An'ei", 1772),
	TENMEI("天明", "Tenmei", 1781),
	KANSEI("寛政", "Kansei", 1789),
	KYOWA("享和", "Kyōwa", 1801),
	BUNKA("文化", "Bunka", 1804),
	BUNSEI("文政", "Bunsei", 1818),
	TENPO("天保", "Tenpō", 1830, "Tenhō"),
	KOKA("弘化", "Kōka", 1844),
	KAEI("嘉永", "Kaei", 1848),
	ANSEI("安政", "Ansei", 1854),
	MANEN("万延", "Man'en", 1860, "萬延"),
	BUNKYU("文久", "Bunkyū", 1861),
	GENJI("元治", "Genji", 1864),
	KEIO("慶応", "Keiō", 1865, "慶應"),
	/** Lunisolar up to 明治5年12月2日 (1872-12-31), Gregorian from 明治6年1月1日 (1873-01-01). */
	MEIJI("明治", "Meiji", 1868, "M"),
	TAISHO("大正", "Taishō", 1912, "T"),
	SHOWA("昭和", "Shōwa", 1926, "S"),
	HEISEI("平成", "Heisei", 1989, "H"),
	REIWA("令和", "Reiwa", 2019, "R");


	private final String kanji;
	private final String romanization;
	/** The (lunar, up to Meiji) year in which the year 1 of the era falls. */
	private final int firstYear;
	private final String[] variants;


	JapaneseEra(final String kanji, final String romanization, final int firstYear, final String... variants){
		this.kanji = kanji;
		this.romanization = romanization;
		this.firstYear = firstYear;
		this.variants = variants;
	}


	public String getKanji(){
		return kanji;
	}

	/**
	 * @return the (lunar, up to Meiji) year in which the year 1 of the era falls
	 */
	public int getFirstYear(){
		return firstYear;
	}

	/**
	 * @param yearOfEra the year of the era (from 1)
	 * @return the year (lunar, up to Meiji 5), numbered as the Gregorian year in which it starts
	 */
	public int toYear(final int yearOfEra){
		return firstYear + yearOfEra - 1;
	}

	/**
	 * @return the last year of the era (the first one of the next era), or {@link Integer#MAX_VALUE} for the current era
	 */
	public int getLastYear(){
		final JapaneseEra[] eras = values();
		return (ordinal() + 1 < eras.length? eras[ordinal() + 1].firstYear: Integer.MAX_VALUE);
	}

	/**
	 * @return whether the era is counted with the modern (from 1873, Gregorian) calendar only
	 */
	public boolean isModern(){
		return (ordinal() > MEIJI.ordinal());
	}

	/**
	 * The names of every era (kanji, romanizations with and without macrons, long vowels as "ou", "n" as "m" before
	 * labials), lowercase, mapped to the era name.
	 *
	 * @return the aliases
	 */
	static Map<String, String> aliases(){
		final Map<String, String> aliases = new HashMap<>();
		final JapaneseEra[] eras = values();
		for(final JapaneseEra era : eras){
			final List<String> names = new ArrayList<>();
			names.add(era.kanji);
			names.add(era.romanization);
			names.addAll(List.of(era.variants));
			for(final String name : names){
				final List<String> aliasList = romanizationVariants(name);
				for(final String alias : aliasList)
					aliases.put(alias, era.name());
			}
		}
		return aliases;
	}

	private static List<String> romanizationVariants(final String name){
		final String lower = name.toLowerCase(Locale.ROOT);
		final String withoutApostrophe = lower.replace("'", "").replace("’", "");
		final String plain = Normalizer.normalize(withoutApostrophe, Normalizer.Form.NFD)
			.replaceAll("[\\u0300-\\u036f]", "");
		final String longVowels = withoutApostrophe.replace("ō", "ou").replace("ū", "uu");
		final String labials = plain.replaceAll("n([pbm])", "m$1");
		return List.of(lower, withoutApostrophe, plain, longVowels, labials);
	}

}

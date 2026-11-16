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
package io.github.mtrevisan.calendars.gregorian;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;


/**
 * Reads the names of the months from the Java locales, all the locales at once on the available processors (the first
 * lookup of a locale loads its data, and there are some hundreds of them).
 * <p>
 * It is a class of its own, and not a part of {@link MonthNames}, because the worker threads cannot run the code of a class
 * whose initialization is in progress on another thread (the names are needed by the initialization of {@link MonthNames}).
 * </p>
 */
final class MonthNameLoader{

	/** The words of each month: the full and the short one, for each month, in the lowercase. */
	static final int WORDS_PER_LOCALE = 2 * Month.values().length;


	private MonthNameLoader(){}


	/**
	 * @param locales the locales
	 * @return for each locale, the full and the short name of January, then of February, and so on (in lowercase)
	 */
	static String[][] load(final List<Locale> locales){
		final String[][] names = new String[locales.size()][];
		IntStream.range(0, names.length)
			.parallel()
			.forEach(i -> names[i] = wordsOf(locales.get(i)));
		return names;
	}

	private static String[] wordsOf(final Locale locale){
		final String[] words = new String[WORDS_PER_LOCALE];
		int index = 0;
		for(final Month month : Month.values()){
			words[index ++] = month.getDisplayName(TextStyle.FULL, locale).toLowerCase(Locale.ROOT);
			words[index ++] = month.getDisplayName(TextStyle.SHORT, locale).toLowerCase(Locale.ROOT);
		}
		return words;
	}

}

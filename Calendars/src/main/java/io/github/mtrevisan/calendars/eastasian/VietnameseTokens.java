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

import java.util.Map;
import java.util.Set;


/** The tokens of the grammar of the Vietnamese dates, defined once and matched after the normalization of the text (lowercase, no accents). */
final class VietnameseTokens{

	static final String DAY = "ngay";
	static final Set<String> DAY_PREFIXES = Set.of("ngay", "mung", "mong");
	static final String MONTH = "thang";
	static final String YEAR = "nam";
	static final String LEAP = "nhuan";
	static final String FULL_MOON_DAY = "ram";
	static final Map<String, Integer> MONTH_NAMES = Map.of("gieng", 1, "mot", 11, "chap", 12);
	static final Set<String> FILLERS = Set.of("am", "lich", "al", "nam", "le", "vao", "luc");
	static final int MIN_YEAR = 100;


	private VietnameseTokens(){}

}

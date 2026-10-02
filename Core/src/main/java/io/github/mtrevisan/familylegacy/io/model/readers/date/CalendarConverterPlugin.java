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
package io.github.mtrevisan.familylegacy.io.model.readers.date;


/**
 * Strategy interface for converting cultural, non-linear text representations
 * of calendar dates directly into absolute Julian Day Numbers.
 */
public interface CalendarConverterPlugin{

	/**
	 * Parses a calendar-specific date string and converts it to a standard JDN.
	 *
	 * @param dateExpression    the raw calendar string (e.g., "庚申年 正月 初一" or "13.0.13.17.13 6 B'en 6 Yax")
	 * @param baseContextAnchor an optional reference tracking year or epoch block used by the parser to break cyclical
	 * 	loops
	 * @return the absolute Julian Day Number (JDN)
	 * @throws IllegalArgumentException if the text layout or internal constraints fail validation
	 */
	long parseToJdn(String dateExpression, int baseContextAnchor);

}

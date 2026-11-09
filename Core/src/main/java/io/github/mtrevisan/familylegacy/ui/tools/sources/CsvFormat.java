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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import org.apache.commons.lang3.StringUtils;

import java.util.List;


/** Writes rows as CSV (RFC 4180): fields with a comma, a quote or a line break are quoted, and the quotes in them are doubled. */
final class CsvFormat{

	private static final String SEPARATOR = ",";
	private static final String LINE_END = "\r\n";


	private CsvFormat(){}


	static String format(final List<String[]> rows){
		final StringBuilder sb = new StringBuilder();
		for(final String[] row : rows){
			for(int i = 0; i < row.length; i ++){
				if(i > 0)
					sb.append(SEPARATOR);
				sb.append(quote(row[i]));
			}
			sb.append(LINE_END);
		}
		return sb.toString();
	}

	private static String quote(final String field){
		final String value = StringUtils.defaultString(field);
		return (StringUtils.containsAny(value, ',', '"', '\n', '\r')
			? '"' + StringUtils.replace(value, "\"", "\"\"") + '"'
			: value);
	}

}

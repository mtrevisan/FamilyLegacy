/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.helpers.parsers;

import io.github.mtrevisan.familylegacy.flef.helpers.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;


public enum Era{

	CE(RegexHelper.pattern("(?i) ?(?:A\\.?C\\.?|(?:^|[^B.])C\\.?E\\.?)")),
	BCE(RegexHelper.pattern("(?i) ?B\\.?C\\.?(?:E\\.?)?"));


	private final Pattern pattern;


	public static Era fromDate(final CharSequence date){
		if(date != null){
			final Era[] values = values();
			for(int i = 0, length = values.length; i < length; i ++){
				final Era type = values[i];

				if(RegexHelper.find(date, type.pattern))
					return type;
			}
		}
		return null;
	}

	Era(final Pattern pattern){
		this.pattern = pattern;
	}

	public Pattern getPattern(){
		return pattern;
	}

	public static String replaceAll(String era){
		era = RegexHelper.replaceAll(era, BCE.pattern, BCE.toString());
		era = RegexHelper.clear(era, CE.pattern);
		return era;
	}

	public static String restoreAll(final String era){
		return era.replaceAll(CE.toString(), StringUtils.EMPTY);
	}

}

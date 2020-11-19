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
package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public enum Qualification{

	NO_QUALIFICATION(StringUtils.EMPTY, null),
	ABOUT("About", RegexHelper.pattern("(?i)ABT\\.?|ABOUT|APP(?:RO)?X\\.?")),
	CALCULATED("Calculated", RegexHelper.pattern("(?i)CALC?\\.?")),
	ESTIMATED("Estimated", RegexHelper.pattern("(?i)EST\\.?"));


	private final String description;
	private final Pattern pattern;


	public static Qualification createFromIndex(final int index){
		return values()[index];
	}

	public static Qualification createFromDate(final CharSequence date){
		for(final Qualification type : values())
			if(type.pattern != null && RegexHelper.find(date, type.pattern))
				return type;
		return null;
	}

	Qualification(final String description, final Pattern pattern){
		this.description = description;
		this.pattern = pattern;
	}

	public static String[] getDescriptions(){
		final List<String> list = new ArrayList<>();
		for(final Qualification qualification : values())
			list.add(qualification.description);
		return list.toArray(new String[0]);
	}

}

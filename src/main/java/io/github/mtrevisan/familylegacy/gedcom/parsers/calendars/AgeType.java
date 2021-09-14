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


public enum AgeType{

	EXACT("Exact", StringUtils.EMPTY, null),
	LESS_THAN("Less than", "<", RegexHelper.pattern("<")),
	MORE_THAN("More than", ">", RegexHelper.pattern(">")),

	//less than 8 years
	CHILD("Child", "CHILD", RegexHelper.pattern("(?i)CHILD")),
	//less than 1 year
	INFANT("Infant", "INFANT", RegexHelper.pattern("(?i)INFANT")),
	STILLBORN("Stillborn", "STILLBORN", RegexHelper.pattern("(?i)STILLBORN"));


	private final String description;
	private final String type;
	private final Pattern pattern;


	public static AgeType createFromIndex(final int index){
		return values()[index];
	}

	public static AgeType createFromText(final CharSequence instant){
		if(instant != null)
			for(final AgeType type : values())
				if(type.pattern != null && RegexHelper.find(instant, type.pattern))
					return type;
		return EXACT;
	}

	AgeType(final String description, final String type, final Pattern pattern){
		this.description = description;
		this.type = type;
		this.pattern = pattern;
	}

	public String getDescription(){
		return description;
	}

	public Pattern getPattern(){
		return pattern;
	}

	public static String[] getDescriptions(){
		final AgeType[] values = values();
		final List<String> list = new ArrayList<>(values.length);
		for(final AgeType ageType : values)
			list.add(ageType.description);
		return list.toArray(new String[0]);
	}

}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;


public enum IntervalType{

	NO_TYPE(StringUtils.EMPTY, StringUtils.EMPTY, null),
	ABOUT("About", "ABT", RegexHelper.pattern("(?i)(?:ABT\\.?|ABOUT|APP(?:RO)?X\\.?)")),
	CALCULATED("Calculated", "CAL", RegexHelper.pattern("(?i)CALC?\\.?")),
	ESTIMATED("Estimated", "EST", RegexHelper.pattern("(?i)EST\\.?")),
	BETWEEN("Between", "BET", RegexHelper.pattern("(?i)(?:(?:BET|BTW)\\.?|BETWEEN)")),
	BEFORE("Before", "BEF", RegexHelper.pattern("(?i)BEF(?:ORE|\\.)?")),
	AFTER("After", "AFT", RegexHelper.pattern("(?i)AFT(?:ER|\\.)?")),
	FROM("From", "FROM", RegexHelper.pattern("(?i)FROM")),
	TO("To", "TO", RegexHelper.pattern("(?i)TO")),
	INTERPRETED("Interpreted", "INT", RegexHelper.pattern("(?i)INT\\.?"));


	private static final Collection<IntervalType> QUALIFICATION_NOT_PRESENT_IF = EnumSet.noneOf(IntervalType.class);
	static{
		QUALIFICATION_NOT_PRESENT_IF.add(ABOUT);
		QUALIFICATION_NOT_PRESENT_IF.add(CALCULATED);
		QUALIFICATION_NOT_PRESENT_IF.add(ESTIMATED);
		QUALIFICATION_NOT_PRESENT_IF.add(INTERPRETED);
	}


	private final String description;
	private final String type;
	private final Pattern pattern;


	public static IntervalType createFromIndex(final int index){
		return values()[index];
	}

	public static IntervalType createFromDate(final CharSequence date){
		final IntervalType[] values = values();
		for(int i = 0, length = values.length; i < length; i ++){
			final IntervalType type = values[i];

			if(type.pattern != null && RegexHelper.find(date, type.pattern))
				return type;
		}
		return NO_TYPE;
	}

	IntervalType(final String description, final String type, final Pattern pattern){
		this.description = description;
		this.type = type;
		this.pattern = pattern;
	}

	public static String[] getDescriptions(){
		final IntervalType[] values = values();
		final List<String> list = new ArrayList<>(values.length);
		for(int i = 0, length = values.length; i < length; i ++)
			list.add(values[i].description);
		return list.toArray(new String[0]);
	}

	public String getType(){
		return type;
	}

	public boolean canQualificationBePresent(){
		return !QUALIFICATION_NOT_PRESENT_IF.contains(this);
	}

	public boolean isRangePresent(){
		return (this == BETWEEN || this == FROM);
	}

	public static String replaceAll(String date){
		final IntervalType[] values = values();
		for(int i = 0, length = values.length; i < length; i ++){
			final IntervalType value = values[i];

			if(value != NO_TYPE)
				date = RegexHelper.replaceAll(date, value.pattern, value.description);
		}
		return date;
	}

	public static String restoreAll(String date){
		final IntervalType[] values = values();
		for(int i = 0, length = values.length; i < length; i ++){
			final IntervalType value = values[i];

			if(value != NO_TYPE)
				date = StringUtils.replaceEachRepeatedly(date, new String[]{value.description}, new String[]{value.type});
		}
		return date;
	}

}

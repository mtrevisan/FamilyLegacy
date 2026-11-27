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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthand;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.function.UnaryOperator;


/**
 * Builds a {@code DateStructure} from a date typed in the shorthand of the date editor ({@code abt 1850}, {@code bet 1850 and 1855},
 * {@code 1850s}, {@code 15th century}...), see {@link DateShorthandReader}.
 * <p>
 * Whether the full dates are good in their calendar is not checked here: that is the job of the calendar, and of the editor that asks
 * the user what to do when a date has more than one reading. An expression that says more than a plain date is kept as the original
 * text of the date, as the editor does.
 */
public final class DateStructureBuilder{

	private DateStructureBuilder(){}


	/**
	 * @param text	The date, in the shorthand.
	 * @param calendar	The code of the calendar of the date ({@code null}: the gregorian one).
	 * @return	The {@code date} structure, {@code null} if the text is blank.
	 * @throws DateShorthandReader.SyntaxException	If the text is not a date.
	 */
	public static FLEFRecord fromShorthand(final String text, final String calendar){
		return fromShorthand(DateReader.TAG_DATE, text, calendar);
	}

	/**
	 * @param tag	The tag of the structure.
	 * @param text	The date, in the shorthand.
	 * @param calendar	The code of the calendar of the date ({@code null}: the gregorian one).
	 * @return	The structure, {@code null} if the text is blank.
	 * @throws DateShorthandReader.SyntaxException	If the text is not a date.
	 */
	public static FLEFRecord fromShorthand(final String tag, final String text, final String calendar){
		return fromShorthand(tag, text, calendar, UnaryOperator.identity());
	}

	/**
	 * @param tag	The tag of the structure.
	 * @param text	The date, in the shorthand.
	 * @param calendar	The code of the calendar of the date ({@code null}: the gregorian one).
	 * @param fullDates	Gives the full date to write for a full date of the text (what the user chose among the readings of an ambiguous
	 * 	one, see {@link DateShorthandChecker}); the text typed is kept as the original text only when it says more than a plain date.
	 * @return	The structure, {@code null} if the text is blank.
	 * @throws DateShorthandReader.SyntaxException	If the text is not a date.
	 */
	public static FLEFRecord fromShorthand(final String tag, final String text, final String calendar, final UnaryOperator<String> fullDates){
		if(StringUtils.isBlank(text))
			return null;

		final DateShorthand typed = DateShorthandReader.parse(text);
		final DateShorthand shorthand = new DateShorthand(typed.kind(), resolve(typed.first(), fullDates), resolve(typed.second(), fullDates));
		final String calendarCode = (StringUtils.isNotBlank(calendar)? calendar: DateReader.ENUM_CALENDAR_GREGORIAN);
		final FLEFRecord value = FLEFRecord.createChildWithTag(DateReader.TAG_VALUE);
		switch(shorthand.kind()){
			case POINT -> value.addChild(single(DateReader.TAG_POINT, shorthand.first(), calendarCode));
			case BOUNDED -> value.addChild(range(DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, shorthand.first(), DateReader.TAG_NOT_AFTER,
				shorthand.second(), calendarCode));
			case SPANNING -> value.addChild(range(DateReader.TAG_SPANNING, DateReader.TAG_FROM, shorthand.first(), DateReader.TAG_TO,
				shorthand.second(), calendarCode));
		}

		final FLEFRecord date = FLEFRecord.createChildWithTag(tag)
			.addChild(value);
		final boolean plainDate = (shorthand.kind() == DateShorthand.Kind.POINT && shorthand.first().fullDate() != null
			&& !shorthand.first().approximate());
		if(!plainDate)
			date.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ASSISTANT_FINDING_IDAL_TEXT, text.strip()));
		return date;
	}

	/** @return	The single date with the full date that the resolver gives (the other parts as they are). */
	private static DateShorthand.Single resolve(final DateShorthand.Single date, final UnaryOperator<String> fullDates){
		if(date == null || date.fullDate() == null)
			return date;

		return new DateShorthand.Single(fullDates.apply(date.fullDate()), date.decade(), date.century(), date.centuryPart(), date.basis(), date.margin());
	}

	/** A bounded or spanning date; an open end is not written. */
	private static FLEFRecord range(final String tag, final String startTag, final DateShorthand.Single start, final String endTag,
			final DateShorthand.Single end, final String calendar){
		final FLEFRecord record = FLEFRecord.createChildWithTag(tag);
		if(start != null)
			record.addChild(single(startTag, start, calendar));
		if(end != null)
			record.addChild(single(endTag, end, calendar));
		return record;
	}

	private static FLEFRecord single(final String tag, final DateShorthand.Single date, final String calendar){
		final FLEFRecord record = FLEFRecord.createChildWithTag(tag);
		final FLEFRecord value = FLEFRecord.createChildWithTag(DateReader.TAG_VALUE);
		if(date.decade() != null)
			value.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_DECADE, String.valueOf(date.decade())));
		else if(date.century() != null){
			final FLEFRecord century = FLEFRecord.createChildWithTag(DateReader.TAG_CENTURY)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ORDINAL, String.valueOf(date.century())));
			if(date.centuryPart() != null)
				century.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_PART, date.centuryPart()));
			value.addChild(century);
		}
		else
			value.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, date.fullDate()));
		record.addChild(value)
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, calendar));
		if(date.approximate()){
			final FLEFRecord approximate = FLEFRecord.createChildWithTag(DateReader.TAG_APPROXIMATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_BASIS, date.basis()));
			if(date.margin() != null)
				approximate.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_MARGIN, date.margin()));
			record.addChild(approximate);
		}
		return record;
	}

}

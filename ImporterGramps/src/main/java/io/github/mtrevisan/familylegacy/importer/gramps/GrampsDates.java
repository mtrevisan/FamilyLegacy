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
package io.github.mtrevisan.familylegacy.importer.gramps;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Locale;


/**
 * Converts the dates of a Gramps XML file ({@code dateval}, {@code daterange}, {@code datespan}, {@code datestr}) into FLEF
 * {@code DateStructure}s, and into the validity ({@code valid_from} and {@code valid_to}) of a fact observed at a date.
 * <p>
 * Gramps says what a date means: a single date (that may be before, after, or about), a range (the date is somewhere in it) and a
 * span (the fact lasted all along it); FLEF says the same with {@code point}, {@code bounded} and {@code spanning}. The calendars
 * that FLEF can write here are the Gregorian, the Julian, the Hebrew and the French Republican; a date of another calendar (or a
 * text, or a date that is not valid) cannot, and is kept as it is by the caller.
 */
final class GrampsDates{

	private static final List<String> MONTHS = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV",
		"DEC");
	/** The months of the Hebrew calendar, in the order Gramps numbers them. */
	private static final List<String> HEBREW_MONTHS = List.of("tishrei", "cheshvan", "kislev", "tevet", "shevat", "adar", "veadar",
		"nisan", "iyar", "sivan", "tammuz", "av", "elul");
	private static final List<String> FRENCH_MONTHS = List.of("vendemiaire", "brumaire", "frimaire", "nivose", "pluviose", "ventose",
		"germinal", "floreal", "prairial", "messidor", "thermidor", "fructidor", "complementaires");

	private static final String CALENDAR_GREGORIAN = "gregorian";
	private static final String CALENDAR_JULIAN = "julian";
	private static final String CALENDAR_HEBREW = "hebrew";
	private static final String CALENDAR_FRENCH = "french_republican";

	private enum Kind{POINT, BOUNDED, SPANNING}

	/** A date written in a calendar: {@code 12 MAR 1850}. */
	private record Single(String value, String calendar){}

	/**
	 * What a Gramps date says.
	 *
	 * @param kind	Whether the date is a point, an interval it falls within, or the interval a fact lasted.
	 * @param first	The date (point), the lower bound (bounded) or the start (spanning); may be {@code null} when open.
	 * @param second	The upper bound (bounded) or the end (spanning); {@code null} for a point.
	 * @param basis	How approximate a point is, {@code null} if it is not.
	 */
	private record Parsed(Kind kind, Single first, Single second, String basis){}


	private GrampsDates(){}


	/**
	 * @param date	A date element, may be {@code null}.
	 * @return	The date as a structure with the tag, or {@code null} if there is none, or if it cannot be written.
	 */
	static FLEFRecord structure(final String tag, final Element date){
		final Parsed parsed = parse(date);
		if(parsed == null)
			return null;

		final FLEFRecord content = switch(parsed.kind()){
			case POINT -> single("point", parsed.first(), parsed.basis());
			case BOUNDED -> range("bounded", "not_before", parsed.first(), "not_after", parsed.second());
			case SPANNING -> range("spanning", "from", parsed.first(), "to", parsed.second());
		};
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(content));
	}

	/**
	 * Builds the {@code valid_from} and {@code valid_to} of a fact observed at the date.
	 * <p>
	 * A span gives the bounds of validity directly; any other date only states that the fact was true at that date, so it started
	 * no later than it and ended no earlier than it.
	 *
	 * @return	The {@code valid_from} and the {@code valid_to} (either may be {@code null}), or {@code null} if there is no date, or if
	 * 	it cannot be written.
	 */
	static FLEFRecord[] validity(final Element date){
		final Parsed parsed = parse(date);
		if(parsed == null)
			return null;

		return switch(parsed.kind()){
			case SPANNING -> new FLEFRecord[]{
				wrap("valid_from", null, single("point", parsed.first(), null)),
				wrap("valid_to", null, single("point", parsed.second(), null))};
			case BOUNDED -> new FLEFRecord[]{
				wrap("valid_from", "bounded", single("not_after", parsed.second(), null)),
				wrap("valid_to", "bounded", single("not_before", parsed.first(), null))};
			case POINT -> new FLEFRecord[]{
				wrap("valid_from", "bounded", single("not_after", parsed.first(), parsed.basis())),
				wrap("valid_to", "bounded", single("not_before", parsed.first(), parsed.basis()))};
		};
	}

	/** @return	The date as text, with its calendar when it is not the Gregorian one ({@code null} if there is no date). */
	static String text(final Element date){
		if(date == null)
			return null;

		final String calendar = GrampsXML.attribute(date, "cformat");
		final String prefix = (calendar != null && !CALENDAR_GREGORIAN.equalsIgnoreCase(calendar)? calendar + " ": StringUtils.EMPTY);
		return switch(GrampsXML.name(date)){
			case "datestr" -> GrampsXML.attribute(date, "val");
			case "dateval" -> {
				final String type = GrampsXML.attribute(date, "type");
				yield (type != null? type + " ": StringUtils.EMPTY) + prefix + GrampsXML.attribute(date, "val");
			}
			case "daterange" -> "between " + prefix + GrampsXML.attribute(date, "start") + " and "
				+ GrampsXML.attribute(date, "stop");
			case "datespan" -> "from " + prefix + GrampsXML.attribute(date, "start") + " to " + GrampsXML.attribute(date, "stop");
			default -> null;
		};
	}


	private static Parsed parse(final Element date){
		if(date == null)
			return null;

		final String calendar = calendarOf(date);
		final boolean dual = "1".equals(GrampsXML.attribute(date, "dualdated"));
		switch(GrampsXML.name(date)){
			case "dateval" -> {
				final Single value = singleOf(GrampsXML.attribute(date, "val"), calendar, dual);
				if(value == null)
					return null;

				final String type = StringUtils.defaultString(GrampsXML.attribute(date, "type")).toLowerCase(Locale.ROOT);
				return switch(type){
					case "before" -> new Parsed(Kind.BOUNDED, null, value, null);
					case "after" -> new Parsed(Kind.BOUNDED, value, null, null);
					case "about" -> new Parsed(Kind.POINT, value, null, DateReader.ENUM_BASIS_STATED);
					default -> new Parsed(Kind.POINT, value, null, basisOf(GrampsXML.attribute(date, "quality")));
				};
			}
			case "daterange", "datespan" -> {
				final Single start = singleOf(GrampsXML.attribute(date, "start"), calendar, dual);
				final Single stop = singleOf(GrampsXML.attribute(date, "stop"), calendar, dual);
				if(start == null || stop == null)
					return null;

				return new Parsed(("daterange".equals(GrampsXML.name(date))? Kind.BOUNDED: Kind.SPANNING), start, stop, null);
			}
			default -> {
				//a text, that has no date to normalize
				return null;
			}
		}
	}

	/** @return	The basis of the approximation that a quality says, {@code null} if it says none. */
	private static String basisOf(final String quality){
		if(quality == null)
			return null;

		return ("calculated".equalsIgnoreCase(quality)? DateReader.ENUM_BASIS_CALCULATED: DateReader.ENUM_BASIS_UNSPECIFIED);
	}

	/** @return	The calendar as FLEF names it, {@code null} if FLEF cannot write the dates of it here. */
	private static String calendarOf(final Element date){
		final String calendar = GrampsXML.attribute(date, "cformat");
		if(calendar == null)
			return CALENDAR_GREGORIAN;

		return switch(calendar.toLowerCase(Locale.ROOT)){
			case "gregorian" -> CALENDAR_GREGORIAN;
			case "julian" -> CALENDAR_JULIAN;
			case "hebrew" -> CALENDAR_HEBREW;
			case "french republican" -> CALENDAR_FRENCH;
			default -> null;
		};
	}

	/**
	 * @param value	The date, as {@code year-month-day} (the unknown parts are {@code 0}).
	 * @param calendar	The calendar, {@code null} if it cannot be written.
	 * @param dual	Whether the year is a dual one (the Julian year that began in March, written {@code 1750/51}).
	 * @return	The date in the notation of its calendar, or {@code null} if it cannot be written.
	 */
	private static Single singleOf(final String value, final String calendar, final boolean dual){
		if(calendar == null || StringUtils.isBlank(value))
			return null;

		final boolean negative = value.startsWith("-");
		final String[] fields = StringUtils.split((negative? value.substring(1): value), '-');
		//the month and the day can be missing ("1875", "1875-03")
		if(fields.length < 1 || fields.length > 3)
			return null;

		final int year;
		final int month;
		final int day;
		try{
			year = Integer.parseInt(fields[0].trim());
			month = (fields.length > 1? Integer.parseInt(fields[1].trim()): 0);
			day = (fields.length > 2? Integer.parseInt(fields[2].trim()): 0);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
		if(year <= 0)
			return null;

		final List<String> months = switch(calendar){
			case CALENDAR_HEBREW -> HEBREW_MONTHS;
			case CALENDAR_FRENCH -> FRENCH_MONTHS;
			default -> MONTHS;
		};
		final StringBuilder sb = new StringBuilder();
		if(month >= 1 && month <= months.size()){
			if(day > 0)
				sb.append(day).append(' ');
			sb.append(months.get(month - 1)).append(' ');
		}
		sb.append(year);
		if(dual)
			sb.append('/').append(String.format("%02d", (year + 1) % 100));
		if(negative)
			sb.append(" BCE");
		return new Single(sb.toString(), calendar);
	}

	private static FLEFRecord single(final String tag, final Single date, final String basis){
		if(date == null)
			return null;

		final FLEFRecord record = FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTagAndValue("full_date", date.value())))
			.addChild(FLEFRecord.createChildWithTagAndValue("calendar", date.calendar()));
		if(basis != null)
			record.addChild(FLEFRecord.createChildWithTag("approximate")
				.addChild(FLEFRecord.createChildWithTagAndValue("basis", basis)));
		return record;
	}

	private static FLEFRecord range(final String kind, final String startTag, final Single start, final String endTag,
			final Single end){
		return FLEFRecord.createChildWithTag(kind)
			.addChild(single(startTag, start, null))
			.addChild(single(endTag, end, null));
	}

	private static FLEFRecord wrap(final String tag, final String kind, final FLEFRecord content){
		if(content == null)
			return null;

		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(kind != null? FLEFRecord.createChildWithTag(kind).addChild(content): content));
	}

}

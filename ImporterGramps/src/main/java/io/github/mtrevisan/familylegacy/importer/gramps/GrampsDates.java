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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts the dates of a Gramps XML file ({@code dateval}, {@code daterange}, {@code datespan}, {@code datestr}) into FLEF
 * {@code DateStructure}s, and into the validity ({@code valid_from} and {@code valid_to}) of a fact observed at a date.
 * <p>
 * Gramps says what a date means: a single date (that may be before, after, or about), a range (the date is somewhere in it) and a
 * span (the fact lasted all along it); FLEF says the same with {@code point}, {@code bounded} and {@code spanning}. The calendars
 * that FLEF can write here are the Gregorian, the Julian, the Hebrew, the French Republican, the Islamic, the Persian and the
 * Swedish; a date of another calendar (or a text, or a date that is not valid) cannot, and is kept as it is by the caller.
 */
final class GrampsDates{

	private static final List<String> MONTHS = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV",
		"DEC");
	/** The months of the Hebrew calendar, in the order Gramps numbers them. */
	private static final List<String> HEBREW_MONTHS = List.of("tishrei", "cheshvan", "kislev", "tevet", "shevat", "adar", "veadar",
		"nisan", "iyar", "sivan", "tammuz", "av", "elul");
	private static final List<String> FRENCH_MONTHS = List.of("vendemiaire", "brumaire", "frimaire", "nivose", "pluviose", "ventose",
		"germinal", "floreal", "prairial", "messidor", "thermidor", "fructidor", "complementaires");

	/** The months of the Islamic calendar, in the order Gramps numbers them (in the notation of the parser of the calendar). */
	private static final List<String> ISLAMIC_MONTHS = List.of("muharram", "safar", "rabi al-awwal", "rabi al-thani", "jumada al-awwal",
		"jumada al-thani", "rajab", "shaban", "ramadan", "shawwal", "dhu al-qadah", "dhu al-hijjah");
	private static final List<String> PERSIAN_MONTHS = List.of("farvardin", "ordibehesht", "khordad", "tir", "mordad", "shahrivar",
		"mehr", "aban", "azar", "dey", "bahman", "esfand");
	/** The age of an individual at an event: a number of years, that may be qualified (less than, more than, about). */
	private static final Pattern AGE = Pattern.compile("\\s*(<|>|circa|about|abt\\.?|ca?\\.?|~)?\\s*(.*?)\\s*",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	/** A part of an age: a number with its unit (years when missing; months and days are less than a year). */
	private static final Pattern AGE_PART = Pattern.compile("(\\d{1,3})\\s*(years?|yrs?|y|months?|mos?|m|days?|d)?",
		Pattern.CASE_INSENSITIVE);
	/** The dates already read (a date is asked for many times); the keys are not kept alive. */
	private static final Map<Element, Optional<Parsed>> PARSED = Collections.synchronizedMap(new WeakHashMap<>());

	private static final String CALENDAR_GREGORIAN = DateReader.ENUM_CALENDAR_GREGORIAN;
	private static final String CALENDAR_JULIAN = DateReader.ENUM_CALENDAR_JULIAN;
	private static final String CALENDAR_HEBREW = DateReader.ENUM_CALENDAR_HEBREW;
	private static final String CALENDAR_FRENCH = DateReader.ENUM_CALENDAR_FRENCH_REPUBLICAN;
	private static final String CALENDAR_ISLAMIC = DateReader.ENUM_CALENDAR_ISLAMIC;
	private static final String CALENDAR_PERSIAN = DateReader.ENUM_CALENDAR_PERSIAN;
	private static final String CALENDAR_SWEDISH = DateReader.ENUM_CALENDAR_SWEDISH;
	/** The first day of the Gregorian calendar in the strategies of the year-start styles, as {@code yyyymmdd}. */
	private static final int GREGORIAN_START = 15821015;

	private enum Kind{POINT, BOUNDED, SPANNING}

	/** A date written in a calendar: {@code 12 MAR 1850} (the year is the number of the year, negative before the era). */
	private record Single(String value, String calendar, int year){}

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
		return structureOf(tag, parse(date));
	}

	private static FLEFRecord structureOf(final String tag, final Parsed parsed){
		if(parsed == null)
			return null;

		final FLEFRecord content = switch(parsed.kind()){
			case POINT -> single(DateReader.TAG_POINT, parsed.first(), parsed.basis());
			case BOUNDED -> range(DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, parsed.first(), DateReader.TAG_NOT_AFTER, parsed.second());
			case SPANNING -> range(DateReader.TAG_SPANNING, DateReader.TAG_FROM, parsed.first(), DateReader.TAG_TO, parsed.second());
		};
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
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
				wrap(DateReader.TAG_VALID_FROM, null, single(DateReader.TAG_POINT, parsed.first(), null)),
				wrap(DateReader.TAG_VALID_TO, null, single(DateReader.TAG_POINT, parsed.second(), null))};
			case BOUNDED -> new FLEFRecord[]{
				wrap(DateReader.TAG_VALID_FROM, DateReader.TAG_BOUNDED, single(DateReader.TAG_NOT_AFTER, parsed.second(), null)),
				wrap(DateReader.TAG_VALID_TO, DateReader.TAG_BOUNDED, single(DateReader.TAG_NOT_BEFORE, parsed.first(), null))};
			case POINT -> new FLEFRecord[]{
				wrap(DateReader.TAG_VALID_FROM, DateReader.TAG_BOUNDED, single(DateReader.TAG_NOT_AFTER, parsed.first(), parsed.basis())),
				wrap(DateReader.TAG_VALID_TO, DateReader.TAG_BOUNDED, single(DateReader.TAG_NOT_BEFORE, parsed.first(), parsed.basis()))};
		};
	}

	/**
	 * The date of birth that the age of an individual at an event says: the year of the event minus the age, in the calendar of the
	 * event, marked as calculated ("less than 8" and "more than 70" give a bound).
	 *
	 * @param tag	The tag of the date structure.
	 * @param date	The date of the event, it must be a point.
	 * @param age	The age, as Gramps keeps it (free text: {@code 25}, {@code 25 years}, {@code <8}, {@code about 40}).
	 * @return	The date, {@code null} if the age or the date cannot be used.
	 */
	static FLEFRecord birthDate(final String tag, final Element date, final String age){
		if(date == null || age == null)
			return null;

		final Matcher matcher = AGE.matcher(age);
		final Parsed event = parse(date);
		if(!matcher.matches() || event == null || event.kind() != Kind.POINT || event.first() == null || event.first().year() <= 0)
			return null;

		//{years, whether the age says less than a year too}
		final int[] years = yearsOf(matcher.group(2));
		if(years == null)
			return null;

		final Single at = event.first();
		final int year = at.year() - years[0];
		if(year <= 0)
			return null;

		final Single birth = new Single(Integer.toString(year), at.calendar(), year);
		final String qualifier = StringUtils.defaultString(matcher.group(1));
		final Parsed parsed = switch(qualifier){
			//younger than the age: born after that year, and not after the event
			case "<" -> new Parsed(Kind.BOUNDED, birth, new Single(Integer.toString(at.year()), at.calendar(), at.year()), null);
			//older than the age: born before that year
			case ">" -> new Parsed(Kind.BOUNDED, null, birth, null);
			//less than a year old: born the year of the event or the year before
			default -> (years[0] == 0 && year > 1
				? new Parsed(Kind.BOUNDED, new Single(Integer.toString(year - 1), at.calendar(), year - 1), birth, null)
				: new Parsed(Kind.POINT, birth, null, DateReader.ENUM_BASIS_CALCULATED));
		};
		return structureOf(tag, parsed);
	}

	/**
	 * @param text	An age: {@code 25}, {@code 25 years}, {@code 25y 3m}, {@code 5 months}, {@code 12 days}.
	 * @return	The whole years (0 if the age has only months or days), {@code null} if it is not an age.
	 */
	private static int[] yearsOf(final String text){
		final Matcher matcher = AGE_PART.matcher(text);
		final int length = text.length();
		Integer years = null;
		boolean partial = false;
		int position = 0;
		while(true){
			while(position < length && Character.isWhitespace(text.charAt(position)))
				position ++;
			if(position == length)
				break;

			matcher.region(position, length);
			if(!matcher.lookingAt())
				return null;

			final String unit = matcher.group(2);
			if(unit == null || unit.toLowerCase(Locale.ROOT).startsWith("y")){
				if(years != null)
					return null;

				years = Integer.parseInt(matcher.group(1));
			}
			else
				partial = true;
			position = matcher.end();
		}
		if(years == null)
			years = (partial? 0: null);
		return (years != null? new int[]{years, (partial? 1: 0)}: null);
	}

	/** @return	The date as text, with its calendar when it is not the Gregorian one ({@code null} if there is no date). */
	static String text(final Element date){
		if(date == null)
			return null;

		final String calendar = GrampsXML.attribute(date, "cformat");
		final String prefix = (calendar != null && !CALENDAR_GREGORIAN.equalsIgnoreCase(calendar)? calendar + StringUtils.SPACE: StringUtils.EMPTY);
		return switch(GrampsXML.name(date)){
			case "datestr" -> GrampsXML.attribute(date, "val");
			case "dateval" -> {
				final String type = GrampsXML.attribute(date, "type");
				yield (type != null? type + StringUtils.SPACE: StringUtils.EMPTY) + prefix + StringUtils.defaultString(GrampsXML.attribute(date, "val"));
			}
			case "daterange" -> I18N.tf("import.gramps.text.between", prefix + StringUtils.defaultString(GrampsXML.attribute(date, "start")),
				StringUtils.defaultString(GrampsXML.attribute(date, "stop")));
			case "datespan" -> I18N.tf("import.gramps.text.span", prefix + StringUtils.defaultString(GrampsXML.attribute(date, "start")),
				StringUtils.defaultString(GrampsXML.attribute(date, "stop")));
			default -> null;
		};
	}


	private static Parsed parse(final Element date){
		return (date != null? PARSED.computeIfAbsent(date, k -> Optional.ofNullable(parseDate(k))).orElse(null): null);
	}

	private static Parsed parseDate(final Element date){
		final String baseCalendar = calendarOf(date);
		final String yearStart = newYearCalendar(date, baseCalendar);
		final String calendar = (yearStart != null? yearStart: baseCalendar);
		final boolean dual = "1".equals(GrampsXML.attribute(date, "dualdated"));
		//Gramps counts the year of a rule that begins it on 1 March (the Venetan one) as the year of the civil date minus one for the days from
		//1 March on and as the year written before that, while the Venetan year number is the civil one from 1 March on and the civil minus one
		//before: in both cases the Venetan year is the one written by Gramps minus one
		final int yearShift = (DateReader.ENUM_CALENDAR_VENETAN.equals(yearStart)? -1: 0);
		switch(GrampsXML.name(date)){
			case "dateval" -> {
				final Single value = singleOf(GrampsXML.attribute(date, "val"), calendar, dual, yearShift);
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
				final Single start = singleOf(GrampsXML.attribute(date, "start"), calendar, dual, yearShift);
				final Single stop = singleOf(GrampsXML.attribute(date, "stop"), calendar, dual, yearShift);
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

	/**
	 * @param date	A date element, may be {@code null}.
	 * @return	Whether the new-year rule of the date is written (as the calendar of the date), so that it is not lost.
	 */
	static boolean newYearKept(final Element date){
		return (date != null && newYearCalendar(date, calendarOf(date)) != null && parse(date) != null);
	}

	/**
	 * @param date	A date element, may be {@code null}.
	 * @return	The quality of the date (estimated, calculated) when it cannot be kept, {@code null} if it is kept or the date has none.
	 * 	It is kept only as the basis of a single date; a before, after, about, range or span loses it.
	 */
	static String ignoredQuality(final Element date){
		final String quality = (date != null? GrampsXML.attribute(date, "quality"): null);
		if(quality == null || parse(date) == null)
			return null;

		final String type = StringUtils.defaultString(GrampsXML.attribute(date, "type")).toLowerCase(Locale.ROOT);
		final boolean kept = "dateval".equals(GrampsXML.name(date)) && !List.of("before", "after", "about").contains(type);
		return (kept? null: quality);
	}

	/**
	 * The year-start style that the new-year rule of a date says. The styles read the days as Julian before 15 October 1582 and as
	 * Gregorian from then on, so the rule is used only if the dates are of the calendar that the style reads them in; a dual-dated
	 * date is already counted from 1 January.
	 *
	 * @return	The calendar, {@code null} if the date has no (known) rule or if the rule cannot be written.
	 */
	private static String newYearCalendar(final Element date, final String calendar){
		final String rule = GrampsXML.attribute(date, "newyear");
		if(rule == null || "1".equals(GrampsXML.attribute(date, "dualdated")))
			return null;

		final boolean julian = CALENDAR_JULIAN.equals(calendar);
		if(!julian && !CALENDAR_GREGORIAN.equals(calendar))
			return null;

		final String style = switch(rule.toLowerCase(Locale.ROOT)){
			//Gramps (Date._adjust_newyear) reads a date on or after the day the year begins as the day of the year before the one written: the
			//year N begins before 1 January N, as in the anticipating styles (Venetan, Pisan, Byzantine, Nativity), and a date before that day is
			//of the year written
			case "mar1" -> DateReader.ENUM_CALENDAR_VENETAN;
			case "mar25" -> DateReader.ENUM_CALENDAR_PISAN;
			case "sep1" -> DateReader.ENUM_CALENDAR_BYZANTINE_STYLE;
			case "dec25" -> DateReader.ENUM_CALENDAR_NATIVITY;
			case "easter" -> DateReader.ENUM_CALENDAR_EASTER_STYLE;
			default -> null;
		};
		if(style == null)
			return null;

		final List<String> values = switch(GrampsXML.name(date)){
			case "dateval" -> List.of(StringUtils.defaultString(GrampsXML.attribute(date, "val")));
			case "daterange", "datespan" -> List.of(StringUtils.defaultString(GrampsXML.attribute(date, "start")),
				StringUtils.defaultString(GrampsXML.attribute(date, "stop")));
			default -> List.of();
		};
		if(values.isEmpty())
			return null;

		for(final String value : values){
			final int key = dayKey(value);
			if(key < 0 || julian == (key >= GREGORIAN_START))
				return null;
		}
		return style;
	}

	/** @return	The date {@code year-month-day} as {@code yyyymmdd} (the unknown parts are {@code 0}), {@code -1} if it is not one. */
	private static int dayKey(final String value){
		//a date before the era is not read in the styles of the year start
		if(value.startsWith("-"))
			return -1;

		final String[] fields = StringUtils.split(value, '-');
		if(fields.length < 1 || fields.length > 3)
			return -1;

		try{
			final int year = Integer.parseInt(fields[0].trim());
			final int month = (fields.length > 1? Integer.parseInt(fields[1].trim()): 0);
			final int day = (fields.length > 2? Integer.parseInt(fields[2].trim()): 0);
			return (year > 0 && month >= 0 && month <= 99 && day >= 0 && day <= 99? year * 10000 + month * 100 + day: -1);
		}
		catch(final NumberFormatException ignored){
			return -1;
		}
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
			case "islamic" -> CALENDAR_ISLAMIC;
			case "persian" -> CALENDAR_PERSIAN;
			case "swedish" -> CALENDAR_SWEDISH;
			default -> null;
		};
	}

	/**
	 * @param value	The date, as {@code year-month-day} (the unknown parts are {@code 0}).
	 * @param calendar	The calendar, {@code null} if it cannot be written.
	 * @param yearShift	The difference between the year the calendar writes and the one Gramps writes.
	 * @param dual	Whether the year is a dual one (the Julian year that began in March, written {@code 1750/51}).
	 * @return	The date in the notation of its calendar, or {@code null} if it cannot be written.
	 */
	private static Single singleOf(final String value, final String calendar, final boolean dual, final int yearShift){
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
			year = Integer.parseInt(fields[0].trim()) + yearShift;
			month = (fields.length > 1? Integer.parseInt(fields[1].trim()): 0);
			day = (fields.length > 2? Integer.parseInt(fields[2].trim()): 0);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
		if(year <= 0)
			return null;

		final List<String> months = monthsOf(calendar);
		//a month that the calendar does not have is not a date that can be written (it is kept as text, not reduced to the year)
		if(month < 0 || month > months.size())
			return null;

		final StringBuilder sb = new StringBuilder();
		if(month >= 1){
			if(day > 0)
				sb.append(day).append(' ');
			sb.append(months.get(month - 1)).append(' ');
		}
		sb.append(year);
		if(dual)
			sb.append('/').append(String.format(Locale.ROOT, "%02d", (year + 1) % 100));
		if(negative)
			sb.append(" BCE");
		return new Single(sb.toString(), calendar, (negative? -year: year));
	}

	/** @return	The names of the months of the calendar, as its parser wants them, in the order Gramps numbers them. */
	private static List<String> monthsOf(final String calendar){
		if(CALENDAR_HEBREW.equalsIgnoreCase(calendar))
			return HEBREW_MONTHS;
		if(CALENDAR_FRENCH.equalsIgnoreCase(calendar))
			return FRENCH_MONTHS;
		if(CALENDAR_ISLAMIC.equalsIgnoreCase(calendar))
			return ISLAMIC_MONTHS;
		if(CALENDAR_PERSIAN.equalsIgnoreCase(calendar))
			return PERSIAN_MONTHS;
		//the Gregorian, the Julian and the Swedish calendars share the months
		return MONTHS;
	}

	private static FLEFRecord single(final String tag, final Single date, final String basis){
		if(date == null)
			return null;

		final FLEFRecord record = FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, date.value())))
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, date.calendar()));
		if(basis != null)
			record.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_APPROXIMATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_BASIS, basis)));
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
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(kind != null? FLEFRecord.createChildWithTag(kind).addChild(content): content));
	}

}

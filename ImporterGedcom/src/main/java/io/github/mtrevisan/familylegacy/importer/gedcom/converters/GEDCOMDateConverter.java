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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.DateInfo;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOM7Normalizer;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMDateParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts GEDCOM dates into FLEF {@code DateStructure}s and GEDCOM date/time pairs into FLEF {@code DateTime}s.
 * <p>
 * A single GEDCOM date may carry a calendar escape ({@code @#DGREGORIAN@}, {@code @#DJULIAN@}, {@code @#DHEBREW@},
 * {@code @#DFRENCH R@}); dates in the other calendars ({@code @#DROMAN@}, {@code @#DUNKNOWN@}) cannot be normalized and
 * make the conversion fail. A date without escape is hebrew or french republican when its month says so, otherwise gregorian, or
 * julian when before 1582 (or before Christ).
 * <p>
 * The calendars of GEDCOM 7 ({@code JULIAN 1 JAN 1700}) are rewritten as escapes by {@link
 * GEDCOM7Normalizer}.
 */
public final class GEDCOMDateConverter{

	private static final Pattern ESCAPE_PATTERN = Pattern.compile("^@#D([A-Z ]+)@\\s*(.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GREGORIAN_DATE_PATTERN = Pattern.compile(
		"^(?:(?:(\\d{1,2})\\s+)?(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\s+)?(\\d{1,4})(/\\d{1,2})?( BCE)?$");
	/** Era before Christ (GEDCOM 5.5.1 "B.C.", GEDCOM 7 "BCE", and their variants). */
	private static final Pattern BEFORE_COMMON_ERA_PATTERN = Pattern.compile("\\s*\\bB\\.?\\s?C\\.?(?:\\s?E\\.?)?$");
	/** Common era ("CE", "C.E.", "AD", "A.D."), redundant. */
	private static final Pattern COMMON_ERA_PATTERN = Pattern.compile("\\s*\\b(?:C\\.?\\s?E|A\\.?\\s?D)\\.?$");
	private static final Pattern MONTH_DATE_PATTERN = Pattern.compile("^(?:(?:(\\d{1,2})\\s+)?([A-Z]{3,4})\\s+)?(\\d{1,4})$");

	/** The alternatives of a date, each one marked by a question mark. */
	private static final Pattern ALTERNATIVE_SEPARATOR_PATTERN = Pattern.compile("(?<=\\?)\\s+");
	/** A year that can be either of two (not a double dating, whose second year has two digits). */
	private static final Pattern YEAR_ALTERNATIVE_PATTERN = Pattern.compile("^(.*?)(\\d{4})/(\\d{4})$");
	/** A qualifier of a single date, e.g. the one of a bound of a range. */
	private static final Pattern QUALIFIER_PATTERN = Pattern.compile("^(ABT|CAL|EST)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
	/** A GEDCOM age: {@code [<|>] [Ny] [Nm] [Nw] [Nd]}. */
	private static final Pattern AGE_PATTERN = Pattern.compile("^([<>])?\\s*(?:(\\d{1,3})\\s*Y)?\\s*(?:(\\d{1,4})\\s*M)?\\s*(?:(\\d{1,4})\\s*W)?\\s*(?:(\\d{1,5})\\s*D)?$");

	private static final Map<String, String> HEBREW_MONTHS = Map.ofEntries(
		Map.entry("TSH", "tishrei"), Map.entry("CSH", "cheshvan"), Map.entry("KSL", "kislev"), Map.entry("TVT", "tevet"),
		Map.entry("SHV", "shevat"), Map.entry("ADR", "adar"), Map.entry("ADS", "veadar"), Map.entry("NSN", "nisan"),
		Map.entry("IYR", "iyar"), Map.entry("SVN", "sivan"), Map.entry("TMZ", "tammuz"), Map.entry("AAV", "av"),
		Map.entry("ELL", "elul"));
	private static final Map<String, String> FRENCH_REPUBLICAN_MONTHS = Map.ofEntries(
		Map.entry("VEND", "vendemiaire"), Map.entry("BRUM", "brumaire"), Map.entry("FRIM", "frimaire"),
		Map.entry("NIVO", "nivose"), Map.entry("PLUV", "pluviose"), Map.entry("VENT", "ventose"), Map.entry("GERM", "germinal"),
		Map.entry("FLOR", "floreal"), Map.entry("PRAI", "prairial"), Map.entry("MESS", "messidor"), Map.entry("THER", "thermidor"),
		Map.entry("FRUC", "fructidor"), Map.entry("COMP", "complementaires"));

	private static final DateTimeFormatter GEDCOM_DATE_EXACT = new DateTimeFormatterBuilder()
		.parseCaseInsensitive()
		.appendPattern("d MMM uuuu")
		.toFormatter(Locale.ENGLISH);
	/** A GEDCOM time: {@code H:mm[:ss[.fraction]]}, with one or two digits for the hour. */
	private static final DateTimeFormatter GEDCOM_TIME = new DateTimeFormatterBuilder()
		.appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
		.appendLiteral(':')
		.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
		.optionalStart()
		.appendLiteral(':')
		.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
		.optionalStart()
		.appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
		.toFormatter(Locale.ROOT);


	private record SingleDate(String date, String calendar){}


	private GEDCOMDateConverter(){}


	/**
	 * Builds a {@code DateStructure} from a GEDCOM date.
	 *
	 * @param tag	The tag of the structure (e.g. {@code date}).
	 * @param gedcomDate	The GEDCOM date.
	 * @return	The structure, or {@code null} if the date cannot be normalized.
	 */
	public static FLEFRecord buildDateStructure(final String tag, final String gedcomDate){
		final List<FLEFRecord> structures = buildDateStructures(tag, gedcomDate);
		return (structures.isEmpty()? null: structures.getFirst());
	}

	/**
	 * Builds the {@code DateStructure}s of a GEDCOM date.
	 * <p>
	 * A date may be a list of alternatives, each one marked by a question mark ({@code 10 MAY 1991? 5 OCT 1991?}), or a calculated
	 * year that can be either of two ({@code CAL 1778/1779}): each alternative (for a range, each combination of the alternatives
	 * of its bounds) gives its own structure. A date marked by a question mark is approximate, with an unspecified basis unless
	 * qualified ({@code ABT}, {@code CAL}, {@code EST}).
	 *
	 * @param tag	The tag of the structures (e.g. {@code date}).
	 * @param gedcomDate	The GEDCOM date.
	 * @return	The structures, empty if the date cannot be normalized.
	 */
	public static List<FLEFRecord> buildDateStructures(final String tag, final String gedcomDate){
		final DateInfo dateInfo = GEDCOMDateParser.parse(gedcomDate);
		if(dateInfo == null || dateInfo.isPhraseOnly())
			return List.of();

		final List<FLEFRecord> values = buildDateValues(dateInfo);
		final List<FLEFRecord> structures = new ArrayList<>(values.size());
		for(final FLEFRecord value : values){
			final FLEFRecord dateStructure = FLEFRecord.createChildWithTag(tag)
				.addChild(value);
			if(StringUtils.isNotBlank(dateInfo.getPhrase()))
				dateStructure.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ORIGINAL_TEXT, dateInfo.getPhrase()));
			structures.add(dateStructure);
		}
		return structures;
	}

	/**
	 * Builds the {@code valid_from} and {@code valid_to} structures of a fact observed at a GEDCOM date.
	 * <p>
	 * A period ({@code FROM x TO y}) gives the bounds of validity directly; a single date is a point in time, so the fact is valid
	 * from that date and to that date; the bounds of a {@code BET x AND y} date are the ones of the interval.
	 *
	 * @param gedcomDate	The GEDCOM date.
	 * @return	The {@code valid_from} and {@code valid_to} structures (either may be {@code null}), or {@code null} if the
	 * 	date cannot be normalized.
	 */
	public static FLEFRecord[] buildValidity(final String gedcomDate){
		final DateInfo dateInfo = GEDCOMDateParser.parse(gedcomDate);
		if(dateInfo == null || dateInfo.isPhraseOnly())
			return null;

		final String basis = mapBasis(dateInfo);
		final FLEFRecord validFrom;
		final FLEFRecord validTo;
		switch(dateInfo.getType()){
			case SPANNING -> {
				validFrom = buildPointValidity(DateReader.TAG_VALID_FROM, first(dateInfo.getFrom()), dateInfo);
				validTo = buildPointValidity(DateReader.TAG_VALID_TO, first(dateInfo.getTo()), dateInfo);
				if(dateInfo.getFrom() != null && validFrom == null || dateInfo.getTo() != null && validTo == null)
					return null;
			}
			case BOUNDED -> {
				// the fact was true at some date in [notBefore, notAfter]
				validFrom = buildBoundedValidity(DateReader.TAG_VALID_FROM, DateReader.TAG_NOT_AFTER, first(dateInfo.getNotAfter()), dateInfo);
				validTo = buildBoundedValidity(DateReader.TAG_VALID_TO, DateReader.TAG_NOT_BEFORE, first(dateInfo.getNotBefore()), dateInfo);
				if(dateInfo.getNotAfter() != null && validFrom == null || dateInfo.getNotBefore() != null && validTo == null)
					return null;
			}
			default -> {
				//a single date is a point in time: the fact is valid from it and to it
				final FLEFRecord from = buildSingleDate(DateReader.TAG_POINT, first(dateInfo.getValue()), basis);
				final FLEFRecord to = buildSingleDate(DateReader.TAG_POINT, first(dateInfo.getValue()), basis);
				if(from == null)
					return null;

				validFrom = wrapDateValue(DateReader.TAG_VALID_FROM, null, from, dateInfo);
				validTo = wrapDateValue(DateReader.TAG_VALID_TO, null, to, dateInfo);
			}
		}
		return new FLEFRecord[]{validFrom, validTo};
	}

	private static FLEFRecord buildPointValidity(final String tag, final String date, final DateInfo dateInfo){
		final FLEFRecord singleDate = buildSingleDate(DateReader.TAG_POINT, date, null);
		return (singleDate != null? wrapDateValue(tag, null, singleDate, dateInfo): null);
	}

	private static FLEFRecord buildBoundedValidity(final String tag, final String boundTag, final String date,
			final DateInfo dateInfo){
		final FLEFRecord singleDate = buildSingleDate(boundTag, date, null);
		return (singleDate != null? wrapDateValue(tag, DateReader.TAG_BOUNDED, singleDate, dateInfo): null);
	}

	private static FLEFRecord wrapDateValue(final String tag, final String kind, final FLEFRecord content, final DateInfo dateInfo){
		final FLEFRecord value = FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
			.addChild(kind != null? FLEFRecord.createChildWithTag(kind).addChild(content): content);
		final FLEFRecord dateStructure = FLEFRecord.createChildWithTag(tag)
			.addChild(value);
		if(StringUtils.isNotBlank(dateInfo.getPhrase()))
			dateStructure.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ORIGINAL_TEXT, dateInfo.getPhrase()));
		return dateStructure;
	}

	/**
	 * Lists, as GEDCOM dates, the alternatives of a date after the first one (the one {@link #buildDateStructure} gives).
	 *
	 * @param gedcomDate	The GEDCOM date.
	 * @return	The other alternatives (e.g. {@code 5 OCT 1991?} for {@code 10 MAY 1991? 5 OCT 1991?}), empty if there are none.
	 */
	public static List<String> buildAlternativeTexts(final String gedcomDate){
		final DateInfo dateInfo = GEDCOMDateParser.parse(gedcomDate);
		if(dateInfo == null || dateInfo.isPhraseOnly())
			return List.of();

		final List<String> texts = new ArrayList<>();
		final String qualifier = dateInfo.getQualifier();
		switch(dateInfo.getType()){
			case POINT -> {
				for(final String alternative : alternatives(dateInfo.getValue()))
					texts.add(qualifier == null || QUALIFIER_PATTERN.matcher(alternative).matches()? alternative: qualifier + StringUtils.SPACE + alternative);
			}
			case BOUNDED -> {
				for(final String start : alternatives(dateInfo.getNotBefore()))
					for(final String end : alternatives(dateInfo.getNotAfter()))
						texts.add(start == null? "BEF " + end: (end == null? "AFT " + start: "BET " + start + " AND " + end));
			}
			default -> {
				for(final String start : alternatives(dateInfo.getFrom()))
					for(final String end : alternatives(dateInfo.getTo()))
						texts.add((start != null? "FROM " + start + (end != null? StringUtils.SPACE: StringUtils.EMPTY): StringUtils.EMPTY)
							+ (end != null? "TO " + end: StringUtils.EMPTY));
			}
		}
		return texts.subList(1, texts.size());
	}

	/** Builds the {@code DateValue}s of a date, one for each alternative; empty if any alternative cannot be normalized. */
	private static List<FLEFRecord> buildDateValues(final DateInfo dateInfo){
		final List<FLEFRecord> contents = new ArrayList<>();
		switch(dateInfo.getType()){
			case POINT -> {
				final String basis = mapBasis(dateInfo);
				for(final String alternative : alternatives(dateInfo.getValue()))
					contents.add(buildSingleDate(DateReader.TAG_POINT, alternative, basis));
			}
			case BOUNDED -> {
				for(final String start : alternatives(dateInfo.getNotBefore()))
					for(final String end : alternatives(dateInfo.getNotAfter()))
						contents.add(buildRange(DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, start, DateReader.TAG_NOT_AFTER, end));
			}
			default -> {
				for(final String start : alternatives(dateInfo.getFrom()))
					for(final String end : alternatives(dateInfo.getTo()))
						contents.add(buildRange(DateReader.TAG_SPANNING, DateReader.TAG_FROM, start, DateReader.TAG_TO, end));
			}
		}

		final List<FLEFRecord> values = new ArrayList<>(contents.size());
		for(final FLEFRecord content : contents){
			if(content == null)
				return List.of();

			values.add(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE).addChild(content));
		}
		return values;
	}

	/** The first alternative of a date (the validity of a fact keeps only that one). */
	private static String first(final String date){
		return alternatives(date).getFirst();
	}

	/**
	 * Splits a date in its alternatives: dates each marked by a question mark ({@code 1854? CAL 1858?}), and a calculated year that
	 * can be either of two ({@code CAL 1778/1779}). A date without alternatives is returned as is; {@code null} gives a single
	 * {@code null}.
	 */
	private static List<String> alternatives(final String date){
		if(date == null)
			return Collections.singletonList(null);

		final List<String> result = new ArrayList<>();
		for(final String part : ALTERNATIVE_SEPARATOR_PATTERN.split(date.trim())){
			final boolean uncertain = part.endsWith("?");
			final String core = (uncertain? Strings.CI.removeEnd(part, "?").trim(): part);
			final Matcher m = YEAR_ALTERNATIVE_PATTERN.matcher(core);
			if(m.matches()){
				final String suffix = (uncertain? "?": StringUtils.EMPTY);
				result.add(m.group(1) + m.group(2) + suffix);
				result.add(m.group(1) + m.group(3) + suffix);
			}
			else
				result.add(part);
		}
		return result;
	}

	private static FLEFRecord buildRange(final String kind, final String startTag, final String start, final String endTag,
			final String end){
		final FLEFRecord range = FLEFRecord.createChildWithTag(kind);
		if(start != null){
			final FLEFRecord startDate = buildSingleDate(startTag, start, null);
			if(startDate == null)
				return null;
			range.addChild(startDate);
		}
		if(end != null){
			final FLEFRecord endDate = buildSingleDate(endTag, end, null);
			if(endDate == null)
				return null;
			range.addChild(endDate);
		}
		return (range.hasChildren()? range: null);
	}

	/**
	 * Builds a {@code SingleDate}.
	 *
	 * @param tag	The tag of the structure.
	 * @param gedcomDate	A single GEDCOM date, possibly with a calendar escape.
	 * @param approximateBasis	The basis of the approximation, or {@code null} if the date is not approximate.
	 * @return	The structure, or {@code null} if the date cannot be normalized.
	 */
	private static FLEFRecord buildSingleDate(final String tag, final String gedcomDate, final String approximateBasis){
		if(StringUtils.isBlank(gedcomDate))
			return null;

		String date = gedcomDate.trim();
		//a question mark makes the date approximate
		final boolean uncertain = date.endsWith("?");
		if(uncertain)
			date = Strings.CI.removeEnd(date, "?").trim();
		//a qualifier of the date itself (e.g. the bound of `BEF ABT 1450`) prevails on the one of the whole date
		String basis = approximateBasis;
		final Matcher qualifierMatcher = QUALIFIER_PATTERN.matcher(date);
		if(qualifierMatcher.matches()){
			basis = mapBasis(qualifierMatcher.group(1).toUpperCase(Locale.ROOT));
			date = qualifierMatcher.group(2)
				.trim();
		}
		if(basis == null && uncertain)
			basis = DateReader.ENUM_BASIS_UNSPECIFIED;
		return buildSingleDate(tag, resolve(date), basis);
	}

	private static FLEFRecord buildSingleDate(final String tag, final SingleDate singleDate, final String approximateBasis){
		if(singleDate == null)
			return null;

		final FLEFRecord record = FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, singleDate.date())))
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, singleDate.calendar()));
		if(approximateBasis != null)
			record.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_APPROXIMATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_BASIS, approximateBasis)));
		return record;
	}

	/**
	 * Builds the {@code DateStructure} of the birth of a participant, calculated from its age at an event.
	 * <p>
	 * The year of the birth is the year of the event minus the years of the age, in the calendar of the event date; the full
	 * date is calculated only for a full gregorian date and an age with years, months, and days (or an age without years). An
	 * age qualified by {@code <} (less than) gives a lower bound ({@code not_before}), one qualified by {@code >} (more than) an
	 * upper bound ({@code not_after}), instead of a point.
	 *
	 * @param tag	The tag of the structure (e.g. {@code date}).
	 * @param gedcomDate	The GEDCOM date of the event.
	 * @param age	The GEDCOM age (e.g. {@code 25y}, {@code < 8y}, {@code 3y 2m 10d}).
	 * @return	The structure, or {@code null} if the age is not a numeric one (e.g. {@code CHILD}), or the date of the event is not
	 * 	a point that can be calculated from.
	 */
	public static FLEFRecord buildBirthDateFromAge(final String tag, final String gedcomDate, final String age){
		if(StringUtils.isBlank(age) || StringUtils.isBlank(gedcomDate))
			return null;

		final Matcher ageMatcher = AGE_PATTERN.matcher(StringUtils.normalizeSpace(age).toUpperCase(Locale.ROOT));
		if(!ageMatcher.matches())
			return null;

		final Integer years = parseAgeUnit(ageMatcher.group(2));
		final Integer months = parseAgeUnit(ageMatcher.group(3));
		final Integer weeks = parseAgeUnit(ageMatcher.group(4));
		final Integer days = parseAgeUnit(ageMatcher.group(5));
		if(years == null && months == null && weeks == null && days == null)
			return null;

		final DateInfo dateInfo = GEDCOMDateParser.parse(gedcomDate);
		if(dateInfo == null || dateInfo.isPhraseOnly() || dateInfo.getType() != DateInfo.Type.POINT)
			return null;

		final SingleDate eventDate = resolve(dateInfo.getValue());
		if(eventDate == null || eventDate.date().contains("BCE"))
			return null;

		final String[] tokens = StringUtils.split(eventDate.date());
		final String yearToken = StringUtils.substringBefore(tokens[tokens.length - 1], "/");
		if(!StringUtils.isNumeric(yearToken))
			return null;

		final int eventYear = Integer.parseInt(yearToken);
		//the full date can be calculated only with the gregorian calendar
		LocalDate fullDate = null;
		final Matcher dateMatcher = GREGORIAN_DATE_PATTERN.matcher(eventDate.date());
		if(DateReader.ENUM_CALENDAR_GREGORIAN.equals(eventDate.calendar()) && dateMatcher.matches() && dateMatcher.group(1) != null
				&& dateMatcher.group(2) != null && dateMatcher.group(4) == null && dateMatcher.group(5) == null){
			try{
				fullDate = LocalDate.parse(StringUtils.normalizeSpace(eventDate.date()), GEDCOM_DATE_EXACT);
			}
			catch(final DateTimeParseException ignored){}
		}

		final String birth;
		if(fullDate != null && (years == null || months != null && (days != null || weeks != null))){
			final LocalDate birthDate = fullDate.minusYears(years != null? years: 0)
				.minusMonths(months != null? months: 0)
				.minusDays((weeks != null? weeks * 7L: 0L) + (days != null? days: 0));
			birth = GEDCOM_DATE_EXACT.format(birthDate)
				.toUpperCase(Locale.ROOT);
		}
		else if(years != null && eventYear - years > 0)
			birth = String.valueOf(eventYear - years);
		else
			return null;

		final String qualifier = ageMatcher.group(1);
		final SingleDate birthSingleDate = new SingleDate(birth, eventDate.calendar());
		final FLEFRecord content;
		if(qualifier == null)
			content = buildSingleDate(DateReader.TAG_POINT, birthSingleDate, DateReader.ENUM_BASIS_CALCULATED);
		else
			//younger than the age: born after the calculated date, older: born before it
			content = FLEFRecord.createChildWithTag(DateReader.TAG_BOUNDED)
				.addChild(buildSingleDate(("<".equals(qualifier)? DateReader.TAG_NOT_BEFORE: DateReader.TAG_NOT_AFTER), birthSingleDate,
					DateReader.ENUM_BASIS_CALCULATED));
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(content));
	}

	private static Integer parseAgeUnit(final String value){
		return (value != null? Integer.valueOf(value): null);
	}

	private static SingleDate resolve(final String gedcomDate){
		if(StringUtils.isBlank(gedcomDate))
			return null;

		String date = gedcomDate.trim();
		String calendar = null;
		final Matcher escapeMatcher = ESCAPE_PATTERN.matcher(date);
		if(escapeMatcher.matches()){
			calendar = switch(escapeMatcher.group(1).trim().toUpperCase(Locale.ROOT)){
				case "GREGORIAN" -> DateReader.ENUM_CALENDAR_GREGORIAN;
				case "JULIAN" -> DateReader.ENUM_CALENDAR_JULIAN;
				case "HEBREW" -> DateReader.ENUM_CALENDAR_HEBREW;
				case "FRENCH R" -> DateReader.ENUM_CALENDAR_FRENCH_REPUBLICAN;
				//ROMAN, UNKNOWN
				default -> null;
			};
			if(calendar == null)
				return null;

			date = escapeMatcher.group(2)
				.trim();
		}
		date = StringUtils.normalizeSpace(date)
			.toUpperCase(Locale.ROOT);

		final Matcher m = MONTH_DATE_PATTERN.matcher(date);
		if(calendar == null && m.matches() && m.group(2) != null){
			// some programs omit the escape: the month codes of these calendars tell it
			if(HEBREW_MONTHS.containsKey(m.group(2)))
				calendar = DateReader.ENUM_CALENDAR_HEBREW;
			else if(FRENCH_REPUBLICAN_MONTHS.containsKey(m.group(2)))
				calendar = DateReader.ENUM_CALENDAR_FRENCH_REPUBLICAN;
		}
		if(DateReader.ENUM_CALENDAR_HEBREW.equals(calendar) || DateReader.ENUM_CALENDAR_FRENCH_REPUBLICAN.equals(calendar)){
			if(!m.matches())
				return null;

			final StringBuilder sb = new StringBuilder();
			if(m.group(2) != null){
				final String month = (DateReader.ENUM_CALENDAR_HEBREW.equals(calendar)? HEBREW_MONTHS: FRENCH_REPUBLICAN_MONTHS)
					.get(m.group(2));
				if(month == null)
					return null;

				if(m.group(1) != null)
					sb.append(m.group(1)).append(' ');
				sb.append(month).append(' ');
			}
			sb.append(m.group(3));
			return new SingleDate(sb.toString(), calendar);
		}

		// the era is written as "BCE", or omitted when it is the common era
		final Matcher eraMatcher = BEFORE_COMMON_ERA_PATTERN.matcher(date);
		date = (eraMatcher.find()
			? eraMatcher.replaceFirst(" BCE")
			: COMMON_ERA_PATTERN.matcher(date).replaceFirst(StringUtils.EMPTY));
		final Matcher gm = GREGORIAN_DATE_PATTERN.matcher(date);
		if(!gm.matches()){
			//a season (e.g. "SPR 1935") is not a GEDCOM month, but the calendar parser reads it
			if(m.matches() && m.group(1) == null && m.group(2) != null && isReadByCalendarParser(date))
				return new SingleDate(date, (calendar != null? calendar: (Integer.parseInt(m.group(3)) < 1582
					? DateReader.ENUM_CALENDAR_JULIAN: DateReader.ENUM_CALENDAR_GREGORIAN)));
			return null;
		}

		if(calendar == null)
			calendar = (gm.group(4) != null || gm.group(5) != null || Integer.parseInt(gm.group(3)) < 1582? DateReader.ENUM_CALENDAR_JULIAN: DateReader.ENUM_CALENDAR_GREGORIAN);
		return new SingleDate(date, calendar);
	}

	private static boolean isReadByCalendarParser(final String date){
		try{
			GregorianCalendarParser.parse(date);
			return true;
		}
		catch(final IllegalArgumentException ignored){
			return false;
		}
	}

	private static String mapBasis(final DateInfo dateInfo){
		return (dateInfo.isApproximate()? mapBasis(dateInfo.getQualifier()): null);
	}

	private static String mapBasis(final String qualifier){
		//EST is "estimated based on an algorithm using some other event date": `conventional` would require the cultural norm
		//that justifies it
		return switch(qualifier){
			case "ABT" -> DateReader.ENUM_BASIS_STATED;
			case "CAL" -> DateReader.ENUM_BASIS_CALCULATED;
			case null, default -> DateReader.ENUM_BASIS_UNSPECIFIED;
		};
	}


	/**
	 * Converts a GEDCOM {@code DATE} node (with its optional {@code TIME} child) into an ISO 8601 date-time.
	 * <p>
	 * GEDCOM does not state the time zone, so the time is taken as UTC.
	 *
	 * @param dateNode	The GEDCOM {@code DATE} node.
	 * @return	The date-time (e.g. {@code 2026-10-14T20:51:22Z}), or {@code null} if the date is not an exact date.
	 */
	public static String toDateTime(final GEDCOMNode dateNode){
		if(dateNode == null || StringUtils.isBlank(dateNode.getValue()))
			return null;

		final LocalDate date;
		try{
			date = LocalDate.parse(StringUtils.normalizeSpace(dateNode.getValue()), GEDCOM_DATE_EXACT);
		}
		catch(final DateTimeParseException ignored){
			return null;
		}

		LocalTime time = LocalTime.MIDNIGHT;
		final GEDCOMNode timeNode = GEDCOMHelper.findFirstChild(dateNode, "TIME");
		if(timeNode != null && StringUtils.isNotBlank(timeNode.getValue())){
			try{
				// GEDCOM 7 marks UTC with a trailing Z, the time zone GEDCOM 5.5.1 times are taken in anyway
				time = LocalTime.parse(Strings.CS.removeEnd(timeNode.getValue().trim(), "Z"), GEDCOM_TIME);
			}
			catch(final DateTimeParseException ignored){}
		}
		return LocalDateTime.of(date, time.withNano(0))
			.atOffset(ZoneOffset.UTC)
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

}

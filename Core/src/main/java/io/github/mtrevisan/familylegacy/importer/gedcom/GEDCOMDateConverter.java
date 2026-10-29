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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
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

	private static final String CALENDAR_GREGORIAN = "gregorian";
	private static final String CALENDAR_JULIAN = "julian";
	private static final String CALENDAR_HEBREW = "hebrew";
	private static final String CALENDAR_FRENCH_REPUBLICAN = "french_republican";


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
		final DateInfo dateInfo = GEDCOMDateParser.parse(gedcomDate);
		if(dateInfo == null || dateInfo.isPhraseOnly())
			return null;

		final FLEFRecord value = buildDateValue(dateInfo);
		if(value == null)
			return null;

		final FLEFRecord dateStructure = FLEFRecord.createChildWithTag(tag)
			.addChild(value);
		if(StringUtils.isNotBlank(dateInfo.getPhrase()))
			dateStructure.addChild(FLEFRecord.createChildWithTagAndValue("original_text", dateInfo.getPhrase()));
		return dateStructure;
	}

	/**
	 * Builds the {@code valid_from} and {@code valid_to} structures of a fact observed at a GEDCOM date.
	 * <p>
	 * A period ({@code FROM x TO y}) gives the bounds of validity directly; any other date only states that the fact was true
	 * at that date, so it started no later than it and ended no earlier than it.
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
				validFrom = buildPointValidity("valid_from", dateInfo.getFrom(), dateInfo);
				validTo = buildPointValidity("valid_to", dateInfo.getTo(), dateInfo);
				if(dateInfo.getFrom() != null && validFrom == null || dateInfo.getTo() != null && validTo == null)
					return null;
			}
			case BOUNDED -> {
				// the fact was true at some date in [notBefore, notAfter]
				validFrom = buildBoundedValidity("valid_from", "not_after", dateInfo.getNotAfter(), dateInfo);
				validTo = buildBoundedValidity("valid_to", "not_before", dateInfo.getNotBefore(), dateInfo);
				if(dateInfo.getNotAfter() != null && validFrom == null || dateInfo.getNotBefore() != null && validTo == null)
					return null;
			}
			default -> {
				final FLEFRecord notAfter = buildSingleDate("not_after", dateInfo.getValue(), basis);
				final FLEFRecord notBefore = buildSingleDate("not_before", dateInfo.getValue(), basis);
				if(notAfter == null)
					return null;

				validFrom = wrapDateValue("valid_from", "bounded", notAfter, dateInfo);
				validTo = wrapDateValue("valid_to", "bounded", notBefore, dateInfo);
			}
		}
		return new FLEFRecord[]{validFrom, validTo};
	}

	private static FLEFRecord buildPointValidity(final String tag, final String date, final DateInfo dateInfo){
		final FLEFRecord singleDate = buildSingleDate("point", date, null);
		return (singleDate != null? wrapDateValue(tag, null, singleDate, dateInfo): null);
	}

	private static FLEFRecord buildBoundedValidity(final String tag, final String boundTag, final String date,
			final DateInfo dateInfo){
		final FLEFRecord singleDate = buildSingleDate(boundTag, date, null);
		return (singleDate != null? wrapDateValue(tag, "bounded", singleDate, dateInfo): null);
	}

	private static FLEFRecord wrapDateValue(final String tag, final String kind, final FLEFRecord content, final DateInfo dateInfo){
		final FLEFRecord value = FLEFRecord.createChildWithTag("value")
			.addChild(kind != null? FLEFRecord.createChildWithTag(kind).addChild(content): content);
		final FLEFRecord dateStructure = FLEFRecord.createChildWithTag(tag)
			.addChild(value);
		if(StringUtils.isNotBlank(dateInfo.getPhrase()))
			dateStructure.addChild(FLEFRecord.createChildWithTagAndValue("original_text", dateInfo.getPhrase()));
		return dateStructure;
	}

	private static FLEFRecord buildDateValue(final DateInfo dateInfo){
		final FLEFRecord content;
		switch(dateInfo.getType()){
			case POINT -> content = buildSingleDate("point", dateInfo.getValue(), mapBasis(dateInfo));
			case BOUNDED -> content = buildRange("bounded", "not_before", dateInfo.getNotBefore(),
				"not_after", dateInfo.getNotAfter());
			default -> content = buildRange("spanning", "from", dateInfo.getFrom(), "to", dateInfo.getTo());
		}
		return (content != null? FLEFRecord.createChildWithTag("value").addChild(content): null);
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
		final SingleDate singleDate = resolve(gedcomDate);
		if(singleDate == null)
			return null;

		final FLEFRecord record = FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTagAndValue("full_date", singleDate.date())))
			.addChild(FLEFRecord.createChildWithTagAndValue("calendar", singleDate.calendar()));
		if(approximateBasis != null)
			record.addChild(FLEFRecord.createChildWithTag("approximate")
				.addChild(FLEFRecord.createChildWithTagAndValue("basis", approximateBasis)));
		return record;
	}

	private static SingleDate resolve(final String gedcomDate){
		if(StringUtils.isBlank(gedcomDate))
			return null;

		String date = gedcomDate.trim();
		String calendar = null;
		final Matcher escapeMatcher = ESCAPE_PATTERN.matcher(date);
		if(escapeMatcher.matches()){
			calendar = switch(escapeMatcher.group(1).trim().toUpperCase(Locale.ROOT)){
				case "GREGORIAN" -> CALENDAR_GREGORIAN;
				case "JULIAN" -> CALENDAR_JULIAN;
				case "HEBREW" -> CALENDAR_HEBREW;
				case "FRENCH R" -> CALENDAR_FRENCH_REPUBLICAN;
				//ROMAN, UNKNOWN
				default -> null;
			};
			if(calendar == null)
				return null;

			date = escapeMatcher.group(2).trim();
		}
		date = StringUtils.normalizeSpace(date).toUpperCase(Locale.ROOT);

		final Matcher m = MONTH_DATE_PATTERN.matcher(date);
		if(calendar == null && m.matches() && m.group(2) != null){
			// some programs omit the escape: the month codes of these calendars tell it
			if(HEBREW_MONTHS.containsKey(m.group(2)))
				calendar = CALENDAR_HEBREW;
			else if(FRENCH_REPUBLICAN_MONTHS.containsKey(m.group(2)))
				calendar = CALENDAR_FRENCH_REPUBLICAN;
		}
		if(CALENDAR_HEBREW.equals(calendar) || CALENDAR_FRENCH_REPUBLICAN.equals(calendar)){
			if(!m.matches())
				return null;

			final StringBuilder sb = new StringBuilder();
			if(m.group(2) != null){
				final String month = (CALENDAR_HEBREW.equals(calendar)? HEBREW_MONTHS: FRENCH_REPUBLICAN_MONTHS)
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
		if(!gm.matches())
			return null;

		if(calendar == null)
			calendar = (gm.group(5) != null || Integer.parseInt(gm.group(3)) < 1582? CALENDAR_JULIAN: CALENDAR_GREGORIAN);
		return new SingleDate(date, calendar);
	}

	private static String mapBasis(final DateInfo dateInfo){
		if(!dateInfo.isApproximate())
			return null;

		//EST is "estimated based on an algorithm using some other event date": `conventional` would require the cultural norm
		//that justifies it
		return switch(dateInfo.getQualifier()){
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
				time = LocalTime.parse(StringUtils.removeEnd(timeNode.getValue().trim(), "Z"));
			}
			catch(final DateTimeParseException ignored){}
		}
		return LocalDateTime.of(date, time.withNano(0))
			.atOffset(ZoneOffset.UTC)
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

}

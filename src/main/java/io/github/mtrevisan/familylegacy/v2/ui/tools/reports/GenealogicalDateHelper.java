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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;


/**
 * Extracts the (approximate) year and the rich display form from any
 * {@code DateStructure} attached to a FLEF record.
 *
 * <p>Two families of methods are provided:</p>
 * <ul>
 *   <li><b>Numeric</b> — {@link #yearOrNull(FLEFRecord)} and
 *       {@link #yearOrMax(FLEFRecord)}, used for chronological sorting and
 *       lifespan computation;</li>
 *   <li><b>Display</b> — {@link #formatEventDate} and
 *       {@link #formatDateStructure}, which combine the source's
 *       {@code original_text}, the normalized date value and the
 *       qualifiers ({@code approximate.basis}, {@code approximate.margin},
 *       {@code approximate.cultural_norm}, {@code calendar},
 *       {@code century.part}) into a single localized string.</li>
 * </ul>
 *
 * <p>Qualifiers are resolved on every branch of the {@code SingleDate}
 * oneof — {@code full_date}, {@code decade} and {@code century} — so a
 * "circa 1950s" or a "circa early 15th century" is displayed with its full
 * qualifier chain.</p>
 */
final class GenealogicalDateHelper{

	/* ======================================================================
	 *                          Numeric paths
	 * ====================================================================== */

	private static final String PATH_FULL_DATE_VALUE = "date.value.point.full_date.value";
	private static final String PATH_FULL_DATE_CAL = "date.value.point.full_date.calendar";
	private static final String PATH_DECADE_START = "date.value.point.decade.start_year";
	private static final String PATH_CENTURY_ORDINAL = "date.value.point.century.ordinal";

	private static final String[] SINGLE_DATE_BASES = {
		"date.value.bounded.not_before",
		"date.value.bounded.not_after",
		"date.value.spanning.from",
		"date.value.spanning.to"
	};

	private static final String DEFAULT_CALENDAR = "gregorian";


	private GenealogicalDateHelper(){
	}


	/* ======================================================================
	 *                          Numeric API
	 * ====================================================================== */

	static int yearOrMax(final FLEFRecord event){
		final Integer y = yearOrNull(event);
		return (y != null? y: Integer.MAX_VALUE);
	}

	static Integer yearOrNull(final FLEFRecord event){
		final Integer y = readFullDate(event, "date.value.point");
		if(y != null)
			return y;
		final Integer d = readDecade(event, "date.value.point");
		if(d != null)
			return d;
		final Integer c = readCentury(event, "date.value.point");
		if(c != null)
			return c;
		for(final String base : SINGLE_DATE_BASES){
			final Integer b = readFullDate(event, base);
			if(b != null)
				return b;
			final Integer bd = readDecade(event, base);
			if(bd != null)
				return bd;
			final Integer bc = readCentury(event, base);
			if(bc != null)
				return bc;
		}
		return null;
	}

	static LocalDate exactDateOrNull(final FLEFRecord event){
		final String value = FLEFRecordHelper.getChildValue(event, PATH_FULL_DATE_VALUE);
		if(value == null || value.isBlank())
			return null;
		final String calendar = FLEFRecordHelper.getChildValue(event, PATH_FULL_DATE_CAL);
		return parseExactDate(calendar, value);
	}


	/* ======================================================================
	 *                          Display API
	 * ====================================================================== */

	static String formatEventDate(final FLEFRecord event, final ReportLabels labels){
		return formatEventDate(event, labels, null);
	}

	static String formatEventDate(final FLEFRecord event, final ReportLabels labels,
		final Function<String, String> normResolver){
		return formatDateStructure(event, "date", labels, normResolver);
	}

	static String formatDateStructure(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels, final Function<String, String> normResolver){
		final String base = baseDate(rec, fieldTag, labels);
		if(base == null)
			return null;

		final List<String> qualifiers = new ArrayList<>();
		final String basis = approximateBasis(rec, fieldTag, labels, normResolver);
		if(basis != null) qualifiers.add(basis);
		final String margin = approximateMargin(rec, fieldTag, labels);
		if(margin != null) qualifiers.add(margin);
		final String calendar = calendarName(rec, fieldTag, labels);
		if(calendar != null) qualifiers.add(calendar);

		if(qualifiers.isEmpty())
			return base;
		return base + " (" + String.join(", ", qualifiers) + ")";
	}


	/* ======================================================================
	 *                          Base date
	 * ====================================================================== */

	private static String baseDate(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels){
		final String originalText = FLEFRecordHelper.getChildValue(rec,
			fieldTag + ".original_text");
		if(originalText != null && !originalText.isBlank())
			return originalText.trim();
		return normalizedDate(rec, fieldTag, labels);
	}

	private static String normalizedDate(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels){
		final String point = singleDate(rec, fieldTag + ".value.point", labels);
		if(point != null)
			return point;

		final String notBefore = singleDate(rec, fieldTag + ".value.bounded.not_before", labels);
		final String notAfter = singleDate(rec, fieldTag + ".value.bounded.not_after", labels);
		if(notBefore != null && notAfter != null)
			return String.format(labels.dates().dateBetween(), notBefore, notAfter);
		if(notBefore != null)
			return String.format(labels.dates().dateAfter(), notBefore);
		if(notAfter != null)
			return String.format(labels.dates().dateBefore(), notAfter);

		final String from = singleDate(rec, fieldTag + ".value.spanning.from", labels);
		final String to = singleDate(rec, fieldTag + ".value.spanning.to", labels);
		if(from != null && to != null)
			return String.format(labels.dates().dateFromTo(), from, to);
		if(from != null)
			return String.format(labels.dates().dateFrom(), from);
		if(to != null)
			return String.format(labels.dates().dateTo(), to);

		return null;
	}

	private static String singleDate(final FLEFRecord rec, final String base,
		final ReportLabels labels){
		// full_date
		final String full = FLEFRecordHelper.getChildValue(rec, base + ".full_date.value");
		if(full != null && !full.isBlank())
			return full.trim();

		// decade
		final String decade = FLEFRecordHelper.getChildValue(rec, base + ".decade.start_year");
		if(decade != null && !decade.isBlank())
			return String.format(labels.dates().dateDecade(), decade.trim());

		// century
		final String ordinal = FLEFRecordHelper.getChildValue(rec, base + ".century.ordinal");
		if(ordinal != null && !ordinal.isBlank()){
			final String part = FLEFRecordHelper.getChildValue(rec, base + ".century.part");
			if(part != null && !part.isBlank())
				return String.format(labels.dates().dateCenturyWithPart(),
					labels.dates().centuryPart(part), ordinal.trim());
			return String.format(labels.dates().dateCentury(), ordinal.trim());
		}
		return null;
	}


	/* ======================================================================
	 *                          Qualifiers (all branches)
	 * ====================================================================== */

	/**
	 * Finds the first {@code approximate} node anywhere under the date
	 * structure rooted at {@code fieldTag}. This covers all branches:
	 * {@code point.full_date}, {@code point.decade}, {@code point.century},
	 * and every {@code SingleDate} inside {@code bounded} and {@code spanning}.
	 */
	private static String approximateBasis(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels, final Function<String, String> normResolver){
		final FLEFRecord approx = findApproximate(rec, fieldTag);
		if(approx == null)
			return null;

		final String basis = FLEFRecordHelper.getChildValue(approx, "basis");
		if(basis == null || basis.isBlank())
			return labels.dates().approxBasisUnspecified();

		return switch(basis.toLowerCase(Locale.ROOT)){
			case "stated" -> labels.dates().approxBasisStated();
			case "calculated" -> labels.dates().approxBasisCalculated();
			case "conventional" -> {
				final List<String> norms = resolveNormTitles(approx, normResolver);
				yield (norms.isEmpty()
					? labels.dates().approxBasisConventional()
					: labels.dates().approxBasisConventionalPer(String.join(", ", norms)));
			}
			default -> labels.dates().approxBasisUnspecified();
		};
	}

	private static String approximateMargin(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels){
		final FLEFRecord approx = findApproximate(rec, fieldTag);
		if(approx == null)
			return null;
		return parseMargin(FLEFRecordHelper.getChildValue(approx, "margin"), labels);
	}

	/**
	 * Finds the first {@code calendar} node under the date structure. The
	 * branch the calendar belongs to does not matter, because every branch
	 * of the {@code SingleDate} oneof declares its own calendar and they are
	 * all equally informative.
	 */
	private static String calendarName(final FLEFRecord rec, final String fieldTag,
		final ReportLabels labels){
		final FLEFRecord dateField = FLEFRecordHelper.findChild(rec, fieldTag);
		if(dateField == null)
			return null;
		final FLEFRecord calendarNode = findFirstChildByTag(dateField, "calendar");
		if(calendarNode == null)
			return null;
		final String code = calendarNode.getValue();
		if(code == null || code.isBlank())
			return null;
		final String normalized = normalizeCalendar(code);
		if("gregorian".equals(normalized))
			return null;
		return labels.dates().calendarDisplay(normalized);
	}


	/* ======================================================================
	 *                          Tree helpers
	 * ====================================================================== */

	/**
	 * Finds the first {@code approximate} node anywhere in the subtree
	 * rooted at the date field. The traversal is depth-first and preserves
	 * document order, so the qualifier closest to the primary branch wins.
	 */
	private static FLEFRecord findApproximate(final FLEFRecord rec, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rec, fieldTag);
		return (field != null? findFirstChildByTag(field, "approximate"): null);
	}

	private static FLEFRecord findFirstChildByTag(final FLEFRecord rec, final String tag){
		for(final FLEFRecord child : rec.getChildren()){
			if(tag.equalsIgnoreCase(child.getTag()))
				return child;
			final FLEFRecord found = findFirstChildByTag(child, tag);
			if(found != null)
				return found;
		}
		return null;
	}

	private static List<String> resolveNormTitles(final FLEFRecord approx,
		final Function<String, String> normResolver){
		if(normResolver == null)
			return List.of();

		final List<String> out = new ArrayList<>();
		for(final FLEFRecord child : approx.getChildren()){
			if(!"cultural_norm".equalsIgnoreCase(child.getTag()))
				continue;
			final String id = child.getValue();
			if(id == null || id.isBlank())
				continue;
			final String label = normResolver.apply(id);
			if(label != null && !label.isBlank())
				out.add(label);
		}
		return out;
	}


	/* ======================================================================
	 *                          Margin parsing
	 * ====================================================================== */

	private static String parseMargin(final String isoDuration, final ReportLabels labels){
		if(isoDuration == null || isoDuration.isBlank())
			return null;
		final String s = isoDuration.trim();
		if(!s.startsWith("P"))
			return null;

		final List<String> parts = new ArrayList<>();
		int i = 1;
		while(i < s.length()){
			int j = i;
			while(j < s.length() && Character.isDigit(s.charAt(j))) j++;
			if(j == i)
				break;
			final int value;
			try{
				value = Integer.parseInt(s.substring(i, j));
			}
			catch(final NumberFormatException ignored){
				return null;
			}
			if(j >= s.length())
				break;
			final char unit = s.charAt(j);
			i = j + 1;
			final String label = switch(unit){
				case 'Y' -> labels.marginYears(value);
				case 'M' -> labels.marginMonths(value);
				case 'W' -> labels.marginWeeks(value);
				case 'D' -> labels.marginDays(value);
				default -> null;
			};
			if(label != null) parts.add(label);
		}
		if(parts.isEmpty())
			return null;
		return String.format(labels.dates().dateMargin(), String.join(", ", parts));
	}


	/* ======================================================================
	 *                          Numeric readers
	 * ====================================================================== */

	private static Integer readFullDate(final FLEFRecord event, final String base){
		final String value = FLEFRecordHelper.getChildValue(event, base + ".full_date.value");
		if(value == null || value.isBlank())
			return null;
		final String calendar = FLEFRecordHelper.getChildValue(event, base + ".full_date.calendar");
		return parseYear(calendar, value);
	}

	private static Integer readDecade(final FLEFRecord event, final String base){
		final String v = FLEFRecordHelper.getChildValue(event, base + ".decade.start_year");
		if(v == null || v.isBlank())
			return null;
		try{
			return Integer.parseInt(v.trim());
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static Integer readCentury(final FLEFRecord event, final String base){
		final String v = FLEFRecordHelper.getChildValue(event, base + ".century.ordinal");
		if(v == null || v.isBlank())
			return null;
		try{
			return (Integer.parseInt(v.trim()) - 1) * 100 + 50;
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static Integer parseYear(final String calendarCode, final String rawDate){
		if(rawDate == null || rawDate.isBlank())
			return null;
		try{
			final ParsedGenealogicalDate parsed = UniversalDateConverter.parse(
				normalizeCalendar(calendarCode), rawDate);
			if(parsed.isoDate() != null)
				return parsed.isoDate().getYear();
		}
		catch(final RuntimeException ignored){
		}
		return null;
	}

	private static LocalDate parseExactDate(final String calendarCode, final String rawDate){
		if(rawDate == null || rawDate.isBlank())
			return null;
		try{
			final ParsedGenealogicalDate parsed = UniversalDateConverter.parse(
				normalizeCalendar(calendarCode), rawDate);
			if(parsed.precision() != ParsedGenealogicalDate.DatePrecision.EXACT)
				return null;
			return parsed.isoDate();
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	private static String normalizeCalendar(final String calendarCode){
		if(calendarCode == null || calendarCode.isBlank())
			return DEFAULT_CALENDAR;
		String c = calendarCode.trim();
		if(c.startsWith("@#") && c.endsWith("@"))
			c = c.substring(2, c.length() - 1);
		else if(c.startsWith("@#"))
			c = c.substring(2);
		return c.toLowerCase(Locale.ROOT);
	}

}

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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.i18n;

import com.ibm.icu.text.MessageFormat;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportLabels;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Handles localized display formatting for historical dates, age calculations,
 * century subdivisions, and plural-sensitive time units.
 */
public final class DateLabelProvider {

	private final ReportLabels labels;

	public DateLabelProvider(final ReportLabels labels){
		this.labels = labels;
	}

	public String dateDecade(){ return labels.getString("DATE_DECADE"); }
	public String dateCentury(){ return labels.getString("DATE_CENTURY"); }
	public String dateCenturyWithPart(){ return labels.getString("DATE_CENTURY_WITH_PART"); }
	public String dateBetween(){ return labels.getString("DATE_BETWEEN"); }
	public String dateAfter(){ return labels.getString("DATE_AFTER"); }
	public String dateBefore(){ return labels.getString("DATE_BEFORE"); }
	public String dateFromTo(){ return labels.getString("DATE_FROM_TO"); }
	public String dateFrom(){ return labels.getString("DATE_FROM"); }
	public String dateTo(){ return labels.getString("DATE_TO"); }
	public String dateMargin(){ return labels.getString("DATE_MARGIN"); }

	public String approxBasisStated(){
		return labels.getString("APPROX_BASIS_STATED");
	}

	public String approxBasisCalculated(){
		return labels.getString("APPROX_BASIS_CALCULATED");
	}

	public String approxBasisConventional(){
		return labels.getString("APPROX_BASIS_CONVENTIONAL");
	}

	public String approxBasisUnspecified(){
		return labels.getString("APPROX_BASIS_UNSPECIFIED");
	}

	public String approxBasisConventionalPer(final String titles){
		return String.format(labels.getString("APPROX_BASIS_CONVENTIONAL_PER"), titles);
	}


	/**
	 * Localized display for the {@code CenturyPart} enum. Unknown values are
	 * prettified by replacing underscores with spaces.
	 */
	public String centuryPart(final String part){
		if(part == null || part.isBlank())
			return StringUtils.EMPTY;
		final String key = "CENTURY_PART_" + part.toUpperCase(Locale.ROOT);
		try{
			return labels.getString(key);
		}catch(final Exception ignored){
			return part.replace('_', ' ');
		}
	}

	/**
	 * Localized display for a calendar code. Unknown calendars fall back to
	 * the raw code.
	 */
	public String calendarDisplay(final String code){
		if(code == null || code.isBlank())
			return StringUtils.EMPTY;
		final String key = "CALENDAR_" + code.toUpperCase(Locale.ROOT);
		try{
			return labels.getString(key);
		}catch(final Exception ignored){
			return code;
		}
	}

	/* ======================================================================
	 *                          Plural-aware Time Units
	 * ====================================================================== */

	public String ageYears(final int years){
		return labels.pluralKey("AGE_YEARS", years);
	}

	public String marginYears(final int n){
		return labels.pluralKey("MARGIN_YEARS", n);
	}

	public String marginMonths(final int n){
		return labels.pluralKey("MARGIN_MONTHS", n);
	}

	public String marginWeeks(final int n){
		return labels.pluralKey("MARGIN_WEEKS", n);
	}

	public String marginDays(final int n){
		return labels.pluralKey("MARGIN_DAYS", n);
	}

	/**
	 * Plural-aware relation-count line, with three nested plurals (parents,
	 * spouses, children).
	 */
	public String relationsCount(final int parents, final int spouses, final int children){
		return new MessageFormat(labels.getString("RELATIONS_COUNT"), labels.language().locale())
			.format(new Object[]{parents, spouses, children});
	}

}

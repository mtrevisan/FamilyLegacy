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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthand;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;


/**
 * Says in words what a date typed in the shorthand of the date editor means (so that the user can check it before it is saved), or what is
 * wrong with it.
 */
public final class DateShorthandDescriber{

	private DateShorthandDescriber(){}


	/** @return	What is wrong with the expression, in words. */
	public static String describeProblem(final DateShorthandReader.SyntaxException exception){
		return I18N.t("dialog.date.quick_entry.error." + exception.getReason().name().toLowerCase(Locale.ROOT));
	}

	/** @return	What the expression means, e.g. {@code Bounded: not before 1850 (approx., stated), not after 1860}. */
	public static String describe(final DateShorthand shorthand){
		final String first = (shorthand.first() != null? describe(shorthand.first()): null);
		final String second = (shorthand.second() != null? describe(shorthand.second()): null);
		return switch(shorthand.kind()){
			case POINT -> I18N.tf("dialog.date.quick_entry.preview.point", first);
			case BOUNDED -> (first != null && second != null
				? I18N.tf("dialog.date.quick_entry.preview.bounded.both", first, second)
				: (first != null
					? I18N.tf("dialog.date.quick_entry.preview.bounded.not_before", first)
					: I18N.tf("dialog.date.quick_entry.preview.bounded.not_after", second)));
			case SPANNING -> (first != null && second != null
				? I18N.tf("dialog.date.quick_entry.preview.spanning.both", first, second)
				: (first != null
					? I18N.tf("dialog.date.quick_entry.preview.spanning.from", first)
					: I18N.tf("dialog.date.quick_entry.preview.spanning.to", second)));
		};
	}

	private static String describe(final DateShorthand.Single date){
		final StringBuilder text = new StringBuilder();
		if(date.decade() != null)
			text.append(I18N.tf("dialog.date.description.decade", String.valueOf(date.decade())));
		else if(date.century() != null){
			//the numbers are given as text to avoid the grouping separator of the locale
			if(date.centuryPart() != null)
				text.append(I18N.tf("dialog.date.description.century.abbreviation_with_part",
					I18N.t("enum.date.century_part." + date.centuryPart()), String.valueOf(date.century())));
			else
				text.append(I18N.tf("dialog.date.description.century.abbreviation", String.valueOf(date.century())));
		}
		else
			text.append(date.fullDate());
		if(date.approximate()){
			text.append(" (")
				.append(I18N.t("dialog.date.description.approximate.abbreviation"))
				.append(", ")
				.append(I18N.t("enum.date.basis." + date.basis()));
			if(date.margin() != null)
				text.append(", ")
					.append(I18N.t("dialog.date.description.margin"))
					.append(StringUtils.SPACE)
					.append(date.margin());
			text.append(')');
		}
		return text.toString();
	}

}

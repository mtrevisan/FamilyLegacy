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

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses GEDCOM date strings (e.g., "2 APR 1956", "AFT 3 MAY 1894", "BET 1900 AND 1910", "INT 1900 (about 1900)")
 * and produces a {@link DateInfo} object that describes the intended FLEF structure.
 * <p>
 * The single dates inside the result keep their calendar escape (e.g. "@#DJULIAN@ 1 MAR 1700"); see
 * {@link GEDCOMDateConverter} for their normalization.
 */
public final class GEDCOMDateParser{

	private static final Pattern DATE_PATTERN = Pattern.compile(
		"^(?:(?<qualifier>ABT|CAL|EST|BEF|AFT|BET|FROM|TO)\\s+)?" +
			"(?<from>.+?)(?:\\s+(?:AND|TO)\\s+(?<to>.+))?$",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern INTERPRETED_PATTERN = Pattern.compile("^INT\\s+(?<date>[^(]+?)\\s*(?:\\((?<phrase>.*)\\))?$",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern PHRASE_PATTERN = Pattern.compile("^\\((?<phrase>.*)\\)$");


	private GEDCOMDateParser(){
	}

	public static DateInfo parse(String dateStr){
		if(StringUtils.isEmpty(dateStr))
			return null;

		String trimmed = dateStr.trim();
		DateInfo.Builder builder = new DateInfo.Builder();

		// 0. Date phrase alone, "(phrase)", or interpreted date, "INT <date> (phrase)"
		final Matcher phraseMatcher = PHRASE_PATTERN.matcher(trimmed);
		if(phraseMatcher.matches())
			return builder.phrase(phraseMatcher.group("phrase").trim())
				.build();
		final Matcher interpretedMatcher = INTERPRETED_PATTERN.matcher(trimmed);
		if(interpretedMatcher.matches()){
			final String phrase = interpretedMatcher.group("phrase");
			builder.phrase(phrase != null? phrase.trim(): null);
			trimmed = interpretedMatcher.group("date").trim();
		}

		// 1. Check for leading qualifier (ABT, BEF, AFT, etc.)
		Matcher m = DATE_PATTERN.matcher(trimmed);
		if(m.matches()){
			String qualifier = m.group("qualifier");
			String fromPart = m.group("from");
			String toPart = m.group("to");

			if(qualifier != null){
				qualifier = qualifier.toUpperCase(Locale.ROOT);
				builder.qualifier(qualifier);
				switch(qualifier){
					case "ABT":
					case "CAL":
					case "EST":
						builder.approximate(true);
						// Treat as a point date with approximate qualifier
						builder.type(DateInfo.Type.POINT);
						builder.value(fromPart);
						break;
					case "BEF":
						builder.type(DateInfo.Type.BOUNDED);
						builder.notAfter(fromPart);
						break;
					case "AFT":
						builder.type(DateInfo.Type.BOUNDED);
						builder.notBefore(fromPart);
						break;
					case "BET":
						if(toPart != null){
							builder.type(DateInfo.Type.BOUNDED);
							builder.notBefore(fromPart);
							builder.notAfter(toPart);
						}
						else{
							// Fallback: treat as point
							builder.type(DateInfo.Type.POINT);
							builder.value(fromPart);
						}
						break;
					case "FROM":
						// FROM x [TO y]: the fact lasted over the period
						builder.type(DateInfo.Type.SPANNING);
						builder.from(fromPart);
						builder.to(toPart);
						break;
					case "TO":
						// TO y: the fact lasted until y
						builder.type(DateInfo.Type.SPANNING);
						builder.to(fromPart);
						break;
					default:
						builder.type(DateInfo.Type.POINT);
						builder.value(trimmed);
				}
			}
			else{
				// No qualifier: simple date
				builder.type(DateInfo.Type.POINT);
				builder.value(trimmed);
			}
		}
		else{
			// No qualifier match: treat as simple date
			builder.type(DateInfo.Type.POINT);
			builder.value(trimmed);
		}

		return builder.build();
	}

}

package io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Byzantine calendar date expressions.
 * Supports structural Greek names, Latin transliterations, and localized numerical scripts.
 */
public final class ByzantineCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Byzantine year starts in September
		// Latin / Ecclesiastical Transliterations
		MONTHS.put("september", 1);
		MONTHS.put("october", 2);
		MONTHS.put("november", 3);
		MONTHS.put("december", 4);
		MONTHS.put("january", 5);
		MONTHS.put("february", 6);
		MONTHS.put("march", 7);
		MONTHS.put("april", 8);
		MONTHS.put("may", 9);
		MONTHS.put("june", 10);
		MONTHS.put("july", 11);
		MONTHS.put("august", 12);

		// Greek Classical/Medieval Script names
		MONTHS.put("σεπτέμβριος", 1);
		MONTHS.put("οκτώβριος", 2);
		MONTHS.put("νοέμβριος", 3);
		MONTHS.put("δεκέμβριος", 4);
		MONTHS.put("ιανουάριος", 5);
		MONTHS.put("φεβρουάριος", 6);
		MONTHS.put("μάρτιος", 7);
		MONTHS.put("απρίλιος", 8);
		MONTHS.put("μάιος", 9);
		MONTHS.put("ιούνιος", 10);
		MONTHS.put("ιούλιος", 11);
		MONTHS.put("αύγουστος", 12);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private ByzantineCalendarParser(){}


	/**
	 * Parses a raw Byzantine text expression into a validated structural DTO.
	 *
	 * @param rawDate the date string (e.g., "1 Σεπτέμβριος 7000" or "15 September 6500")
	 * @return a configured ByzantineDateInput instance
	 */
	public static ByzantineDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Byzantine date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2).toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Byzantine month context: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		// Basic sanity boundary check (Julian structure mapping)
		if(day < 1 || day > 31)
			throw new IllegalArgumentException("Invalid day count for the targeted Julian boundary.");

		return new ByzantineDateInput(year, month, day);
	}

	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			// Handle basic eastern script anomalies if present
			if(c >= '۰' && c <= '精')
				sb.append((char)('0' + c - '۰'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}

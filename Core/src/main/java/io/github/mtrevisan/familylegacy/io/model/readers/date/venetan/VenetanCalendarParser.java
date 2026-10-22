package io.github.mtrevisan.familylegacy.io.model.readers.date.venetan;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for historical Venetian (more Veneto) calendar date expressions.
 * Supports historical Venetian month names, standard Italian items, and "MV" markers.
 */
public final class VenetanCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Venetian Month Names (Year starting in March)
		MONTHS.put("marzo", 1);
		MONTHS.put("marzo mv", 1);

		MONTHS.put("aprile", 2);
		MONTHS.put("maio", 3);
		MONTHS.put("maggio", 3);
		MONTHS.put("giugno", 4);
		MONTHS.put("lugio", 5);
		MONTHS.put("luglio", 5);
		MONTHS.put("agosto", 6);

		// Traditional numerical naming style often found in scripts
		MONTHS.put("settembre", 7);
		MONTHS.put("september", 7);
		MONTHS.put("ottobre", 8);
		MONTHS.put("novembre", 9);
		MONTHS.put("dicembre", 10);

		// Crucial shift months (implicitly or explicitly marked as MV)
		MONTHS.put("zenaro", 11);
		MONTHS.put("gennaio", 11);
		MONTHS.put("genaro", 11);
		MONTHS.put("zenaro mv", 11);
		MONTHS.put("gennaio mv", 11);

		MONTHS.put("febraro", 12);
		MONTHS.put("febbraio", 12);
		MONTHS.put("febraro mv", 12);
		MONTHS.put("febbraio mv", 12);
	}

	// Pattern mapping: "Day Month Year" with optional trailing "MV" or "More Veneto" markers
	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+([a-zA-Zæøç\\s-]+?)\\s+(\\d+)(?:\\s+(?:mv|more veneto))?", Pattern.CASE_INSENSITIVE);


	private VenetanCalendarParser(){}


	/**
	 * Parses a raw Venetian date string into a configured structural container.
	 *
	 * @param rawDate the date expression (e.g., "15 Febraro 1700" or "22 Gennaio 1530 MV")
	 * @return a configured VenetoDateInput instance
	 */
	public static VenetanDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate).toLowerCase(Locale.ROOT);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid Venetian (more Veneto) date format: " + rawDate);
		}

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2).trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null){
			throw new IllegalArgumentException("Unknown Venetian month descriptor: " + monthName);
		}

		// Julian framework boundary verification
		if(day < 1 || day > 31){
			throw new IllegalArgumentException("Day count violates standard Julian calendar monthly limits.");
		}

		return new VenetanDateInput(year, month, day);
	}

}

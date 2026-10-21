package io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for ancient Egyptian civil calendar date expressions.
 * Supports standard month names and explicit epagomenal day markers.
 */
public final class EgyptianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Season 1: Akhet (Inundation)
		MONTHS.put("thoth", 1);
		MONTHS.put("phaophi", 2);
		MONTHS.put("athyr", 3);
		MONTHS.put("choiak", 4);

		// Season 2: Peret (Growing/Sowing)
		MONTHS.put("tybi", 5);
		MONTHS.put("mechir", 6);
		MONTHS.put("phamenoth", 7);
		MONTHS.put("pharmouthi", 8);

		// Season 3: Shemu (Harvest)
		MONTHS.put("pachons", 9);
		MONTHS.put("payni", 10);
		MONTHS.put("epiphi", 11);
		MONTHS.put("mesore", 12);

		// Intercalary period (5 Epagomenal Days)
		MONTHS.put("epagomenai", 13);
		MONTHS.put("epagomenal", 13);
		MONTHS.put("meskut", 13);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private EgyptianCalendarParser(){}


	/**
	 * Parses a raw Egyptian text string into a validated data container.
	 *
	 * @param rawDate the date expression (e.g., "1 Thoth 1400" or "3 Meskut 1520")
	 * @return a structured EgyptianDateInput container
	 */
	public static EgyptianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Egyptian date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2).toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Egyptian month or epagomenal marker: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		// Boundary checks matching Egyptian civil constraints
		if(month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Egyptian months must have exactly 30 days.");
		if(month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Epagomenal period consists strictly of 5 days.");

		return new EgyptianDateInput(year, month, day);
	}

}

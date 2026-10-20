package io.github.mtrevisan.familylegacy.io.model.readers.date;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Persian (Solar Hijri) calendar dates.
 * <p>
 * Supported examples:
 * - ۲۲ بهمن ۱۳۵۷
 * - 22 Bahman 1357
 * - 1 Farvardin 1300
 * - ۱ فروردین ۱۳۰۰
 */
final class PersianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();

	static{
		// Persian names
		MONTHS.put("فروردین", 1);
		MONTHS.put("اردیبهشت", 2);
		MONTHS.put("خرداد", 3);
		MONTHS.put("تیر", 4);
		MONTHS.put("مرداد", 5);
		MONTHS.put("شهریور", 6);
		MONTHS.put("مهر", 7);
		MONTHS.put("آبان", 8);
		MONTHS.put("آذر", 9);
		MONTHS.put("دی", 10);
		MONTHS.put("بهمن", 11);
		MONTHS.put("اسفند", 12);

		// Transliterations
		MONTHS.put("farvardin", 1);
		MONTHS.put("ordibehesht", 2);
		MONTHS.put("khordad", 3);
		MONTHS.put("tir", 4);
		MONTHS.put("mordad", 5);
		MONTHS.put("shahrivar", 6);
		MONTHS.put("mehr", 7);
		MONTHS.put("aban", 8);
		MONTHS.put("azar", 9);
		MONTHS.put("dey", 10);
		MONTHS.put("bahman", 11);
		MONTHS.put("esfand", 12);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private PersianCalendarParser(){}


	public static PersianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Persian date: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));

		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Persian month: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		return new PersianDateInput(year, month, day);
	}

	/**
	 * Converts Persian and Arabic-Indic digits to ASCII digits.
	 */
	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);

			if(c >= '۰' && c <= '۹')
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				sb.append((char)('0' + c - '٠'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}

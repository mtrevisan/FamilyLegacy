package io.github.mtrevisan.calendars.islamic;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Islamic (Umm al-Qura) calendar date expressions using exact astronomical mapping flags.
 * Supports canonical Hijri months and traditional lunar-drift seasonal markers.
 */
public final class IslamicCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Hijri Months
		MONTHS.put("muharram", 1);
		MONTHS.put("محرم", 1);
		MONTHS.put("safar", 2);
		MONTHS.put("صفر", 2);
		MONTHS.put("rabi al-awwal", 3);
		MONTHS.put("ربيع الأول", 3);
		MONTHS.put("rabi al-thani", 4);
		MONTHS.put("ربيع الثاني", 4);
		MONTHS.put("jumada al-awwal", 5);
		MONTHS.put("جمادى الأولى", 5);
		MONTHS.put("jumada al-thani", 6);
		MONTHS.put("جمادى الآخرة", 6);
		MONTHS.put("rajab", 7);
		MONTHS.put("رجب", 7);
		MONTHS.put("shaban", 8);
		MONTHS.put("شعبان", 8);
		MONTHS.put("ramadan", 9);
		MONTHS.put("رمضان", 9);
		MONTHS.put("shawwal", 10);
		MONTHS.put("شوال", 10);
		MONTHS.put("dhu al-qadah", 11);
		MONTHS.put("ذو القعدة", 11);
		MONTHS.put("dhu al-hijjah", 12);
		MONTHS.put("ذو الحجة", 12);

		// TRADITIONAL ARABIC SEASONAL FLAGS
		// Al-Kharif (Autumn)
		MONTHS.put("al-kharif", CalendarType.SEASON_AUTUMN);
		MONTHS.put("kharif", CalendarType.SEASON_AUTUMN);
		MONTHS.put("الخريف", CalendarType.SEASON_AUTUMN);
		// Al-Shita (Winter)
		MONTHS.put("al-shita", CalendarType.SEASON_WINTER);
		MONTHS.put("shita", CalendarType.SEASON_WINTER);
		MONTHS.put("الشتاء", CalendarType.SEASON_WINTER);
		// Al-Rabi (Spring)
		MONTHS.put("al-rabi", CalendarType.SEASON_SPRING);
		MONTHS.put("rabi", CalendarType.SEASON_SPRING);
		MONTHS.put("الربيع", CalendarType.SEASON_SPRING);
		// Al-Sayf / Al-Qayz (Summer)
		MONTHS.put("al-sayf", CalendarType.SEASON_SUMMER);
		MONTHS.put("sayf", CalendarType.SEASON_SUMMER);
		MONTHS.put("الصيف", CalendarType.SEASON_SUMMER);
		MONTHS.put("qayz", CalendarType.SEASON_SUMMER);
		MONTHS.put("القيظ", CalendarType.SEASON_SUMMER);
	}

	// Adaptive pattern supporting both standalone seasonal notations ("Al-Sayf 1420")
	// and explicit three-token expressions ("1 Ramadan 1420")
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([a-zA-Z0-7أ-ي\\s-]+?)\\s+(\\d+)");


	private IslamicCalendarParser(){}


	/**
	 * Parses a raw Islamic date string text expression into a validated structural DTO.
	 *
	 * @param rawDate the string representing the Islamic date
	 * @return a configured IslamicDateInput instance
	 * @throws IllegalArgumentException if the expression violates text matching layout criteria
	 */
	public static IslamicDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new IslamicDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Islamic date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Islamic month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Islamic lunar months cannot exceed 30 days.");

		return new IslamicDateInput(year, month, day);
	}

	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			if(c >= '۰' && c <= '۹')
				// Persian
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				// Arabic-Indic
				sb.append((char)('0' + c - '٠'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}

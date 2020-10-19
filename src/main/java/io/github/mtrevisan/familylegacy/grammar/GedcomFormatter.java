/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mtrevisan.familylegacy.grammar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;


/**
 * This class contains some static methods which help with formatting data to be used in gedcom files.<br />
 * <br />
 * All the methods are synchronized for thread save access since many objects (like the date formats) are shared.
 */
class GedcomFormatter{

	public static final String INSET = "  ";

	public enum GedcomDateFormat{
		YEAR, YEAR_MONTH, YEAR_MONTH_DAY, UNKNOWN
	}

	private static final Pattern DATE_FORMAT_YEAR = Pattern.compile("[0-9]{4}");
	private static final Pattern DATE_FORMAT_YEAR_MONTH = Pattern.compile("[A-Z]{3} [0-9]{4}");
	private static final Pattern DATE_FORMAT_YEAR_MONTH_DAY = Pattern.compile("[0-3]?[0-9] [A-Z]{3} [0-9]{4}");

	private static final SimpleDateFormat DATE_YEAR_MONTH_DAY = new SimpleDateFormat("dd MMM yyyy");
	private static final SimpleDateFormat DATE_YEAR_MONTH = new SimpleDateFormat("MMM yyyy");
	private static final SimpleDateFormat DATE_YEAR = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat DATE_TIME_YEAR_MONTH_DAY = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
	private static final SimpleDateFormat DATE_TIME_YEAR_MONTH = new SimpleDateFormat("MMM yyyy HH:mm:ss");
	private static final SimpleDateFormat DATE_TIME_YEAR = new SimpleDateFormat("yyyy HH:mm:ss");
	private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

	private static final Calendar CALENDAR = new GregorianCalendar();

	static{
		DATE_YEAR_MONTH_DAY.setCalendar(CALENDAR);
		DATE_YEAR_MONTH.setCalendar(CALENDAR);
		DATE_YEAR.setCalendar(CALENDAR);
		DATE_TIME_YEAR_MONTH_DAY.setCalendar(CALENDAR);
		DATE_TIME_YEAR_MONTH.setCalendar(CALENDAR);
		DATE_TIME_YEAR.setCalendar(CALENDAR);
		TIME.setCalendar(CALENDAR);
	}


//	/**
//	 * Returns today date in the format needed for gedcom files.
//	 */
//	public synchronized static String getGedcomDateNow(){
//		CALENDAR.setTime(new Date());
//		return DATE_YEAR_MONTH_DAY.format(CALENDAR.getTime()).toUpperCase();
//	}

//	/**
//	 * Returns today time in the format needed for gedcom files.
//	 */
//	public synchronized static String getGedcomTimeNow(){
//		CALENDAR.setTime(new Date());
//		return TIME.format(CALENDAR.getTime()).toUpperCase();
//	}

//	/**
//	 * Returns the time part of the given date in the format needed for gedcom files.
//	 */
//	public synchronized static String getGedcomTime(Date d){
//		CALENDAR.setTime(d);
//		return TIME.format(CALENDAR.getTime());
//	}

//	/**
//	 * Returns the given date in the format needed for gedcom files. It has to
//	 * be specified if the day and the month should be included in the returned
//	 * date string.
//	 *
//	 * @param d
//	 * @param month Include the month?
//	 * @param day Include the day (only possible if the month is included)?
//	 * @return The date as string, or <code>null</code> if <code>d=null</code>
//	 */
//	public synchronized static String getGedcomDate(Date d, boolean month, boolean day){
//		if(d == null)
//			return null;
//
//		CALENDAR.setTime(d);
//
//		if(month){
//			if(!day)
//				//Year and month. With uppercase month
//				return DATE_YEAR_MONTH.format(CALENDAR.getTime()).toUpperCase();
//			else
//				//Year, month and day. With uppercase month
//				return DATE_YEAR_MONTH_DAY.format(CALENDAR.getTime()).toUpperCase();
//		}
//		else
//			//Year only
//			return DATE_YEAR.format(CALENDAR.getTime());
//	}

//	/**
//	 * Returns the given date and time in the format needed for gedcom files. It has to
//	 * be specified if the day and the month should be included in the returned
//	 * date-time string.
//	 *
//	 * @param d
//	 * @param month Include the month?
//	 * @param day Include the day (only possible if the month is included)?
//	 * @return The date and time as string, or <code>null</code> if <code>d=null</code>
//	 */
//	public synchronized static String getGedcomDateTime(Date d, boolean month, boolean day){
//		if(d == null)
//			return null;
//
//		CALENDAR.setTime(d);
//
//		if(month){
//			if(!day)
//				//Year and month. With uppercase month
//				return DATE_TIME_YEAR_MONTH.format(CALENDAR.getTime()).toUpperCase();
//			else
//				//Year, month and day. With uppercase month
//				return DATE_TIME_YEAR_MONTH_DAY.format(CALENDAR.getTime()).toUpperCase();
//		}
//		else
//			//Year only
//			return DATE_TIME_YEAR.format(CALENDAR.getTime());
//	}


//	/**
//	 * Returns the Date object from the given gedcom dateString. Gedcom dates
//	 * can be in the following three patterns:<br>
//	 * 1. "dd MMM yyyy"<br>
//	 * 2. "MMM yyyy"<br>
//	 * 3. "yyyy"
//	 * <br>
//	 * Note: The missing pieces are set to 01 (first day and first month). Thus, the
//	 * input date string "FEB 1995" would produce a date object "01 02 1995". This
//	 * is just how {@link Date} works.
//	 *
//	 * @param gedcomDateString
//	 * @return
//	 */
//	public synchronized static Date getDateFromGedcom(String gedcomDateString){
//		if(gedcomDateString == null || gedcomDateString.length() == 0)
//			return null;
//
//		GedcomDateFormat format = getGedcomDateFormat(gedcomDateString);
//
//		try{
//			switch(format){
//				case YEAR_MONTH_DAY:
//					return DATE_YEAR_MONTH_DAY.parse(gedcomDateString);
//				case YEAR_MONTH:
//					return DATE_YEAR_MONTH.parse(gedcomDateString);
//				case YEAR:
//					return DATE_YEAR.parse(gedcomDateString);
//				default:
//					break;
//			}
//		}
//		catch(ParseException e){
//		}
//
//		return null;
//	}

//	/**
//	 * Converts the given gedcom time (HH:mm:ss) string to a date object.
//	 *
//	 * @param gedcomTimeString
//	 * @return
//	 */
//	public synchronized static Date getTimeFromGedcom(String gedcomTimeString){
//		if(gedcomTimeString == null || gedcomTimeString.length() == 0)
//			return null;
//
//		try{
//			return TIME.parse(gedcomTimeString);
//		}
//		catch(ParseException e){
//			return null;
//		}
//	}

//	/**
//	 * Returns the Date object from the given dateTimeString. It is expected
//	 * that the given string contains the date and the time part. Gedcom dates
//	 * can be in the following three patterns:<br>
//	 * 1. "dd MMM yyyy"<br>
//	 * 2. "MMM yyyy"<br>
//	 * 3. "yyyy"
//	 * <br>
//	 * The time part is parsed with the format "HH:mm:ss"<br>
//	 * <br>
//	 * Note: The missing pieces are set to 01 (first day and first month). Thus, the
//	 * input date string "1995 12:02:20" would produce a date object "01 01 1995 12:02:20". This
//	 * is just how {@link Date} works.
//	 *
//	 * @param gedcomDateTimeString
//	 * @return
//	 */
//	public synchronized static Date getDateTimeFromGedcom(String gedcomDateTimeString){
//		if(gedcomDateTimeString == null || gedcomDateTimeString.length() == 0)
//			return null;
//
//		//Only analyze the date part
//		GedcomDateFormat format = getGedcomDateFormat(gedcomDateTimeString.substring(0, gedcomDateTimeString.lastIndexOf(" ")));
//
//		try{
//			switch(format){
//				case YEAR_MONTH_DAY:
//					return DATE_TIME_YEAR_MONTH_DAY.parse(gedcomDateTimeString);
//				case YEAR_MONTH:
//					return DATE_TIME_YEAR_MONTH.parse(gedcomDateTimeString);
//				case YEAR:
//					return DATE_TIME_YEAR.parse(gedcomDateTimeString);
//				default:
//					break;
//			}
//		}
//		catch(ParseException e){
//		}
//
//		return null;
//	}

//	/**
//	 * Checks if the given date string is in a valid gedcom date format
//	 *
//	 * @param gedcomDate
//	 * @return
//	 */
//	public synchronized static boolean isGedcomDate(String gedcomDate){
//		return getGedcomDateFormat(gedcomDate) != GedcomDateFormat.UNKNOWN;
//	}

//	/**
//	 * Analyzes the format of the given date string and returns if the date contains only
//	 * the year, the year and month or year, month and day in gedcom format.
//	 */
//	public synchronized static GedcomDateFormat getGedcomDateFormat(String gedcomDate){
//		//The gedcom date could either be in the format "dd MMM yyyy", "MMM yyyy", or "yyyy"
//
//		if(RegexHelper.matches(gedcomDate, DATE_FORMAT_YEAR_MONTH_DAY))
//			return GedcomDateFormat.YEAR_MONTH_DAY;
//		else if(RegexHelper.matches(gedcomDate, DATE_FORMAT_YEAR_MONTH))
//			return GedcomDateFormat.YEAR_MONTH;
//		else if(RegexHelper.matches(gedcomDate, DATE_FORMAT_YEAR))
//			return GedcomDateFormat.YEAR;
//		return GedcomDateFormat.UNKNOWN;
//	}

//	/**
//	 * Converts the given gedcom date into a date string with one of the given formats. Since the
//	 * gedcom date format can occur in three ways (with "day month year", "month year"
//	 * or "year"), three possibilities of output formats have to be given.
//	 */
//	public synchronized static String convertGedcomDate(String gedcomDate, String dateFormatYear, String dateFormatYearMonth, String dateFormatYearMonthDay){
//		GedcomDateFormat format = getGedcomDateFormat(gedcomDate);
//		try{
//			switch(format){
//				case YEAR_MONTH_DAY:
//					Date date1 = DATE_YEAR_MONTH_DAY.parse(gedcomDate);
//					SimpleDateFormat df1 = new SimpleDateFormat(dateFormatYearMonthDay);
//					return df1.format(date1);
//				case YEAR_MONTH:
//					Date date2 = DATE_YEAR_MONTH.parse(gedcomDate);
//					SimpleDateFormat df2 = new SimpleDateFormat(dateFormatYearMonth);
//					return df2.format(date2);
//				case YEAR:
//					Date date3 = DATE_YEAR.parse(gedcomDate);
//					SimpleDateFormat df3 = new SimpleDateFormat(dateFormatYear);
//					return df3.format(date3);
//				default:
//					break;
//			}
//		}
//		catch(ParseException e){
//		}
//
//		return null;
//	}

//	/**
//	 * Tries to parse the given date string with the given dateFormatPatterns. The
//	 * order of trying is exactly the order of the patterns given with dateFormatPatterns.
//	 * The first pattern which works to parse the date string returns its {@link Date} object.
//	 * If none of the patterns work, null is returned. The date parsing starts
//	 * at the beginning of the given date string and parses until it fails. It might
//	 * not use the whole date string, thus, longer format patterns should appear
//	 * first.
//	 *
//	 * @param dateString
//	 * @param dateFormatPatterns One or more date formats as used in {@link SimpleDateFormat}
//	 * @return
//	 */
//	public synchronized static Date extractDate(String dateString, String... dateFormatPatterns){
//		if(dateString == null || dateString.length() == 0)
//			return null;
//
//		SimpleDateFormat dateFormat;
//		for(int i = 0; i < dateFormatPatterns.length; i++){
//			dateFormat = new SimpleDateFormat(dateFormatPatterns[i]);
//
//			try{
//				Date date = dateFormat.parse(dateString);
//
//				//If parsing the date did not fail, return the date
//				return date;
//			}
//			catch(ParseException e){
//				continue;
//			}
//		}
//
//		//Parsing the date with any given pattern failed
//		return null;
//	}

//	/**
//	 * Prints the given value (with prefix and postfix if given), but only if the
//	 * given value is not null. If the given value is null, an empty string is returned.
//	 */
//	public synchronized static String printIfNotNull(String prefix, String valueToPrintIfNotNull, String postfix){
//		if(valueToPrintIfNotNull == null)
//			return "";
//		return prefix + valueToPrintIfNotNull + postfix;
//	}


//	/**
//	 * Creates a string with the number of spaces defined with the parameter insets
//	 */
//	public synchronized static StringBuffer makeInset(int inset){
//		StringBuffer sb = new StringBuffer();
//		for(int i = 0; i < inset; i ++)
//			sb.append(INSET);
//		return sb;
//	}

//	/**
//	 * Returns the required number of spaces to align text further to the right
//	 *
//	 * @param spaceFromLeft How many spaces from the left page border are needed?
//	 * @param preStringLength The current length of the string on this line
//	 * @return
//	 */
//	public synchronized static StringBuffer makeRightAlign(int spaceFromLeft, int preStringLength){
//		StringBuffer sb = new StringBuffer();
//
//		int spaceNeeded = spaceFromLeft - preStringLength;
//		if(spaceNeeded <= 0)
//			spaceNeeded = 3;
//		for(int i = spaceNeeded; i > 0; i--){
//			if(i % 2 == 0)
//				sb.append("°");
//			else
//				sb.append(" ");
//		}
//
//		return sb;
//	}


	/**
	 * Makes a or-list out of the given list, adding the given pre- and suffixes
	 * to each or-item.<br>
	 * A generated list could look like:<br>
	 * [ITEM1|ITEM2|ITEM3]<br>
	 * or with prefix &lt; and suffix &gt;:<br>
	 * [&lt;ITEM1&gt;|&lt;ITEM2&gt;|&lt;ITEM3&gt;]
	 */
	public synchronized static StringBuilder makeOrList(Collection<String> list, String itemPrefix, String itemSuffix){
		return makeStringList(list, "|", itemPrefix, itemSuffix, true, null, true);
	}

	public synchronized static StringBuilder makeStringList(Collection<String> list, String separator, String itemPrefix, String itemSuffix, boolean includeNullAndEmpty, String replaceNullWith, boolean addBrackets){
		StringBuilder sb = new StringBuilder();


		for(String item : list){
			if(item == null)
				item = replaceNullWith;

			if(!includeNullAndEmpty && (item == null || item.length() == 0))
				continue;

			if(sb.length() > 0 && separator != null)
				sb.append(separator);

			if(item == null || item.equals("NULL"))
				sb.append("<");
			else if(itemPrefix != null)
				sb.append(itemPrefix);

			sb.append(item);

			if(item == null || item.equals("NULL"))
				sb.append(">");
			else if(itemSuffix != null)
				sb.append(itemSuffix);
		}

		if(addBrackets && list.size() > 1)
			sb.insert(0, "[");

		if(addBrackets && list.size() > 1)
			sb.append("]");

		return sb;
	}

}

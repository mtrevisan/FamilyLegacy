package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Matcher;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


class Age{

	private static final Matcher AGE_MATCHER = RegexHelper.matcher("[^\\d]+");


	public static enum ApproximationType{
		PREFIX("~"),
		SUFFIX(" ca.");

		private final String approx;

	};


	private final int value;
	private final boolean approximated;
	private final ApproximationType approximationType;


	public Age(String value, ApproximationType approximationType){
		value = StringUtils.strip(value);

		String age = RegexHelper.replaceAll(value, AGE_MATCHER, StringUtils.EMPTY);
		this.value = Integer.valueOf(age);
		approximated = (value.length() != age.length());
		this.approximationType = approximationType;
	}

	public static Age createFromExactAge(int value){
		return new Age(value, false, null);
	}

	public static Age createFromApproximatedAge(int value, ApproximationType approximationType){
		return new Age(value, true, approximationType);
	}

	public static Age createFromDates(String dateFrom, String dateTo, ApproximationType approximationType){
		Age age = null;
		if(dateFrom != null && dateTo != null){
			LocalDate from = DateParser.parse(dateFrom);
			LocalDate to = DateParser.parse(dateTo);
			if(from != null && to != null){
				boolean approximatedAge = (AbstractCalendarParser.isApproximation(dateFrom) || AbstractCalendarParser.isApproximation(dateTo));
				age = new Age(Period.between(from, to).getYears(), approximatedAge, approximationType);
			}
		}
		return age;
	}

	public static Age createFromDates(LocalDate from, LocalDate to){
		return createFromPeriod(Period.between(from, to));
	}

	public static Age createFromPeriod(Period period){
		return createFromExactAge(period.getYears());
	}

	@Override
	public String toString(){
		String text = Integer.toString(value);
		if(!approximated)
			return text;

		return (approximationType == ApproximationType.PREFIX? approximationType.approx + text: text + approximationType.approx);
	}

}

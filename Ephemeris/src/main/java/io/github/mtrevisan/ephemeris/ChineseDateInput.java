package io.github.mtrevisan.ephemeris;

/**
 * A lightweight data record holding raw integers parsed from traditional Chinese calendar strings.
 */
public final class ChineseDateInput{
	private final int year;
	private final int month;
	private final boolean leapMonth;
	private final int day;

	public ChineseDateInput(final int year, final int month, final boolean leapMonth, final int day){
		this.year = year;
		this.month = month;
		this.leapMonth = leapMonth;
		this.day = day;
	}

	public int getYear(){
		return year;
	}

	public int getMonth(){
		return month;
	}

	public boolean isLeapMonth(){
		return leapMonth;
	}

	public int getDay(){
		return day;
	}

	@Override
	public String toString(){
		return "Year: " + year + ", Month: " + month + " (Leap: " + leapMonth + "), Day: " + day;
	}

}


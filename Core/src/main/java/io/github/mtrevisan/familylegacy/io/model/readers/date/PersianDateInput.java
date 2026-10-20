package io.github.mtrevisan.familylegacy.io.model.readers.date;


final class PersianDateInput{

	private final int year;
	private final int month;
	private final int day;


	public PersianDateInput(final int year, final int month, final int day){
		this.year = year;
		this.month = month;
		this.day = day;
	}


	public int getYear(){
		return year;
	}

	public int getMonth(){
		return month;
	}

	public int getDay(){
		return day;
	}

	@Override
	public String toString(){
		return "Year: " + year
			+ ", Month: " + month
			+ ", Day: " + day;
	}

}

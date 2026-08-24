package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

public class DateNormalizer{
	private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

	public static String normalize(String gedcomDate){
		if(gedcomDate == null) return null;
		String cleaned = gedcomDate.replaceAll("(?i)ABT|CAL|EST|BEF|AFT", "").trim();
		for(int i = 0; i < MONTHS.length; i++){
			if(cleaned.contains(MONTHS[i])){
				String[] parts = cleaned.split(" ");
				if(parts.length == 3){
					try{
						int day = Integer.parseInt(parts[0]);
						int month = i + 1;
						int year = Integer.parseInt(parts[2]);
						return String.format("%04d-%02d-%02d", year, month, day);
					}
					catch(NumberFormatException e){
					}
				}
			}
		}
		if(cleaned.matches("\\d{4}")) return cleaned;
		return null;
	}

}

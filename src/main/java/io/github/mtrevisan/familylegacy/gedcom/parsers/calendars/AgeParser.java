package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * A class for parsing ages from strings.
 */
class AgeParser{

	private static final Pattern PATTERN_AGE = RegexHelper.pattern("(?i)^(((?<relation><|>) )?((?<years>\\d{1,2})y)? ?((?<months>\\d{1,2})m)? ?((?<days>\\d{1,3})d)?|(?<instant>CHILD|INFANT|STILLBORN))$");


	public enum AgeType{
		EXACT("Exact", StringUtils.EMPTY, null),
		LESS_THAN("Less than", "<", RegexHelper.pattern("<")),
		MORE_THAN("More than", ">", RegexHelper.pattern(">")),

		//less than 8 years
		CHILD("Child", "CHILD", RegexHelper.pattern("(?i)CHILD")),
		//less than 1 year
		INFANT("Infant", "INFANT", RegexHelper.pattern("(?i)INFANT")),
		STILLBORN("Stillborn", "STILLBORN", RegexHelper.pattern("(?i)STILLBORN"));


		private final String description;
		private final String type;
		private final Pattern pattern;


		public static AgeType createFromIndex(int index){
			return values()[index];
		}

		public static AgeType createFromText(String instant){
			if(instant != null)
				for(AgeType type : values())
					if(type.pattern != null && RegexHelper.find(instant, type.pattern))
						return type;
			return EXACT;
		}

		AgeType(final String description, final String type, final Pattern pattern){
			this.description = description;
			this.type = type;
			this.pattern = pattern;
		}

		public String getDescription(){
			return description;
		}

		public static String[] getDescriptions(){
			final List<String> list = new ArrayList<>();
			for(final AgeType ageType : values())
				list.add(ageType.description);
			return list.toArray(new String[0]);
		}
	}


	/**
	 * Parse the string as age.
	 *
	 * @param age	the age string
	 * @return	the age, if it can be derived from the string
	 */
	public static AgeData parse(String age){
		AgeData ad = null;
		PATTERN_AGE.reset(age);
		if(PATTERN_AGE.find()){
			String instant = PATTERN_AGE.group("instant");
			if(StringUtils.isNotEmpty(instant))
				ad = AgeData.builder()
					.ageType(AgeType.createFromText(instant))
					.build();
			else{
				String relation = PATTERN_AGE.group("relation");
				String years = PATTERN_AGE.group("years");
				String months = PATTERN_AGE.group("months");
				String days = PATTERN_AGE.group("days");
				ad = new AgeData()
					.withAgeType(AgeType.createFromText(relation))
					.withYears(years)
					.withMonths(months)
					.withDays(days);
			}
		}
		return ad;
	}

	public static String formatAge(String age){
		String formattedAge = null;
		if(StringUtils.isNotBlank(age)){
			RegexHelper.replaceAll(age, AgeType.CHILD.pattern, AgeType.CHILD.description);
			RegexHelper.replaceAll(age, AgeType.INFANT.pattern, AgeType.INFANT.description);
			RegexHelper.replaceAll(age, AgeType.STILLBORN.pattern, AgeType.STILLBORN.description);
			formattedAge = age;
		}
		return formattedAge;
	}

}

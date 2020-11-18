package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.Arrays;
import java.util.regex.Matcher;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * A class for parsing ages from strings.
 */
class AgeParser{

	private static final Matcher MATCHER_AGE = RegexHelper.matcher("(?i)^(((?<relation><|>) )?((?<years>\\d{1,2})y)? ?((?<months>\\d{1,2})m)? ?((?<days>\\d{1,3})d)?|(?<instant>CHILD|INFANT|STILLBORN))$");


	public static enum AgeType{
		EXACT("Exact", StringUtils.EMPTY, null),
		LESS_THAN("Less than", "<", RegexHelper.matcher("<")),
		MORE_THAN("More than", ">", RegexHelper.matcher(">")),

		//less than 8 years
		CHILD("Child", "CHILD", RegexHelper.matcher("(?i)CHILD")),
		//less than 1 year
		INFANT("Infant", "INFANT", RegexHelper.matcher("(?i)INFANT")),
		STILLBORN("Stillborn", "STILLBORN", RegexHelper.matcher("(?i)STILLBORN"));


		private final String description;
		private final String type;
		private final Matcher matcher;


		public static AgeType createFromIndex(int index){
			return values()[index];
		}

		public static AgeType createFromText(String instant){
			if(instant != null)
				for(AgeType type : values())
					if(type.matcher != null && RegexHelper.find(instant, type.matcher))
						return type;
			return AgeType.EXACT;
		}

		public static String[] getDescriptions(){
			return Arrays.stream(values())
				.map(AgeType::getDescription)
				.toArray(String[]::new);
		}
	};


	/**
	 * Parse the string as age.
	 *
	 * @param age	the age string
	 * @return	the age, if it can be derived from the string
	 */
	public static AgeData parse(String age){
		AgeData ad = null;
		MATCHER_AGE.reset(age);
		if(MATCHER_AGE.find()){
			String instant = MATCHER_AGE.group("instant");
			if(StringUtils.isNotEmpty(instant))
				ad = AgeData.builder()
					.ageType(AgeType.createFromText(instant))
					.build();
			else{
				String relation = MATCHER_AGE.group("relation");
				String years = MATCHER_AGE.group("years");
				String months = MATCHER_AGE.group("months");
				String days = MATCHER_AGE.group("days");
				ad = AgeData.builder()
					.ageType(AgeType.createFromText(relation))
					.years(years)
					.months(months)
					.days(days)
					.build();
			}
		}
		return ad;
	}

	public static String formatAge(String age){
		String formattedAge = null;
		if(StringUtils.isNotBlank(age)){
			RegexHelper.replaceAll(age, AgeType.CHILD.matcher, AgeType.CHILD.description);
			RegexHelper.replaceAll(age, AgeType.INFANT.matcher, AgeType.INFANT.description);
			RegexHelper.replaceAll(age, AgeType.STILLBORN.matcher, AgeType.STILLBORN.description);
			formattedAge = age;
		}
		return formattedAge;
	}

}

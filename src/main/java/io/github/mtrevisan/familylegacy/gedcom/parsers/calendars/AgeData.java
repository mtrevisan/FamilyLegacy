package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.StringJoiner;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;


class AgeData{

	private final AgeParser.AgeType ageType;
	private final String years;
	private final String months;
	private final String days;


	public String getAge(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(ageType.getDescription());
		if(ageType == AgeParser.AgeType.EXACT || ageType == AgeParser.AgeType.LESS_THAN || ageType == AgeParser.AgeType.MORE_THAN){
			if(StringUtils.isNotBlank(years))
				sj.add(years)
					.add("y");
			if(StringUtils.isNotBlank(months))
				sj.add(months)
					.add("m");
			if(StringUtils.isNotBlank(days))
				sj.add(days)
					.add("d");
		}
		return sj.toString();
	}

}

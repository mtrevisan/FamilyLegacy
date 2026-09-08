package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum CalendarType{
	GREGORIAN("gregorian"),
	JULIAN("julian"),
	ISLAMIC("islamic"),
	HEBREW("hebrew"),
	CHINESE("chinese"),
	INDIAN("indian"),
	BUDDHIST("buddhist"),
	FRENCH_REPUBLICAN("french_republican"),
	COPTIC("coptic"),
	SOVIET_ETERNAL("soviet_eternal"),
	ETHIOPIAN("ethiopian"),
	MAYAN("mayan");


	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarType.class);


	private final String code;


	CalendarType(final String code){
		this.code = code;
	}


	public String getCode(){
		return code;
	}

	public static CalendarType fromCode(final String code){
		for(final CalendarType type : values())
			if(type.code.equalsIgnoreCase(code))
				return type;

//		throw new IllegalArgumentException("Unsupported calendar: " + code);
//		LOGGER.warn("Unsupported calendar: {}, default to 'gregorian'", code);
		return GREGORIAN;
	}

}

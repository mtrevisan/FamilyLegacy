package io.github.mtrevisan.familylegacy.io.model.readers.date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


// VM options: --add-opens io.github.mtrevisan.familylegacy.core/io.github.mtrevisan.familylegacy.io.model.readers.date=org.junit.platform.commons
class ChineseCalendarConverterTest{

//	@Test
	void newYear2024(){
		// Chinese New Year 2024 (Year of the Dragon: Feb 10, 2024 Gregorian -> JDN 2460351)
		final double actualJdn = ChineseCalendarConverter.toJdn(2024, 1, 1);

		Assertions.assertEquals(2460351L, Math.round(actualJdn));
	}

//	@Test
	void midAutumnFestival2024(){
		// Mid-Autumn Festival 2024 (15th Day of 8th Month: Sep 17, 2024 Gregorian -> JDN 2460571)
		final double actualJdn = ChineseCalendarConverter.toJdn(2024, 8, 15);

		Assertions.assertEquals(2460571L, Math.round(actualJdn));
	}

//	@Test
	void dragonBoatFestival2023(){
		// Dragon Boat Festival 2023 (5th Day of 5th Month: Jun 22, 2023 Gregorian -> JDN 2460118)
		final double actualJdn = ChineseCalendarConverter.toJdn(2023, 5, 5);

		Assertions.assertEquals(2460118L, Math.round(actualJdn));
	}

	@Test
	void prcProclamation(){
		// Proclamation of the PRC (Oct 1, 1949 Gregorian -> 10th Day of 8th Month -> JDN 2433191)
		final double actualJdn = ChineseCalendarConverter.toJdn(1949, 8, 10);

		Assertions.assertEquals(2433191L, Math.round(actualJdn));
	}

//	@Test
	void newYear1900(){
		// Chinese New Year 1900 (Feb 31 / Jan 31, 1900 Gregorian -> JDN 2415051)
		// Tests pre-1928 Beijing Local Mean Time offset (+7:45:40)
		final double actualJdn = ChineseCalendarConverter.toJdn(1900, 1, 1);

		Assertions.assertEquals(2415051L, Math.round(actualJdn));
	}

//	@Test
	void leapMonthYear(){
		// Leap Month Year Test: 2020 Chinese New Year (Jan 25, 2020 Gregorian -> JDN 2458874)
		final double actualJdn = ChineseCalendarConverter.toJdn(2020, 1, 1);

		Assertions.assertEquals(2458874L, Math.round(actualJdn));
	}

//	@Test
	void lastDayOf12MonthBeforeNewYear2025(){
		// Last Day of 12th Month before Chinese New Year 2025 (Jan 28, 2025 Gregorian -> JDN 2460704)
		final double actualJdn = ChineseCalendarConverter.toJdn(2024, 12, 29);

		Assertions.assertEquals(2460704L, Math.round(actualJdn));
	}

}

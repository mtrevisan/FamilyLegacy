package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DossierFormattingTest{

	private static long jdn(final int year){
		return java.time.LocalDate.of(year, 6, 15).toEpochDay() + 2_440_588l;
	}

	private static NormalizedDate year(final int year){
		return NormalizedDate.exact(jdn(year), DatePrecision.YEAR);
	}

	private static NormalizedDate aboutYear(final int year){
		return NormalizedDate.approximated(jdn(year), DatePrecision.YEAR, "gregorian", "stated", null);
	}


	@Test
	void anExactDateHasNoQualifier(){
		Assertions.assertEquals("1850", DossierFormatting.formatSpan(TemporalSpan.point(year(1850))));
	}

	@Test
	void anApproximateDateIsAbout(){
		Assertions.assertEquals("about 1850", DossierFormatting.formatSpan(TemporalSpan.point(aboutYear(1850))));
	}

	@Test
	void aCalculatedDateIsTold(){
		final NormalizedDate calculated = NormalizedDate.approximated(jdn(1850), DatePrecision.YEAR,
			"gregorian", "calculated", null);
		Assertions.assertEquals("calculated 1850", DossierFormatting.formatSpan(TemporalSpan.point(calculated)));
	}

	@Test
	void aBoundedDateIsBeforeAfterOrBetween(){
		Assertions.assertEquals("before 1850", DossierFormatting.formatSpan(TemporalSpan.bounded(null, year(1850))));
		Assertions.assertEquals("after 1850", DossierFormatting.formatSpan(TemporalSpan.bounded(year(1850), null)));
		Assertions.assertEquals("between 1850 and 1860", DossierFormatting.formatSpan(TemporalSpan.bounded(year(1850), year(1860))));
	}

	@Test
	void aSpanningDateIsFromTo(){
		Assertions.assertEquals("from 1850 to 1860", DossierFormatting.formatSpan(TemporalSpan.spanning(year(1850), year(1860))));
	}

}

/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.places;

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * The reform of a place is the one of the historical jurisdiction that contains it.
 */
class PlaceCalendarReformTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		//Innsbruck is in Tyrol (Austria); Salzburg's town is in Salzburg (Austria); Wien is in Austria only
		model.addRecord(place("P1", "Innsbruck"));
		model.addRecord(place("P2", "Tirol", "Tyrol"));
		model.addRecord(place("P3", "Austria"));
		model.addRecord(place("P4", "Wien"));
		model.addRecord(place("P5", "Basel"));
		model.addRecord(place("P6", "Switzerland"));
		model.addRecord(place("P7", "Basel"));
		model.addRecord(place("P8", "Paris"));
		model.addRecord(place("P9", "France"));
		model.addRecord(relationship("PR1", "P1", "P2"));
		model.addRecord(relationship("PR2", "P2", "P3"));
		model.addRecord(relationship("PR3", "P4", "P3"));
		model.addRecord(relationship("PR4", "P5", "P6"));
		model.addRecord(relationship("PR5", "P8", "P9"));
	}


	@Test
	void townTakesTheReformOfItsLand(){
		final PlaceNameResolver resolver = new PlaceNameResolver(model);

		//found through an alternative name of the container
		Assertions.assertEquals(CalendarReform.ROME_1583, resolver.calendarReform("P1").orElseThrow());
		Assertions.assertEquals(CalendarReform.ROME_1583, resolver.calendarReform("P2").orElseThrow());
	}

	@Test
	void countryWithSeveralReformsAsksTheUser(){
		final PlaceNameResolver resolver = new PlaceNameResolver(model);

		Assertions.assertTrue(resolver.calendarReform("P3").isEmpty());
		Assertions.assertTrue(resolver.calendarReform("P4").isEmpty());
	}

	@Test
	void countryWithOneReformIsTheFallback(){
		Assertions.assertEquals(CalendarReform.FRANCE, new PlaceNameResolver(model).calendarReform("P8").orElseThrow());
	}

	@Test
	void jurisdictionNeedsItsCountryInTheChain(){
		final PlaceNameResolver resolver = new PlaceNameResolver(model);

		//a "Basel" within Switzerland is the canton, a "Basel" with no container is not guessed
		Assertions.assertTrue(resolver.calendarReform("P5").isPresent());
		Assertions.assertTrue(resolver.calendarReform("P7").isEmpty());
	}

	@Test
	void nameLevelsGoOutwards(){
		final List<List<String>> levels = new PlaceNameResolver(model).nameLevels("P1");

		Assertions.assertEquals(List.of(List.of("Innsbruck"), List.of("Tirol", "Tyrol"), List.of("Austria")), levels);
	}


	private static FLEFRecord place(final String id, final String... names){
		final FLEFRecord record = FLEFRecord.createMainRecord(id, PlaceReader.TYPE);
		for(final String name : names)
			record.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
		return record;
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, target)));
	}

}

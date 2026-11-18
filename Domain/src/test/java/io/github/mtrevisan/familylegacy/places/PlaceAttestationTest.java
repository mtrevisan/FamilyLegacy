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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** The interval in which a place surely existed, from the dates of the sources that talk about it. */
class PlaceAttestationTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		//Canizzano is part of Treviso, that is part of Italia; Elsewhere is alone
		for(final String id : new String[]{"P1", "P2", "P3", "P4"})
			model.addRecord(FLEFRecord.createMainRecord(id, PlaceReader.TYPE));
		model.addRecord(relationship("PR1", "P1", "P2", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
		model.addRecord(relationship("PR2", "P2", "P3", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
	}


	@Test
	void thePlaceAndTheContainingPlacesExistedAtTheDateOfTheSource(){
		model.addRecord(source("S1", "P1", "1750-03-12"));

		for(final String id : new String[]{"P1", "P2", "P3"}){
			final PlaceAttestation.Interval interval = new PlaceAttestation(model, r -> true).of(id);
			Assertions.assertNotNull(interval, id);
			Assertions.assertTrue(interval.sameDate(), id);
			Assertions.assertEquals("1750-03-12", fullDate(interval, PlaceAttestation.Interval.TAG_FROM), id);
		}
		Assertions.assertNull(new PlaceAttestation(model, r -> true).of("P4"));
	}

	@Test
	void theIntervalSpansTheEarliestAndTheLatestSource(){
		model.addRecord(source("S1", "P1", "1750-03-12"));
		model.addRecord(source("S2", "P1", "1900-01-01"));
		model.addRecord(source("S3", "P2", "1600-06-01"));

		final PlaceAttestation attestation = new PlaceAttestation(model, r -> true);
		final PlaceAttestation.Interval town = attestation.of("P1");
		Assertions.assertFalse(town.sameDate());
		Assertions.assertEquals("1750-03-12", fullDate(town, PlaceAttestation.Interval.TAG_FROM));
		Assertions.assertEquals("1900-01-01", fullDate(town, PlaceAttestation.Interval.TAG_TO));
		//the province is older through its own source, and as recent as its town
		final PlaceAttestation.Interval province = attestation.of("P2");
		Assertions.assertEquals("1600-06-01", fullDate(province, PlaceAttestation.Interval.TAG_FROM));
		Assertions.assertEquals("1900-01-01", fullDate(province, PlaceAttestation.Interval.TAG_TO));
	}

	@Test
	void aSourceWithoutADateGivesNothing(){
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceCitationReader.TAG_PLACE, "P1"))));

		Assertions.assertNull(new PlaceAttestation(model, r -> true).of("P1"));
	}

	@Test
	void aSourceThatIsNotVisibleIsNotCounted(){
		model.addRecord(source("S1", "P1", "1750-03-12"));

		Assertions.assertNull(new PlaceAttestation(model, r -> false).of("P1"));
	}

	@Test
	void aSuccessionDoesNotCarryTheDateToThePredecessor(){
		model.addRecord(relationship("PR3", "P1", "P4", PlaceRelationshipReader.ENUM_TYPE_SUCCESSOR_OF));
		model.addRecord(source("S1", "P1", "1750-03-12"));

		Assertions.assertNull(new PlaceAttestation(model, r -> true).of("P4"));
	}


	@Test
	void anEventAtAPlaceCitedByADatedSourceGivesTheDateToThePlace(){
		//the source has a date but no place: the place comes from the event
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(source("S9", "P4", "1820-05-01").getChildren().getFirst()));
		model.addRecord(event("E1", "P1", "S1", false));

		final PlaceAttestation.Interval interval = new PlaceAttestation(model, r -> true).of("P1");
		Assertions.assertNotNull(interval);
		Assertions.assertEquals("1820-05-01", fullDate(interval, PlaceAttestation.Interval.TAG_FROM));
		//and the places containing it
		Assertions.assertNotNull(new PlaceAttestation(model, r -> true).of("P3"));
		Assertions.assertNull(new PlaceAttestation(model, r -> true).of("P4"));
	}

	@Test
	void theSourceCitedByThePlaceOfAnEventCountsToo(){
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(source("S9", "P4", "1820-05-01").getChildren().getFirst()));
		model.addRecord(event("E1", "P1", "S1", true));

		Assertions.assertNotNull(new PlaceAttestation(model, r -> true).of("P1"));
	}

	@Test
	void anyRecordAtAPlaceAndThePlaceItselfCountWhenCitedByADatedSource(){
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(source("S9", "P4", "1820-05-01").getChildren().getFirst()));
		//an attribute (residence), not an event
		final FLEFRecord attribute = event("A1", "P1", "S1", false);
		model.addRecord(FLEFRecord.createMainRecord("A1", "INDIVIDUAL_ATTRIBUTE")
			.addChild(attribute.getChildren().get(0))
			.addChild(attribute.getChildren().get(1)));
		//a place with its own source citation
		model.getRecordById("P4").addChild(FLEFRecord.createChildWithTag("source")
			.addChild(FLEFRecord.createChildWithTagAndValue("source", "S1")));

		final PlaceAttestation attestation = new PlaceAttestation(model, r -> true);
		Assertions.assertNotNull(attestation.of("P1"));
		Assertions.assertNotNull(attestation.of("P4"));
	}

	private static FLEFRecord event(final String id, final String placeId, final String sourceId, final boolean onPlace){
		final FLEFRecord citation = FLEFRecord.createChildWithTag("source")
			.addChild(FLEFRecord.createChildWithTagAndValue("source", sourceId));
		final FLEFRecord place = FLEFRecord.createChildWithTag("place")
			.addChild(FLEFRecord.createChildWithTagAndValue("place", placeId));
		final FLEFRecord event = FLEFRecord.createMainRecord(id, "EVENT")
			.addChild(place);
		if(onPlace)
			place.addChild(citation);
		else
			event.addChild(citation);
		return event;
	}

	private static String fullDate(final PlaceAttestation.Interval interval, final String tag){
		return FLEFRecordHelper.getChildValue(interval.holder(), FLEFRecordHelper.composePath(tag, "value", "point", "value", "full_date"));
	}

	private static FLEFRecord source(final String id, final String placeId, final String fullDate){
		return FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_DATE)
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))))
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceCitationReader.TAG_PLACE, placeId)));
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target, final String type){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TYPE, subject)))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TYPE, target)))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE, type));
	}

}

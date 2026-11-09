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
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.strategies.PlaceSearchStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


/**
 * A place is found by the names of the places that contain it.
 */
class PlaceContainerNamesTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		//Borgo is part of Treviso, that is part of Veneto (administrative); Treviso is also part of the Diocese (ecclesiastical)
		model.addRecord(place("P1", "Borgo"));
		model.addRecord(place("P2", "Treviso"));
		model.addRecord(place("P3", "Veneto"));
		model.addRecord(place("P4", "Diocese of Treviso"));
		model.addRecord(place("P5", "Elsewhere"));
		model.addRecord(relationship("PR1", "P1", "P2"));
		model.addRecord(relationship("PR2", "P2", "P3"));
		model.addRecord(relationship("PR3", "P2", "P4"));
	}


	@Test
	void containersAtAnyDistanceAndThroughAnyRelationship(){
		final PlaceNameResolver resolver = new PlaceNameResolver(model);

		Assertions.assertEquals(List.of("Treviso", "Veneto", "Diocese of Treviso"), resolver.containerNames("P1"));
		Assertions.assertEquals(List.of("Veneto", "Diocese of Treviso"), resolver.containerNames("P2"));
		Assertions.assertTrue(resolver.containerNames("P3").isEmpty());
		Assertions.assertTrue(resolver.containerNames("P5").isEmpty());
	}

	@Test
	void cycleDoesNotLoop(){
		model.addRecord(relationship("PR4", "P3", "P1"));

		final List<String> names = new PlaceNameResolver(model).containerNames("P1");

		Assertions.assertEquals(List.of("Treviso", "Veneto", "Diocese of Treviso"), names);
	}

	@Test
	void placeFoundByTheNameOfAContainer(){
		final Predicate<FLEFRecord> byVeneto = predicate("Veneto");

		Assertions.assertTrue(byVeneto.test(model.getRecordById("P1")));
		Assertions.assertTrue(byVeneto.test(model.getRecordById("P2")));
		//its own name
		Assertions.assertTrue(byVeneto.test(model.getRecordById("P3")));
		Assertions.assertFalse(byVeneto.test(model.getRecordById("P5")));
		//the container is not found by the name of what it contains
		Assertions.assertFalse(predicate("Borgo").test(model.getRecordById("P3")));
	}

	@Test
	void blankNameAdmitsEverything(){
		final Predicate<FLEFRecord> all = predicate("");

		Assertions.assertTrue(all.test(model.getRecordById("P5")));
	}


	private Predicate<FLEFRecord> predicate(final String name){
		final SearchCriteria criteria = new SearchCriteria(PlaceReader.TYPE, "", Set.of())
			.withFilter(PlaceReader.TAG_NAME, name);
		return new PlaceSearchStrategy().buildPredicate(criteria, model);
	}

	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, target)));
	}

}

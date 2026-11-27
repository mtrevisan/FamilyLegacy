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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** A place typed as a chain of names: the place, then the places that contain it. */
class PlaceChainTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
	}


	private void place(final String id, final String name){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name))));
	}

	private void partOf(final String id, final String subject, final String target){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, target)))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
				PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));
	}

	private static List<String> ids(final List<FLEFRecord> places){
		return places.stream()
			.map(FLEFRecord::getId)
			.toList();
	}


	@Test
	void theNamesAreTheOnesBetweenTheCommasTheSmallestFirst(){
		Assertions.assertEquals(List.of("Padova", "Veneto", "Italy"), PlaceChain.names("Padova, Veneto, Italy"));
		Assertions.assertEquals(List.of("Padova", "Veneto"), PlaceChain.names("  Padova ,  Veneto ,, "));
		Assertions.assertEquals(List.of("Padova di Sotto"), PlaceChain.names("Padova   di Sotto"));
		Assertions.assertTrue(PlaceChain.names("  , ").isEmpty());
		Assertions.assertTrue(PlaceChain.names(null).isEmpty());
	}

	@Test
	void theCandidatesHaveExactlyTheNameIgnoringCaseAndAccents(){
		place("P1", "Forlì");
		place("P2", "Forlimpopoli");
		place("P3", "FORLI");

		Assertions.assertEquals(List.of("P1", "P3"), ids(PlaceChain.candidates(model, " forli ", null)));
		Assertions.assertTrue(PlaceChain.candidates(model, "", null).isEmpty());
		Assertions.assertTrue(PlaceChain.candidates(model, "Venezia", null).isEmpty());
	}

	@Test
	void withAParentTheCandidatesAreTheOnesThatArePartOfIt(){
		place("P1", "Padova");
		place("P2", "Padova");
		place("P3", "Veneto");
		place("P4", "Lombardia");
		partOf("PR1", "P1", "P3");
		partOf("PR2", "P2", "P4");

		Assertions.assertEquals(List.of("P1", "P2"), ids(PlaceChain.candidates(model, "Padova", null)));
		Assertions.assertEquals(List.of("P1"), ids(PlaceChain.candidates(model, "Padova", "P3")));
		Assertions.assertEquals(List.of("P2"), ids(PlaceChain.candidates(model, "Padova", "P4")));
		Assertions.assertTrue(PlaceChain.candidates(model, "Padova", "P1").isEmpty());
	}

}

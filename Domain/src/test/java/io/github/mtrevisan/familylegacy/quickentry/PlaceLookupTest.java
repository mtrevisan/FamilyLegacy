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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


class PlaceLookupTest{

	private FLEFModel model;


	private static FLEFRecord place(final String id, final String... names){
		final FLEFRecord place = FLEFRecord.createMainRecord(id, PlaceReader.TYPE);
		for(final String name : names)
			place.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name)));
		return place;
	}

	private static List<String> ids(final List<FLEFRecord> places){
		return places.stream()
			.map(FLEFRecord::getId)
			.toList();
	}


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		model.addRecord(place("P1", "Padova"));
		model.addRecord(place("P2", "Padovana di Sotto"));
		model.addRecord(place("P3", "Patavium", "Padua"));
		model.addRecord(place("P4", "Forlì"));
	}


	@Test
	void theExactNamesComeBeforeTheOnesThatStartWithTheText(){
		model.addRecord(place("P5", "PADOVA"));

		Assertions.assertEquals(List.of("P1", "P5", "P2"), ids(PlaceLookup.find(model, "padova", 10)));
	}

	@Test
	void everyNameOfAPlaceCounts(){
		Assertions.assertEquals(List.of("P3"), ids(PlaceLookup.find(model, "Padua", 10)));
	}

	@Test
	void caseAccentsAndSpacesAreIgnored(){
		Assertions.assertEquals(List.of("P4"), ids(PlaceLookup.find(model, "  forli ", 10)));
		Assertions.assertEquals(List.of("P2"), ids(PlaceLookup.find(model, "padovana   DI sotto", 10)));
	}

	@Test
	void theResultIsLimited(){
		Assertions.assertEquals(List.of("P1"), ids(PlaceLookup.find(model, "pad", 1)));
		Assertions.assertTrue(PlaceLookup.find(model, "pad", 0).isEmpty());
	}

	@Test
	void blankOrUnknownFindsNothing(){
		Assertions.assertTrue(PlaceLookup.find(model, null, 10).isEmpty());
		Assertions.assertTrue(PlaceLookup.find(model, "   ", 10).isEmpty());
		Assertions.assertTrue(PlaceLookup.find(model, "Venezia", 10).isEmpty());
	}

}

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
package io.github.mtrevisan.familylegacy.consanguinity;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The graph of the biological ancestry: only the biological links of the model are in it. */
class AncestryGraphTest{

	private static FLEFRecord person(final String id){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
	}

	private static FLEFRecord link(final String id, final String childId, final String parentId, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
	}


	@Test
	void onlyTheBiologicalParentsAreKept(){
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("C", "BF", "BM", "AF", "FF", "SF"))
			model.addRecord(person(id));
		model.addRecord(link("R1", "C", "BF", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R2", "C", "BM", RelationshipReader.ENUM_TYPE_CHILD));
		model.addRecord(link("R3", "C", "AF", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));
		model.addRecord(link("R4", "C", "FF", RelationshipReader.ENUM_TYPE_FOSTER_CHILD));
		model.addRecord(link("R5", "C", "SF", RelationshipReader.ENUM_TYPE_STEP_CHILD));

		final AncestryGraph graph = AncestryGraph.of(model);

		Assertions.assertEquals(List.of("BF", "BM"), graph.parentsOf("C"));
	}

	@Test
	void anAdoptedChildOfRelativesIsNotInbred(){
		//A and B are siblings; C is the biological child of A and the adoptive one of B: C is not a child of two relatives by blood
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("F", "M", "A", "B", "C", "W"))
			model.addRecord(person(id));
		model.addRecord(link("R1", "A", "F", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R2", "A", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R3", "B", "F", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R4", "B", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R5", "C", "A", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R6", "C", "B", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));

		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(AncestryGraph.of(model));

		Assertions.assertEquals(List.of("A"), AncestryGraph.of(model).parentsOf("C"));
		Assertions.assertEquals(0., calculator.inbreeding("C"), 1e-12);
	}

	@Test
	void aBiologicalChildOfSiblingsIsInbred(){
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("F", "M", "A", "B", "C"))
			model.addRecord(person(id));
		model.addRecord(link("R1", "A", "F", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R2", "A", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R3", "B", "F", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R4", "B", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R5", "C", "A", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(link("R6", "C", "B", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));

		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(AncestryGraph.of(model));

		Assertions.assertEquals(0.25, calculator.inbreeding("C"), 1e-12);
	}

}

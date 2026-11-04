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
package io.github.mtrevisan.familylegacy.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.ego.EgoNetworkService;
import io.github.mtrevisan.familylegacy.ego.EgoNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.assistant.SyntheticTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** The union between the ego and a partner that is a relative is marked on the ego network (the badge on the edge). */
class EgoNetworkConsanguinityTest{

	//Xavier and Yvonne are first cousins (grandchildren of Gino and Gina) and married; Zeno is their child
	//Walter and Berta are not related: Berta is Yvonne's mother, Walter her father
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Gino /Rossi/
		1 SEX M
		0 @I2@ INDI
		1 NAME Gina /Bianchi/
		1 SEX F
		0 @I3@ INDI
		1 NAME Aldo /Rossi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Berta /Rossi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Wanda /Verdi/
		1 SEX F
		0 @I6@ INDI
		1 NAME Walter /Neri/
		1 SEX M
		0 @I7@ INDI
		1 NAME Xavier /Rossi/
		1 SEX M
		0 @I8@ INDI
		1 NAME Yvonne /Neri/
		1 SEX F
		0 @I9@ INDI
		1 NAME Zeno /Rossi/
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I3@
		1 CHIL @I4@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I5@
		1 CHIL @I7@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I4@
		1 CHIL @I8@
		0 @F4@ FAM
		1 HUSB @I7@
		1 WIFE @I8@
		1 CHIL @I9@
		1 MARR
		2 DATE 1 JAN 1900
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static String idOf(final FLEFModel model, final String firstName){
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE)){
			final String name = IndividualReader.extractPrimaryFullname(individual);
			if(name != null && name.startsWith(firstName))
				return individual.getId();
		}
		throw new IllegalArgumentException("No individual named " + firstName);
	}

	private static EgoNetworkService service(final FLEFModel model){
		return new EgoNetworkService(new GenealogyRepository(RelationshipReader.FAMILY, model), model);
	}

	private static EgoNode partnerOf(final EgoNode ego, final String id){
		for(final EgoNode partner : ego.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER))
			if(id.equals(partner.getEgoId()))
				return partner;
		throw new IllegalArgumentException("No partner " + id);
	}


	@Test
	void theCousinPartnerIsMarkedAsConsanguineous() throws IOException{
		final FLEFModel model = convert();
		final String xavier = idOf(model, "Xavier");
		final String yvonne = idOf(model, "Yvonne");

		final EgoNode ego = service(model).buildEgoNetwork(xavier);
		final ConsanguinityReport.ConsanguineousUnion union = partnerOf(ego, yvonne).getConsanguineousUnion();

		Assertions.assertNotNull(union, "first cousins");
		Assertions.assertEquals(0.0625, union.kinship(), 1e-12);
		Assertions.assertEquals("first cousins", union.term());
	}

	@Test
	void theMarkIsTheSameFromTheOtherPartner() throws IOException{
		final FLEFModel model = convert();
		final String xavier = idOf(model, "Xavier");
		final String yvonne = idOf(model, "Yvonne");

		final EgoNode ego = service(model).buildEgoNetwork(yvonne);

		Assertions.assertNotNull(partnerOf(ego, xavier).getConsanguineousUnion());
	}

	@Test
	void anUnrelatedPartnerIsNotMarked() throws IOException{
		final FLEFModel model = convert();
		final String walter = idOf(model, "Walter");
		final String berta = idOf(model, "Berta");

		final EgoNode ego = service(model).buildEgoNetwork(walter);

		Assertions.assertNull(partnerOf(ego, berta).getConsanguineousUnion());
	}

	@Test
	void theMarkIsRecomputedAfterTheIndicesAreInvalidated() throws IOException{
		final FLEFModel model = convert();
		final String xavier = idOf(model, "Xavier");
		final String yvonne = idOf(model, "Yvonne");
		final EgoNetworkService service = service(model);

		Assertions.assertNotNull(partnerOf(service.buildEgoNetwork(xavier), yvonne).getConsanguineousUnion());

		service.invalidateIndices();

		Assertions.assertNotNull(partnerOf(service.buildEgoNetwork(xavier), yvonne).getConsanguineousUnion());
	}

	/**
	 * The ego network looks at the couples of the ego only, not at the whole file (the report of a big file takes minutes): what it
	 * marks must be what the report of the whole file says, for every couple of a tree full of cousins that marry.
	 */
	@Test
	void theMarksAreTheOnesOfTheReportOfTheWholeFile() throws IOException{
		final FLEFModel model = SyntheticTree.model(600);
		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(model);
		Assertions.assertFalse(report.unions().isEmpty(), "the tree has cousins that marry");
		final Map<String, ConsanguinityReport.ConsanguineousUnion> expected = new HashMap<>();
		for(final ConsanguinityReport.ConsanguineousUnion union : report.unions())
			expected.put(union.firstId() + '|' + union.secondId(), union);

		final EgoNetworkService service = service(model);
		int marked = 0;
		int partners = 0;
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE)){
			final EgoNode ego = service.buildEgoNetwork(individual.getId());
			for(final EgoNode partner : ego.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER)){
				partners ++;
				final String first = (ego.getEgoId().compareTo(partner.getEgoId()) <= 0? ego.getEgoId(): partner.getEgoId());
				final String second = (first.equals(ego.getEgoId())? partner.getEgoId(): ego.getEgoId());
				final ConsanguinityReport.ConsanguineousUnion union = expected.get(first + '|' + second);
				final ConsanguinityReport.ConsanguineousUnion found = partner.getConsanguineousUnion();

				if(union == null)
					Assertions.assertNull(found, ego.getEgoId() + " and " + partner.getEgoId());
				else{
					Assertions.assertNotNull(found, ego.getEgoId() + " and " + partner.getEgoId());
					Assertions.assertEquals(union.kinship(), found.kinship(), 1e-12);
					Assertions.assertEquals(union.relationship(), found.relationship(), 1e-12);
					Assertions.assertEquals(union.commonAncestors(), found.commonAncestors());
					marked ++;
				}
			}
		}
		Assertions.assertTrue(partners > 0);
		Assertions.assertTrue(marked > 0, "some partners are related");
	}

}

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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** The sex of an individual is the preferred assertion among its sex attributes. */
class PreferredSexTest{

	private FLEFModel model;
	private FLEFRecord individual;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		model.addRecord(individual);
	}


	private FLEFRecord addSex(final String id, final String value, final String confidence){
		final FLEFRecord attribute = FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));
		if(confidence != null)
			attribute.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_CONFIDENCE, confidence));
		model.addRecord(attribute);
		return attribute;
	}

	private void addConclusion(final String id, final String status, final String updated, final String preferred, final String... resolves){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "sex"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, status));
		for(final String target : resolves)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue("individual_attribute", target)));
		if(preferred != null)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
				.addChild(FLEFRecord.createChildWithTagAndValue("individual_attribute", preferred)));
		conclusion.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, "2020-01-01T00:00:00Z")))
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_UPDATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, updated))));
		model.addRecord(conclusion);
	}

	private SexType sex(){
		return PreferredSex.of(individual, model);
	}


	@Test
	void noAssertionIsUndetermined(){
		Assertions.assertEquals(SexType.UNKNOWN, sex());
		Assertions.assertNull(PreferredSex.preferredAssertion(individual, model));
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(null, model));
	}

	@Test
	void theOnlyAssertionIsTheSex(){
		addSex("IA1", "female", null);

		Assertions.assertEquals(SexType.FEMALE, sex());
		Assertions.assertEquals("female", PreferredSex.rawOf(individual, model));
	}

	@Test
	void theAssertionsOfOtherIndividualsAndOtherTypesAreIgnored(){
		final FLEFRecord other = addSex("IA1", "male", null);
		other.getChildren().removeIf(child -> IndividualAttributeReader.TAG_INDIVIDUAL.equals(child.getTag()));
		other.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I2"));
		final FLEFRecord occupation = addSex("IA2", "male", null);
		occupation.getChildren().removeIf(child -> IndividualAttributeReader.TAG_TYPE.equals(child.getTag()));
		occupation.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_OCCUPATION));

		Assertions.assertEquals(SexType.UNKNOWN, sex());
	}

	@Test
	void anUnknownAssertionSaysNothing(){
		addSex("IA1", "unknown", null);
		addSex("IA2", "male", null);

		Assertions.assertEquals(SexType.MALE, sex());
	}

	@Test
	void conflictingAssertionsWithoutConfidenceOrConclusionAreUndetermined(){
		addSex("IA1", "male", null);
		addSex("IA2", "female", null);

		Assertions.assertEquals(SexType.UNKNOWN, sex());
	}

	@Test
	void theStrictlyHighestConfidenceWins(){
		addSex("IA1", "male", ConfidenceReader.ENUM_LOW);
		addSex("IA2", "female", ConfidenceReader.ENUM_HIGH);
		addSex("IA3", "male", ConfidenceReader.ENUM_MEDIUM);

		Assertions.assertEquals(SexType.FEMALE, sex());
	}

	@Test
	void aTieInTheHighestConfidenceIsUndetermined(){
		addSex("IA1", "male", ConfidenceReader.ENUM_HIGH);
		addSex("IA2", "female", ConfidenceReader.ENUM_HIGH);

		Assertions.assertEquals(SexType.UNKNOWN, sex());
	}

	@Test
	void aConclusionOverridesTheConfidence(){
		addSex("IA1", "male", ConfidenceReader.ENUM_LOW);
		addSex("IA2", "female", ConfidenceReader.ENUM_HIGH);
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "2021-01-01T00:00:00Z", "IA1", "IA1", "IA2");

		Assertions.assertEquals(SexType.MALE, sex());
		Assertions.assertEquals("IA1", PreferredSex.preferredAssertion(individual, model).getId());
	}

	@Test
	void aConclusionResolvesTheConflictWithoutConfidence(){
		addSex("IA1", "male", null);
		addSex("IA2", "female", null);
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, "2021-01-01T00:00:00Z", "IA2", "IA1", "IA2");

		Assertions.assertEquals(SexType.FEMALE, sex());
	}

	@Test
	void provenBeatsSupportedWhateverTheDate(){
		addSex("IA1", "male", null);
		addSex("IA2", "female", null);
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "2025-01-01T00:00:00Z", "IA1", "IA1", "IA2");
		addConclusion("CC2", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, "2021-01-01T00:00:00Z", "IA2", "IA1", "IA2");

		Assertions.assertEquals(SexType.FEMALE, sex());
	}

	@Test
	void amongEqualStatusesTheMostRecentConclusionWins(){
		addSex("IA1", "male", null);
		addSex("IA2", "female", null);
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "2021-01-01T00:00:00Z", "IA1", "IA1", "IA2");
		addConclusion("CC2", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "2022-01-01T00:00:00Z", "IA2", "IA1", "IA2");

		Assertions.assertEquals(SexType.FEMALE, sex());
	}

	@Test
	void aConclusionThatDecidesNothingDoesNotApply(){
		addSex("IA1", "male", ConfidenceReader.ENUM_HIGH);
		addSex("IA2", "female", null);
		//conflicting evidence and disproven do not select an assertion
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE, "2021-01-01T00:00:00Z", "IA2", "IA1", "IA2");
		addConclusion("CC2", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, "2022-01-01T00:00:00Z", "IA2", "IA1", "IA2");

		Assertions.assertEquals(SexType.MALE, sex());
	}

	@Test
	void aConclusionThatResolvesOnlyOneAssertionDoesNotApply(){
		addSex("IA1", "male", null);
		addSex("IA2", "female", null);
		addConclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, "2021-01-01T00:00:00Z", "IA1", "IA1");

		Assertions.assertEquals(SexType.UNKNOWN, sex());
	}

	@Test
	void theIndexGivesTheSameSexAsTheModel(){
		addSex("IA1", "male", ConfidenceReader.ENUM_LOW);
		addSex("IA2", "female", ConfidenceReader.ENUM_HIGH);

		Assertions.assertEquals(sex(), PreferredSex.of(individual, PreferredSex.indexByIndividual(model), model));
		Assertions.assertEquals(SexType.UNKNOWN,
			PreferredSex.of(FLEFRecord.createMainRecord("I9", IndividualReader.TYPE), PreferredSex.indexByIndividual(model), model));
	}

}

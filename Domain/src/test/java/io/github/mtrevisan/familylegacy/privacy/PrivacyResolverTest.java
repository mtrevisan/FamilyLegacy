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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;


class PrivacyResolverTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
	private static final String AUDIT = "\taudit.creation.date 2026-01-01T00:00:00Z\n";


	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar initializes the index of the Xref paths
		try(final InputStream is = PrivacyResolverTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static FLEFModel parse(final String records){
		return new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""" + records);
	}

	private static String individual(final String id, final String extra){
		return "individual " + id + " {\n" + extra + AUDIT + "}\n";
	}

	private static String place(final String id, final String extra){
		return "place " + id + " {\n" + extra + AUDIT + "}\n";
	}

	private static String event(final String id, final String type, final String extra){
		return "event " + id + " {\n\ttype " + type + StringUtils.LF + extra + AUDIT + "}\n";
	}

	private static String participation(final String id, final String eventId, final String individualId, final String role){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual " + individualId + StringUtils.LF
			+ "\trole " + role + StringUtils.LF + AUDIT + "}\n";
	}

	private static String attribute(final String id, final String individualId, final String type){
		return "individual_attribute " + id + " {\n\tindividual " + individualId + "\n\ttype " + type + "\n\tvalue x\n" + AUDIT + "}\n";
	}


	@Test
	void declaredLevelIsUsed(){
		final FLEFModel model = parse(individual("I1", "\tprivacy.level confidential\n"));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		final PrivacyResolver.Effective effective = resolver.describe(model.getRecordById("I1"));

		Assertions.assertEquals(PrivacyLevel.CONFIDENTIAL, effective.level());
		Assertions.assertEquals(PrivacyResolver.Origin.DECLARED, effective.origin());
	}

	@Test
	void expiredRestrictionNoLongerApplies(){
		final FLEFModel model = parse(place("P1", "\tprivacy.level restricted\n\tprivacy.expires 2000-01-01\n")
			+ place("P2", "\tprivacy.level restricted\n\tprivacy.expires 2030-01-01\n"));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("P1")));
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("P2")));
	}

	@Test
	void individualThatMayBeLivingIsRestricted(){
		final FLEFModel model = parse(individual("I1", "")
			+ individual("I2", "")
			+ event("E1", "death", "")
			+ participation("EP1", "E1", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		final PrivacyResolver.Effective living = resolver.describe(model.getRecordById("I1"));
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, living.level());
		Assertions.assertEquals(PrivacyResolver.Origin.LIVING_PERSON, living.origin());

		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("I2")));
	}

	@Test
	void principalOfAnOldBirthIsNotLiving(){
		final FLEFModel model = parse(individual("I1", "")
			+ individual("I2", "")
			+ individual("I3", "")
			+ event("E1", EventReader.ENUM_TYPE_BIRTH, "\tdate.value.point.value.full_date 1850-03-04\n")
			+ event("E2", EventReader.ENUM_TYPE_BIRTH, "\tdate.value.point.value.full_date 1990-03-04\n")
			+ event("E3", EventReader.ENUM_TYPE_BIRTH, "")
			+ participation("EP1", "E1", "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP2", "E2", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP3", "E3", "I3", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("I1")));
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("I2")));
		//an unknown birth date does not exclude that the individual is living
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("I3")));
	}

	@Test
	void anEventIsAboutTheLivingOnlyIfAPrincipalMayBeLiving(){
		//the birth of somebody long dead, with a midwife that may be living (no date): it is not about a living person
		final FLEFModel model = parse(individual("I1", "")
			+ individual("I2", "")
			+ individual("I3", "")
			+ event("E1", EventReader.ENUM_TYPE_BIRTH, "\tdate.value.point.value.full_date 1850-03-04\n")
			+ event("E2", EventReader.ENUM_TYPE_BIRTH, "\tdate.value.point.value.full_date 1990-03-04\n")
			+ participation("EP1", "E1", "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP2", "E1", "I3", EventParticipationReader.ENUM_ROLE_WITNESS)
			+ participation("EP3", "E2", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		Assertions.assertFalse(resolver.concernsLiving(model.getRecordById("I1")));
		Assertions.assertTrue(resolver.concernsLiving(model.getRecordById("I3")));
		//the level of the event is still the one inherited, as the policy says
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("E1")));
		Assertions.assertFalse(resolver.concernsLiving(model.getRecordById("E1")));
		Assertions.assertTrue(resolver.concernsLiving(model.getRecordById("E2")));
	}

	@Test
	void onlyThePrincipalCountsAsBornOrDead(){
		final FLEFModel model = parse(individual("I1", "")
			+ event("E1", "death", "")
			+ participation("EP1", "E1", "I1", EventParticipationReader.ENUM_ROLE_WITNESS));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("I1")));
	}

	@Test
	void declaredPublicOverridesTheLivingDefault(){
		final FLEFModel model = parse(individual("I1", "\tprivacy.level public\n"));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("I1")));
	}

	@Test
	void attributesInheritFromTheirIndividualAndNationalIdIsConfidential(){
		final FLEFModel model = parse(individual("I1", "")
			+ individual("I2", "")
			+ event("E1", "death", "")
			+ participation("EP1", "E1", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ attribute("A1", "I1", "occupation")
			+ attribute("A2", "I2", "occupation")
			+ attribute("A3", "I2", "national_id"));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		final PrivacyResolver.Effective inherited = resolver.describe(model.getRecordById("A1"));
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, inherited.level());
		Assertions.assertEquals(PrivacyResolver.Origin.INHERITED, inherited.origin());
		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("A2")));
		Assertions.assertEquals(PrivacyLevel.CONFIDENTIAL, resolver.effectiveLevel(model.getRecordById("A3")));
	}

	@Test
	void eventInheritsFromItsParticipants(){
		final FLEFModel model = parse(individual("I1", "")
			+ individual("I2", "")
			+ event("E1", "death", "")
			+ event("E2", "marriage", "")
			+ event("E3", "marriage", "")
			+ participation("EP1", "E1", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP2", "E2", "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP3", "E2", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL)
			+ participation("EP4", "E3", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		//a living participant restricts the event
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("E2")));
		//a participation inherits from its participant too
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("EP2")));
		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("E3")));
	}

	@Test
	void nestedStructureNeverIsLessRestrictiveThanItsContainer(){
		final FLEFModel model = parse(individual("I1", "\tprivacy.level confidential\n") + place("P1", ""));
		final PrivacyResolver resolver = new PrivacyResolver(model, TODAY);

		final FLEFRecord restrictedNote = FLEFRecord.createEmpty();
		restrictedNote.addChild(FLEFRecord.createChildWithTag("privacy")
			.addChild(FLEFRecord.createChildWithTagAndValue("level", "restricted")));
		final FLEFRecord publicNote = FLEFRecord.createEmpty();

		Assertions.assertEquals(PrivacyLevel.CONFIDENTIAL, resolver.effectiveLevel(model.getRecordById("I1"), restrictedNote));
		Assertions.assertEquals(PrivacyLevel.CONFIDENTIAL, resolver.effectiveLevel(model.getRecordById("I1"), publicNote));
		Assertions.assertEquals(PrivacyLevel.RESTRICTED, resolver.effectiveLevel(model.getRecordById("P1"), restrictedNote));
		Assertions.assertEquals(PrivacyLevel.PUBLIC, resolver.effectiveLevel(model.getRecordById("P1"), publicNote));
	}

	@Test
	void policyHidesTheExpectedLevels(){
		Assertions.assertFalse(PrivacyPolicy.SHOW_ALL.hides(PrivacyLevel.CONFIDENTIAL));

		Assertions.assertFalse(PrivacyPolicy.HIDE_CONFIDENTIAL.hides(PrivacyLevel.RESTRICTED));
		Assertions.assertTrue(PrivacyPolicy.HIDE_CONFIDENTIAL.hides(PrivacyLevel.CONFIDENTIAL));

		Assertions.assertFalse(PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL.hides(PrivacyLevel.PUBLIC));
		Assertions.assertTrue(PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL.hides(PrivacyLevel.RESTRICTED));
		Assertions.assertTrue(PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL.hides(PrivacyLevel.CONFIDENTIAL));
	}

}

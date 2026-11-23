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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** What hangs from a source (citations, individuals, families, events, extracts, media) and how the sources are grouped. */
class SourceWorkspaceTest{

	private static FLEFRecord citation(final String sourceId, final String locator, final String stance){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		return citation;
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, subject)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, target)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
	}

	private static FLEFRecord participation(final String id, final String individual, final String event){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individual)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event));
	}

	private static FLEFRecord source(final String id, final String title){
		return FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE, title));
	}

	private static FLEFRecord repositoryCitation(final String repositoryId, final String callNumber){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
			.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REPOSITORY, repositoryId));
		if(callNumber != null)
			citation.addChild(IdentifierReader.create(IdentifierReader.ENUM_SCHEME_CALL_NUMBER, callNumber));
		return citation;
	}

	private static FLEFRecord repository(final String id, final String name, final String type){
		return FLEFRecord.createMainRecord(id, RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, name)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryReader.TAG_TYPE, type));
	}

	private static List<String> ids(final List<FLEFRecord> records){
		return records.stream()
			.map(FLEFRecord::getId)
			.toList();
	}

	/**
	 * <pre>
	 * S1 is cited by: the individual A (locator p. 3), the attribute AT1 of B, the relationship R1 (spouses A-W), the child
	 * relationship R2 (B of A, not a family), the event E1 (participants A and W), the participation EP2 (of B in the event E2),
	 * and, contradicting, the individual D; S2 is cited by nobody.
	 * </pre>
	 */
	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(final String id : new String[]{"A", "B", "D", "W"})
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		model.getRecordById("A").addChild(citation("S1", "p. 3", SourceCitationReader.STANCE_SUPPORTS));
		model.getRecordById("D").addChild(citation("S1", null, SourceCitationReader.STANCE_CONTRADICTS));
		//a citation of another source is not a citation of S1
		model.getRecordById("W").addChild(citation("S2X", null, SourceCitationReader.STANCE_SUPPORTS));

		model.addRecord(FLEFRecord.createMainRecord("AT1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "B"))
			.addChild(citation("S1", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(relationship("R1", "W", "A", RelationshipReader.ENUM_TYPE_SPOUSE)
			.addChild(citation("S1", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(relationship("R2", "B", "A", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.addChild(citation("S1", null, SourceCitationReader.STANCE_SUPPORTS)));

		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(citation("S1", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(FLEFRecord.createMainRecord("E2", EventReader.TYPE));
		model.addRecord(participation("EP1", "A", "E1"));
		model.addRecord(participation("EP3", "W", "E1"));
		model.addRecord(participation("EP2", "B", "E2")
			.addChild(citation("S1", null, SourceCitationReader.STANCE_SUPPORTS)));

		model.addRecord(FLEFRecord.createMainRecord("X1", ExtractReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, "S1"))
			.addChild(FLEFRecord.createChildWithTag(ExtractReader.TAG_MEDIA_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_MEDIA, "M2"))));
		model.addRecord(FLEFRecord.createMainRecord("X2", ExtractReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, "S2")));

		model.addRecord(source("S1", "Parish register")
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TYPE.toLowerCase(), "M1")));
		model.addRecord(source("S2", "Census"));
		model.addRecord(FLEFRecord.createMainRecord("M1", MediaReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("M2", MediaReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("M3", MediaReader.TYPE));
		return model;
	}


	@Test
	void everythingThatCitesTheSourceIsListedAndTheWayBackEndsAtThePeople(){
		final SourceWorkspace workspace = SourceWorkspace.of(model(), "S1");

		Assertions.assertEquals(7, workspace.citations().size());
		final SourceWorkspace.Citation first = workspace.citations().getFirst();
		Assertions.assertEquals("A", first.owner().getId());
		Assertions.assertEquals("p. 3", first.locator());
		Assertions.assertFalse(first.contradicts());
		//source -> citation -> person
		Assertions.assertEquals(List.of("A"), ids(first.individuals()));
		Assertions.assertTrue(workspace.citations().stream().anyMatch(citation -> citation.owner().getId().equals("D")
			&& citation.contradicts()));

		//A and D cite it themselves, W through the event (and the relationship), B through the attribute (and the participation)
		Assertions.assertEquals(List.of("A", "D", "W", "B"), ids(workspace.individuals()));
	}

	@Test
	void anEventIsAboutItsParticipantsAndACitedParticipationBacksItsEvent(){
		final SourceWorkspace workspace = SourceWorkspace.of(model(), "S1");

		final SourceWorkspace.Citation event = workspace.citations().stream()
			.filter(citation -> citation.owner().getId().equals("E1"))
			.findFirst().orElseThrow();
		Assertions.assertEquals(List.of("A", "W"), ids(event.individuals()));

		Assertions.assertEquals(List.of("E1", "E2"), ids(workspace.events()));
	}

	@Test
	void aFamilyIsAPartnershipNotAParentChildLink(){
		final SourceWorkspace workspace = SourceWorkspace.of(model(), "S1");

		Assertions.assertEquals(List.of("R1"), ids(workspace.families()));
	}

	@Test
	void theExtractsAreTheOnesOfTheSourceAndTheMediaAreTheOnesItAndItsExtractsReferTo(){
		final SourceWorkspace workspace = SourceWorkspace.of(model(), "S1");

		Assertions.assertEquals(List.of("X1"), ids(workspace.extracts()));
		Assertions.assertEquals(List.of("M1", "M2"), ids(workspace.media()));
	}

	@Test
	void aSourceNobodyCitesHasAnEmptyWorkspace(){
		final SourceWorkspace workspace = SourceWorkspace.of(model(), "S2");

		Assertions.assertTrue(workspace.citations().isEmpty());
		Assertions.assertTrue(workspace.individuals().isEmpty());
		Assertions.assertEquals(List.of("X2"), ids(workspace.extracts()));
	}

	@Test
	void whatIsNotASourceHasNoWorkspace(){
		final FLEFModel model = model();

		Assertions.assertNull(SourceWorkspace.of(model, "A"));
		Assertions.assertNull(SourceWorkspace.of(model, "missing"));
		Assertions.assertNull(SourceWorkspace.of(model, null));
	}

	/**
	 * <pre>
	 * R1 (archive "State Archive"): S1 as MS 45/12, S3 as MS 45/13
	 * R2 (library "City Library"): S1 as 900.1
	 * S2 has no repository
	 * </pre>
	 */
	private static FLEFModel modelWithRepositories(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(repository("R1", "State Archive", RepositoryReader.ENUM_TYPE_ARCHIVE));
		model.addRecord(repository("R2", "City Library", RepositoryReader.ENUM_TYPE_LIBRARY));
		model.addRecord(source("S1", "Baptisms")
			.addChild(repositoryCitation("R1", "MS 45/12"))
			.addChild(repositoryCitation("R2", "900.1")));
		model.addRecord(source("S2", "Census"));
		model.addRecord(source("S3", "Burials")
			.addChild(repositoryCitation("R1", "MS 45/13")));
		return model;
	}

	private static List<String> keys(final List<SourceWorkspace.SourceGroup> groups){
		return groups.stream()
			.map(SourceWorkspace.SourceGroup::key)
			.toList();
	}

	@Test
	void theSourcesAreListedByRepositoryAndASourceInTwoRepositoriesIsInBoth(){
		final List<SourceWorkspace.SourceGroup> groups = SourceWorkspace.group(modelWithRepositories(), SourceWorkspace.Grouping.REPOSITORY);

		//the sources with no repository come last
		Assertions.assertEquals(List.of("City Library", "State Archive", SourceWorkspace.NO_GROUP), keys(groups));
		Assertions.assertEquals(List.of("S1"), ids(groups.get(0).sources()));
		//by title: Baptisms, Burials
		Assertions.assertEquals(List.of("S1", "S3"), ids(groups.get(1).sources()));
		Assertions.assertEquals(List.of("S2"), ids(groups.get(2).sources()));
	}

	@Test
	void theSourcesAreListedByKindOfRepository(){
		final List<SourceWorkspace.SourceGroup> groups = SourceWorkspace.group(modelWithRepositories(), SourceWorkspace.Grouping.REPOSITORY_TYPE);

		Assertions.assertEquals(List.of(RepositoryReader.ENUM_TYPE_ARCHIVE, RepositoryReader.ENUM_TYPE_LIBRARY, SourceWorkspace.NO_GROUP),
			keys(groups));
		//S1 is kept by an archive and a library, but counts once in each group
		Assertions.assertEquals(List.of("S1", "S3"), ids(groups.get(0).sources()));
		Assertions.assertEquals(List.of("S1"), ids(groups.get(1).sources()));
	}

	@Test
	void theSourcesAreListedByShelfMarkInsideTheRepository(){
		final List<SourceWorkspace.SourceGroup> groups = SourceWorkspace.group(modelWithRepositories(), SourceWorkspace.Grouping.RECORD_GROUP);

		Assertions.assertEquals(List.of("City Library / 900.1", "State Archive / MS 45/12", "State Archive / MS 45/13",
			SourceWorkspace.NO_GROUP), keys(groups));
	}

	@Test
	void aSourceWithARepositoryWithoutShelfMarkIsInNoRecordGroup(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(repository("R1", "State Archive", RepositoryReader.ENUM_TYPE_ARCHIVE));
		model.addRecord(source("S1", "Baptisms").addChild(repositoryCitation("R1", null)));

		Assertions.assertEquals(List.of(SourceWorkspace.NO_GROUP), keys(SourceWorkspace.group(model, SourceWorkspace.Grouping.RECORD_GROUP)));
		Assertions.assertEquals(List.of("State Archive"), keys(SourceWorkspace.group(model, SourceWorkspace.Grouping.REPOSITORY)));
	}

}

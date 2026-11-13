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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.EventIndex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/** The privacy filter applied to the participants of an event, and the records related to a research activity or task. */
class ReportContextPrivacyAndRelatedTest{

	@Test
	void aHiddenParticipantIsSkipped(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PRIVACY)
				.addChild(FLEFRecord.createChildWithTagAndValue(PrivacyReader.TAG_LEVEL, PrivacyReader.ENUM_LEVEL_CONFIDENTIAL))));
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH)));
		model.addRecord(participation("EP1", "E1", "I1"));
		model.addRecord(participation("EP2", "E1", "I2"));

		final ReportConfig hiding = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.HIDE_CONFIDENTIAL)
			.withLivingPrivacy(LivingPrivacy.SHOW);
		final ReportContext hidingContext = ReportContext.build(model, model.getRecordById("I1"), hiding);
		final List<EventIndex.Participant> visible = hidingContext.index.participantsOf(model.getRecordById("E1"));
		Assertions.assertEquals(1, visible.size());
		Assertions.assertEquals("I1", visible.getFirst().record().getId());

		final ReportConfig showing = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL);
		final ReportContext showingContext = ReportContext.build(model, model.getRecordById("I1"), showing);
		Assertions.assertEquals(2, showingContext.index.participantsOf(model.getRecordById("E1")).size());
	}

	@Test
	void relatedToAResearchActivity(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("RE1", RepositoryReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
				.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REPOSITORY, "RE1"))));
		model.addRecord(FLEFRecord.createMainRecord("RQ1", ResearchQuestionReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("RA1", ResearchActivityReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_QUESTION, "RQ1"))
			.addChild(FLEFRecord.createChildWithTag(ResearchActivityReader.TAG_SOURCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1"))));
		model.addRecord(FLEFRecord.createMainRecord("RT1", ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_CREATED_BY, "RA1")));

		final ReportConfig config = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL);

		final Set<String> activity = ReportContext.build(model, model.getRecordById("RA1"), config).relatedRecordIds();
		Assertions.assertEquals(Set.of("RA1", "RQ1", "S1", "RE1", "RT1"), activity);

		final Set<String> task = ReportContext.build(model, model.getRecordById("RT1"), config).relatedRecordIds();
		Assertions.assertEquals(Set.of("RT1", "RA1"), task);
	}


	private static FLEFRecord participation(final String id, final String eventId, final String individualId){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL));
	}

}

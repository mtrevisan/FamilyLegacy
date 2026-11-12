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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The connections of the temporal projection: a relationship whose endpoint is a group is drawn too. */
class TemporalProjectionServiceTest{

	private FLEFModel model;


	/**
	 * Individual I1 and groups G1 and G2, each with a dated event (so that each has a row); the relationships R1 (I1 is a member of G1) and
	 * R2 (G1 is part of G2).
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("G2", GroupReader.TYPE));

		addDatedEvent("E1", "EP1", "1800-01-01", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1");
		addDatedEvent("E2", "EP2", "1800-01-01", EventParticipationReader.TAG_PARTICIPANT_GROUP, "G1");
		addDatedEvent("E3", "EP3", "1800-01-01", EventParticipationReader.TAG_PARTICIPANT_GROUP, "G2");

		model.addRecord(relationship("R1", "group_member", RelationshipReader.TAG_SUBJECT, "individual", "I1", "group", "G1"));
		model.addRecord(relationship("R2", "part_of", RelationshipReader.TAG_SUBJECT, "group", "G1", "group", "G2"));
	}


	private void addDatedEvent(final String eventId, final String participationId, final String fullDate, final String participantTag,
			final String participantId){
		model.addRecord(FLEFRecord.createMainRecord(eventId, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(point(EventReader.TAG_DATE, fullDate)));
		model.addRecord(FLEFRecord.createMainRecord(participationId, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(participantTag, participantId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
	}

	private static FLEFRecord relationship(final String id, final String type, final String subjectTag, final String subjectKind,
			final String subjectId, final String targetKind, final String targetId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(subjectTag)
				.addChild(FLEFRecord.createChildWithTagAndValue(subjectKind, subjectId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(targetKind, targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(point(RelationshipReader.TAG_VALID_FROM, "1801-01-01"))
			.addChild(point(RelationshipReader.TAG_VALID_TO, "1850-01-01"));
	}

	private static FLEFRecord point(final String tag, final String fullDate){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}

	private TemporalConnection connectionOf(final List<TemporalConnection> connections, final String relationshipId){
		return connections.stream()
			.filter(connection -> relationshipId.equals(connection.sourceRecord().getId()))
			.findFirst()
			.orElse(null);
	}


	@Test
	void aGroupMembershipIsAnIndividualGroupConnection(){
		final TemporalProjectionModel projection = new TemporalProjectionService(model).build();

		final TemporalConnection connection = connectionOf(projection.connections(), "R1");
		Assertions.assertNotNull(connection);
		Assertions.assertEquals(TemporalConnectionType.INDIVIDUAL_GROUP, connection.type());
		Assertions.assertEquals("I1", connection.sourceRow().id());
		Assertions.assertEquals("G1", connection.targetRow().id());
	}

	@Test
	void aPartOfRelationshipIsAGroupGroupConnection(){
		final TemporalProjectionModel projection = new TemporalProjectionService(model).build();

		final TemporalConnection connection = connectionOf(projection.connections(), "R2");
		Assertions.assertNotNull(connection);
		Assertions.assertEquals(TemporalConnectionType.GROUP_GROUP, connection.type());
		Assertions.assertEquals("G1", connection.sourceRow().id());
		Assertions.assertEquals("G2", connection.targetRow().id());
	}

}

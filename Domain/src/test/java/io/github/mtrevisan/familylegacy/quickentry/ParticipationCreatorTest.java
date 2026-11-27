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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** The participation of a person in an event is made with one call. */
class ParticipationCreatorTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
	}


	@Test
	void theParticipationLinksThePersonToTheEventWithTheRole(){
		final FLEFRecord participation = ParticipationCreator.addIndividual(model, "E1", "I2", EventParticipationReader.ENUM_ROLE_SPONSOR);

		Assertions.assertNotNull(participation);
		Assertions.assertSame(participation, model.getRecordById(participation.getId()));
		Assertions.assertEquals("E1", FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT));
		Assertions.assertEquals("I2", EventParticipationReader.extractParticipantRef(participation,
			EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL));
		Assertions.assertEquals("sponsor", FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE));
	}

	@Test
	void theEventDoesNotHaveToBeInTheModelYet(){
		//an event that is being created has an id and is not in the file until it is saved
		Assertions.assertNotNull(ParticipationCreator.addIndividual(model, "E99", "I1", EventParticipationReader.ENUM_ROLE_WITNESS));
	}

	@Test
	void thePersonHasTheRoleOnlyOnceInTheEvent(){
		Assertions.assertNotNull(ParticipationCreator.addIndividual(model, "E1", "I2", EventParticipationReader.ENUM_ROLE_WITNESS));
		final int records = model.getRecordCount();

		Assertions.assertNull(ParticipationCreator.addIndividual(model, "E1", "I2", EventParticipationReader.ENUM_ROLE_WITNESS));

		Assertions.assertEquals(records, model.getRecordCount());
		//another role of the same person, or the same role in another event, is another participation
		Assertions.assertNotNull(ParticipationCreator.addIndividual(model, "E1", "I2", EventParticipationReader.ENUM_ROLE_INFORMANT));
		Assertions.assertNotNull(ParticipationCreator.addIndividual(model, "E2", "I2", EventParticipationReader.ENUM_ROLE_WITNESS));
	}

	@Test
	void noRoleIsAParticipationWithNoRole(){
		final FLEFRecord participation = ParticipationCreator.addIndividual(model, "E1", "I1", null);

		Assertions.assertNotNull(participation);
		Assertions.assertNull(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE));
		Assertions.assertNull(ParticipationCreator.addIndividual(model, "E1", "I1", ""));
	}

	@Test
	void aMissingIdMakesNothing(){
		Assertions.assertNull(ParticipationCreator.addIndividual(model, null, "I1", "witness"));
		Assertions.assertNull(ParticipationCreator.addIndividual(model, "E1", " ", "witness"));
	}

}

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
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * Makes the participation of a person in an event with one call: the link between the individual and the event, with the role the
 * person had in it (a witness of a baptism, the godparent, the priest who performed it).
 */
public final class ParticipationCreator{

	private ParticipationCreator(){}


	/**
	 * Adds the participation of an individual in an event to the model. Nothing is added if the individual already has that role in
	 * the event.
	 *
	 * @param model	The model (the participation is added to it).
	 * @param eventId	The id of the event (the event may not be in the model yet, if it is being created).
	 * @param individualId	The id of the individual.
	 * @param role	The role (one of {@link EventParticipationReader#ROLES}), {@code null} or blank for none.
	 * @return	The participation, {@code null} if the individual already had the role in the event (or an id is missing).
	 */
	public static FLEFRecord addIndividual(final FLEFModel model, final String eventId, final String individualId, final String role){
		if(StringUtils.isBlank(eventId) || StringUtils.isBlank(individualId))
			return null;

		final String wanted = StringUtils.defaultString(role);
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			if(eventId.equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT))
					&& individualId.equals(EventParticipationReader.extractParticipantRef(participation,
						EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL))
					&& wanted.equals(StringUtils.defaultString(FLEFRecordHelper.getChildValue(participation,
						EventParticipationReader.TAG_ROLE))))
				return null;

		final FLEFRecord participation = FLEFRecord.createMainRecord(EventParticipationReader.TYPE, EventParticipationReader.ID_PREFIX,
				model)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
		if(!wanted.isEmpty())
			participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, wanted));
		participation.addChild(AuditStamp.now());
		model.addRecord(participation);
		return participation;
	}

}

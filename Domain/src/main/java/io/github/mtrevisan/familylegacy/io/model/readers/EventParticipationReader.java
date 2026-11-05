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


import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.util.Set;


/**
 * Handler for EVENT PARTICIPATION records.
 * <p>
 * Structure:
 * <pre>
 * // Associates an entity with an event and, when known, describes the role played by the entity within that event. All participation in events SHALL
 * // be represented through EVENT_PARTICIPATION records. This design allows the same event to have multiple participants (e.g., a marriage has two
 * // spouses) and the same entity to participate in multiple events (e.g., a person can be born, married, and buried).
 * // Implementations SHOULD validate participant types according to the EVENT type when such constraints are known.
 * record EventParticipationRecord {
 *   participant: EventParticipant   // may reference any record type capable of participating in an event, including individuals, groups, or future extensible entities
 *   event: Xref&lt;EventRecord&gt;        // the event in which the entity participated
 *   role?: enum {                   // the function of the participant in the event; roles never express kinship (see RelationshipRecord)
 *     principal,          // the participant the event is about (the person born, baptized, buried; each of the persons married or divorced)
 *     officiant,          // the person who performed or administered the event (priest, registrar, notary)
 *     witness,            // a person who witnessed the event
 *     informant,          // a person who reported the event (e.g. the declarant of a birth or death)
 *     sponsor,            // godparent or sponsor (baptism, confirmation)
 *     power_of_attorney, prisoner, executor, grantor, grantee, landlord, tenant, soldier, commander, victim, survivor, accused, judge,
 *     property            // the place that is the object of the event (participant MUST be a place)
 *   } | Text
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * EventParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 * }
 * </pre>
 */
public final class EventParticipationReader{

	public static final String TYPE = "EVENT_PARTICIPATION";
	public static final String ID_PREFIX = "EP";

	public static final String TAG_PARTICIPANT = "participant";
	public static final String TAG_EVENT = "event";
	public static final String TAG_ROLE = "role";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_CONFIDENCE = "confidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_ROLE_PRINCIPAL = "principal";
	public static final String ENUM_ROLE_OFFICIANT = "officiant";
	public static final String ENUM_ROLE_WITNESS = "witness";
	public static final String ENUM_ROLE_INFORMANT = "informant";
	public static final String ENUM_ROLE_SPONSOR = "sponsor";
	public static final String ENUM_ROLE_POWER_OF_ATTORNEY = "power_of_attorney";
	public static final String ENUM_ROLE_PRISONER = "prisoner";
	public static final String ENUM_ROLE_EXECUTOR = "executor";
	public static final String ENUM_ROLE_GRANTOR = "grantor";
	public static final String ENUM_ROLE_GRANTEE = "grantee";
	public static final String ENUM_ROLE_LANDLORD = "landlord";
	public static final String ENUM_ROLE_TENANT = "tenant";
	public static final String ENUM_ROLE_SOLDIER = "soldier";
	public static final String ENUM_ROLE_COMMANDER = "commander";
	public static final String ENUM_ROLE_VICTIM = "victim";
	public static final String ENUM_ROLE_SURVIVOR = "survivor";
	public static final String ENUM_ROLE_ACCUSED = "accused";
	public static final String ENUM_ROLE_JUDGE = "judge";
	public static final String ENUM_ROLE_PROPERTY = "property";
	public static final String[] ROLES = new String[]{
		ENUM_ROLE_PRINCIPAL,
		ENUM_ROLE_OFFICIANT,
		ENUM_ROLE_WITNESS,
		ENUM_ROLE_INFORMANT,
		ENUM_ROLE_SPONSOR,
		ENUM_ROLE_POWER_OF_ATTORNEY,
		ENUM_ROLE_PRISONER,
		ENUM_ROLE_EXECUTOR,
		ENUM_ROLE_GRANTOR, ENUM_ROLE_GRANTEE,
		ENUM_ROLE_LANDLORD, ENUM_ROLE_TENANT,
		ENUM_ROLE_SOLDIER, ENUM_ROLE_COMMANDER,
		ENUM_ROLE_VICTIM, ENUM_ROLE_SURVIVOR,
		ENUM_ROLE_ACCUSED, ENUM_ROLE_JUDGE,
		ENUM_ROLE_PROPERTY
	};

	// Roles carried by EventParticipationRecord that express a social link.
	public static final Set<String> RELATIONAL_EVENT_ROLES = Set.of(
		ENUM_ROLE_POWER_OF_ATTORNEY,
		ENUM_ROLE_WITNESS,
		ENUM_ROLE_OFFICIANT, ENUM_ROLE_INFORMANT,
		ENUM_ROLE_EXECUTOR,
		ENUM_ROLE_GRANTOR, ENUM_ROLE_GRANTEE,
		ENUM_ROLE_LANDLORD, ENUM_ROLE_TENANT,
		ENUM_ROLE_ACCUSED, ENUM_ROLE_JUDGE
	);

	public static final String[] INDIVIDUAL_OR_GROUP_ROLES = new String[]{
		ENUM_ROLE_PRINCIPAL, ENUM_ROLE_SPONSOR,
		ENUM_ROLE_POWER_OF_ATTORNEY,
		ENUM_ROLE_PRISONER,
		ENUM_ROLE_WITNESS,
		ENUM_ROLE_OFFICIANT, ENUM_ROLE_INFORMANT,
		ENUM_ROLE_EXECUTOR,
		ENUM_ROLE_GRANTOR, ENUM_ROLE_GRANTEE,
		ENUM_ROLE_LANDLORD, ENUM_ROLE_TENANT,
		ENUM_ROLE_SOLDIER, ENUM_ROLE_COMMANDER,
		ENUM_ROLE_VICTIM, ENUM_ROLE_SURVIVOR,
		ENUM_ROLE_ACCUSED, ENUM_ROLE_JUDGE
	};

	public static final String[] PLACE_ROLES = new String[]{
		ENUM_ROLE_PROPERTY
	};


	private EventParticipationReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}


	public static String extractParticipantRef(final FLEFRecord eventParticipation){
		final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation,
			EventParticipationReader.TAG_PARTICIPANT);
		if(participant == null)
			return null;

		final FLEFRecord ref = participant.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	public static String extractParticipantRef(final FLEFRecord eventParticipation, final String participantTag){
		final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation,
			EventParticipationReader.TAG_PARTICIPANT);
		if(participant == null)
			return null;

		final FLEFRecord ref = participant.getTheOnlyChild();
		return (ref != null && participantTag.equalsIgnoreCase(ref.getTag())? ref.getValue(): null);
	}

	public static String[] getValidRoles(final FLEFRecord participant){
		if(participant != null){
			if(IndividualReader.isOfType(participant) || GroupReader.isOfType(participant))
				return INDIVIDUAL_OR_GROUP_ROLES;

			if(PlaceReader.isOfType(participant))
				return PLACE_ROLES;
		}
		return ROLES;
	}

}

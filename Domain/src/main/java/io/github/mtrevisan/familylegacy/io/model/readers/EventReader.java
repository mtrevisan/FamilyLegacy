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


/**
 * Handler for EVENT records.
 * <p>
 * Structure:
 * <pre>
 * // Represents the occurrence of a historical action, ceremony, condition, transaction, legal process, or other happening. An EVENT describes what
 * // happened. Persistent associations resulting from an event SHOULD be represented separately using RELATIONSHIP records.
 * record EventRecord {
 *   type: enum {                    // The type of event. Custom types are permitted.
 *     // life
 *     birth, adoption, death, cremation, burial, coroner_report, illness, hospitalization, medical_procedure,
 *     // family
 *     engagement, marriage_bann, marriage_contract, marriage_license, marriage_settlement, marriage, separation, divorce_filed, divorce_decree, divorce, annulment,
 *     // achievements
 *     education, graduation, retirement, military_induction, military_muster_roll, military_service, military_award, military_release, military_discharge, military_resignation, military_retirement, prison, pardon, jury_duty, honor, bankruptcy,
 *     // national / government
 *     immigration, naturalization, emigration, deportation, internment, liberation, emancipation, relocation, census,
 *     // possessions and titles
 *     deed, escrow, chancery, will, probate, guardianship
 *   } | Text
 *   description?: Text              // Human-readable description of the event when a structured representation is insufficient.
 *   date?: DateStructure            // the date of the event
 *   place?: PlaceCitation           // the location of the event
 *   agency?: Text                   // Organization, institution, authority, corporation, or person responsible for the associated context (e.g., an employer, a church that administered rites, an archiving organization)
 *   cause?: struct {
 *     reason: Text   // used in special cases to record the reasons which precipitated an event, e.g. cause of death subordinate to a death event
 *     evidence?: EvidenceQualifiers
 *   }
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers   // evidence qualifiers for the event
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class EventReader{

	public static final String TYPE = "EVENT";
	public static final String ID_PREFIX = "E";

	public static final String TAG_TYPE = "type";
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_DATE = "date";
	public static final String TAG_PLACE = "place";
	public static final String TAG_CAUSE = "cause";
	public static final String TAG_REASON = "reason";
	public static final String TAG_CAUSE_REASON = FLEFRecordHelper.composePath(TAG_CAUSE, TAG_REASON);
	public static final String TAG_CAUSE_SOURCE = FLEFRecordHelper.composePath(TAG_CAUSE, "source");
	public static final String TAG_CAUSE_CONFIDENCE = FLEFRecordHelper.composePath(TAG_CAUSE, "evidence");
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_CONFIDENCE = "confidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_TYPE_BIRTH = "birth";
	public static final String ENUM_TYPE_DEATH = "death";
	public static final String ENUM_TYPE_CREMATION = "cremation";
	public static final String ENUM_TYPE_BURIAL = "burial";
	public static final String ENUM_TYPE_MARRIAGE_BANN = "marriage_bann";
	public static final String ENUM_TYPE_MARRIAGE_CONTRACT = "marriage_contract";
	public static final String ENUM_TYPE_MARRIAGE_LICENSE = "marriage_license";
	public static final String ENUM_TYPE_MARRIAGE_SETTLEMENT = "marriage_settlement";
	public static final String ENUM_TYPE_MARRIAGE = "marriage";
	public static final String ENUM_TYPE_SEPARATION = "separation";
	public static final String ENUM_TYPE_DIVORCE_FILED = "divorce_filed";
	public static final String ENUM_TYPE_DIVORCE_DECREE = "divorce_decree";
	public static final String ENUM_TYPE_DIVORCE = "divorce";
	public static final String ENUM_TYPE_ANNULMENT = "annulment";
	public static final String ENUM_TYPE_EMIGRATION = "emigration";
	public static final String ENUM_TYPE_IMMIGRATION = "immigration";
	public static final String ENUM_TYPE_ADOPTION = "adoption";
	public static final String ENUM_TYPE_CORONER_REPORT = "coroner_report";
	public static final String ENUM_TYPE_ILLNESS = "illness";
	public static final String ENUM_TYPE_HOSPITALIZATION = "hospitalization";
	public static final String ENUM_TYPE_MEDICAL_PROCEDURE = "medical_procedure";
	public static final String ENUM_TYPE_ENGAGEMENT = "engagement";
	public static final String ENUM_TYPE_EDUCATION = "education";
	public static final String ENUM_TYPE_GRADUATION = "graduation";
	public static final String ENUM_TYPE_RETIREMENT = "retirement";
	public static final String ENUM_TYPE_MILITARY_INDUCTION = "military_induction";
	public static final String ENUM_TYPE_MILITARY_MUSTER_ROLL = "military_muster_roll";
	public static final String ENUM_TYPE_MILITARY_SERVICE = "military_service";
	public static final String ENUM_TYPE_MILITARY_AWARD = "military_award";
	public static final String ENUM_TYPE_MILITARY_RELEASE = "military_release";
	public static final String ENUM_TYPE_MILITARY_DISCHARGE = "military_discharge";
	public static final String ENUM_TYPE_MILITARY_RESIGNATION = "military_resignation";
	public static final String ENUM_TYPE_MILITARY_RETIREMENT = "military_retirement";
	public static final String ENUM_TYPE_PRISON = "prison";
	public static final String ENUM_TYPE_PARDON = "pardon";
	public static final String ENUM_TYPE_JURY_DUTY = "jury_duty";
	public static final String ENUM_TYPE_HONOR = "honor";
	public static final String ENUM_TYPE_BANKRUPTCY = "bankruptcy";
	public static final String ENUM_TYPE_NATURALIZATION = "naturalization";
	public static final String ENUM_TYPE_DEPORTATION = "deportation";
	public static final String ENUM_TYPE_INTERNMENT = "internment";
	public static final String ENUM_TYPE_LIBERATION = "liberation";
	public static final String ENUM_TYPE_EMANCIPATION = "emancipation";
	public static final String ENUM_TYPE_RELOCATION = "relocation";
	public static final String ENUM_TYPE_CENSUS = "census";
	public static final String ENUM_TYPE_DEED = "deed";
	public static final String ENUM_TYPE_ESCROW = "escrow";
	public static final String ENUM_TYPE_CHANCERY = "chancery";
	public static final String ENUM_TYPE_WILL = "will";
	public static final String ENUM_TYPE_PROBATE = "probate";
	public static final String ENUM_TYPE_GUARDIANSHIP = "guardianship";
	/** Not in the protocol list (types of one's own), but frequent in the files that are imported: a story or a remembrance (that can come after the death), a baptism, a christening. */
	public static final String CUSTOM_TYPE_ANECDOTE = "anecdote";
	public static final String CUSTOM_TYPE_COMMEMORATION = "commemoration";
	public static final String CUSTOM_TYPE_BAPTISM = "baptism";
	public static final String CUSTOM_TYPE_CHRISTENING = "christening";
	/**
	 * Event types declared by the protocol, in a stable order.
	 * <p>
	 * Grouped by theme to help the UI present them in a readable way:
	 * life events, family events, achievements, national and government
	 * events, possessions and titles, religious and social events.
	 */
	public static final String[] TYPES = new String[]{
		// Life
		ENUM_TYPE_BIRTH, ENUM_TYPE_ADOPTION, ENUM_TYPE_DEATH, ENUM_TYPE_CREMATION, ENUM_TYPE_BURIAL,
		ENUM_TYPE_CORONER_REPORT, ENUM_TYPE_ILLNESS, ENUM_TYPE_HOSPITALIZATION, ENUM_TYPE_MEDICAL_PROCEDURE,
		// Family
		ENUM_TYPE_ENGAGEMENT,
		ENUM_TYPE_MARRIAGE_BANN, ENUM_TYPE_MARRIAGE_CONTRACT, ENUM_TYPE_MARRIAGE_LICENSE, ENUM_TYPE_MARRIAGE_SETTLEMENT,
		ENUM_TYPE_MARRIAGE, ENUM_TYPE_SEPARATION, ENUM_TYPE_DIVORCE_FILED, ENUM_TYPE_DIVORCE_DECREE,
		ENUM_TYPE_DIVORCE, ENUM_TYPE_ANNULMENT,
		// Achievements
		ENUM_TYPE_EDUCATION, ENUM_TYPE_GRADUATION, ENUM_TYPE_RETIREMENT,
		ENUM_TYPE_MILITARY_INDUCTION, ENUM_TYPE_MILITARY_MUSTER_ROLL, ENUM_TYPE_MILITARY_SERVICE,
		ENUM_TYPE_MILITARY_AWARD, ENUM_TYPE_MILITARY_RELEASE, ENUM_TYPE_MILITARY_DISCHARGE,
		ENUM_TYPE_MILITARY_RESIGNATION, ENUM_TYPE_MILITARY_RETIREMENT,
		ENUM_TYPE_PRISON, ENUM_TYPE_PARDON, ENUM_TYPE_JURY_DUTY, ENUM_TYPE_HONOR, ENUM_TYPE_BANKRUPTCY,
		// National / government
		ENUM_TYPE_EMIGRATION, ENUM_TYPE_IMMIGRATION, ENUM_TYPE_NATURALIZATION, ENUM_TYPE_DEPORTATION,
		ENUM_TYPE_INTERNMENT, ENUM_TYPE_LIBERATION, ENUM_TYPE_EMANCIPATION, ENUM_TYPE_RELOCATION, ENUM_TYPE_CENSUS,
		// Possessions and titles
		ENUM_TYPE_DEED, ENUM_TYPE_ESCROW, ENUM_TYPE_CHANCERY, ENUM_TYPE_WILL, ENUM_TYPE_PROBATE, ENUM_TYPE_GUARDIANSHIP
	};

	public static final String[] DIVORCE_TYPES = {
		ENUM_TYPE_SEPARATION, ENUM_TYPE_DIVORCE_FILED, ENUM_TYPE_DIVORCE_DECREE, ENUM_TYPE_DIVORCE, ENUM_TYPE_ANNULMENT
	};

	/** Core life cycle events. */
	public static final String[] CORE_LIFE_EVENTS = {
		ENUM_TYPE_BIRTH, ENUM_TYPE_DEATH, ENUM_TYPE_CREMATION, ENUM_TYPE_BURIAL,
		ENUM_TYPE_EMIGRATION, ENUM_TYPE_IMMIGRATION
	};


	private EventReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

	public static String extractDescription(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION);
	}

	public static boolean isTypeBirth(final String type){
		return ENUM_TYPE_BIRTH.equals(type);
	}

	public static boolean isTypeDeath(final String type){
		return ENUM_TYPE_DEATH.equals(type);
	}


	public static String extractCauseReason(final FLEFRecord event){
		return FLEFRecordHelper.getChildValue(event, TAG_CAUSE_REASON);
	}

}

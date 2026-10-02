package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


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
 *     engagement, marriage_bann, marriage_contract, marriage_license, marriage_settlement, marriage, divorce_filed, divorce_decree, divorce, annulment,
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

	public static final String TAG_TYPE = "type";
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_DATE = "date";
	public static final String TAG_PLACE = "place";
	public static final String TAG_AGENCY = "agency";
	private static final String TAG_CAUSE = "cause";
	public static final String TAG_CAUSE_REASON = FLEFRecordHelper.composePath(TAG_CAUSE, "reason");
	public static final String TAG_CAUSE_EVIDENCE = FLEFRecordHelper.composePath(TAG_CAUSE, "evidence");
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	private static final String ENUM_TYPE_BIRTH = "birth";
	private static final String ENUM_TYPE_DEATH = "death";
	/**
	 * Event types declared by the protocol, in a stable order.
	 * <p>
	 * Grouped by theme to help the UI present them in a readable way:
	 * life events, family events, achievements, national and government
	 * events, possessions and titles, religious and social events.
	 */
	public static final String[] TYPES = new String[]{
		// Life
		ENUM_TYPE_BIRTH, "adoption", ENUM_TYPE_DEATH, "cremation", "burial",
		"coroner_report", "illness", "hospitalization", "medical_procedure",
		// Family
		"engagement", "marriage_bann", "marriage_contract", "marriage_license",
		"marriage_settlement", "marriage", "divorce_filed", "divorce_decree",
		"divorce", "annulment",
		// Achievements
		"education", "graduation", "retirement",
		"military_induction", "military_muster_roll", "military_service",
		"military_award", "military_release", "military_discharge",
		"military_resignation", "military_retirement",
		"prison", "pardon", "jury_duty", "honor", "bankruptcy",
		// National / government
		"immigration", "naturalization", "emigration", "deportation",
		"internment", "liberation", "emancipation", "relocation", "census",
		// Possessions and titles
		"deed", "escrow", "chancery", "will", "probate", "guardianship"
	};


	private EventReader(){}


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

package io.github.mtrevisan.familylegacy.v2.io.model.readers;


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
 *   role?: enum {                   // optional because the role may be unknown or not applicable
 *     child, parent, spouse, power_of_attorney, prisoner, witness, officiant, informant, executor, grantor, grantee,
 *     landlord, tenant, soldier, commander, victim, survivor, accused, judge
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

	public static final String TAG_PARTICIPANT = "participant";
	public static final String TAG_EVENT = "event";
	public static final String TAG_ROLE = "role";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] ROLES = new String[]{
		"child", "parent", "spouse",
		"power_of_attorney",
		"prisoner",
		"witness",
		"officiant", "informant",
		"executor",
		"grantor", "grantee",
		"landlord", "tenant",
		"soldier", "commander",
		"victim", "survivor",
		"accused", "judge"
	};


	private EventParticipationReader(){}

}

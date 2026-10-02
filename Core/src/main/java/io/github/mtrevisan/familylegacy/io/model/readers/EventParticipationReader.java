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

	// Roles carried by EventParticipationRecord that express a social link.
	public static final Set<String> RELATIONAL_EVENT_ROLES = Set.of(
		"power_of_attorney",
		"witness",
		"officiant", "informant",
		"executor",
		"grantor", "grantee",
		"landlord", "tenant",
		"accused", "judge"
	);


	private EventParticipationReader(){}

}

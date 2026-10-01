package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for IDENTITY HYPOTHESIS records.
 * <p>
 * Structure:
 * <pre>
 * // Represents the possibility that two records describe the same real-world entity. The hypothesis itself carries no acceptance
 * // or rejection status. Evaluation of the hypothesis is expressed through one or more ConclusionRecord instances.
 * // SUBJECT and CANDIDATE MUST reference different records.
 * // The record expresses a possibility, not a probability assessment. The presence of an identity hypothesis does not imply that the entities
 * // are likely to be identical.
 * // Multiple conclusions MAY evaluate the same identity hypothesis over time.
 * record IdentityHypothesisRecord {
 *   identity+: IdentityCandidate
 *   comment?: Text   // a note explaining the basis for the possible duplicate hypothesis
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 *
 *   require count(identity) == 2
 *   require identity[0] != identity[1]
 *   require type(identity[0]) == type(identity[1])
 * }
 *
 * IdentityCandidate = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 * }
 * </pre>
 */
public final class IdentityHypothesisReader{

	public static final String TAG_IDENTITY = "identity";
	public static final String TAG_COMMENT = "comment";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_AUDIT = "audit";


	private IdentityHypothesisReader(){}


	public static String extractComment(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_COMMENT);
	}

}

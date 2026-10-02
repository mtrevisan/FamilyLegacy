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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for CONCLUSION records.
 * <p>
 * Structure:
 * <pre>
 * // A formal statement that resolves a conflict between conflicting evidence by selecting the hypothesis deemed most truthful based on a critical
 * // analysis of the sources.
 * // Conclusions are research outcomes rather than source assertions. A conclusion records the researcher’s evaluation of competing
 * // evidence and is separate from the underlying records it analyzes.
 * // A conclusion does not modify, invalidate, or delete the records it evaluates. All competing assertions remain preserved so that the reasoning process
 * // remains transparent and auditable.
 * // Conclusions never replace evidence. The protocol preserves source assertions, competing hypotheses,
 * // and researcher conclusions simultaneously.
 * record ConclusionRecord {
 *   issue: Text                               // the specific issue that is resolved, e.g. "birth_date", "marriage_place", "parentage", "death_cause", "relationship_type"
 *   proof_status: enum {
 *     conflicting_evidence,   // the evidence is conflicting, none prevails
 *     supported,              // preponderance of evidence, but not absolute certainty
 *     proven,                 // proven according to the Genealogical Proof Standard
 *     disproven               // proven false
 *   }
 *   narrative?: Text                          // a discursive text that explains the logical reasoning, the sources evaluated and the reason why this conclusion was chosen
 *   resolves*: ConclusionTarget               // a pointer to one or more conflicting assertions or hypotheses. When PROOF_STATUS is DISPROVEN, RESOLVES SHOULD normally contain at least two targets.
 *   preferred?: ConclusionTarget              // the assertion or hypothesis judged most likely to be correct. If the conclusion is purely negative (e.g., "none of the hypotheses are valid"), this field can be omitted. When present, PREFERRED MUST be one of the records listed in RESOLVES. When PROOF_STATUS is DISPROVEN, PREFERRED SHOULD normally be omitted.
 *   research*: Xref&lt;ResearchQuestionRecord&gt;   // references to research questions that led to this conclusion
 *   source*: SourceCitation                   // any sources that support the conclusion itself (e.g., a methodology treatise, or a secondary source that has already resolved the conflict)
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 *
 *   require preferred member_of resolves
 * }
 *
 * ConclusionTarget = oneof {
 *   event: Xref&lt;EventRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual: Xref&lt;IndividualRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   source: Xref&lt;SourceRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 * }
 * </pre>
 */
public final class ConclusionReader{

	public static final String TAG_ISSUE = "issue";
	public static final String TAG_PROOF_STATUS = "proof_status";
	public static final String TAG_NARRATIVE = "narrative";
	public static final String TAG_RESOLVES = "resolves";
	public static final String TAG_PREFERRED = "preferred";
	public static final String TAG_RESEARCH = "research";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] PROOF_STATUSES = new String[]{
		"unresearched",
		"conflicting_evidence",
		"supported",
		"proven",
		"disproven"
	};


	private ConclusionReader(){}


	public static String extractIssues(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_ISSUE);
	}

	public static String extractProofStatus(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS);
	}

	public static String extractNarrative(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE);
	}

}

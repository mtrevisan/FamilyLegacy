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
 *   rationale?: Text
 *   source*: SourceCitation
 *   confidence?: Confidence
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
 *   source: Xref&lt;SourceRecord&gt;
 *   repository: Xref&lt;RepositoryRecord&gt;
 *   media: Xref&lt;MediaRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 * }
 * </pre>
 */
public final class IdentityHypothesisReader{

	public static final String TYPE = "IDENTITY_HYPOTHESIS";
	public static final String ID_PREFIX = "IH";

	public static final String TAG_IDENTITY = "identity";
	public static final String TAG_RATIONALE = "rationale";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_CONFIDENCE = "confidence";
	public static final String TAG_AUDIT = "audit";


	private IdentityHypothesisReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



	public static String extractRationale(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_RATIONALE);
	}

}

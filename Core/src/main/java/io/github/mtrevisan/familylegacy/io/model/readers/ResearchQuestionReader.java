package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for RESEARCH QUESTION records.
 * <p>
 * Structure:
 * <pre>
 * // Represents a research objective, question, hypothesis, or problem that requires investigation. Multiple research activities may contribute to
 * // answering the same question.
 * //
 * // ResearchQuestionRecord.conclusion provides a convenient summary of the current answer to a research question.
 * // ConclusionRecord represents a formal analytical decision that evaluates one or more competing assertions, hypotheses, or records.
 * // A research question MAY have a summary conclusion without any ConclusionRecord.
 * // Conversely, one or more ConclusionRecord instances MAY contribute to the conclusion of a research question.
 * record ResearchQuestionRecord {
 *   title: Text               // short human-readable summary of the research objective
 *   question: Text            // the actual research question or hypothesis being investigated
 *   target*: ResearchTarget   // people, families, events, places, or records involved in the question
 *   status: enum {
 *     open,       // investigation is ongoing
 *     on_hold,    // temporarily suspended
 *     resolved,   // sufficient evidence has been gathered
 *     disproven   // the underlying hypothesis was shown to be incorrect
 *   }
 *   conclusion?: Text         // Current answer or working conclusion for this research question. This field is intended as a concise human-readable summary of the researcher's current assessment. Unlike ConclusionRecord, this field does not formally resolve conflicting evidence and does not provide structured proof analysis. It serves as an executive summary of the present state of research.
 *   conclusion_confidence?: enum { low, medium, high } // Indicates the researcher's confidence in the current conclusion. This value reflects the current state of investigation and is intended for planning, prioritization, reporting, and user interfaces. It does not replace formal proof evaluation expressed through ConclusionRecord. Suggested interpretation are low: limited evidence, unresolved conflicts, or speculative conclusion; medium: evidence generally supports the conclusion, but gaps or uncertainties remain; high: evidence strongly supports the conclusion and no significant unresolved conflicts are currently known
 *   rationale?: Text          // explanation supporting the conclusion
 *   closed_date?: Date        // date the question was resolved or disproven
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * ResearchTarget = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   event: Xref&lt;EventRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   source: Xref&lt;SourceRecord&gt;
 *   document: Xref&lt;DocumentRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 *   void: struct {}
 * }
 * </pre>
 */
public final class ResearchQuestionReader{

	public static final String TAG_TITLE = "title";
	public static final String TAG_QUESTION = "question";
	public static final String TAG_TARGET = "target";
	public static final String TAG_STATUS = "status";
	public static final String TAG_CONCLUSION = "conclusion";
	public static final String TAG_CONCLUSION_CONFIDENCE = "conclusion_confidence";
	public static final String TAG_RATIONALE = "rationale";
	public static final String TAG_CLOSED_DATE = "closed_date";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_STATUS_OPEN = "open";
	public static final String ENUM_STATUS_ON_HOLD = "on_hold";
	public static final String ENUM_STATUS_RESOLVED = "resolved";
	public static final String ENUM_STATUS_DISPROVEN = "disproven";
	public static final String[] STATUSES = new String[]{
		ENUM_STATUS_OPEN,
		ENUM_STATUS_ON_HOLD,
		ENUM_STATUS_RESOLVED,
		ENUM_STATUS_DISPROVEN
	};

	public static final String[] CONFIDENCES = new String[]{
		"low", "medium", "high"
	};


	private ResearchQuestionReader(){}


	public static String extractTitle(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TITLE);
	}

	public static String extractQuestion(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_QUESTION);
	}

	public static String extractStatus(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_STATUS);
	}

	public static String extractConclusionConfidence(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_CONCLUSION_CONFIDENCE);
	}

}

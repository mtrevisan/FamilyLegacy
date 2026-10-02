package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for CONTEXT IMPACT records.
 * <p>
 * Structure:
 * <pre>
 * // Links a contextual factor to a genealogical entity, assertion, or research outcome that it influences, explains, constrains, motivates, or causes.
 * // Contextual factors may include cultural norms, historical events, legal frameworks, social customs, environmental conditions, or other external influences represented
 * // through CONTEXT.
 * // This record is intended to document interpretative relationships rather than direct evidence of participation or ownership. It explicitly captures how broader context
 * // helps explain, influence, or justify genealogical conclusions and assertions.
 * //
 * // Recommended usage:
 * //  - Historic events commonly EXPLAIN, INFLUENCE, MOTIVATE or CAUSE.
 * //  - Cultural norms commonly EXPLAIN, INFLUENCE or CONSTRAIN.
 * // Implementations SHOULD warn when a combination appears semantically unusual.
 * // Implementations SHOULD index ContextImpactRecord by both CONTEXT and TARGET.
 * record ContextImpactRecord {
 *   context: ContextSource    // The contextual factor exerting the influence.
 *   target: ImpactTarget      // The entity, assertion, attribute, relationship, or conclusion affected by the context.
 *   impact_type?: enum {
 *     explains,     // Provides explanatory context for the target.
 *     influences,   // Exerts a general influence on the target.
 *     constrains,   // Limits or restricts what was possible or likely.
 *     motivates,    // Encourages, promotes, or makes the target more likely.
 *     causes        // Directly contributes to the occurrence or existence of the target.
 *   } | Text
 *   rationale?: Text          // Explanation of how and why the contextual factor influences, explains, constrains, motivates, or causes the target.
 *   confidence?: enum {
 *      low,
 *      medium,
 *      high
 *   }
 *   source*: SourceCitation   // Sources supporting the existence or interpretation of this contextual relationship.
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 *
 * // A contextual factor that may influence or explain genealogical information.
 * // Context sources are not themselves evidence that a target participated in an event or followed a practice; they provide historical, cultural, legal, social,
 * // environmental, or interpretative context.
 * ContextSource = oneof {
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 * }
 *
 * ImpactTarget = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   event: Xref&lt;EventRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   conclusion: Xref&lt;ConclusionRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 * }
 * </pre>
 */
public final class ContextImpactReader{

	public static final String TAG_CONTEXT = "context";
	public static final String TAG_TARGET = "target";
	public static final String TAG_IMPACT_TYPE = "impact_type";
	public static final String TAG_RATIONALE = "rationale";
	public static final String TAG_CONFIDENCE = "confidence";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_AUDIT = "audit";

	public static final String[] IMPACT_TYPES = new String[]{
		"explains",
		"influences",
		"constrains",
		"motivates",
		"causes"
	};
	public static final String[] CONFIDENCES = new String[]{
		"low", "medium", "high"
	};


	private ContextImpactReader(){}


	public static String extractImpactType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_IMPACT_TYPE);
	}

//	public static String extractType(final FLEFRecord record){
//		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
//	}
//
//	public static String extractName(final FLEFRecord record){
//		return FLEFRecordHelper.getChildValue(record, TAG_NAME);
//	}

}

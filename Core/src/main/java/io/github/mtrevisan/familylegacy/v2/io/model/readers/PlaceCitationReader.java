package io.github.mtrevisan.familylegacy.v2.io.model.readers;


/**
 * Handler for PLACE CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Reference to a PLACE record together with location-specific citations and evidence qualifiers.
 * // Implementations SHOULD prevent cyclic expansion of citation metadata (PlaceCitation > SourceCitation > SourceRecord > PlaceCitation).
 * struct PlaceCitation {
 *   place: Xref&lt;PlaceRecord&gt;
 *   original_text?: Text   // The place name exactly as recorded in the source. This value MAY coexist with PLACE when the source wording is preserved alongside a normalized place identification.
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 */
public final class PlaceCitationReader{

	public static final String TAG_PLACE = "place";
	public static final String TAG_ORIGINAL_TEXT = "original_text";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";


	private PlaceCitationReader(){}

}

package io.github.mtrevisan.familylegacy.v2.io.model.readers;


/**
 * Handler for GROUP ATTRIBUTE records.
 * <p>
 * Structure:
 * <pre>
 * // A characteristic, condition, status, classification, or descriptive property attributed to a group. Attributes may be directly recorded in
 * // sources, derived from evidence, inferred from research, or reported through oral tradition.
 * // Group attributes describe the group itself as a collective entity rather than the individual members who compose it. Events that occur at a specific
 * // point in time (such as founding, migration, merger, or dissolution) SHOULD be represented using EVENT records instead.
 * record GroupAttributeRecord {
 *   group: Xref&lt;GroupRecord&gt;        // The group to which this attribute applies.
 *   type: enum {                    // The type of attribute being asserted.
 *     residence,              // Place where the group is known to reside or be established.
 *     member_count,           // Reported number of group members. This value represents a source assertion and may differ from the number of members explicitly represented in the file.
 *     children_count,         // Reported number of children associated with the group. This value represents a source assertion and may differ from the number of child members explicitly represented in the file
 *     social_class,           // Economic or social classification of the group (e.g. "peasant", "middle_class", "nobility").
 *     ethnicity,              // Ethnic, tribal, cultural, or ancestral identity associated with the group as a whole.
 *     religion,               // Religion practiced or associated with the group.
 *     language,               // Language primarily spoken or associated with the group.
 *     wealth,                 // Description of the group's wealth, economic standing, or reported financial condition.
 *     land_holding,           // Description or quantity of land owned, occupied, or controlled by the group.
 *     primary_income_source   // Principal economic activity or source of income associated with the group.
 *   } | Text
 *   value?: Text                    // The value of the attribute.
 *   valid_from?: DateStructure      // The date from which this attribute is known or believed to have been valid.
 *   valid_to?: DateStructure        // The date until which this attribute is known or believed to have been valid.
 *   place?: PlaceCitation           // Place specifically associated with this attribute, when different from the group's general residence.
 *   source*: SourceCitation         // Sources supporting this attribute.
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers   // Evidentiary assessment of this assertion.
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class GroupAttributeReader{

	public static final String TAG_GROUP = "group";
	public static final String TAG_TYPE = "type";
	public static final String TAG_VALUE = "value";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";
	public static final String TAG_PLACE = "place";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] TYPES = new String[]{
		"residence", "member_count", "children_count", "social_class", "ethnicity", "religion", "language",
		"wealth", "land_holding", "primary_income_source"
	};


	private GroupAttributeReader(){}


}

package io.github.mtrevisan.familylegacy.io.model.readers;


/**
 * Handler for INDIVIDUAL ATTRIBUTE records.
 * <p>
 * Structure:
 * <pre>
 * // A characteristic, condition, status, classification, or descriptive property attributed to an individual. Attributes may be directly recorded
 * // in sources, derived from evidence, inferred from research, or reported through oral tradition.
 * // Individual attributes describe enduring or observable characteristics of a person rather than discrete events. Events such as birth,
 * // marriage, military service, education, immigration, or death SHOULD normally be represented using EVENT records.
 * record IndividualAttributeRecord {
 *   individual: Xref&lt;IndividualRecord&gt;   // The individual to whom this attribute applies.
 *   type: enum {                         // The type of attribute being asserted.
 *     characteristic,    // Physical, behavioral, medical, or descriptive characteristic (e.g "blue eyes", "blind", "left-handed", "scar on forehead").
 *     residence,         // Place where the individual is known to have lived, resided, or maintained a household.
 *     occupation,        // Occupation, profession, trade, office, or vocation (e.g. "farmer", "blacksmith", "teacher").
 *     possession,        // Property, assets, land, livestock, tools, money, or other possessions associated with the individual.
 *     military_rank,     // Military rank or grade held by the individual (e.g. "captain", "sergeant", "colonel").
 *     caste,             // Social, hereditary, religious, or legal caste classification associated with the individual.
 *     social_class,      // Social, economic, or societal classification associated with the individual (e.g. "nobility", "bourgeoisie", "peasant").
 *     ethnicity,         // Ethnic, tribal, cultural, or ancestral identity.
 *     citizenship,       // Citizenship, nationality, or legal political allegiance.
 *     nationality,       // National identity, ethnic-national affiliation, or nationality associated with the individual. Unlike CITIZENSHIP, nationality may reflect cultural or historical identity rather than legal status.
 *     ssn,               // Government-issued personal identification number (e.g. Social Security Number, Tax Code, National ID).
 *     title,             // Noble, honorific, professional, hereditary, religious, or administrative title associated with the individual (e.g. "Count", "Doctor", "Bishop", "Sir").
 *     children_count,    // Reported number of children associated with the individual.
 *     marriages_count,   // Reported number of marriages associated with the individual.
 *     religion,          // Religion, denomination, faith, or spiritual affiliation.
 *     language,          // Language spoken, written, understood, or otherwise associated with the individual.
 *     literacy,          // Ability to read and/or write (e.g. "illiterate", "literate", "signature only").
 *     education          // Educational attainment, schooling level, degree, qualification, or academic status (e.g. "elementary", "secondary", "university", "doctorate").
 *   } | Text
 *   value?: Text                         // The value of the attribute.
 *   valid_from?: DateStructure           // The date from which this attribute is known or believed to have been valid.
 *   valid_to?: DateStructure             // The date until which this attribute is known or believed to have been valid.
 *   place?: PlaceCitation                // Place specifically associated with this attribute.
 *   source*: SourceCitation              // Sources supporting this attribute.
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers        // Evidentiary assessment of this assertion.
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class IndividualAttributeReader{

	public static final String TAG_INDIVIDUAL = "individual";
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
		"characteristic", "residence", "occupation", "possession", "military_rank", "caste", "social_class",
		"ethnicity", "citizenship", "nationality", "ssn", "title", "children_count", "marriages_count",
		"religion", "language", "literacy", "education"
	};


	private IndividualAttributeReader(){}


}

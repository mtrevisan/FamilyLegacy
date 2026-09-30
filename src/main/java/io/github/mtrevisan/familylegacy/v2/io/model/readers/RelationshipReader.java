package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.NameAnatomyService;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.NamePart;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;


/**
 * Handler for RELATIONSHIP records.
 * <p>
 * Structure:
 * <pre>
 * // Represents an enduring association between genealogical entities. A RELATIONSHIP describes a state, connection, or affiliation that may exist
 * // before, during, or after one or more events. Events that create, modify, recognize, or terminate a relationship SHOULD be represented separately
 * // using EVENT records. This separation allows relationships to exist independently of events (e.g., a biological parent-child relationship
 * // exists regardless of whether a birth event is documented).
 * // Implementations SHOULD validate the allowed SUBJECT and TARGET types according to the relationship TYPE.
 * record RelationshipRecord {
 *   subject: RelationshipParticipant   // The entity for which the relationship is being asserted. The SUBJECT is always the entity whose role is described by TYPE and ROLE relative to the TARGET. Example: `biological_child(Alice -> John)` means that Alice is the biological child of John.
 *   object: RelationshipParticipant    // the entity to which the subject is related to
 *   type: enum {
 *     biological_child,     // (Individual -> Individual) relationship exists through birth
 *     adoptive_child,       // (Individual -> Individual) relationship created through legal adoption
 *     foster_child,         // (Individual -> Individual) temporary foster-care relationship
 *     guarded_child,        // (Individual -> Individual) court-appointed legal guardianship relationship
 *     step_child,           // (Individual -> Individual) relationship created through marriage and not through descent
 *     civil_spouse,         // (Individual -> Individual) marriage recognized by civil authority
 *     religious_spouse,     // (Individual -> Individual) marriage recognized by religious authority
 *     customary_spouse,     // (Individual -> Individual) marriage recognized by local custom or tradition
 *     cohabiting_partner,   // (Individual -> Individual) non-marital domestic partnership
 *     engaged_partner,      // (Individual -> Individual) promise or agreement to marry
 *     group_member,         // (Individual -> Group) membership of an individual in a group
 *     associate             // (Individual -> Individual | Individual -> Group | Group -> Group) general association
 *     part_of               // (Group -> Group) membership of a group within a larger group
 *   }
 *   role?: Text                        // the function, position, capacity or responsibility held by the SUBJECT relative to the TARGET. This field provides additional contextual, organizational, legal, social, or cultural interpretation that is not fully expressed by TYPE alone. ROLE supplements TYPE and SHOULD NOT contradict it. Implementations SHOULD verify that ROLE is semantically compatible with TYPE (ex. biological_child/adoptive_child/foster_child/guarded_child/step_child/civil_spouse/religious_spouse/customary_spouse/cohabiting_partner/engaged_partner/part_of -> ROLE is normally unnecessary and SHOULD be omitted, group_member -> examples include member, president, secretary, treasurer, resident, head_of_household, tribal_leader, elder, custodian, ..., associate -> any role MAY be supplied because the relationship itself is generic).
 *   status?: enum {
 *     active,   // relationship currently exists
 *     ended,    // relationship no longer exists
 *     unknown   // relationship status is not known
 *   }
 *   valid_from?: DateStructure         // the date from which this relationship was known to be in effect
 *   valid_to?: DateStructure           // the date until which this relationship was known to be in effect
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * RelationshipParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 * }
 * </pre>
 */
public final class RelationshipReader{

	public static final String TAG_SUBJECT = "SUBJECT";
	public static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_TYPE = "TYPE";
	public static final String TAG_ROLE = "ROLE";
	public static final String TAG_STATUS = "STATUS";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_CIVIL_SPOUSE = "civil_spouse";
	private static final String ENUM_TYPE_RELIGIOUS_SPOUSE = "religious_spouse";
	private static final String ENUM_TYPE_CUSTOMARY_SPOUSE = "customary_spouse";
	private static final String ENUM_TYPE_COHABITING_PARTNER = "cohabiting_partner";
	private static final String ENUM_TYPE_ENGAGED_PARTNER = "engaged_partner";
	private static final String ENUM_TYPE_GROUP_MEMBER = "group_member";
	private static final String ENUM_TYPE_ASSOCIATE = "associate";
	private static final String ENUM_TYPE_PART_OF = "part_of";
	private static final String ENUM_TYPE_ENDS_WITH_CHILD = "_child";
	private static final String ENUM_TYPE_ENDS_WITH_SPOUSE = "_spouse";
	private static final String ENUM_TYPE_ENDS_WITH_PARTNER = "_partner";
	private static final String[] TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD,
		ENUM_TYPE_STEP_CHILD, ENUM_TYPE_CIVIL_SPOUSE, ENUM_TYPE_RELIGIOUS_SPOUSE, ENUM_TYPE_CUSTOMARY_SPOUSE,
		ENUM_TYPE_COHABITING_PARTNER, ENUM_TYPE_ENGAGED_PARTNER, ENUM_TYPE_GROUP_MEMBER, ENUM_TYPE_ASSOCIATE,
		ENUM_TYPE_PART_OF
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD,
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	private static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{"group_member", "associate"};
	private static final String[] GROUP_TO_GROUP_TYPES = new String[]{"part_of", "associate"};
	private static final String[] GROUP_TO_INDIVIDUAL_TYPES = new String[0];
	private static final String[] EMPTY_TYPES = new String[0];
	private static final String[] BIOLOGICAL = new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
	private static final String[] FAMILY = new String[]{ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD,
		ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD};

	public static final String[] STATUSES = new String[]{
		"active", "ended", "unknown"
	};

	private static final Set<String> CHILD_TYPES = new HashSet<>(List.of(ENUM_TYPE_BIOLOGICAL_CHILD,
		ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD));
	private static final Set<String> PARTNER_TYPES = new HashSet<>(List.of(ENUM_TYPE_CIVIL_SPOUSE,
		ENUM_TYPE_RELIGIOUS_SPOUSE, ENUM_TYPE_CUSTOMARY_SPOUSE, ENUM_TYPE_COHABITING_PARTNER, ENUM_TYPE_ENGAGED_PARTNER));


	private RelationshipReader(){}


	public static boolean isTypeBiologicalChild(final String type){
		return ENUM_TYPE_BIOLOGICAL_CHILD.equals(type);
	}

	public static boolean isTypeGroupMember(final String type){
		return ENUM_TYPE_GROUP_MEMBER.equals(type);
	}

	public static boolean isTypePartOf(final String type){
		return ENUM_TYPE_PART_OF.equals(type);
	}

	public static boolean isTypeAssociate(final String type){
		return ENUM_TYPE_ASSOCIATE.equals(type);
	}

	public static boolean isTypeChild(final String type){
		return CHILD_TYPES.contains(type);
	}

	public static boolean isTypePartner(final String type){
		return PARTNER_TYPES.contains(type);
	}


	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

}

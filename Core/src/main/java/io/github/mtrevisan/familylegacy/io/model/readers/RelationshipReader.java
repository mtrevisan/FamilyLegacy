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
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


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

	public static final String TAG_SUBJECT = "subject";
	public static final String TAG_OBJECT = "object";
	public static final String TAG_TYPE = "type";
	public static final String TAG_ROLE = "role";
	public static final String TAG_STATUS = "status";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	public static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_CIVIL_SPOUSE = "civil_spouse";
	private static final String ENUM_TYPE_RELIGIOUS_SPOUSE = "religious_spouse";
	private static final String ENUM_TYPE_CUSTOMARY_SPOUSE = "customary_spouse";
	private static final String ENUM_TYPE_COHABITING_PARTNER = "cohabiting_partner";
	private static final String ENUM_TYPE_ENGAGED_PARTNER = "engaged_partner";
	public static final String ENUM_TYPE_GROUP_MEMBER = "group_member";
	private static final String ENUM_TYPE_ASSOCIATE = "associate";
	private static final String ENUM_TYPE_PART_OF = "part_of";
	public static final String[] TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD,
		ENUM_TYPE_STEP_CHILD, ENUM_TYPE_CIVIL_SPOUSE, ENUM_TYPE_RELIGIOUS_SPOUSE, ENUM_TYPE_CUSTOMARY_SPOUSE,
		ENUM_TYPE_COHABITING_PARTNER, ENUM_TYPE_ENGAGED_PARTNER, ENUM_TYPE_GROUP_MEMBER, ENUM_TYPE_ASSOCIATE,
		ENUM_TYPE_PART_OF
	};
	public static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD,
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	public static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	// Relationship types considered social
	public static final Set<String> SOCIAL_RELATIONSHIP_TYPES = Set.of(
		"associate", "group_member", "part_of"
	);
	public static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{"group_member", "associate"};
	public static final String[] GROUP_TO_GROUP_TYPES = new String[]{"part_of", "associate"};
	private static final String[] GROUP_TO_INDIVIDUAL_TYPES = new String[0];
	private static final String[] EMPTY_TYPES = new String[0];
	public static final Set<String> CHILD_TYPES = new HashSet<>(List.of(ENUM_TYPE_BIOLOGICAL_CHILD,
		ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD));
	public static final String[] BIOLOGICAL = new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
	public static final String[] FAMILY = CHILD_TYPES.toArray(String[]::new);
	public static final Set<String> PARTNER_TYPES = new TreeSet<>(List.of(
		ENUM_TYPE_CIVIL_SPOUSE, ENUM_TYPE_RELIGIOUS_SPOUSE, ENUM_TYPE_CUSTOMARY_SPOUSE, ENUM_TYPE_COHABITING_PARTNER,
		ENUM_TYPE_ENGAGED_PARTNER));

	public static final String[] STATUSES = new String[]{
		"active", "ended", "unknown"
	};


	private RelationshipReader(){}


	public static boolean isTypeBiologicalChild(final String type){
		return ENUM_TYPE_BIOLOGICAL_CHILD.equals(type);
	}

	public static boolean isTypeAdoptiveChild(final String type){
		return ENUM_TYPE_ADOPTIVE_CHILD.equals(type);
	}

	public static boolean isTypeStepChild(final String type){
		return ENUM_TYPE_STEP_CHILD.equals(type);
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

	public static String extractRole(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_ROLE);
	}


	public static String[] getValidTypes(final String subjectType, final String objectType){
		if(subjectType == null || objectType == null)
			return EMPTY_TYPES;

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(IndividualHandler.TYPE, objectType))
			return INDIVIDUAL_TO_INDIVIDUAL_TYPES;

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return INDIVIDUAL_TO_GROUP_TYPES;

		if(Strings.CI.equals(GroupHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return GROUP_TO_GROUP_TYPES;

		return GROUP_TO_INDIVIDUAL_TYPES;
	}

}

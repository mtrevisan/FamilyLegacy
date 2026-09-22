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
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Shared helpers for the tools in the {@code individuals} package.
 * <p>
 * The class centralizes three concerns that would otherwise be
 * duplicated across every tool:
 * <ul>
 *   <li>reading individual fields (name, sex, birth/death);</li>
 *   <li>creating, re-pointing, and deleting relationships;</li>
 *   <li>computing the set of relationships that involve an individual,
 *       so that a delete or a merge can clean up before mutating the
 *       model.</li>
 * </ul>
 */
public final class IndividualHelper{

	public static final String TYPE_INDIVIDUAL = "individual";
	public static final String TYPE_RELATIONSHIP = "relationship";

	public static final String TAG_TYPE = "type";
	public static final String TAG_SUBJECT = "subject";
	public static final String TAG_TARGET = "target";
	public static final String TAG_NAME = "name";
	public static final String TAG_VALUE = "value";
	public static final String TAG_SEX = "sex";

	public static final String REL_BIOLOGICAL_CHILD = "biological_child";
	public static final String REL_ADOPTIVE_CHILD = "adoptive_child";
	public static final String REL_FOSTER_CHILD = "foster_child";
	public static final String REL_GUARDED_CHILD = "guarded_child";
	public static final String REL_STEP_CHILD = "step_child";
	public static final String REL_CIVIL_SPOUSE = "civil_spouse";
	public static final String REL_RELIGIOUS_SPOUSE = "religious_spouse";
	public static final String REL_CUSTOMARY_SPOUSE = "customary_spouse";
	public static final String REL_COHABITING = "cohabiting_partner";
	public static final String REL_ENGAGED = "engaged_partner";

	public static final String SEX_MALE = "male";
	public static final String SEX_FEMALE = "female";

	public static final List<String> CHILD_RELATION_TYPES = List.of(
		REL_BIOLOGICAL_CHILD, REL_ADOPTIVE_CHILD, REL_FOSTER_CHILD,
		REL_GUARDED_CHILD, REL_STEP_CHILD);

	public static final List<String> SPOUSE_RELATION_TYPES = List.of(
		REL_CIVIL_SPOUSE, REL_RELIGIOUS_SPOUSE, REL_CUSTOMARY_SPOUSE,
		REL_COHABITING, REL_ENGAGED);


	private IndividualHelper(){
	}


	public static List<FLEFRecord> listAllIndividuals(final FLEFModel model){
		return model.getRecordsByType(IndividualHandler.TYPE);
	}

	public static Map<String, FLEFRecord> indexIndividualsById(final FLEFModel model){
		final Map<String, FLEFRecord> result = new LinkedHashMap<>();
		for(final FLEFRecord individual : listAllIndividuals(model))
			if(individual.getId() != null)
				result.put(individual.getId(), individual);
		return result;
	}

	/**
	 * Returns the primary display name of an individual, or the id when
	 * no name is available.
	 */
	public static String displayName(final FLEFRecord individual){
		if(individual == null)
			return StringUtils.EMPTY;
		for(final FLEFRecord nameBlock : individual.getChildren()){
			if(!TAG_NAME.equalsIgnoreCase(nameBlock.getTag()))
				continue;
			final StringBuilder fullName = new StringBuilder();
			for(final FLEFRecord part : nameBlock.getChildren()){
				if(!"part".equalsIgnoreCase(part.getTag()))
					continue;
				final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
				if(value != null && !value.isBlank()){
					if(fullName.length() > 0)
						fullName.append(' ');
					fullName.append(value);
				}
			}
			if(fullName.length() > 0)
				return fullName.toString();
			// Fallback: the name block might carry a direct value.
			final String direct = FLEFRecordHelper.getChildValue(nameBlock, TAG_VALUE);
			if(direct != null && !direct.isBlank())
				return direct;
		}
		return (individual.getId() != null? individual.getId(): StringUtils.EMPTY);
	}

	public static String sex(final FLEFRecord individual){
		return (individual != null? FLEFRecordHelper.getChildValue(individual, TAG_SEX): null);
	}

	public static boolean isMale(final FLEFRecord individual){
		return SEX_MALE.equalsIgnoreCase(sex(individual));
	}

	public static boolean isFemale(final FLEFRecord individual){
		return SEX_FEMALE.equalsIgnoreCase(sex(individual));
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	/**
	 * Creates a relationship record of the given type, with the given
	 * subject and target, both individuals. The relationship is added to
	 * the model and returned.
	 * <p>
	 * The convention is the one used throughout the model: the subject
	 * is the endpoint described by the type relative to the target.
	 * For a parent-child relationship, the subject is the child and the
	 * target is the parent.
	 */
	public static FLEFRecord createRelationship(final FLEFModel model,
		final String subjectId, final String targetId, final String type,
		final String idPrefix){
		final FLEFRecord rel = FLEFRecord.createMainRecord(TYPE_RELATIONSHIP, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(TYPE_INDIVIDUAL, subjectId))
			)
			.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(TYPE_INDIVIDUAL, targetId))
			)
			.addChild(AuditBuilder.build());
		model.addRecord(rel);
		return rel;
	}

	/**
	 * Returns the ids of every relationship in which the given
	 * individual appears as subject or target.
	 */
	public static List<String> relationshipIdsForIndividual(final FLEFModel model,
		final String individualId){
		final List<String> result = new ArrayList<>();
		if(individualId == null)
			return result;
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subject = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
			if(individualId.equals(subject) || individualId.equals(target))
				result.add(rel.getId());
		}
		return result;
	}

	/**
	 * Returns the ids of the parents of the given individual, using the
	 * biological-child relationship only. The result is ordered: father
	 * first when present, then mother.
	 */
	public static List<String> biologicalParentIds(final FLEFModel model,
		final String individualId){
		final List<String> fathers = new ArrayList<>();
		final List<String> mothers = new ArrayList<>();
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null || !REL_BIOLOGICAL_CHILD.equalsIgnoreCase(type))
				continue;
			final String child = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String parent = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
			if(!individualId.equals(child) || parent == null)
				continue;
			final FLEFRecord parentRecord = model.getRecordById(parent);
			if(isMale(parentRecord))
				fathers.add(parent);
			else if(isFemale(parentRecord))
				mothers.add(parent);
			else
				fathers.add(parent);
		}
		final List<String> result = new ArrayList<>();
		result.addAll(fathers);
		result.addAll(mothers);
		return result;
	}

	/**
	 * Returns the id of the spouse of the given individual, or
	 * {@code null} when none is recorded. When several spouses exist,
	 * the first one is returned.
	 */
	public static String firstSpouseId(final FLEFModel model, final String individualId){
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null || !isSpouseType(type))
				continue;
			final String subject = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
			if(individualId.equals(subject) && target != null)
				return target;
			if(individualId.equals(target) && subject != null)
				return subject;
		}
		return null;
	}

	public static boolean isSpouseType(final String type){
		if(type == null)
			return false;
		final String t = type.toLowerCase(Locale.ROOT);
		for(final String s : SPOUSE_RELATION_TYPES)
			if(s.equals(t))
				return true;
		return false;
	}

	public static boolean isChildType(final String type){
		if(type == null)
			return false;
		final String t = type.toLowerCase(Locale.ROOT);
		for(final String s : CHILD_RELATION_TYPES)
			if(s.equals(t))
				return true;
		return false;
	}

	/**
	 * Returns whether the given individual already has a parent of the
	 * same sex as {@code parentSex}, so that the caller can decide
	 * whether to replace an existing parent instead of adding a second
	 * one.
	 */
	public static boolean hasParentOfSex(final FLEFModel model, final String childId,
		final String parentSex){
		for(final String parentId : biologicalParentIds(model, childId)){
			final FLEFRecord parent = model.getRecordById(parentId);
			if(parentSex.equalsIgnoreCase(sex(parent)))
				return true;
		}
		return false;
	}

	/**
	 * Re-points an existing relationship to a new endpoint. Used by the
	 * merge operation to move relationships without recreating them.
	 *
	 * @param rel        the relationship to modify
	 * @param wrapperTag {@code subject} or {@code target}
	 * @param newId      the new individual id
	 */
	public static void setRelationshipEndpoint(final FLEFRecord rel,
		final String wrapperTag, final String newId){
		final FLEFRecord wrapper = FLEFRecordHelper.findChild(rel, wrapperTag);
		if(wrapper == null)
			return;
		final FLEFRecord entityBlock = wrapper.getTheOnlyChild();
		if(entityBlock == null)
			return;
		final FLEFRecord idRef = entityBlock.getTheOnlyChild();
		if(idRef != null)
			idRef.setValue(newId);
		else
			entityBlock.setValue(newId);
	}


	/* ======================================================================
	 *                          Cleanup before delete
	 * ====================================================================== */

	/**
	 * Deletes the given individual and all the relationships that
	 * involve it, in a single operation. The relationships are removed
	 * first, so the model is never left with a dangling reference.
	 *
	 * @param model        the model
	 * @param individualId the individual to delete
	 */
	public static void deleteIndividualCascade(final FLEFModel model, final String individualId){
		if(individualId == null)
			return;
		final List<String> relIds = relationshipIdsForIndividual(model, individualId);
		for(final String relId : relIds)
			model.removeRecord(relId);
		model.removeRecord(individualId);
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row used by the merge dialog to preview the two records. */
	public record MergePreview(String id, String name, String sex,
										int parentCount, int childCount, int spouseCount, int eventCount){}

	/**
	 * Builds a compact preview of an individual, used by the merge
	 * dialog to show what is about to be merged.
	 */
	public static MergePreview buildMergePreview(final FLEFModel model,
		final String individualId){
		final FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null)
			return new MergePreview(individualId, individualId, null, 0, 0, 0, 0);

		final Set<String> parents = new LinkedHashSet<>(biologicalParentIds(model, individualId));
		final Set<String> children = new LinkedHashSet<>();
		final Set<String> spouses = new LinkedHashSet<>();
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null)
				continue;
			final String subject = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
			if(REL_BIOLOGICAL_CHILD.equalsIgnoreCase(type)){
				if(individualId.equals(target) && subject != null)
					children.add(subject);
			}
			else if(isSpouseType(type)){
				if(individualId.equals(subject) && target != null)
					spouses.add(target);
				else if(individualId.equals(target) && subject != null)
					spouses.add(subject);
			}
		}

		int eventCount = 0;
		for(final FLEFRecord participation : model.getRecordsByType("event_participation")){
			final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation,
				"participant");
			if(participantBlock == null)
				continue;
			final FLEFRecord oneof = participantBlock.getTheOnlyChild();
			if(oneof == null || !TYPE_INDIVIDUAL.equalsIgnoreCase(oneof.getTag()))
				continue;
			final FLEFRecord ref = oneof.getTheOnlyChild();
			final String id = (ref != null? ref.getValue(): oneof.getValue());
			if(individualId.equals(id))
				eventCount++;
		}

		return new MergePreview(individualId, displayName(individual), sex(individual),
			parents.size(), children.size(), spouses.size(), eventCount);
	}

}

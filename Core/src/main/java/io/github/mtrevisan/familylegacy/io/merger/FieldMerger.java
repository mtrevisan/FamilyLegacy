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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Merges the fields of an absorbed record into the surviving record, following the cardinalities of the grammar:
 * <ul>
 *   <li>repeated fields are united; an occurrence that states the same thing as one of the survivor (see {@link RecordSignature})
 *     is not duplicated, its evidence (citations, notes) is united into the survivor's occurrence instead;</li>
 *   <li>a singular field keeps the survivor's value, takes the absorbed one when the survivor has none, and reports a conflict
 *     when both are present and differ (a singular field is never assigned twice);</li>
 *   <li>PRIVACY takes the most restrictive of the two, since a merge must not weaken the intent of a contributor;</li>
 *   <li>AUDIT stays the survivor's: it describes the record, not the subject (the merge itself is appended as an update by the
 *     caller);</li>
 *   <li>fields unknown to the grammar are preserved from both records.</li>
 * </ul>
 * Several occurrences of the same singular field in one record (blocks of the same field written separately) are folded into one.
 */
final class FieldMerger{

	private static final String TAG_AUDIT = "audit";
	private static final String TAG_CREATION = "creation";
	private static final String TAG_UPDATE = "update";
	private static final String TAG_DATE = "date";
	private static final String TAG_COMMENT = "comment";
	private static final String TAG_LEVEL = "level";
	private static final String TAG_EXPIRES = "expires";
	private static final String TAG_SEX = "sex";
	private static final String SEX_UNKNOWN = "unknown";

	private static final List<String> PRIVACY_LEVELS = List.of("public", "restricted", "confidential");


	private final GrammarSupport grammar;
	private final RecordSignature signature;


	FieldMerger(final GrammarSupport grammar, final RecordSignature signature){
		this.grammar = grammar;
		this.signature = signature;
	}


	/**
	 * Merges {@code absorbed} into {@code survivor}, which is modified in place.
	 *
	 * @param survivor  the record that remains
	 * @param absorbed  the record that disappears (not modified)
	 * @param conflicts where to describe the values of the absorbed record that could not be kept
	 */
	void mergeRecord(final FLEFRecord survivor, final FLEFRecord absorbed, final List<String> conflicts){
		final StructType type = grammar.recordType(survivor.getTag());
		if(type == null){
			conflicts.add("record kind '" + survivor.getTag() + "' unknown to the grammar: fields of " + absorbed.getId()
				+ " added verbatim");
			for(final FLEFRecord child : absorbed.getChildren())
				insertChild(survivor, copy(child));
			return;
		}

		mergeStruct(survivor, absorbed, type, survivor.getTag(), conflicts);
	}

	/**
	 * Merges a structure of the given type, as {@link #mergeRecord} does for a record.
	 */
	void mergeStructure(final FLEFRecord target, final FLEFRecord source, final StructType type, final String path,
			final List<String> conflicts){
		mergeStruct(target, source, type, path, conflicts);
	}

	private void mergeStruct(final FLEFRecord target, final FLEFRecord source, final StructType type, final String path,
			final List<String> conflicts){
		final Set<String> knownTags = new HashSet<>();
		for(final FieldDefinition field : type.getFields()){
			final String tag = field.name();
			knownTags.add(tag);

			mergeField(target, source, field, path, conflicts);
		}

		mergeUnknownFields(target, source, knownTags, path, conflicts);
	}

	/**
	 * Merges a single field of a structure.
	 */
	void mergeField(final FLEFRecord target, final FLEFRecord source, final FieldDefinition field, final String path,
			final List<String> conflicts){
		final String fieldPath = path + "." + field.name();
		final String typeName = grammar.declaredTypeName(field);
		if(GrammarSupport.TYPE_AUDIT.equals(typeName))
			foldSingular(target, null, field, fieldPath, conflicts);
		else if(GrammarSupport.TYPE_PRIVACY.equals(typeName))
			mergePrivacy(target, source, field.name());
		else if(GrammarSupport.isRepeated(field))
			mergeRepeated(target, source, field, fieldPath, conflicts);
		else
			foldSingular(target, source, field, fieldPath, conflicts);
	}

	private void mergeRepeated(final FLEFRecord target, final FLEFRecord source, final FieldDefinition field,
			final String path, final List<String> conflicts){
		final List<FLEFRecord> sourceOccurrences = children(source, field.name());
		if(sourceOccurrences.isEmpty())
			return;

		final StructType struct = grammar.structOf(field);
		for(final FLEFRecord occurrence : sourceOccurrences){
			final String occurrenceSignature = signature.of(occurrence, field.type());
			FLEFRecord same = null;
			for(final FLEFRecord existing : children(target, field.name()))
				if(occurrenceSignature.equals(signature.of(existing, field.type()))){
					same = existing;
					break;
				}

			if(same == null)
				insertChild(target, copy(occurrence));
			else if(struct != null)
				// same assertion: unite its evidence
				mergeStruct(same, occurrence, struct, path, conflicts);
		}
	}

	/**
	 * Folds every occurrence of a singular field, of the target first and then of the source, into one occurrence.
	 *
	 * @param source the record whose occurrences are folded in after the target's, {@code null} to fold only the target's
	 */
	private void foldSingular(final FLEFRecord target, final FLEFRecord source, final FieldDefinition field,
			final String path, final List<String> conflicts){
		final List<FLEFRecord> occurrences = new ArrayList<>(children(target, field.name()));
		final int targetCount = occurrences.size();
		if(source != null)
			occurrences.addAll(children(source, field.name()));
		if(occurrences.isEmpty() || occurrences.size() == 1 && targetCount == 1)
			return;

		FLEFRecord kept;
		if(targetCount == 0){
			kept = copy(occurrences.getFirst());
			insertChild(target, kept);
		}
		else
			kept = occurrences.getFirst();
		for(int i = 1, size = occurrences.size(); i < size; i ++){
			final FLEFRecord other = occurrences.get(i);
			mergeSingularValue(kept, other, field, path, conflicts);
			if(i < targetCount)
				target.removeChild(other);
		}
	}

	private void mergeSingularValue(final FLEFRecord kept, final FLEFRecord other, final FieldDefinition field,
			final String path, final List<String> conflicts){
		final StructType struct = grammar.structOf(field);
		if(struct != null){
			mergeStruct(kept, other, struct, path, conflicts);
			return;
		}

		final UnionType union = grammar.unionOf(field);
		if(union != null){
			mergeUnion(kept, other, union, path, conflicts);
			return;
		}

		mergeLeaf(kept, other, field.name(), path, conflicts);
	}

	/**
	 * A union holds exactly one arm: the arms are merged when they are the same, otherwise the kept arm wins.
	 */
	private void mergeUnion(final FLEFRecord kept, final FLEFRecord other, final UnionType union, final String path,
			final List<String> conflicts){
		final FLEFRecord keptArm = (kept.getChildren().isEmpty()? null: kept.getChildren().getFirst());
		final FLEFRecord otherArm = (other.getChildren().isEmpty()? null: other.getChildren().getFirst());
		if(otherArm == null)
			return;
		if(keptArm == null){
			insertChild(kept, copy(otherArm));
			return;
		}

		final String armPath = path + "." + keptArm.getTag();
		if(!Objects.equals(keptArm.getTag(), otherArm.getTag())){
			conflicts.add(armPath + ": kept, discarded " + describe(otherArm));
			return;
		}

		final StructType armStruct = grammar.structOfArm(union, keptArm.getTag());
		if(armStruct != null)
			mergeStruct(keptArm, otherArm, armStruct, armPath, conflicts);
		else
			mergeLeaf(keptArm, otherArm, keptArm.getTag(), armPath, conflicts);
	}

	private static void mergeLeaf(final FLEFRecord kept, final FLEFRecord other, final String tag, final String path,
			final List<String> conflicts){
		final String keptValue = kept.getValue();
		final String otherValue = other.getValue();
		if(otherValue == null || otherValue.strip().equals(keptValue != null? keptValue.strip(): null))
			return;

		// an unknown sex carries no information against a known one
		if(keptValue == null || TAG_SEX.equals(tag) && SEX_UNKNOWN.equals(keptValue))
			kept.setValue(otherValue);
		else if(!(TAG_SEX.equals(tag) && SEX_UNKNOWN.equals(otherValue)))
			conflicts.add(path + ": kept '" + keptValue + "', discarded '" + otherValue + "'");
	}

	/**
	 * Keeps the most restrictive privacy; at equal level, the one that does not expire, then the one that expires later.
	 */
	private static void mergePrivacy(final FLEFRecord target, final FLEFRecord source, final String tag){
		final List<FLEFRecord> occurrences = new ArrayList<>(children(target, tag));
		final int targetCount = occurrences.size();
		occurrences.addAll(children(source, tag));
		if(occurrences.size() <= 1 && targetCount == occurrences.size())
			return;

		FLEFRecord strictest = occurrences.getFirst();
		for(int i = 1, size = occurrences.size(); i < size; i ++)
			if(isStricter(occurrences.get(i), strictest))
				strictest = occurrences.get(i);

		final FLEFRecord kept = copy(strictest);
		target.getChildren()
			.removeIf(child -> tag.equals(child.getTag()));
		insertChild(target, kept);
	}

	private static boolean isStricter(final FLEFRecord privacy, final FLEFRecord reference){
		final int level = privacyLevel(privacy);
		final int referenceLevel = privacyLevel(reference);
		if(level != referenceLevel)
			return (level > referenceLevel);

		final String expires = value(privacy, TAG_EXPIRES);
		final String referenceExpires = value(reference, TAG_EXPIRES);
		if(referenceExpires == null)
			return false;
		// ISO 8601 date-times with the same offset compare as text; a missing expiry never expires
		return (expires == null || expires.compareTo(referenceExpires) > 0);
	}

	private static int privacyLevel(final FLEFRecord privacy){
		final int level = PRIVACY_LEVELS.indexOf(value(privacy, TAG_LEVEL));
		// an unreadable level is treated as the most restrictive
		return (level >= 0? level: PRIVACY_LEVELS.size());
	}

	private void mergeUnknownFields(final FLEFRecord target, final FLEFRecord source, final Set<String> knownTags,
			final String path, final List<String> conflicts){
		for(final FLEFRecord child : source.getChildren()){
			final String tag = child.getTag();
			if(tag == null || knownTags.contains(tag))
				continue;

			final String childSignature = signature.of(child, null);
			boolean present = false;
			for(final FLEFRecord existing : children(target, tag))
				if(childSignature.equals(signature.of(existing, null))){
					present = true;
					break;
				}
			if(!present){
				if(!children(target, tag).isEmpty())
					conflicts.add(path + "." + tag + ": field unknown to the grammar, both occurrences preserved");
				insertChild(target, copy(child));
			}
		}
	}


	/**
	 * Appends an {@code audit.update} entry to a record, creating the audit when missing.
	 */
	static void addAuditUpdate(final FLEFRecord record, final String dateTime, final String comment){
		FLEFRecord audit = first(record, TAG_AUDIT);
		if(audit == null){
			audit = FLEFRecord.createChildWithTag(TAG_AUDIT);
			audit.addChild(FLEFRecord.createChildWithTag(TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_DATE, dateTime)));
			record.addChild(audit);
		}

		final FLEFRecord update = FLEFRecord.createChildWithTag(TAG_UPDATE)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_DATE, dateTime));
		if(comment != null && !comment.isBlank())
			update.addChild(FLEFRecord.createChildWithTagAndValue(TAG_COMMENT, comment));
		insertChild(audit, update);
	}

	static FLEFRecord copy(final FLEFRecord original){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		original.deepCopyTo(copy);
		return copy;
	}

	/**
	 * Adds a child after the last child with the same tag, so that the occurrences of a field stay together.
	 */
	static void insertChild(final FLEFRecord parent, final FLEFRecord child){
		final List<FLEFRecord> children = parent.getChildren();
		for(int i = children.size() - 1; i >= 0; i --)
			if(Objects.equals(children.get(i).getTag(), child.getTag())){
				children.add(i + 1, child);
				return;
			}
		children.add(child);
	}

	static List<FLEFRecord> children(final FLEFRecord parent, final String tag){
		final List<FLEFRecord> result = new ArrayList<>(0);
		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()))
				result.add(child);
		return result;
	}

	static FLEFRecord first(final FLEFRecord parent, final String tag){
		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()))
				return child;
		return null;
	}

	private static String value(final FLEFRecord parent, final String tag){
		final FLEFRecord child = first(parent, tag);
		return (child != null? child.getValue(): null);
	}

	private static String describe(final FLEFRecord node){
		return node.getTag() + (node.getValue() != null? " '" + node.getValue() + "'": " {…}");
	}

}

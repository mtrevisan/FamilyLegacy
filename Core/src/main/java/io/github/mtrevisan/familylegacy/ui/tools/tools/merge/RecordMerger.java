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
package io.github.mtrevisan.familylegacy.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Plans and applies the merge of two records of the same type.
 * <p>
 * The merge is <b>field-based</b>: every direct child tag of the two
 * records is treated as a field. For each tag:
 * <ul>
 *   <li>if only the target has instances, they are kept;</li>
 *   <li>if only the source has instances, they are added to the target;</li>
 *   <li>if both have instances and they are equal, nothing changes;</li>
 *   <li>otherwise a collision is recorded and the user must decide
 *       whether to keep the target's, the source's, or both.</li>
 * </ul>
 * After the fields are merged, the references to the source are
 * re-pointed to the target: any value in the model that equals the
 * source id becomes the target id. This is what keeps relationships,
 * event participations, attributes, identity hypotheses, and any other
 * reference valid after the source is deleted.
 * <p>
 * The service works on any record type. It does not use a schema: the
 * FLEF format is self-describing, so tags and values are enough to
 * decide what to merge and what to re-point.
 */
public final class RecordMerger{

	private final FLEFModel model;


	public RecordMerger(final FLEFModel model){
		this.model = Objects.requireNonNull(model, "Model must not be null");
	}


	/* ======================================================================
	 *                          Planning
	 * ====================================================================== */

	/**
	 * Builds a merge plan from two records of the same type.
	 *
	 * @param sourceId the id of the record to be deleted
	 * @param targetId the id of the record to keep
	 * @return the plan, never {@code null}
	 * @throws IllegalArgumentException if either id is missing, if the
	 *                                  ids are equal, if a record does not exist, or if the two
	 *                                  records have different tags
	 */
	public MergePlan plan(final String sourceId, final String targetId){
		validate(sourceId, targetId);

		final FLEFRecord source = model.getRecordById(sourceId);
		final FLEFRecord target = model.getRecordById(targetId);
		if(source == null)
			throw new IllegalArgumentException("Source record does not exist: " + sourceId);
		if(target == null)
			throw new IllegalArgumentException("Target record does not exist: " + targetId);
		if(!Objects.equals(source.getTag(), target.getTag()))
			throw new IllegalArgumentException("Source and target are not the same record type: "
				+ source.getTag() + " vs " + target.getTag());

		final Map<String, List<FLEFRecord>> targetByTag = indexChildrenByTag(target);
		final Map<String, List<FLEFRecord>> sourceByTag = indexChildrenByTag(source);

		// Every tag that appears on either side is a candidate field.
		// A field is a collision only when both sides have at least one
		// instance and the instances differ; otherwise it is auto-resolved
		// during apply.
		final Set<String> allTags = new LinkedHashSet<>();
		allTags.addAll(targetByTag.keySet());
		allTags.addAll(sourceByTag.keySet());

		final List<MergeField> collisions = new ArrayList<>();
		for(final String tag : allTags){
			final List<FLEFRecord> targetInstances = targetByTag.getOrDefault(tag, List.of());
			final List<FLEFRecord> sourceInstances = sourceByTag.getOrDefault(tag, List.of());

			if(targetInstances.isEmpty() || sourceInstances.isEmpty())
				continue;
			if(equalInstances(targetInstances, sourceInstances))
				continue;

			final MergeDecision suggested = suggest(targetInstances, sourceInstances);
			collisions.add(new MergeField(tag, targetInstances, sourceInstances, suggested));
		}

		return new MergePlan(source, target, collisions, List.of());
	}

	private static Map<String, List<FLEFRecord>> indexChildrenByTag(final FLEFRecord record){
		final Map<String, List<FLEFRecord>> byTag = new LinkedHashMap<>();
		for(final FLEFRecord child : record.getChildren()){
			final String tag = child.getTag();
			if(tag != null)
				byTag.computeIfAbsent(tag, k -> new ArrayList<>())
					.add(child);
		}
		return byTag;
	}

	private static boolean equalInstances(final List<FLEFRecord> a, final List<FLEFRecord> b){
		if(a.size() != b.size())
			return false;

		for(int i = 0; i < a.size(); i ++)
			if(!a.get(i).equals(b.get(i)))
				return false;
		return true;
	}

	/**
	 * Default suggestion for a collision:
	 * <ul>
	 *   <li>repeated fields (more than one instance on either side) →
	 *       {@code KEEP_BOTH}, the additive choice;</li>
	 *   <li>single-valued fields → {@code KEEP_TARGET}, because the
	 *       target is the surviving record and its value is the one the
	 *       user has been looking at.</li>
	 * </ul>
	 */
	private static MergeDecision suggest(final List<FLEFRecord> targetInstances, final List<FLEFRecord> sourceInstances){
		if(targetInstances.size() > 1 || sourceInstances.size() > 1)
			return MergeDecision.KEEP_BOTH;
		return MergeDecision.KEEP_TARGET;
	}


	/* ======================================================================
	 *                          Applying
	 * ====================================================================== */

	/**
	 * Applies the plan to the model: writes the source's fields into the
	 * target according to the decisions, re-points every reference to the
	 * source to the target, and finally removes the source record.
	 *
	 * @param plan the plan, with the user's decisions already recorded
	 * @return a summary of what was moved
	 */
	public MergeResult apply(final MergePlan plan){
		final FLEFRecord source = plan.source();
		final FLEFRecord target = plan.target();
		final String sourceId = source.getId();
		final String targetId = target.getId();

		// 1. Per-tag decisions on collisions.
		for(final MergeField field : plan.collisions())
			applyCollision(target, field);

		// 2. Tags that appear only on the source: taken automatically.
		final Set<String> collisionTags = new LinkedHashSet<>();
		for(final MergeField field : plan.collisions())
			collisionTags.add(field.tag());

		int addedFromSource = 0;
		final Map<String, List<FLEFRecord>> sourceByTag = indexChildrenByTag(source);
		for(final Map.Entry<String, List<FLEFRecord>> entry : sourceByTag.entrySet()){
			if(collisionTags.contains(entry.getKey()))
				continue;

			for(final FLEFRecord field : entry.getValue()){
				target.addChild(copyOf(field));
				addedFromSource ++;
			}
		}

		// 3. Re-point references: any value in the model that equals the
		//    source id becomes the target id. Skipped inside the source
		//    (about to be deleted) and inside the target (already
		//    handled by the field merge; any inner reference to the
		//    source is meaningful and gets updated too).
		final int repointed = repointReferences(sourceId, targetId, source);

		// 4. Delete the source.
		model.removeRecord(sourceId);

		return new MergeResult(addedFromSource, repointed);
	}

	/**
	 * Applies one collision decision: removes the losing instances from
	 * the target and, when the source wins, adds copies of the source's
	 * instances to the target.
	 */
	private static void applyCollision(final FLEFRecord target, final MergeField field){
		final List<FLEFRecord> result = field.resultInstances();

		// Fast path: the result equals the current target content, nothing
		// to do. This is the common case for "keep target" decisions and for
		// common rows in a comparison-driven merge.
		if(sameInstances(result, field.targetInstances()))
			return;

		removeChildrenWithTag(target, field.tag());
		for(final FLEFRecord resultInstance : result)
			target.addChild(copyOf(resultInstance));
	}

	private static boolean sameInstances(final List<FLEFRecord> a, final List<FLEFRecord> b){
		if(a.size() != b.size())
			return false;

		for(int i = 0; i < a.size(); i ++)
			if(!a.get(i).equals(b.get(i)))
				return false;
		return true;
	}

	private static void removeChildrenWithTag(final FLEFRecord record, final String tag){
		record.getChildren()
			.removeIf(child -> Objects.equals(tag, child.getTag()));
	}

	/**
	 * Deep copy of a field, so that adding it to the target does not move
	 * the object out of the source's tree (the source is going to be
	 * deleted anyway, but the copy makes the operation safe and
	 * order-independent).
	 */
	private static FLEFRecord copyOf(final FLEFRecord original){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		original.deepCopyTo(copy);
		return copy;
	}


	/* ======================================================================
	 *                          Reference re-pointing
	 * ====================================================================== */

	/**
	 * Re-points every value in the model that equals {@code sourceId} to
	 * {@code targetId}, except inside the records listed in
	 * {@code skipRecords}.
	 * <p>
	 * The walk is generic and does not use a schema: it treats every
	 * value as a potential reference and replaces it when the whole
	 * value matches the source id exactly. False positives are unlikely
	 * because ids are unique within the file, and the exact-match rule
	 * excludes any string that merely contains the id.
	 */
	private int repointReferences(final String sourceId, final String targetId, final FLEFRecord... skipRecords){
		final Set<String> skipIds = new LinkedHashSet<>();
		for(final FLEFRecord r : skipRecords)
			if(r != null && r.getId() != null)
				skipIds.add(r.getId());

		int count = 0;
		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records){
			if(skipIds.contains(record.getId()))
				continue;

			count += repointIn(record, sourceId, targetId);
		}
		return count;
	}

	private static int repointIn(final FLEFRecord record, final String oldId, final String newId){
		int count = 0;
		if(oldId.equals(record.getValue())){
			record.setValue(newId);
			count ++;
		}
		for(final FLEFRecord child : record.getChildren())
			count += repointIn(child, oldId, newId);
		return count;
	}


	/* ======================================================================
	 *                          Validation
	 * ====================================================================== */

	private static void validate(final String sourceId, final String targetId){
		if(StringUtils.isEmpty(sourceId) || StringUtils.isEmpty(targetId))
			throw new IllegalArgumentException("Source and target ids must not be empty");
		if(sourceId.equals(targetId))
			throw new IllegalArgumentException("Source and target must be different records");
	}


	/**
	 * Summary of what a merge changed.
	 *
	 * @param fieldsTakenFromSource number of field instances added to the
	 *                              target
	 * @param referencesRepointed   number of references re-pointed from
	 *                              the source to the target
	 */
	public record MergeResult(int fieldsTakenFromSource, int referencesRepointed){}

}

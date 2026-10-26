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

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
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
 * Plans and applies the merge of two records of the same type, decided by the user.
 * <p>
 * The merge is <b>field-based</b>: every direct child tag of the two records is a field, and its role comes from the grammar (see
 * {@link RecordAbsorber.FieldRole}). For each field:
 * <ul>
 *   <li>if both records have it and the occurrences differ, a collision is recorded and the user decides whether to keep the
 *       target's, the source's, or (for repeated fields only) both;</li>
 *   <li>otherwise it is merged automatically with the rules of the protocol: the source's occurrences are added, an occurrence
 *       stating the same thing as one of the target unites its citations and notes into it, a singular field is taken only if
 *       the target lacks it;</li>
 *   <li>AUDIT is never a collision: the target keeps its own, and gains an update entry naming the source;</li>
 *   <li>PRIVACY is never a collision: the most restrictive of the two is kept, since a merge must not weaken it.</li>
 * </ul>
 * The fields being final, the source is absorbed following the Merge rule of the protocol (see {@link RecordAbsorber#absorb}):
 * every reference and {@code flef:} link is redirected to the target, the association records that became identical are
 * collapsed, the source is deleted, and the identity hypothesis that proposed the merge is deleted with its cascade.
 */
public final class RecordMerger{

	private final FLEFModel model;
	private final RecordAbsorber absorber;


	/**
	 * @param model   the model to merge in
	 * @param grammar the FLEF grammar, which tells references and field cardinalities
	 * @throws NullPointerException if the grammar is {@code null}
	 */
	public RecordMerger(final FLEFModel model, final FLEFGrammar grammar){
		this.model = model;
		absorber = new RecordAbsorber(Objects.requireNonNull(grammar, "The FLEF grammar is required to merge records"));
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

		final String kind = target.getTag();
		final Map<String, List<FLEFRecord>> targetByTag = indexChildrenByTag(target);
		final Map<String, List<FLEFRecord>> sourceByTag = indexChildrenByTag(source);

		// Every tag that appears on either side is a candidate field.
		// A field is a collision only when both sides have at least one
		// instance and they state different things; otherwise it is
		// merged automatically during apply.
		final Set<String> allTags = new LinkedHashSet<>();
		allTags.addAll(targetByTag.keySet());
		allTags.addAll(sourceByTag.keySet());

		final List<MergeField> collisions = new ArrayList<>();
		for(final String tag : allTags){
			final RecordAbsorber.FieldRole role = absorber.fieldRole(kind, tag);
			if(role == RecordAbsorber.FieldRole.AUDIT || role == RecordAbsorber.FieldRole.PRIVACY)
				continue;

			final List<FLEFRecord> targetInstances = targetByTag.getOrDefault(tag, List.of());
			final List<FLEFRecord> sourceInstances = sourceByTag.getOrDefault(tag, List.of());
			if(targetInstances.isEmpty() || sourceInstances.isEmpty())
				continue;
			if(absorber.sameContent(kind, tag, targetInstances, sourceInstances))
				continue;

			// a field unknown to the grammar may be repeated: keeping both preserves it, as the protocol requires
			final boolean repeated = (role != RecordAbsorber.FieldRole.SINGULAR);
			final MergeDecision suggested = (repeated? MergeDecision.KEEP_BOTH: MergeDecision.KEEP_TARGET);
			collisions.add(new MergeField(tag, targetInstances, sourceInstances, suggested, repeated));
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


	/* ======================================================================
	 *                          Applying
	 * ====================================================================== */

	/**
	 * Applies the plan to the model: writes the source's fields into the
	 * target according to the decisions, then absorbs the source into the
	 * target.
	 *
	 * @param plan the plan, with the user's decisions already recorded
	 * @return a summary of what changed
	 * @throws IllegalArgumentException if a decision would assign a singular field twice; the model is left untouched
	 */
	public MergeResult apply(final MergePlan plan){
		final FLEFRecord source = plan.source();
		final FLEFRecord target = plan.target();

		for(final MergeField field : plan.collisions())
			if(!field.isRepeated() && field.resultInstances().size() > 1)
				throw new IllegalArgumentException("Field '" + field.tag() + "' holds a single value: keep either the target's or the"
					+ " source's");

		final int childrenBefore = target.getChildren().size();
		final List<String> conflicts = new ArrayList<>();

		// 1. Per-tag decisions on collisions.
		final Set<String> collisionTags = new LinkedHashSet<>();
		for(final MergeField field : plan.collisions()){
			collisionTags.add(field.tag());

			if(field.chosen() == MergeDecision.KEEP_BOTH && !field.isComposed())
				// union that does not duplicate what the target already states
				conflicts.addAll(absorber.mergeField(target, source, field.tag()));
			else
				applyCollision(target, field);
		}

		// 2. Every other field, with the rules of the protocol.
		final Set<String> otherTags = new LinkedHashSet<>();
		for(final FLEFRecord child : source.getChildren())
			if(child.getTag() != null && !collisionTags.contains(child.getTag()))
				otherTags.add(child.getTag());
		for(final FLEFRecord child : target.getChildren())
			if(child.getTag() != null && !collisionTags.contains(child.getTag()))
				otherTags.add(child.getTag());
		for(final String tag : otherTags)
			conflicts.addAll(absorber.mergeField(target, source, tag));

		// 3. Redirect the references, collapse the duplicates, delete the source and the accepted hypothesis.
		final RecordAbsorber.Result result = absorber.absorb(model, source.getId(), target.getId(), null);

		return new MergeResult(Math.max(0, target.getChildren().size() - childrenBefore), result.referencesRedirected(),
			result.collapsedIds(), result.deletedIds(), conflicts);
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
	 * @param fieldsTakenFromSource number of field instances added to the target
	 * @param referencesRepointed   number of references (and {@code flef:} links) re-pointed from the source to the target
	 * @param collapsedIds          association records collapsed into an identical one after the merge
	 * @param deletedIds            records deleted by the merge (the accepted identity hypothesis and its cascade)
	 * @param conflicts             values of the source that could not be kept in the target
	 */
	public record MergeResult(int fieldsTakenFromSource, int referencesRepointed, List<String> collapsedIds,
		List<String> deletedIds, List<String> conflicts){}

}

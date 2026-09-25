package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The outcome of planning a merge: which fields of the source will be
 * added to the target, which collisions need a decision from the user,
 * and which references to the source will be re-pointed to the target.
 * <p>
 * The plan is produced by {@link RecordMerger#plan(String, String)} and
 * consumed by {@link RecordMerger#apply(MergePlan)}.
 */
public final class MergePlan{

	private final FLEFRecord source;
	private final FLEFRecord target;
	private final List<MergeField> collisions;
	private final List<String> referenceIdsToRepoint;


	MergePlan(final FLEFRecord source, final FLEFRecord target,
			final List<MergeField> collisions, final List<String> referenceIdsToRepoint){
		this.source = source;
		this.target = target;
		this.collisions = new ArrayList<>(collisions);
		this.referenceIdsToRepoint = List.copyOf(referenceIdsToRepoint);
	}


	public FLEFRecord source(){
		return source;
	}

	public FLEFRecord target(){
		return target;
	}

	public List<MergeField> collisions(){
		return Collections.unmodifiableList(collisions);
	}

	public List<String> referenceIdsToRepoint(){
		return referenceIdsToRepoint;
	}

	public boolean hasCollisions(){
		return !collisions.isEmpty();
	}

}

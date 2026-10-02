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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

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

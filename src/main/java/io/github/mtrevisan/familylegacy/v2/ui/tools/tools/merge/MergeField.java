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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;


/**
 * One field (a direct child tag) of the record being merged. Holds the
 * instances found on the target and on the source, and the decision the
 * user made about how to combine them.
 */
public final class MergeField{

	private final String tag;
	private final List<FLEFRecord> targetInstances;
	private final List<FLEFRecord> sourceInstances;
	private final MergeDecision suggested;
	private MergeDecision chosen;
	private List<FLEFRecord> resultInstances;
	/** {@code true} once the user has made an explicit decision on this field. */
	private boolean resolved;


	MergeField(final String tag, final List<FLEFRecord> targetInstances,
		final List<FLEFRecord> sourceInstances, final MergeDecision suggested){
		this.tag = tag;
		this.targetInstances = List.copyOf(targetInstances);
		this.sourceInstances = List.copyOf(sourceInstances);
		this.suggested = suggested;
		this.chosen = suggested;
	}


	public String tag(){ return tag; }
	public List<FLEFRecord> targetInstances(){ return targetInstances; }
	public List<FLEFRecord> sourceInstances(){ return sourceInstances; }
	public MergeDecision suggested(){ return suggested; }
	public MergeDecision chosen(){ return chosen; }

	public boolean isResolved(){
		return resolved;
	}

	/**
	 * Records a coarse decision for the whole field. Marks the field
	 * as resolved so the merge dialog can visually distinguish the rows
	 * the user has explicitly acted on.
	 */
	public void choose(final MergeDecision decision){
		this.chosen = decision;
		this.resultInstances = null;
		this.resolved = true;
	}

	/**
	 * Records a fine-grained composition from the comparison dialog.
	 * Marks the field as resolved so the merge dialog can visually
	 * distinguish the rows the user has explicitly acted on.
	 */
	public void setResultInstances(final List<FLEFRecord> instances){
		this.resultInstances = (instances != null? List.copyOf(instances): null);
		if(instances != null)
			this.resolved = true;
	}

	public List<FLEFRecord> resultInstances(){
		if(resultInstances != null)
			return resultInstances;
		return switch(chosen){
			case KEEP_TARGET -> targetInstances;
			case KEEP_SOURCE -> sourceInstances;
			case KEEP_BOTH -> {
				final List<FLEFRecord> both = new ArrayList<>(targetInstances);
				both.addAll(sourceInstances);
				yield both;
			}
		};
	}

	public boolean isCollision(){
		return !targetInstances.isEmpty() && !sourceInstances.isEmpty();
	}

	/**
	 * Restores the field to its initial state: the chosen decision is the
	 * suggested one, any fine-grained composition from the comparison
	 * dialog is dropped, and the {@code resolved} flag is cleared. Called
	 * by the merge dialog when the user clicks the reset button on a row
	 * or presses Delete on a selected row.
	 */
	public void reset(){
		this.chosen = suggested;
		this.resultInstances = null;
		this.resolved = false;
	}

}

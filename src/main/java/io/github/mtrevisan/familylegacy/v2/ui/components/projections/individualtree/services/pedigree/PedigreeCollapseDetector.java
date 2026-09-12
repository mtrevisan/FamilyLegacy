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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks an ancestor tree and identifies every individual that appears more
 * than once, together with the paths through which they are reached from
 * the root.
 * <p>
 * The walk is iterative (a stack-based depth-first traversal) and visits
 * every slot of every panel in the tree. A slot is the position of a
 * father or a mother inside a {@link TreeNode}; each slot contains exactly
 * one individual (or none, if the corresponding parent is unknown).
 * <p>
 * The detector does not modify the tree: it produces an immutable list of
 * {@link PedigreeCollapse} records that can be cached and reused until the
 * tree changes.
 */
public final class PedigreeCollapseDetector{

	private static final String FATHER_STEP = "F";
	private static final String MOTHER_STEP = "M";


	private PedigreeCollapseDetector(){}


	/**
	 * Detects every pedigree collapse in the given tree.
	 *
	 * @param root  the root of the ancestor tree; may be {@code null}
	 * @param model the FLEF model, used to resolve display names (must not
	 *              be {@code null})
	 * @return the list of collapses, ordered by descending occurrence
	 *         count, then by display name; never {@code null}
	 */
	public static List<PedigreeCollapse> detect(final TreeNode root, final FLEFModel model){
		if(root == null)
			return List.of();

		// Accumulate the paths through which each individual is reached.
		// Insertion order is preserved so that ties in the final sort are
		// broken deterministically by the order of discovery.
		final Map<String, List<PedigreePath>> occurrences = new LinkedHashMap<>();

		final Deque<TraversalEntry> stack = new ArrayDeque<>();
		stack.push(new TraversalEntry(root, "", 0));
		while(!stack.isEmpty()){
			final TraversalEntry entry = stack.pop();
			final TreeNode node = entry.node;

			// Father slot
			final TreeNode father = node.getFather();
			if(father != null){
				final String fatherId = father.getIndividualId();
				if(fatherId != null){
					final String pathCode = entry.path + FATHER_STEP;
					occurrences.computeIfAbsent(fatherId, k -> new ArrayList<>())
						.add(new PedigreePath(pathCode, entry.generation + 1));
					stack.push(new TraversalEntry(father, pathCode, entry.generation + 1));
				}
			}

			// Mother slot
			final TreeNode mother = node.getMother();
			if(mother != null){
				final String motherId = mother.getIndividualId();
				if(motherId != null){
					final String pathCode = entry.path + MOTHER_STEP;
					occurrences.computeIfAbsent(motherId, k -> new ArrayList<>())
						.add(new PedigreePath(pathCode, entry.generation + 1));
					stack.push(new TraversalEntry(mother, pathCode, entry.generation + 1));
				}
			}
		}

		// Build the collapse list, keeping only individuals with more than one occurrence
		final IndividualHandler handler = IndividualHandler.getInstance();
		final List<PedigreeCollapse> collapses = new ArrayList<>();
		for(final Map.Entry<String, List<PedigreePath>> entry : occurrences.entrySet()){
			final List<PedigreePath> paths = entry.getValue();
			if(paths.size() <= 1)
				continue;

			final String id = entry.getKey();
			final FLEFRecord record = model.getRecordById(id);
			final String displayName;
			if(record != null){
				try{
					final String text = handler.getDisplayText(record, model);
					displayName = (text != null && !text.isBlank()? text: id);
				}
				catch(final RuntimeException ignored){
					// Fall through to the id-based fallback
					// We cannot use a named local here without restructuring, so just build a small holder
					final String fallback = id;
					collapses.add(new PedigreeCollapse(id, fallback, paths.size(), paths));
					continue;
				}
			}
			else
				displayName = id;

			collapses.add(new PedigreeCollapse(id, displayName, paths.size(), paths));
		}

		collapses.sort(Comparator
			.comparingInt(PedigreeCollapse::occurrenceCount)
			.reversed()
			.thenComparing(PedigreeCollapse::displayName));

		return collapses;
	}


	/**
	 * Internal stack entry used during traversal. Combines the tree node
	 * being visited with the path leading to it and the generation depth.
	 */
	private record TraversalEntry(TreeNode node, String path, int generation){}

}

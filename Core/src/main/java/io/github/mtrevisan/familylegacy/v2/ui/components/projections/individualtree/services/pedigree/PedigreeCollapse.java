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

import java.util.List;


/**
 * A single pedigree collapse detected in the ancestor tree.
 * <p>
 * A collapse occurs when the same individual appears more than once in the
 * ancestor tree, at different positions. The two most common causes are:
 * <ul>
 *   <li>two siblings married two siblings, so the same grandparents appear
 *       on both the paternal and the maternal side;</li>
 *   <li>endogamous marriages within a closed community, so the same
 *       ancestors recur across multiple branches.</li>
 * </ul>
 * The record captures the individual, the number of occurrences, and the
 * list of paths through which the individual is reached from the root.
 *
 * @param individualId    the id of the individual that appears multiple times
 * @param displayName     the display name of the individual
 * @param occurrenceCount the number of times the individual appears in the tree
 * @param paths           the paths from the root to each occurrence
 */
public record PedigreeCollapse(
	String individualId,
	String displayName,
	int occurrenceCount,
	List<PedigreePath> paths
){

	/**
	 * Compact constructor with validation.
	 */
	public PedigreeCollapse{
		if(individualId == null)
			throw new IllegalArgumentException("Individual id must not be null");

		if(displayName == null)
			displayName = individualId;
		if(occurrenceCount < 2)
			throw new IllegalArgumentException("Occurrence count must be at least 2 for a collapse");

		paths = (paths != null? List.copyOf(paths): List.of());
		if(paths.size() != occurrenceCount)
			throw new IllegalArgumentException("Path count must match occurrence count");
	}


	/**
	 * Returns the highest generation reached by this individual, i.e. the
	 * generation of the furthest occurrence from the root.
	 *
	 * @return the highest generation, always positive
	 */
	int maxGeneration(){
		int max = 0;
		for(final PedigreePath path : paths)
			if(path.generation() > max)
				max = path.generation();
		return max;
	}

	/**
	 * Returns a concise summary line suitable for a table row.
	 *
	 * @return the summary
	 */
	String summary(){
		return displayName + " (" + occurrenceCount + "×, generations: ";
	}

}

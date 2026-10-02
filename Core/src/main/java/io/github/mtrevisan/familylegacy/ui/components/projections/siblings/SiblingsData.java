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
package io.github.mtrevisan.familylegacy.ui.components.projections.siblings;

import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Data Transfer Object containing prepared display information for a siblings group.
 */
public final class SiblingsData{

	private final List<IndividualData> siblings;
	private final Map<String, String> relationshipTypes;
	private final Set<String> siblingIdsWithDescendants;


	public static SiblingsData create(final List<IndividualData> siblings, final Map<String, String> relationshipTypes,
			final Set<String> siblingIdsWithDescendants){
		return new SiblingsData(siblings, relationshipTypes, siblingIdsWithDescendants);
	}

	/**
	 * A single child shown without a known couple: the relationship type
	 * is not known and will not be displayed.
	 */
	public static SiblingsData createSingleChild(final IndividualData siblings){
		return new SiblingsData(Collections.singletonList(siblings), Collections.emptyMap(), null);
	}

	/**
	 * Convenience factory when only the sibling list is known: no
	 * relationship type will be attached to any child.
	 */
	public static SiblingsData create(final List<IndividualData> siblings,
			final Set<String> siblingIdsWithDescendants){
		return new SiblingsData(siblings, Collections.emptyMap(), siblingIdsWithDescendants);
	}


	/**
	 * Constructs a new SiblingsData container with pre-calculated sibling information.
	 *
	 * @param siblings           list of sibling IndividualData objects
	 * @param siblingIdsWithDescendants     set of individual IDs that have descendants
	 */
	private SiblingsData(final List<IndividualData> siblings, final Map<String, String> relationshipTypes,
			final Set<String> siblingIdsWithDescendants){
		this.siblings = (siblings != null? siblings: Collections.emptyList());
		this.relationshipTypes = (relationshipTypes != null? relationshipTypes: Collections.emptyMap());
		this.siblingIdsWithDescendants = (siblingIdsWithDescendants != null
			? siblingIdsWithDescendants
			: Collections.emptySet());
	}


	public List<IndividualData> getSiblings(){
		return siblings;
	}

	/**
	 * Returns the relationship type through which the given child is
	 * linked to the couple that owns this SiblingsData, or {@code null}
	 * when it is not known.
	 *
	 * @param childId the child id
	 * @return the relationship type, or {@code null}
	 */
	public String getRelationshipType(final String childId){
		return (childId != null? relationshipTypes.get(childId): null);
	}

	public boolean hasDescendants(final String individualId){
		return siblingIdsWithDescendants.contains(individualId);
	}

}

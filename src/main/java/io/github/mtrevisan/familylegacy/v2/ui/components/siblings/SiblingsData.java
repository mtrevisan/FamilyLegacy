package io.github.mtrevisan.familylegacy.v2.ui.components.siblings;

import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;

import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Data Transfer Object containing prepared display information for a siblings group.
 */
public final class SiblingsData{

	// whether only the root should be visile
	private final boolean onlyRoot;
	private final List<IndividualData> siblings;
	private final Set<String> siblingIdsWithDescendants;


	public static SiblingsData create(final List<IndividualData> siblings, final Set<String> siblingIdsWithDescendants){
		return new SiblingsData(siblings, siblingIdsWithDescendants);
	}

	public static SiblingsData createSingleChild(final IndividualData siblings){
		return new SiblingsData(Collections.singletonList(siblings), null);
	}


	/**
	 * Constructs a new SiblingsData container with pre-calculated sibling information.
	 *
	 * @param siblings           list of sibling IndividualData objects
	 * @param siblingIdsWithDescendants     set of individual IDs that have descendants
	 */
	private SiblingsData(final List<IndividualData> siblings, final Set<String> siblingIdsWithDescendants){
		onlyRoot = (siblingIdsWithDescendants == null);
		this.siblings = (siblings != null
			? Collections.unmodifiableList(siblings)
			: Collections.emptyList());
		this.siblingIdsWithDescendants = (siblingIdsWithDescendants != null
			? siblingIdsWithDescendants
			: Collections.emptySet());
	}


	public boolean isOnlyRoot(){
		return onlyRoot;
	}

	public List<IndividualData> getSiblings(){
		return siblings;
	}

	public boolean hasDescendants(final String individualId){
		return siblingIdsWithDescendants.contains(individualId);
	}

}

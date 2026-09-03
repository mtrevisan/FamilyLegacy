package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;


/**
 * Event listener for changes in the biological tree structure or root node focus.
 */
@FunctionalInterface
public interface BiologicalTreeChangeListener{

	/**
	 * Triggered when the tree structure is modified or the root individual changes.
	 *
	 * @param rootIndividualId the ID of the individual to focus as root
	 */
	void onTreeStructureChanged(String rootIndividualId);

}

package io.github.mtrevisan.familylegacy.v2.ui.components.individual;


// TODO used?
/**
 * Listener for individual panel actions.
 */
public interface IndividualListener{

	void onIndividualFocus(IndividualPanel individualPanel);

	void onIndividualEdit(IndividualPanel individualPanel);

	void onIndividualAdd(IndividualPanel individualPanel);

	void onIndividualLink(IndividualPanel individualPanel);

	void onIndividualRemove(IndividualPanel individualPanel);

	void onIndividualUnlinkFromParentGroup(IndividualPanel individualPanel);

	void onIndividualAddToSiblingGroup(IndividualPanel individualPanel);

	void onIndividualUnlinkFromSiblingGroup(IndividualPanel individualPanel);

	void onIndividualAddPreferredImage(IndividualPanel individualPanel);

	void onIndividualEditPreferredImage(IndividualPanel individualPanel);

}

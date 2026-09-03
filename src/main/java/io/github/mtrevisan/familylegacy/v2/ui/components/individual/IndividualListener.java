package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;


/**
 * Listener for individual panel actions.
 */
public interface IndividualListener{

	void onIndividualEdit(FLEFRecord individual);


	void onIndividualSelected(FLEFRecord individual);

	void onIndividualRemove(FLEFRecord individual);

	void onIndividualUnlinkFromParentGroup(FLEFRecord individual);

	void onIndividualUnlinkFromPartner(FLEFRecord targetSibling);


	void onIndividualAdd(FLEFRecord targetParent);

	void onIndividualLink(FLEFRecord targetParent);

}

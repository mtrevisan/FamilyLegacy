package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;


/**
 * Listener for individual panel actions.
 */
public interface IndividualListener{

	void onIndividualSelected(FLEFRecord individual);

	void onIndividualEdit(FLEFRecord individual);

	void onIndividualRemove(FLEFRecord individual);

	void onIndividualMove(FLEFRecord individual);

	void onPanelSelected(IndividualPanel panel);

	void onIndividualAddOrLink(IndividualOperation operation, FLEFRecord father, FLEFRecord mother);

	void onIndividualUnlink(IndividualOperation operation, FLEFRecord individual, FLEFRecord child);

}

package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.Side;

import java.util.Map;


/**
 * Listener for individual panel actions.
 */
public interface IndividualListener{

	void onIndividualEdit(FLEFRecord individual);


	void onIndividualSelected(FLEFRecord individual);

	void onIndividualRemove(FLEFRecord individual);

	void onIndividualUnlinkFromParentGroup(FLEFRecord individual);

	void onIndividualUnlinkFromPartner(FLEFRecord targetSibling);


	void onIndividualMove(FLEFRecord individual);


	void onChildLink(FLEFRecord father, FLEFRecord mother);


	void onPartnerLink(FLEFRecord partner, Side side);


	void onParentLink(FLEFRecord child, Side side);




	void onPanelSelected(IndividualPanel panel);

	/**
	 * Performs a generic tree modification operation.
	 *
	 * @param operation the type of operation to perform
	 * @param subject   the individual being added, moved, or linked
	 * @param target    the target individual (e.g., father for ADD_CHILD, individual for ADD_PARTNER)
	 * @param params    additional parameters (e.g., mother for ADD_CHILD, sex for ADD_PARTNER)
	 */
	void onAddIndividual(IndividualOperation operation, FLEFRecord subject, FLEFRecord target, Map<String, Object> params);

}

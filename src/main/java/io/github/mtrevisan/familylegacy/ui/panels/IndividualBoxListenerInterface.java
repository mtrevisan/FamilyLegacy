package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.ui.enums.Sex;


public interface IndividualBoxListenerInterface{

	void onIndividualEdit(final IndividualBoxPanel boxPanel, final GedcomNode individual);

	void onIndividualFocus(final IndividualBoxPanel boxPanel, final GedcomNode individual);

	void onIndividualNew(final IndividualBoxPanel boxPanel);

	void onIndividualLink(final IndividualBoxPanel boxPanel);

	void onIndividualAddPreferredImage(final IndividualBoxPanel boxPanel, final GedcomNode individual);

}

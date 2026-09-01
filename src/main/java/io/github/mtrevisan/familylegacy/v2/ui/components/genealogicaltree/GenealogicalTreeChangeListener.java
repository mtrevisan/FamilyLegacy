package io.github.mtrevisan.familylegacy.v2.ui.components.genealogicaltree;


@FunctionalInterface
public interface GenealogicalTreeChangeListener{

	void onTreeStructureChanged(String rootIndividualId);

}

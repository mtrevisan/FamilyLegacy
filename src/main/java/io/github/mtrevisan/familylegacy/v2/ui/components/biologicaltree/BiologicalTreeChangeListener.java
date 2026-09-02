package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;


@FunctionalInterface
public interface BiologicalTreeChangeListener{

	void onTreeStructureChanged(String rootIndividualId);

}

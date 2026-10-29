package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.components.searches.projections.ProjectionSelectionViews;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.SelectionViewProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SelectionViewProviderTest{

	/** The selection dialog does not name the views of the projections: it finds them, so the provider has to be declared. */
	@Test
	void theSelectionDialogFindsTheViewsOfTheProjections(){
		Assertions.assertInstanceOf(ProjectionSelectionViews.class, SelectionViewProvider.find());
	}

}

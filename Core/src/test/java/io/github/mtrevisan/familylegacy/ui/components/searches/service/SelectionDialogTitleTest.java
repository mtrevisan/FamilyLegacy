/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;


/** The selection dialog says what is being chosen, for every kind of record: never a placeholder. */
class SelectionDialogTitleTest{

	@SuppressWarnings("unchecked")
	private static String titleOf(final Class<? extends io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler<?>> type)
			throws Exception{
		final AtomicReference<String> title = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			final RecordSelectionDialog dialog = RecordSelectionDialog.create(null, new FLEFModel(), (record, handler) -> {}, type);
			title.set(dialog.getTitle());
			dialog.dispose();
		});
		return title.get();
	}


	@Test
	void theKindsWithoutATitleOfTheirOwnAreNamedByTheirLabel() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");

		//the menu Place > Jurisdiction Timeline asks for a place: its dialog had the title "--"
		Assertions.assertEquals("Select Place", titleOf(PlaceHandler.class));
		Assertions.assertEquals("Select Group", titleOf(GroupHandler.class));
		Assertions.assertEquals("Select Cultural Norm", titleOf(CulturalNormHandler.class));
		Assertions.assertEquals("Select Historic Event", titleOf(HistoricEventHandler.class));
	}

	@Test
	void theKindsWithATitleKeepIt() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");

		Assertions.assertEquals("Select Record", titleOf(IndividualHandler.class));
	}

	@Test
	void withNoTypeChosenTheTitleIsTheGenericOne(){
		Assertions.assertEquals("Select Record", RecordSelectionDialog.selectionTitle(null));
		Assertions.assertEquals("Select Place", RecordSelectionDialog.selectionTitle(PlaceHandler.getInstance()));
	}

}

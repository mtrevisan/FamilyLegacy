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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.menus.EgoNetworkIndividualPopupMenuFactory;
import io.github.mtrevisan.familylegacy.ui.components.projections.menus.EntityTreePopupMenuFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.lang.reflect.Proxy;


/** The popup menus of an empty individual box: nothing that reads the individual may be enabled, or it would act on no record. */
class IndividualPopupMenusTest{

	private static IndividualListener listener(){
		return (IndividualListener)Proxy.newProxyInstance(IndividualListener.class.getClassLoader(),
			new Class<?>[]{IndividualListener.class}, (proxy, method, args) -> null);
	}

	private static JPopupMenu opened(final JPopupMenu popup){
		for(final PopupMenuListener l : popup.getPopupMenuListeners())
			l.popupMenuWillBecomeVisible(new PopupMenuEvent(popup));
		return popup;
	}

	private static void assertOnlyAddingItemsEnabled(final JPopupMenu popup){
		for(final MenuElement element : popup.getSubElements())
			if(element instanceof JMenuItem item)
				Assertions.assertFalse(item.isEnabled() && !mayActOnEmptyBox(item.getText()),
					"enabled on an empty box: " + item.getText());
	}

	/** Adding, connecting and pasting are the only things offered on an empty box (paste stays disabled when the clipboard is empty). */
	private static boolean mayActOnEmptyBox(final String text){
		final String lower = text.toLowerCase();
		return (lower.contains("add") || lower.contains("connect") || lower.contains("paste")) && !lower.contains("child");
	}

	@Test
	void theTreeMenuOffersNothingToReadOnAnEmptyBox(){
		final FLEFModel model = new FLEFModel();
		final IndividualPanel panel = IndividualPanel.createEmpty(BoxPanelType.PRIMARY);

		assertOnlyAddingItemsEnabled(opened(new EntityTreePopupMenuFactory().createPopupMenu(panel, listener(), model)));
	}

	@Test
	void theEgoNetworkMenusOfferNothingToReadOnAnEmptyBox(){
		final FLEFModel model = new FLEFModel();
		final IndividualPanel panel = IndividualPanel.createEmpty(BoxPanelType.PRIMARY);

		assertOnlyAddingItemsEnabled(opened(EgoNetworkIndividualPopupMenuFactory.createForEgo()
			.createPopupMenu(panel, listener(), model)));
		assertOnlyAddingItemsEnabled(opened(EgoNetworkIndividualPopupMenuFactory.createForChild()
			.createPopupMenu(panel, listener(), model)));
	}

}

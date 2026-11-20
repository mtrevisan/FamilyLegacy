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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.components.projections.menus.EgoNetworkIndividualPopupMenuFactory;
import io.github.mtrevisan.familylegacy.ui.components.projections.menus.EntityTreePopupMenuFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;


/** In a menu (the menu bar, a menu, a submenu, a popup) no two entries have the same mnemonic: the key would reach only one of them. */
class MenuMnemonicsTest{

	@Test
	void noTwoMenusOfTheBarShareTheMnemonic() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");

		final List<String> duplicates = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final FamilyLegacyFrame frame = new FamilyLegacyFrame(TreeType.FAMILY, new FLEFModel());
			frame.setTerminator(() -> {});
			final JMenuBar bar = frame.getJMenuBar();

			final List<JMenuItem> tops = new ArrayList<>();
			for(int i = 0; i < bar.getMenuCount(); i ++)
				tops.add(bar.getMenu(i));
			collect("menu bar", tops, duplicates);
			for(final JMenuItem top : tops)
				walk((JMenu)top, top.getText(), duplicates);
		});

		Assertions.assertTrue(duplicates.isEmpty(), "duplicate mnemonics: " + duplicates);
	}

	@Test
	void noTwoEntriesOfAnIndividualPopupShareTheMnemonic() throws Exception{
		final FLEFModel model = new FLEFModel();
		final IndividualPanel panel = IndividualPanel.createEmpty(BoxPanelType.PRIMARY);
		final IndividualListener listener = (IndividualListener)Proxy.newProxyInstance(IndividualListener.class.getClassLoader(),
			new Class<?>[]{IndividualListener.class}, (proxy, method, args) -> null);

		final List<String> duplicates = new ArrayList<>();
		check("tree popup", new EntityTreePopupMenuFactory().createPopupMenu(panel, listener, model), duplicates);
		check("ego popup", EgoNetworkIndividualPopupMenuFactory.createForEgo().createPopupMenu(panel, listener, model), duplicates);
		check("child popup", EgoNetworkIndividualPopupMenuFactory.createForChild().createPopupMenu(panel, listener, model), duplicates);

		Assertions.assertTrue(duplicates.isEmpty(), "duplicate mnemonics: " + duplicates);
	}


	private static void check(final String where, final JPopupMenu popup, final List<String> duplicates){
		for(final PopupMenuListener listener : popup.getPopupMenuListeners())
			listener.popupMenuWillBecomeVisible(new PopupMenuEvent(popup));

		final List<JMenuItem> items = new ArrayList<>();
		for(final Component component : popup.getComponents())
			if(component instanceof JMenuItem item)
				items.add(item);
		collect(where, items, duplicates);
		for(final JMenuItem item : items)
			if(item instanceof JMenu menu)
				walk(menu, where + " > " + menu.getText(), duplicates);
	}

	private static void walk(final JMenu menu, final String where, final List<String> duplicates){
		//a menu that rebuilds its entries when it opens is opened first
		for(final MenuListener listener : menu.getMenuListeners())
			listener.menuSelected(new MenuEvent(menu));

		final List<JMenuItem> items = new ArrayList<>();
		for(int i = 0; i < menu.getItemCount(); i ++)
			if(menu.getItem(i) != null)
				items.add(menu.getItem(i));
		collect(where, items, duplicates);
		for(final JMenuItem item : items)
			if(item instanceof JMenu sub)
				walk(sub, where + " > " + sub.getText(), duplicates);
	}

	private static void collect(final String where, final List<? extends JMenuItem> items, final List<String> duplicates){
		final List<Integer> seen = new ArrayList<>();
		for(final JMenuItem item : items){
			final int mnemonic = Character.toUpperCase(item.getMnemonic());
			if(mnemonic == 0)
				continue;

			if(seen.contains(mnemonic))
				duplicates.add(where + ": '" + (char)mnemonic + "' on " + item.getText());
			seen.add(mnemonic);
		}
	}

}

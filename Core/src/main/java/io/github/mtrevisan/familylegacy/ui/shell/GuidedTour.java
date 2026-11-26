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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.GuidedTourDialog;
import org.apache.commons.lang3.Strings;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.awt.Component;
import java.util.List;


/**
 * The guided tour of the first steps (a file, the people, the events and dates, the participants, the sources, the views, undo): it
 * starts by itself the first time the application runs, and again from <em>Help &gt; Guided Tour</em>.
 */
final class GuidedTour{

	private static final String ELLIPSIS = "…";

	private final FamilyLegacyFrame frame;


	GuidedTour(final FamilyLegacyFrame frame){
		this.frame = frame;
	}


	/** Starts the tour, whenever the user asks for it. */
	void start(){
		AppPreferences.getInstance()
			.setTourShown(true);
		GuidedTourDialog.show(frame, steps(), null);
	}

	/** Starts the tour if it was never shown (and remembers it: it is offered once); call it on the event thread. */
	void startOnce(){
		if(!AppPreferences.getInstance().tourShown())
			start();
	}

	private List<GuidedTourDialog.Step> steps(){
		final String help = menu("menu.help");
		final String tour = item("menu.help.guided.tour");
		return List.of(
			step(1, null, help, tour),
			step(2, "menu.file", menu("menu.file"), item("menu.file.new.file"), item("menu.file.open.file"), item("menu.file.import")),
			step(3, "menu.individual", menu("menu.individual"), item("menu.individual.add.individual"),
				item("menu.individual.add.parent"), item("menu.individual.add.partner"), item("menu.individual.add.child")),
			step(4, "menu.event", menu("menu.event"), item("menu.event.new.event")),
			step(5, "menu.event", menu("menu.event"), item("menu.event.manage.participants")),
			step(6, "menu.source", menu("menu.source"), item("menu.source.new.repository"), item("menu.source.new.source"),
				item("menu.source.add.citation")),
			step(7, "menu.view", menu("menu.view"), item("menu.view.show.sidebar")),
			step(8, "menu.edit", menu("menu.edit"), item("menu.edit.undo"), item("menu.edit.redo"), item("menu.edit.recent_changes")),
			step(9, "menu.help", help, help, tour)
		);
	}

	/** The step {@code number}, whose texts take the given arguments, outlining the menu with the given key (none if {@code null}). */
	private GuidedTourDialog.Step step(final int number, final String menuKey, final Object... arguments){
		return new GuidedTourDialog.Step(I18N.t("dialog.tour.step." + number + ".title"),
			I18N.tf("dialog.tour.step." + number + ".text", arguments), (menuKey != null? () -> findMenu(menuKey): null));
	}

	/** The menu bar is built again when the level of the interface changes: the menu is looked for when its step is shown. */
	private Component findMenu(final String key){
		final JMenuBar bar = frame.getJMenuBar();
		if(bar == null)
			return null;

		final String name = I18N.t(key);
		for(int i = 0; i < bar.getMenuCount(); i ++){
			final JMenu menu = bar.getMenu(i);
			if(menu != null && name.equals(menu.getText()))
				return menu;
		}
		return null;
	}

	private static String menu(final String key){
		return I18N.t(key);
	}

	/** The label of a menu item, as it is written in the menu, without the dots that say it opens a dialog. */
	private static String item(final String key){
		return Strings.CS.removeEnd(I18N.t(key), ELLIPSIS);
	}

}

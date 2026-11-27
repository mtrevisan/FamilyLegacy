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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.i18n.I18N;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Lets the user pick a record for a text field with the right button of the mouse (<em>Set...</em>), as every reference of the editors
 * is picked, with no button beside the field. A field that only shows the choice (it cannot be typed in) is also picked with a double-click.
 */
public final class PickMenu{

	private PickMenu(){}


	/**
	 * @param field	The field: while it is empty it says that the right button picks (the same hint of the references of the editors, or one
	 * 	that says that it can also be typed in).
	 * @param pick	What to do to pick the record.
	 * @param clear	What to do to empty the field, {@code null} if the field has no <em>Clear</em>.
	 */
	public static void install(final HintTextField field, final Runnable pick, final Runnable clear){
		field.setHint(I18N.t(field.isEditable()? "placeholder.type_or_set": "placeholder.text"));
		install((JComponent)field, pick, clear);
	}

	/**
	 * @param field	The field.
	 * @param pick	What to do to pick the record.
	 * @param clear	What to do to empty the field, {@code null} if the field has no <em>Clear</em>.
	 */
	public static void install(final JComponent field, final Runnable pick, final Runnable clear){
		final JPopupMenu menu = new JPopupMenu();
		final JMenuItem set = new JMenuItem(I18N.t("popupmenu.set"));
		set.addActionListener(e -> pick.run());
		menu.add(set);
		if(clear != null){
			final JMenuItem clearItem = new JMenuItem(I18N.t("popupmenu.clear"));
			clearItem.addActionListener(e -> clear.run());
			menu.add(clearItem);
		}

		field.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				show(e);
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				show(e);
			}

			private void show(final MouseEvent e){
				if(e.isPopupTrigger() && field.isEnabled())
					menu.show(field, e.getX(), e.getY());
			}

			@Override
			public void mouseClicked(final MouseEvent e){
				//a field that cannot be typed in is picked with a double-click; one that can, uses it to select a word
				if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && field.isEnabled()
						&& field instanceof JTextComponent text && !text.isEditable())
					pick.run();
			}
		});
	}

}

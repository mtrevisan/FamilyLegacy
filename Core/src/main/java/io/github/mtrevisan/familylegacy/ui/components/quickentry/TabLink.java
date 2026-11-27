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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;


/**
 * A note of the quick entry that sends the user to another tab of the editor ("see the Participations tab"): it shows as a text, and
 * becomes a link once the editor tells where to go ({@link #setNavigator}); the click opens the tab with the entry selected.
 */
public final class TabLink extends JLabel{

	/** Goes to a tab of the editor and selects an entry of its lists. */
	@FunctionalInterface
	public interface Navigator{

		/**
		 * @param tab	The tab ({@code EditorTab} name).
		 * @param entry	The entry to select ({@code null}: only the tab).
		 */
		void show(String tab, FLEFRecord entry);

	}


	private final String tab;
	private FLEFRecord entry;
	private Navigator navigator;


	/** @param tab	The tab the link leads to. */
	TabLink(final String tab){
		this.tab = tab;

		setVisible(false);
		setForeground(UIManager.getColor("Label.disabledForeground"));
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(navigator != null && entry != null)
					navigator.show(TabLink.this.tab, entry);
			}
		});
	}


	/** Shows the note, leading to the entry (the note is plain text if there is no navigator or no entry). */
	void show(final String text, final FLEFRecord entry){
		this.entry = entry;
		setText(text);
		refreshLook();
		setVisible(true);
	}

	void setNavigator(final Navigator navigator){
		this.navigator = navigator;
		refreshLook();
	}

	private void refreshLook(){
		final boolean link = (navigator != null && entry != null);
		final Font font = getFont();
		if(font != null)
			setFont(font.deriveFont(link? Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON): Map.of(TextAttribute.UNDERLINE, -1)));
		final Color linkColor = UIManager.getColor("Component.linkColor");
		setForeground(link? (linkColor != null? linkColor: new Color(0x2A6BD6)): UIManager.getColor("Label.disabledForeground"));
		setCursor(Cursor.getPredefinedCursor(link? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
	}

}

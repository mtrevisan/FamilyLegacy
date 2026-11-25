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

import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;


/**
 * A label (or a check box, a button) whose text does not fit in the room it has is cut by Swing: the mouse over it shows the whole text as
 * a tooltip, in every window of the application; so is the selected item of a combo box, and the item of a list (the one of the list of a combo box)
 * that is cut. If the component has a tooltip already, the text comes first, then the tooltip.
 */
public final class ClippedTextTooltips{

	/** The tooltip the component had before this class added the text, stored with the component while the text is added. */
	private static final String ORIGINAL = "clippedTextTooltips.original";
	private static final Object NO_TOOLTIP = new Object();

	private static boolean installed;


	private ClippedTextTooltips(){}


	/** Shows the whole text of the cut labels as a tooltip, in the whole application (once: calling it again does nothing). */
	public static synchronized void install(){
		if(installed)
			return;

		installed = true;
		Toolkit.getDefaultToolkit()
			.addAWTEventListener(ClippedTextTooltips::onEvent, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}

	private static void onEvent(final AWTEvent event){
		final int id = event.getID();
		if(id != MouseEvent.MOUSE_ENTERED && id != MouseEvent.MOUSE_MOVED || !(event.getSource() instanceof final JComponent component))
			return;

		if(component instanceof final JList<?> list)
			updateList(list, ((MouseEvent)event).getPoint());
		else
			update(component);
	}


	/** Adds the whole text to the tooltip of the component if the text is cut, takes it away if it is not (the window was resized). */
	static void update(final JComponent component){
		final String text = textOf(component);
		if(text == null || component instanceof ComposesOwnTooltip)
			return;

		final boolean clipped = isClipped(component);
		final boolean added = (component.getClientProperty(ORIGINAL) != null);
		if(clipped && !added){
			final String original = component.getToolTipText();
			component.putClientProperty(ORIGINAL, (original != null? original: NO_TOOLTIP));
			component.setToolTipText(withText(text, original));
		}
		else if(!clipped && added){
			final Object original = component.getClientProperty(ORIGINAL);
			component.putClientProperty(ORIGINAL, null);
			component.setToolTipText(original == NO_TOOLTIP? null: (String)original);
		}
	}

	/** The tooltip of a list is the whole text of the item under the mouse, if that is cut (and the one of the list otherwise). */
	static void updateList(final JList<?> list, final Point point){
		if(list instanceof ComposesOwnTooltip)
			return;

		final int index = list.locationToIndex(point);
		String text = null;
		if(index >= 0 && list.getCellBounds(index, index) != null && list.getCellBounds(index, index).contains(point)){
			final Component cell = rendered(list, index);
			if(cell != null && list.getWidth() > 0 && cell.getPreferredSize().width > list.getWidth())
				text = textOf(cell);
		}
		final boolean added = (list.getClientProperty(ORIGINAL) != null);
		final Object original = (added? list.getClientProperty(ORIGINAL): null);
		if(text != null){
			final String own = (added? (original == NO_TOOLTIP? null: (String)original): list.getToolTipText());
			if(!added)
				list.putClientProperty(ORIGINAL, (own != null? own: NO_TOOLTIP));
			list.setToolTipText(withText(text, own));
		}
		else if(added){
			list.putClientProperty(ORIGINAL, null);
			list.setToolTipText(original == NO_TOOLTIP? null: (String)original);
		}
	}

	private static Component rendered(final JList<?> list, final int index){
		@SuppressWarnings("unchecked")
		final javax.swing.ListCellRenderer<Object> renderer = (javax.swing.ListCellRenderer<Object>)list.getCellRenderer();
		return (renderer != null? renderer.getListCellRendererComponent((JList<Object>)list, list.getModel().getElementAt(index), index, false, false): null);
	}

	/** @return	The text of a label, of a button or of the selected item of a combo box, {@code null} if it has none. */
	private static String textOf(final JComponent component){
		final String text = switch(component){
			case final JLabel label -> label.getText();
			case final AbstractButton button -> button.getText();
			case final JComboBox<?> combo -> selectedText(combo);
			default -> null;
		};
		return (StringUtils.isNotBlank(text)? text: null);
	}

	private static String textOf(final Component component){
		return (component instanceof final JLabel label? label.getText(): null);
	}

	/** @return	The text of the selected item of a combo box as its renderer writes it, {@code null} if it is not a text. */
	private static String selectedText(final JComboBox<?> combo){
		if(combo.isEditable() || combo.getSelectedItem() == null)
			return null;

		return textOf(renderedSelection(combo));
	}

	private static Component renderedSelection(final JComboBox<?> combo){
		@SuppressWarnings("unchecked")
		final javax.swing.ListCellRenderer<Object> renderer = (javax.swing.ListCellRenderer<Object>)combo.getRenderer();
		return (renderer != null
			? renderer.getListCellRendererComponent(new JList<>(), combo.getSelectedItem(), -1, false, false): null);
	}

	/** @return	Whether the component is narrower than its text needs. */
	static boolean isClipped(final JComponent component){
		final int width = component.getWidth();
		if(component instanceof final JComboBox<?> combo){
			//(the arrow takes about as much as the height of the box)
			final Component selected = renderedSelection(combo);
			return (width > 0 && selected != null && selected.getPreferredSize().width > width - combo.getHeight());
		}
		return (width > 0 && width < component.getPreferredSize().width);
	}

	/** @return	The tooltip with the text on top, in bold, and the tooltip the component had under it. */
	static String withText(final String text, final String original){
		final String shown = (StringUtils.startsWithIgnoreCase(text, "<html>")? plain(text): StringUtils.replaceEach(text,
			new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"}));
		if(StringUtils.isBlank(original))
			return "<html>" + shown + "</html>";

		final String rest = (StringUtils.startsWithIgnoreCase(original, "<html>")
			? StringUtils.removeEndIgnoreCase(original.substring("<html>".length()), "</html>")
			: StringUtils.replaceEach(original, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"}));
		return "<html><b>" + shown + "</b><br>" + rest + "</html>";
	}

	/** @return	The content of an HTML text, without the enclosing tags. */
	private static String plain(final String html){
		return StringUtils.removeEndIgnoreCase(html.substring("<html>".length()), "</html>");
	}

}

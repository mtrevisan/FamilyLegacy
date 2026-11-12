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
package io.github.mtrevisan.familylegacy.ui.preferences;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.Set;


/**
 * Dialog to choose the application font: a filter field, the list of installed families (each
 * rendered in its own font), the size and a preview. Closes with ESC (cancel).
 */
final class FontChooserDialog extends JDialog{

	private static final String SAMPLE = "The quick brown fox jumps over the lazy dog 0123456789";


	private final DefaultListModel<String> model = new DefaultListModel<>();
	private final JList<String> list = new JList<>(model);
	private final SearchBox filter = new SearchBox(24, false);
	private final JSpinner size;
	private final JLabel preview = new JLabel(SAMPLE);
	private final String[] families;


	private FontChooserDialog(final Window owner, final Font current){
		super(owner, I18N.t("menu.appearance.font"), ModalityType.APPLICATION_MODAL);

		families = AppearanceManager.fontFamiliesCoveringMessages();
		size = new JSpinner(new SpinnerNumberModel(current != null? current.getSize(): 12, 8, 48, 1));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWW");
		list.setVisibleRowCount(14);
		list.setCellRenderer((l, value, index, selected, focus) -> {
			final JLabel label = new JLabel(value);
			label.setOpaque(true);
			label.setBorder(new EmptyBorder(1, 4, 1, 4));
			label.setFont(new Font(value, Font.PLAIN, 14));
			label.setBackground(selected? l.getSelectionBackground(): l.getBackground());
			label.setForeground(selected? l.getSelectionForeground(): l.getForeground());
			return label;
		});
		list.addListSelectionListener(e -> updatePreview());
		size.addChangeListener(e -> updatePreview());
		filter.onChange(() -> refill(null));

		refill(current != null? current.getFamily(): null);

		preview.setBorder(new EmptyBorder(8, 8, 8, 8));
		final JScrollPane previewPane = new JScrollPane(preview, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		previewPane.setPreferredSize(new Dimension(420, 70));

		final JPanel top = new JPanel(new BorderLayout(6, 0));
		top.add(filter, BorderLayout.CENTER);
		top.add(size, BorderLayout.EAST);

		final JPanel center = new JPanel(new BorderLayout(0, 6));
		center.setBorder(new EmptyBorder(8, 8, 0, 8));
		center.add(top, BorderLayout.NORTH);
		center.add(new JScrollPane(list), BorderLayout.CENTER);
		center.add(previewPane, BorderLayout.SOUTH);

		final JPanel buttons = GUIHelper.createButtonPanel(this, I18N.t("button.select"), this::accept,
			this::dispose);
		final JButton defaultButton = new JButton(I18N.t("checkbox.default"));
		defaultButton.addActionListener(e -> {
			AppearanceManager.applyFont(null);
			dispose();
		});
		buttons.add(defaultButton, 0);

		add(center, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}


	/** Shows the dialog; the chosen font is applied and persisted, if any. */
	static void show(final Window owner){
		final FontChooserDialog dialog = new FontChooserDialog(owner, effectiveFont());
		dialog.filter.field().requestFocusInWindow();
		dialog.setVisible(true);
	}


	/** The font in use now: the saved one or, with none saved (Default), the one of the look-and-feel. */
	private static Font effectiveFont(){
		final Font saved = AppearanceManager.savedFont();
		return saved != null? saved: javax.swing.UIManager.getFont("Label.font");
	}

	private void refill(final String toSelect){
		final String text = filter.query();
		final Set<SearchMode> modes = filter.modes();
		final String keep = (toSelect != null? toSelect: list.getSelectedValue());
		model.clear();
		for(final String family : families)
			if(SearchBox.matches(text, modes, family))
				model.addElement(family);
		if(keep != null && model.contains(keep)){
			list.setSelectedValue(keep, true);
		}
		else if(!model.isEmpty())
			list.setSelectedIndex(0);
		updatePreview();
	}

	private void updatePreview(){
		final String family = list.getSelectedValue();
		if(family != null)
			preview.setFont(new Font(family, Font.PLAIN, (Integer)size.getValue()));
	}

	private void accept(){
		final String family = list.getSelectedValue();
		if(family != null)
			AppearanceManager.applyFont(new Font(family, Font.PLAIN, (Integer)size.getValue()));
		dispose();
	}

}

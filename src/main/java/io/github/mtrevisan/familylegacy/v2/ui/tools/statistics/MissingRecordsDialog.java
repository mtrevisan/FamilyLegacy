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
package io.github.mtrevisan.familylegacy.v2.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * Shows the records that fall into a completeness category. Each row
 * can be double-clicked to open the record's edit dialog; after a
 * successful edit, the row is refreshed with the new display text.
 * <p>
 * The dialog is used by the statistics result dialog to turn the
 * "Without birth date" style rows into a working list the user can act
 * on.
 */
final class MissingRecordsDialog extends JDialog{

	private final FLEFModel model;
	private final DefaultListModel<FLEFRecord> listModel = new DefaultListModel<>();
	private final JList<FLEFRecord> list = new JList<>(listModel);


	MissingRecordsDialog(final Window owner, final String category,
		final FLEFModel model, final List<FLEFRecord> records){
		super(owner, category, ModalityType.APPLICATION_MODAL);
		this.model = model;

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new DisplayTextRenderer());
		for(final FLEFRecord r : records)
			listModel.addElement(r);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					editSelected();
			}
		});

		final JLabel count = new JLabel(records.size()
			+ (records.size() == 1? " record": " records"));

		final JButton edit = new JButton("Open record");
		edit.addActionListener(e -> editSelected());
		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(count, BorderLayout.WEST);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(edit);
		buttons.add(close);
		bottom.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout(8, 8));
		final JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(scroll, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(700, 480));
		pack();
		setLocationRelativeTo(owner);
	}


	private void editSelected(){
		final FLEFRecord selected = list.getSelectedValue();
		if(selected == null)
			return;

		final String tag = selected.getTag();
		final RecordTypeHandler<?> handler = (tag != null? HandlerRegistry.getHandler(tag): null);
		if(handler == null){
			JOptionPane.showMessageDialog(this,
				"No editor registered for record type '" + tag + "'.",
				"Statistics", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(this, model, selected);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			// Refresh the display text of the edited row.
			final int index = list.getSelectedIndex();
			if(index >= 0)
				listModel.set(index, selected);
		}
	}


	private final class DisplayTextRenderer extends DefaultListCellRenderer{
		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value,
			final int index, final boolean isSelected, final boolean cellHasFocus){
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if(value instanceof FLEFRecord r){
				final String tag = r.getTag();
				final RecordTypeHandler<?> handler = (tag != null? HandlerRegistry.getHandler(tag): null);
				final String text = (handler != null
					? handler.getDisplayText(r, model)
					: r.getId());
				setText(text);
				setToolTipText(text);
			}
			return this;
		}
	}

}

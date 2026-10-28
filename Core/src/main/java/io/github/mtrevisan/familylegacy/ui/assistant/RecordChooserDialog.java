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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * The records of a finding, to open one after the other: the list stays on the screen while an editor is open, so that the
 * researcher can compare the records of a conflict. ESC closes it.
 */
final class RecordChooserDialog extends JDialog{

	/** What to do to open a record (the usual editor). */
	@FunctionalInterface
	interface Opener{

		void open(Window owner, FLEFModel model, FLEFRecord record);

	}


	private final FLEFModel model;
	private final Opener opener;

	private final DefaultListModel<FLEFRecord> recordsModel = new DefaultListModel<>();
	private final JList<FLEFRecord> recordsList = new JList<>(recordsModel);
	private final JButton openButton = new JButton(I18N.t("assistant.open_records.open"));


	RecordChooserDialog(final Window owner, final FLEFModel model, final List<FLEFRecord> records, final Opener opener){
		super(owner, I18N.t("assistant.open_records"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.opener = opener;

		records.forEach(recordsModel::addElement);
		initComponents();

		pack();
		setLocationRelativeTo(owner);
	}


	private void initComponents(){
		recordsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//the text is computed when painted, so that it shows what an editor has just changed
		recordsList.setCellRenderer((list, value, index, selected, focus) -> {
			final JLabel label = new JLabel(OpenRecordsAction.describe(model, value));
			label.setOpaque(true);
			label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
			return label;
		});
		recordsList.addListSelectionListener(e -> openButton.setEnabled(recordsList.getSelectedValue() != null));
		recordsList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSelected();
			}
		});
		recordsList.setSelectedIndex(0);

		openButton.addActionListener(e -> openSelected());
		getRootPane().setDefaultButton(openButton);
		//ESC closes the list, from any component, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout("fill,wrap 1", "[grow,fill,400::]", "[]5[grow,fill]5[]"));
		add(new JLabel(I18N.t("assistant.open_records.hint")));
		add(new JScrollPane(recordsList));
		add(openButton, "align right");
	}

	/** Opens the editor of the selected record; the list stays, to open another one afterward. */
	void openSelected(){
		final FLEFRecord record = recordsList.getSelectedValue();
		if(record == null)
			return;

		opener.open(this, model, record);
		recordsList.repaint();
	}

	/** @return the records of the list */
	List<FLEFRecord> records(){
		return java.util.Collections.list(recordsModel.elements());
	}

	void select(final int index){
		recordsList.setSelectedIndex(index);
	}

}

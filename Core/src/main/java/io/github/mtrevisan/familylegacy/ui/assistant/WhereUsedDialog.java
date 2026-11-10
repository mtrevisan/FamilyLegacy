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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.RecordReferences;
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
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Where the records of a finding are used: for each record, the records that point to it and by which field. The list stays on the
 * screen while an editor is open, so that the researcher can follow the references of a duplicate before deciding what to do with it.
 * ESC closes it.
 */
final class WhereUsedDialog extends JDialog{

	/** A line of the list: a record of the finding, or one of the records that point to it. */
	sealed interface Row permits Subject, Use, None{}

	/** A record of the finding, with the number of records that point to it. */
	record Subject(FLEFRecord record, int uses) implements Row{}

	/** A record that points to the record of the finding above it, by the given field. */
	record Use(FLEFRecord owner, String path) implements Row{}

	/** The record above is not pointed to by any other. */
	record None() implements Row{}


	private final FLEFModel model;

	private final DefaultListModel<Row> rowsModel = new DefaultListModel<>();
	private final JList<Row> rowsList = new JList<>(rowsModel);
	private final JButton openButton = new JButton(I18N.t("assistant.open_records.open"));


	WhereUsedDialog(final Window owner, final FLEFModel model, final List<FLEFRecord> records){
		super(owner, I18N.t("assistant.where_used"), ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.model = model;

		for(final Row row : rows(model, records))
			rowsModel.addElement(row);
		initComponents();

		pack();
		setLocationRelativeTo(owner);
	}


	/** @return the lines for the records: each record, then the records that point to it (or that nobody does) */
	static List<Row> rows(final FLEFModel model, final List<FLEFRecord> records){
		final List<String> ids = records.stream()
			.map(FLEFRecord::getId)
			.toList();
		final Map<String, List<RecordReferences.Reference>> references = RecordReferences.to(model, ids);
		final List<Row> rows = new ArrayList<>();
		for(final FLEFRecord record : records){
			final List<RecordReferences.Reference> uses = references.get(record.getId());
			rows.add(new Subject(record, uses.size()));
			if(uses.isEmpty())
				rows.add(new None());
			else
				for(final RecordReferences.Reference use : uses)
					rows.add(new Use(use.owner(), use.path()));
		}
		return rows;
	}

	private void initComponents(){
		rowsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//the text is computed when painted, so that it shows what an editor has just changed
		rowsList.setCellRenderer((list, value, index, selected, focus) -> {
			final JLabel label = new JLabel();
			label.setOpaque(true);
			label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
			switch(value){
				case Subject s -> {
					label.setText(I18N.tf("assistant.where_used.subject", OpenRecordsAction.describe(model, s.record()), s.uses()));
					label.setFont(label.getFont().deriveFont(Font.BOLD));
				}
				case Use u -> label.setText("    " + I18N.tf("assistant.where_used.use", OpenRecordsAction.describe(model, u.owner()), u.path()));
				case None n -> {
					label.setText("    " + I18N.t("assistant.where_used.none"));
					if(!selected)
						label.setForeground(UIManager.getColor("Label.disabledForeground"));
				}
			}
			return label;
		});
		rowsList.addListSelectionListener(e -> openButton.setEnabled(!(rowsList.getSelectedValue() instanceof None)
			&& rowsList.getSelectedValue() != null));
		rowsList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSelected();
			}
		});
		rowsList.setSelectedIndex(0);

		openButton.addActionListener(e -> openSelected());
		getRootPane().setDefaultButton(openButton);
		//ESC closes the list, from any component, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout("fill,wrap 1", "[grow,fill,500::]", "[]5[grow,fill,200::]5[]"));
		add(new JLabel(I18N.t("assistant.where_used.hint")));
		add(new JScrollPane(rowsList));
		add(openButton, "align right");
	}

	/** Opens the editor of the selected record (the one of the finding, or the one that points to it); the list stays. */
	void openSelected(){
		final FLEFRecord record = switch(rowsList.getSelectedValue()){
			case Subject s -> s.record();
			case Use u -> u.owner();
			case null, default -> null;
		};
		//only a record that has an editor can be opened
		if(record == null || OpenRecordsAction.records(model, List.of(record.getId())).isEmpty())
			return;

		OpenRecordsAction.open(this, model, record);
		rowsList.repaint();
	}

	/** @return the lines of the list */
	List<Row> rows(){
		return Collections.list(rowsModel.elements());
	}

	void select(final int index){
		rowsList.setSelectedIndex(index);
	}

}

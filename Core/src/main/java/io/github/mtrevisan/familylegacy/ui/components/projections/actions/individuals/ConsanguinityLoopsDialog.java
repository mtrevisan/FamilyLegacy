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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityLoops;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Shows why an individual is inbred: one row for each consanguinity loop, that is each common ancestor of the two parents and the two
 * lines of descent from it to them (written from the ancestor down to the parent), with what the loop adds to the coefficient of
 * inbreeding. A double-click, or "Open ancestor", opens the common ancestor.
 * <p>
 * It only reads the model: nothing is changed.
 */
final class ConsanguinityLoopsDialog extends JDialog{

	private static final String ARROW = " → ";

	private final FLEFModel model;
	private final List<String> ancestorIds = new ArrayList<>();
	private final JTable table;
	private final JButton openAncestor = new JButton(I18N.t("button.open_ancestor"));


	ConsanguinityLoopsDialog(final Window owner, final FLEFModel model, final ConsanguinityLoops.Result result){
		super(owner, I18N.tf("dialog.consanguinity_loops.title", describe(model, result.individualId())), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;

		final List<Object[]> rows = rowsOf(model, result, ancestorIds);
		final RowTableModel tableModel = new RowTableModel(new String[]{I18N.t("column.loops.ancestor"),
			I18N.t("column.loops.first"), I18N.t("column.loops.second"), I18N.t("column.loops.generations"),
			I18N.t("column.loops.contribution")}, rows);
		table = ToolDialogs.createTable(tableModel, this::openAncestorAt);
		table.setAutoCreateRowSorter(false);
		final int[] widths = {170, 300, 300, 90, 90};
		for(int column = 0; column < widths.length; column ++)
			table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
		table.getSelectionModel()
			.addListSelectionListener(e -> openAncestor.setEnabled(table.getSelectedRow() >= 0));
		openAncestor.setEnabled(false);
		openAncestor.addActionListener(e -> openAncestorAt(table.getSelectedRow()));

		final JPanel content = ToolDialogs.createHeaderPanel(summaryOf(result), null);
		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(920, 300));
		content.add(scroll, BorderLayout.CENTER);

		setLayout(new BorderLayout(8, 8));
		add(content, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(I18N.t("text.loops.note"), openAncestor, ToolDialogs.createCopyButton(tableModel::toText),
			ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/** @return	The line above the table: the coefficient and the number of loops, or why there are none. */
	static String summaryOf(final ConsanguinityLoops.Result result){
		if(!result.hasTwoParents())
			return I18N.t("text.loops.no_parents");
		if(result.loops().isEmpty())
			return I18N.t("text.loops.unrelated");

		final String summary = I18N.tf("text.loops.summary", percent(result.inbreeding()), result.loops().size());
		return (result.truncated()? summary + " " + I18N.tf("text.loops.truncated", ConsanguinityLoops.MAX_LOOPS): summary);
	}

	/** @return	The value as a percentage, with the digits that tell the small ones apart (a child of third cousins has F of 0.39 %). */
	static String percent(final double value){
		return GUIHelper.formatDouble(value * 100., (value > 0. && value < 0.001? 4: 2)) + " %";
	}

	/** @return	One row for each loop; the identifier of the ancestor of each row is added to the list. */
	private static List<Object[]> rowsOf(final FLEFModel model, final ConsanguinityLoops.Result result, final List<String> ancestorIds){
		final List<Object[]> rows = new ArrayList<>();
		final String individual = describe(model, result.individualId());
		for(final ConsanguinityLoops.Loop loop : result.loops()){
			rows.add(new Object[]{describe(model, loop.ancestorId()), lineText(model, loop.firstLine(), individual),
				lineText(model, loop.secondLine(), individual), loop.firstGenerations() + " + " + loop.secondGenerations(),
				percent(loop.contribution())});
			ancestorIds.add(loop.ancestorId());
		}
		return rows;
	}

	/** @return	The line written from the ancestor down to the parent and then to the individual: "Anna → Bruno → Carlo → Dario". */
	static String lineText(final FLEFModel model, final List<String> upwardLine, final String individual){
		final List<String> names = new ArrayList<>(upwardLine.size() + 1);
		for(int i = upwardLine.size() - 1; i >= 0; i --)
			names.add(describe(model, upwardLine.get(i)));
		names.add(individual);
		return String.join(ARROW, names);
	}

	private static String describe(final FLEFModel model, final String id){
		final FLEFRecord record = model.getRecordById(id);
		return (record != null? RecordDisplay.describe(record, model): id);
	}

	private void openAncestorAt(final int row){
		if(row < 0)
			return;

		ToolDialogs.openRecordEditor(this, model, model.getRecordById(ancestorIds.get(table.convertRowIndexToModel(row))));
	}

}

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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Generic dialog that merges two records of the same type.
 * <p>
 * The dialog is not tied to a specific record type: it is opened with a
 * {@link RecordTypeHandler} and works on whatever tags and values the
 * FLEF records contain. The actual merge logic lives in
 * {@link RecordMerger}; the dialog only collects the two records, shows
 * the collisions in a table, and lets the user pick a decision per row.
 * <p>
 * Source and target are picked through an {@link EntityField}, which
 * already renders the record using the handler's display text and
 * provides the standard "Set… / Edit… / Clear" context menu used
 * everywhere else in the application. The dialog therefore does not need
 * a separate preview label.
 */
public final class MergeRecordsDialog extends JDialog{

	private static final int ACTION_COLUMN = 3;
	private static final int RESET_COLUMN = 4;


	private final ToolContext context;
	private final RecordMerger merger;

	private final EntityField sourceField;
	private final EntityField targetField;

	private final CollisionsTableModel tableModel = new CollisionsTableModel();
	private final JTable collisionsTable = new JTable(tableModel);

	private MergePlan plan;
	private boolean confirmed;


	public MergeRecordsDialog(final ToolContext context, final Class<? extends RecordTypeHandler<?>> handlerType){
		super(context.owner(), "Merge Records", ModalityType.APPLICATION_MODAL);

		this.context = context;
		this.merger = new RecordMerger(context.model());

		// The two fields own their own popup menu (Set/Edit/Clear) and
		// render the record through the handler's display text. The
		// property change listener keeps the plan in sync with whatever
		// the user chooses, whether through the field menu or through a
		// programmatic call to setSource/setTarget.
		final FLEFModel model = context.model();
		sourceField = EntityField.createForRecordFromReference(null, this, model, handlerType);
		targetField = EntityField.createForRecordFromReference(null, this, model, handlerType);
		sourceField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED,
			e -> rebuildPlan());
		targetField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED,
			e -> rebuildPlan());

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(880, 520));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the source record programmatically. Equivalent to picking a
	 * record through the field's context menu, but skips the selection
	 * dialog. The plan is rebuilt as a side effect.
	 *
	 * @param source the record that will be deleted; may be {@code null}
	 *               to clear the current selection
	 * @return this dialog, for chaining
	 */
	public MergeRecordsDialog withSource(final FLEFRecord source){
		sourceField.setEntity(source);

		return this;
	}

	/**
	 * Sets the target record programmatically. Equivalent to picking a
	 * record through the field's context menu, but skips the selection
	 * dialog. The plan is rebuilt as a side effect.
	 *
	 * @param target the record that will be kept; may be {@code null} to
	 *               clear the current selection
	 * @return this dialog, for chaining
	 */
	public MergeRecordsDialog withTarget(final FLEFRecord target){
		targetField.setEntity(target);

		return this;
	}

	/**
	 * Returns the source record currently selected, or {@code null}.
	 */
	public FLEFRecord getSource(){
		return sourceField.getEntity();
	}

	/**
	 * Returns the target record currently selected, or {@code null}.
	 */
	public FLEFRecord getTarget(){
		return targetField.getEntity();
	}

	/**
	 * Returns whether the user confirmed the merge.
	 */
	public boolean isConfirmed(){
		return confirmed;
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private JPanel createForm(){
		// MigLayout handles the growing column better than GridBagLayout:
		// the two columns are declared once ("[][grow,fill]"), and every
		// field in the second column grows with the dialog. Rows are
		// declared as "[]8[]16[grow,fill]" so the table takes all the
		// remaining vertical space and the two label/field rows stay at
		// their natural height. "wrap 2" makes the layout start a new row
		// after every two components, so the label+field pairs line up
		// without any grid arithmetic.
		final JPanel form = new JPanel(new MigLayout(
			"ins 10,fillx,wrap 2",
			"[][grow,fill]",
			"[]8[]16[grow,fill]"));

		// Source row: label + EntityField. The field's own popup menu
		// (Set…, Edit…, Clear) is the only way to change the selection,
		// so no extra "Choose…" button is needed.
		form.add(new JLabel("Source (to be deleted):"));
		form.add(sourceField, "growx");

		// Target row.
		form.add(new JLabel("Target (to be kept):"));
		form.add(targetField, "growx");

		// Collisions table.
		collisionsTable.setRowHeight(24);
		collisionsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		collisionsTable.getColumnModel().getColumn(1).setPreferredWidth(260);
		collisionsTable.getColumnModel().getColumn(2).setPreferredWidth(260);
		collisionsTable.getColumnModel().getColumn(3).setPreferredWidth(140);
		collisionsTable.getColumnModel().getColumn(4).setPreferredWidth(28);
		collisionsTable.getColumnModel().getColumn(4).setMaxWidth(40);

		collisionsTable.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(
			new DefaultCellEditor(new JComboBox<>(MergeDecision.values())));
		collisionsTable.setDefaultRenderer(Object.class, new ResolvedAwareRenderer(tableModel));

		// Mouse listener: double-click opens the comparison, single-click on
		// the reset column resets the row.
		collisionsTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				final int row = collisionsTable.rowAtPoint(e.getPoint());
				final int col = collisionsTable.columnAtPoint(e.getPoint());
				if(row < 0)
					return;

				if(e.getClickCount() == 2 && col != RESET_COLUMN){
					openComparison(row);
					return;
				}

				if(col == RESET_COLUMN){
					final MergeField f = tableModel.fieldAt(row);
					if(f != null && f.isResolved())
						tableModel.resetRow(row);
				}
			}
		});

		// Keyboard shortcut: Delete on the selected row resets it.
		collisionsTable.getInputMap()
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "reset-row");
		collisionsTable.getActionMap()
			.put("reset-row", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					final int row = collisionsTable.getSelectedRow();
					if(row >= 0){
						final MergeField f = tableModel.fieldAt(row);
						if(f != null && f.isResolved())
							tableModel.resetRow(row);
					}
				}
			});

		final JScrollPane tableScroll = new JScrollPane(collisionsTable);
		tableScroll.setBorder(BorderFactory.createTitledBorder("Collisions"));
		// "span 2" spans both columns; "grow,push" takes the remaining
		// horizontal and vertical space, so the table stretches with the
		// dialog instead of staying at its preferred size.
		form.add(tableScroll, "span 2,grow,push");

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Merge");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton(I18N.t("button.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	/* ======================================================================
	 *                          Plan management
	 * ====================================================================== */

	/**
	 * Rebuilds the merge plan from the current source and target and
	 * refreshes the collisions table. Called whenever either field
	 * changes.
	 */
	private void rebuildPlan(){
		final FLEFRecord source = sourceField.getEntity();
		final FLEFRecord target = targetField.getEntity();

		if(source == null || target == null || source.getId() == null || target.getId() == null){
			plan = null;
			tableModel.setFields(List.of());
			return;
		}
		if(source.getId().equals(target.getId())){
			plan = null;
			tableModel.setFields(List.of());
			return;
		}

		try{
			plan = merger.plan(source.getId(), target.getId());
			tableModel.setFields(plan.collisions());
		}
		catch(final IllegalArgumentException ex){
			plan = null;
			tableModel.setFields(List.of());
		}
	}


	/* ======================================================================
	 *                          Comparison
	 * ====================================================================== */

	/**
	 * Opens the side-by-side comparison dialog for the given collision
	 * row. If the user accepts a choice, the choice is written back into
	 * the table model and the row summary is refreshed.
	 */
	private void openComparison(final int row){
		final MergeField field = tableModel.fieldAt(row);
		if(field == null)
			return;

		final FLEFRecord target = targetField.getEntity();
		final FLEFModel model = context.model();

		final MergeFieldComparisonDialog dialog = new MergeFieldComparisonDialog(this, field,
			target, model);
		dialog.setVisible(true);

		tableModel.refreshRow(row);
	}


	/* ======================================================================
	 *                          Confirm
	 * ====================================================================== */

	private void onConfirm(){
		final FLEFRecord source = sourceField.getEntity();
		final FLEFRecord target = targetField.getEntity();

		if(source == null || target == null){
			warn("Choose both a source and a target.");
			return;
		}
		if(source.getId().equals(target.getId())){
			warn("The source and the target must be different records.");
			return;
		}
		if(plan == null){
			warn("Unable to plan the merge: " + source.getTag() + " vs " + target.getTag());
			return;
		}

		// Push the table's current choices into the plan before applying.
		tableModel.commitChoices(plan);

		if(!confirm())
			return;

		try{
			final RecordMerger.MergeResult result = merger.apply(plan);
			confirmed = true;

			JOptionPane.showMessageDialog(this,
				"Merge completed." + StringUtils.LF
					+ "Fields taken from source: " + result.fieldsTakenFromSource() + StringUtils.LF
					+ "References re-pointed: " + result.referencesRepointed(),
				"Merge Records", JOptionPane.INFORMATION_MESSAGE);

			dispose();
		}
		catch(final IllegalArgumentException ex){
			warn(ex.getMessage());
		}
	}

	private boolean confirm(){
		final FLEFRecord source = sourceField.getEntity();
		final FLEFRecord target = targetField.getEntity();
		final String message = "Merge " + displayOf(source)
			+ " into " + displayOf(target) + "?\n\n"
			+ "The source record will be deleted. All references to it "
			+ "will be re-pointed to the target.";
		final int choice = JOptionPane.showConfirmDialog(this, message,
			"Confirm Merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		return (choice == JOptionPane.YES_OPTION);
	}

	private void warn(final String message){
		JOptionPane.showMessageDialog(this, message,
			"Merge Records", JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Short human-readable identifier of a record, used in confirmation
	 * and error messages. The {@link EntityField} already shows the full
	 * display text; this is only for text that ends up inside a
	 * {@link JOptionPane}.
	 */
	private static String displayOf(final FLEFRecord record){
		if(record == null)
			return "<none>";
		final String id = record.getId();
		return (id != null? "[" + id + "] ": StringUtils.EMPTY) + record.getTag();
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class CollisionsTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Field", "Target", "Source", "Action", StringUtils.EMPTY};

		private final List<MergeField> fields = new ArrayList<>();
		private final List<MergeDecision> chosen = new ArrayList<>();

		void setFields(final List<MergeField> newFields){
			fields.clear();
			chosen.clear();
			for(final MergeField f : newFields){
				fields.add(f);
				chosen.add(f.chosen());
			}
			fireTableDataChanged();
		}

		void commitChoices(final MergePlan plan){
			// The plan holds the same MergeField instances the table is
			// showing; the table's chosen values are the user's picks.
			for(int i = 0; i < fields.size(); i ++)
				fields.get(i).choose(chosen.get(i));
		}

		MergeField fieldAt(final int row){
			return (row >= 0 && row < fields.size()? fields.get(row): null);
		}

		void setChoice(final int row, final MergeDecision decision){
			if(row < 0 || row >= fields.size())
				return;

			chosen.set(row, decision);
			fields.get(row).choose(decision);
			fireTableCellUpdated(row, 3);
		}

		/**
		 * Refreshes the summary of a row after its {@link MergeField} has
		 * been recomposed by the comparison dialog. The decision column
		 * is left untouched: the comparison dialog updates the field's
		 * result instances, not its coarse decision.
		 */
		void refreshRow(final int row){
			if(row >= 0 && row < fields.size())
				fireTableRowsUpdated(row, row);
		}

		@Override
		public int getRowCount(){
			return fields.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final MergeField field = fields.get(rowIndex);
			return switch(columnIndex){
				case 0 -> field.tag();
				case 1 -> summarize(field.targetInstances());
				case 2 -> summarize(field.sourceInstances());
				case 3 -> chosen.get(rowIndex);
				case 4 -> StringUtils.EMPTY;
				default -> null;
			};
		}

		@Override
		public boolean isCellEditable(final int rowIndex, final int columnIndex){
			return (columnIndex == ACTION_COLUMN);
		}

		@Override
		public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex){
			if(columnIndex == ACTION_COLUMN && aValue instanceof MergeDecision decision){
				// Guard against spurious commits from the cell editor: when the
				// user simply selects the cell and then leaves it, Swing calls
				// setValueAt with the current value. Marking the field as
				// resolved in that case would tint the row green even though the
				// user never made a choice.
				final MergeDecision current = chosen.get(rowIndex);
				if(decision == current)
					return;

				chosen.set(rowIndex, decision);
				fields.get(rowIndex).choose(decision);
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}

		@Override
		public Class<?> getColumnClass(final int columnIndex){
			return (columnIndex == 3? MergeDecision.class: String.class);
		}

		private static String summarize(final List<FLEFRecord> instances){
			if(instances.isEmpty())
				return "—";
			if(instances.size() == 1)
				return abbreviate(serialize(instances.get(0)));
			final StringBuilder sb = new StringBuilder();
			sb.append(instances.size()).append(" items: ");
			for(int i = 0; i < instances.size() && i < 3; i ++){
				if(i > 0)
					sb.append(" | ");
				sb.append(abbreviate(serialize(instances.get(i))));
			}
			if(instances.size() > 3)
				sb.append(" …");
			return sb.toString();
		}

		private static String serialize(final FLEFRecord record){
			if(!record.hasChildren())
				return record.getTag() + (record.getValue() != null? StringUtils.SPACE + record.getValue(): StringUtils.EMPTY);
			final StringBuilder sb = new StringBuilder();
			sb.append(record.getTag());
			if(record.getValue() != null)
				sb.append(' ').append(record.getValue());
			sb.append(" {…}");
			return sb.toString();
		}

		private static String abbreviate(final String text){
			final int max = 60;
			return (text.length() <= max? text: text.substring(0, max - 1) + "…");
		}

		/**
		 * Restores the given row to its initial state. Clears the resolved
		 * flag, drops any fine-grained composition from the comparison dialog,
		 * and reverts the action column to the suggested decision.
		 *
		 * @param row the row to reset
		 */
		void resetRow(final int row){
			if(row < 0 || row >= fields.size())
				return;
			fields.get(row).reset();
			chosen.set(row, fields.get(row).chosen());
			fireTableRowsUpdated(row, row);
		}

	}


	/**
	 * Renders the collision table. When a field has been explicitly
	 * resolved by the user (either by opening the comparison dialog and
	 * clicking OK, or by picking a decision from the combo), the row is
	 * tinted in pale green, the action cell is prefixed with a checkmark,
	 * and the reset cell shows a small undo arrow. The user can click the
	 * arrow (or press Delete on a selected row) to bring the row back to
	 * its initial state.
	 * <p>
	 * The selection state wins over the resolved tint: a selected row uses
	 * the standard selection colours, and the tint reappears when the
	 * selection moves away.
	 */
	private static final class ResolvedAwareRenderer extends DefaultTableCellRenderer{

		private static final Color COLOR_RESOLVED_BG = new Color(0xE8F5E9);
		private static final Color COLOR_RESOLVED_FG = new Color(0x2E7D32);

		private final CollisionsTableModel model;

		ResolvedAwareRenderer(final CollisionsTableModel model){
			this.model = model;
		}

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			final int modelColumn = table.convertColumnIndexToModel(column);
			final MergeField field = model.fieldAt(row);
			final boolean resolved = (field != null && field.isResolved());

			// Alignment: every column sets its own, no exceptions. The
			// renderer is shared between cells, so leaving a value from a
			// previous cell would leak to the next one.
			if(modelColumn == RESET_COLUMN)
				setHorizontalAlignment(CENTER);
			else
				setHorizontalAlignment(LEFT);

			// Text: override whatever super set (value.toString()) with the
			// human-readable label, in both the selected and the unselected
			// state.
			if(modelColumn == ACTION_COLUMN && value instanceof MergeDecision d)
				setText(resolved? "\u2713 " + d.label(): d.label());
			else if(modelColumn == RESET_COLUMN)
				setText(resolved? "\u21BA": StringUtils.EMPTY);

			// Colors: selection wins over the resolved tint.
			if(isSelected){
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else{
				setBackground(resolved? COLOR_RESOLVED_BG: table.getBackground());
				setForeground(resolved? COLOR_RESOLVED_FG: table.getForeground());
			}

			return this;
		}

	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		// Change this to test other record types.
		final String modelUri = "/tests/TGMZ.flef";
		final Class<? extends RecordTypeHandler<?>> handlerType = IndividualHandler.class;

		final String content;
		try(final InputStream is = MergeRecordsDialog.class.getResourceAsStream(modelUri)){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final ToolContext context = new ToolContext(model, null, null, null);
			final MergeRecordsDialog dialog = new MergeRecordsDialog(context, handlerType)
				.withSource(model.getRecordById("I1"))
				.withTarget(model.getRecordById("I635"));
			dialog.setVisible(true);

			// Print the model state once the dialog is closed, so a test run
			// can verify what the merge actually changed.
			System.out.println("--- after merge ---");
			final List<FLEFRecord> records = model.getRecords();
			for(final FLEFRecord record : records){
				final String id = record.getId();
				if(id != null && (id.startsWith(IndividualHandler.ID_PREFIX) || id.startsWith("F") || id.startsWith(GroupHandler.ID_PREFIX)))
					System.out.println("  " + id + "  " + record.getTag());
			}
		});
	}

}

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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Generic dialog that merges two records of the same type.
 * <p>
 * The dialog is not tied to a specific record type: it is opened with a {@link RecordTypeHandler} and works on whatever tags and
 * values the FLEF records contain. It only collects the two records: the merge is decided in the review of the whole record
 * ({@link MergeRecordReviewDialog}), where source, result and target are side by side, as {@link RecordMergeWorkflow} does.
 * <p>
 * Source and target are picked through an {@link EntityField}, which already renders the record using the handler's display text and
 * provides the standard "Set… / Edit… / Clear" context menu used everywhere else in the application. The dialog therefore does not
 * need a separate preview label.
 */
public final class MergeRecordsDialog extends JDialog{

	private final ToolContext context;

	private final EntityField sourceField;
	private final EntityField targetField;

	private final JButton differentButton = new JButton(I18N.t("button.different_records"));
	private final JButton mergeButton = new JButton(I18N.t("button.merge_review"));

	private boolean confirmed;
	private boolean rejected;


	public MergeRecordsDialog(final ToolContext context, final Class<? extends RecordTypeHandler<?>> handlerType){
		super(context.owner(), I18N.t("dialog.merge.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		// The two fields own their own popup menu (Set/Edit/Clear) and render the record through the handler's display text. The
		// property change listener keeps the buttons in sync with whatever the user chooses, whether through the field menu or
		// through a programmatic call to withSource/withTarget.
		final FLEFModel model = context.model();
		sourceField = EntityField.createForRecordFromReference(null, this, model, handlerType);
		targetField = EntityField.createForRecordFromReference(null, this, model, handlerType);
		sourceField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> onRecordsChanged());
		targetField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> onRecordsChanged());

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);
		onRecordsChanged();

		setPreferredSize(new Dimension(760, 230));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the source record programmatically. Equivalent to picking a record through the field's context menu, but skips the
	 * selection dialog.
	 *
	 * @param source the record that will be deleted; may be {@code null} to clear the current selection
	 * @return this dialog, for chaining
	 */
	public MergeRecordsDialog withSource(final FLEFRecord source){
		sourceField.setEntity(source);

		return this;
	}

	/**
	 * Sets the target record programmatically. Equivalent to picking a record through the field's context menu, but skips the
	 * selection dialog.
	 *
	 * @param target the record that will be kept; may be {@code null} to clear the current selection
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

	/**
	 * Returns whether the user recorded the pair as different records.
	 */
	public boolean isRejected(){
		return rejected;
	}

	/**
	 * Returns whether the file was changed by this dialog, that is the pair was merged or recorded as different.
	 */
	public boolean isResolved(){
		return (confirmed || rejected);
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private JPanel createForm(){
		// MigLayout handles the growing column better than GridBagLayout: the two columns are declared once ("[][grow,fill]"), and
		// every field in the second column grows with the dialog. "wrap 2" makes the layout start a new row after every two
		// components, so the label+field pairs line up without any grid arithmetic.
		final JPanel form = new JPanel(new MigLayout("ins 10,fillx,wrap 2", "[][grow,fill]", "[]8[]16[]"));

		// Source row: label + EntityField. The field's own popup menu (Set…, Edit…, Clear) is the only way to change the selection,
		// so no extra "Choose…" button is needed.
		form.add(new JLabel(I18N.l("dialog.merge.source")));
		form.add(sourceField, "growx");

		// Target row.
		form.add(new JLabel(I18N.l("dialog.merge.target")));
		form.add(targetField, "growx");

		final JLabel hint = new JLabel(I18N.t("dialog.merge.hint"));
		hint.setForeground(new Color(0x757575));
		form.add(hint, "span 2,growx");

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		// the alternative to merging: the judgment that they are not the same, kept in the file
		differentButton.setToolTipText(I18N.t("tooltip.merge_different"));
		differentButton.addActionListener(e -> onReject());
		buttons.add(differentButton);

		mergeButton.setToolTipText(I18N.t("tooltip.merge_review"));
		mergeButton.addActionListener(e -> onConfirm());
		buttons.add(mergeButton);
		getRootPane().setDefaultButton(mergeButton);

		return buttons;
	}


	/* ======================================================================
	 *                          Selection
	 * ====================================================================== */

	/** The buttons need two different records of one kind: without them they do nothing but warn. */
	private void onRecordsChanged(){
		final FLEFRecord source = sourceField.getEntity();
		final FLEFRecord target = targetField.getEntity();

		differentButton.setEnabled(RecordMergeWorkflow.canRecordAsDifferent(context.model(), source, target));
		mergeButton.setEnabled(RecordMergeWorkflow.canMerge(source, target));
	}


	/* ======================================================================
	 *                          Confirm
	 * ====================================================================== */

	/** Reviews the whole record, side by side, and merges it (see {@link RecordMergeWorkflow#merge}). */
	private void onConfirm(){
		if(RecordMergeWorkflow.merge(this, context.model(), sourceField.getEntity(), targetField.getEntity())){
			confirmed = true;
			dispose();
		}
	}

	/** Records that the two are different entities, instead of merging them (see {@link RecordMergeWorkflow#recordAsDifferent}). */
	private void onReject(){
		if(RecordMergeWorkflow.recordAsDifferent(this, context.model(), sourceField.getEntity(), targetField.getEntity())){
			rejected = true;
			dispose();
		}
	}


	static void main(final String[] args) throws IOException{
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

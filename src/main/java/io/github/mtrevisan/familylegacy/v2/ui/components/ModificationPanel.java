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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


/**
 * Panel for editing a {@code MODIFICATION_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * MODIFICATION_STRUCTURE :=
 * n CREATION    {1:1}
 *   +1 DATE <DATE>    {1:1}
 * n UPDATE    {0:M}
 *   +1 DATE <DATE>    {1:1}
 *   +1 COMMENT <TEXT>    {0:1}
 * </pre>
 */
public class ModificationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556766L;


	private final BindingManager bindingManager = new BindingManager();

	private final FLEFModel model;
	private final Dialog parent;

	// UI components
	// Creation fields
	private String creationDate;
	private final BoundTextArea creationCommentArea;

	private final UpdateListPanel updateListPanel;


	/**
	 * Constructs a new ModificationPanel.
	 *
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public ModificationPanel(final Dialog parent, FLEFModel model){
		this.model = model;
		this.parent = parent;

		creationCommentArea = new BoundTextArea("CREATION.COMMENT", 3, 25);
		updateListPanel = new UpdateListPanel(parent, model);

		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx", "[grow]", "[]10[]"));

		bindingManager.bind(creationCommentArea);

		final JPanel creationPanel = new JPanel(new MigLayout("fillx", "[grow]"));
		creationPanel.setBorder(new TitledBorder("Creation Comment"));
		creationPanel.add(GUIHelper.createScrollPane(creationCommentArea), "growx");
		add(creationPanel, "growx,wrap");

		add(updateListPanel, "growx");
	}



	/**
	 * Loads data from a record's MODIFICATION_STRUCTURE into the panel.
	 *
	 * @param record the record containing the MODIFICATION_STRUCTURE
	 */
	public void load(final FLEFRecord record){
		clear();

		bindingManager.load(record);

		// Find CREATION
		final FLEFRecord creation = FLEFRecordHelper.findChild(record, "CREATION");
		if(creation != null){
			creationDate = FLEFRecordHelper.getChildValue(creation, "DATE");

			// Load creation comment if present (non-standard, but we keep it)
			final String comment = FLEFRecordHelper.getChildValue(creation, "COMMENT");
			creationCommentArea.setValue(comment);
		}

		updateListPanel.load(record);
	}

	/**
	 * Saves the panel data into the parent's record.
	 *
	 * @param targetRecord the record to save into
	 */
	public void save(final FLEFRecord targetRecord){
		final FLEFRecord record = (targetRecord != null? targetRecord: FLEFRecord.createEmpty());

		// Remove existing children
		FLEFRecordHelper.removeChildren(record, "CREATION");
		FLEFRecordHelper.removeChildren(record, "UPDATE");

		// CREATION
		if(creationDate == null || creationDate.isEmpty())
			creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		FLEFRecordHelper.addChild(record, "CREATION.DATE", creationDate);

		// ---- Save bound simple fields ----
		bindingManager.save(record);

		// Save creation comment if present
		final String creationComment = creationCommentArea.getValue();
		FLEFRecordHelper.addChild(record, "CREATION.COMMENT", creationComment);

		// UPDATE entries
		updateListPanel.save(record);
	}

	public void clear(){
		creationCommentArea.setText(StringUtils.EMPTY);
		updateListPanel.clear();
	}

}

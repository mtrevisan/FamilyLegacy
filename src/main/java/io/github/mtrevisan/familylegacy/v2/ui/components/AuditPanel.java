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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.BasicNoteListPanel;
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
 * Panel for editing a {@code MODIFICATION_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct ModificationStructure {
 *   creation: struct {
 *     date: Date
 *     comment?: Text
 *   }
 *   update*: struct {
 *     date: Date
 *     comment?: Text
 *   }
 * }
 * </pre>
 */
public class AuditPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556766L;

	public static final String DOT = ".";

	public static final String TAG_AUDIT = "AUDIT";
	public static final String TAG_AUDIT_CREATION = TAG_AUDIT + DOT + "CREATION";
	public static final String TAG_UPDATE = "UPDATE";
	public static final String TAG_DATE = "DATE";
	public static final String TAG_COMMENT = "COMMENT";


	private final BindingManager bindingManager = new BindingManager();

	private final JPanel creationPanel;
	private String creationDate;
	private final BoundTextArea creationCommentArea;
	private final BasicNoteListPanel updateListPanel;


	/**
	 * Constructs a new ModificationPanel.
	 *
	 * @param parent	the parent dialog (used for showing message dialogs)
	 */
	public AuditPanel(final Dialog parent){
		creationPanel = new JPanel(new MigLayout("fillx", "[grow]"));

		creationCommentArea = new BoundTextArea(TAG_AUDIT_CREATION + DOT + TAG_COMMENT, 3, 25);
		updateListPanel = new BasicNoteListPanel(TAG_UPDATE, parent, "Updates", TAG_COMMENT);


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(creationCommentArea);


		GUIHelper.setLayoutLabelFieldPanel(this, 10, "[]15[]");

		creationPanel.setBorder(new TitledBorder("Creation Comment"));
		creationPanel.add(GUIHelper.createScrollPane(creationCommentArea), "growx");
		GUIHelper.addComponent(this, creationPanel);

		GUIHelper.addComponent(this, updateListPanel);
	}


	/**
	 * Loads data from a record's MODIFICATION_STRUCTURE into the panel.
	 *
	 * @param record	the record containing the MODIFICATION_STRUCTURE
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		// creation.date
		final FLEFRecord creation = FLEFRecordHelper.findChild(record, TAG_AUDIT_CREATION);
		creationDate = FLEFRecordHelper.getChildValue(creation, TAG_DATE);

		if(creationDate != null)
			creationPanel.setBorder(new TitledBorder("Creation Comment (" + creationDate + ")"));

		bindingManager.load(record);

		// update.comment
		final FLEFRecord audit = FLEFRecordHelper.findChild(record, TAG_AUDIT);
		updateListPanel.load(audit);
	}

	/**
	 * Saves the panel data into the parent's record.
	 *
	 * @param record	the record to save into
	 */
	public void save(final FLEFRecord record){
		// creation.date
		final FLEFRecord creation = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_AUDIT_CREATION);
		if(StringUtils.isBlank(creationDate))
			creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		FLEFRecordHelper.addChild(creation, TAG_DATE, creationDate);

		bindingManager.save(record);

		// update
		final FLEFRecord audit = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_AUDIT);
		updateListPanel.save(audit);
	}

	public void clear(){
		creationCommentArea.setText(StringUtils.EMPTY);
		updateListPanel.clear();
	}

}

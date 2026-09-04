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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.ProjectInfo;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HeaderHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;
import java.time.ZoneOffset;


/**
 * Dialog for editing the {@code HEADER} singleton structure according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * struct Header {
 *   protocol: struct {
 *     name: Text
 *     version: SemVer
 *   }
 *   source?: struct {
 *     name?: Text
 *     version?: SemVer
 *     organization?: Text
 *   }
 *   date: Date
 *   copyright?: Text
 *   submitter?: struct {
 *     contact*: ContactStructure
 *     note*: Text
 *   }
 *   scope?: Text
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): (date), copyright, scope
 * Tab 11 (Submitter): name, contact, note
 */
public class HeaderDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8685753096364050900L;


	private static final String DOT = ".";

	private static final String TAG_NAME = "NAME";
	private static final String TAG_VERSION = "VERSION";
	private static final String TAG_PROTOCOL = "PROTOCOL";
	private static final String TAG_PROTOCOL_NAME = TAG_PROTOCOL + DOT + TAG_NAME;
	private static final String TAG_PROTOCOL_VERSION = TAG_PROTOCOL + DOT + TAG_VERSION;
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SOURCE_NAME = TAG_SOURCE + DOT + TAG_NAME;
	private static final String TAG_SOURCE_VERSION = TAG_SOURCE + DOT + TAG_VERSION;
	private static final String TAG_SOURCE_ORGANIZATION = TAG_SOURCE + DOT + "ORGANIZATION";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_COPYRIGHT = "COPYRIGHT";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_SUBMITTER_CONTACT = TAG_SUBMITTER + DOT + "CONTACT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SUBMITTER_NOTE = TAG_SUBMITTER + DOT + TAG_NOTE;
	private static final String TAG_SCOPE = "SCOPE";

	private static final String PROTOCOL_NAME = "Family LEgacy Format";
	private static final String PROTOCOL_VERSION = "0.1.2";
	private static final String SOURCE_ORGANIZATION = "Mauro Trevisan";

	private final RecordDialogComponents components;

	private final BoundTextField protocolNameField;
	private final BoundTextField protocolVersionField;

	private final BoundTextField sourceNameField;
	private final BoundTextField sourceVersionField;
	private final BoundTextField sourceOrganizationField;

	private final BoundTextField dateField;

	private final BoundTextArea copyrightArea;

	private final EntityListPanel submitterContactListPanel;
	private final BoundTextArea submitterNoteArea;

	private final BoundTextArea scopeArea;


	public HeaderDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, model.getHeader(), HeaderHandler.getInstance());

		final String sourceName = ProjectInfo.getAppName();
		final String sourceVersion = ProjectInfo.getAppVersion();

		protocolNameField = new BoundTextField(TAG_PROTOCOL_NAME, PROTOCOL_NAME);
		protocolVersionField = new BoundTextField(TAG_PROTOCOL_VERSION, PROTOCOL_VERSION);
		sourceNameField = new BoundTextField(TAG_SOURCE_NAME, sourceName);
		sourceVersionField = new BoundTextField(TAG_SOURCE_VERSION, sourceVersion);
		sourceOrganizationField = new BoundTextField(TAG_SOURCE_ORGANIZATION, SOURCE_ORGANIZATION);
		dateField = new BoundTextField(TAG_DATE);
		dateField.setEnabled(false);
		copyrightArea = new BoundTextArea(TAG_COPYRIGHT, 3, 25);
		submitterContactListPanel = EntityListPanel.createForStructure(TAG_SUBMITTER_CONTACT, this, "Contacts", model, ContactHandler.class);
		submitterNoteArea = new BoundTextArea(TAG_SUBMITTER_NOTE, 3, 25);
		scopeArea = new BoundTextArea(TAG_SCOPE, 3, 25);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.build();

		components.bind(protocolNameField);
		components.bind(protocolVersionField);
		components.bind(sourceNameField);
		components.bind(sourceVersionField);
		components.bind(sourceOrganizationField);
		components.bind(dateField);
		components.bind(copyrightArea);
		components.bind(submitterNoteArea);
		components.bind(scopeArea);


		finalizeDialog(parent);
	}


	@Override
	protected void initComponents(){
		setLayout(GUIHelper.createLabelFieldLayout(10, "[grow]"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Properties", createPropertiesPanel());
		tabbedPane.addTab("Submitter", createSubmitterPanel());
		GUIHelper.addComponent(this, tabbedPane);

		final JPanel buttonPanel = GUIHelper.createSaveCancelButtonPanel(this,
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		// date
		GUIHelper.addLabeledComponent(panel, "Date:", dateField);

		// copyright
		GUIHelper.addLabeledComponent(panel, "Copyright:", copyrightArea);

		// scope
		GUIHelper.addLabeledComponent(panel, "Scope:", scopeArea);

		return panel;
	}

	private JPanel createSubmitterPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// contact
		GUIHelper.addComponent(panel, submitterContactListPanel);

		// note
		GUIHelper.addLabeledComponent(panel, "Note:", submitterNoteArea);

		return panel;
	}


	@Override
	protected void loadData(){
		components.load(record);

		submitterContactListPanel.load(record);
	}

	@Override
	protected void saveData(){
		if(dateField.isEmpty())
			dateField.setText(LocalDate.now(ZoneOffset.UTC).toString());

		components.save(record);

		submitterContactListPanel.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(HeaderDialog::new, "/tests/test.flef", null);
	}

}

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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.BasicNoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.time.LocalDate;
import java.time.ZoneOffset;


/* DONE */
/**
 * Dialog for editing the {@code HEADER} singleton structure according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct Header {
 *   protocol: struct {
 *     name: Text
 *     version: SemVer
 *   }
 *   source: struct {
 *     system_id: Text
 *     name?: Text
 *     version?: SemVer
 *     corporate?: Text
 *   }
 *   date: Date
 *   copyright?: Text
 *   submitter?: struct {
 *     name: Text
 *     contact*: ContactStructure
 *     note*: Text
 *   }
 *   scope?: Text
 * }
 * </pre>
 */
public class HeaderDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8685753096364050900L;


	private static final String DOT = ".";
	private static final String TAG_NAME = "NAME";
	private static final String TAG_VERSION = "VERSION";

	private static final String TAG_PROTOCOL = "PROTOCOL";
	private static final String TAG_PROTOCOL_NAME = TAG_PROTOCOL + DOT + TAG_NAME;
	private static final String TAG_PROTOCOL_VERSION = TAG_PROTOCOL + DOT + TAG_VERSION;
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SOURCE_SYSTEM_ID = TAG_SOURCE + DOT + "SYSTEM_ID";
	private static final String TAG_SOURCE_NAME = TAG_SOURCE + DOT + TAG_NAME;
	private static final String TAG_SOURCE_VERSION = TAG_SOURCE + DOT + TAG_VERSION;
	private static final String TAG_SOURCE_CORPORATE = TAG_SOURCE + DOT + "CORPORATE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_COPYRIGHT = "COPYRIGHT";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_SUBMITTER_NAME = TAG_SUBMITTER + DOT + TAG_NAME;
	private static final String TAG_SUBMITTER_CONTACT = TAG_SUBMITTER + DOT + "CONTACT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SCOPE = "SCOPE";


	static{
		HandlerRegistry.register(new ContactHandler());
	}

	private final BindingManager bindingManager = new BindingManager();

	private final FLEFRecord headerRecord;

	private final BoundTextField protocolNameField;
	private final BoundTextField protocolVersionField;

	private final BoundTextField sourceSystemIdField;
	private final BoundTextField sourceNameField;
	private final BoundTextField sourceVersionField;
	private final BoundTextField sourceCorporateField;

	private final BoundTextField dateField;

	private final BoundTextArea copyrightArea;

	private final BoundTextField submitterNameField;
	private final EntityReferenceListPanel submitterContactListPanel;
	private final BasicNoteListPanel submitterNotePanel;

	private final BoundTextArea scopeArea;


	public HeaderDialog(final Dialog parent, final FLEFModel model){
		super(parent, "Header", ModalityType.APPLICATION_MODAL);

		headerRecord = model.getHeader();

		protocolNameField = new BoundTextField(TAG_PROTOCOL_NAME, "Family LEgacy Format");
		protocolVersionField = new BoundTextField(TAG_PROTOCOL_VERSION, "0.1.1");
		sourceSystemIdField = new BoundTextField(TAG_SOURCE_SYSTEM_ID, "FamilyLegacy");
		sourceNameField = new BoundTextField(TAG_SOURCE_NAME, "FL");
		sourceVersionField = new BoundTextField(TAG_SOURCE_VERSION, "0.1");
		sourceCorporateField = new BoundTextField(TAG_SOURCE_CORPORATE, "(c) Mauro Trevisan");
		dateField = new BoundTextField(TAG_DATE);
		dateField.setEnabled(false);
		copyrightArea = new BoundTextArea(TAG_COPYRIGHT, 3, 25);
		submitterNameField = new BoundTextField(TAG_SUBMITTER_NAME);
		submitterContactListPanel = EntityReferenceListPanel.createForStructure(TAG_SUBMITTER_CONTACT, this, "Contacts", model, ContactHandler.TYPE);
		submitterNotePanel = new BasicNoteListPanel(TAG_SUBMITTER, this, "Notes",
			false, TAG_NOTE);
		scopeArea = new BoundTextArea(TAG_SCOPE, 3, 25);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(protocolNameField);
		bindingManager.bind(protocolVersionField);
		bindingManager.bind(sourceSystemIdField);
		bindingManager.bind(sourceNameField);
		bindingManager.bind(sourceVersionField);
		bindingManager.bind(sourceCorporateField);
		bindingManager.bind(dateField);
		bindingManager.bind(copyrightArea);
		bindingManager.bind(submitterNameField);
		bindingManager.bind(scopeArea);


		setLayout(new MigLayout("ins 10,fillx", "[grow]"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Submitter", createSubmitterPanel());
		add(tabbedPane, BorderLayout.CENTER);

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]15[]15[]"));

		// date
		panel.add(new JLabel("Date:"), "align label");
		panel.add(dateField, "growx,wrap");

		// copyright
		panel.add(new JLabel("Copyright:"), "align label,top");
		panel.add(GUIHelper.createScrollPane(copyrightArea), "growx,wrap");

		// scope
		panel.add(new JLabel("Scope:"), "align label,top");
		panel.add(GUIHelper.createScrollPane(scopeArea), "growx");

		return panel;
	}

	private JPanel createSubmitterPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]15[]15[]"));

		// name
		panel.add(new JLabel("Name:"), "align label,top");
		panel.add(submitterNameField, "growx,wrap");

		// contact
		panel.add(submitterContactListPanel, "span 2,growx,wrap");

		// note
		panel.add(submitterNotePanel, "span 2,growx,wrap");

		return panel;
	}


	private void loadData(){
		bindingManager.load(headerRecord);

		submitterContactListPanel.load(headerRecord);
		submitterNotePanel.load(headerRecord);
	}

	private void save(){
		FLEFRecordHelper.removeAllChildren(headerRecord);

		if(dateField.isEmpty())
			dateField.setText(LocalDate.now(ZoneOffset.UTC).toString());

		bindingManager.save(headerRecord);

		submitterContactListPanel.save(headerRecord);
		submitterNotePanel.save(headerRecord);

		dispose();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final HeaderDialog dialog = new HeaderDialog(null, model);
			dialog.setVisible(true);
		});
	}

}

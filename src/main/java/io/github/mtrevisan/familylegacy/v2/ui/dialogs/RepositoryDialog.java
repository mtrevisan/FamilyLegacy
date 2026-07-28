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

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a {@code REPOSITORY_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * REPOSITORY_RECORD :=
 * n @<XREF:REPOSITORY>@ REPOSITORY    {1:1}
 *   +1 <<NAME_STRUCTURE>>    {1:M}
 *   +1 CUSTODIAN @<XREF:INDIVIDUAL>@    {0:1}
 *   +1 <<PLACE_STRUCTURE>>    {0:1}
 *   +1 <<CONTACT_STRUCTURE>>    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class RepositoryDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3053114409506763765L;


	// Handlers
	static{
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
	}


	private final NameListPanel namePanel;
	private final JTextField custodianDisplayField = new JTextField(20);
	private String selectedCustodianId;
	private final PlaceField placeField;
	private final ContactListPanel contactPanel;
	private final NoteListPanel notePanel;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
	private final ModificationPanel modificationPanel;


	public static RepositoryDialog createNew(final Dialog parent, final FLEFModel model){
		return new RepositoryDialog(parent, model, null);
	}

	public static RepositoryDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new RepositoryDialog(parent, model, record);
	}


	private RepositoryDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RepositoryHandler.TYPE));

		placeField = PlaceField.create(parent, "Place", model);
		modificationPanel = new ModificationPanel(this);
		notePanel = new NoteListPanel(model, this);
		namePanel = new NameListPanel(this, model);
		contactPanel = new ContactListPanel(model, this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new MigLayout("fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		add(createButtonPanel(), BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// NAME_STRUCTURE
		mainPanel.add(namePanel, "span 2,growx,wrap");

		// CUSTODIAN
		mainPanel.add(new JLabel("Custodian:"), "align label");
		custodianDisplayField.setEditable(false);
		custodianDisplayField.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(custodianDisplayField,
			() -> selectedCustodianId != null && !selectedCustodianId.isEmpty(),
			this::editIndividual,
			this::createNewIndividual,
			this::clearIndividual,
			builder -> {
				builder.item("Create New...", this::createNewIndividual);
				builder.item("Add Existing...", this::addIndividual);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editIndividual);
				builder.selectionSensitiveItem("Clear", this::clearIndividual);
			});
		mainPanel.add(custodianDisplayField, "growx,wrap");

		// PLACE
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeField, "growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(contactPanel, "growx");
		panel.add(notePanel, "growx");
		return panel;
	}

	private void createNewIndividual(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final BaseRecordDialog dialog = (BaseRecordDialog)individualHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		if(dialog.isSaved() && dialog.getRecord() != null){
			selectedCustodianId = dialog.getRecord().getId();
			updateIndividualDisplay();
		}
	}

	private void addIndividual(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, individualHandler, selectedId -> {
			if(selectedId != null){
				selectedCustodianId = selectedId;
				updateIndividualDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void editIndividual(){
		if(selectedCustodianId == null)
			return;

		final FLEFRecord record = model.getRecordById(selectedCustodianId);
		if(record != null){
			final IndividualDialog dialog = IndividualDialog.createEdit(this, model, record);
			dialog.setVisible(true);
			updateIndividualDisplay();
		}
	}

	private void clearIndividual(){
		selectedCustodianId = null;
		custodianDisplayField.setText(StringUtils.EMPTY);
	}

	private void updateIndividualDisplay(){
		String displayText = StringUtils.EMPTY;
		if(selectedCustodianId != null && !selectedCustodianId.isEmpty()){
			final FLEFRecord rec = model.getRecordById(selectedCustodianId);
			if(rec != null){
				final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
				displayText = individualHandler.getDisplayText(rec);
			}
			else
				displayText = selectedCustodianId;
		}
		custodianDisplayField.setText(displayText);
	}


	@Override
	protected void loadData(){
		// NAME_STRUCTURE
		final List<FLEFRecord> names = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("NAME".equals(child.getTag()))
				names.add(child);
		namePanel.setItems(names);

		// CUSTODIAN
		final String custodianId = FLEFRecordUtils.getChildValue(record, "CUSTODIAN");
		if(custodianId != null && !custodianId.isEmpty()){
			selectedCustodianId = custodianId;
			updateIndividualDisplay();
		}

		// PLACE_STRUCTURE
		placeField.load(record);

		// CONTACT_STRUCTURE
		List<FLEFRecord> contacts = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("CONTACT".equals(child.getTag()))
				contacts.add(child);
		contactPanel.setItems(contacts);

		// NOTE
		final List<FLEFRecord> notes = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("NOTE".equals(child.getTag()) && child.getValue() != null)
				notes.add(child);
		notePanel.loadFromNotes(notes);

		// MODIFICATION
		modificationPanel.load(record);
	}

	@Override
	protected boolean validateData(){
		if(namePanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this, "At least one NAME structure is required.",
				tabbedPane, mainPanel, namePanel);

			return false;
		}
		return true;
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// NAME
		for(final FLEFRecord nameRec : namePanel.getItems()){
			nameRec.setTag("NAME");
			record.addChild(nameRec);
		}

		// CUSTODIAN
		if(StringUtils.isNotBlank(selectedCustodianId))
			FLEFRecordUtils.addChild(record, "CUSTODIAN", FLEFRecordUtils.formatXRef(selectedCustodianId));

		// PLACE
		placeField.save(record);

		// CONTACT
		for(final FLEFRecord contact : contactPanel.getItems()){
			contact.setTag("CONTACT");
			record.addChild(contact);
		}

		// NOTE
		for(final FLEFRecord note : notePanel.getNotes())
			FLEFRecordUtils.addChild(record, "NOTE", note.getId());

		// MODIFICATION
		modificationPanel.save(record);

		if(isNew)
			model.addRecord(record);
		isSaved = true;

// TODO to be removed
FLEFFile.print(model);
//		dispose();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final RepositoryDialog dialog = RepositoryDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}

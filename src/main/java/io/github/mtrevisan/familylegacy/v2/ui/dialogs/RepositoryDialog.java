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
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.IndividualField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
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


	static{
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
	}


	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final NameListPanel namePanel;
	private final IndividualField custodianField;
	private final PlaceField placeField;
	private final ContactListPanel contactPanel;
	private final NoteListPanel notePanel;
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

		namePanel = new NameListPanel("NAME", this, model);
		custodianField = IndividualField.create("CUSTODIAN", parent, model);
		placeField = PlaceField.create("PLACE", parent, model);
		contactPanel = new ContactListPanel("CONTACT", model, this);
		notePanel = new NoteListPanel("NOTE", model, this);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		setLayout(new MigLayout("fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// name structure
		mainPanel.add(namePanel, "span 2,growx,wrap");

		// custodian
		mainPanel.add(new JLabel("Custodian:"), "align label");
		mainPanel.add(custodianField, "growx,wrap");

		// place structure
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeField, "growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]"));

		// contact structure
		panel.add(contactPanel, "growx");

		// note
		panel.add(notePanel, "growx");

		return panel;
	}


	@Override
	protected void loadData(){
		namePanel.load(record);
		custodianField.load(record);
		placeField.load(record);
		contactPanel.load(record);
		notePanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(namePanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this, "At least one NAME structure is required.",
				tabbedPane, mainPanel, namePanel);

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		namePanel.save(record);
		custodianField.save(record);
		placeField.save(record);
		contactPanel.save(record);
		notePanel.save(record);
		modificationPanel.save(record);
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

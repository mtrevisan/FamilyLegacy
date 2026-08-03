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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DocumentStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * SOURCE_RECORD :=
 * n @<XREF:SOURCE>@ SOURCE    {1:1}
 *   +1 TITLE    {1:M}
 *     +2 <<TEXT_VALUE>>    {1:1}
 *   +1 AUTHOR <SOURCE_ORIGINATOR>    {0:1}
 *   +1 DATE    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 <<PLACE_STRUCTURE>>    {0:1}
 *   +1 PUBLISHER <SOURCE_PUBLISHER>    {0:1}
 *   +1 <<REPOSITORY_CITATION>>    {0:M}
 *   +1 MEDIA_TYPE <SOURCE_MEDIA_TYPE>    {0:1}
 *   +1 <<DOCUMENT_STRUCTURE>>    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class SourceDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8722200901398839002L;


	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField;
	private final BoundTextField authorField;
	private final BoundTextField publisherField;
	private final BoundComboBox<String> mediaTypeCombo;
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final PlaceField placeField;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("PLACE", "Place Evidence");
	private final DateField dateField;
	private final DefaultListModel<String> repositoryListModel = new DefaultListModel<>();
	private final JList<String> repositoryList = new JList<>(repositoryListModel);
	private final List<FLEFRecord> repositoryRecords = new ArrayList<>();
	private final DocumentStructurePanel documentPanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();
	private final ModificationPanel modificationPanel;


	public static SourceDialog createNew(final Dialog parent, final FLEFModel model){
		return new SourceDialog(parent, model, null);
	}

	public static SourceDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new SourceDialog(parent, model, record);
	}

	private SourceDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(SourceHandler.TYPE));

		// Initialize bound components
		titleField = new BoundTextField("TITLE.VALUE", 30);
		authorField = new BoundTextField("AUTHOR", 30);
		publisherField = new BoundTextField("PUBLISHER", 30);
		mediaTypeCombo = new BoundComboBox<>("MEDIA_TYPE",
			new String[]{StringUtils.EMPTY, "audio", "book", "card", "electronic", "fiche", "film",
				"magazine", "manuscript", "map", "newspaper", "photo",
				"tombstone", "video"});

		placeField = PlaceField.create("PLACE", parent, model);
		dateField = DateField.createWithWrapperTag("DATE", this, "Valid Date", model);
		documentPanel = new DocumentStructurePanel(model, this);
		modificationPanel = new ModificationPanel(this, model);
		sourceCitationPanel = new SourceCitationListPanel("SOURCE", this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		bindingManager.bind(titleField);
		bindingManager.bind(authorField);
		bindingManager.bind(publisherField);
		bindingManager.bind(mediaTypeCombo);

		setLayout(new MigLayout("fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Place & Date", createPlaceDatePanel());
		tabbedPane.addTab("Repositories", createRepositoryCitationsPanel());
		tabbedPane.addTab("Document", documentPanel);
		tabbedPane.addTab("Source Citations", sourceCitationPanel);
		tabbedPane.addTab("Notes", createNotesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));

		panel.add(new JLabel("Title:"), "align label");
		panel.add(titleField, "growx,wrap");
		panel.add(new JLabel("Author:"), "align label");
		panel.add(authorField, "growx,wrap");
		panel.add(new JLabel("Publisher:"), "align label");
		panel.add(publisherField, "growx,wrap");
		panel.add(new JLabel("Media Type:"), "align label");
		panel.add(mediaTypeCombo, "growx,wrap");
		panel.add(restrictionCheckBox, "span 2");

		return panel;
	}

	private JPanel createPlaceDatePanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]"));

		final JPanel placePanel = new JPanel(new MigLayout("ins 10,fillx,top", "[grow]", "[]5[]"));
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.add(placeField, "growx,wrap");
		placePanel.add(placeQualifiers, "growx,wrap");
		panel.add(placePanel, "span 2,growx,wrap");

		panel.add(new JLabel("Date:"), "align label,top");
		panel.add(dateField, "growx");

		return panel;
	}

	private JPanel createRepositoryCitationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Repository Citation"));

		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editRepositoryCitation();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(repositoryList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Repository");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		repositoryList.addListSelectionListener(e -> {
			boolean selected = repositoryList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addRepositoryCitation());
		newBtn.addActionListener(e -> createNewRepository());
		editBtn.addActionListener(e -> editRepositoryCitation());
		deleteBtn.addActionListener(e -> deleteRepositoryCitation());

		return panel;
	}

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note"));

		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addNote());
		newBtn.addActionListener(e -> createNewNote());
		editBtn.addActionListener(e -> editNote());
		deleteBtn.addActionListener(e -> deleteNote());

		return panel;
	}


	private String getRepositoryCitationDisplay(FLEFRecord citation){
		String repoId = citation.getValue();
		if(repoId != null){
			FLEFRecord rec = model.getRecordById(repoId);
			if(rec != null){
				final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler(RepositoryHandler.TYPE);
				String display = repositoryHandler.getDisplayText(rec, model);
				String location = FLEFRecordHelper.getChildValue(citation, "LOCATION");
				if(location != null && !location.isEmpty()){
					return display + " (loc: " + location + ")";
				}
				return display;
			}
		}
		return repoId != null? repoId: "[empty]";
	}

	private void loadRepositoryCitations(){
		repositoryListModel.clear();
		repositoryRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("REPOSITORY_CITATION".equals(child.getTag())){
				repositoryRecords.add(child);
				repositoryListModel.addElement(getRepositoryCitationDisplay(child));
			}
		}
	}

	private void addRepositoryCitation(){
		final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler(RepositoryHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, repositoryHandler, selectedId -> {
			if(selectedId != null){
				String location = JOptionPane.showInputDialog(
					this,
					"Enter location within repository:",
					"Location",
					JOptionPane.PLAIN_MESSAGE
				);
				FLEFRecord citation = FLEFRecord.createChildWithValue("REPOSITORY_CITATION", selectedId);
				FLEFRecordHelper.updateChildValue(citation, "LOCATION", location.trim());
				repositoryRecords.add(citation);
				repositoryListModel.addElement(getRepositoryCitationDisplay(citation));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editRepositoryCitation(){
		int idx = repositoryList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = repositoryRecords.get(idx);
		String repoId = existing.getValue();
		String location = FLEFRecordHelper.getChildValue(existing, "LOCATION");

		final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler(RepositoryHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, repositoryHandler, selectedId -> {
			if(selectedId != null){
				String newLocation = (String)JOptionPane.showInputDialog(
					this,
					"Enter location within repository:",
					"Location",
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					StringUtils.defaultString(location)
				);
				existing.setValue(selectedId);
				FLEFRecordHelper.updateChildValue(existing, "LOCATION", newLocation.trim());
			}
			repositoryRecords.set(idx, existing);
			repositoryListModel.set(idx, getRepositoryCitationDisplay(existing));
		}
		);
		dialog.setVisible(true);
	}

	private void deleteRepositoryCitation(){
		int idx = repositoryList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this repository citation?"))
			return;
		repositoryRecords.remove(idx);
		repositoryListModel.remove(idx);
	}

	private void createNewRepository(){
		final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler(RepositoryHandler.TYPE);
		JDialog dialog = repositoryHandler.createNewDialog(this, model);
		dialog.setVisible(true);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
			return noteHandler.getDisplayText(rec, model);
		}
		return id;
	}

	private void loadNotes(){
		noteListModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
		}
	}

	private void addNote(){
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this note reference?"))
			return;
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
				break;
			}
		}
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		// ---- Load manual fields ----

		// Restriction (checkbox)
		String restriction = FLEFRecordHelper.getChildValue(record, "RESTRICTION");
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		// PLACE_STRUCTURE
		placeField.load(record);

		// Date
		dateField.load(record);

		// Repository Citations
		loadRepositoryCitations();

		// Document
		FLEFRecord doc = FLEFRecordHelper.findChild(record, "DOCUMENT_STRUCTURE");
		documentPanel.loadFromRecord(doc);

		// SOURCE_CITATION
		sourceCitationPanel.load(record);

		// Notes
		loadNotes();

		// Modification
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		// ---- Save bound simple fields ----
		bindingManager.save(record);

		// ---- Save manual fields ----

		// Restriction
		FLEFRecordHelper.updateChildValue(record, "RESTRICTION",
			(restrictionCheckBox.isSelected()? "confidential": null));

		// PLACE
		placeField.save(record);

		// DATE
		dateField.save(record);

		// Repository Citations
		for(FLEFRecord citation : repositoryRecords){
			citation.setTag("REPOSITORY_CITATION");
			record.addChild(citation);
		}

		// Document
		if(documentPanel.hasData()){
			FLEFRecord doc = documentPanel.saveToRecord(null);
			if(doc != null){
				doc.setTag("DOCUMENT_STRUCTURE");
				record.addChild(doc);
			}
		}

		// SOURCE CITATIONS
		sourceCitationPanel.save(record);

		// Notes
		for(String id : noteIds){
			FLEFRecordHelper.addChild(record, "NOTE", id);
		}

		// Modification
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final SourceDialog dialog = SourceDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
